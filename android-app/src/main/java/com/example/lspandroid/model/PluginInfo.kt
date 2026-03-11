package com.example.lspandroid.model

import kotlinx.serialization.Serializable

@Serializable
data class PluginInfo(
    val pluginId: String = "",
    val pluginName: String = "",
    val manufacturer: String = "",
    val category: String = "",
    val description: String = "",
    val version: String = "1.0.0",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val rating: Float = 0f,
    val downloadCount: Int = 0,
    val lastUpdated: Long = 0L,
    val fileSize: Long = 0L,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val parameterCount: Int = 0,
    val dependencies: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val isInstrument: Boolean = false
) {
    val displayName: String
        get() = if (pluginName.isNotEmpty()) pluginName else pluginId
}