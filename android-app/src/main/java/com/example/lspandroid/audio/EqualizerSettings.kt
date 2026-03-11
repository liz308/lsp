package com.example.lspandroid.audio

import kotlinx.serialization.Serializable

/**
 * Represents equalizer settings with multiple frequency bands.
 * 
 * @property name Name of the equalizer preset
 * @property bandGains Gain values for each frequency band in dB
 * @property isDefault Whether this is a default preset
 * @property description Optional description of the preset
 */
@Serializable
data class EqualizerSettings(
    val name: String = "Default",
    val bandGains: List<Float> = List(10) { 0f },
    val isDefault: Boolean = false,
    val description: String = ""
) {
    companion object {
        /**
         * Creates default equalizer settings with all bands at 0dB.
         */
        fun getDefaultSettings(): EqualizerSettings {
            return EqualizerSettings(
                name = "Flat",
                bandGains = List(10) { 0f },
                isDefault = true,
                description = "All bands at 0dB (flat response)"
            )
        }
        
        /**
         * Creates bass boost equalizer settings.
         */
        fun getBassBoostSettings(): EqualizerSettings {
            return EqualizerSettings(
                name = "Bass Boost",
                bandGains = listOf(6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                description = "Enhanced low frequencies"
            )
        }
        
        /**
         * Creates treble boost equalizer settings.
         */
        fun getTrebleBoostSettings(): EqualizerSettings {
            return EqualizerSettings(
                name = "Treble Boost",
                bandGains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 6f, 8f),
                description = "Enhanced high frequencies"
            )
        }
        
        /**
         * Creates vocal enhancement equalizer settings.
         */
        fun getVocalEnhancementSettings(): EqualizerSettings {
            return EqualizerSettings(
                name = "Vocal Enhancement",
                bandGains = listOf(0f, 0f, 2f, 4f, 6f, 4f, 2f, 0f, 0f, 0f),
                description = "Enhanced vocal frequencies (300Hz - 3kHz)"
            )
        }
        
        /**
         * Gets all factory presets.
         */
        fun getFactoryPresets(): List<EqualizerSettings> {
            return listOf(
                getDefaultSettings(),
                getBassBoostSettings(),
                getTrebleBoostSettings(),
                getVocalEnhancementSettings()
            )
        }
    }
    
    /**
     * Gets the gain for a specific band index.
     * 
     * @param index Band index (0-9)
     * @return Gain in dB, or 0f if index is out of bounds
     */
    fun getBandGain(index: Int): Float {
        return if (index in bandGains.indices) bandGains[index] else 0f
    }
    
    /**
     * Creates a copy with updated band gain.
     * 
     * @param index Band index to update
     * @param gain New gain value in dB
     * @return New EqualizerSettings instance with updated gain
     */
    fun withBandGain(index: Int, gain: Float): EqualizerSettings {
        val newGains = bandGains.toMutableList()
        if (index in newGains.indices) {
            newGains[index] = gain.coerceIn(-24f, 24f)
        }
        return copy(bandGains = newGains)
    }
    
    /**
     * Creates a copy with all bands reset to 0dB.
     */
    fun reset(): EqualizerSettings {
        return copy(bandGains = List(bandGains.size) { 0f })
    }
    
    /**
     * Validates that all band gains are within acceptable range.
     */
    fun isValid(): Boolean {
        return bandGains.all { it in -24f..24f }
    }
    
    /**
     * Gets the frequency for a band index (approximate).
     * 
     * @param index Band index (0-9)
     * @return Approximate center frequency in Hz
     */
    fun getBandFrequency(index: Int): Float {
        val frequencies = listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        return if (index in frequencies.indices) frequencies[index] else 0f
    }
    
    /**
     * Gets the frequency label for a band index.
     */
    fun getBandLabel(index: Int): String {
        val frequency = getBandFrequency(index)
        return when {
            frequency < 1000 -> "${frequency.toInt()}Hz"
            else -> "${frequency / 1000}kHz"
        }
    }
}