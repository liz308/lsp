package com.example.lspandroid.model

import kotlinx.serialization.Serializable

/**
 * Metadata for a single plugin port (parameter, audio, or meter).
 */
@Serializable
data class PortMetadata(
    val index: Int,
    val name: String,
    val type: PortType,
    val direction: PortDirection,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val defaultValue: Float = 0f,
    val isLogScale: Boolean = false,
    val unit: String? = null,
    val section: String? = "General",
    val stepSize: Float? = null,
    val enumValues: List<String>? = null,
    val isToggle: Boolean = false,
    val displayName: String? = null
) {
    /**
     * Returns true if this is a control input port.
     */
    fun isControl(): Boolean {
        return type == PortType.CONTROL && direction == PortDirection.INPUT
    }
    
    /**
     * Returns true if this is a meter output port.
     */
    fun isMeter(): Boolean {
        return type == PortType.METER
    }
    
    /**
     * Returns true if this is an audio port.
     */
    fun isAudio(): Boolean {
        return type == PortType.AUDIO
    }
    
    /**
     * Gets the display name or falls back to the port name.
     */
    fun getDisplayName(): String {
        return displayName ?: name
    }
}

/**
 * Type of port.
 */
@Serializable
enum class PortType {
    AUDIO,
    CONTROL,
    METER
}

/**
 * Direction of port (input or output).
 */
@Serializable
enum class PortDirection {
    INPUT,
    OUTPUT
}
