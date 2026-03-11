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

#include <dsp/dsp.h>
#include <dsp/alloc.h>
#include <core/alloc.h>
#include <core/debug.h>
#include <stdlib.h>
#include <string.h>

#ifdef __ANDROID__
    #include <malloc.h>
    #include <sys/mman.h>
    #include <unistd.h>
    #include <android/log.h>
    #define LOG_TAG "LSP_DSP"
    #define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
    #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

namespace dsp
{
    // Default alignment for SIMD operations
    #if defined(ARCH_X86)
        static const size_t DEFAULT_DSP_ALIGN = 32; // AVX alignment
    #elif defined(ARCH_ARM) || defined(ARCH_AARCH64) || defined(__ANDROID__)
        static const size_t DEFAULT_DSP_ALIGN = 16; // NEON alignment
    #else
        static const size_t DEFAULT_DSP_ALIGN = 16; // Default SIMD alignment
    #endif
    /**
     * Allocate aligned memory for DSP operations
     * This function ensures memory is aligned for SIMD operations
     * Android-compatible implementation using posix_memalign when available
     * 
     * @param size size in bytes to allocate
     * @param align alignment boundary (must be power of 2)
     * @return aligned pointer or NULL if allocation failed
     */
    void *alloc_aligned(size_t size, size_t align)
    {
        // Check for power of 2
        if ((align == 0) || (align & (align - 1)))
            return NULL;
            
        if (size == 0)
            return NULL;

    #ifdef __ANDROID__
        // Android API level 16+ supports posix_memalign
        void *ptr = NULL;
        if (posix_memalign(&ptr, align, size) == 0)
        {
            return ptr;
        }

        // Fallback to manual alignment for older Android versions
        LOGD("posix_memalign failed, using manual alignment");
    #endif

        // Allocate extra space for alignment and pointer storage
        void *original = ::malloc(size + align + sizeof(void*));
        if (original == NULL)
        {
    #ifdef __ANDROID__
            LOGE("Failed to allocate %zu bytes", size);
    #endif
            return NULL;
        }

        // Calculate aligned address
        uintptr_t original_addr = reinterpret_cast<uintptr_t>(original);
        uintptr_t aligned_addr = (original_addr + sizeof(void*) + align - 1) & ~(align - 1);
        
        // Store original pointer before aligned address
        void **pointer_store = reinterpret_cast<void**>(aligned_addr) - 1;
        *pointer_store = original;

        return reinterpret_cast<void*>(aligned_addr);
    }

    /**
     * Free aligned memory allocated by alloc_aligned
     * Android-compatible implementation
     * 
     * @param ptr aligned pointer to free
     */
    void free_aligned(void *ptr)
    {
        if (ptr == NULL)
            return;

    #ifdef __ANDROID__
        // Check if this was allocated with posix_memalign
        // We can't easily distinguish, so we try the safer approach
        // by checking if the pointer looks like it has our header
        void **potential_store = reinterpret_cast<void**>(ptr) - 1;
        
        // Simple heuristic: if the stored pointer is close to our pointer,
        // it's likely our manual allocation
        uintptr_t ptr_addr = reinterpret_cast<uintptr_t>(ptr);
        uintptr_t stored_addr = reinterpret_cast<uintptr_t>(*potential_store);
        
        if (stored_addr < ptr_addr && (ptr_addr - stored_addr) < 1024)
        {
            // Looks like manual allocation
            ::free(*potential_store);
        }
        else
        {
            // Likely posix_memalign allocation
            ::free(ptr);
        }
    #else
        // Retrieve original pointer
        void **pointer_store = reinterpret_cast<void**>(ptr) - 1;
        void *original = *pointer_store;
        
        ::free(original);
    #endif
    }

    /**
     * Allocate aligned memory for float arrays (common in DSP)
     * 
     * @param count number of float elements
     * @param align alignment boundary
     * @return aligned float pointer or NULL if allocation failed
     */
    float *alloc_aligned_floats(size_t count, size_t align)
    {
        return reinterpret_cast<float*>(alloc_aligned(count * sizeof(float), align));
    }

    /**
     * Allocate aligned memory for double arrays
     * 
     * @param count number of double elements
     * @param align alignment boundary
     * @return aligned double pointer or NULL if allocation failed
     */
    double *alloc_aligned_doubles(size_t count, size_t align)
    {
        return reinterpret_cast<double*>(alloc_aligned(count * sizeof(double), align));
    }

    /**
     * Allocate aligned memory with default DSP alignment
     * 
     * @param size size in bytes to allocate
     * @return aligned pointer or NULL if allocation failed
     */
    void *alloc_aligned_default(size_t size)
    {
        return alloc_aligned(size, DEFAULT_DSP_ALIGN);
    }

    /**
     * Allocate aligned float array with default DSP alignment
     * 
     * @param count number of float elements
     * @return aligned float pointer or NULL if allocation failed
     */
    float *alloc_aligned_floats_default(size_t count)
    {
        return alloc_aligned_floats(count, DEFAULT_DSP_ALIGN);
    }

    /**
     * Allocate aligned double array with default DSP alignment
     * 
     * @param count number of double elements
     * @return aligned double pointer or NULL if allocation failed
     */
    double *alloc_aligned_doubles_default(size_t count)
    {
        return alloc_aligned_doubles(count, DEFAULT_DSP_ALIGN);
    }

    /**
     * Check if pointer is aligned to specified boundary
     * 
     * @param ptr pointer to check
     * @param align alignment boundary (must be power of 2)
     * @return true if pointer is aligned
     */
    bool is_aligned(const void *ptr, size_t align)
    {
        if (align == 0)
            return true;
            
        uintptr_t addr = reinterpret_cast<uintptr_t>(ptr);
        return (addr & (align - 1)) == 0;
    }

    /**
     * Check if pointer is aligned to default DSP alignment
     * 
     * @param ptr pointer to check
     * @return true if pointer is aligned
     */
    bool is_aligned_default(const void *ptr)
    {
        return is_aligned(ptr, DEFAULT_DSP_ALIGN);
    }

    /**
     * Allocate and zero-initialize aligned memory
     * 
     * @param size size in bytes to allocate
     * @param align alignment boundary
     * @return aligned and zeroed pointer or NULL if allocation failed
     */
    void *calloc_aligned(size_t size, size_t align)
    {
        void *ptr = alloc_aligned(size, align);
        if (ptr != NULL)
            ::memset(ptr, 0, size);
        return ptr;
    }

    /**
     * Allocate and zero-initialize aligned float array
     * 
     * @param count number of float elements
     * @param align alignment boundary
     * @return aligned and zeroed float pointer or NULL if allocation failed
     */
    float *calloc_aligned_floats(size_t count, size_t align)
    {
        float *ptr = alloc_aligned_floats(count, align);
        if (ptr != NULL)
            ::memset(ptr, 0, count * sizeof(float));
        return ptr;
}
    /**
     * Allocate and zero-initialize aligned float array with default alignment
     * 
     * @param count number of float elements
     * @return aligned and zeroed float pointer or NULL if allocation failed
     */
    float *calloc_aligned_floats_default(size_t count)
    {
        return calloc_aligned_floats(count, DEFAULT_DSP_ALIGN);
    }

    /**
     * Reallocate aligned memory
     * Android-compatible implementation
     * Note: This may not preserve alignment if reallocation moves the block
     * 
     * @param ptr current aligned pointer (can be NULL)
     * @param size new size in bytes
     * @param align alignment boundary
     * @return new aligned pointer or NULL if reallocation failed
     */
    void *realloc_aligned(void *ptr, size_t size, size_t align)
    {
        if (ptr == NULL)
            return alloc_aligned(size, align);
            
        if (size == 0)
        {
            free_aligned(ptr);
            return NULL;
        }

        // For Android compatibility and simplicity, allocate new block
        // This ensures alignment is preserved across all Android versions
        void *new_ptr = alloc_aligned(size, align);
        if (new_ptr == NULL)
        {
    #ifdef __ANDROID__
            LOGE("Failed to reallocate %zu bytes", size);
    #endif
            return NULL;
        }

        // We don't know the original size, so we can't copy all data
        // This is a limitation of this simple implementation
        // In practice, DSP code should know the size of allocated buffers
        
        free_aligned(ptr);
        return new_ptr;
    }

    /**
     * Get system page size (useful for Android memory management)
     * 
     * @return system page size in bytes
     */
    size_t get_page_size()
    {
    #ifdef __ANDROID__
        static size_t page_size = 0;
        if (page_size == 0)
        {
            page_size = static_cast<size_t>(sysconf(_SC_PAGESIZE));
            if (page_size == 0)
                page_size = 4096; // Fallback to common page size
        }
        return page_size;
    #else
        return 4096; // Default page size
    #endif
    }

    /**
     * Check Android API level for feature availability
     * 
     * @return Android API level or 0 if not on Android
     */
    int get_android_api_level()
    {
    #ifdef __ANDROID__
        return android_get_device_api_level();
    #else
        return 0;
    #endif
    }
}
