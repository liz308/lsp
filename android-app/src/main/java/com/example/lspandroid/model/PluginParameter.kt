package com.example.lspandroid.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class PluginParameter(
    val id: String = "",
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val type: ParameterType = ParameterType.FLOAT,
    val defaultValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val step: Float = 0.01f,
    val unit: String = "",
    val isOutput: Boolean = false,
    val isEnabled: Boolean = true,
    val isBypassed: Boolean = false,
    val group: String = "",
    val tooltip: String = "",
    val helpText: String = "",
    val customParams: Map<String, String> = emptyMap(),
    val enumValues: List<String> = emptyList(),
    val precision: Int = 2
) {
    @Serializable
    enum class ParameterType {
        FLOAT, INT, BOOL, STRING, ENUM
    }
    
    val normalizedValue: Float
        get() = if (maxValue > minValue) {
            (defaultValue - minValue) / (maxValue - minValue)
        } else {
            0f
        }
    
    val displayValue: String
        get() = when (type) {
            ParameterType.FLOAT -> "%.${precision}f%s".format(defaultValue, unit)
            ParameterType.INT -> "${defaultValue.roundToInt()}$unit"
            ParameterType.BOOL -> if (defaultValue > 0.5f) "On" else "Off"
            ParameterType.ENUM -> {
                val index = defaultValue.roundToInt().coerceIn(0, enumValues.size - 1)
                enumValues.getOrElse(index) { "Unknown" }
    }
            ParameterType.STRING -> defaultValue.toString()
}    
    val isValid: Boolean
        get() = when (type) {
            ParameterType.FLOAT, ParameterType.INT -> defaultValue in minValue..maxValue
            ParameterType.BOOL -> true
            ParameterType.ENUM -> defaultValue.roundToInt() in 0 until enumValues.size
            ParameterType.STRING -> true
        }
    
    fun withValue(newValue: Float): PluginParameter {
        val clampedValue = when (type) {
            ParameterType.FLOAT -> newValue.coerceIn(minValue, maxValue)
            ParameterType.INT -> newValue.roundToInt().toFloat().coerceIn(minValue, maxValue)
            ParameterType.BOOL -> if (newValue > 0.5f) 1f else 0f
            ParameterType.ENUM -> newValue.roundToInt().toFloat().coerceIn(0f, (enumValues.size - 1).toFloat())
            ParameterType.STRING -> newValue
        }
        return copy(defaultValue = clampedValue)
    }
    
    fun withNormalizedValue(normalizedValue: Float): PluginParameter {
        val denormalizedValue = if (maxValue > minValue) {
            minValue + (normalizedValue.coerceIn(0f, 1f) * (maxValue - minValue))
        } else {
            defaultValue
        }
        return withValue(denormalizedValue)
    }
    
    fun reset(): PluginParameter {
        return copy(defaultValue = when (type) {
            ParameterType.FLOAT, ParameterType.INT -> (minValue + maxValue) / 2f
            ParameterType.BOOL -> 0f
            ParameterType.ENUM -> 0f
            ParameterType.STRING -> 0f
        })
    }
    
    fun getSteppedValue(direction: Int): PluginParameter {
        val stepSize = if (step > 0f) step else (maxValue - minValue) / 100f
        val newValue = defaultValue + (direction * stepSize)
        return withValue(newValue)
    }
}
