package com.example.lspandroid.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PortMetadata data classes.
 * Tests basic functionality, inference logic, and helper methods.
 */
class PortMetadataTest {

    @Test
    fun testPortTypeFromValue() {
        assertEquals(PortType.CONTROL, PortType.fromValue(0))
        assertEquals(PortType.AUDIO, PortType.fromValue(1))
        assertEquals(PortType.CV, PortType.fromValue(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPortTypeFromInvalidValue() {
        PortType.fromValue(99)
    }

    @Test
    fun testPortDescriptorCreation() {
        val descriptor = PortDescriptor(
            index = 0,
            type = PortType.CONTROL,
            minValue = 0f,
            maxValue = 100f,
            defaultValue = 50f,
            isLogScale = false,
            name = "Gain",
            unit = "dB",
            section = "Input"
        )

        assertEquals(0, descriptor.index)
        assertEquals(PortType.CONTROL, descriptor.type)
        assertEquals(0f, descriptor.minValue, 0.001f)
        assertEquals(100f, descriptor.maxValue, 0.001f)
        assertEquals(50f, descriptor.defaultValue, 0.001f)
        assertFalse(descriptor.isLogScale)
        assertEquals("Gain", descriptor.name)
        assertEquals("dB", descriptor.unit)
        assertEquals("Input", descriptor.section)
    }

    @Test
    fun testPortMetadataConvenienceAccessors() {
        val descriptor = PortDescriptor(
            index = 5,
            type = PortType.CONTROL,
            minValue = 20f,
            maxValue = 20000f,
            defaultValue = 1000f,
            isLogScale = true,
            name = "Frequency",
            unit = "Hz",
            section = "Filter"
        )
        val metadata = PortMetadata(descriptor, PortDirection.INPUT, PortRole.CONTROL)

        assertEquals(5, metadata.index)
        assertEquals(PortType.CONTROL, metadata.type)
        assertEquals("Frequency", metadata.name)
        assertEquals(20f, metadata.minValue, 0.001f)
        assertEquals(20000f, metadata.maxValue, 0.001f)
        assertEquals(1000f, metadata.defaultValue, 0.001f)
        assertTrue(metadata.isLogScale)
        assertEquals("Hz", metadata.unit)
        assertEquals("Filter", metadata.section)
    }

    @Test
    fun testPortMetadataTypeChecking() {
        val controlPort = PortMetadata(
            PortDescriptor(0, PortType.CONTROL, 0f, 1f, 0.5f, false, "Gain", "dB", null),
            PortDirection.INPUT,
            PortRole.CONTROL
        )
        assertTrue(controlPort.isControl())
        assertFalse(controlPort.isMeter())
        assertFalse(controlPort.isAudioInput())
        assertFalse(controlPort.isAudioOutput())

        val meterPort = PortMetadata(
            PortDescriptor(1, PortType.CONTROL, 0f, 1f, 0f, false, "Level Meter", "dB", null),
            PortDirection.OUTPUT,
            PortRole.METER
        )
        assertFalse(meterPort.isControl())
        assertTrue(meterPort.isMeter())

        val audioInPort = PortMetadata(
            PortDescriptor(2, PortType.AUDIO, 0f, 1f, 0f, false, "Input L", null, null),
            PortDirection.INPUT,
            PortRole.UNKNOWN
        )
        assertTrue(audioInPort.isAudioInput())
        assertFalse(audioInPort.isAudioOutput())

        val audioOutPort = PortMetadata(
            PortDescriptor(3, PortType.AUDIO, 0f, 1f, 0f, false, "Output R", null, null),
            PortDirection.OUTPUT,
            PortRole.UNKNOWN
        )
        assertFalse(audioOutPort.isAudioInput())
        assertTrue(audioOutPort.isAudioOutput())
    }

    @Test
    fun testFormatValue() {
        val portWithUnit = PortMetadata(
            PortDescriptor(0, PortType.CONTROL, 0f, 100f, 50f, false, "Gain", "dB", null),
            PortDirection.INPUT,
            PortRole.CONTROL
        )
        assertEquals("12.50 dB", portWithUnit.formatValue(12.5f))

        val portWithoutUnit = PortMetadata(
            PortDescriptor(1, PortType.CONTROL, 0f, 1f, 0.5f, false, "Mix", null, null),
            PortDirection.INPUT,
            PortRole.CONTROL
        )
        assertEquals("0.75", portWithoutUnit.formatValue(0.75f))
    }

    @Test
    fun testPluginMetadataFiltering() {
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 1f, 0.5f, false, "Gain", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 0f, 1f, 0f, false, "Meter", "dB", "Output"),
                PortDirection.OUTPUT, PortRole.METER
            ),
            PortMetadata(
                PortDescriptor(2, PortType.AUDIO, 0f, 1f, 0f, false, "Input L", null, null),
                PortDirection.INPUT, PortRole.UNKNOWN
            ),
            PortMetadata(
                PortDescriptor(3, PortType.AUDIO, 0f, 1f, 0f, false, "Output R", null, null),
                PortDirection.OUTPUT, PortRole.UNKNOWN
            )
        )

        val metadata = PluginDescriptor("test_eq", "Test EQ", ports)

        assertEquals(1, metadata.getControlPorts().size)
        assertEquals("Gain", metadata.getControlPorts()[0].name)

        assertEquals(1, metadata.getMeterPorts().size)
        assertEquals("Meter", metadata.getMeterPorts()[0].name)

        assertEquals(1, metadata.getAudioInputPorts().size)
        assertEquals("Input L", metadata.getAudioInputPorts()[0].name)

        assertEquals(1, metadata.getAudioOutputPorts().size)
        assertEquals("Output R", metadata.getAudioOutputPorts()[0].name)
    }

    @Test
    fun testPluginMetadataGrouping() {
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 1f, 0.5f, false, "Gain", "dB", "Input"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(1, PortType.CONTROL, 0f, 1f, 0.5f, false, "Volume", "dB", "Output"),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(2, PortType.CONTROL, 0f, 1f, 0.5f, false, "Mix", null, null),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginDescriptor("test_plugin", "Test Plugin", ports)
        val grouped = metadata.getPortsBySection()

        assertEquals(3, grouped.size)
        assertTrue(grouped.containsKey("Input"))
        assertTrue(grouped.containsKey("Output"))
        assertTrue(grouped.containsKey(null))

        assertEquals(1, grouped["Input"]?.size)
        assertEquals(1, grouped["Output"]?.size)
        assertEquals(1, grouped[null]?.size)
    }

    @Test
    fun testPluginMetadataLookup() {
        val ports = listOf(
            PortMetadata(
                PortDescriptor(0, PortType.CONTROL, 0f, 1f, 0.5f, false, "Gain", "dB", null),
                PortDirection.INPUT, PortRole.CONTROL
            ),
            PortMetadata(
                PortDescriptor(5, PortType.CONTROL, 20f, 20000f, 1000f, true, "Frequency", "Hz", null),
                PortDirection.INPUT, PortRole.CONTROL
            )
        )

        val metadata = PluginDescriptor("test_eq", "Test EQ", ports)

        val portByIndex = metadata.getPortByIndex(5)
        assertNotNull(portByIndex)
        assertEquals("Frequency", portByIndex?.name)

        val portByName = metadata.getPortByName("Gain")
        assertNotNull(portByName)
        assertEquals(0, portByName?.index)

        assertNull(metadata.getPortByIndex(99))
        assertNull(metadata.getPortByName("NonExistent"))
    }

    @Test
    fun testPortMetadataParserCreateDescriptor() {
        val descriptor = PortMetadataParser.createPortDescriptor(
            index = 10,
            typeValue = 0,
            minValue = -12f,
            maxValue = 12f,
            defaultValue = 0f,
            isLogScale = false,
            name = "Pan",
            unit = null,
            section = "Stereo"
        )

        assertEquals(10, descriptor.index)
        assertEquals(PortType.CONTROL, descriptor.type)
        assertEquals(-12f, descriptor.minValue, 0.001f)
        assertEquals(12f, descriptor.maxValue, 0.001f)
        assertEquals(0f, descriptor.defaultValue, 0.001f)
        assertFalse(descriptor.isLogScale)
        assertEquals("Pan", descriptor.name)
        assertNull(descriptor.unit)
        assertEquals("Stereo", descriptor.section)
    }

    @Test
    fun testPortMetadataParserValidation() {
        val validDescriptor = PortDescriptor(
            0, PortType.CONTROL, 0f, 100f, 50f, false, "Gain", "dB", null
        )
        assertTrue(PortMetadataParser.validatePortDescriptor(validDescriptor))

        val invalidRange = PortDescriptor(
            0, PortType.CONTROL, 100f, 0f, 50f, false, "Gain", "dB", null
        )
        assertFalse(PortMetadataParser.validatePortDescriptor(invalidRange))

        val invalidDefault = PortDescriptor(
            0, PortType.CONTROL, 0f, 100f, 150f, false, "Gain", "dB", null
        )
        assertFalse(PortMetadataParser.validatePortDescriptor(invalidDefault))

        val emptyName = PortDescriptor(
            0, PortType.CONTROL, 0f, 100f, 50f, false, "", "dB", null
        )
        assertFalse(PortMetadataParser.validatePortDescriptor(emptyName))
    }
}
