package com.example.lspandroid.model

import kotlinx.serialization.Serializable

/**
 * Comprehensive metadata for an audio plugin.
 * Contains all information needed to render UI and manage plugin state.
 */
@Serializable
data class PluginDescriptor(
    val pluginId: String,
    val pluginName: String,
    val version: String = "1.0.0",
    val category: String = "Effect",
    val description: String = "",
    val manufacturer: String = "LSP Project",
    val ports: List<PortMetadata> = emptyList(),
    val presets: List<String> = emptyList(),
    val tags: List<String> = emptyList()
) {
    /**
     * Groups ports by their section for organized UI display.
     */
    fun getPortsBySection(): Map<String?, List<PortMetadata>> {
        return ports.groupBy { it.section }
    }
    
    /**
     * Gets a port by its name.
     */
    fun getPortByName(name: String): PortMetadata? {
        return ports.firstOrNull { it.name == name }
    }
    
    /**
     * Gets a port by its index.
     */
    fun getPortByIndex(index: Int): PortMetadata? {
        return ports.firstOrNull { it.index == index }
    }
    
    /**
     * Gets all control input ports.
     */
    fun getControlPorts(): List<PortMetadata> {
        return ports.filter { it.type == PortType.CONTROL && it.direction == PortDirection.INPUT }
    }
    
    /**
     * Gets all meter output ports.
     */
    fun getMeterPorts(): List<PortMetadata> {
        return ports.filter { it.type == PortType.METER }
    }
}
