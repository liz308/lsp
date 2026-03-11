#!/bin/bash

# Comprehensive JNI Exception Boundary Verification Script
# Checks that no C++ exceptions cross the JNI boundary

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

VIOLATIONS=0
WARNINGS=0

echo "=== JNI Exception Boundary Verification ==="
echo ""

# Find all JNI implementation files
JNI_FILES=$(find . -type f \( -name "*.cpp" -o -name "*.cc" -o -name "*.cxx" \) | grep -i jni || true)

if [ -z "$JNI_FILES" ]; then
    echo -e "${YELLOW}Warning: No JNI files found${NC}"
    exit 0
fi

echo "Scanning JNI files for exception boundary violations..."
echo ""

# Check 1: JNIEXPORT functions without try-catch blocks
echo "Check 1: Verifying JNIEXPORT functions have exception handling..."
for file in $JNI_FILES; do
    # Find JNIEXPORT function definitions
    grep -n "JNIEXPORT" "$file" | while read -r line; do
        line_num=$(echo "$line" | cut -d: -f1)
        
        # Extract function body (simplified check)
        # Look for try-catch within reasonable distance (next 50 lines)
        end_line=$((line_num + 50))
        
        if ! sed -n "${line_num},${end_line}p" "$file" | grep -q "try\s*{"; then
            echo -e "${RED}VIOLATION${NC}: $file:$line_num - JNIEXPORT function may lack try-catch block"
            ((VIOLATIONS++))
        fi
    done
done

# Check 2: Throw statements in JNI code
echo ""
echo "Check 2: Detecting C++ throw statements in JNI code..."
for file in $JNI_FILES; do
    grep -n "throw\s" "$file" | grep -v "nothrow" | grep -v "//" | while read -r line; do
        line_num=$(echo "$line" | cut -d: -f1)
        
        # Check if throw is within a try block that's caught before JNI boundary
        # This is a heuristic check
        echo -e "${YELLOW}WARNING${NC}: $file:$line_num - C++ throw detected, verify it's caught before JNI boundary"
        ((WARNINGS++))
    done
done

# Check 3: Functions that might throw without noexcept
echo ""
echo "Check 3: Checking for potentially throwing operations..."
for file in $JNI_FILES; do
    # Look for common throwing operations: new, vector operations, string operations
    grep -n -E "(new\s+[A-Za-z]|\.push_back\(|\.at\(|std::string)" "$file" | while read -r line; do
        line_num=$(echo "$line" | cut -d: -f1)
        context=$(echo "$line" | cut -d: -f2-)
        
        # Check if within try-catch
        start_line=$((line_num - 20 > 0 ? line_num - 20 : 1))
        end_line=$((line_num + 20))
        
        if ! sed -n "${start_line},${end_line}p" "$file" | grep -q "try\s*{"; then
            echo -e "${YELLOW}WARNING${NC}: $file:$line_num - Potentially throwing operation outside try-catch: ${context:0:60}"
            ((WARNINGS++))
        fi
    done
done

# Check 4: Missing catch(...) blocks
echo ""
echo "Check 4: Verifying catch-all handlers..."
for file in $JNI_FILES; do
    grep -n "JNIEXPORT" "$file" | while read -r line; do
        line_num=$(echo "$line" | cut -d: -f1)
        end_line=$((line_num + 100))
        
        # Check if there's a catch(...) block
        if ! sed -n "${line_num},${end_line}p" "$file" | grep -q "catch\s*(\.\.\.)\s*{"; then
            echo -e "${RED}VIOLATION${NC}: $file:$line_num - JNIEXPORT function missing catch(...) handler"
            ((VIOLATIONS++))
        fi
    done
done

# Check 5: Verify exception translation to Java exceptions
echo ""
echo "Check 5: Checking for proper Java exception throwing..."
for file in $JNI_FILES; do
    # Find catch blocks
    grep -n "catch\s*(" "$file" | while read -r line; do
        line_num=$(echo "$line" | cut -d: -f1)
        end_line=$((line_num + 20))
        
        # Check if JNI ThrowNew or similar is called
        if ! sed -n "${line_num},${end_line}p" "$file" | grep -qE "(ThrowNew|Throw|ExceptionCheck|ExceptionOccurred)"; then
            echo -e "${YELLOW}WARNING${NC}: $file:$line_num - catch block may not translate to Java exception"
            ((WARNINGS++))
        fi
    done
done

# Check 6: Destructors that might throw
echo ""
echo "Check 6: Checking for throwing destructors..."
for file in $JNI_FILES; do
    grep -n "~[A-Za-z_][A-Za-z0-9_]*\s*(" "$file" | while read -r line; do
        line_num=$(echo "$line" | cut -d: -f1)
        end_line=$((line_num + 30))
        
        # Check if destructor has noexcept or try-catch
        if ! sed -n "${line_num},${end_line}p" "$file" | grep -qE "(noexcept|try\s*{)"; then
            echo -e "${YELLOW}WARNING${NC}: $file:$line_num - Destructor should be noexcept or have exception handling"
            ((WARNINGS++))
        fi
    done
done

# Summary
echo ""
echo "=== Verification Summary ==="
echo -e "Files scanned: $(echo "$JNI_FILES" | wc -l)"
echo -e "${RED}Violations: $VIOLATIONS${NC}"
echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
echo ""

if [ $VIOLATIONS -gt 0 ]; then
    echo -e "${RED}FAILED: Found $VIOLATIONS critical violations${NC}"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}PASSED with warnings: Review $WARNINGS potential issues${NC}"
    exit 0
else
    echo -e "${GREEN}PASSED: No violations detected${NC}"
    exit 0
fi
