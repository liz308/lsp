/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins - Android Port
 * 
 * Minimal types.h for Android NDK compatibility
 */

#ifndef CORE_TYPES_H_
#define CORE_TYPES_H_

#include <stdint.h>
#include <stddef.h>

// Android NDK compatibility
#ifdef __ANDROID__
    #define PLATFORM_UNIX
    #define PLATFORM_LINUX
#endif

// Alignment macros for memory allocation
#define DEFAULT_ALIGN           16
#define ALIGN_SIZE(x, align)    (((x) + ((align) - 1)) & ~((align) - 1))

typedef uint64_t        wsize_t;
typedef int64_t         wssize_t;
typedef uint32_t        lsp_wchar_t;
typedef int32_t         lsp_swchar_t;

#endif /* CORE_TYPES_H_ */
