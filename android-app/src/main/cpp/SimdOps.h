#ifndef LSP_ANDROID_SIMD_OPS_H
#define LSP_ANDROID_SIMD_OPS_H

#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize SIMD operations
 * Detects CPU features and selects optimal implementation
 * @return true if initialization succeeded
 */
bool simd_ops_init(void);

/**
 * Check if SIMD operations are available
 * @return true if SIMD is enabled, false if using scalar fallback
 */
bool simd_ops_available(void);

/**
 * Force scalar mode (disable SIMD)
 * Useful for testing and validation
 * @param force_scalar true to force scalar mode
 */
void simd_ops_force_scalar(bool force_scalar);

// ============================================================================
// Vector Addition: dst[i] = src1[i] + src2[i]
// ============================================================================

/**
 * Add two float arrays (SIMD optimized)
 * @param dst Destination array
 * @param src1 First source array
 * @param src2 Second source array
 * @param count Number of elements
 */
void simd_add_f32(float* dst, const float* src1, const float* src2, size_t count);

/**
 * Add two float arrays (scalar reference)
 * @param dst Destination array
 * @param src1 First source array
 * @param src2 Second source array
 * @param count Number of elements
 */
void simd_add_f32_scalar(float* dst, const float* src1, const float* src2, size_t count);

// ============================================================================
// Vector Multiplication: dst[i] = src1[i] * src2[i]
// ============================================================================

/**
 * Multiply two float arrays (SIMD optimized)
 * @param dst Destination array
 * @param src1 First source array
 * @param src2 Second source array
 * @param count Number of elements
 */
void simd_mul_f32(float* dst, const float* src1, const float* src2, size_t count);

/**
 * Multiply two float arrays (scalar reference)
 * @param dst Destination array
 * @param src1 First source array
 * @param src2 Second source array
 * @param count Number of elements
 */
void simd_mul_f32_scalar(float* dst, const float* src1, const float* src2, size_t count);

// ============================================================================
// Fused Multiply-Add: dst[i] = src1[i] * src2[i] + src3[i]
// ============================================================================

/**
 * Fused multiply-add (SIMD optimized)
 * @param dst Destination array
 * @param src1 First source array (multiplicand)
 * @param src2 Second source array (multiplier)
 * @param src3 Third source array (addend)
 * @param count Number of elements
 */
void simd_fma_f32(float* dst, const float* src1, const float* src2, const float* src3, size_t count);

/**
 * Fused multiply-add (scalar reference)
 * @param dst Destination array
 * @param src1 First source array (multiplicand)
 * @param src2 Second source array (multiplier)
 * @param src3 Third source array (addend)
 * @param count Number of elements
 */
void simd_fma_f32_scalar(float* dst, const float* src1, const float* src2, const float* src3, size_t count);

// ============================================================================
// Fast Math Functions (architecture-specific approximations)
// ============================================================================

/**
 * Fast sine approximation (SIMD optimized)
 * @param dst Destination array
 * @param src Source array (radians)
 * @param count Number of elements
 */
void simd_fast_sin_f32(float* dst, const float* src, size_t count);

/**
 * Fast cosine approximation (SIMD optimized)
 * @param dst Destination array
 * @param src Source array (radians)
 * @param count Number of elements
 */
void simd_fast_cos_f32(float* dst, const float* src, size_t count);

/**
 * Fast exponential approximation (SIMD optimized)
 * @param dst Destination array
 * @param src Source array
 * @param count Number of elements
 */
void simd_fast_exp_f32(float* dst, const float* src, size_t count);

/**
 * Fast natural logarithm approximation (SIMD optimized)
 * @param dst Destination array
 * @param src Source array
 * @param count Number of elements
 */
void simd_fast_log_f32(float* dst, const float* src, size_t count);

// ============================================================================
// Scalar reference implementations for fast math
// ============================================================================

void simd_fast_sin_f32_scalar(float* dst, const float* src, size_t count);
void simd_fast_cos_f32_scalar(float* dst, const float* src, size_t count);
void simd_fast_exp_f32_scalar(float* dst, const float* src, size_t count);
void simd_fast_log_f32_scalar(float* dst, const float* src, size_t count);

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Get SIMD implementation name
 * @return String describing active SIMD implementation
 */
const char* simd_ops_get_implementation(void);

/**
 * Benchmark SIMD vs scalar performance
 * @param operation Operation name
 * @param count Number of elements to process
 * @return Speedup factor (SIMD time / scalar time)
 */
float simd_ops_benchmark(const char* operation, size_t count);

#ifdef __cplusplus
}
#endif

#endif // LSP_ANDROID_SIMD_OPS_H
