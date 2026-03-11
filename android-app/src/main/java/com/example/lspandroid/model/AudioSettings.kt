package com.example.lspandroid.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioSettings(
    val sampleRate: Int = 48000,
    val bufferSize: Int = 256,
    val enableLowLatency: Boolean = true,
    val audioDeviceId: Int = 0
)
