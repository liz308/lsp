#ifndef LSP_ANDROID_CPU_FEATURES_H
#define LSP_ANDROID_CPU_FEATURES_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// CPU architecture types
typedef enum {
    CPU_ARCH_UNKNOWN = 0,
    CPU_ARCH_ARM64_V8A,
    CPU_ARCH_X86_64,
    CPU_ARCH_ARMEABI_V7A
} cpu_arch_t;

// SIMD feature flags
typedef enum {
    SIMD_NONE = 0,
    SIMD_NEON = (1 << 0),      // ARM NEON
    SIMD_SSE2 = (1 << 1),      // x86 SSE2
    SIMD_SSE4_1 = (1 << 2),    // x86 SSE4.1
    SIMD_AVX = (1 << 3),       // x86 AVX
    SIMD_AVX2 = (1 << 4)       // x86 AVX2
} simd_features_t;

// CPU feature detection results
typedef struct {
    cpu_arch_t architecture;
    uint32_t simd_features;
    const char* cpu_family;
    const char* cpu_model;
    bool has_neon;
    bool has_sse2;
    bool has_sse4_1;
    bool has_avx;
    bool has_avx2;
} cpu_features_info_t;

/**
 * Initialize CPU feature detection
 * Must be called once at startup before using any SIMD functions
 * @return true if detection succeeded, false otherwise
 */
bool cpu_features_init(void);

/**
 * Get detected CPU features
 * @return Pointer to CPU features info structure
 */
const cpu_features_info_t* cpu_features_get(void);

/**
 * Get CPU architecture type
 * @return CPU architecture enum value
 */
cpu_arch_t cpu_features_get_arch(void);

/**
 * Check if NEON is available
 * @return true if NEON is supported and enabled
 */
bool cpu_features_has_neon(void);

/**
 * Check if SSE2 is available
 * @return true if SSE2 is supported and enabled
 */
bool cpu_features_has_sse2(void);

/**
 * Check if SSE4.1 is available
 * @return true if SSE4.1 is supported and enabled
 */
bool cpu_features_has_sse4_1(void);

/**
 * Check if AVX is available
 * @return true if AVX is supported and enabled
 */
bool cpu_features_has_avx(void);

/**
 * Check if AVX2 is available
 * @return true if AVX2 is supported and enabled
 */
bool cpu_features_has_avx2(void);

/**
 * Log detected CPU features to Android logcat
 */
void cpu_features_log(void);

/**
 * Get human-readable string for architecture
 * @param arch CPU architecture type
 * @return String representation of architecture
 */
const char* cpu_features_arch_string(cpu_arch_t arch);

/**
 * Get human-readable string for SIMD features
 * @param features SIMD feature flags
 * @param buffer Output buffer for string
 * @param buffer_size Size of output buffer
 * @return Number of characters written (excluding null terminator)
 */
int cpu_features_simd_string(uint32_t features, char* buffer, size_t buffer_size);

#ifdef __cplusplus
}
#endif

#endif // LSP_ANDROID_CPU_FEATURES_H
