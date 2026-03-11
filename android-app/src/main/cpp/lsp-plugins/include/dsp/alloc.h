/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins
 * Created on: 25 апр. 2016 г.
 *
 * lsp-plugins is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * lsp-plugins is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with lsp-plugins. If not, see <https://www.gnu.org/licenses/>.
 */

#ifndef DSP_ALLOC_H_
#define DSP_ALLOC_H_

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
namespace dsp
{
    /**
     * Default alignment for SIMD operations based on architecture
     */
    #if defined(ARCH_X86)
        static const size_t DEFAULT_DSP_ALIGN = 32; // AVX alignment
    #elif defined(ARCH_ARM) || defined(ARCH_AARCH64)
        static const size_t DEFAULT_DSP_ALIGN = 16; // NEON alignment
    #else
        static const size_t DEFAULT_DSP_ALIGN = 16; // Default SIMD alignment
    #endif

    /**
     * Allocate aligned memory for DSP operations
     * This function ensures memory is aligned for SIMD operations
     * 
     * @param size size in bytes to allocate
     * @param align alignment boundary (must be power of 2)
     * @return aligned pointer or NULL if allocation failed
     */
    void *alloc_aligned(size_t size, size_t align);

    /**
     * Free aligned memory allocated by alloc_aligned
     * 
     * @param ptr aligned pointer to free
     */
    void free_aligned(void *ptr);

    /**
     * Allocate aligned memory for float arrays (common in DSP)
     * 
     * @param count number of float elements
     * @param align alignment boundary
     * @return aligned float pointer or NULL if allocation failed
     */
    float *alloc_aligned_floats(size_t count, size_t align);

    /**
     * Allocate aligned memory for double arrays
     * 
     * @param count number of double elements
     * @param align alignment boundary
     * @return aligned double pointer or NULL if allocation failed
     */
    double *alloc_aligned_doubles(size_t count, size_t align);

    /**
     * Allocate aligned memory with default DSP alignment
     * 
     * @param size size in bytes to allocate
     * @return aligned pointer or NULL if allocation failed
     */
    void *alloc_aligned_default(size_t size);

    /**
     * Allocate aligned float array with default DSP alignment
     * 
     * @param count number of float elements
     * @return aligned float pointer or NULL if allocation failed
     */
    float *alloc_aligned_floats_default(size_t count);

    /**
     * Allocate aligned double array with default DSP alignment
     * 
     * @param count number of double elements
     * @return aligned double pointer or NULL if allocation failed
     */
    double *alloc_aligned_doubles_default(size_t count);

    /**
     * Check if pointer is aligned to specified boundary
     * 
     * @param ptr pointer to check
     * @param align alignment boundary (must be power of 2)
     * @return true if pointer is aligned
     */
    bool is_aligned(const void *ptr, size_t align);

    /**
     * Check if pointer is aligned to default DSP alignment
     * 
     * @param ptr pointer to check
     * @return true if pointer is aligned
     */
    bool is_aligned_default(const void *ptr);

    /**
     * Allocate and zero-initialize aligned memory
     * 
     * @param size size in bytes to allocate
     * @param align alignment boundary
     * @return aligned and zeroed pointer or NULL if allocation failed
     */
    void *calloc_aligned(size_t size, size_t align);

    /**
     * Allocate and zero-initialize aligned float array
     * 
     * @param count number of float elements
     * @param align alignment boundary
     * @return aligned and zeroed float pointer or NULL if allocation failed
     */
    float *calloc_aligned_floats(size_t count, size_t align);

    /**
     * Allocate and zero-initialize aligned float array with default alignment
     * 
     * @param count number of float elements
     * @return aligned and zeroed float pointer or NULL if allocation failed
     */
    float *calloc_aligned_floats_default(size_t count);

    /**
     * Reallocate aligned memory
     * Note: This may not preserve alignment if reallocation moves the block
     * 
     * @param ptr current aligned pointer (can be NULL)
     * @param size new size in bytes
     * @param align alignment boundary
     * @return new aligned pointer or NULL if reallocation failed
     */
    void *realloc_aligned(void *ptr, size_t size, size_t align);

} /* namespace dsp */
#endif /* __cplusplus */

#endif /* DSP_ALLOC_H_ */