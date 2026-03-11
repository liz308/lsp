package com.example.lspandroid.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Preset data classes and PresetManager.
 * Tests preset creation, validation, and round-trip serialization.
 */
class PresetTest {

    @Test
    fun testPresetCreation() {
        val parameters = mapOf(0 to 12.5f, 1 to 1000.0f, 2 to 0.5f)
        val preset = PresetManager.createPreset(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Bright",
            parameters = parameters
        )

        assertEquals("test_eq", preset.pluginId)
        assertEquals("1.0.0", preset.pluginVersion)
        assertEquals("Bright", preset.presetName)
        assertEquals(3, preset.parameters.size)
        assertEquals(12.5f, preset.parameters[0], 0.001f)
        assertEquals(1000.0f, preset.parameters[1], 0.001f)
        assertEquals(0.5f, preset.parameters[2], 0.001f)
    }

    @Test
    fun testPresetVersionCompatibility() {
        val preset = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Test",
            parameters = emptyMap()
        )

        assertTrue(preset.isCompatibleWith("1.0.0"))
        assertFalse(preset.isCompatibleWith("1.0.1"))
        assertFalse(preset.isCompatibleWith("2.0.0"))
    }

    @Test
    fun testPresetValidation() {
        val validPreset = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Valid",
            parameters = mapOf(0 to 12.5f)
        )
        assertTrue(PresetManager.validatePreset(validPreset))

        // Invalid: empty plugin ID
        val invalidPluginId = PresetData(
            pluginId = "",
            pluginVersion = "1.0.0",
            presetName = "Invalid",
            parameters = emptyMap()
        )
        assertFalse(PresetManager.validatePreset(invalidPluginId))

        // Invalid: empty preset name
        val invalidName = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "",
            parameters = emptyMap()
        )
        assertFalse(PresetManager.validatePreset(invalidName))

        // Invalid: NaN value
        val invalidNaN = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Invalid",
            parameters = mapOf(0 to Float.NaN)
        )
        assertFalse(PresetManager.validatePreset(invalidNaN))

        // Invalid: Infinity value
        val invalidInfinity = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Invalid",
            parameters = mapOf(0 to Float.POSITIVE_INFINITY)
        )
        assertFalse(PresetManager.validatePreset(invalidInfinity))

        // Invalid: future timestamp
        val invalidTimestamp = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Invalid",
            timestamp = System.currentTimeMillis() + 1000000,
            parameters = emptyMap()
        )
        assertFalse(PresetManager.validatePreset(invalidTimestamp))
    }

    @Test
    fun testRoundTripValidation() {
        val original = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Test",
            parameters = mapOf(0 to 12.5f, 1 to 1000.0f)
        )

        // Identical preset should pass round-trip
        val identical = original.copy()
        assertTrue(PresetManager.validateRoundTrip(original, identical))

        // Different plugin ID should fail
        val differentId = original.copy(pluginId = "different_eq")
        assertFalse(PresetManager.validateRoundTrip(original, differentId))

        // Different version should fail
        val differentVersion = original.copy(pluginVersion = "2.0.0")
        assertFalse(PresetManager.validateRoundTrip(original, differentVersion))

        // Different preset name should fail
        val differentName = original.copy(presetName = "Different")
        assertFalse(PresetManager.validateRoundTrip(original, differentName))

        // Different parameters should fail
        val differentParams = original.copy(parameters = mapOf(0 to 13.0f, 1 to 1000.0f))
        assertFalse(PresetManager.validateRoundTrip(original, differentParams))

        // Missing parameter should fail
        val missingParam = original.copy(parameters = mapOf(0 to 12.5f))
        assertFalse(PresetManager.validateRoundTrip(original, missingParam))

        // Extra parameter should fail
        val extraParam = original.copy(parameters = mapOf(0 to 12.5f, 1 to 1000.0f, 2 to 0.5f))
        assertFalse(PresetManager.validateRoundTrip(original, extraParam))

        // Slightly different parameter value (within tolerance) should pass
        val slightlyDifferent = original.copy(
            parameters = mapOf(0 to 12.5000001f, 1 to 1000.0f)
        )
        assertTrue(PresetManager.validateRoundTrip(original, slightlyDifferent))
    }

    @Test
    fun testFormattedTimestamp() {
        val preset = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Test",
            timestamp = 1000000000000L,  // Fixed timestamp
            parameters = emptyMap()
        )

        val formatted = preset.getFormattedTimestamp()
        assertNotNull(formatted)
        assertFalse(formatted.isEmpty())
    }

    @Test
    fun testPresetParameterImmutability() {
        val originalParams = mutableMapOf(0 to 12.5f, 1 to 1000.0f)
        val preset = PresetManager.createPreset(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Test",
            parameters = originalParams
        )

        // Modify original map
        originalParams[0] = 99.0f

        // Preset should not be affected
        assertEquals(12.5f, preset.parameters[0], 0.001f)
    }

    @Test
    fun testEmptyPreset() {
        val emptyPreset = PresetData(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Empty",
            parameters = emptyMap()
        )

        assertTrue(PresetManager.validatePreset(emptyPreset))
        assertEquals(0, emptyPreset.parameters.size)
    }

    @Test
    fun testLargeParameterSet() {
        // Create a preset with many parameters
        val largeParams = mutableMapOf<Int, Float>()
        for (i in 0 until 100) {
            largeParams[i] = i.toFloat()
        }

        val preset = PresetManager.createPreset(
            pluginId = "test_eq",
            pluginVersion = "1.0.0",
            presetName = "Large",
            parameters = largeParams
        )

        assertTrue(PresetManager.validatePreset(preset))
        assertEquals(100, preset.parameters.size)

        // Verify round-trip
        val copy = preset.copy()
        assertTrue(PresetManager.validateRoundTrip(preset, copy))
    }
}
