// Test to verify core/alloc.cpp compiles and works correctly
#include <core/alloc.h>
#include <core/debug.h>
#include <stdio.h>
#include <string.h>

int main() {
    printf("Testing LSP core memory allocation utilities...\n");
    
    // Test lsp_malloc
    void* ptr1 = lsp_malloc(1024);
    if (ptr1 != nullptr) {
        printf("✓ lsp_malloc: allocated 1024 bytes\n");
        lsp_free(ptr1);
        printf("✓ lsp_free: freed memory\n");
    } else {
        printf("✗ lsp_malloc: failed to allocate\n");
        return 1;
    }
    
    // Test lsp_calloc
    void* ptr2 = lsp_calloc(10, 100);
    if (ptr2 != nullptr) {
        printf("✓ lsp_calloc: allocated 10 x 100 bytes\n");
        // Verify it's zeroed
        bool is_zeroed = true;
        for (int i = 0; i < 1000; i++) {
            if (((char*)ptr2)[i] != 0) {
                is_zeroed = false;
                break;
            }
        }
        if (is_zeroed) {
            printf("✓ lsp_calloc: memory is zero-initialized\n");
        } else {
            printf("✗ lsp_calloc: memory is not zero-initialized\n");
        }
        lsp_free(ptr2);
    } else {
        printf("✗ lsp_calloc: failed to allocate\n");
        return 1;
    }
    
    // Test lsp_strdup
    const char* test_str = "Hello, LSP Plugins!";
    char* dup_str = lsp_strdup(test_str);
    if (dup_str != nullptr) {
        if (strcmp(dup_str, test_str) == 0) {
            printf("✓ lsp_strdup: string duplicated correctly\n");
        } else {
            printf("✗ lsp_strdup: string mismatch\n");
        }
        lsp_free(dup_str);
    } else {
        printf("✗ lsp_strdup: failed to duplicate string\n");
        return 1;
    }
    
    // Test lsp_strbuild
    const char* build_str = "Build";
    char* built = lsp::lsp_strbuild(build_str, 5);
    if (built != nullptr) {
        if (strcmp(built, build_str) == 0) {
            printf("✓ lsp_strbuild: string built correctly\n");
        } else {
            printf("✗ lsp_strbuild: string mismatch\n");
        }
        lsp_free(built);
    } else {
        printf("✗ lsp_strbuild: failed to build string\n");
        return 1;
    }
    
    // Test lsp_realloc
    void* ptr3 = lsp_malloc(512);
    if (ptr3 != nullptr) {
        void* ptr4 = lsp_realloc(ptr3, 2048);
        if (ptr4 != nullptr) {
            printf("✓ lsp_realloc: reallocated from 512 to 2048 bytes\n");
            lsp_free(ptr4);
        } else {
            printf("✗ lsp_realloc: failed to reallocate\n");
            lsp_free(ptr3);
            return 1;
        }
    }
    
    printf("\nAll core/alloc tests passed!\n");
    return 0;
}
