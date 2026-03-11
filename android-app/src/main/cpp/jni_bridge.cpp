#include <jni.h>
#include <memory>
#include <android/log.h>
#include <mutex>
#include <thread>
#include <chrono>
#include <vector>
#include <atomic>
#include <condition_variable>
#include <queue>
#include <functional>
#include <string>
#include <unordered_map>
#include <algorithm>
#include <cmath>
#include <future>
#include <random>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <cassert>
#include <cstring>
#include <array>
#include <deque>
#include <set>
#include <map>
#include <bitset>
#include <numeric>
#include <limits>

#include "AudioEngine.h"

#define LOG_TAG "AudioEngineJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace {

// Constants for audio processing
constexpr size_t MAX_CHANNELS = 32;
constexpr size_t MAX_BUFFER_SIZE = 8192;
constexpr size_t MIN_BUFFER_SIZE = 64;
constexpr int32_t MIN_SAMPLE_RATE = 8000;
constexpr int32_t MAX_SAMPLE_RATE = 192000;
constexpr float MAX_GAIN_DB = 20.0f;
constexpr float MIN_GAIN_DB = -60.0f;
constexpr size_t MAX_ERROR_QUEUE_SIZE = 100;
constexpr size_t BUFFER_POOL_SIZE = 32;
constexpr int MAX_RETRY_ATTEMPTS = 5;
constexpr int FADE_DURATION_MS = 500;
constexpr float CLIPPING_THRESHOLD = 0.95f;
constexpr float SILENCE_THRESHOLD = -80.0f;

// Audio engine state management with comprehensive thread safety
class AudioEngineState {
private:
    mutable std::recursive_mutex stateMutex;
    std::atomic<bool> initialized{false};
    std::atomic<bool> running{false};
    std::atomic<bool> shuttingDown{false};
    std::atomic<bool> suspended{false};
    std::atomic<bool> errorState{false};
    std::chrono::steady_clock::time_point lastStateChange;
    
public:
    bool isInitialized() const { return initialized.load(); }
    bool isRunning() const { return running.load(); }
    bool isShuttingDown() const { return shuttingDown.load(); }
    bool isSuspended() const { return suspended.load(); }
    bool hasError() const { return errorState.load(); }
    
    void setInitialized(bool state) {
        std::lock_guard<std::recursive_mutex> lock(stateMutex);
        initialized = state;
        lastStateChange = std::chrono::steady_clock::now();
    }
    
    void setRunning(bool state) {
        std::lock_guard<std::recursive_mutex> lock(stateMutex);
        running = state;
        lastStateChange = std::chrono::steady_clock::now();
    }
    
    void setShuttingDown(bool state) {
        std::lock_guard<std::recursive_mutex> lock(stateMutex);
        shuttingDown = state;
        lastStateChange = std::chrono::steady_clock::now();
    }
    
    void setSuspended(bool state) {
        std::lock_guard<std::recursive_mutex> lock(stateMutex);
        suspended = state;
        lastStateChange = std::chrono::steady_clock::now();
    }
    
    void setError(bool state) {
        std::lock_guard<std::recursive_mutex> lock(stateMutex);
        errorState = state;
        lastStateChange = std::chrono::steady_clock::now();
    }
    
    std::chrono::steady_clock::time_point getLastStateChange() const {
        std::lock_guard<std::recursive_mutex> lock(stateMutex);
        return lastStateChange;
    }
    
    bool canStart() const {
        return initialized.load() && !running.load() && !shuttingDown.load() && !errorState.load();
    }
    
    bool canStop() const {
        return running.load() && !shuttingDown.load();
    }
};

// Global audio engine and state
std::unique_ptr<AudioEngine> gEngine;
std::recursive_mutex gEngineMutex;
AudioEngineState gEngineState;

// Advanced performance monitoring with detailed metrics
struct DetailedPerformanceMetrics {
    std::atomic<uint64_t> totalFramesProcessed{0};
    std::atomic<uint64_t> totalSamplesProcessed{0};
    std::atomic<uint32_t> underruns{0};
    std::atomic<uint32_t> overruns{0};
    std::atomic<uint32_t> glitches{0};
    std::atomic<uint32_t> dropouts{0};
    std::atomic<float> cpuLoad{0.0f};
    std::atomic<float> averageLatency{0.0f};
    std::atomic<float> peakLatency{0.0f};
    std::atomic<float> jitter{0.0f};
    std::atomic<float> thd{0.0f}; // Total Harmonic Distortion
    std::atomic<float> snr{0.0f}; // Signal to Noise Ratio
    std::atomic<float> dynamicRange{0.0f};
    std::atomic<float> peakLevel{0.0f};
    std::atomic<float> rmsLevel{0.0f};
    std::atomic<uint32_t> clippingEvents{0};
    std::atomic<uint64_t> totalCallbacks{0};
    std::atomic<uint64_t> missedCallbacks{0};
    std::chrono::steady_clock::time_point startTime;
    std::chrono::steady_clock::time_point lastUpdate;
    
    // Circular buffers for historical data
    std::array<float, 1000> latencyHistory{};
    std::array<float, 1000> cpuHistory{};
    std::array<float, 1000> levelHistory{};
    std::atomic<size_t> historyIndex{0};
    
    void updateHistory(float latency, float cpu, float level) {
        size_t idx = historyIndex.fetch_add(1) % latencyHistory.size();
        latencyHistory[idx] = latency;
        cpuHistory[idx] = cpu;
        levelHistory[idx] = level;
    }
    
    float getAverageLatency() const {
        float sum = 0.0f;
        for (const auto& val : latencyHistory) {
            sum += val;
        }
        return sum / latencyHistory.size();
    }
    
    float getAverageCpuLoad() const {
        float sum = 0.0f;
        for (const auto& val : cpuHistory) {
            sum += val;
        }
        return sum / cpuHistory.size();
    }
    
    void reset() {
        totalFramesProcessed = 0;
        totalSamplesProcessed = 0;
        underruns = 0;
        overruns = 0;
        glitches = 0;
        dropouts = 0;
        cpuLoad = 0.0f;
        averageLatency = 0.0f;
        peakLatency = 0.0f;
        jitter = 0.0f;
        thd = 0.0f;
        snr = 0.0f;
        dynamicRange = 0.0f;
        peakLevel = 0.0f;
        rmsLevel = 0.0f;
        clippingEvents = 0;
        totalCallbacks = 0;
        missedCallbacks = 0;
        historyIndex = 0;
        latencyHistory.fill(0.0f);
        cpuHistory.fill(0.0f);
        levelHistory.fill(0.0f);
        startTime = std::chrono::steady_clock::now();
        lastUpdate = startTime;
    }
};

DetailedPerformanceMetrics gMetrics;

// Advanced audio session management with priority handling
struct AudioSession {
    int32_t sessionId;
    std::string packageName;
    std::string applicationName;
    int32_t uid;
    int32_t pid;
    bool isActive;
    bool isPaused;
    float volume;
    float pan;
    int32_t priority;
    AudioEngine::AudioUsage usage;
    AudioEngine::AudioContentType contentType;
    std::chrono::steady_clock::time_point creationTime;
    std::chrono::steady_clock::time_point lastActivity;
    std::chrono::milliseconds totalActiveTime{0};
    uint64_t framesProcessed{0};
    std::vector<float> channelVolumes;
    bool ducked{false};
    float duckingFactor{1.0f};
    
    AudioSession() : channelVolumes(2, 1.0f) {}
    
    void updateActivity() {
        lastActivity = std::chrono::steady_clock::now();
    }
    
    bool isExpired(std::chrono::milliseconds timeout) const {
        auto now = std::chrono::steady_clock::now();
        return std::chrono::duration_cast<std::chrono::milliseconds>(now - lastActivity) > timeout;
    }
    
    void setChannelCount(size_t channels) {
        channelVolumes.resize(channels, 1.0f);
    }
};

std::unordered_map<int32_t, std::unique_ptr<AudioSession>> gActiveSessions;
std::mutex gSessionMutex;
std::atomic<int32_t> gNextSessionId{1000};

// Comprehensive EQ system with multiple filter types
enum class FilterType {
    PEAKING,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
    BAND_PASS,
    NOTCH,
    ALL_PASS
};

struct EQBand {
    float frequency;
    float gain;
    float q;
    FilterType type;
    bool enabled;
    
    EQBand(float freq = 1000.0f, float g = 0.0f, float quality = 0.707f, FilterType t = FilterType::PEAKING)
        : frequency(freq), gain(g), q(quality), type(t), enabled(true) {}
};

struct EQPreset {
    std::string name;
    std::string description;
    std::vector<EQBand> bands;
    float preGain;
    float postGain;
    bool enabled;
    std::string category;
    int32_t version;
    
    EQPreset() : preGain(0.0f), postGain(0.0f), enabled(true), version(1) {}
};

// Predefined EQ presets with professional configurations
std::unordered_map<std::string, EQPreset> gEQPresets = {
    {"flat", {
        "Flat Response",
        "No equalization applied - natural frequency response",
            {
            {31.25f, 0.0f, 0.707f, FilterType::PEAKING},
            {62.5f, 0.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {250.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {500.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, 0.0f, 0.707f, FilterType::PEAKING}
        },
        0.0f, 0.0f, true, "Reference", 1
    }},
    {"rock", {
        "Rock",
        "Enhanced bass and treble for rock music",
            {
            {31.25f, 4.0f, 0.707f, FilterType::LOW_SHELF},
            {62.5f, 3.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 1.0f, 0.707f, FilterType::PEAKING},
            {250.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {500.0f, -2.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 2.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, 4.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, 5.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, 5.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -2.0f, 0.0f, true, "Music", 1
    }},
    {"pop", {
        "Pop",
        "Optimized for modern pop music with vocal clarity",
            {
            {31.25f, -1.0f, 0.707f, FilterType::LOW_SHELF},
            {62.5f, 1.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 3.0f, 0.707f, FilterType::PEAKING},
            {250.0f, 4.0f, 0.707f, FilterType::PEAKING},
            {500.0f, 3.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, 1.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, -1.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -1.0f, 0.0f, true, "Music", 1
    }},
    {"jazz", {
        "Jazz",
        "Warm sound with enhanced midrange for jazz instruments",
        {
            {31.25f, 2.0f, 0.707f, FilterType::LOW_SHELF},
            {62.5f, 1.5f, 0.707f, FilterType::PEAKING},
            {125.0f, 0.5f, 0.707f, FilterType::PEAKING},
            {250.0f, 1.0f, 0.707f, FilterType::PEAKING},
            {500.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, -0.5f, 0.707f, FilterType::PEAKING},
            {2000.0f, 0.5f, 0.707f, FilterType::PEAKING},
            {4000.0f, 1.5f, 0.707f, FilterType::PEAKING},
            {8000.0f, 2.5f, 0.707f, FilterType::PEAKING},
            {16000.0f, 3.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -1.5f, 0.0f, true, "Music", 1
    }},
    {"classical", {
        "Classical",
        "Natural sound reproduction for orchestral music",
        {
            {31.25f, 3.0f, 0.707f, FilterType::LOW_SHELF},
            {62.5f, 2.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {250.0f, -0.5f, 0.707f, FilterType::PEAKING},
            {500.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 1.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, 2.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, 3.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, 4.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -2.0f, 0.0f, true, "Music", 1
    }},
    {"bass_boost", {
        "Bass Boost",
        "Enhanced low frequency response",
        {
            {31.25f, 8.0f, 0.707f, FilterType::LOW_SHELF},
            {62.5f, 6.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 4.0f, 0.707f, FilterType::PEAKING},
            {250.0f, 2.0f, 0.707f, FilterType::PEAKING},
            {500.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, -2.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, -2.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, -2.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, -2.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -4.0f, 0.0f, true, "Enhancement", 1
    }},
    {"vocal", {
        "Vocal Enhancement",
        "Optimized for speech and vocal clarity",
        {
            {31.25f, -3.0f, 0.707f, FilterType::HIGH_PASS},
            {62.5f, -2.0f, 0.707f, FilterType::PEAKING},
            {125.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {250.0f, 1.0f, 0.707f, FilterType::PEAKING},
            {500.0f, 3.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, 4.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 4.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, 2.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, -1.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -1.0f, 0.0f, true, "Speech", 1
    }},
    {"electronic", {
        "Electronic",
        "Enhanced for electronic music with tight bass",
        {
            {31.25f, 5.0f, 1.414f, FilterType::PEAKING},
            {62.5f, 3.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 1.0f, 0.707f, FilterType::PEAKING},
            {250.0f, -1.0f, 0.707f, FilterType::PEAKING},
            {500.0f, -2.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 2.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, 4.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, 6.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, 4.0f, 0.707f, FilterType::HIGH_SHELF}
        },
        -3.0f, 0.0f, true, "Music", 1
    }}
};
        
// Advanced audio processing parameters with comprehensive controls
struct ProcessingParameters {
    // Equalizer
    std::vector<EQBand> eqBands;
    float eqPreGain;
    float eqPostGain;
    bool eqEnabled;
    bool eqBypass;
    
    // Multi-band compressor
    struct CompressorBand {
        float lowFreq;
        float highFreq;
        float threshold;
        float ratio;
        float attack;
        float release;
        float knee;
        float makeupGain;
        bool enabled;
        bool autoMakeup;
        
        CompressorBand() : lowFreq(20.0f), highFreq(20000.0f), threshold(-12.0f), 
                          ratio(4.0f), attack(5.0f), release(50.0f), knee(2.0f), 
                          makeupGain(0.0f), enabled(false), autoMakeup(true) {}
    };
    
    std::vector<CompressorBand> compressorBands;
    bool compressorEnabled;
    bool compressorBypass;
    
    // Limiter
    bool limiterEnabled;
    float limiterThreshold;
    float limiterRelease;
    float limiterLookahead;
    bool limiterBypass;
    
    // Reverb
    bool reverbEnabled;
    float reverbRoomSize;
    float reverbDamping;
    float reverbWetLevel;
    float reverbDryLevel;
    float reverbWidth;
    float reverbPreDelay;
    float reverbDecayTime;
    bool reverbBypass;
    
    // Stereo enhancement
    bool stereoEnhancementEnabled;
    float stereoWidth;
    float bassMonoFreq;
    float stereoBalance;
    bool stereoBypass;
    
    // Noise gate
    bool noiseGateEnabled;
    float noiseGateThreshold;
    float noiseGateRatio;
    float noiseGateAttack;
    float noiseGateRelease;
    float noiseGateHold;
    bool noiseGateBypass;
    
    // De-esser
    bool deEsserEnabled;
    float deEsserThreshold;
    float deEsserRatio;
    float deEsserFrequency;
    float deEsserBandwidth;
    bool deEsserBypass;
    
    // Exciter/Enhancer
    bool enhancerEnabled;
    float enhancerAmount;
    float enhancerFrequency;
    float enhancerHarmonics;
    bool enhancerBypass;
    
    // Master section
    float masterVolume;
    float masterPan;
    bool masterMute;
    bool masterBypass;
    
    ProcessingParameters() {
        // Initialize EQ with 10 bands
        eqBands = {
            {31.25f, 0.0f, 0.707f, FilterType::PEAKING},
            {62.5f, 0.0f, 0.707f, FilterType::PEAKING},
            {125.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {250.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {500.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {1000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {2000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {4000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {8000.0f, 0.0f, 0.707f, FilterType::PEAKING},
            {16000.0f, 0.0f, 0.707f, FilterType::PEAKING}
        };
        eqPreGain = 0.0f;
        eqPostGain = 0.0f;
        eqEnabled = true;
        eqBypass = false;
        
        // Initialize 4-band compressor
        compressorBands = {
            {20.0f, 250.0f, -15.0f, 3.0f, 10.0f, 100.0f, 2.0f, 0.0f, false, true},
            {250.0f, 2000.0f, -12.0f, 4.0f, 5.0f, 50.0f, 2.0f, 0.0f, false, true},
            {2000.0f, 8000.0f, -10.0f, 6.0f, 3.0f, 30.0f, 2.0f, 0.0f, false, true},
            {8000.0f, 20000.0f, -8.0f, 8.0f, 1.0f, 20.0f, 2.0f, 0.0f, false, true}
        };
        compressorEnabled = false;
        compressorBypass = false;
        
        limiterEnabled = true;
        limiterThreshold = -1.0f;
        limiterRelease = 5.0f;
        limiterLookahead = 2.0f;
        limiterBypass = false;
        
        reverbEnabled = false;
        reverbRoomSize = 0.5f;
        reverbDamping = 0.5f;
        reverbWetLevel = 0.3f;
        reverbDryLevel = 0.7f;
        reverbWidth = 1.0f;
        reverbPreDelay = 20.0f;
        reverbDecayTime = 1.5f;
        reverbBypass = false;
        
        stereoEnhancementEnabled = false;
        stereoWidth = 1.0f;
        bassMonoFreq = 120.0f;
        stereoBalance = 0.0f;
        stereoBypass = false;
        
        noiseGateEnabled = false;
        noiseGateThreshold = -40.0f;
        noiseGateRatio = 10.0f;
        noiseGateAttack = 1.0f;
        noiseGateRelease = 100.0f;
        noiseGateHold = 10.0f;
        noiseGateBypass = false;
        
        deEsserEnabled = false;
        deEsserThreshold = -15.0f;
        deEsserRatio = 3.0f;
        deEsserFrequency = 6000.0f;
        deEsserBandwidth = 2000.0f;
        deEsserBypass = false;
        
        enhancerEnabled = false;
        enhancerAmount = 0.0f;
        enhancerFrequency = 3000.0f;
        enhancerHarmonics = 2.0f;
        enhancerBypass = false;
        
        masterVolume = 1.0f;
        masterPan = 0.0f;
        masterMute = false;
        masterBypass = false;
    }
    
    void reset() {
        *this = ProcessingParameters();
    }
    
    bool validate() const {
        // Validate EQ parameters
        for (const auto& band : eqBands) {
            if (band.frequency < 10.0f || band.frequency > 24000.0f) return false;
            if (band.gain < MIN_GAIN_DB || band.gain > MAX_GAIN_DB) return false;
            if (band.q < 0.1f || band.q > 30.0f) return false;
        }
        
        // Validate compressor parameters
        for (const auto& band : compressorBands) {
            if (band.threshold < -60.0f || band.threshold > 0.0f) return false;
            if (band.ratio < 1.0f || band.ratio > 20.0f) return false;
            if (band.attack < 0.1f || band.attack > 1000.0f) return false;
            if (band.release < 1.0f || band.release > 10000.0f) return false;
        }
        
        // Validate other parameters
        if (masterVolume < 0.0f || masterVolume > 2.0f) return false;
        if (masterPan < -1.0f || masterPan > 1.0f) return false;
        
        return true;
    }
};

ProcessingParameters gProcessingParams;
std::recursive_mutex gProcessingMutex;

// Advanced audio buffer management with memory pooling
class AdvancedAudioBufferPool {
private:
    struct BufferInfo {
        std::vector<float> buffer;
        std::chrono::steady_clock::time_point lastUsed;
        bool inUse;
        size_t useCount;
        
        BufferInfo(size_t size) : buffer(size, 0.0f), inUse(false), useCount(0) {
            lastUsed = std::chrono::steady_clock::now();
        }
    };
    
    std::vector<std::unique_ptr<BufferInfo>> buffers;
    std::queue<size_t> availableIndices;
    std::mutex poolMutex;
    size_t bufferSize;
    size_t maxBuffers;
    size_t totalAllocated;
    size_t peakUsage;
    std::atomic<size_t> currentUsage{0};
    
    void cleanupUnusedBuffers() {
        auto now = std::chrono::steady_clock::now();
        const auto timeout = std::chrono::minutes(5);
        
        for (auto& buffer : buffers) {
            if (buffer && !buffer->inUse && 
                std::chrono::duration_cast<std::chrono::minutes>(now - buffer->lastUsed) > timeout) {
                buffer.reset();
            }
        }
    }
    
public:
    AdvancedAudioBufferPool(size_t size, size_t maxCount) 
        : bufferSize(size), maxBuffers(maxCount), totalAllocated(0), peakUsage(0) {
        buffers.reserve(maxBuffers);
        
        // Pre-allocate some buffers
        for (size_t i = 0; i < std::min(maxBuffers / 4, size_t(8)); ++i) {
            buffers.push_back(std::make_unique<BufferInfo>(bufferSize));
            availableIndices.push(i);
            totalAllocated++;
        }
    }
    
    std::pair<std::vector<float>*, size_t> acquireBuffer() {
        std::lock_guard<std::mutex> lock(poolMutex);
        
        size_t index;
        if (!availableIndices.empty()) {
            index = availableIndices.front();
            availableIndices.pop();
        } else if (totalAllocated < maxBuffers) {
            index = buffers.size();
            buffers.push_back(std::make_unique<BufferInfo>(bufferSize));
            totalAllocated++;
        } else {
            // Pool exhausted, return nullptr
            return {nullptr, SIZE_MAX};
        }
        
        auto& buffer = buffers[index];
        buffer->inUse = true;
        buffer->useCount++;
        buffer->lastUsed = std::chrono::steady_clock::now();
        
        currentUsage++;
        peakUsage = std::max(peakUsage, currentUsage.load());
        
        return {&buffer->buffer, index};
    }
    
    void releaseBuffer(size_t index) {
        std::lock_guard<std::mutex> lock(poolMutex);
        
        if (index >= buffers.size() || !buffers[index] || !buffers[index]->inUse) {
            return;
        }
        
        auto& buffer = buffers[index];
        buffer->inUse = false;
        buffer->lastUsed = std::chrono::steady_clock::now();
        
        // Clear buffer data
        std::fill(buffer->buffer.begin(), buffer->buffer.end(), 0.0f);
        
        availableIndices.push(index);
        currentUsage--;
        
        // Periodic cleanup
        if (availableIndices.size() > maxBuffers / 2) {
            cleanupUnusedBuffers();
        }
    }
    
    size_t getCurrentUsage() const { return currentUsage.load(); }
    size_t getPeakUsage() const { return peakUsage; }
    size_t getTotalAllocated() const { return totalAllocated; }
    
    void reset() {
        std::lock_guard<std::mutex> lock(poolMutex);
        buffers.clear();
        while (!availableIndices.empty()) {
            availableIndices.pop();
        }
        totalAllocated = 0;
        peakUsage = 0;
        currentUsage = 0;
    }
};

std::unique_ptr<AdvancedAudioBufferPool> gBufferPool;

// Comprehensive error handling and recovery system
enum class AudioError {
    NONE = 0,
    INITIALIZATION_FAILED = 1,
    PERMISSION_DENIED = 2,
    DEVICE_UNAVAILABLE = 3,
    BUFFER_UNDERRUN = 4,
    BUFFER_OVERRUN = 5,
    PROCESSING_ERROR = 6,
    HARDWARE_ERROR = 7,
    TIMEOUT_ERROR = 8,
    MEMORY_ERROR = 9,
    CONFIGURATION_ERROR = 10,
    DRIVER_ERROR = 11,
    SYSTEM_ERROR = 12,
    NETWORK_ERROR = 13,
    CODEC_ERROR = 14,
    FORMAT_ERROR = 15,
    SYNCHRONIZATION_ERROR = 16,
    RESOURCE_EXHAUSTED = 17,
    INVALID_STATE = 18,
    OPERATION_CANCELLED = 19,
    UNKNOWN_ERROR = 999
};

struct ErrorInfo {
    AudioError error;
    std::string message;
    std::string details;
    std::chrono::steady_clock::time_point timestamp;
    int32_t errorCode;
    int32_t systemErrorCode;
    std::string stackTrace;
    std::string threadId;
    bool recoverable;
    int32_t severity; // 0=info, 1=warning, 2=error, 3=critical
    std::string component;
    
    ErrorInfo() : error(AudioError::NONE), errorCode(0), systemErrorCode(0), 
                  recoverable(true), severity(0) {
        timestamp = std::chrono::steady_clock::now();
        
        // Get thread ID
        std::ostringstream oss;
        oss << std::this_thread::get_id();
        threadId = oss.str();
    }
};

class ErrorManager {
private:
    std::queue<ErrorInfo> errorQueue;
    std::mutex errorMutex;
    std::atomic<size_t> totalErrors{0};
    std::atomic<size_t> criticalErrors{0};
    std::unordered_map<AudioError, size_t> errorCounts;
    std::chrono::steady_clock::time_point lastCleanup;
    
public:
    void logError(AudioError error, const std::string& message, 
                  const std::string& details = "", int32_t code = 0, 
                  int32_t systemCode = 0, bool recoverable = true, 
                  int32_t severity = 2, const std::string& component = "AudioEngine") {
        std::lock_guard<std::mutex> lock(errorMutex);
        
        ErrorInfo info;
        info.error = error;
        info.message = message;
        info.details = details;
        info.errorCode = code;
        info.systemErrorCode = systemCode;
        info.recoverable = recoverable;
        info.severity = severity;
        info.component = component;
        
        errorQueue.push(info);
        totalErrors++;
        
        if (severity >= 3) {
            criticalErrors++;
        }
        
        errorCounts[error]++;
        
        // Maintain queue size
        if (errorQueue.size() > MAX_ERROR_QUEUE_SIZE) {
            errorQueue.pop();
        }
        
        // Log to Android log
        const char* severityStr[] = {"INFO", "WARN", "ERROR", "CRITICAL"};
        int androidLogLevel[] = {ANDROID_LOG_INFO, ANDROID_LOG_WARN, ANDROID_LOG_ERROR, ANDROID_LOG_FATAL};
        
        __android_log_print(androidLogLevel[std::min(severity, 3)], LOG_TAG,
                           "[%s] %s Error [%d]: %s - %s (Code: %d, System: %d, Thread: %s)",
                           severityStr[std::min(severity, 3)], component.c_str(),
                           static_cast<int>(error), message.c_str(), details.c_str(),
                           code, systemCode, info.threadId.c_str());
    }
    
    std::vector<ErrorInfo> getRecentErrors(size_t maxCount = 50) {
        std::lock_guard<std::mutex> lock(errorMutex);
        
        std::vector<ErrorInfo> errors;
        auto tempQueue = errorQueue;
        
        while (!tempQueue.empty() && errors.size() < maxCount) {
            errors.push_back(tempQueue.front());
            tempQueue.pop();
        }
        
        std::reverse(errors.begin(), errors.end());
        return errors;
    }
    
    void clearErrors() {
        std::lock_guard<std::mutex> lock(errorMutex);
        while (!errorQueue.empty()) {
            errorQueue.pop();
        }
        errorCounts.clear();
    }
    
    size_t getTotalErrors() const { return totalErrors.load(); }
    size_t getCriticalErrors() const { return criticalErrors.load(); }
    
    size_t getErrorCount(AudioError error) const {
        std::lock_guard<std::mutex> lock(errorMutex);
        auto it = errorCounts.find(error);
        return it != errorCounts.end() ? it->second : 0;
    }
    
    bool hasRecentCriticalErrors(std::chrono::milliseconds timeWindow = std::chrono::minutes(5)) const {
        std::lock_guard<std::mutex> lock(errorMutex);
        
        auto now = std::chrono::steady_clock::now();
        auto tempQueue = errorQueue;
        
        while (!tempQueue.empty()) {
            const auto& error = tempQueue.front();
            if (error.severity >= 3 && 
                std::chrono::duration_cast<std::chrono::milliseconds>(now - error.timestamp) < timeWindow) {
                return true;
            }
            tempQueue.pop();
        }
        
        return false;
    }
};

ErrorManager gErrorManager;

// Advanced audio device management with hot-plug support
struct AudioDeviceInfo {
    int32_t deviceId;
    std::string name;
    std::string address;
    std::string manufacturer;
    std::string productName;
    int32_t type;
    int32_t sampleRate;
    int32_t channelCount;
    std::vector<int32_t> supportedSampleRates;
    std::vector<int32_t> supportedChannelCounts;
    std::vector<AudioEngine::AudioFormat> supportedFormats;
    bool isInput;
    bool isOutput;
    bool isDefault;
    bool isConnected;
    bool isWireless;
    bool supportsLowLatency;
    bool supportsProAudio;
    float latency;
    int32_t bufferSize;
    std::chrono::steady_clock::time_point lastSeen;
    std::string driverVersion;
    std::map<std::string, std::string> properties;
    
    AudioDeviceInfo() : deviceId(-1), type(0), sampleRate(0), channelCount(0),
                       isInput(false), isOutput(false), isDefault(false), 
                       isConnected(false), isWireless(false), 
                       supportsLowLatency(false), supportsProAudio(false),
                       latency(0.0f), bufferSize(0) {
        lastSeen = std::chrono::steady_clock::now();
    }
    
    bool isValid() const {
        return deviceId >= 0 && !name.empty() && channelCount > 0 && sampleRate > 0;
    }
    
    bool supportsConfiguration(int32_t sampleRate, int32_t channels) const {
        bool sampleRateSupported = supportedSampleRates.empty() || 
            std::find(supportedSampleRates.begin(), supportedSampleRates.end(), sampleRate) != supportedSampleRates.end();
        
        bool channelsSupported = supportedChannelCounts.empty() ||
            std::find(supportedChannelCounts.begin(), supportedChannelCounts.end(), channels) != supportedChannelCounts.end();
        
        return sampleRateSupported && channelsSupported;
    }
};

class AudioDeviceManager {
private:
    std::vector<AudioDeviceInfo> availableDevices;
    std::mutex deviceMutex;
    AudioDeviceInfo currentInputDevice;
    AudioDeviceInfo currentOutputDevice;
    std::function<void(const AudioDeviceInfo&, bool)> deviceChangeCallback;
    std::thread deviceMonitorThread;
    std::atomic<bool> monitoringActive{false};
    
    void monitorDevices() {
        while (monitoringActive.load()) {
    try {
                updateDeviceList();
                std::this_thread::sleep_for(std::chrono::seconds(2));
    } catch (const std::exception& e) {
                gErrorManager.logError(AudioError::SYSTEM_ERROR, 
                                     "Device monitoring error", e.what(), 0, 0, true, 1, "DeviceManager");
    }
}
    }

    void updateDeviceList() {
        if (!gEngine) return;
        
        std::lock_guard<std::mutex> lock(deviceMutex);
    try {
            auto newDevices = gEngine->getAvailableAudioDevices();
            std::vector<AudioDeviceInfo> updatedDevices;
            
            for (const auto& engineDevice : newDevices) {
                AudioDeviceInfo info;
                info.deviceId = engineDevice.id;
                info.name = engineDevice.name;
                info.address = engineDevice.address;
                info.manufacturer = engineDevice.manufacturer;
                info.productName = engineDevice.productName;
                info.type = engineDevice.type;
                info.sampleRate = engineDevice.sampleRate;
                info.channelCount = engineDevice.channelCount;
                info.supportedSampleRates = engineDevice.supportedSampleRates;
                info.supportedChannelCounts = engineDevice.supportedChannelCounts;
                info.supportedFormats = engineDevice.supportedFormats;
                info.isInput = engineDevice.isInput;
                info.isOutput = engineDevice.isOutput;
                info.isDefault = engineDevice.isDefault;
                info.isConnected = engineDevice.isConnected;
                info.isWireless = engineDevice.isWireless;
                info.supportsLowLatency = engineDevice.supportsLowLatency;
                info.supportsProAudio = engineDevice.supportsProAudio;
                info.latency = engineDevice.latency;
                info.bufferSize = engineDevice.bufferSize;
                info.driverVersion = engineDevice.driverVersion;
                info.properties = engineDevice.properties;
                info.lastSeen = std::chrono::steady_clock::now();
                
                updatedDevices.push_back(info);
                
                // Check for new devices
                auto existingIt = std::find_if(availableDevices.begin(), availableDevices.end(),
                    [&info](const AudioDeviceInfo& existing) {
                        return existing.deviceId == info.deviceId;
                    });
                
                if (existingIt == availableDevices.end()) {
                    LOGI("New audio device detected: %s (ID: %d)", info.name.c_str(), info.deviceId);
                    if (deviceChangeCallback) {
                        deviceChangeCallback(info, true);
                    }
                }
                
                // Update default devices
                if (info.isDefault) {
                    if (info.isInput) {
                        currentInputDevice = info;
                    }
                    if (info.isOutput) {
                        currentOutputDevice = info;
                    }
                }
            }
            
            // Check for removed devices
            for (const auto& existing : availableDevices) {
                auto newIt = std::find_if(updatedDevices.begin(), updatedDevices.end(),
                    [&existing](const AudioDeviceInfo& newDevice) {
                        return newDevice.deviceId == existing.deviceId;
                    });
                
                if (newIt == updatedDevices.end()) {
                    LOGI("Audio device removed: %s (ID: %d)", existing.name.c_str(), existing.deviceId);
                    if (deviceChangeCallback) {
                        deviceChangeCallback(existing, false);
                    }
                }
            }
            
            availableDevices = std::move(updatedDevices);
    } catch (const std::exception& e) {
            gErrorManager.logError(AudioError::SYSTEM_ERROR, 
                                 "Failed to update device list", e.what(), 0, 0, true, 2, "DeviceManager");
    }
}

public:
    void startMonitoring() {
        if (!monitoringActive.load()) {
            monitoringActive = true;
            deviceMonitorThread = std::thread(&AudioDeviceManager::monitorDevices, this);
        }
    }
    
    void stopMonitoring() {
        if (monitoringActive.load()) {
            monitoringActive = false;
            if (deviceMonitorThread.joinable()) {
                deviceMonitorThread.join();
            }
        }
    }
    
    std::vector<AudioDeviceInfo> getAvailableDevices() {
        std::lock_guard<std::mutex> lock(deviceMutex);
        updateDeviceList();
        return availableDevices;
    }
    
    AudioDeviceInfo getCurrentInputDevice() const {
        std::lock_guard<std::mutex> lock(deviceMutex);
        return currentInputDevice;
    }
    
    AudioDeviceInfo getCurrentOutputDevice() const {
        std::lock_guard<std::mutex> lock(deviceMutex);
        return currentOutputDevice;
    }
    
    bool setInputDevice(int32_t deviceId) {
        std::lock_guard<std::mutex> lock(deviceMutex);
        
        auto it = std::find_if(availableDevices.begin(), availableDevices.end(),
            [deviceId](const AudioDeviceInfo& device) {
                return device.deviceId == deviceId && device.isInput;
            });
        
        if (it != availableDevices.end() && gEngine) {
            if (gEngine->setInputDevice(deviceId)) {
                currentInputDevice = *it;
                return true;
            }
        }
        
        return false;
    }
    
    bool setOutputDevice(int32_t deviceId) {
        std::lock_guard<std::mutex> lock(deviceMutex);
        
        auto it = std::find_if(availableDevices.begin(), availableDevices.end(),
            [deviceId](const AudioDeviceInfo& device) {
                return device.deviceId == deviceId && device.isOutput;
            });
        
        if (it != availableDevices.end() && gEngine) {
            if (gEngine->setOutputDevice(deviceId)) {
                currentOutputDevice = *it;
                return true;
            }
        }
        
        return false;
    }
    
    void setDeviceChangeCallback(std::function<void(const AudioDeviceInfo&, bool)> callback) {
        deviceChangeCallback = callback;
    }
    
    AudioDeviceInfo findBestDevice(bool isInput, int32_t preferredSampleRate = 48000, 
                                  int32_t preferredChannels = 2) {
        std::lock_guard<std::mutex> lock(deviceMutex);
        
        AudioDeviceInfo bestDevice;
        int bestScore = -1;
        
        for (const auto& device : availableDevices) {
            if ((isInput && !device.isInput) || (!isInput && !device.isOutput) || !device.isConnected) {
                continue;
            }
            
            int score = 0;
            
            // Prefer default devices
            if (device.isDefault) score += 100;
            
            // Prefer devices that support low latency
            if (device.supportsLowLatency) score += 50;
            
            // Prefer devices that support pro audio
            if (device.supportsProAudio) score += 30;
            
            // Prefer wired over wireless
            if (!device.isWireless) score += 20;
            
            // Score based on sample rate match
            if (device.supportsConfiguration(preferredSampleRate, preferredChannels)) {
                score += 40;
            }
            
            // Score based on channel count
            if (device.channelCount >= preferredChannels) {
                score += 10;
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestDevice = device;
            }
        }
        
        return bestDevice;
    }
};

AudioDeviceManager gDeviceManager;

// Configuration validation with comprehensive checks
class ConfigurationValidator {
public:
    struct ValidationResult {
        bool isValid;
        std::vector<std::string> errors;
        std::vector<std::string> warnings;
        AudioEngine::Config suggestedConfig;
        
        ValidationResult() : isValid(true) {}
    };
    
    static ValidationResult validateAudioConfig(const AudioEngine::Config& config) {
        ValidationResult result;
        
        // Sample rate validation
        if (config.sampleRate < MIN_SAMPLE_RATE || config.sampleRate > MAX_SAMPLE_RATE) {
            result.errors.push_back("Invalid sample rate: " + std::to_string(config.sampleRate) + 
                                   " (valid range: " + std::to_string(MIN_SAMPLE_RATE) + 
                                   "-" + std::to_string(MAX_SAMPLE_RATE) + ")");
            result.isValid = false;
        }
        
        // Common sample rates check
        std::vector<int32_t> commonRates = {8000, 11025, 16000, 22050, 44100, 48000, 88200, 96000, 176400, 192000};
        if (std::find(commonRates.begin(), commonRates.end(), config.sampleRate) == commonRates.end()) {
            result.warnings.push_back("Uncommon sample rate: " + std::to_string(config.sampleRate) + 
                                     ". Consider using 44100 or 48000 Hz for better compatibility.");
        }
        
        // Buffer size validation
        if (config.framesPerBuffer < MIN_BUFFER_SIZE || config.framesPerBuffer > MAX_BUFFER_SIZE) {
            result.errors.push_back("Invalid buffer size: " + std::to_string(config.framesPerBuffer) + 
                                   " (valid range: " + std::to_string(MIN_BUFFER_SIZE) + 
                                   "-" + std::to_string(MAX_BUFFER_SIZE) + ")");
            result.isValid = false;
        }
        
        // Power of 2 buffer size check
        if ((config.framesPerBuffer & (config.framesPerBuffer - 1)) != 0) {
            result.warnings.push_back("Buffer size is not a power of 2: " + std::to_string(config.framesPerBuffer) + 
                                     ". Power of 2 sizes may provide better performance.");
        }
        
        // Channel validation
        if (config.inputChannels < 0 || config.inputChannels > MAX_CHANNELS) {
            result.errors.push_back("Invalid input channels: " + std::to_string(config.inputChannels) + 
                                   " (valid range: 0-" + std::to_string(MAX_CHANNELS) + ")");
            result.isValid = false;
        }
        
        if (config.outputChannels < 1 || config.outputChannels > MAX_CHANNELS) {
            result.errors.push_back("Invalid output channels: " + std::to_string(config.outputChannels) + 
                                   " (valid range: 1-" + std::to_string(MAX_CHANNELS) + ")");
            result.isValid = false;
        }
        
        // Latency warnings
        float latencyMs = (static_cast<float>(config.framesPerBuffer) / config.sampleRate) * 1000.0f;
        if (latencyMs > 50.0f) {
            result.warnings.push_back("High latency configuration: " + std::to_string(latencyMs) + 
                                     "ms. Consider reducing buffer size for real-time applications.");
        }
        
        if (latencyMs < 5.0f && config.enableLowLatency) {
            result.warnings.push_back("Very low latency configuration: " + std::to_string(latencyMs) + 
                                     "ms. May cause audio dropouts on some devices.");
        }
        
        // Exclusive mode warnings
        if (config.enableExclusiveMode) {
            result.warnings.push_back("Exclusive mode enabled. This may prevent other applications from using audio.");
        }
        
        // Generate suggested configuration if current is invalid
        if (!result.isValid) {
            result.suggestedConfig = generateOptimalConfig();
        }
    return result;
}
    static AudioEngine::Config generateOptimalConfig() {
        AudioEngine::Config config;
        
        // Get device capabilities
        auto outputDevice = gDeviceManager.getCurrentOutputDevice();
        auto inputDevice = gDeviceManager.getCurrentInputDevice();
        
        // Set optimal sample rate
        if (outputDevice.isValid() && !outputDevice.supportedSampleRates.empty()) {
            // Prefer 48kHz, then 44.1kHz
            if (std::find(outputDevice.supportedSampleRates.begin(), outputDevice.supportedSampleRates.end(), 48000) 
                != outputDevice.supportedSampleRates.end()) {
                config.sampleRate = 48000;
            } else if (std::find(outputDevice.supportedSampleRates.begin(), outputDevice.supportedSampleRates.end(), 44100) 
                       != outputDevice.supportedSampleRates.end()) {
                config.sampleRate = 44100;
            } else {
                config.sampleRate = outputDevice.supportedSampleRates[0];
            }
        } else {
            config.sampleRate = 48000; // Default
        }
        
        // Set optimal buffer size based on device capabilities
        if (outputDevice.supportsLowLatency) {
            config.framesPerBuffer = 128;
            config.enableLowLatency = true;
        } else {
            config.framesPerBuffer = 256;
            config.enableLowLatency = false;
        }
        
        // Set channel configuration
        config.outputChannels = outputDevice.isValid() ? 
            std::min(outputDevice.channelCount, 2) : 2;
        config.inputChannels = inputDevice.isValid() ? 
            std::min(inputDevice.channelCount, 2) : 0;
        
        // Conservative settings for stability
        config.enableExclusiveMode = false;
        config.enableCallbackMode = true;
        config.priorityLevel = AudioEngine::Priority::HIGH;
        config.enableEqualizer = true;
        config.equalizerBands = 10;
        
        return config;
    }
};

// Advanced performance monitoring with real-time analysis
class PerformanceMonitor {
private:
    std::thread monitorThread;
    std::atomic<bool> monitoring{false};
    std::mutex metricsMutex;
    
    struct RealtimeMetrics {
        std::chrono::steady_clock::time_point lastCallback;
        std::chrono::microseconds callbackInterval{0};
        std::chrono::microseconds maxCallbackTime{0};
        std::chrono::microseconds avgCallbackTime{0};
        std::deque<std::chrono::microseconds> callbackTimes;
        float instantCpuLoad{0.0f};
        float avgCpuLoad{0.0f};
        std::deque<float> cpuHistory;
        size_t maxHistorySize{1000};
    };
    
    RealtimeMetrics realtimeMetrics;
    
    void monitorPerformance() {
        while (monitoring.load()) {
            try {
                updateRealtimeMetrics();
                analyzePerformance();
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
            } catch (const std::exception& e) {
                gErrorManager.logError(AudioError::SYSTEM_ERROR, 
                                     "Performance monitoring error", e.what(), 0, 0, true, 1, "PerformanceMonitor");
            }
        }
    }
    
    void updateRealtimeMetrics() {
        if (!gEngine || !gEngineState.isRunning()) return;
        
        std::lock_guard<std::mutex> lock(metricsMutex);
        
        try {
            auto stats = gEngine->getPerformanceStats();
            
            // Update basic metrics
            gMetrics.totalFramesProcessed = stats.totalFramesProcessed;
            gMetrics.totalSamplesProcessed = stats.totalFramesProcessed * 2; // Assume stereo
            gMetrics.underruns = stats.underruns;
            gMetrics.overruns = stats.overruns;
            gMetrics.cpuLoad = stats.cpuLoad;
            gMetrics.averageLatency = stats.averageLatency;
            
            // Update realtime metrics
            realtimeMetrics.instantCpuLoad = stats.cpuLoad;
            realtimeMetrics.cpuHistory.push_back(stats.cpuLoad);
            
            if (realtimeMetrics.cpuHistory.size() > realtimeMetrics.maxHistorySize) {
                realtimeMetrics.cpuHistory.pop_front();
            }
            
            // Calculate average CPU load
            float sum = std::accumulate(realtimeMetrics.cpuHistory.begin(), 
                                       realtimeMetrics.cpuHistory.end(), 0.0f);
            realtimeMetrics.avgCpuLoad = sum / realtimeMetrics.cpuHistory.size();
            
            // Update callback timing
            auto now = std::chrono::steady_clock::now();
            if (realtimeMetrics.lastCallback.time_since_epoch().count() > 0) {
                auto interval = std::chrono::duration_cast<std::chrono::microseconds>(
                    now - realtimeMetrics.lastCallback);
                realtimeMetrics.callbackInterval = interval;
                
                realtimeMetrics.callbackTimes.push_back(interval);
                if (realtimeMetrics.callbackTimes.size() > realtimeMetrics.maxHistorySize) {
                    realtimeMetrics.callbackTimes.pop_front();
                }
                
                // Calculate average callback time
                auto totalTime = std::accumulate(realtimeMetrics.callbackTimes.begin(),
                                               realtimeMetrics.callbackTimes.end(),
                                               std::chrono::microseconds(0));
                realtimeMetrics.avgCallbackTime = totalTime / realtimeMetrics.callbackTimes.size();
                
                // Update max callback time
                auto maxTime = *std::max_element(realtimeMetrics.callbackTimes.begin(),
                                               realtimeMetrics.callbackTimes.end());
                realtimeMetrics.maxCallbackTime = maxTime;
            }
            realtimeMetrics.lastCallback = now;
            
            // Update historical data
            gMetrics.updateHistory(stats.averageLatency, stats.cpuLoad, stats.peakLevel);
            gMetrics.lastUpdate = now;
            
        } catch (const std::exception& e) {
            gErrorManager.logError(AudioError::PROCESSING_ERROR, 
                                 "Failed to update performance metrics", e.what(), 0, 0, true, 1, "PerformanceMonitor");
        }
    }
    
    void analyzePerformance() {
        std::lock_guard<std::mutex> lock(metricsMutex);
        
        // Check for performance issues
        if (realtimeMetrics.avgCpuLoad > 0.85f) {
            gErrorManager.logError(AudioError::PROCESSING_ERROR, 
                                 "High CPU load detected", 
                                 "Average CPU load: " + std::to_string(realtimeMetrics.avgCpuLoad * 100.0f) + "%",
                                 0, 0, true, 2, "PerformanceMonitor");
        }
        
        if (gMetrics.underruns.load() > 0) {
            static uint32_t lastUnderruns = 0;
