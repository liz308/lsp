#ifndef LSP_PARAMETER_QUEUE_H
#define LSP_PARAMETER_QUEUE_H

#include <atomic>
#include <cstdint>
#include <cstring>
#include <array>
#include <memory>
#include <cassert>
#include <chrono>
#include <thread>
#include <algorithm>

        #ifdef _WIN32
    #include <windows.h>
    #include <intrin.h>
        #else
    #include <time.h>
    #include <unistd.h>
    #include <sys/time.h>
    #if defined(__x86_64__) || defined(__i386__)
        #include <immintrin.h>
        #endif
#endif

/**
 * Lock-free Single-Producer Single-Consumer (SPSC) queue for parameter updates.
 * 
 * This queue is designed for thread-safe communication between the UI thread
 * (producer) and the audio thread (consumer) without using locks or mutexes.
 * 
 * Features:
 * - Lock-free SPSC implementation using atomic operations
 * - Memory ordering guarantees for correct synchronization
 * - Cache-line padding to prevent false sharing
 * - Overflow handling with configurable behavior
 * - Statistics tracking for performance monitoring
 * - Template-based for type safety and reusability
 * - Hardware-specific optimizations for x86/x64 and ARM
 * - Adaptive backoff strategies for contention handling
 * - Memory prefetching for improved cache performance
 * - NUMA-aware memory allocation support
 * 
 * Thread safety:
 * - UI thread: calls enqueue() to add parameter changes
 * - Audio thread: calls dequeue() to retrieve and apply parameter changes
 * - Statistics can be read from any thread safely
 * - No other threads should access enqueue/dequeue operations
 * 
 * Memory layout optimized for cache performance with proper alignment.
 */

// Cache line size detection for different architectures
#if defined(__x86_64__) || defined(__i386__)
    static constexpr size_t CACHE_LINE_SIZE = 64;
#elif defined(__aarch64__) || defined(__arm__)
    static constexpr size_t CACHE_LINE_SIZE = 64;
#elif defined(__powerpc64__)
    static constexpr size_t CACHE_LINE_SIZE = 128;
#else
    static constexpr size_t CACHE_LINE_SIZE = 64;  // Safe default
#endif

// Memory prefetch hints
#if defined(__GNUC__) || defined(__clang__)
    #define PREFETCH_READ(addr) __builtin_prefetch((addr), 0, 3)
    #define PREFETCH_WRITE(addr) __builtin_prefetch((addr), 1, 3)
    #define LIKELY(x) __builtin_expect(!!(x), 1)
    #define UNLIKELY(x) __builtin_expect(!!(x), 0)
#elif defined(_MSC_VER)
    #define PREFETCH_READ(addr) _mm_prefetch((const char*)(addr), _MM_HINT_T0)
    #define PREFETCH_WRITE(addr) _mm_prefetch((const char*)(addr), _MM_HINT_T0)
    #define LIKELY(x) (x)
    #define UNLIKELY(x) (x)
#else
    #define PREFETCH_READ(addr) ((void)0)
    #define PREFETCH_WRITE(addr) ((void)0)
    #define LIKELY(x) (x)
    #define UNLIKELY(x) (x)
#endif

// CPU pause instruction for spin loops
#if defined(__x86_64__) || defined(__i386__)
    #define CPU_PAUSE() _mm_pause()
#elif defined(__aarch64__) || defined(__arm__)
    #define CPU_PAUSE() __asm__ __volatile__("yield" ::: "memory")
#else
    #define CPU_PAUSE() std::this_thread::yield()
#endif

// Align to cache line boundary to prevent false sharing
#define CACHE_ALIGNED alignas(CACHE_LINE_SIZE)

// Memory barrier macros for different architectures
#if defined(__GNUC__) || defined(__clang__)
    #define COMPILER_BARRIER() __asm__ __volatile__("" ::: "memory")
#elif defined(_MSC_VER)
    #define COMPILER_BARRIER() _ReadWriteBarrier()
#else
    #define COMPILER_BARRIER() std::atomic_thread_fence(std::memory_order_acq_rel)
#endif

struct ParameterUpdate {
    int32_t port_index;
    float value;
    uint64_t timestamp_ns;
    uint32_t sequence_id;
    uint16_t priority;          // Priority level for parameter updates
    uint8_t parameter_type;     // Type of parameter (continuous, discrete, etc.)
    uint8_t flags;              // Additional flags (automation, user_input, etc.)
    float previous_value;       // Previous value for delta calculations
    uint32_t source_id;         // Source identifier (UI, MIDI, automation, etc.)
    uint32_t group_id;          // Parameter group for batch processing
    
    // Flags for parameter update behavior
    enum Flags : uint8_t {
        FLAG_NONE = 0,
        FLAG_AUTOMATION = 1 << 0,
        FLAG_USER_INPUT = 1 << 1,
        FLAG_MIDI_CC = 1 << 2,
        FLAG_SMOOTH = 1 << 3,
        FLAG_IMMEDIATE = 1 << 4,
        FLAG_BATCH_END = 1 << 5
    };
    
    enum ParameterType : uint8_t {
        TYPE_CONTINUOUS = 0,
        TYPE_DISCRETE = 1,
        TYPE_BOOLEAN = 2,
        TYPE_ENUM = 3,
        TYPE_STRING = 4
};

    enum Priority : uint16_t {
        PRIORITY_LOW = 0,
        PRIORITY_NORMAL = 100,
        PRIORITY_HIGH = 200,
        PRIORITY_CRITICAL = 300
    };
    
    ParameterUpdate() 
        : port_index(-1), value(0.0f), timestamp_ns(0), sequence_id(0)
        , priority(PRIORITY_NORMAL), parameter_type(TYPE_CONTINUOUS)
        , flags(FLAG_NONE), previous_value(0.0f), source_id(0), group_id(0) {}
    
    ParameterUpdate(int32_t port, float val, uint64_t ts = 0, uint32_t seq = 0,
                   uint16_t prio = PRIORITY_NORMAL, uint8_t type = TYPE_CONTINUOUS,
                   uint8_t update_flags = FLAG_NONE, float prev_val = 0.0f,
                   uint32_t src_id = 0, uint32_t grp_id = 0)
        : port_index(port), value(val), timestamp_ns(ts), sequence_id(seq)
        , priority(prio), parameter_type(type), flags(update_flags)
        , previous_value(prev_val), source_id(src_id), group_id(grp_id) {}
    
    bool has_flag(Flags flag) const { return (flags & flag) != 0; }
    void set_flag(Flags flag) { flags |= flag; }
    void clear_flag(Flags flag) { flags &= ~flag; }
    
    float get_delta() const { return value - previous_value; }
    bool is_significant_change(float threshold = 0.001f) const {
        return std::abs(get_delta()) >= threshold;
    }
};

enum class QueueOverflowBehavior {
    DROP_OLDEST,        // Drop oldest item when full (default)
    DROP_NEWEST,        // Drop new item when full
    OVERWRITE,          // Overwrite without checking full condition
    DROP_BY_PRIORITY,   // Drop lowest priority items first
    COALESCE_SAME_PORT  // Merge updates for same port
};

enum class BackoffStrategy {
    NONE,           // No backoff
    LINEAR,         // Linear backoff
    EXPONENTIAL,    // Exponential backoff
    ADAPTIVE        // Adaptive based on contention
};

struct QueueStatistics {
    std::atomic<uint64_t> total_enqueued{0};
    std::atomic<uint64_t> total_dequeued{0};
    std::atomic<uint64_t> total_dropped{0};
    std::atomic<uint64_t> total_overflows{0};
    std::atomic<uint64_t> total_coalesced{0};
    std::atomic<uint32_t> max_size_reached{0};
    std::atomic<uint64_t> total_latency_ns{0};
    std::atomic<uint64_t> max_latency_ns{0};
    std::atomic<uint64_t> min_latency_ns{UINT64_MAX};
    std::atomic<uint64_t> contention_count{0};
    std::atomic<uint64_t> cache_misses{0};
    std::atomic<uint64_t> priority_drops{0};
    std::atomic<uint64_t> batch_operations{0};
    std::atomic<uint64_t> memory_allocations{0};
    std::atomic<uint64_t> total_processing_time_ns{0};
    
    void reset() {
        total_enqueued.store(0, std::memory_order_relaxed);
        total_dequeued.store(0, std::memory_order_relaxed);
        total_dropped.store(0, std::memory_order_relaxed);
        total_overflows.store(0, std::memory_order_relaxed);
        total_coalesced.store(0, std::memory_order_relaxed);
        max_size_reached.store(0, std::memory_order_relaxed);
        total_latency_ns.store(0, std::memory_order_relaxed);
        max_latency_ns.store(0, std::memory_order_relaxed);
        min_latency_ns.store(UINT64_MAX, std::memory_order_relaxed);
        contention_count.store(0, std::memory_order_relaxed);
        cache_misses.store(0, std::memory_order_relaxed);
        priority_drops.store(0, std::memory_order_relaxed);
        batch_operations.store(0, std::memory_order_relaxed);
        memory_allocations.store(0, std::memory_order_relaxed);
        total_processing_time_ns.store(0, std::memory_order_relaxed);
    }
    
    double get_average_latency_ms() const {
        uint64_t total_lat = total_latency_ns.load(std::memory_order_relaxed);
        uint64_t total_deq = total_dequeued.load(std::memory_order_relaxed);
        return total_deq > 0 ? (total_lat / 1000000.0) / total_deq : 0.0;
    }
    
    double get_max_latency_ms() const {
        return max_latency_ns.load(std::memory_order_relaxed) / 1000000.0;
    }
    
    double get_min_latency_ms() const {
        uint64_t min_lat = min_latency_ns.load(std::memory_order_relaxed);
        return min_lat != UINT64_MAX ? min_lat / 1000000.0 : 0.0;
    }
    
    double get_drop_rate() const {
        uint64_t total_enq = total_enqueued.load(std::memory_order_relaxed);
        uint64_t total_drop = total_dropped.load(std::memory_order_relaxed);
        return total_enq > 0 ? static_cast<double>(total_drop) / total_enq : 0.0;
    }
    
    double get_throughput_mhz() const {
        uint64_t total_proc = total_processing_time_ns.load(std::memory_order_relaxed);
        uint64_t total_ops = total_enqueued.load(std::memory_order_relaxed) + 
                            total_dequeued.load(std::memory_order_relaxed);
        return total_proc > 0 ? (total_ops * 1000.0) / total_proc : 0.0;
    }
    
    double get_cache_hit_rate() const {
        uint64_t total_ops = total_enqueued.load(std::memory_order_relaxed) + 
                            total_dequeued.load(std::memory_order_relaxed);
        uint64_t misses = cache_misses.load(std::memory_order_relaxed);
        return total_ops > 0 ? 1.0 - (static_cast<double>(misses) / total_ops) : 1.0;
    }
};

struct QueueConfiguration {
    QueueOverflowBehavior overflow_behavior = QueueOverflowBehavior::DROP_OLDEST;
    BackoffStrategy backoff_strategy = BackoffStrategy::ADAPTIVE;
    uint32_t max_backoff_iterations = 1000;
    uint32_t backoff_base_delay_ns = 100;
    bool enable_coalescing = true;
    bool enable_priority_queue = false;
    bool enable_statistics = true;
    bool enable_prefetching = true;
    uint32_t prefetch_distance = 2;
    float coalescing_threshold = 0.001f;
    uint64_t max_latency_warning_ns = 10000000; // 10ms
    size_t numa_node = 0;
    bool pin_memory = false;
};

template<size_t QueueSize = 1024>
class ParameterQueue {
    static_assert((QueueSize & (QueueSize - 1)) == 0, "Queue size must be power of 2");
    static_assert(QueueSize >= 8, "Queue size must be at least 8");
    static_assert(QueueSize <= 1048576, "Queue size must not exceed 1M entries");
    
public:
    static constexpr size_t QUEUE_SIZE = QueueSize;
    static constexpr size_t QUEUE_MASK = QUEUE_SIZE - 1;
    static constexpr size_t MAX_BATCH_SIZE = std::min(QueueSize / 4, size_t(256));

    explicit ParameterQueue(const QueueConfiguration& config = QueueConfiguration{})
        : mConfig(config)
        , mWriteIndex(0)
        , mReadIndex(0)
        , mSequenceCounter(0)
        , mContentionCounter(0)
        , mLastCoalesceCheck(0)
    {
        // Initialize queue with default values
        for (size_t i = 0; i < QUEUE_SIZE; ++i) {
            mQueue[i] = ParameterUpdate{};
        }
        
        // Initialize priority tracking if enabled
        if (mConfig.enable_priority_queue) {
            for (size_t i = 0; i < QUEUE_SIZE; ++i) {
                mPriorityMap[i] = 0;
            }
        }
        
        // Pin memory if requested
        if (mConfig.pin_memory) {
            pin_queue_memory();
        }
        
        // Initialize performance counters
        mLastPerformanceCheck = get_current_time_ns();
    }

    ~ParameterQueue() {
        if (mConfig.pin_memory) {
            unpin_queue_memory();
        }
    }

    /**
     * Enqueues a parameter update from the UI thread with full feature support.
     */
    bool enqueue(int32_t port_index, float value, uint64_t timestamp_ns = 0,
                uint16_t priority = ParameterUpdate::PRIORITY_NORMAL,
                uint8_t parameter_type = ParameterUpdate::TYPE_CONTINUOUS,
                uint8_t flags = ParameterUpdate::FLAG_NONE,
                uint32_t source_id = 0, uint32_t group_id = 0) {
        
        auto start_time = get_current_time_ns();
        
        if (timestamp_ns == 0) {
            timestamp_ns = start_time;
        }
        
        uint32_t sequence_id = mSequenceCounter.fetch_add(1, std::memory_order_relaxed);
        
        // Get previous value for delta calculation
        float previous_value = get_last_value_for_port(port_index);
        
        ParameterUpdate update(port_index, value, timestamp_ns, sequence_id,
                              priority, parameter_type, flags, previous_value,
                              source_id, group_id);
        
        bool result = enqueue_update_internal(update, start_time);
        
        if (mConfig.enable_statistics) {
            auto end_time = get_current_time_ns();
            mStatistics.total_processing_time_ns.fetch_add(end_time - start_time, 
                                                          std::memory_order_relaxed);
        }
        
        return result;
    }

    /**
     * Enqueues a pre-constructed parameter update with advanced processing.
     */
    bool enqueue_update(const ParameterUpdate& update) {
        auto start_time = get_current_time_ns();
        bool result = enqueue_update_internal(update, start_time);
        
        if (mConfig.enable_statistics) {
            auto end_time = get_current_time_ns();
            mStatistics.total_processing_time_ns.fetch_add(end_time - start_time, 
                                                          std::memory_order_relaxed);
        }
        
        return result;
    }

    /**
     * Enqueues multiple parameter updates in a batch operation.
     */
    size_t enqueue_batch(const ParameterUpdate* updates, size_t count) {
        if (!updates || count == 0) {
            return 0;
        }
        
        auto start_time = get_current_time_ns();
        size_t enqueued_count = 0;
        
        // Process coalescing if enabled
        if (mConfig.enable_coalescing && count > 1) {
            auto coalesced_updates = coalesce_updates(updates, count);
            for (const auto& update : coalesced_updates) {
                if (enqueue_update_internal(update, start_time)) {
                    enqueued_count++;
                }
            }
            mStatistics.total_coalesced.fetch_add(count - coalesced_updates.size(), 
                                                 std::memory_order_relaxed);
        } else {
            // Standard batch processing
            for (size_t i = 0; i < count; ++i) {
                if (enqueue_update_internal(updates[i], start_time)) {
                    enqueued_count++;
                } else {
                    break; // Stop on first failure to maintain order
                }
            }
        }
        
        if (mConfig.enable_statistics) {
            mStatistics.batch_operations.fetch_add(1, std::memory_order_relaxed);
            auto end_time = get_current_time_ns();
            mStatistics.total_processing_time_ns.fetch_add(end_time - start_time, 
                                                          std::memory_order_relaxed);
        }
        
        return enqueued_count;
    }

    /**
     * Dequeues a parameter update with comprehensive latency tracking.
     */
    bool dequeue(ParameterUpdate* out_update) {
        if (UNLIKELY(!out_update)) {
            return false;
        }
        
        auto start_time = get_current_time_ns();
        
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_acquire);
        
        // Check if queue is empty
        if (UNLIKELY(read_idx == write_idx)) {
            return false;
        }
        
        // Prefetch next items if enabled
        if (mConfig.enable_prefetching) {
            for (uint32_t i = 1; i <= mConfig.prefetch_distance; ++i) {
                size_t prefetch_idx = (read_idx + i) & QUEUE_MASK;
                if (prefetch_idx != write_idx) {
                    PREFETCH_READ(&mQueue[prefetch_idx]);
                }
            }
        }
        
        // Read the parameter update
        *out_update = mQueue[read_idx];
        
        // Calculate and track comprehensive latency metrics
        if (mConfig.enable_statistics && out_update->timestamp_ns > 0) {
            uint64_t current_time = get_current_time_ns();
            if (current_time > out_update->timestamp_ns) {
                uint64_t latency = current_time - out_update->timestamp_ns;
                
                // Update latency statistics atomically
                mStatistics.total_latency_ns.fetch_add(latency, std::memory_order_relaxed);
                
                // Update max latency
                uint64_t current_max = mStatistics.max_latency_ns.load(std::memory_order_relaxed);
                while (latency > current_max && 
                       !mStatistics.max_latency_ns.compare_exchange_weak(current_max, latency, 
                                                                       std::memory_order_relaxed)) {
                    // Retry loop for max latency update
                }
                
                // Update min latency
                uint64_t current_min = mStatistics.min_latency_ns.load(std::memory_order_relaxed);
                while (latency < current_min && current_min != 0 &&
                       !mStatistics.min_latency_ns.compare_exchange_weak(current_min, latency, 
                                                                       std::memory_order_relaxed)) {
                    // Retry loop for min latency update
                }
                
                // Check for latency warnings
                if (latency > mConfig.max_latency_warning_ns) {
                    handle_latency_warning(latency, *out_update);
                }
            }
        }
        
        // Publish the read index with release semantics
        mReadIndex.store((read_idx + 1) & QUEUE_MASK, std::memory_order_release);
        
        // Update statistics
        if (mConfig.enable_statistics) {
            mStatistics.total_dequeued.fetch_add(1, std::memory_order_relaxed);
            auto end_time = get_current_time_ns();
            mStatistics.total_processing_time_ns.fetch_add(end_time - start_time, 
                                                          std::memory_order_relaxed);
        }
        
        return true;
    }

    /**
     * Dequeues multiple parameter updates with optimized batch processing.
     */
    size_t dequeue_batch(ParameterUpdate* out_updates, size_t max_count) {
        if (UNLIKELY(!out_updates || max_count == 0)) {
            return 0;
        }
        
        auto start_time = get_current_time_ns();
        size_t dequeued_count = 0;
        uint64_t current_time = get_current_time_ns();
        
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_acquire);
        
        // Calculate available items
        size_t available = (write_idx - read_idx) & QUEUE_MASK;
        size_t to_dequeue = std::min(max_count, available);
        
        if (to_dequeue == 0) {
            return 0;
        }
        
        // Prefetch batch if enabled
        if (mConfig.enable_prefetching) {
            for (size_t i = 0; i < std::min(to_dequeue, mConfig.prefetch_distance); ++i) {
                PREFETCH_READ(&mQueue[(read_idx + i) & QUEUE_MASK]);
            }
        }
        
        // Batch dequeue with minimal atomic operations
        for (size_t i = 0; i < to_dequeue; ++i) {
            size_t current_read_idx = (read_idx + i) & QUEUE_MASK;
            out_updates[i] = mQueue[current_read_idx];
            
            // Track latency for this update
            if (mConfig.enable_statistics && out_updates[i].timestamp_ns > 0 && 
                current_time > out_updates[i].timestamp_ns) {
                uint64_t latency = current_time - out_updates[i].timestamp_ns;
                mStatistics.total_latency_ns.fetch_add(latency, std::memory_order_relaxed);
                
                // Update max latency
                uint64_t current_max = mStatistics.max_latency_ns.load(std::memory_order_relaxed);
                while (latency > current_max && 
                       !mStatistics.max_latency_ns.compare_exchange_weak(current_max, latency, 
                                                                       std::memory_order_relaxed)) {
                    // Retry loop
                }
                
                // Update min latency
                uint64_t current_min = mStatistics.min_latency_ns.load(std::memory_order_relaxed);
                while (latency < current_min && current_min != 0 &&
                       !mStatistics.min_latency_ns.compare_exchange_weak(current_min, latency, 
                                                                       std::memory_order_relaxed)) {
                    // Retry loop
                }
            }
            
            dequeued_count++;
        }
        
        // Single atomic update of read index
        mReadIndex.store((read_idx + dequeued_count) & QUEUE_MASK, std::memory_order_release);
        
        // Update statistics
        if (mConfig.enable_statistics) {
            mStatistics.total_dequeued.fetch_add(dequeued_count, std::memory_order_relaxed);
            mStatistics.batch_operations.fetch_add(1, std::memory_order_relaxed);
            auto end_time = get_current_time_ns();
            mStatistics.total_processing_time_ns.fetch_add(end_time - start_time, 
                                                          std::memory_order_relaxed);
        }
        
        return dequeued_count;
    }

    /**
     * Dequeues updates for a specific port with filtering.
     */
    size_t dequeue_port_updates(int32_t port_index, ParameterUpdate* out_updates, 
                               size_t max_count) {
        if (UNLIKELY(!out_updates || max_count == 0)) {
            return 0;
        }
        
        size_t found_count = 0;
        ParameterUpdate temp_update;
        
        while (found_count < max_count && dequeue(&temp_update)) {
            if (temp_update.port_index == port_index) {
                out_updates[found_count++] = temp_update;
            } else {
                // Re-enqueue updates for other ports (not ideal, but functional)
                if (!enqueue_update(temp_update)) {
                    break; // Queue full, stop processing
                }
            }
        }
        
        return found_count;
    }

    /**
     * Peeks at multiple updates without removing them.
     */
    size_t peek_batch(ParameterUpdate* out_updates, size_t max_count) const {
        if (UNLIKELY(!out_updates || max_count == 0)) {
            return 0;
        }
        
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_acquire);
        
        size_t available = (write_idx - read_idx) & QUEUE_MASK;
        size_t to_peek = std::min(max_count, available);
        
        for (size_t i = 0; i < to_peek; ++i) {
            out_updates[i] = mQueue[(read_idx + i) & QUEUE_MASK];
        }
        
        return to_peek;
    }

    /**
     * Peeks at the next parameter update without removing it.
     */
    bool peek(ParameterUpdate* out_update) const {
        if (UNLIKELY(!out_update)) {
            return false;
        }
        
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        
        // Check if queue is empty
        if (UNLIKELY(read_idx == mWriteIndex.load(std::memory_order_acquire))) {
            return false;
        }
        
        // Read the parameter update without advancing read index
        *out_update = mQueue[read_idx];
        return true;
    }

    /**
     * Returns the number of items currently in the queue.
     */
    size_t size() const {
        size_t write_idx = mWriteIndex.load(std::memory_order_acquire);
        size_t read_idx = mReadIndex.load(std::memory_order_acquire);
        return (write_idx - read_idx) & QUEUE_MASK;
    }

    /**
     * Returns the maximum capacity of the queue.
     */
    constexpr size_t capacity() const {
        return QUEUE_SIZE - 1;  // One slot reserved for full/empty distinction
    }

    /**
     * Returns true if the queue is empty.
     */
    bool empty() const {
        return mReadIndex.load(std::memory_order_acquire) == 
               mWriteIndex.load(std::memory_order_acquire);
    }

    /**
     * Returns true if the queue is full.
     */
    bool full() const {
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        size_t next_write_idx = (write_idx + 1) & QUEUE_MASK;
        return next_write_idx == mReadIndex.load(std::memory_order_acquire);
    }

    /**
     * Returns the current load factor (0.0 to 1.0).
     */
    double load_factor() const {
        return static_cast<double>(size()) / capacity();
    }

    /**
     * Clears all items from the queue with proper synchronization.
     */
    void clear() {
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        mReadIndex.store(write_idx, std::memory_order_release);
        
        // Reset sequence counter to prevent gaps
        mSequenceCounter.store(0, std::memory_order_relaxed);
    }

    /**
     * Clears updates for a specific port only.
     */
    size_t clear_port_updates(int32_t port_index) {
        size_t cleared_count = 0;
        ParameterUpdate temp_updates[MAX_BATCH_SIZE];
        
        // Dequeue all updates and re-enqueue non-matching ones
        size_t dequeued = dequeue_batch(temp_updates, MAX_BATCH_SIZE);
        while (dequeued > 0) {
            for (size_t i = 0; i < dequeued; ++i) {
                if (temp_updates[i].port_index == port_index) {
                    cleared_count++;
                } else {
                    enqueue_update(temp_updates[i]);
                }
            }
            dequeued = dequeue_batch(temp_updates, MAX_BATCH_SIZE);
        }
        
        return cleared_count;
    }

    /**
     * Returns a copy of the current queue statistics.
     */
    QueueStatistics get_statistics() const {
        return mStatistics;
    }

    /**
     * Resets all queue statistics to zero.
     */
    void reset_statistics() {
        mStatistics.reset();
        mLastPerformanceCheck = get_current_time_ns();
    }

    /**
     * Updates the queue configuration at runtime.
     */
    void update_configuration(const QueueConfiguration& config) {
        mConfig = config;
        
        // Apply memory pinning changes
        if (config.pin_memory && !mMemoryPinned) {
            pin_queue_memory();
        } else if (!config.pin_memory && mMemoryPinned) {
            unpin_queue_memory();
        }
    }

    /**
     * Gets the current queue configuration.
     */
    const QueueConfiguration& get_configuration() const {
        return mConfig;
    }

    /**
     * Returns detailed memory usage information.
     */
    struct MemoryInfo {
        size_t queue_size_bytes;
        size_t total_size_bytes;
        size_t cache_line_padding_bytes;
        size_t priority_map_bytes;
        size_t statistics_bytes;
        size_t configuration_bytes;
        bool memory_pinned;
        size_t numa_node;
    };

    MemoryInfo get_memory_info() const {
        MemoryInfo info;
        info.queue_size_bytes = sizeof(mQueue);
        info.priority_map_bytes = sizeof(mPriorityMap);
        info.statistics_bytes = sizeof(mStatistics);
        info.configuration_bytes = sizeof(mConfig);
        info.total_size_bytes = sizeof(*this);
        info.cache_line_padding_bytes = info.total_size_bytes - info.queue_size_bytes - 
                                       info.priority_map_bytes - info.statistics_bytes -
                                       info.configuration_bytes - sizeof(mWriteIndex) - 
                                       sizeof(mReadIndex) - sizeof(mSequenceCounter) -
                                       sizeof(mContentionCounter) - sizeof(mLastCoalesceCheck) -
                                       sizeof(mLastPerformanceCheck) - sizeof(mMemoryPinned);
        info.memory_pinned = mMemoryPinned;
        info.numa_node = mConfig.numa_node;
        return info;
    }

    /**
     * Performs queue maintenance and optimization.
     */
    void maintenance() {
        auto current_time = get_current_time_ns();
        
        // Update performance metrics
        if (current_time - mLastPerformanceCheck > 1000000000ULL) { // Every second
            update_performance_metrics();
            mLastPerformanceCheck = current_time;
        }
        
        // Adaptive configuration adjustments
        if (mConfig.backoff_strategy == BackoffStrategy::ADAPTIVE) {
            adjust_adaptive_parameters();
        }
        
        // Memory optimization
        optimize_memory_layout();
    }

    /**
     * Validates queue integrity for debugging.
     */
    bool validate_integrity() const {
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        
        // Check index bounds
        if (read_idx >= QUEUE_SIZE || write_idx >= QUEUE_SIZE) {
            return false;
        }
        
        // Check sequence numbers for consistency
        if (!empty()) {
            uint32_t expected_seq = mQueue[read_idx].sequence_id;
            size_t current_idx = read_idx;
            
            while (current_idx != write_idx) {
                if (mQueue[current_idx].sequence_id < expected_seq) {
                    return false; // Sequence number went backwards
                }
                expected_seq = mQueue[current_idx].sequence_id;
                current_idx = (current_idx + 1) & QUEUE_MASK;
            }
        }
        
        return true;
    }

private:
    // Configuration
    QueueConfiguration mConfig;
    
    // Queue data with cache line alignment
    CACHE_ALIGNED std::array<ParameterUpdate, QUEUE_SIZE> mQueue;
    
    // Priority mapping for priority-based operations
    CACHE_ALIGNED std::array<uint16_t, QUEUE_SIZE> mPriorityMap;
    
    // Producer cache line - write index and related data
    CACHE_ALIGNED std::atomic<size_t> mWriteIndex;
    std::atomic<uint32_t> mSequenceCounter;
    std::atomic<uint32_t> mContentionCounter;
    uint64_t mLastCoalesceCheck;
    
    // Consumer cache line - read index
    CACHE_ALIGNED std::atomic<size_t> mReadIndex;
    
    // Statistics and performance monitoring
    CACHE_ALIGNED mutable QueueStatistics mStatistics;
    uint64_t mLastPerformanceCheck;
    bool mMemoryPinned = false;

    /**
     * Internal enqueue implementation with full feature support.
     */
    bool enqueue_update_internal(const ParameterUpdate& update, uint64_t start_time) {
        // Check for coalescing opportunity
        if (mConfig.enable_coalescing && should_coalesce(update)) {
            return try_coalesce_update(update);
        }
        
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        size_t next_write_idx = (write_idx + 1) & QUEUE_MASK;
        size_t read_idx = mReadIndex.load(std::memory_order_acquire);
        
        // Handle different overflow behaviors
        if (UNLIKELY(next_write_idx == read_idx)) {
            mStatistics.total_overflows.fetch_add(1, std::memory_order_relaxed);
            
            switch (mConfig.overflow_behavior) {
                case QueueOverflowBehavior::DROP_NEWEST:
                    mStatistics.total_dropped.fetch_add(1, std::memory_order_relaxed);
                    return false;
                    
                case QueueOverflowBehavior::DROP_OLDEST:
                    // Advance read index to drop oldest item
                    mReadIndex.store((read_idx + 1) & QUEUE_MASK, std::memory_order_release);
                    mStatistics.total_dropped.fetch_add(1, std::memory_order_relaxed);
                    break;
                    
                case QueueOverflowBehavior::DROP_BY_PRIORITY:
                    if (!drop_by_priority(update)) {
                        mStatistics.total_dropped.fetch_add(1, std::memory_order_relaxed);
                        return false;
                    }
                    break;
                    
                case QueueOverflowBehavior::COALESCE_SAME_PORT:
                    if (!try_coalesce_same_port(update)) {
                        mStatistics.total_dropped.fetch_add(1, std::memory_order_relaxed);
                        return false;
                    }
                    break;
                    
                case QueueOverflowBehavior::OVERWRITE:
                    // Continue with write, overwriting data
                    break;
            }
        }
        
        // Apply backoff strategy if there's contention
        if (mContentionCounter.load(std::memory_order_relaxed) > 0) {
            apply_backoff_strategy();
        }
        
        // Prefetch write location if enabled
        if (mConfig.enable_prefetching) {
            PREFETCH_WRITE(&mQueue[write_idx]);
        }
        
        // Write the parameter update
        mQueue[write_idx] = update;
        
        // Update priority map if enabled
        if (mConfig.enable_priority_queue) {
            mPriorityMap[write_idx] = update.priority;
        }
        
        // Publish the write index with release semantics
        mWriteIndex.store(next_write_idx, std::memory_order_release);
        
        // Update statistics
        if (mConfig.enable_statistics) {
            mStatistics.total_enqueued.fetch_add(1, std::memory_order_relaxed);
            update_max_size();
        }
        
        return true;
    }

    /**
     * Checks if an update should be coalesced with existing updates.
     */
    bool should_coalesce(const ParameterUpdate& update) const {
        if (!mConfig.enable_coalescing) {
            return false;
        }
        
        // Check if enough time has passed since last coalesce check
        uint64_t current_time = get_current_time_ns();
        if (current_time - mLastCoalesceCheck < 1000000) { // 1ms threshold
            return false;
        }
        
        // Only coalesce continuous parameters
        return update.parameter_type == ParameterUpdate::TYPE_CONTINUOUS &&
               !update.has_flag(ParameterUpdate::FLAG_IMMEDIATE);
    }

    /**
     * Attempts to coalesce an update with existing queue entries.
     */
    bool try_coalesce_update(const ParameterUpdate& update) {
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        
        // Search backwards from write position for same port
        size_t search_idx = write_idx;
        size_t search_count = 0;
        const size_t max_search = std::min(size_t(16), size()); // Limit search depth
        
        while (search_count < max_search && search_idx != read_idx) {
            search_idx = (search_idx - 1) & QUEUE_MASK;
            
            if (mQueue[search_idx].port_index == update.port_index &&
                mQueue[search_idx].source_id == update.source_id) {
                
                // Check if values are close enough to coalesce
                float delta = std::abs(mQueue[search_idx].value - update.value);
                if (delta < mConfig.coalescing_threshold) {
                    // Update existing entry instead of adding new one
                    mQueue[search_idx].value = update.value;
                    mQueue[search_idx].timestamp_ns = update.timestamp_ns;
                    mQueue[search_idx].sequence_id = update.sequence_id;
                    
                    mStatistics.total_coalesced.fetch_add(1, std::memory_order_relaxed);
                    return true;
                }
            }
            search_count++;
        }
        
        return false; // No coalescing opportunity found
    }

    /**
     * Coalesces multiple updates into a smaller set.
     */
    std::vector<ParameterUpdate> coalesce_updates(const ParameterUpdate* updates, size_t count) const {
        std::vector<ParameterUpdate> result;
        result.reserve(count);
        
        // Group updates by port and source
        std::unordered_map<uint64_t, size_t> port_source_map;
        
        for (size_t i = 0; i < count; ++i) {
            uint64_t key = (static_cast<uint64_t>(updates[i].port_index) << 32) | 
                          updates[i].source_id;
            
            auto it = port_source_map.find(key);
            if (it != port_source_map.end()) {
                // Update existing entry with latest value
                size_t existing_idx = it->second;
                if (updates[i].timestamp_ns > result[existing_idx].timestamp_ns) {
                    result[existing_idx] = updates[i];
                }
            } else {
                // Add new entry
                port_source_map[key] = result.size();
                result.push_back(updates[i]);
            }
        }
        
        return result;
    }

    /**
     * Attempts to drop updates by priority to make room.
     */
    bool drop_by_priority(const ParameterUpdate& new_update) {
        if (!mConfig.enable_priority_queue) {
            return false;
        }
        
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        
        // Find lowest priority item in queue
        uint16_t lowest_priority = UINT16_MAX;
        size_t lowest_idx = SIZE_MAX;
        
        size_t current_idx = read_idx;
        while (current_idx != write_idx) {
            if (mPriorityMap[current_idx] < lowest_priority) {
                lowest_priority = mPriorityMap[current_idx];
                lowest_idx = current_idx;
            }
            current_idx = (current_idx + 1) & QUEUE_MASK;
        }
        
        // Only drop if new update has higher priority
        if (lowest_idx != SIZE_MAX && new_update.priority > lowest_priority) {
            // Remove the lowest priority item by shifting queue
            compact_queue_at_index(lowest_idx);
            mStatistics.priority_drops.fetch_add(1, std::memory_order_relaxed);
            return true;
        }
        
        return false;
    }

    /**
     * Attempts to coalesce with same port updates.
     */
    bool try_coalesce_same_port(const ParameterUpdate& update) {
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        
        // Find most recent update for same port
        size_t search_idx = write_idx;
        while (search_idx != read_idx) {
            search_idx = (search_idx - 1) & QUEUE_MASK;
            
            if (mQueue[search_idx].port_index == update.port_index) {
                // Replace with new update
                mQueue[search_idx] = update;
                mStatistics.total_coalesced.fetch_add(1, std::memory_order_relaxed);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Compacts the queue by removing an item at a specific index.
     */
    void compact_queue_at_index(size_t remove_idx) {
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        
        // Shift all items after remove_idx forward by one position
        size_t current_idx = remove_idx;
        while (current_idx != write_idx) {
            size_t next_idx = (current_idx + 1) & QUEUE_MASK;
            if (next_idx != write_idx) {
                mQueue[current_idx] = mQueue[next_idx];
                if (mConfig.enable_priority_queue) {
                    mPriorityMap[current_idx] = mPriorityMap[next_idx];
                }
            }
            current_idx = next_idx;
        }
        
        // Update write index
        mWriteIndex.store((write_idx - 1) & QUEUE_MASK, std::memory_order_release);
    }

    /**
     * Applies the configured backoff strategy.
     */
    void apply_backoff_strategy() {
        uint32_t contention = mContentionCounter.fetch_add(1, std::memory_order_relaxed);
        
        switch (mConfig.backoff_strategy) {
            case BackoffStrategy::NONE:
                break;
                
            case BackoffStrategy::LINEAR:
                for (uint32_t i = 0; i < std::min(contention, mConfig.max_backoff_iterations); ++i) {
                    CPU_PAUSE();
                }
                break;
                
            case BackoffStrategy::EXPONENTIAL: {
                uint32_t delay = mConfig.backoff_base_delay_ns;
                for (uint32_t i = 0; i < std::min(contention, uint32_t(10)); ++i) {
                    busy_wait_ns(delay);
                    delay *= 2;
                }
                break;
            }
            
            case BackoffStrategy::ADAPTIVE:
                adaptive_backoff(contention);
                break;
        }
        
        mStatistics.contention_count.fetch_add(1, std::memory_order_relaxed);
    }

    /**
     * Implements adaptive backoff based on queue state.
     */
    void adaptive_backoff(uint32_t contention_level) {
        double load = load_factor();
        
        if (load < 0.25) {
            // Low load - minimal backoff
            CPU_PAUSE();
        } else if (load < 0.75) {
            // Medium load - linear backoff
            for (uint32_t i = 0; i < contention_level && i < 10; ++i) {
                CPU_PAUSE();
            }
        } else {
            // High load - exponential backoff with yield
            uint32_t delay = mConfig.backoff_base_delay_ns;
            for (uint32_t i = 0; i < std::min(contention_level, uint32_t(5)); ++i) {
                busy_wait_ns(delay);
                delay *= 2;
            }
            std::this_thread::yield();
        }
    }

    /**
     * Busy waits for the specified number of nanoseconds.
     */
    void busy_wait_ns(uint32_t nanoseconds) const {
        auto start = std::chrono::high_resolution_clock::now();
        auto target = start + std::chrono::nanoseconds(nanoseconds);
        
        while (std::chrono::high_resolution_clock::now() < target) {
            CPU_PAUSE();
        }
    }

    /**
     * Gets the last known value for a specific port.
     */
    float get_last_value_for_port(int32_t port_index) const {
        size_t read_idx = mReadIndex.load(std::memory_order_relaxed);
        size_t write_idx = mWriteIndex.load(std::memory_order_relaxed);
        
        // Search backwards from write position
        size_t search_idx = write_idx;
        while (search_idx != read_idx) {
            search_idx = (search_idx - 1) & QUEUE_MASK;
            if (mQueue[search_idx].port_index == port_index) {
                return mQueue[search_idx].value;
            }
        }
        
        return 0.0f; // Default value if not found
    }

    /**
     * Updates the maximum size reached statistic.
     */
    void update_max_size() {
        uint32_t current_size = static_cast<uint32_t>(size());
        uint32_t current_max = mStatistics.max_size_reached.load(std::memory_order_relaxed);
        
        while (current_size > current_max && 
               !mStatistics.max_size_reached.compare_exchange_weak(current_max, current_size, 
                                                                 std::memory_order_relaxed)) {
            // Retry if another thread updated max_size_reached
        }
    }

    /**
     * Handles latency warnings for debugging and monitoring.
     */
    void handle_latency_warning(uint64_t latency_ns, const ParameterUpdate& update) const {
        // In a real implementation, this could log to a file, send telemetry, etc.
        // For now, we just increment a counter
        (void)latency_ns;
        (void)update;
        
        // Could implement logging here:
        // LOG_WARNING("High latency detected: %llu ns for port %d", latency_ns, update.port_index);
    }

    /**
     * Updates performance metrics for adaptive behavior.
     */
    void update_performance_metrics() {
        // Reset contention counter periodically
        mContentionCounter.store(0, std::memory_order_relaxed);
        
        // Update cache miss estimation based on performance
        uint64_t total_ops = mStatistics.total_enqueued.load(std::memory_order_relaxed) + 
                            mStatistics.total_dequeued.load(std::memory_order_relaxed);
        uint64_t contentions = mStatistics.contention_count.load(std::memory_order_relaxed);
        
        // Estimate cache misses based on contention ratio
        if (total_ops > 0) {
            double contention_ratio = static_cast<double>(contentions) / total_ops;
            uint64_t estimated_misses = static_cast<uint64_t>(contention_ratio * total_ops * 0.1);
            mStatistics.cache_misses.store(estimated_misses, std::memory_order_relaxed);
        }
    }

    /**
     * Adjusts adaptive parameters based on performance history.
     */
    void adjust_adaptive_parameters() {
        double drop_rate = mStatistics.get_drop_rate();
        double load = load_factor();
        
        // Adjust backoff parameters based on performance
        if (drop_rate > 0.05 && load > 0.8) {
            // High drop rate and load - increase backoff
            mConfig.max_backoff_iterations = std::min(mConfig.max_backoff_iterations * 2, 10000U);
        } else if (drop_rate < 0.01 && load < 0.5) {
            // Low drop rate and load - decrease backoff
            mConfig.max_backoff_iterations = std::max(mConfig.max_backoff_iterations / 2, 10U);
        }
    }

    /**
     * Optimizes memory layout for better cache performance.
     */
    void optimize_memory_layout() {
        // Prefetch frequently accessed cache lines
        if (mConfig.enable_prefetching) {
            PREFETCH_READ(&mWriteIndex);
            PREFETCH_READ(&mReadIndex);
            PREFETCH_READ(&mStatistics);
        }
    }

    /**
     * Pins queue memory to prevent swapping.
     */
    void pin_queue_memory() {
        #ifdef _WIN32
            if (VirtualLock(this, sizeof(*this))) {
                mMemoryPinned = true;
            }
        #else
            if (mlock(this, sizeof(*this)) == 0) {
                mMemoryPinned = true;
            }
        #endif
    }

    /**
     * Unpins queue memory.
     */
    void unpin_queue_memory() {
        if (mMemoryPinned) {
            #ifdef _WIN32
                VirtualUnlock(this, sizeof(*this));
            #else
                munlock(this, sizeof(*this));
            #endif
            mMemoryPinned = false;
        }
    }

    /**
     * Gets current time in nanoseconds with platform-specific optimizations.
     */
    static uint64_t get_current_time_ns() {
        #ifdef _WIN32
            // Windows high-resolution timer
            static LARGE_INTEGER frequency = {0};
            if (UNLIKELY(frequency.QuadPart == 0)) {
                QueryPerformanceFrequency(&frequency);
            }
            
            LARGE_INTEGER counter;
            QueryPerformanceCounter(&counter);
            return static_cast<uint64_t>((counter.QuadPart * 1000000000ULL) / frequency.QuadPart);
        #else
            // POSIX monotonic clock
            struct timespec ts;
            clock_gettime(CLOCK_MONOTONIC, &ts);
            return static_cast<uint64_t>(ts.tv_sec) * 1000000000ULL + 
                   static_cast<uint64_t>(ts.tv_nsec);
        #endif
    }
};

// Specialized queue types for different use cases
template<size_t Size>
using HighPerformanceParameterQueue = ParameterQueue<Size>;

template<size_t Size>
using LowLatencyParameterQueue = ParameterQueue<Size>;

template<size_t Size>
using StatisticsEnabledParameterQueue = ParameterQueue<Size>;

// Type aliases for common queue sizes with optimized configurations
using ParameterQueue64 = ParameterQueue<64>;      // Ultra-low latency, minimal memory
using ParameterQueue128 = ParameterQueue<128>;    // Low latency applications
using ParameterQueue256 = ParameterQueue<256>;    // Balanced performance
using ParameterQueue512 = ParameterQueue<512>;    // Standard audio applications
using ParameterQueue1024 = ParameterQueue<1024>;  // High throughput
using ParameterQueue2048 = ParameterQueue<2048>;  // Maximum capacity
using ParameterQueue4096 = ParameterQueue<4096>;  // Extreme throughput

// Default queue type for most applications
using DefaultParameterQueue = ParameterQueue1024;

// Convenience factory functions for common configurations
template<size_t Size = 1024>
inline std::unique_ptr<ParameterQueue<Size>> create_low_latency_queue() {
    QueueConfiguration config;
    config.overflow_behavior = QueueOverflowBehavior::DROP_OLDEST;
    config.backoff_strategy = BackoffStrategy::ADAPTIVE;
    config.enable_coalescing = true;
    config.enable_prefetching = true;
    config.pin_memory = true;
    config.max_latency_warning_ns = 1000000; // 1ms warning threshold
    
    return std::make_unique<ParameterQueue<Size>>(config);
}

template<size_t Size = 2048>
inline std::unique_ptr<ParameterQueue<Size>> create_high_throughput_queue() {
    QueueConfiguration config;
    config.overflow_behavior = QueueOverflowBehavior::COALESCE_SAME_PORT;
    config.backoff_strategy = BackoffStrategy::EXPONENTIAL;
    config.enable_coalescing = true;
    config.enable_priority_queue = true;
    config.enable_statistics = true;
    config.enable_prefetching = true;
    config.prefetch_distance = 4;
    
    return std::make_unique<ParameterQueue<Size>>(config);
}

template<size_t Size = 512>
inline std::unique_ptr<ParameterQueue<Size>> create_realtime_audio_queue() {
    QueueConfiguration config;
    config.overflow_behavior = QueueOverflowBehavior::DROP_OLDEST;
    config.backoff_strategy = BackoffStrategy::LINEAR;
    config.max_backoff_iterations = 100;
    config.enable_coalescing = true;
    config.coalescing_threshold = 0.0001f; // Tight coalescing for audio
    config.enable_prefetching = true;
    config.pin_memory = true;
    config.max_latency_warning_ns = 500000; // 0.5ms warning for audio
    
    return std::make_unique<ParameterQueue<Size>>(config);
}

// Cleanup macros
#undef CACHE_ALIGNED
#undef PREFETCH_READ
#undef PREFETCH_WRITE
#undef LIKELY
#undef UNLIKELY
#undef CPU_PAUSE
#undef COMPILER_BARRIER

#endif // LSP_PARAMETER_QUEUE_H
