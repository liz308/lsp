#include "lsp_android_bridge.h"

// Minimal Android bridge implementation for LSP plugins
// This is a stub implementation to allow the build to succeed

extern "C" {
    // Minimal JNI bridge functions
    void* lsp_android_create_engine() {
        return nullptr;
    }
    
    void lsp_android_destroy_engine(void* engine) {
        // No-op
    }
    
    int lsp_android_process_audio(void* engine, float* input, float* output, int frames) {
        // Minimal pass-through
        if (input && output && frames > 0) {
            for (int i = 0; i < frames; ++i) {
                output[i] = input[i];
            }
        }
        return 0;
    }
}
