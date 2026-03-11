/*
 * Copyright (C) 2020 Linux Studio Plugins Project <https://lsp-plug.in/>
 *           (C) 2020 Vladimir Sadovnikov <sadko4u@gmail.com>
 *
 * This file is part of lsp-plugins - Android Port
 * 
 * Minimal debug.h for Android NDK compatibility
 */

#ifndef CORE_DEBUG_H_
#define CORE_DEBUG_H_

#include <stdio.h>
#include <stdarg.h>

// Android NDK logging
#ifdef __ANDROID__
    #include <android/log.h>
    #define LSP_LOG_TAG "LSP-Plugins"
    
    #ifdef LSP_DEBUG
        #define lsp_error(msg, ...)     __android_log_print(ANDROID_LOG_ERROR, LSP_LOG_TAG, "[ERR][%s:%d] %s: " msg, __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__)
        #define lsp_warn(msg, ...)      __android_log_print(ANDROID_LOG_WARN, LSP_LOG_TAG, "[WRN][%s:%d] %s: " msg, __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__)
        #define lsp_info(msg, ...)      __android_log_print(ANDROID_LOG_INFO, LSP_LOG_TAG, "[INF][%s:%d] %s: " msg, __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__)
        #define lsp_debug(msg, ...)     __android_log_print(ANDROID_LOG_DEBUG, LSP_LOG_TAG, "[DBG][%s:%d] %s: " msg, __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__)
        #define lsp_trace(msg, ...)     __android_log_print(ANDROID_LOG_VERBOSE, LSP_LOG_TAG, "[TRC][%s:%d] %s: " msg, __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__)
    #else
        #define lsp_error(msg, ...)     __android_log_print(ANDROID_LOG_ERROR, LSP_LOG_TAG, "[ERR] " msg, ## __VA_ARGS__)
        #define lsp_warn(msg, ...)      __android_log_print(ANDROID_LOG_WARN, LSP_LOG_TAG, "[WRN] " msg, ## __VA_ARGS__)
        #define lsp_info(msg, ...)      __android_log_print(ANDROID_LOG_INFO, LSP_LOG_TAG, "[INF] " msg, ## __VA_ARGS__)
        #define lsp_debug(msg, ...)
        #define lsp_trace(msg, ...)
    #endif
#else
    // Fallback to stderr for non-Android platforms
    #define LSP_LOG_FD stderr
    
    #ifdef LSP_DEBUG
        #define lsp_error(msg, ...)     { fprintf(LSP_LOG_FD, "[ERR][%s:%d] %s: " msg "\n", __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_warn(msg, ...)      { fprintf(LSP_LOG_FD, "[WRN][%s:%d] %s: " msg "\n", __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_info(msg, ...)      { fprintf(LSP_LOG_FD, "[INF][%s:%d] %s: " msg "\n", __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_debug(msg, ...)     { fprintf(LSP_LOG_FD, "[DBG][%s:%d] %s: " msg "\n", __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_trace(msg, ...)     { fprintf(LSP_LOG_FD, "[TRC][%s:%d] %s: " msg "\n", __FILE__, __LINE__, __FUNCTION__, ## __VA_ARGS__); fflush(LSP_LOG_FD); }
    #else
        #define lsp_error(msg, ...)     { fprintf(LSP_LOG_FD, "[ERR] " msg "\n", ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_warn(msg, ...)      { fprintf(LSP_LOG_FD, "[WRN] " msg "\n", ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_info(msg, ...)      { fprintf(LSP_LOG_FD, "[INF] " msg "\n", ## __VA_ARGS__); fflush(LSP_LOG_FD); }
        #define lsp_debug(msg, ...)
        #define lsp_trace(msg, ...)
    #endif
#endif

// Assertions
#ifdef LSP_DEBUG
    #define lsp_assert(x)           if (!(x)) { lsp_error("Assertion failed: %s", #x); }
    #define lsp_assert_msg(x, msg, ...)  if (!(x)) { lsp_error("Assertion failed: %s, " msg, #x, ## __VA_ARGS__); }
#else
    #define lsp_assert(x)
    #define lsp_assert_msg(x, ...)
#endif

#endif /* CORE_DEBUG_H_ */
