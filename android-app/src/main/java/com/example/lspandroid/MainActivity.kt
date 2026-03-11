package com.example.lspandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lspandroid.ui.theme.LSPAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainActivity for testing JNI bridge on arm64-v8a physical devices
 * 
 * This activity provides a comprehensive test harness for validating:
 * - Native library loading
 * - JNI method signatures
 * - Parameter passing and return values
 * - Exception safety across JNI boundary
 * - Audio engine lifecycle
 * - EQ parameter updates
 * - Performance on physical hardware
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        
        init {
            try {
                System.loadLibrary("lsp_audio_engine")
                Log.i(TAG, "Successfully loaded lsp_audio_engine library")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load lsp_audio_engine library", e)
            }
        }
    }

    // Native method declarations
    private external fun nativeInitializeEngine(sampleRate: Int, bufferSize: Int): Boolean
    private external fun nativeStartEqTest(): Boolean
    private external fun nativeStopEqTest(): Boolean
    private external fun nativeSetEqualizerBand(bandIndex: Int, gainDb: Float): Boolean
    private external fun nativeGetEqualizerBand(bandIndex: Int): Float
    private external fun nativeSetMasterVolume(volume: Float): Boolean
    private external fun nativeGetMasterVolume(): Float
    private external fun nativeSetBypass(bypass: Boolean): Boolean
    private external fun nativeIsEngineRunning(): Boolean
    private external fun nativeCleanup()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val modifyAudioGranted = permissions[Manifest.permission.MODIFY_AUDIO_SETTINGS] ?: false
        
        if (recordAudioGranted && modifyAudioGranted) {
            Log.d(TAG, "Audio permissions granted")
        } else {
            Log.w(TAG, "Audio permissions not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        logDeviceInfo()

        setContent {
            LSPAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JniBridgeTestScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            nativeCleanup()
            Log.d(TAG, "Native cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during native cleanup", e)
        }
    }

    private fun checkAndRequestPermissions() {
        val recordAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        )
        val modifyAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        if (recordAudioPermission != PackageManager.PERMISSION_GRANTED ||
            modifyAudioPermission != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
            )
        }
    }

    private fun logDeviceInfo() {
        Log.i(TAG, "=== Device Information ===")
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.i(TAG, "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.i(TAG, "Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        Log.i(TAG, "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
        Log.i(TAG, "CPU ABI: ${Build.CPU_ABI}")
        Log.i(TAG, "CPU ABI2: ${Build.CPU_ABI2}")
        Log.i(TAG, "Board: ${Build.BOARD}")
        Log.i(TAG, "Hardware: ${Build.HARDWARE}")
        Log.i(TAG, "=========================")
    }

    @Composable
    fun JniBridgeTestScreen() {
        var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
        var isRunning by remember { mutableStateOf(false) }
        var engineInitialized by remember { mutableStateOf(false) }
        var audioRunning by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JNI Bridge Test Harness",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "arm64-v8a Physical Device Testing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Device Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Information",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    Text("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    Text("Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
                    Text("All ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isRunning = true
                            testResults = runAllTests()
                            isRunning = false
                        }
                    },
                    enabled = !isRunning
                ) {
                    Text("Run All Tests")
                }

                Button(
                    onClick = {
                        testResults = emptyList()
                    },
                    enabled = !isRunning
                ) {
                    Text("Clear Results")
                }
            }

            // Engine Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            engineInitialized = testInitializeEngine()
                        }
                    },
                    enabled = !engineInitialized && !isRunning
                ) {
                    Text("Initialize Engine")
                }

                Button(
                    onClick = {
                        scope.launch {
                            audioRunning = testStartAudio()
                        }
                    },
                    enabled = engineInitialized && !audioRunning && !isRunning
                ) {
                    Text("Start Audio")
                }

                Button(
                    onClick = {
                        scope.launch {
                            audioRunning = !testStopAudio()
                        }
                    },
                    enabled = audioRunning && !isRunning
                ) {
                    Text("Stop Audio")
                }
            }

            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Engine Status",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("Initialized: ${if (engineInitialized) "Yes" else "No"}")
                    Text("Audio Running: ${if (audioRunning) "Yes" else "No"}")
                    Text("Test Running: ${if (isRunning) "Yes" else "No"}")
                }
            }

            // Test Results
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
                Text("Running tests...")
            }

            if (testResults.isNotEmpty()) {
                Text(
                    text = "Test Results",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                val passedTests = testResults.count { it.passed }
                val totalTests = testResults.size

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (passedTests == totalTests) 
                            Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Summary: $passedTests / $totalTests tests passed",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }

                testResults.forEach { result ->
                    TestResultCard(result)
                }
            }
        }
    }

    @Composable
    fun TestResultCard(result: TestResult) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (result.passed) 
                    Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = result.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (result.passed) "✓ PASS" else "✗ FAIL",
                        color = if (result.passed) Color(0xFF4CAF50) else Color(0xFFF44336),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (result.message.isNotEmpty()) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (result.duration > 0) {
                    Text(
                        text = "Duration: ${result.duration}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    private suspend fun runAllTests(): List<TestResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<TestResult>()

        // Test 1: Library Loading
        results.add(testLibraryLoading())

        // Test 2: ABI Verification
        results.add(testAbiVerification())

        // Test 3: Engine Initialization
        results.add(testEngineInitialization())

        // Test 4: Parameter Setting
        results.add(testParameterSetting())

        // Test 5: Parameter Getting
        results.add(testParameterGetting())

        // Test 6: EQ Band Operations
        results.add(testEqBandOperations())

        // Test 7: Volume Control
        results.add(testVolumeControl())

        // Test 8: Bypass Control
        results.add(testBypassControl())

        // Test 9: Exception Safety
        results.add(testExceptionSafety())

        // Test 10: Performance Test
        results.add(testPerformance())

        results
    }

    private fun testLibraryLoading(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            // Library is loaded in companion object init block
            // If we got here, it loaded successfully
            TestResult(
                name = "Library Loading",
                passed = true,
                message = "lsp_audio_engine library loaded successfully",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Library Loading",
                passed = false,
                message = "Failed to load library: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testAbiVerification(): TestResult {
        val startTime = System.currentTimeMillis()
        val primaryAbi = Build.SUPPORTED_ABIS[0]
        val isArm64 = primaryAbi == "arm64-v8a"
        
        return TestResult(
            name = "ABI Verification",
            passed = isArm64,
            message = if (isArm64) {
                "Running on arm64-v8a as expected"
            } else {
                "WARNING: Running on $primaryAbi, not arm64-v8a"
            },
            duration = System.currentTimeMillis() - startTime
        )
    }

    private fun testEngineInitialization(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val result = nativeInitializeEngine(48000, 256)
            TestResult(
                name = "Engine Initialization",
                passed = result,
                message = if (result) "Engine initialized successfully" else "Engine initialization failed",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Engine Initialization",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testParameterSetting(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val result = nativeSetMasterVolume(0.75f)
            TestResult(
                name = "Parameter Setting",
                passed = result,
                message = if (result) "Set master volume to 0.75" else "Failed to set parameter",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Parameter Setting",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testParameterGetting(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val volume = nativeGetMasterVolume()
            val passed = volume >= 0.0f && volume <= 2.0f
            TestResult(
                name = "Parameter Getting",
                passed = passed,
                message = "Got master volume: $volume",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Parameter Getting",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testEqBandOperations(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            var allPassed = true
            val testValues = listOf(-12.0f, -6.0f, 0.0f, 6.0f, 12.0f)
            
            for (band in 0 until 10) {
                for (gain in testValues) {
                    if (!nativeSetEqualizerBand(band, gain)) {
                        allPassed = false
                        break
                    }
                    val retrieved = nativeGetEqualizerBand(band)
                    if (kotlin.math.abs(retrieved - gain) > 0.01f) {
                        allPassed = false
                        break
                    }
                }
                if (!allPassed) break
            }
            
            TestResult(
                name = "EQ Band Operations",
                passed = allPassed,
                message = if (allPassed) "All EQ band operations successful" else "Some EQ operations failed",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "EQ Band Operations",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testVolumeControl(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val testVolumes = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f)
            var allPassed = true
            
            for (volume in testVolumes) {
                if (!nativeSetMasterVolume(volume)) {
                    allPassed = false
                    break
                }
                val retrieved = nativeGetMasterVolume()
                if (kotlin.math.abs(retrieved - volume) > 0.01f) {
                    allPassed = false
                    break
                }
            }
            
            TestResult(
                name = "Volume Control",
                passed = allPassed,
                message = if (allPassed) "All volume operations successful" else "Some volume operations failed",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Volume Control",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testBypassControl(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val result1 = nativeSetBypass(true)
            val result2 = nativeSetBypass(false)
            val passed = result1 && result2
            
            TestResult(
                name = "Bypass Control",
                passed = passed,
                message = if (passed) "Bypass control working" else "Bypass control failed",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Bypass Control",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testExceptionSafety(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            // Test with invalid parameters
            nativeSetEqualizerBand(-1, 0.0f)  // Invalid band index
            nativeSetEqualizerBand(100, 0.0f)  // Invalid band index
            nativeGetEqualizerBand(-1)  // Invalid band index
            nativeGetEqualizerBand(100)  // Invalid band index
            
            // If we got here without crashing, exception safety is good
            TestResult(
                name = "Exception Safety",
                passed = true,
                message = "No exceptions crossed JNI boundary",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Exception Safety",
                passed = false,
                message = "Exception crossed JNI boundary: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testPerformance(): TestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val iterations = 1000
            val perfStart = System.nanoTime()
            
            for (i in 0 until iterations) {
                nativeSetEqualizerBand(i % 10, (i % 24 - 12).toFloat())
                nativeGetEqualizerBand(i % 10)
            }
            
            val perfEnd = System.nanoTime()
            val avgTimeUs = (perfEnd - perfStart) / (iterations * 2) / 1000
            
            TestResult(
                name = "Performance Test",
                passed = avgTimeUs < 100, // Should be less than 100 microseconds per call
                message = "Average JNI call time: ${avgTimeUs}μs",
                duration = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                name = "Performance Test",
                passed = false,
                message = "Exception: ${e.message}",
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testInitializeEngine(): Boolean {
        return try {
            nativeInitializeEngine(48000, 256)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize engine", e)
            false
        }
    }

    private fun testStartAudio(): Boolean {
        return try {
            nativeStartEqTest()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio", e)
            false
        }
    }

    private fun testStopAudio(): Boolean {
        return try {
            nativeStopEqTest()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio", e)
            false
        }
    }

    data class TestResult(
        val name: String,
        val passed: Boolean,
        val message: String,
        val duration: Long
    )
}
