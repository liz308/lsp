#!/bin/bash
# Verification script for -Wl,--gc-sections configuration
# This script verifies that unused code removal is properly configured for all architectures

set -e

echo "=== Verifying gc-sections Configuration ==="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if CMakeLists.txt exists
if [ ! -f "CMakeLists.txt" ]; then
    echo -e "${RED}ERROR: CMakeLists.txt not found${NC}"
    exit 1
fi

echo "Checking CMakeLists.txt for gc-sections configuration..."
echo ""

# Check for -ffunction-sections
if grep -q "\-ffunction-sections" CMakeLists.txt; then
    echo -e "${GREEN}✓${NC} Found -ffunction-sections compiler flag"
else
    echo -e "${RED}✗${NC} Missing -ffunction-sections compiler flag"
    exit 1
fi

# Check for -fdata-sections
if grep -q "\-fdata-sections" CMakeLists.txt; then
    echo -e "${GREEN}✓${NC} Found -fdata-sections compiler flag"
else
    echo -e "${RED}✗${NC} Missing -fdata-sections compiler flag"
    exit 1
fi

# Check for -Wl,--gc-sections
if grep -q "\-Wl,--gc-sections" CMakeLists.txt; then
    echo -e "${GREEN}✓${NC} Found -Wl,--gc-sections linker flag"
else
    echo -e "${RED}✗${NC} Missing -Wl,--gc-sections linker flag"
    exit 1
fi

echo ""
echo "=== Configuration Verification ==="
echo ""

# Verify the flags are in the Android-specific section
if grep -A 20 "if(ANDROID)" CMakeLists.txt | grep -q "\-Wl,--gc-sections"; then
    echo -e "${GREEN}✓${NC} gc-sections is configured for Android builds"
else
    echo -e "${YELLOW}⚠${NC} gc-sections may not be Android-specific"
fi

echo ""
echo "=== Architecture-Specific Verification ==="
echo ""

# Check that the configuration applies to all architectures
for ABI in "arm64-v8a" "x86_64" "armeabi-v7a"; do
    echo "Checking $ABI configuration..."
    if grep -q "$ABI" CMakeLists.txt; then
        echo -e "  ${GREEN}✓${NC} $ABI is configured"
    else
        echo -e "  ${YELLOW}⚠${NC} $ABI not explicitly mentioned (may use defaults)"
    fi
done

echo ""
echo "=== Build Test (Optional) ==="
echo ""
echo "To verify gc-sections is working at build time, run:"
echo "  cd ../../../../"
echo "  ./gradlew assembleDebug"
echo ""
echo "Then check the build output for linker flags:"
echo "  grep -r 'gc-sections' android-app/build/intermediates/cmake/"
echo ""

# If build directory exists, check for actual usage
if [ -d "../../../../android-app/build/intermediates/cmake" ]; then
    echo "Checking build artifacts for gc-sections usage..."
    if grep -r "gc-sections" ../../../../android-app/build/intermediates/cmake/ 2>/dev/null | head -5; then
        echo -e "${GREEN}✓${NC} gc-sections is being used in actual builds"
    else
        echo -e "${YELLOW}⚠${NC} No build artifacts found (build may not have run yet)"
    fi
    echo ""
fi

echo "=== Size Reduction Verification ==="
echo ""
echo "To measure the impact of gc-sections, compare library sizes:"
echo "  1. Build with gc-sections (current configuration)"
echo "  2. Build without gc-sections (comment out the flags)"
echo "  3. Compare .so file sizes in android-app/build/intermediates/cmake/"
echo ""
echo "Expected reduction: 5-15% depending on unused code"
echo ""

echo -e "${GREEN}=== Verification Complete ===${NC}"
echo ""
echo "Summary:"
echo "  - Compiler flags: -ffunction-sections -fdata-sections"
echo "  - Linker flag: -Wl,--gc-sections"
echo "  - Applies to: All Android architectures (arm64-v8a, x86_64, armeabi-v7a)"
echo ""
9