package com.example.lspandroid

import androidx.lifecycle.ViewModel
import com.example.lspandroid.model.PluginChain
import com.example.lspandroid.model.AudioSettings
import com.example.lspandroid.model.ChainedPlugin
import com.example.lspandroid.model.PluginInfo
import com.example.lspandroid.repository.PluginRepository
import com.example.lspandroid.repository.PreferencesRepository
import com.example.lspandroid.audio.AudioEngine
import com.example.lspandroid.utils.Logger
import com.example.lspandroid.utils.PerformanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simplified application state management for the LSP Android app.
 * Manages plugin chain, audio engine state, and UI state.
 */
class AppState(
    private val pluginRepository: PluginRepository,
    private val preferencesRepository: PreferencesRepository,
    private val audioEngine: AudioEngine,
    private val logger: Logger,
    private val performanceMonitor: PerformanceMonitor
) : ViewModel() {

    // Plugin chain state
    private val _pluginChain = MutableStateFlow(PluginChain())
    val pluginChain: StateFlow<PluginChain> = _pluginChain.asStateFlow()

    // Audio settings
    private val _audioSettings = MutableStateFlow(AudioSettings())
    val audioSettings: StateFlow<AudioSettings> = _audioSettings.asStateFlow()

    // UI state
    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()
    
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()
    
    private val _showPluginBrowser = MutableStateFlow(false)
    val showPluginBrowser: StateFlow<Boolean> = _showPluginBrowser.asStateFlow()

    // Selected plugin index
    private val _selectedPluginIndex = MutableStateFlow(-1)
    val selectedPluginIndex: StateFlow<Int> = _selectedPluginIndex.asStateFlow()

    // Audio engine state properties
    private val _isAudioRunning = MutableStateFlow(false)
    val isAudioRunning: StateFlow<Boolean> = _isAudioRunning.asStateFlow()
    
    private val _audioError = MutableStateFlow<String?>(null)
    val audioError: StateFlow<String?> = _audioError.asStateFlow()
    
    private val _cpuUsage = MutableStateFlow(0f)
    val cpuUsage: StateFlow<Float> = _cpuUsage.asStateFlow()
    
    private val _bufferSize = MutableStateFlow(512)
    val bufferSize: StateFlow<Int> = _bufferSize.asStateFlow()
    
    private val _sampleRate = MutableStateFlow(44100)
    val sampleRate: StateFlow<Int> = _sampleRate.asStateFlow()
    
    private val _latency = MutableStateFlow(0f)
    val latency: StateFlow<Float> = _latency.asStateFlow()

    // Plugin chain state properties
    private val _chainedPlugins = MutableStateFlow<List<ChainedPlugin>>(emptyList())
    val chainedPlugins: StateFlow<List<ChainedPlugin>> = _chainedPlugins.asStateFlow()
    
    private val _showPluginEditor = MutableStateFlow(false)
    val showPluginEditor: StateFlow<Boolean> = _showPluginEditor.asStateFlow()
    
    private val _availablePlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val availablePlugins: StateFlow<List<PluginInfo>> = _availablePlugins.asStateFlow()

    // Audio device properties
    private val _selectedAudioDevice = MutableStateFlow("Default")
    val selectedAudioDevice: StateFlow<String> = _selectedAudioDevice.asStateFlow()
    
    private val _availableAudioDevices = MutableStateFlow<List<String>>(listOf("Default"))
    val availableAudioDevices: StateFlow<List<String>> = _availableAudioDevices.asStateFlow()

    // UI preference properties
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _showCpuUsage = MutableStateFlow(true)
    val showCpuUsage: StateFlow<Boolean> = _showCpuUsage.asStateFlow()
    
    private val _hapticFeedbackEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()

    // Performance properties
    private val _cpuLimit = MutableStateFlow(80f)
    val cpuLimit: StateFlow<Float> = _cpuLimit.asStateFlow()
    
    private val _autoBypassOnOverload = MutableStateFlow(false)
    val autoBypassOnOverload: StateFlow<Boolean> = _autoBypassOnOverload.asStateFlow()
    
    private val _backgroundProcessing = MutableStateFlow(false)
    val backgroundProcessing: StateFlow<Boolean> = _backgroundProcessing.asStateFlow()

    init {
        logger.i("AppState initialized")
    }

    // Existing methods
    fun updateAudioSettings(settings: AudioSettings) {
        _audioSettings.value = settings
    }

    fun setShowOnboarding(show: Boolean) {
        _showOnboarding.value = show
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun setShowPluginBrowser(show: Boolean) {
        _showPluginBrowser.value = show
    }

    fun selectPlugin(index: Int) {
        _selectedPluginIndex.value = index
    }

    // Plugin chain methods
    fun reorderPlugins(from: Int, to: Int) {
        logger.d("Reordering plugins from $from to $to")
        val currentList = _chainedPlugins.value.toMutableList()
        if (from in currentList.indices && to in currentList.indices) {
            val plugin = currentList.removeAt(from)
            currentList.add(to, plugin)
            _chainedPlugins.value = currentList
        }
    }

    fun addPluginToChain(plugin: PluginInfo) {
        logger.d("Adding plugin to chain: ${plugin.pluginName}")
        // Stub implementation - would create ChainedPlugin from PluginInfo
        // and add to chain via audio engine
    }

    fun removePluginFromChain(index: Int) {
        logger.d("Removing plugin from chain at index: $index")
        val currentList = _chainedPlugins.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _chainedPlugins.value = currentList
        }
    }

    fun togglePluginBypass(index: Int) {
        logger.d("Toggling plugin bypass at index: $index")
        val currentList = _chainedPlugins.value.toMutableList()
        if (index in currentList.indices) {
            val plugin = currentList[index]
            currentList[index] = plugin.copy(isBypassed = !plugin.isBypassed)
            _chainedPlugins.value = currentList
        }
    }

    fun updatePluginParameter(pluginIndex: Int, paramId: String, value: Float) {
        logger.d("Updating plugin parameter: plugin=$pluginIndex, param=$paramId, value=$value")
        // Stub implementation - would delegate to audio engine
    }

    fun addQuickPlugin(category: String) {
        logger.d("Adding quick plugin from category: $category")
        // Stub implementation - would find first plugin in category and add it
    }

    // UI control methods
    fun setShowPluginEditor(show: Boolean) {
        _showPluginEditor.value = show
    }

    // Audio configuration methods
    fun setBufferSize(size: Int) {
        logger.d("Setting buffer size: $size")
        _bufferSize.value = size
        // Stub implementation - would delegate to audio engine
    }

    fun setSampleRate(rate: Int) {
        logger.d("Setting sample rate: $rate")
        _sampleRate.value = rate
        // Stub implementation - would delegate to audio engine
    }

    fun setAudioDevice(device: String) {
        logger.d("Setting audio device: $device")
        _selectedAudioDevice.value = device
        // Stub implementation - would delegate to audio engine
    }

    // Preference methods
    fun setDarkMode(enabled: Boolean) {
        logger.d("Setting dark mode: $enabled")
        _isDarkMode.value = enabled
        // Stub implementation - would persist via preferences repository
    }

    fun setShowCpuUsage(show: Boolean) {
        logger.d("Setting show CPU usage: $show")
        _showCpuUsage.value = show
        // Stub implementation - would persist via preferences repository
    }

    fun setHapticFeedback(enabled: Boolean) {
        logger.d("Setting haptic feedback: $enabled")
        _hapticFeedbackEnabled.value = enabled
        // Stub implementation - would persist via preferences repository
    }

    fun setCpuLimit(limit: Float) {
        logger.d("Setting CPU limit: $limit")
        _cpuLimit.value = limit
        // Stub implementation - would persist via preferences repository
    }

    fun setAutoBypassOnOverload(enabled: Boolean) {
        logger.d("Setting auto-bypass on overload: $enabled")
        _autoBypassOnOverload.value = enabled
        // Stub implementation - would persist via preferences repository
    }

    fun setBackgroundProcessing(enabled: Boolean) {
        logger.d("Setting background processing: $enabled")
        _backgroundProcessing.value = enabled
        // Stub implementation - would persist via preferences repository
    }

    // Utility methods
    fun resetOnboarding() {
        logger.d("Resetting onboarding")
        _showOnboarding.value = true
        // Stub implementation - would reset onboarding state in preferences
    }

    fun clearPluginCache() {
        logger.d("Clearing plugin cache")
        // Stub implementation - would clear cached plugin data
    }

    fun exportSettings() {
        logger.d("Exporting settings")
        // Stub implementation - would export settings to file
    }

    fun importSettings() {
        logger.d("Importing settings")
        // Stub implementation - would import settings from file
    }

    override fun onCleared() {
        super.onCleared()
        logger.i("AppState cleared")
    }
}
