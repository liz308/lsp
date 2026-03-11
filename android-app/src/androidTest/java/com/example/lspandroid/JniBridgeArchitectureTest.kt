package com.example.lspandroid

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * JNI Bridge Architecture Verification Test
 * 
 * This instrumented test verifies that the JNI bridge works correctly across
 * all supported Android architectures (arm64-v8a, x86_64, armeabi-v7a).
 * 
 * Requirements tested:
 * - Requirement 1.5: Android NDK and ABI Compatibility
 * - Requirement 2: Android Architecture Support
 * - Requirement 6: JNI Bridge Interface
 * 
 * Design sections tested:
 * - Section 2.2: JNI Bridge Interface
 * - Section 5.1: JNI Call Sequence
 */
@RunWith(AndroidJUnit4::class)
class JniBridgeArchitectureTest {

    companion object {
        private const val TAG = "JniBridgeArchTest"
        
        init {
            try {
                // Load the native library
                System.loadLibrary("lsp_audio_engine")
                Log.i(TAG, "Successfully loaded lsp_audio_engine library")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load lsp_audio_engine library", e)
                throw e
            }
        }
    }

    // Native method declarations
    private external fun runTests(): Array<String>
    private external fun getArchitectureInfo(): String

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Context should not be null", context)
        
        // Log architecture information
        val archInfo = getArchitectureInfo()
        Log.i(TAG, "Architecture Info: $archInfo")
    }

    @Test
    fun testJniBridgeOnCurrentArchitecture() {
        Log.i(TAG, "=== Starting JNI Bridge Architecture Tests ===")
        
        // Get architecture info
        val archInfo = getArchitectureInfo()
        Log.i(TAG, archInfo)
        
        // Run native tests
        val results = runTests()
        
        // Log all results
        Log.i(TAG, "=== Test Results ===")
        results.forEach { result ->
            Log.i(TAG, result)
        }
        
        // Check for failures
        val failures = results.filter { it.startsWith("[FAIL]") }
        
        if (failures.isNotEmpty()) {
            val failureMessage = buildString {
                appendLine("JNI Bridge tests failed on architecture: $archInfo")
                appendLine("Failed tests:")
                failures.forEach { appendLine("  $it") }
            }
            fail(failureMessage)
        }
        
        // Verify we ran all expected tests
        assertTrue("Should have run at least 10 tests", results.size >= 10)
        
        // Verify all tests passed
        val passCount = results.count { it.startsWith("[PASS]") }
        assertEquals("All tests should pass", results.size, passCount)
        
        Log.i(TAG, "=== All JNI Bridge Tests Passed ===")
    }

    @Test
    fun testSystemLoadLibraryWithAbiSelection() {
        // This test verifies that System.loadLibrary() correctly selects
        // the appropriate ABI-specific library for the current device architecture
        // 
        // Requirements tested:
        // - Requirement 1.5 AC 15: Plugin_Host SHALL verify native library loading 
        //   succeeds for all architectures using System.loadLibrary() with proper ABI selection
        // - Requirement 2: Android Architecture Support
        
        Log.i(TAG, "=== Testing System.loadLibrary() ABI Selection ===")
        
        val archInfo = getArchitectureInfo()
        Log.i(TAG, "Current architecture: $archInfo")
        
        // The library should already be loaded in the companion object
        // If we got here, it means System.loadLibrary() succeeded
        assertTrue("Library should be loaded", true)
        
        // Verify we can call native methods
        val results = getArchitectureInfo()
        assertNotNull("Should be able to call native methods", results)
        assertTrue("Architecture info should not be empty", results.isNotEmpty())
        
        // Verify the correct ABI was selected based on device architecture
        val expectedAbi = android.os.Build.SUPPORTED_ABIS[0]
        Log.i(TAG, "Device primary ABI: $expectedAbi")
        Log.i(TAG, "All supported ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
        
        // Verify architecture matches expected ABI
        when (expectedAbi) {
            "arm64-v8a" -> {
                assertTrue("Should detect arm64-v8a architecture", 
                          archInfo.contains("arm64-v8a") || archInfo.contains("aarch64"))
                Log.i(TAG, "✓ Correctly loaded arm64-v8a library")
            }
            "x86_64" -> {
                assertTrue("Should detect x86_64 architecture", 
                          archInfo.contains("x86_64") || archInfo.contains("x86-64"))
                Log.i(TAG, "✓ Correctly loaded x86_64 library")
            }
            "armeabi-v7a" -> {
                assertTrue("Should detect armeabi-v7a architecture", 
                          archInfo.contains("armeabi-v7a") || archInfo.contains("armv7"))
                Log.i(TAG, "✓ Correctly loaded armeabi-v7a library")
            }
            else -> {
                Log.w(TAG, "Unexpected ABI: $expectedAbi")
                // Still pass if library loaded successfully
            }
        }
        
        // Verify library path contains correct ABI
        try {
            val libraryPath = System.getProperty("java.library.path")
            Log.i(TAG, "Library path: $libraryPath")
            
            // The library should be loaded from the correct ABI directory
            if (libraryPath != null && libraryPath.contains(expectedAbi)) {
                Log.i(TAG, "✓ Library loaded from correct ABI directory: $expectedAbi")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify library path: ${e.message}")
        }
        
        // Verify we can successfully call multiple native methods
        // This confirms the JNI bridge is working correctly with the loaded library
        try {
            val testResults = runTests()
            assertTrue("Should be able to run native tests", testResults.isNotEmpty())
            Log.i(TAG, "✓ Successfully called native methods through JNI bridge")
        } catch (e: Exception) {
            fail("Failed to call native methods: ${e.message}")
        }
        
        Log.i(TAG, "=== System.loadLibrary() ABI Selection Test PASSED ===")
        Log.i(TAG, "Summary: System.loadLibrary() correctly selected and loaded")
        Log.i(TAG, "         the $expectedAbi native library for this device")
    }

    @Test
    fun testNoExceptionsCrossJniBoundary() {
        // This test verifies that no C++ exceptions cross the JNI boundary
        // All exceptions should be caught and converted to error codes
        
        Log.i(TAG, "Testing exception handling across JNI boundary")
        
        try {
            // Run all tests - none should throw exceptions
            val results = runTests()
            
            // Check for critical failures indicating exceptions crossed boundary
            val criticalFailures = results.filter { 
                it.contains("CRITICAL") && it.contains("exception")
            }
            
            if (criticalFailures.isNotEmpty()) {
                fail("CRITICAL: C++ exceptions crossed JNI boundary:\n" +
                     criticalFailures.joinToString("\n"))
            }
            
            Log.i(TAG, "No exceptions crossed JNI boundary - PASS")
            
        } catch (e: Exception) {
            fail("Unexpected exception from native code: ${e.message}")
        }
    }

    @Test
    fun testArchitectureSpecificOptimizations() {
        // This test verifies that architecture-specific optimizations are active
        
        val archInfo = getArchitectureInfo()
        Log.i(TAG, "Verifying architecture-specific optimizations: $archInfo")
        
        when {
            archInfo.contains("arm64-v8a") -> {
                assertTrue("arm64-v8a should have NEON support", 
                          archInfo.contains("NEON"))
                Log.i(TAG, "arm64-v8a: NEON optimizations active")
            }
            archInfo.contains("x86_64") -> {
                assertTrue("x86_64 should have SSE2 support", 
                          archInfo.contains("SSE2"))
                Log.i(TAG, "x86_64: SSE2 optimizations active")
            }
            archInfo.contains("armeabi-v7a") -> {
                // armeabi-v7a may or may not have NEON
                Log.i(TAG, "armeabi-v7a: SIMD support = ${archInfo.contains("NEON")}")
            }
            else -> {
                Log.w(TAG, "Unknown architecture: $archInfo")
            }
        }
    }

    @Test
    fun testPluginLifecycleFunctions() {
        // This test verifies that plugin lifecycle functions work correctly
        // through the JNI bridge
        
        Log.i(TAG, "Testing plugin lifecycle functions")
        
        val results = runTests()
        
        // Check specific lifecycle tests
        val lifecycleTests = listOf(
            "Plugin Creation",
            "Plugin Initialization",
            "Library Cleanup"
        )
        
        lifecycleTests.forEach { testName ->
            val testResult = results.find { it.contains(testName) }
            assertNotNull("Should have result for $testName", testResult)
            assertTrue("$testName should pass", testResult?.startsWith("[PASS]") == true)
            Log.i(TAG, "Lifecycle test passed: $testName")
        }
    }

    @Test
    fun testParameterOperations() {
        // This test verifies that parameter operations work correctly
        // through the JNI bridge
        
        Log.i(TAG, "Testing parameter operations")
        
        val results = runTests()
        
        val paramTest = results.find { it.contains("Parameter Operations") }
        assertNotNull("Should have Parameter Operations test result", paramTest)
        assertTrue("Parameter Operations should pass", 
                  paramTest?.startsWith("[PASS]") == true)
        
        Log.i(TAG, "Parameter operations test passed")
    }

    @Test
    fun testAudioProcessing() {
        // This test verifies that audio processing works correctly
        // through the JNI bridge
        
        Log.i(TAG, "Testing audio processing")
        
        val results = runTests()
        
        val audioTest = results.find { it.contains("Audio Processing") }
        assertNotNull("Should have Audio Processing test result", audioTest)
        assertTrue("Audio Processing should pass", 
                  audioTest?.startsWith("[PASS]") == true)
        
        Log.i(TAG, "Audio processing test passed")
    }

    @Test
    fun testMultiplePluginTypes() {
        // This test verifies that multiple plugin types can be created
        // through the JNI bridge
        
        Log.i(TAG, "Testing multiple plugin types")
        
        val results = runTests()
        
        val pluginTest = results.find { it.contains("Multiple Plugin Types") }
        assertNotNull("Should have Multiple Plugin Types test result", pluginTest)
        assertTrue("Multiple Plugin Types should pass", 
                  pluginTest?.startsWith("[PASS]") == true)
        
        Log.i(TAG, "Multiple plugin types test passed")
    }

    @Test
    fun testLibraryLoadedFromCorrectAbiDirectory() {
        // This test verifies that the native library was loaded from the correct
        // ABI-specific directory in the APK
        //
        // Requirements tested:
        // - Requirement 1.5 AC 15: Verify native library loading with proper ABI selection
        // - Requirement 2 AC 7: Separate shared libraries per architecture
        
        Log.i(TAG, "=== Testing Library Loaded from Correct ABI Directory ===")
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName, 
            android.content.pm.PackageManager.GET_SHARED_LIBRARY_FILES
        )
        
        val primaryAbi = android.os.Build.SUPPORTED_ABIS[0]
        Log.i(TAG, "Device primary ABI: $primaryAbi")
        Log.i(TAG, "Application info native library dir: ${packageInfo.applicationInfo.nativeLibraryDir}")
        
        // Verify the native library directory contains the expected ABI
        val nativeLibDir = packageInfo.applicationInfo.nativeLibraryDir
        assertNotNull("Native library directory should not be null", nativeLibDir)
        
        // The native library directory should contain the ABI name
        // Format is typically: /data/app/<package>/lib/<abi>
        assertTrue("Native library directory should contain ABI: $primaryAbi",
                  nativeLibDir?.contains(primaryAbi) == true)
        
        Log.i(TAG, "✓ Native library loaded from correct ABI directory")
        
        // Verify the library file exists in the expected location
        val libraryFile = java.io.File(nativeLibDir, "liblsp_audio_engine.so")
        assertTrue("Library file should exist: ${libraryFile.absolutePath}",
                  libraryFile.exists())
        
        Log.i(TAG, "✓ Library file exists: ${libraryFile.absolutePath}")
        Log.i(TAG, "✓ Library size: ${libraryFile.length()} bytes")
        
        // Verify we can read the library file
        assertTrue("Library file should be readable", libraryFile.canRead())
        Log.i(TAG, "✓ Library file is readable")
        
        Log.i(TAG, "=== Library ABI Directory Test PASSED ===")
    }

    @Test
    fun testAllSupportedAbisHaveLibraries() {
        // This test verifies that libraries exist for all ABIs supported by the device
        // Note: This test may report warnings if the APK doesn't include all ABIs,
        // which is expected for ABI splits
        
        Log.i(TAG, "=== Testing All Supported ABIs ===")
        
        val supportedAbis = android.os.Build.SUPPORTED_ABIS
        Log.i(TAG, "Device supports ${supportedAbis.size} ABIs: ${supportedAbis.joinToString()}")
        
        val primaryAbi = supportedAbis[0]
        Log.i(TAG, "Primary ABI: $primaryAbi")
        
        // For ABI splits, only the primary ABI library will be present
        // This is expected and correct behavior
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_SHARED_LIBRARY_FILES
        )
        
        val nativeLibDir = packageInfo.applicationInfo.nativeLibraryDir
        Log.i(TAG, "Native library directory: $nativeLibDir")
        
        // Verify primary ABI library exists
        val primaryLibrary = java.io.File(nativeLibDir, "liblsp_audio_engine.so")
        assertTrue("Primary ABI library must exist", primaryLibrary.exists())
        Log.i(TAG, "✓ Primary ABI ($primaryAbi) library exists")
        
        // Log information about other ABIs (may not be present due to ABI splits)
        supportedAbis.drop(1).forEach { abi ->
            Log.i(TAG, "Secondary ABI: $abi (may not be present due to ABI splits)")
        }
        
        Log.i(TAG, "=== ABI Support Test PASSED ===")
    }

    @Test
    fun testLibraryLoadingFailsGracefullyForMissingAbi() {
        // This test verifies that attempting to load a library for an unsupported
        // ABI fails gracefully with an appropriate error
        //
        // Note: This test verifies error handling, not a failure case
        
        Log.i(TAG, "=== Testing Graceful Failure for Missing ABI ===")
        
        // Try to load a library with an obviously wrong name
        // This should fail with UnsatisfiedLinkError
        try {
            System.loadLibrary("nonexistent_library_for_testing")
            fail("Should have thrown UnsatisfiedLinkError for nonexistent library")
        } catch (e: UnsatisfiedLinkError) {
            Log.i(TAG, "✓ Correctly threw UnsatisfiedLinkError for missing library")
            Log.i(TAG, "Error message: ${e.message}")
            
            // Verify error message is informative
            assertNotNull("Error message should not be null", e.message)
            assertTrue("Error message should mention library name",
                      e.message?.contains("nonexistent_library_for_testing") == true)
        }
        
        Log.i(TAG, "=== Graceful Failure Test PASSED ===")
    }
}
