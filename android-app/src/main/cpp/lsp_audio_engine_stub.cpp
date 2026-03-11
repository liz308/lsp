// Minimal stub for LSP audio engine
// This allows the build to succeed while we port more LSP plugins files

#include "CpuFeatures.h"

extern "C" {
    void lsp_audio_engine_init() {
        // Initialize CPU feature detection
        cpu_features_init();
    }
}
