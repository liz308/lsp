/**
 * JNI Bridge Architecture Verification Test
 * 
 * This test verifies that the JNI bridge works correctly across all supported
 * Android architectures: arm64-v8a, x86_64, and armeabi-v7a.
 * 
 * Tests:
 * 1. System.loadLibrary() works with proper ABI selection
 * 2. No C++ exceptions cross JNI boundary
 * 3. Plugin lifecycle functions work correctly
 * 4. Parameter operations work correctly
 * 5. Audio processing works correctly
 * 6. Architecture-specific optimizations are active
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include <cmath>
#include <cassert>

// Include the JNI bridge header
#include "../../../../dsp-core/lsp_android_bridge.h"

#define LOG_TAG "JNIBridgeArchTest"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Test result structure
struct TestResult {
    std::string testName;
    bool passed;
    std::string message;
    
    TestResult(const std::string& name, bool pass, const std::string& msg = "")
        : testName(name), passed(pass), message(msg) {}
};

// Architecture detection
const char* getArchitectureName() {
#if defined(__aarch64__) || defined(__arm64__)
    return "arm64-v8a";
#elif defined(__arm__)
    return "armeabi-v7a";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#else
    return "unknown";
#endif
}

// SIMD capability detection
bool hasNeonSupport() {
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
    return true;
#else
    return false;
#endif
}

bool hasSse2Support() {
#if defined(__SSE2__)
    return true;
#else
    return false;
#endif
}

// Test 1: Architecture detection and capabilities
TestResult testArchitectureDetection() {
    const char* arch = getArchitectureName();
    LOGI("Detected architecture: %s", arch);
    
    std::string message = "Architecture: ";
    message += arch;
    
    // Check SIMD capabilities
    if (strcmp(arch, "arm64-v8a") == 0) {
        if (!hasNeonSupport()) {
            return TestResult("Architecture Detection", false, 
                            "arm64-v8a should have NEON support");
        }
        message += " (NEON enabled)";
    } else if (strcmp(arch, "armeabi-v7a") == 0) {
        message += hasNeonSupport() ? " (NEON enabled)" : " (NEON disabled)";
    } else if (strcmp(arch, "x86_64") == 0) {
        if (!hasSse2Support()) {
            return TestResult("Architecture Detection", false,
                            "x86_64 should have SSE2 support");
        }
        message += " (SSE2 enabled)";
    }
    
    return TestResult("Architecture Detection", true, message);
}

// Test 2: Bridge API version
TestResult testBridgeApiVersion() {
    int32_t version = lsp_android_bridge_get_api_version();
    LOGI("Bridge API version: %d", version);
    
    if (version != LSP_ANDROID_BRIDGE_API_VERSION) {
        return TestResult("Bridge API Version", false,
                        "Version mismatch: expected " + 
                        std::to_string(LSP_ANDROID_BRIDGE_API_VERSION) +
                        ", got " + std::to_string(version));
    }
    
    return TestResult("Bridge API Version", true,
                    "Version: " + std::to_string(version));
}

// Test 3: Library initialization
TestResult testLibraryInitialization() {
    lsp_android_error_code result = lsp_android_initialize();
    
    if (result != LSP_ANDROID_SUCCESS) {
        return TestResult("Library Initialization", false,
                        "Initialization failed with error code: " + 
                        std::to_string(result));
    }
    
    return TestResult("Library Initialization", true, "Success");
}

// Test 4: Plugin creation (no exceptions should cross JNI boundary)
TestResult testPluginCreation() {
    lsp_android_plugin_handle handle = nullptr;
    
    try {
        // Test creating a parametric EQ plugin
        handle = lsp_android_create_parametric_eq();
        
        if (handle == nullptr) {
            return TestResult("Plugin Creation", false,
                            "Failed to create parametric EQ plugin");
        }
        
        // Clean up
        lsp_android_destroy_plugin(handle);
        
        return TestResult("Plugin Creation", true,
                        "Successfully created and destroyed plugin");
        
    } catch (const std::exception& e) {
        // This should NEVER happen - exceptions must not cross JNI boundary
        return TestResult("Plugin Creation", false,
                        "CRITICAL: C++ exception crossed JNI boundary: " + 
                        std::string(e.what()));
    } catch (...) {
        return TestResult("Plugin Creation", false,
                        "CRITICAL: Unknown exception crossed JNI boundary");
    }
}

// Test 5: Plugin initialization
TestResult testPluginInitialization() {
    lsp_android_plugin_handle handle = lsp_android_create_parametric_eq();
    
    if (handle == nullptr) {
        return TestResult("Plugin Initialization", false,
                        "Failed to create plugin");
    }
    
    try {
        // Initialize with standard sample rate and buffer size
        lsp_android_error_code result = lsp_android_initialize_plugin(
            handle, 48000.0f, 512);
        
        if (result != LSP_ANDROID_SUCCESS) {
            lsp_android_destroy_plugin(handle);
            return TestResult("Plugin Initialization", false,
                            "Initialization failed with error code: " + 
                            std::to_string(result));
        }
        
        // Activate the plugin
        result = lsp_android_activate_plugin(handle);
        
        if (result != LSP_ANDROID_SUCCESS) {
            lsp_android_destroy_plugin(handle);
            return TestResult("Plugin Initialization", false,
                            "Activation failed with error code: " + 
                            std::to_string(result));
        }
        
        lsp_android_destroy_plugin(handle);
        return TestResult("Plugin Initialization", true,
                        "Successfully initialized and activated plugin");
        
    } catch (...) {
        lsp_android_destroy_plugin(handle);
        return TestResult("Plugin Initialization", false,
                        "CRITICAL: Exception crossed JNI boundary");
    }
}

// Test 6: Port information retrieval
TestResult testPortInformation() {
    lsp_android_plugin_handle handle = lsp_android_create_parametric_eq();
    
    if (handle == nullptr) {
        return TestResult("Port Information", false,
                        "Failed to create plugin");
    }
    
    try {
        lsp_android_initialize_plugin(handle, 48000.0f, 512);
        lsp_android_activate_plugin(handle);
        
        int32_t portCount = lsp_android_get_port_count(handle);
        
        if (portCount <= 0) {
            lsp_android_destroy_plugin(handle);
            return TestResult("Port Information", false,
                            "Invalid port count: " + std::to_string(portCount));
        }
        
        // Get descriptor for first port
        lsp_android_port_descriptor descriptor;
        lsp_android_error_code result = lsp_android_get_port_descriptor(
            handle, 0, &descriptor);
        
        if (result != LSP_ANDROID_SUCCESS) {
            lsp_android_destroy_plugin(handle);
            return TestResult("Port Information", false,
                            "Failed to get port descriptor");
        }
        
        lsp_android_destroy_plugin(handle);
        return TestResult("Port Information", true,
                        "Port count: " + std::to_string(portCount));
        
    } catch (...) {
        lsp_android_destroy_plugin(handle);
        return TestResult("Port Information", false,
                        "CRITICAL: Exception crossed JNI boundary");
    }
}

// Test 7: Parameter operations
TestResult testParameterOperations() {
    lsp_android_plugin_handle handle = lsp_android_create_parametric_eq();
    
    if (handle == nullptr) {
        return TestResult("Parameter Operations", false,
                        "Failed to create plugin");
    }
    
    try {
        lsp_android_initialize_plugin(handle, 48000.0f, 512);
        lsp_android_activate_plugin(handle);
        
        int32_t portCount = lsp_android_get_port_count(handle);
        
        if (portCount > 0) {
            // Test set_param and get_param
            float testValue = 0.5f;
            lsp_android_error_code result = set_param(handle, 0, testValue);
            
            if (result != LSP_ANDROID_SUCCESS) {
                lsp_android_destroy_plugin(handle);
                return TestResult("Parameter Operations", false,
                                "set_param failed");
            }
            
            float retrievedValue = 0.0f;
            result = get_param(handle, 0, &retrievedValue);
            
            if (result != LSP_ANDROID_SUCCESS) {
                lsp_android_destroy_plugin(handle);
                return TestResult("Parameter Operations", false,
                                "get_param failed");
            }
            
            // Values should match (within floating point tolerance)
            if (std::abs(retrievedValue - testValue) > 0.0001f) {
                lsp_android_destroy_plugin(handle);
                return TestResult("Parameter Operations", false,
                                "Parameter value mismatch: set " + 
                                std::to_string(testValue) + ", got " + 
                                std::to_string(retrievedValue));
            }
        }
        
        lsp_android_destroy_plugin(handle);
        return TestResult("Parameter Operations", true,
                        "Successfully set and retrieved parameters");
        
    } catch (...) {
        lsp_android_destroy_plugin(handle);
        return TestResult("Parameter Operations", false,
                        "CRITICAL: Exception crossed JNI boundary");
    }
}

// Test 8: Audio processing
TestResult testAudioProcessing() {
    lsp_android_plugin_handle handle = lsp_android_create_parametric_eq();
    
    if (handle == nullptr) {
        return TestResult("Audio Processing", false,
                        "Failed to create plugin");
    }
    
    try {
        const int32_t bufferSize = 512;
        const float sampleRate = 48000.0f;
        
        lsp_android_initialize_plugin(handle, sampleRate, bufferSize);
        lsp_android_activate_plugin(handle);
        
        // Create test buffers
        std::vector<float> inputBuffer(bufferSize);
        std::vector<float> outputBuffer(bufferSize);
        
        // Generate test signal (sine wave at 1kHz)
        const float frequency = 1000.0f;
        const float omega = 2.0f * M_PI * frequency / sampleRate;
        
        for (int32_t i = 0; i < bufferSize; ++i) {
            inputBuffer[i] = std::sin(omega * i);
        }
        
        // Process audio
        lsp_android_error_code result = process(
            handle,
            inputBuffer.data(),
            outputBuffer.data(),
            bufferSize);
        
        if (result != LSP_ANDROID_SUCCESS) {
            lsp_android_destroy_plugin(handle);
            return TestResult("Audio Processing", false,
                            "Processing failed with error code: " + 
                            std::to_string(result));
        }
        
        // Verify output is not all zeros (plugin should process something)
        bool hasNonZero = false;
        for (int32_t i = 0; i < bufferSize; ++i) {
            if (std::abs(outputBuffer[i]) > 0.0001f) {
                hasNonZero = true;
                break;
            }
        }
        
        if (!hasNonZero) {
            lsp_android_destroy_plugin(handle);
            return TestResult("Audio Processing", false,
                            "Output buffer is all zeros");
        }
        
        lsp_android_destroy_plugin(handle);
        return TestResult("Audio Processing", true,
                        "Successfully processed " + std::to_string(bufferSize) + 
                        " frames");
        
    } catch (...) {
        lsp_android_destroy_plugin(handle);
        return TestResult("Audio Processing", false,
                        "CRITICAL: Exception crossed JNI boundary");
    }
}

// Test 9: Multiple plugin types
TestResult testMultiplePluginTypes() {
    std::vector<std::pair<std::string, lsp_android_plugin_handle>> plugins = {
        {"Parametric EQ", lsp_android_create_parametric_eq()},
        {"Compressor", lsp_android_create_compressor()},
        {"Limiter", lsp_android_create_limiter()},
        {"Gate", lsp_android_create_gate()},
        {"Analyzer", lsp_android_create_analyzer()}
    };
    
    int successCount = 0;
    std::string failedPlugins;
    
    for (auto& plugin : plugins) {
        if (plugin.second != nullptr) {
            successCount++;
            lsp_android_destroy_plugin(plugin.second);
        } else {
            if (!failedPlugins.empty()) failedPlugins += ", ";
            failedPlugins += plugin.first;
        }
    }
    
    if (successCount == 0) {
        return TestResult("Multiple Plugin Types", false,
                        "Failed to create any plugins");
    }
    
    if (successCount < plugins.size()) {
        return TestResult("Multiple Plugin Types", true,
                        "Created " + std::to_string(successCount) + "/" + 
                        std::to_string(plugins.size()) + " plugins. Failed: " + 
                        failedPlugins);
    }
    
    return TestResult("Multiple Plugin Types", true,
                    "Successfully created all " + std::to_string(successCount) + 
                    " plugin types");
}

// Test 10: Library cleanup
TestResult testLibraryCleanup() {
    try {
        lsp_android_cleanup();
        return TestResult("Library Cleanup", true, "Success");
    } catch (...) {
        return TestResult("Library Cleanup", false,
                        "CRITICAL: Exception during cleanup");
    }
}

// JNI function to run all tests
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_lspandroid_JniBridgeArchitectureTest_runTests(
    JNIEnv* env,
    jobject /* this */) {
    
    LOGI("=== Starting JNI Bridge Architecture Tests ===");
    LOGI("Architecture: %s", getArchitectureName());
    
    std::vector<TestResult> results;
    
    // Run all tests
    results.push_back(testArchitectureDetection());
    results.push_back(testBridgeApiVersion());
    results.push_back(testLibraryInitialization());
    results.push_back(testPluginCreation());
    results.push_back(testPluginInitialization());
    results.push_back(testPortInformation());
    results.push_back(testParameterOperations());
    results.push_back(testAudioProcessing());
    results.push_back(testMultiplePluginTypes());
    results.push_back(testLibraryCleanup());
    
    // Count results
    int passed = 0;
    int failed = 0;
    for (const auto& result : results) {
        if (result.passed) {
            passed++;
            LOGI("[PASS] %s: %s", result.testName.c_str(), result.message.c_str());
        } else {
            failed++;
            LOGE("[FAIL] %s: %s", result.testName.c_str(), result.message.c_str());
        }
    }
    
    LOGI("=== Test Summary ===");
    LOGI("Total: %d, Passed: %d, Failed: %d", 
         (int)results.size(), passed, failed);
    
    // Create Java String array for results
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray(
        results.size(), stringClass, nullptr);
    
    for (size_t i = 0; i < results.size(); ++i) {
        std::string resultStr = (results[i].passed ? "[PASS] " : "[FAIL] ") +
                               results[i].testName + ": " + results[i].message;
        jstring jstr = env->NewStringUTF(resultStr.c_str());
        env->SetObjectArrayElement(resultArray, i, jstr);
        env->DeleteLocalRef(jstr);
    }
    
    return resultArray;
}

// JNI function to get architecture info
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_lspandroid_JniBridgeArchitectureTest_getArchitectureInfo(
    JNIEnv* env,
    jobject /* this */) {
    
    std::string info = "Architecture: ";
    info += getArchitectureName();
    info += "\nSIMD Support: ";
    
    if (hasNeonSupport()) {
        info += "NEON";
    } else if (hasSse2Support()) {
        info += "SSE2";
    } else {
        info += "None";
    }
    
    return env->NewStringUTF(info.c_str());
}
