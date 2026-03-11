// Simple test to verify dsp/alloc.h compiles
#include <dsp/alloc.h>

int main() {
    // Test allocation functions
    void* ptr = dsp::alloc_aligned(1024, 16);
    if (ptr != nullptr) {
        dsp::free_aligned(ptr);
    }
    
    // Test float allocation
    float* floats = dsp::alloc_aligned_floats(256, 16);
    if (floats != nullptr) {
        dsp::free_aligned(floats);
    }
    
    // Test default alignment
    void* default_ptr = dsp::alloc_aligned_default(512);
    if (default_ptr != nullptr) {
        dsp::free_aligned(default_ptr);
    }
    
    return 0;
}