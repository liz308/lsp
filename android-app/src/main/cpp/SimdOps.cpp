#include "SimdOps.h"
#include "CpuFeatures.h"
#include <android/log.h>
#include <math.h>
#include <time.h>
#include <string.h>

// Include SIMD intrinsics based on architecture
#if defined(__aarch64__) || defined(__ARM_NEON)
    #include <arm_neon.h>
    #define SIMD_NEON_AVAILABLE 1
#else
    #define SIMD_NEON_AVAILABLE 0
#endif

#if defined(__x86_64__) || defined(__SSE2__)
    #include <emmintrin.h>  // SSE2
    #include <xmmintrin.h>  // SSE
    #define SIMD_SSE2_AVAILABLE 1
    #if defined(__SSE4_1__)
        #include <smmintrin.h>  // SSE4.1
        #define SIMD_SSE4_1_AVAILABLE 1
    #else
        #define SIMD_SSE4_1_AVAILABLE 0
    #endif
#else
    #define SIMD_SSE2_AVAILABLE 0
    #define SIMD_SSE4_1_AVAILABLE 0
#endif

#define LOG_TAG "LSP-SimdOps"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static bool g_simd_initialized = false;
static bool g_simd_available = false;
static bool g_force_scalar = false;
static const char* g_implementation = "uninitialized";

// ============================================================================
// Initialization
// ============================================================================

bool simd_ops_init(void) {
    if (g_simd_initialized) {
        return true;
    }
    
    // Initialize CPU feature detection
    if (!cpu_features_init()) {
        LOGW("CPU feature detection failed, using scalar fallback");
        g_simd_available = false;
        g_implementation = "scalar (detection failed)";
        g_simd_initialized = true;
        return false;
    }
    
    // Check for SIMD support
    cpu_arch_t arch = cpu_features_get_arch();
    
    switch (arch) {
        case CPU_ARCH_ARM64_V8A:
            if (cpu_features_has_neon()) {
                g_simd_available = true;
                g_implementation = "ARM NEON (arm64-v8a)";
                LOGI("SIMD enabled: ARM NEON");
            } else {
                g_simd_available = false;
                g_implementation = "scalar (NEON unavailable)";
                LOGW("NEON not available on ARM64 (unexpected)");
            }
            break;
            
        case CPU_ARCH_ARMEABI_V7A:
            if (cpu_features_has_neon()) {
                g_simd_available = true;
                g_implementation = "ARM NEON (armeabi-v7a)";
                LOGI("SIMD enabled: ARM NEON");
            } else {
                g_simd_available = false;
                g_implementation = "scalar (NEON unavailable on ARMv7)";
                LOGW("NEON not available, using scalar fallback");
            }
            break;
            
        case CPU_ARCH_X86_64:
            if (cpu_features_has_sse2()) {
                g_simd_available = true;
                if (cpu_features_has_avx2()) {
                    g_implementation = "x86 AVX2";
                } else if (cpu_features_has_avx()) {
                    g_implementation = "x86 AVX";
                } else if (cpu_features_has_sse4_1()) {
                    g_implementation = "x86 SSE4.1";
                } else {
                    g_implementation = "x86 SSE2";
                }
                LOGI("SIMD enabled: %s", g_implementation);
            } else {
                g_simd_available = false;
                g_implementation = "scalar (SSE2 unavailable)";
                LOGW("SSE2 not available on x86_64 (unexpected)");
            }
            break;
            
        default:
            g_simd_available = false;
            g_implementation = "scalar (unknown architecture)";
            LOGW("Unknown architecture, using scalar fallback");
            break;
    }
    
    g_simd_initialized = true;
    return g_simd_available;
}

bool simd_ops_available(void) {
    if (!g_simd_initialized) {
        simd_ops_init();
    }
    return g_simd_available && !g_force_scalar;
}

void simd_ops_force_scalar(bool force_scalar) {
    g_force_scalar = force_scalar;
    if (force_scalar) {
        LOGI("SIMD operations forced to scalar mode");
    } else {
        LOGI("SIMD operations enabled (if available)");
    }
}

const char* simd_ops_get_implementation(void) {
    if (!g_simd_initialized) {
        simd_ops_init();
    }
    return g_force_scalar ? "scalar (forced)" : g_implementation;
}

// ============================================================================
// Scalar Reference Implementations
// ============================================================================

void simd_add_f32_scalar(float* dst, const float* src1, const float* src2, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = src1[i] + src2[i];
    }
}

void simd_mul_f32_scalar(float* dst, const float* src1, const float* src2, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = src1[i] * src2[i];
    }
}

void simd_fma_f32_scalar(float* dst, const float* src1, const float* src2, const float* src3, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = src1[i] * src2[i] + src3[i];
    }
}

void simd_fast_sin_f32_scalar(float* dst, const float* src, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = sinf(src[i]);
    }
}

void simd_fast_cos_f32_scalar(float* dst, const float* src, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = cosf(src[i]);
    }
}

void simd_fast_exp_f32_scalar(float* dst, const float* src, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = expf(src[i]);
    }
}

void simd_fast_log_f32_scalar(float* dst, const float* src, size_t count) {
    for (size_t i = 0; i < count; i++) {
        dst[i] = logf(src[i]);
    }
}

// ============================================================================
// NEON Implementations (ARM)
// ============================================================================

#if SIMD_NEON_AVAILABLE

static void simd_add_f32_neon(float* dst, const float* src1, const float* src2, size_t count) {
    size_t i = 0;
    
    // Process 4 floats at a time
    for (; i + 4 <= count; i += 4) {
        float32x4_t v1 = vld1q_f32(src1 + i);
        float32x4_t v2 = vld1q_f32(src2 + i);
        float32x4_t result = vaddq_f32(v1, v2);
        vst1q_f32(dst + i, result);
    }
    
    // Handle remaining elements
    for (; i < count; i++) {
        dst[i] = src1[i] + src2[i];
    }
}

static void simd_mul_f32_neon(float* dst, const float* src1, const float* src2, size_t count) {
    size_t i = 0;
    
    // Process 4 floats at a time
    for (; i + 4 <= count; i += 4) {
        float32x4_t v1 = vld1q_f32(src1 + i);
        float32x4_t v2 = vld1q_f32(src2 + i);
        float32x4_t result = vmulq_f32(v1, v2);
        vst1q_f32(dst + i, result);
    }
    
    // Handle remaining elements
    for (; i < count; i++) {
        dst[i] = src1[i] * src2[i];
    }
}

static void simd_fma_f32_neon(float* dst, const float* src1, const float* src2, const float* src3, size_t count) {
    size_t i = 0;
    
    // Process 4 floats at a time
    for (; i + 4 <= count; i += 4) {
        float32x4_t v1 = vld1q_f32(src1 + i);
        float32x4_t v2 = vld1q_f32(src2 + i);
        float32x4_t v3 = vld1q_f32(src3 + i);
        
        // Use fused multiply-add instruction if available, otherwise multiply-accumulate
#if defined(__aarch64__)
        float32x4_t result = vfmaq_f32(v3, v1, v2);  // v3 + v1 * v2
#else
        float32x4_t result = vmlaq_f32(v3, v1, v2);  // v3 + v1 * v2
#endif
        vst1q_f32(dst + i, result);
    }
    
    // Handle remaining elements
    for (; i < count; i++) {
        dst[i] = src1[i] * src2[i] + src3[i];
    }
}

#endif // SIMD_NEON_AVAILABLE

// ============================================================================
// SSE2 Implementations (x86_64)
// ============================================================================

#if SIMD_SSE2_AVAILABLE

static void simd_add_f32_sse2(float* dst, const float* src1, const float* src2, size_t count) {
    size_t i = 0;
    
    // Process 4 floats at a time
    for (; i + 4 <= count; i += 4) {
        __m128 v1 = _mm_loadu_ps(src1 + i);
        __m128 v2 = _mm_loadu_ps(src2 + i);
        __m128 result = _mm_add_ps(v1, v2);
        _mm_storeu_ps(dst + i, result);
    }
    
    // Handle remaining elements
    for (; i < count; i++) {
        dst[i] = src1[i] + src2[i];
    }
}

static void simd_mul_f32_sse2(float* dst, const float* src1, const float* src2, size_t count) {
    size_t i = 0;
    
    // Process 4 floats at a time
    for (; i + 4 <= count; i += 4) {
        __m128 v1 = _mm_loadu_ps(src1 + i);
        __m128 v2 = _mm_loadu_ps(src2 + i);
        __m128 result = _mm_mul_ps(v1, v2);
        _mm_storeu_ps(dst + i, result);
    }
    
    // Handle remaining elements
    for (; i < count; i++) {
        dst[i] = src1[i] * src2[i];
    }
}

static void simd_fma_f32_sse2(float* dst, const float* src1, const float* src2, const float* src3, size_t count) {
    size_t i = 0;
    
    // Process 4 floats at a time
    for (; i + 4 <= count; i += 4) {
        __m128 v1 = _mm_loadu_ps(src1 + i);
        __m128 v2 = _mm_loadu_ps(src2 + i);
        __m128 v3 = _mm_loadu_ps(src3 + i);
        
        // Emulate FMA with separate multiply and add
        __m128 mul_result = _mm_mul_ps(v1, v2);
        __m128 result = _mm_add_ps(mul_result, v3);
        _mm_storeu_ps(dst + i, result);
    }
    
    // Handle remaining elements
    for (; i < count; i++) {
        dst[i] = src1[i] * src2[i] + src3[i];
    }
}

#endif // SIMD_SSE2_AVAILABLE

// ============================================================================
// Public API (dispatches to appropriate implementation)
// ============================================================================

void simd_add_f32(float* dst, const float* src1, const float* src2, size_t count) {
    if (!simd_ops_available()) {
        simd_add_f32_scalar(dst, src1, src2, count);
        return;
    }
    
#if SIMD_NEON_AVAILABLE
    if (cpu_features_has_neon()) {
        simd_add_f32_neon(dst, src1, src2, count);
        return;
    }
#endif
    
#if SIMD_SSE2_AVAILABLE
    if (cpu_features_has_sse2()) {
        simd_add_f32_sse2(dst, src1, src2, count);
        return;
    }
#endif
    
    simd_add_f32_scalar(dst, src1, src2, count);
}

void simd_mul_f32(float* dst, const float* src1, const float* src2, size_t count) {
    if (!simd_ops_available()) {
        simd_mul_f32_scalar(dst, src1, src2, count);
        return;
    }
    
#if SIMD_NEON_AVAILABLE
    if (cpu_features_has_neon()) {
        simd_mul_f32_neon(dst, src1, src2, count);
        return;
    }
#endif
    
#if SIMD_SSE2_AVAILABLE
    if (cpu_features_has_sse2()) {
        simd_mul_f32_sse2(dst, src1, src2, count);
        return;
    }
#endif
    
    simd_mul_f32_scalar(dst, src1, src2, count);
}

void simd_fma_f32(float* dst, const float* src1, const float* src2, const float* src3, size_t count) {
    if (!simd_ops_available()) {
        simd_fma_f32_scalar(dst, src1, src2, src3, count);
        return;
    }
    
#if SIMD_NEON_AVAILABLE
    if (cpu_features_has_neon()) {
        simd_fma_f32_neon(dst, src1, src2, src3, count);
        return;
    }
#endif
    
#if SIMD_SSE2_AVAILABLE
    if (cpu_features_has_sse2()) {
        simd_fma_f32_sse2(dst, src1, src2, src3, count);
        return;
    }
#endif
    
    simd_fma_f32_scalar(dst, src1, src2, src3, count);
}

// Fast math functions default to scalar for now
// Production implementation would include SIMD-optimized approximations
void simd_fast_sin_f32(float* dst, const float* src, size_t count) {
    simd_fast_sin_f32_scalar(dst, src, count);
}

void simd_fast_cos_f32(float* dst, const float* src, size_t count) {
    simd_fast_cos_f32_scalar(dst, src, count);
}

void simd_fast_exp_f32(float* dst, const float* src, size_t count) {
    simd_fast_exp_f32_scalar(dst, src, count);
}

void simd_fast_log_f32(float* dst, const float* src, size_t count) {
    simd_fast_log_f32_scalar(dst, src, count);
}

// ============================================================================
// Benchmarking
// ============================================================================

static double get_time_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000.0 + ts.tv_nsec / 1000000.0;
}

float simd_ops_benchmark(const char* operation, size_t count) {
    const int iterations = 1000;
    float* src1 = (float*)malloc(count * sizeof(float));
    float* src2 = (float*)malloc(count * sizeof(float));
    float* src3 = (float*)malloc(count * sizeof(float));
    float* dst = (float*)malloc(count * sizeof(float));
    
    // Initialize with test data
    for (size_t i = 0; i < count; i++) {
        src1[i] = (float)i / count;
        src2[i] = (float)(count - i) / count;
        src3[i] = 1.0f;
    }
    
    double simd_time = 0.0;
    double scalar_time = 0.0;
    
    if (strcmp(operation, "add") == 0) {
        // Benchmark SIMD
        double start = get_time_ms();
        for (int i = 0; i < iterations; i++) {
            simd_add_f32(dst, src1, src2, count);
        }
        simd_time = get_time_ms() - start;
        
        // Benchmark scalar
        start = get_time_ms();
        for (int i = 0; i < iterations; i++) {
            simd_add_f32_scalar(dst, src1, src2, count);
        }
        scalar_time = get_time_ms() - start;
    }
    
    free(src1);
    free(src2);
    free(src3);
    free(dst);
    
    float speedup = (float)(scalar_time / simd_time);
    LOGI("Benchmark %s: SIMD=%.2fms, Scalar=%.2fms, Speedup=%.2fx",
         operation, simd_time, scalar_time, speedup);
    
    return speedup;
}
