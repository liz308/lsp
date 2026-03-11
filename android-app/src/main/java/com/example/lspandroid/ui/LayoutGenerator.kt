package com.example.lspandroid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lspandroid.model.PortMetadata
import com.example.lspandroid.model.PluginMetadata
import com.example.lspandroid.ui.components.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.util.concurrent.ConcurrentHashMap
import com.example.lspandroid.ui.utils.ScreenSizeUtils
import androidx.compose.foundation.clickable



/**
 * Dynamic layout generator from plugin metadata.
 * Creates touch-optimized UI controls from port information.
 * Supports layout override JSON files for custom layouts.
 * 
 * Requirement 10: Dynamic UI Layout Generation
 */
object LayoutGenerator {

    /**
     * Generates a UI layout for a plugin based on its metadata.
     * Supports layout override JSON files for custom layouts.
     * 
     * @param metadata Plugin metadata containing port information
     * @param onParameterChange Callback when a parameter is changed
     * @param layoutOverride Optional custom layout override JSON
     */
    @Composable
    fun GeneratePluginLayout(
        metadata: PluginMetadata,
        onParameterChange: (Int, Float) -> Unit,
        layoutOverride: String? = null,
        modifier: Modifier = Modifier
    ) {
        // Load layout override if provided
        layoutOverride?.let {
            LayoutOverrideManager.loadLayoutOverride(metadata.pluginId, it)
        }

        // Get override or generate default
        val override = LayoutOverrideManager.getLayoutOverride(metadata.pluginId)
            ?: DefaultLayoutGenerator.generateDefaultLayout(metadata.ports)

        val portsBySection = metadata.getPortsBySection()
        var parameterValues by remember {
            mutableStateOf(
                metadata.ports.associate { it.index to it.defaultValue }
            )
        }

        var collapsedSections by remember {
            mutableStateOf(
                override.sections.associate { 
                    it.name to !it.defaultExpanded 
                }.filterKeys { it != null }
            )
        }

        // Detect tablet and calculate responsive columns
        val isTablet = ScreenSizeUtils.isTablet()
        val columnCount = ScreenSizeUtils.getColumnCount()
        val contentPadding = ScreenSizeUtils.getContentPadding()
        val columnSpacing = ScreenSizeUtils.getColumnSpacing()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plugin header
            Text(
                text = metadata.pluginName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (metadata.description.isNotEmpty()) {
                Text(
                    text = metadata.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (override.sections.isNotEmpty()) {
                // Use layout override sections
                override.sections.forEach { section ->
                    val isCollapsed = collapsedSections[section.name] ?: false
                    
                    // Use responsive column count for tablets, or section-defined columns
                    val effectiveColumns = if (isTablet && section.columns == 1) {
                        columnCount
                    } else {
                        section.columns
                    }
                    
                    SectionContainer(
                        section = section.copy(
                            columns = effectiveColumns,
                            spacing = if (isTablet) columnSpacing.value else section.spacing
                        ),
                        isCollapsed = isCollapsed,
                        onToggleCollapse = { 
                            if (section.isCollapsible && section.name != null) {
                                collapsedSections = collapsedSections.toMutableMap().apply {
                                    this[section.name] = !isCollapsed
                                }
                            }
                        }
                    ) {
                        if (!isCollapsed) {
                            if (section.columns > 1) {
                                // Multi-column layout
                                val chunkedItems = section.items.chunked(section.columns)
                                chunkedItems.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(section.spacing.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            Box(
                                                modifier = Modifier.weight(item.width)
                                            ) {
                                                RenderLayoutItem(
                                                    item = item,
                                                    metadata = metadata,
                                                    parameterValues = parameterValues,
                                                    onParameterChange = { index, value ->
                                                        parameterValues = parameterValues.toMutableMap().apply {
                                                            this[index] = value
                                                        }
                                                        onParameterChange(index, value)
                                                    }
                                                )
                                            }
                                        }
                                        // Fill remaining space if row is not complete
                                        repeat(section.columns - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                // Single column layout
                                section.items.forEach { item ->
                                    RenderLayoutItem(
                                        item = item,
                                        metadata = metadata,
                                        parameterValues = parameterValues,
                                        onParameterChange = { index, value ->
                                            parameterValues = parameterValues.toMutableMap().apply {
                                                this[index] = value
                                            }
                                            onParameterChange(index, value)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback to metadata-based sections with responsive columns
                portsBySection.forEach { (section, ports) ->
                    if (section != null) {
                        SectionHeader(section)
                    }

                    // Group ports into columns for tablet layout
                    if (isTablet && columnCount > 1) {
                        val controlPorts = ports.filter { it.isControl() }
                        val meterPorts = ports.filter { it.isMeter() }
                        
                        // Render control ports in multi-column grid
                        if (controlPorts.isNotEmpty()) {
                            val chunkedPorts = controlPorts.chunked(columnCount)
                            chunkedPorts.forEach { rowPorts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(columnSpacing)
                                ) {
                                    rowPorts.forEach { port ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ControlPortUI(
                                                port = port,
                                                value = parameterValues[port.index] ?: port.defaultValue,
                                                onValueChange = { newValue ->
                                                    parameterValues = parameterValues.toMutableMap().apply {
                                                        this[port.index] = newValue
                                                    }
                                                    onParameterChange(port.index, newValue)
                                                }
                                            )
                                        }
                                    }
                                    // Fill remaining space if row is not complete
                                    repeat(columnCount - rowPorts.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        
                        // Render meter ports full-width
                        meterPorts.forEach { port ->
                            MeterPortUI(
                                port = port,
                                value = parameterValues[port.index] ?: port.defaultValue
                            )
                        }
                    } else {
                        // Single column layout for phones
                        ports.forEach { port ->
                            if (port.isControl()) {
                                ControlPortUI(
                                    port = port,
                                    value = parameterValues[port.index] ?: port.defaultValue,
                                    onValueChange = { newValue ->
                                        parameterValues = parameterValues.toMutableMap().apply {
                                            this[port.index] = newValue
                                        }
                                        onParameterChange(port.index, newValue)
                                    }
                                )
                            } else if (port.isMeter()) {
                                MeterPortUI(
                                    port = port,
                                    value = parameterValues[port.index] ?: port.defaultValue
                                )
                            }
                        }
                    }

                    if (section != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionContainer(
        section: LayoutSection,
        isCollapsed: Boolean,
        onToggleCollapse: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val backgroundColor = section.backgroundColor?.let { 
            try { Color(android.graphics.Color.parseColor(it)) } 
            catch (e: Exception) { MaterialTheme.colorScheme.surface }
        } ?: MaterialTheme.colorScheme.surface

        val borderColor = section.borderColor?.let {
            try { Color(android.graphics.Color.parseColor(it)) }
            catch (e: Exception) { MaterialTheme.colorScheme.outline }
        } ?: MaterialTheme.colorScheme.outline

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(section.spacing.dp)
        ) {
            if (section.name != null) {
                SectionHeader(
                    section = section.name,
                    isCollapsible = section.isCollapsible,
                    isCollapsed = isCollapsed,
                    onToggle = onToggleCollapse
                )
            }
            
            content()
        }
    }

    @Composable
    private fun RenderLayoutItem(
        item: LayoutItem,
        metadata: PluginMetadata,
        parameterValues: Map<Int, Float>,
        onParameterChange: (Int, Float) -> Unit
    ) {
        if (!item.isVisible) return

        val port = metadata.getPortByName(item.portName) ?: return

        when {
            port.isControl() -> {
                ControlPortUI(
                    port = port,
                    value = parameterValues[port.index] ?: port.defaultValue,
                    onValueChange = { newValue ->
                        onParameterChange(port.index, newValue)
                    },
                    layoutItem = item
                )
            }
            port.isMeter() -> {
                MeterPortUI(
                    port = port,
                    value = parameterValues[port.index] ?: port.defaultValue,
                    layoutItem = item
                )
            }
        }
    }

    /**
     * Renders a section header.
     */
    @Composable
    private fun SectionHeader(
        section: String,
        isCollapsible: Boolean = false,
        isCollapsed: Boolean = false,
        onToggle: () -> Unit = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isCollapsible) it.clickable { onToggle() } else it },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = section,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            if (isCollapsible) {
                Text(
                    text = if (isCollapsed) "▶" else "▼",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    /**
     * Renders a control port as an appropriate UI component.
     * 
     * @param port Port metadata
     * @param value Current parameter value
     * @param onValueChange Callback when value changes
     * @param layoutItem Optional layout item for custom control type
     */
    @Composable
    private fun ControlPortUI(
        port: PortMetadata,
        value: Float,
        onValueChange: (Float) -> Unit,
        layoutItem: LayoutItem? = null
    ) {
        // Normalize value to 0-1 range
        val normalizedValue = if (port.maxValue > port.minValue) {
            (value - port.minValue) / (port.maxValue - port.minValue)
        } else {
            0.5f
        }

        // Determine control type from layout item or auto-detect
        val controlType = layoutItem?.getEffectiveControlType(port) ?: determineControlType(port)
        val label = layoutItem?.customLabel ?: port.name

        when (controlType) {
            "toggle" -> {
                LspToggle(
                    value = normalizedValue > 0.5f,
                    onValueChange = { newValue ->
                        onValueChange(if (newValue) port.maxValue else port.minValue)
                    },
                    label = label,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "knob" -> {
                LspKnob(
                    value = normalizedValue,
                    onValueChange = { newNormalized ->
                        val newValue = port.minValue + newNormalized * (port.maxValue - port.minValue)
                        onValueChange(newValue)
                    },
                    label = label,
                    minValue = port.minValue,
                    maxValue = port.maxValue,
                    isLogScale = port.isLogScale,
                    unit = port.unit ?: "",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "slider" -> {
                LspSlider(
                    value = normalizedValue,
                    onValueChange = { newNormalized ->
                        val newValue = port.minValue + newNormalized * (port.maxValue - port.minValue)
                        onValueChange(newValue)
                    },
                    label = label,
                    minValue = port.minValue,
                    maxValue = port.maxValue,
                    unit = port.unit ?: "",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "dropdown" -> {
                val options = generateDropdownOptions(port)
                LspDropdown(
                    value = value,
                    options = options,
                    onValueChange = onValueChange,
                    label = label,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "button" -> {
                LspButton(
                    onClick = { onValueChange(if (value > 0.5f) 0f else 1f) },
                    label = label,
                    isPressed = value > 0.5f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                // Default to slider
                LspSlider(
                    value = normalizedValue,
                    onValueChange = { newNormalized ->
                        val newValue = port.minValue + newNormalized * (port.maxValue - port.minValue)
                        onValueChange(newValue)
                    },
                    label = label,
                    minValue = port.minValue,
                    maxValue = port.maxValue,
                    unit = port.unit ?: "",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private fun generateDropdownOptions(port: PortMetadata): List<Pair<String, Float>> {
        val range = port.maxValue - port.minValue
        val stepCount = when {
            range <= 10 -> range.toInt() + 1
            range <= 50 -> 10
            else -> 20
        }
        
        return (0 until stepCount).map { i ->
            val value = port.minValue + (i * range / (stepCount - 1))
            val displayValue = when {
                port.unit?.contains("Hz") == true -> "${(value / 1000).format(1)}kHz"
                port.unit?.contains("dB") == true -> "${value.format(1)}dB"
                port.unit?.contains("%") == true -> "${(value * 100).format(0)}%"
                else -> value.format(2)
            }
            displayValue to value
        }
    }

    private fun Float.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }

    /**
     * Determines the appropriate control type based on port metadata.
     */
    private fun determineControlType(port: PortMetadata): String {
        val name = port.name.lowercase()
        val range = port.maxValue - port.minValue
        
        return when {
            // Toggle control (binary values)
            (port.minValue == 0f && port.maxValue == 1f && !port.isLogScale) ||
            name.contains("bypass") || name.contains("enable") || name.contains("mute") -> "toggle"
            
            // Button for momentary actions
            name.contains("trigger") || name.contains("reset") || name.contains("clear") -> "button"
            
            // Dropdown for discrete values with small range
            range <= 10 && range == range.toInt().toFloat() -> "dropdown"
            
            // Use knob for compact rotary controls (typical audio parameters)
            shouldUseKnob(port) -> "knob"
            
            // Default to slider
            else -> "slider"
        }
    }

    /**
     * Determines if a port should use a knob control.
     * Knobs are preferred for typical audio parameters like gain, frequency, etc.
     */
    private fun shouldUseKnob(port: PortMetadata): Boolean {
        val name = port.name.lowercase()
        // Use knobs for common audio parameters
        return name.contains("gain") ||
                name.contains("level") ||
                name.contains("freq") ||
                name.contains("threshold") ||
                name.contains("ratio") ||
                name.contains("attack") ||
                name.contains("release") ||
                name.contains("time") ||
                name.contains("depth") ||
                name.contains("mix") ||
                name.contains("drive") ||
                name.contains("resonance") ||
                name.contains("q") ||
                name.contains("cutoff") ||
                name.contains("bandwidth") ||
                name.contains("decay") ||
                name.contains("sustain") ||
                name.contains("feedback") ||
                name.contains("wet") ||
                name.contains("dry")
    }

    /**
     * Renders a meter port for visualization.
     */
    @Composable
    private fun MeterPortUI(
        port: PortMetadata,
        value: Float,
        layoutItem: LayoutItem? = null
    ) {
        val normalizedValue = if (port.maxValue > port.minValue) {
            (value - port.minValue) / (port.maxValue - port.minValue)
        } else {
            0f
        }

        val label = layoutItem?.customLabel ?: port.name
        val meterType = layoutItem?.customProperties?.get("meterType") ?: determineMeterType(port)

        when (meterType) {
            "vu" -> {
                LspVUMeter(
                    value = normalizedValue.coerceIn(0f, 1f),
                    label = label,
                    unit = port.unit ?: "",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "spectrum" -> {
                LspSpectrumMeter(
                    value = normalizedValue.coerceIn(0f, 1f),
                    label = label,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "phase" -> {
                LspPhaseMeter(
                    value = normalizedValue.coerceIn(0f, 1f),
                    label = label,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                LspMeter(
                    value = normalizedValue.coerceIn(0f, 1f),
                    label = label,
                    unit = port.unit ?: "",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private fun determineMeterType(port: PortMetadata): String {
        val name = port.name.lowercase()
        return when {
            name.contains("vu") || name.contains("level") -> "vu"
            name.contains("spectrum") || name.contains("fft") -> "spectrum"
            name.contains("phase") || name.contains("correlation") -> "phase"
            else -> "basic"
        }
    }
}
