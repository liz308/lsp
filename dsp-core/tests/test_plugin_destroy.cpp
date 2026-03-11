#include "../lsp_android_bridge.h"
#include <cassert>
#include <iostream>

// Unit tests for plugin_destroy() function
// Validates Requirement 4 (JNI Bridge Interface) and Requirement 29 (Memory Management)

namespace {

void test_destroy_valid_handle() {
    std::cout << "Test: Destroy valid plugin handle\n";
    
    lsp_android_plugin_handle handle = nullptr;
    lsp_android_error_code result = plugin_create(&handle);
    assert(result == LSP_ANDROID_SUCCESS);
    assert(handle != nullptr);
    
    // Destroy the plugin
    result = plugin_destroy(handle);
    assert(result == LSP_ANDROID_SUCCESS);
    assert(lsp_android_get_last_error() == LSP_ANDROID_SUCCESS);
    
    std::cout << "  PASSED: Valid handle destroyed successfully\n";
}

void test_destroy_null_handle() {
    std::cout << "Test: Destroy null plugin handle\n";
    
    // Attempt to destroy a null handle
    lsp_android_error_code result = plugin_destroy(nullptr);
    assert(result == LSP_ANDROID_ERROR_INVALID_HANDLE);
    assert(lsp_android_get_last_error() == LSP_ANDROID_ERROR_INVALID_HANDLE);
    
    std::cout << "  PASSED: Null handle rejected with appropriate error code\n";
}

void test_destroy_sets_error_state() {
    std::cout << "Test: Destroy sets thread-local error state\n";
    
    // Test with valid handle
    lsp_android_plugin_handle handle = nullptr;
    plugin_create(&handle);
    
    lsp_android_error_code result = plugin_destroy(handle);
    assert(result == LSP_ANDROID_SUCCESS);
    assert(lsp_android_get_last_error() == LSP_ANDROID_SUCCESS);
    
    // Test with invalid handle
    result = plugin_destroy(nullptr);
    assert(result == LSP_ANDROID_ERROR_INVALID_HANDLE);
    assert(lsp_android_get_last_error() == LSP_ANDROID_ERROR_INVALID_HANDLE);
    
    std::cout << "  PASSED: Error state correctly set for both valid and invalid cases\n";
}

void test_destroy_releases_resources() {
    std::cout << "Test: Destroy releases all associated resources\n";
    
    // Create multiple plugins and destroy them
    const int num_plugins = 10;
    lsp_android_plugin_handle handles[num_plugins];
    
    // Create plugins
    for (int i = 0; i < num_plugins; ++i) {
        lsp_android_error_code result = plugin_create(&handles[i]);
        assert(result == LSP_ANDROID_SUCCESS);
        assert(handles[i] != nullptr);
    }
    
    // Destroy all plugins
    for (int i = 0; i < num_plugins; ++i) {
        lsp_android_error_code result = plugin_destroy(handles[i]);
        assert(result == LSP_ANDROID_SUCCESS);
    }
    
    std::cout << "  PASSED: Multiple plugins created and destroyed without errors\n";
}

void test_destroy_no_exceptions_across_boundary() {
    std::cout << "Test: Destroy does not throw exceptions across C boundary\n";
    
    // This test verifies that the function is marked extern "C" and
    // handles all exceptions internally
    
    lsp_android_plugin_handle handle = nullptr;
    plugin_create(&handle);
    
    // Call destroy - should not throw
    lsp_android_error_code result = plugin_destroy(handle);
    assert(result == LSP_ANDROID_SUCCESS);
    
    // Call destroy with invalid handle - should not throw
    result = plugin_destroy(nullptr);
    assert(result == LSP_ANDROID_ERROR_INVALID_HANDLE);
    
    std::cout << "  PASSED: No exceptions thrown across C boundary\n";
}

void test_destroy_after_parameter_operations() {
    std::cout << "Test: Destroy after parameter operations\n";
    
    lsp_android_plugin_handle handle = nullptr;
    plugin_create(&handle);
    
    // Set some parameters
    set_param(handle, 0, 12.0f);
    
    float value = 0.0f;
    get_param(handle, 0, &value);
    assert(value == 12.0f);
    
    // Destroy the plugin
    lsp_android_error_code result = plugin_destroy(handle);
    assert(result == LSP_ANDROID_SUCCESS);
    
    std::cout << "  PASSED: Plugin destroyed successfully after parameter operations\n";
}

void test_destroy_after_processing() {
    std::cout << "Test: Destroy after audio processing\n";
    
    lsp_android_plugin_handle handle = nullptr;
    plugin_create(&handle);
    
    // Process some audio
    const int num_frames = 256;
    float input[num_frames];
    float output[num_frames];
    
    for (int i = 0; i < num_frames; ++i) {
        input[i] = 0.5f;
    }
    
    lsp_android_error_code result = process(handle, input, output, num_frames);
    assert(result == LSP_ANDROID_SUCCESS);
    
    // Destroy the plugin
    result = plugin_destroy(handle);
    assert(result == LSP_ANDROID_SUCCESS);
    
    std::cout << "  PASSED: Plugin destroyed successfully after audio processing\n";
}

void test_error_message_retrieval() {
    std::cout << "Test: Error message retrieval for destroy errors\n";
    
    // Test error message for invalid handle
    plugin_destroy(nullptr);
    const char* error_msg = lsp_android_get_error_message(LSP_ANDROID_ERROR_INVALID_HANDLE);
    assert(error_msg != nullptr);
    assert(std::string(error_msg) == "Invalid plugin handle");
    
    std::cout << "  PASSED: Error message correctly retrieved: " << error_msg << "\n";
}

} // namespace

int main() {
    std::cout << "=== Testing plugin_destroy() Implementation ===\n\n";
    
    try {
        test_destroy_valid_handle();
        test_destroy_null_handle();
        test_destroy_sets_error_state();
        test_destroy_releases_resources();
        test_destroy_no_exceptions_across_boundary();
        test_destroy_after_parameter_operations();
        test_destroy_after_processing();
        test_error_message_retrieval();
        
        std::cout << "\n=== All Tests PASSED ===\n";
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "\n=== Test FAILED with exception: " << e.what() << " ===\n";
        return 1;
    } catch (...) {
        std::cerr << "\n=== Test FAILED with unknown exception ===\n";
        return 1;
    }
}
