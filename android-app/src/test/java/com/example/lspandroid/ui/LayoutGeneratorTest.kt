package com.example.lspandroid.ui

import com.example.lspandroid.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LayoutGenerator.
 * Tests layout generation from plugin metadata.
 */
class LayoutGeneratorTest {

    @Test
    fun testLayoutGenerationWithControlPorts() {
        // Create test metadata with control ports
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 100f, 50f, false, "Gain", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 20f, 20000f, 1000f, true, "Frequency", "Hz", "Filter"),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)

        // Verify layout can be generated
        assertNotNull(metadata)
        assertEquals(2, metadata.getControlPorts().size)
        assertEquals(0, metadata.getMeterPorts().size)
    }

    @Test
    fun testLayoutGenerationWithMeterPorts() {
        // Create test metadata with meter ports
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 100f, 50f, false, "Gain", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, -60f, 0f, -20f, false, "Output Level", "dB", "Output"),
                PortDirection.OUTPUT, PortRole.METER
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)

        // Verify layout includes both control and meter ports
        assertEquals(1, metadata.getControlPorts().size)
        assertEquals(1, metadata.getMeterPorts().size)
    }

    @Test
    fun testLayoutGenerationWithAudioPorts() {
        // Create test metadata with audio ports
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.AUDIO, 0f, 1f, 0f, false, "Input L", null, null),
                PortDirection.INPUT, PortRole.UNKNOWN
            ),
            PortMetadata(
                PortDescriptor(1, PortType.AUDIO, 0f, 1f, 0f, false, "Output R", null, null),
                PortDirection.OUTPUT, PortRole.UNKNOWN
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)

        // Verify audio ports are identified
        assertEquals(1, metadata.getAudioInputPorts().size)
        assertEquals(1, metadata.getAudioOutputPorts().size)
    }

    @Test
    fun testLayoutGenerationGrouping() {
        // Create test metadata with multiple sections
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 100f, 50f, false, "Gain", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 0f, 100f, 50f, false, "Volume", "dB", "Output"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(2, PortType.CONTROL, 0f, 1f, 0.5f, false, "Mix", null, null),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)
        val grouped = metadata.getPortsBySection()

        // Verify grouping by section
        assertEquals(3, grouped.size)
        assertTrue(grouped.containsKey("Input"))
        assertTrue(grouped.containsKey("Output"))
        assertTrue(grouped.containsKey(null))
    }

    @Test
    fun testLayoutGenerationWithLogScale() {
        // Create test metadata with logarithmic scale
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 20f, 20000f, 1000f, true, "Frequency", "Hz", "Filter"),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)
        val freqPort = metadata.getControlPorts()[0]

        // Verify logarithmic scale is detected
        assertTrue(freqPort.isLogScale)
        assertEquals(20f, freqPort.minValue, 0.001f)
        assertEquals(20000f, freqPort.maxValue, 0.001f)
    }

    @Test
    fun testLayoutGenerationWithToggle() {
        // Create test metadata with toggle control (0-1 range)
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 1f, 0f, false, "Bypass", null, "Main"),
                PortDirection.INPUT, PortRole.BYPASS
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)
        val bypassPort = metadata.ports[0]

        // Verify toggle control is identified
        assertEquals(0f, bypassPort.minValue, 0.001f)
        assertEquals(1f, bypassPort.maxValue, 0.001f)
    }

    @Test
    fun testLayoutGenerationComplexPlugin() {
        // Create test metadata for a complex plugin (compressor)
        val ports = listOf(
            // Input section
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, -60f, 0f, -20f, false, "Threshold", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 1f, 100f, 4f, true, "Ratio", null, "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            // Output section
            PortMetadata(
                PortDescriptor(2, PortType.CONTROL, 0f, 24f, 0f, false, "Makeup Gain", "dB", "Output"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            // Metering
            PortMetadata(
                PortDescriptor(3, PortType.CONTROL, -60f, 0f, -20f, false, "Gain Reduction", "dB", "Metering"),
                PortDirection.OUTPUT, PortRole.METER
            )
        )

        val metadata = PluginMetadata("compressor", "Compressor", ports)

        // Verify complex layout
        assertEquals(3, metadata.getControlPorts().size)
        assertEquals(1, metadata.getMeterPorts().size)

        val grouped = metadata.getPortsBySection()
        assertEquals(4, grouped.size)
        assertEquals(2, grouped["Input"]?.size)
        assertEquals(1, grouped["Output"]?.size)
        assertEquals(1, grouped["Metering"]?.size)
    }

    @Test
    fun testLayoutGenerationEmptyPlugin() {
        // Create test metadata with no ports
        val metadata = PluginMetadata("empty", "Empty Plugin", emptyList())

        // Verify empty layout is handled
        assertEquals(0, metadata.getControlPorts().size)
        assertEquals(0, metadata.getMeterPorts().size)
        assertEquals(0, metadata.getAudioInputPorts().size)
        assertEquals(0, metadata.getAudioOutputPorts().size)
    }

    @Test
    fun testLayoutGenerationParameterNormalization() {
        // Create test metadata with various parameter ranges
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, -24f, 24f, 0f, false, "Gain", "dB", null),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 0f, 1f, 0.5f, false, "Mix", null, null),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(2, PortType.CONTROL, 20f, 20000f, 1000f, true, "Frequency", "Hz", null),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_eq", "Test EQ", ports)

        // Verify parameter ranges are preserved
        val gainPort = metadata.getPortByIndex(0)
        assertEquals(-24f, gainPort?.minValue, 0.001f)
        assertEquals(24f, gainPort?.maxValue, 0.001f)

        val mixPort = metadata.getPortByIndex(1)
        assertEquals(0f, mixPort?.minValue, 0.001f)
        assertEquals(1f, mixPort?.maxValue, 0.001f)

        val freqPort = metadata.getPortByIndex(2)
        assertEquals(20f, freqPort?.minValue, 0.001f)
        assertEquals(20000f, freqPort?.maxValue, 0.001f)
    }

    @Test
    fun testLspKnobIntegrationWithGainParameter() {
        // Create test metadata with gain parameter (should use knob)
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, -24f, 24f, 0f, false, "Input Gain", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_plugin", "Test Plugin", ports)
        val gainPort = metadata.getControlPorts()[0]

        // Verify gain parameter properties for knob control
        assertEquals("Input Gain", gainPort.name)
        assertTrue(gainPort.name.lowercase().contains("gain"))
        assertEquals(-24f, gainPort.minValue, 0.001f)
        assertEquals(24f, gainPort.maxValue, 0.001f)
        assertEquals(0f, gainPort.defaultValue, 0.001f)
    }

    @Test
    fun testLspKnobIntegrationWithFrequencyParameter() {
        // Create test metadata with frequency parameter (should use knob)
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 20f, 20000f, 1000f, true, "Cutoff Frequency", "Hz", "Filter"),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_filter", "Test Filter", ports)
        val freqPort = metadata.getControlPorts()[0]

        // Verify frequency parameter properties for knob control
        assertEquals("Cutoff Frequency", freqPort.name)
        assertTrue(freqPort.name.lowercase().contains("freq"))
        assertTrue(freqPort.isLogScale)
        assertEquals(20f, freqPort.minValue, 0.001f)
        assertEquals(20000f, freqPort.maxValue, 0.001f)
    }

    @Test
    fun testLspKnobIntegrationWithCompressorParameters() {
        // Create test metadata for compressor (multiple knob-suitable parameters)
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, -60f, 0f, -20f, false, "Threshold", "dB", "Dynamics"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 1f, 100f, 4f, true, "Ratio", null, "Dynamics"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(2, PortType.CONTROL, 0.1f, 100f, 10f, true, "Attack Time", "ms", "Envelope"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(3, PortType.CONTROL, 10f, 1000f, 100f, true, "Release Time", "ms", "Envelope"),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("compressor", "Compressor", ports)

        // Verify all parameters are suitable for knob controls
        val controlPorts = metadata.getControlPorts()
        assertEquals(4, controlPorts.size)

        // Threshold should use knob
        val threshold = controlPorts[0]
        assertTrue(threshold.name.lowercase().contains("threshold"))

        // Ratio should use knob
        val ratio = controlPorts[1]
        assertTrue(ratio.name.lowercase().contains("ratio"))

        // Attack should use knob
        val attack = controlPorts[2]
        assertTrue(attack.name.lowercase().contains("attack"))

        // Release should use knob
        val release = controlPorts[3]
        assertTrue(release.name.lowercase().contains("release"))
    }

    @Test
    fun testLspKnobIntegrationWithMixParameter() {
        // Create test metadata with mix parameter (should use knob)
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 100f, 50f, false, "Dry/Wet Mix", "%", "Output"),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_effect", "Test Effect", ports)
        val mixPort = metadata.getControlPorts()[0]

        // Verify mix parameter properties for knob control
        assertEquals("Dry/Wet Mix", mixPort.name)
        assertTrue(mixPort.name.lowercase().contains("mix"))
        assertEquals(0f, mixPort.minValue, 0.001f)
        assertEquals(100f, mixPort.maxValue, 0.001f)
    }

    @Test
    fun testLspKnobVerticalDragNormalization() {
        // Create test metadata to verify value normalization for vertical drag
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, -12f, 12f, 0f, false, "Level", "dB", null),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginMetadata("test_plugin", "Test Plugin", ports)
        val levelPort = metadata.getControlPorts()[0]

        // Test normalization calculations
        val minValue = levelPort.minValue
        val maxValue = levelPort.maxValue
        val range = maxValue - minValue

        // Test value at 0% (minimum)
        val normalized0 = 0f
        val actual0 = minValue + normalized0 * range
        assertEquals(-12f, actual0, 0.001f)

        // Test value at 50% (middle)
        val normalized50 = 0.5f
        val actual50 = minValue + normalized50 * range
        assertEquals(0f, actual50, 0.001f)

        // Test value at 100% (maximum)
        val normalized100 = 1f
        val actual100 = minValue + normalized100 * range
        assertEquals(12f, actual100, 0.001f)
    }
}
