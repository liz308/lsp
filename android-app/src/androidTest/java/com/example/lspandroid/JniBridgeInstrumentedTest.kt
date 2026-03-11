package com.example.lspandroid

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Instrumented test for JNI bridge on arm64-v8a physical devices
 * 
 * These tests run on the device and validate:
 * - Native library loading
 * - JNI method signatures
 * - Parameter passing
 * - Exception safety
 * - Performance characteristics
 * 
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class JniBridgeInstrumentedTest {

    companion object {
        init {
            System.loadLibrary("lsp_audio_engine")
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

    @Before
    fun setUp() {
        // Log device information
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        android.util.Log.i("JniBridgeTest", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        android.util.Log.i("JniBridgeTest", "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        android.util.Log.i("JniBridgeTest", "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
        android.util.Log.i("JniBridgeTest", "All ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
    }

    @Test
    fun testDeviceIsArm64() {
        // Verify we're running on arm64-v8a
        val primaryAbi = Build.SUPPORTED_ABIS[0]
        assertTrue(
            "Test should run on arm64-v8a device, but running on $primaryAbi",
            Build.SUPPORTED_ABIS.contains("arm64-v8a")
        )
    }

    @Test
    fun testLibraryLoaded() {
        // If we got here, the library loaded successfully in the companion object
        assertTrue("Native library should be loaded", true)
    }

    @Test
    fun testEngineInitialization() {
        val result = nativeInitializeEngine(48000, 256)
        assertTrue("Engine should initialize successfully", result)
    }

    @Test
    fun testEngineInitializationWithDifferentParameters() {
        // Test various valid configurations
        assertTrue(nativeInitializeEngine(44100, 128))
        assertTrue(nativeInitializeEngine(48000, 256))
        assertTrue(nativeInitializeEngine(96000, 512))
    }

    @Test
    fun testSetMasterVolume() {
        nativeInitializeEngine(48000, 256)
        val result = nativeSetMasterVolume(0.75f)
        assertTrue("Should be able to set master volume", result)
    }

    @Test
    fun testGetMasterVolume() {
        nativeInitializeEngine(48000, 256)
        nativeSetMasterVolume(0.75f)
        val volume = nativeGetMasterVolume()
        assertEquals("Volume should match set value", 0.75f, volume, 0.01f)
    }

    @Test
    fun testVolumeRange() {
        nativeInitializeEngine(48000, 256)
        
        val testVolumes = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f)
        for (volume in testVolumes) {
            assertTrue("Should set volume $volume", nativeSetMasterVolume(volume))
            val retrieved = nativeGetMasterVolume()
            assertEquals("Volume should match", volume, retrieved, 0.01f)
        }
    }

    @Test
    fun testSetEqualizerBand() {
        nativeInitializeEngine(48000, 256)
        
        for (band in 0 until 10) {
            val result = nativeSetEqualizerBand(band, 6.0f)
            assertTrue("Should set EQ band $band", result)
        }
    }

    @Test
    fun testGetEqualizerBand() {
        nativeInitializeEngine(48000, 256)
        
        for (band in 0 until 10) {
            nativeSetEqualizerBand(band, 6.0f)
            val gain = nativeGetEqualizerBand(band)
            assertEquals("EQ band $band should match", 6.0f, gain, 0.01f)
        }
    }

    @Test
    fun testEqualizerBandRange() {
        nativeInitializeEngine(48000, 256)
        
        val testGains = listOf(-24.0f, -12.0f, -6.0f, 0.0f, 6.0f, 12.0f, 24.0f)
        
        for (band in 0 until 10) {
            for (gain in testGains) {
                assertTrue("Should set band $band to $gain dB", nativeSetEqualizerBand(band, gain))
                val retrieved = nativeGetEqualizerBand(band)
                assertEquals("Band $band gain should match", gain, retrieved, 0.01f)
            }
        }
    }

    @Test
    fun testBypassControl() {
        nativeInitializeEngine(48000, 256)
        
        assertTrue("Should enable bypass", nativeSetBypass(true))
        assertTrue("Should disable bypass", nativeSetBypass(false))
    }

    @Test
    fun testStartStopAudio() {
        nativeInitializeEngine(48000, 256)
        
        assertTrue("Should start audio", nativeStartEqTest())
        assertTrue("Engine should be running", nativeIsEngineRunning())
        
        assertTrue("Should stop audio", nativeStopEqTest())
        assertFalse("Engine should not be running", nativeIsEngineRunning())
    }

    @Test
    fun testExceptionSafetyInvalidBandIndex() {
        nativeInitializeEngine(48000, 256)
        
        // These should not crash, even with invalid parameters
        try {
            nativeSetEqualizerBand(-1, 0.0f)
            nativeSetEqualizerBand(100, 0.0f)
            nativeGetEqualizerBand(-1)
            nativeGetEqualizerBand(100)
            // If we got here without exception, test passes
            assertTrue("No exception should cross JNI boundary", true)
        } catch (e: Exception) {
            fail("Exception should not cross JNI boundary: ${e.message}")
        }
    }

    @Test
    fun testExceptionSafetyInvalidVolume() {
        nativeInitializeEngine(48000, 256)
        
        // Test with extreme values - should not crash
        try {
            nativeSetMasterVolume(-100.0f)
            nativeSetMasterVolume(1000.0f)
            nativeSetMasterVolume(Float.NaN)
            nativeSetMasterVolume(Float.POSITIVE_INFINITY)
            nativeSetMasterVolume(Float.NEGATIVE_INFINITY)
            assertTrue("No exception should cross JNI boundary", true)
        } catch (e: Exception) {
            fail("Exception should not cross JNI boundary: ${e.message}")
        }
    }

    @Test
    fun testPerformanceJniCallOverhead() {
        nativeInitializeEngine(48000, 256)
        
        val iterations = 1000
        val startTime = System.nanoTime()
        
        for (i in 0 until iterations) {
            nativeSetEqualizerBand(i % 10, (i % 24 - 12).toFloat())
            nativeGetEqualizerBand(i % 10)
        }
        
        val endTime = System.nanoTime()
        val avgTimeUs = (endTime - startTime) / (iterations * 2) / 1000
        
        android.util.Log.i("JniBridgeTest", "Average JNI call time: ${avgTimeUs}μs")
        
        // Should be less than 100 microseconds per call on modern devices
        assertTrue(
            "JNI call overhead should be < 100μs, got ${avgTimeUs}μs",
            avgTimeUs < 100
        )
    }

    @Test
    fun testConcurrentParameterUpdates() {
        nativeInitializeEngine(48000, 256)
        
        // Rapidly update multiple parameters
        for (i in 0 until 100) {
            nativeSetMasterVolume((i % 20) / 20.0f)
            nativeSetEqualizerBand(i % 10, (i % 24 - 12).toFloat())
            nativeSetBypass(i % 2 == 0)
        }
        
        // Verify final state is consistent
        val volume = nativeGetMasterVolume()
        assertTrue("Volume should be valid", volume >= 0.0f && volume <= 2.0f)
        
        for (band in 0 until 10) {
            val gain = nativeGetEqualizerBand(band)
            assertTrue("EQ gain should be valid", gain >= -24.0f && gain <= 24.0f)
        }
    }

    @Test
    fun testEngineLifecycle() {
        // Test multiple init/cleanup cycles
        for (cycle in 0 until 5) {
            assertTrue("Cycle $cycle: Init should succeed", nativeInitializeEngine(48000, 256))
            assertTrue("Cycle $cycle: Start should succeed", nativeStartEqTest())
            assertTrue("Cycle $cycle: Stop should succeed", nativeStopEqTest())
            nativeCleanup()
        }
    }

    @Test
    fun testParameterPersistenceAcrossStartStop() {
        nativeInitializeEngine(48000, 256)
        
        // Set parameters
        nativeSetMasterVolume(0.8f)
        nativeSetEqualizerBand(5, 10.0f)
        
        // Start and stop audio
        nativeStartEqTest()
        nativeStopEqTest()
        
        // Verify parameters are still set
        assertEquals("Volume should persist", 0.8f, nativeGetMasterVolume(), 0.01f)
        assertEquals("EQ should persist", 10.0f, nativeGetEqualizerBand(5), 0.01f)
    }

    @Test
    fun testAllEqualizerBandsIndependent() {
        nativeInitializeEngine(48000, 256)
        
        // Set each band to a unique value
        for (band in 0 until 10) {
            nativeSetEqualizerBand(band, band.toFloat() * 2.0f)
        }
        
        // Verify each band retained its value
        for (band in 0 until 10) {
            val expected = band.toFloat() * 2.0f
            val actual = nativeGetEqualizerBand(band)
            assertEquals("Band $band should be independent", expected, actual, 0.01f)
        }
    }

    @Test
    fun testZeroInitialization() {
        nativeInitializeEngine(48000, 256)
        
        // After init, all EQ bands should be at 0 dB
        for (band in 0 until 10) {
            val gain = nativeGetEqualizerBand(band)
            assertEquals("Band $band should start at 0 dB", 0.0f, gain, 0.01f)
        }
        
        // Master volume should be at 1.0
        val volume = nativeGetMasterVolume()
        assertEquals("Master volume should start at 1.0", 1.0f, volume, 0.01f)
    }
}
