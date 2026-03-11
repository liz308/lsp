#include "CpuFeatures.h"
#include <cpu-features.h>
#include <android/log.h>
#include <cpu-features.h>
#include <string.h>
#include <string.h>
#include <stdio.h>

#define LOG_TAG "LSP-CpuFeatures"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global CPU features info
static cpu_features_info_t g_cpu_features = {
    .architecture = CPU_ARCH_UNKNOWN,
    .simd_features = SIMD_NONE,
    .cpu_family = "Unknown",
    .cpu_model = "Unknown",
    .has_neon = false,
    .has_sse2 = false,
    .has_sse4_1 = false,
    .has_avx = false,
    .has_avx2 = false
};

static bool g_initialized = false;

// Detect ARM architecture and features
static void detect_arm_features(void) {
    AndroidCpuFamily family = android_getCpuFamily();
    
    if (family == ANDROID_CPU_FAMILY_ARM64) {
        g_cpu_features.architecture = CPU_ARCH_ARM64_V8A;
        g_cpu_features.cpu_family = "ARM64";
        
        // NEON is mandatory for ARM64
        g_cpu_features.has_neon = true;
        g_cpu_features.simd_features |= SIMD_NEON;
        
        uint64_t features = android_getCpuFeatures();
        
        // ARM64 always has NEON (Advanced SIMD)
        if (features & ANDROID_CPU_ARM64_FEATURE_ASIMD) {
            g_cpu_features.cpu_model = "ARMv8-A with NEON";
        } else {
            g_cpu_features.cpu_model = "ARMv8-A";
        }
        
        LOGI("Detected ARM64-v8a with mandatory NEON support");
        
    } else if (family == ANDROID_CPU_FAMILY_ARM) {
        g_cpu_features.architecture = CPU_ARCH_ARMEABI_V7A;
        g_cpu_features.cpu_family = "ARM";
        
        uint64_t features = android_getCpuFeatures();
        
        // Check for NEON on ARMv7
        if (features & ANDROID_CPU_ARM_FEATURE_NEON) {
            g_cpu_features.has_neon = true;
            g_cpu_features.simd_features |= SIMD_NEON;
            g_cpu_features.cpu_model = "ARMv7-A with NEON";
            LOGI("Detected ARMv7-A with NEON support");
        } else {
            g_cpu_features.cpu_model = "ARMv7-A without NEON";
            LOGW("Detected ARMv7-A WITHOUT NEON - will use scalar fallback");
        }
    }
}

// Detect x86/x86_64 architecture and features
static void detect_x86_features(void) {
    AndroidCpuFamily family = android_getCpuFamily();
    
    if (family == ANDROID_CPU_FAMILY_X86 || family == ANDROID_CPU_FAMILY_X86_64) {
        g_cpu_features.architecture = CPU_ARCH_X86_64;
        g_cpu_features.cpu_family = "x86_64";
        
        uint64_t features = android_getCpuFeatures();
        
        // SSE2 is baseline for x86_64 (always true for x86_64 Android ABIs)
        g_cpu_features.has_sse2 = true;
        g_cpu_features.simd_features |= SIMD_SSE2;
        
        // Check for SSE4.1
        if (features & ANDROID_CPU_X86_FEATURE_SSE4_1) {
            g_cpu_features.has_sse4_1 = true;
            g_cpu_features.simd_features |= SIMD_SSE4_1;
        }
        
        // Check for AVX
        if (features & ANDROID_CPU_X86_FEATURE_AVX) {
            g_cpu_features.has_avx = true;
            g_cpu_features.simd_features |= SIMD_AVX;
        }
        
        // Check for AVX2
        if (features & ANDROID_CPU_X86_FEATURE_AVX2) {
            g_cpu_features.has_avx2 = true;
            g_cpu_features.simd_features |= SIMD_AVX2;
        }
        
        // Build model string
        char model[128];
        snprintf(model, sizeof(model), "x86_64 (SSE2:%d SSE4.1:%d AVX:%d AVX2:%d)",
                 g_cpu_features.has_sse2,
                 g_cpu_features.has_sse4_1,
                 g_cpu_features.has_avx,
                 g_cpu_features.has_avx2);
        
        // Note: This is a static string, in production you'd want to allocate
        static char model_static[128];
        strncpy(model_static, model, sizeof(model_static) - 1);
        g_cpu_features.cpu_model = model_static;
        
        LOGI("Detected %s", model);
    }
}

bool cpu_features_init(void) {
    if (g_initialized) {
        LOGW("CPU features already initialized");
        return true;
    }
    
    LOGI("Initializing CPU feature detection...");
    
    // Detect CPU family
    AndroidCpuFamily family = android_getCpuFamily();
    
    switch (family) {
        case ANDROID_CPU_FAMILY_ARM:
        case ANDROID_CPU_FAMILY_ARM64:
            detect_arm_features();
            break;
            
        case ANDROID_CPU_FAMILY_X86:
        case ANDROID_CPU_FAMILY_X86_64:
            detect_x86_features();
            break;
            
        default:
            LOGE("Unknown CPU family: %d", family);
            return false;
    }
    
    g_initialized = true;
    
    // Log detected features
    cpu_features_log();
    
    return true;
}

const cpu_features_info_t* cpu_features_get(void) {
    if (!g_initialized) {
        LOGW("CPU features not initialized, calling cpu_features_init()");
        cpu_features_init();
    }
    return &g_cpu_features;
}

cpu_arch_t cpu_features_get_arch(void) {
    return g_cpu_features.architecture;
}

bool cpu_features_has_neon(void) {
    return g_cpu_features.has_neon;
}

bool cpu_features_has_sse2(void) {
    return g_cpu_features.has_sse2;
}

bool cpu_features_has_sse4_1(void) {
    return g_cpu_features.has_sse4_1;
}

bool cpu_features_has_avx(void) {
    return g_cpu_features.has_avx;
}

bool cpu_features_has_avx2(void) {
    return g_cpu_features.has_avx2;
}

void cpu_features_log(void) {
    LOGI("=== CPU Features ===");
    LOGI("Architecture: %s", cpu_features_arch_string(g_cpu_features.architecture));
    LOGI("CPU Family: %s", g_cpu_features.cpu_family);
    LOGI("CPU Model: %s", g_cpu_features.cpu_model);
    
    char simd_str[256];
    cpu_features_simd_string(g_cpu_features.simd_features, simd_str, sizeof(simd_str));
    LOGI("SIMD Features: %s", simd_str);
    
    LOGI("NEON: %s", g_cpu_features.has_neon ? "YES" : "NO");
    LOGI("SSE2: %s", g_cpu_features.has_sse2 ? "YES" : "NO");
    LOGI("SSE4.1: %s", g_cpu_features.has_sse4_1 ? "YES" : "NO");
    LOGI("AVX: %s", g_cpu_features.has_avx ? "YES" : "NO");
    LOGI("AVX2: %s", g_cpu_features.has_avx2 ? "YES" : "NO");
    LOGI("===================");
}

const char* cpu_features_arch_string(cpu_arch_t arch) {
    switch (arch) {
        case CPU_ARCH_ARM64_V8A:
            return "arm64-v8a";
        case CPU_ARCH_X86_64:
            return "x86_64";
        case CPU_ARCH_ARMEABI_V7A:
            return "armeabi-v7a";
        default:
            return "unknown";
    }
}

int cpu_features_simd_string(uint32_t features, char* buffer, size_t buffer_size) {
    if (!buffer || buffer_size == 0) {
        return 0;
    }
    
    buffer[0] = '\0';
    int written = 0;
    bool first = true;
    
    if (features == SIMD_NONE) {
        written = snprintf(buffer, buffer_size, "None (scalar only)");
        return written;
    }
    
    if (features & SIMD_NEON) {
        written += snprintf(buffer + written, buffer_size - written, 
                           "%sNEON", first ? "" : ", ");
        first = false;
    }
    
    if (features & SIMD_SSE2) {
        written += snprintf(buffer + written, buffer_size - written,
                           "%sSSE2", first ? "" : ", ");
        first = false;
    }
    
    if (features & SIMD_SSE4_1) {
        written += snprintf(buffer + written, buffer_size - written,
                           "%sSSE4.1", first ? "" : ", ");
        first = false;
    }
    
    if (features & SIMD_AVX) {
        written += snprintf(buffer + written, buffer_size - written,
                           "%sAVX", first ? "" : ", ");
        first = false;
    }
    
    if (features & SIMD_AVX2) {
        written += snprintf(buffer + written, buffer_size - written,
                           "%sAVX2", first ? "" : ", ");
        first = false;
    }
    
    return written;
}
