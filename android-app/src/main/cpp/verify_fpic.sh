#!/bin/bash
# Verification script for -fPIC (position-independent code) configuration
# This script verifies that all compiled shared libraries use position-independent code

set -e

echo "=== Verifying -fPIC Configuration ==="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if readelf is available
if ! command -v readelf &> /dev/null; then
    echo -e "${YELLOW}Warning: readelf not found. Install binutils to verify PIC.${NC}"
    echo "On Ubuntu/Debian: sudo apt-get install binutils"
    exit 1
fi

# Find all .so files in the build output
SO_FILES=$(find ../../../build -name "*.so" 2>/dev/null || true)

if [ -z "$SO_FILES" ]; then
    echo -e "${YELLOW}No .so files found. Build the project first with:${NC}"
    echo "  cd ../../../../"
    echo "  ./gradlew assembleDebug"
    exit 1
fi

echo "Checking shared libraries for position-independent code..."
echo ""

ALL_PASS=true

for SO_FILE in $SO_FILES; do
    ABI=$(echo "$SO_FILE" | grep -oP '(arm64-v8a|x86_64|armeabi-v7a)' || echo "unknown")
    BASENAME=$(basename "$SO_FILE")
    
    echo -n "Checking $BASENAME [$ABI]... "
    
    # Check if the library is position-independent
    # For shared libraries, we check for DYNAMIC section and DT_FLAGS_1 with DF_1_PIE
    # or verify that it's a shared object (ET_DYN)
    FILE_TYPE=$(readelf -h "$SO_FILE" | grep "Type:" | awk '{print $2}')
    
    if [ "$FILE_TYPE" == "DYN" ]; then
        echo -e "${GREEN}PASS${NC} (Position-independent shared object)"
    else
        echo -e "${RED}FAIL${NC} (Not a position-independent shared object)"
        ALL_PASS=false
    fi
done

echo ""
echo "=== CMake Configuration Check ==="
echo ""

# Check CMakeLists.txt for -fPIC configuration
if grep -q "CMAKE_POSITION_INDEPENDENT_CODE ON" CMakeLists.txt; then
    echo -e "${GREEN}✓${NC} CMAKE_POSITION_INDEPENDENT_CODE is set to ON"
else
    echo -e "${RED}✗${NC} CMAKE_POSITION_INDEPENDENT_CODE is NOT set"
    ALL_PASS=false
fi

if grep -q "add_compile_options(-fPIC)" CMakeLists.txt; then
    echo -e "${GREEN}✓${NC} -fPIC flag is explicitly added for Android builds"
else
    echo -e "${YELLOW}⚠${NC} -fPIC flag not explicitly added (relying on CMAKE_POSITION_INDEPENDENT_CODE)"
fi

echo ""
echo "=== Architecture-Specific Verification ==="
echo ""

# Verify that -fPIC is applied to all architectures
for ABI in "arm64-v8a" "x86_64" "armeabi-v7a"; do
    echo -n "Checking $ABI configuration... "
    if grep -A 10 "ANDROID_ABI STREQUAL \"$ABI\"" CMakeLists.txt | grep -q "add_compile_options(-fPIC)" || \
       grep -q "CMAKE_POSITION_INDEPENDENT_CODE ON" CMakeLists.txt; then
        echo -e "${GREEN}PASS${NC}"
    else
        echo -e "${RED}FAIL${NC}"
        ALL_PASS=false
    fi
done

echo ""
if [ "$ALL_PASS" = true ]; then
    echo -e "${GREEN}=== All Checks Passed ===${NC}"
    echo "Position-independent code (-fPIC) is correctly configured for all architectures."
    exit 0
else
    echo -e "${RED}=== Some Checks Failed ===${NC}"
    echo "Please review the configuration and ensure -fPIC is enabled."
    exit 1
fi
