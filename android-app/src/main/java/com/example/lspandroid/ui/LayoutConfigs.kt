package com.example.lspandroid.ui

import kotlinx.serialization.Serializable
import com.example.lspandroid.model.PortMetadata

/**
 * Layout override configuration with comprehensive styling and behavior options.
 */
@Serializable
data class LayoutOverride(
    val pluginId: String,
    val version: String = "1.0",
    val sections: List<LayoutSection> = emptyList(),
    val globalSettings: GlobalLayoutSettings = GlobalLayoutSettings()
)

/**
 * Layout section with comprehensive styling and behavior options.
 */
@Serializable
data class LayoutSection(
    val name: String? = null,
    val items: List<LayoutItem> = emptyList(),
    val columns: Int = 1,
    val spacing: Float = 16f,
    val backgroundColor: String? = null,
    val borderColor: String? = null,
    val isCollapsible: Boolean = false,
    val defaultExpanded: Boolean = true
)

/**
 * Individual layout item with comprehensive styling and behavior options.
 */
@Serializable
data class LayoutItem(
    val portName: String,
    val controlType: String? = null,
    val customLabel: String? = null,
    val isVisible: Boolean = true,
    val width: Float = 1f,
    val height: Float? = null,
    val x: Float? = null,
    val y: Float? = null,
    val customProperties: Map<String, String> = emptyMap()
)

@Serializable
data class GlobalLayoutSettings(
    val theme: String = "default",
    val compactMode: Boolean = false,
    val showUnits: Boolean = true,
    val animationDuration: Int = 300,
    val touchSensitivity: Float = 1f
)

// Extension function for LayoutItem
fun LayoutItem.getEffectiveControlType(port: PortMetadata): String {
    return controlType ?: when {
        port.minValue == 0f && port.maxValue == 1f && !port.isLogScale -> "toggle"
        port.name.lowercase().let { name ->
            name.contains("gain") || name.contains("level") || name.contains("freq") ||
            name.contains("threshold") || name.contains("ratio") || name.contains("attack") ||
            name.contains("release") || name.contains("time") || name.contains("depth") ||
            name.contains("mix") || name.contains("drive") || name.contains("resonance") ||
            name.contains("q")
        } -> "knob"
        else -> "slider"
    }
}