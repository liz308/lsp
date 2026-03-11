#!/bin/bash

# Verification script for x86_64 test harness
# Checks that all required files are present and properly configured

set -e

echo "=== Verifying x86_64 Test Harness Setup ==="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# Check function
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} Found: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Missing: $1"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

check_executable() {
    if [ -x "$1" ]; then
        echo -e "${GREEN}✓${NC} Executable: $1"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} Not executable: $1"
        WARNINGS=$((WARNINGS + 1))
        return 1
    fi
}

# Check required source files
echo "Checking source files..."
check_file "test_harness_x86_64.cpp"
check_file "build_test_harness_x86_64.sh"
check_file "lsp-plugins/src/dsp/compare.cpp"
check_file "lsp-plugins/src/core/alloc.cpp"
check_file "lsp-plugins/src/core/debug.cpp"
check_file "lsp-plugins/include/dsp/dsp.h"
echo ""

# Check build script is executable
echo "Checking build script permissions..."
check_executable "build_test_harness_x86_64.sh"
echo ""

# Check for NDK
echo "Checking Android NDK..."
if [ -n "$ANDROID_NDK_HOME" ] && [ -d "$ANDROID_NDK_HOME" ]; then
    echo -e "${GREEN}✓${NC} ANDROID_NDK_HOME set: $ANDROID_NDK_HOME"
    
    # Check for x86_64 toolchain
    if [ -d "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" ]; then
        echo -e "${GREEN}✓${NC} NDK toolchain found"
    else
        echo -e "${RED}✗${NC} NDK toolchain not found"
        ERRORS=$((ERRORS + 1))
    fi
elif [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/ndk-bundle" ]; then
    echo -e "${GREEN}✓${NC} ANDROID_HOME set with NDK bundle: $ANDROID_HOME/ndk-bundle"
else
    echo -e "${YELLOW}⚠${NC} ANDROID_NDK_HOME not set (required for building)"
    echo "  Set with: export ANDROID_NDK_HOME=/path/to/ndk"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Check if test harness was already built
echo "Checking for built test harness..."
if [ -f "build_test_harness_x86_64/test-harness-x86_64" ]; then
    echo -e "${GREEN}✓${NC} Test harness binary exists"
    
    # Check if it's executable
    if [ -x "build_test_harness_x86_64/test-harness-x86_64" ]; then
        echo -e "${GREEN}✓${NC} Test harness is executable"
        
        # Get file info
        FILE_SIZE=$(stat -f%z "build_test_harness_x86_64/test-harness-x86_64" 2>/dev/null || stat -c%s "build_test_harness_x86_64/test-harness-x86_64" 2>/dev/null || echo "unknown")
        echo "  Binary size: $FILE_SIZE bytes"
        
        # Check if it's an ELF binary
        if command -v file &> /dev/null; then
            FILE_TYPE=$(file "build_test_harness_x86_64/test-harness-x86_64")
            echo "  File type: $FILE_TYPE"
            
            if echo "$FILE_TYPE" | grep -q "x86-64"; then
                echo -e "${GREEN}✓${NC} Correct architecture: x86_64"
            else
                echo -e "${RED}✗${NC} Wrong architecture (expected x86_64)"
                ERRORS=$((ERRORS + 1))
            fi
        fi
    else
        echo -e "${YELLOW}⚠${NC} Test harness exists but is not executable"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠${NC} Test harness not built yet"
    echo "  Run: ./build_test_harness_x86_64.sh"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Check for documentation
echo "Checking documentation..."
check_file "README_TEST_HARNESS_X86_64.md"
echo ""

# Check for related test files
echo "Checking related test files..."
check_file "test_dsp_compare.cpp"
check_file "test_dsp_delay.cpp"
echo ""

# Summary
echo "=== Verification Summary ==="
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "x86_64 test harness is ready to build."
    echo ""
    echo "Next steps:"
    echo "  1. Build: ./build_test_harness_x86_64.sh"
    echo "  2. Start x86_64 emulator"
    echo "  3. Push binary: adb push build_test_harness_x86_64/test-harness-x86_64 /data/local/tmp/"
    echo "  4. Run: adb shell /data/local/tmp/test-harness-x86_64"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ Verification completed with $WARNINGS warning(s)${NC}"
    echo ""
    echo "The test harness setup is mostly complete, but some optional items are missing."
    exit 0
else
    echo -e "${RED}✗ Verification failed with $ERRORS error(s) and $WARNINGS warning(s)${NC}"
    echo ""
    echo "Please fix the errors above before building the test harness."
    exit 1
fi
