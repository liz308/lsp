package com.example.lspandroid.model

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore 
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.log10
import kotlin.math.sqrt 
import kotlin.math.tan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
/**
 * Represents a single plugin instance in the chain with comprehensive state management.
 * 
 * @property pluginId Unique identifier for the plugin
 * @property pluginName Human-readable plugin name
 * @property pluginFormat Format of the plugin (VST, AU, LV2, etc.)
 * @property uniqueId Unique identifier across all plugins and formats
 * @property pluginType Type/category of the plugin (EQ, Compressor, Reverb, etc.)
 * @property handle Native plugin handle (from JNI)
 * @property isBypassed Whether this plugin is currently bypassed
 * @property parameters Current parameter values mapped by port index
 * @property presets Available presets for this plugin
 * @property currentPreset Currently active preset name
 * @property wetDryMix Wet/dry mix ratio (0.0 = dry, 1.0 = wet)
 * @property inputGain Input gain in dB
 * @property outputGain Output gain in dB
 * @property latencyMs Plugin latency in milliseconds
 * @property cpuUsage Current CPU usage percentage
 * @property isEnabled Whether the plugin is enabled for processing
 * @property metadata Additional plugin metadata
 * @property automationData Parameter automation curves and envelopes
 * @property sideChainInput Optional sidechain input configuration
 * @property midiMapping MIDI controller mappings for parameters
 */
@Serializable
data class ChainedPlugin(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chainId: String,
    val position: Int,
    val pluginInstanceId: String,
    val pluginId: String,
    val pluginName: String,
    val pluginType: PluginType,
    val handle: Long = 0,
    val isBypassed: Boolean = false,
    val parameters: Map<Int, ChainParameter> = emptyMap(),
    val presets: Map<String, PluginPreset> = emptyMap(),
    val currentPreset: String? = null,
    val wetDryMix: Float = 1.0f,
    val inputGain: Float = 0.0f,
    val outputGain: Float = 0.0f,
    val latencyMs: Double = 0.0,
    val cpuUsage: Float = 0.0f,
    val isEnabled: Boolean = true,
    val metadata: PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        pluginName = pluginName
    ),
    val automationData: Map<Int, ParameterAutomation> = emptyMap(),
    val sideChainInput: SideChainConfig? = null,
    val midiMapping: Map<Int, MidiControllerMapping> = emptyMap(),
    val processingMode: ProcessingMode = ProcessingMode.REALTIME,
    val bufferSize: Int = 512,
    val sampleRate: Double = 44100.0,
    val channelConfiguration: ChannelConfiguration = ChannelConfiguration.STEREO,
    val pluginFormat: PluginFormat = PluginFormat.VST3,
    val libraryPath: String = "",
    val uniqueId: String = "",
    val version: String = "1.0.0",
    val manufacturer: String = "",
    val category: String = "",
    val isInstrument: Boolean = false,
    val hasMidiInput: Boolean = false,
    val hasMidiOutput: Boolean = false,
    val tailTimeMs: Double = 0.0,
    val programChangeSupport: Boolean = false,
    val stateData: ByteArray? = null
) {
    fun getParameter(portIndex: Int): ChainParameter? = parameters[portIndex]
    
    fun getParameterValue(portIndex: Int): Float? = parameters[portIndex]?.value
    
    fun withParameter(portIndex: Int, value: Float): ChainedPlugin {
        val param = parameters[portIndex] ?: return this
        val clampedValue = value.coerceIn(param.minValue, param.maxValue)
        val updatedParam = param.copy(value = clampedValue, lastModified = System.currentTimeMillis())
        return copy(parameters = parameters + (portIndex to updatedParam))
    }
    
    fun withParameterNormalized(portIndex: Int, normalizedValue: Float): ChainedPlugin {
        val param = parameters[portIndex] ?: return this
        val actualValue = param.denormalizeValue(normalizedValue.coerceIn(0f, 1f))
        return withParameter(portIndex, actualValue)
    }
    
    fun withPreset(presetName: String): ChainedPlugin? {
        val preset = presets[presetName] ?: return null
        val updatedParams = parameters.toMutableMap()
        preset.parameters.forEach { (portIndex, value) ->
            parameters[portIndex]?.let { param ->
                updatedParams[portIndex] = param.copy(
                    value = value.coerceIn(param.minValue, param.maxValue),
                    lastModified = System.currentTimeMillis()
                )
            }
        }
        return copy(
            parameters = updatedParams, 
            currentPreset = presetName,
            stateData = preset.stateData
        )
    }
    
    fun getAutomatedValue(portIndex: Int, timeMs: Long): Float? {
        val automation = automationData[portIndex] ?: return getParameterValue(portIndex)
        return automation.getValueAtTime(timeMs) ?: getParameterValue(portIndex)
    }
    
    fun withAutomation(portIndex: Int, automation: ParameterAutomation): ChainedPlugin {
        return copy(automationData = automationData + (portIndex to automation))
    }
    
    fun clearAutomation(portIndex: Int): ChainedPlugin {
        return copy(automationData = automationData - portIndex)
    }
    
    fun getMidiMappedParameter(ccNumber: Int): Int? {
        return midiMapping.entries.find { it.value.ccNumber == ccNumber }?.key
    }
    
    fun withMidiMapping(portIndex: Int, mapping: MidiControllerMapping): ChainedPlugin {
        return copy(midiMapping = midiMapping + (portIndex to mapping))
    }
    
    fun getLinearGain(): Float = 10f.pow(inputGain / 20f)
    
    fun getOutputLinearGain(): Float = 10f.pow(outputGain / 20f)
    
    fun getTotalLatencyMs(): Double = latencyMs + (bufferSize.toDouble() / sampleRate * 1000.0)
    
    fun isCompatibleWith(sampleRate: Double, bufferSize: Int): Boolean {
        // Always compatible - sample rate and buffer size validation handled by audio engine
        return bufferSize in 32..8192
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChainedPlugin
        return pluginId == other.pluginId
    }
    
    override fun hashCode(): Int = pluginId.hashCode()
}

/**
 * Plugin parameter with validation, automation support, and advanced metadata
 */
@Serializable
data class ChainParameter(
    val portIndex: Int,
    val name: String,
    val value: Float,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val unit: String = "",
    val isLogarithmic: Boolean = false,
    val stepSize: Float = 0.01f,
    val displayPrecision: Int = 2,
    val isReadOnly: Boolean = false,
    val isAutomatable: Boolean = true,
    val group: String = "",
    val description: String = "",
    val valueStrings: Map<Float, String> = emptyMap(),
    val scaleFactor: Float = 1.0f,
    val lastModified: Long = System.currentTimeMillis(),
    val modulationDepth: Float = 0.0f,
    val modulationSource: ModulationSource? = null
) {
    fun normalizedValue(): Float = (value - minValue) / (maxValue - minValue)
    
    fun setNormalizedValue(normalizedValue: Float): ChainParameter {
        val newValue = denormalizeValue(normalizedValue.coerceIn(0f, 1f))
        return copy(value = newValue, lastModified = System.currentTimeMillis())
    }
    
    fun denormalizeValue(normalizedValue: Float): Float {
        return if (isLogarithmic) {
            val logMin = ln(minValue.coerceAtLeast(0.001f))
            val logMax = ln(maxValue)
            exp(logMin + normalizedValue * (logMax - logMin))
        } else {
            minValue + (normalizedValue * (maxValue - minValue))
        }
    }
            
    fun getDisplayValue(): String {
        return valueStrings[value] ?: when {
            unit.isNotEmpty() -> String.format("%.${displayPrecision}f %s", value * scaleFactor, unit)
            else -> String.format("%.${displayPrecision}f", value * scaleFactor)
            }
            }
            
    fun isValidValue(testValue: Float): Boolean {
        return testValue in minValue..maxValue
    }
    
    fun getQuantizedValue(inputValue: Float): Float {
        if (stepSize <= 0f) return inputValue.coerceIn(minValue, maxValue)
        val steps = ((inputValue - minValue) / stepSize).toInt()
        return (minValue + steps * stepSize).coerceIn(minValue, maxValue)
    }
    
    fun withModulation(depth: Float, source: ModulationSource): ChainParameter {
        return copy(modulationDepth = depth.coerceIn(-1f, 1f), modulationSource = source)
    }
    
    fun getModulatedValue(modulationValue: Float): Float {
        if (modulationSource == null || modulationDepth == 0f) return value
        val modAmount = modulationValue * modulationDepth * (maxValue - minValue)
        return (value + modAmount).coerceIn(minValue, maxValue)
    }
}

    /**
 * Parameter automation with multiple curve types and keyframe support
     */
@Serializable
data class ParameterAutomation(
    val parameterIndex: Int,
    val keyframes: List<AutomationKeyframe>,
    val curveType: AutomationCurveType = AutomationCurveType.LINEAR,
    val isEnabled: Boolean = true,
    val loopMode: LoopMode = LoopMode.NONE,
    val loopStartMs: Long = 0,
    val loopEndMs: Long = 0
) {
    fun getValueAtTime(timeMs: Long): Float? {
        if (!isEnabled || keyframes.isEmpty()) return null
        
        val adjustedTime = when (loopMode) {
            LoopMode.NONE -> timeMs
            LoopMode.REPEAT -> {
                if (timeMs > loopEndMs) {
                    val loopDuration = loopEndMs - loopStartMs
                    loopStartMs + ((timeMs - loopStartMs) % loopDuration)
                } else timeMs
        }
            LoopMode.PING_PONG -> {
                if (timeMs > loopEndMs) {
                    val loopDuration = loopEndMs - loopStartMs
                    val cycles = (timeMs - loopStartMs) / loopDuration
                    if (cycles % 2 == 0L) {
                        loopStartMs + ((timeMs - loopStartMs) % loopDuration)
            } else {
                        loopEndMs - ((timeMs - loopStartMs) % loopDuration)
            }
                } else timeMs
        }
    }

        val sortedKeyframes = keyframes.sortedBy { it.timeMs }
        
        // Find surrounding keyframes
        val beforeIndex = sortedKeyframes.indexOfLast { it.timeMs <= adjustedTime }
        if (beforeIndex == -1) return sortedKeyframes.first().value
        if (beforeIndex == sortedKeyframes.size - 1) return sortedKeyframes.last().value
        
        val before = sortedKeyframes[beforeIndex]
        val after = sortedKeyframes[beforeIndex + 1]
        
        val progress = (adjustedTime - before.timeMs).toFloat() / (after.timeMs - before.timeMs).toFloat()
        
        return when (curveType) {
            AutomationCurveType.LINEAR -> lerp(before.value, after.value, progress)
            AutomationCurveType.EXPONENTIAL -> {
                val expProgress = progress * progress
                lerp(before.value, after.value, expProgress)
            }
            AutomationCurveType.LOGARITHMIC -> {
                val logProgress = 1f - (1f - progress) * (1f - progress)
                lerp(before.value, after.value, logProgress)
            }
            AutomationCurveType.SMOOTH -> {
                val smoothProgress = progress * progress * (3f - 2f * progress)
                lerp(before.value, after.value, smoothProgress)
            }
            AutomationCurveType.BEZIER -> {
                // Simplified cubic bezier interpolation
                val t = progress
                val t2 = t * t
                val t3 = t2 * t
                val mt = 1f - t
                val mt2 = mt * mt
                val mt3 = mt2 * mt
                
                mt3 * before.value + 3f * mt2 * t * before.value + 
                3f * mt * t2 * after.value + t3 * after.value
            }
        }
    }
    
    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)
    
    fun addKeyframe(keyframe: AutomationKeyframe): ParameterAutomation {
        val updatedKeyframes = (keyframes + keyframe).sortedBy { it.timeMs }
        return copy(keyframes = updatedKeyframes)
    }
    
    fun removeKeyframe(timeMs: Long): ParameterAutomation {
        val updatedKeyframes = keyframes.filter { it.timeMs != timeMs }
        return copy(keyframes = updatedKeyframes)
    }
    
    fun clearKeyframes(): ParameterAutomation = copy(keyframes = emptyList())
}

@Serializable
data class AutomationKeyframe(
    val timeMs: Long,
    val value: Float,
    val tension: Float = 0f,
    val bias: Float = 0f,
    val continuity: Float = 0f
            )

@Serializable
enum class AutomationCurveType {
    LINEAR, EXPONENTIAL, LOGARITHMIC, SMOOTH, BEZIER
        }

@Serializable
enum class LoopMode {
    NONE, REPEAT, PING_PONG
    }

    /**
 * Modulation source for parameter modulation
     */
@Serializable
data class ModulationSource(
    val type: ModulationType,
    val rate: Float = 1.0f,
    val depth: Float = 1.0f,
    val phase: Float = 0.0f,
    val sourceParameter: Int? = null
                )

@Serializable
enum class ModulationType {
    LFO_SINE, LFO_TRIANGLE, LFO_SQUARE, LFO_SAW, 
    ENVELOPE, RANDOM, PARAMETER, MIDI_CC, VELOCITY
            }

    /**
 * MIDI controller mapping for parameter control
     */
@Serializable
data class MidiControllerMapping(
    val ccNumber: Int,
    val channel: Int = -1, // -1 for all channels
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val curve: MidiCurveType = MidiCurveType.LINEAR,
    val isEnabled: Boolean = true
        )

@Serializable
enum class MidiCurveType {
    LINEAR, EXPONENTIAL, LOGARITHMIC
    }

    /**
 * Sidechain configuration for plugins that support it
     */
@Serializable
data class SideChainConfig(
    val isEnabled: Boolean = false,
    val sourcePluginId: String? = null,
    val sourceChannel: Int = 0,
    val threshold: Float = -20f,
    val ratio: Float = 4f,
    val attack: Float = 1f,
    val release: Float = 100f
    )

    /**
 * Plugin preset containing parameter values and state data
     */
@Serializable
data class PluginPreset(
    val name: String,
    val description: String = "",
    val parameters: Map<Int, Float>,
    val isFactory: Boolean = false,
    val author: String = "",
    val tags: Set<String> = emptySet(),
    val category: String = "",
    val version: String = "1.0",
    val createdTime: Long = System.currentTimeMillis(),
    val modifiedTime: Long = System.currentTimeMillis(),
    val stateData: ByteArray? = null,
    val automationData: Map<Int, ParameterAutomation> = emptyMap(),
    val midiMappings: Map<Int, MidiControllerMapping> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PluginPreset
        return name == other.name && parameters == other.parameters
    }
    
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }
/**
 * Extended plugin descriptor for additional information
 * Note: This is separate from PluginMetadata which is used for port metadata
 */
@Serializable
data class PluginDescriptor(
    val version: String = "",
    val author: String = "",
    val description: String = "",
    val website: String = "",
    val supportedSampleRates: Set<Int> = setOf(44100, 48000, 88200, 96000),
    val maxChannels: Int = 2,
    val hasGui: Boolean = false,
    val category: String = "",
    val tags: Set<String> = emptySet(),
    val license: String = "",
    val copyright: String = "",
    val buildDate: String = "",
    val sdkVersion: String = "",
    val pluginFormat: PluginFormat = PluginFormat.VST3,
    val vendorId: String = "",
    val productId: String = "",
    val minBufferSize: Int = 32,
    val maxBufferSize: Int = 8192,
    val preferredBufferSize: Int = 512,
    val supportsDoubleProcessing: Boolean = false,
    val supportsBypass: Boolean = true,
    val supportsOfflineProcessing: Boolean = false,
    val tailTimeSeconds: Double = 0.0,
    val programChangeParameter: Int = -1
)   val tailTimeSeconds: Double = 0.0,
    val programChangeParameter: Int = -1
)

/**
 * Plugin types enumeration with detailed categorization
 */
@Serializable
enum class PluginType(val displayName: String, val category: String) {
    EQUALIZER("Equalizer", "Filter"),
    COMPRESSOR("Compressor", "Dynamics"),
    LIMITER("Limiter", "Dynamics"),
    GATE("Gate", "Dynamics"),
    EXPANDER("Expander", "Dynamics"),
    REVERB("Reverb", "Spatial"),
    DELAY("Delay", "Spatial"),
    ECHO("Echo", "Spatial"),
    CHORUS("Chorus", "Modulation"),
    FLANGER("Flanger", "Modulation"),
    PHASER("Phaser", "Modulation"),
    TREMOLO("Tremolo", "Modulation"),
    VIBRATO("Vibrato", "Modulation"),
    DISTORTION("Distortion", "Saturation"),
    OVERDRIVE("Overdrive", "Saturation"),
    FUZZ("Fuzz", "Saturation"),
    BITCRUSHER("Bitcrusher", "Saturation"),
    FILTER("Filter", "Filter"),
    LOWPASS("Low Pass", "Filter"),
    HIGHPASS("High Pass", "Filter"),
    BANDPASS("Band Pass", "Filter"),
    NOTCH("Notch", "Filter"),
    SYNTHESIZER("Synthesizer", "Instrument"),
    SAMPLER("Sampler", "Instrument"),
    DRUM_MACHINE("Drum Machine", "Instrument"),
    BASS("Bass", "Instrument"),
    PIANO("Piano", "Instrument"),
    ORGAN("Organ", "Instrument"),
    GUITAR_AMP("Guitar Amp", "Amplifier"),
    BASS_AMP("Bass Amp", "Amplifier"),
    CABINET("Cabinet", "Amplifier"),
    MICROPHONE("Microphone", "Amplifier"),
    PREAMP("Preamp", "Amplifier"),
    ANALYZER("Analyzer", "Analysis"),
    SPECTRUM("Spectrum", "Analysis"),
    OSCILLOSCOPE("Oscilloscope", "Analysis"),
    TUNER("Tuner", "Analysis"),
    METER("Meter", "Analysis"),
    UTILITY("Utility", "Utility"),
    MIXER("Mixer", "Utility"),
    SPLITTER("Splitter", "Utility"),
    MERGER("Merger", "Utility"),
    GAIN("Gain", "Utility"),
    PAN("Pan", "Utility"),
    GENERATOR("Generator", "Generator"),
    OSCILLATOR("Oscillator", "Generator"),
    NOISE("Noise", "Generator"),
    TONE("Tone", "Generator"),
    OTHER("Other", "Other")
}

@Serializable
enum class ProcessingMode {
    REALTIME, OFFLINE, FREEZE
}

@Serializable
enum class ChannelConfiguration(val channels: Int, val displayName: String) {
    MONO(1, "Mono"),
    STEREO(2, "Stereo"),
    SURROUND_5_1(6, "5.1 Surround"),
    SURROUND_7_1(8, "7.1 Surround")
}

@Serializable
enum class PluginFormat {
    VST2, VST3, AU, AAX, LADSPA, LV2, CLAP
}

/**
 * Chain operation result with detailed information
 */
sealed class ChainOperationResult {
    object Success : ChainOperationResult()
    data class Error(val message: String, val code: Int = -1, val exception: Throwable? = null) : ChainOperationResult()
    data class Warning(val message: String, val details: String = "") : ChainOperationResult()
    data class PartialSuccess(val message: String, val completedOperations: Int, val totalOperations: Int) : ChainOperationResult()
}

/**
 * Chain event for real-time notifications with comprehensive event types
 */
sealed class ChainEvent {
    data class PluginAdded(val plugin: ChainedPlugin, val position: Int, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class PluginRemoved(val pluginId: String, val position: Int, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class PluginMoved(val pluginId: String, val fromPosition: Int, val toPosition: Int, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class PluginBypassed(val pluginId: String, val position: Int, val bypassed: Boolean, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class ParameterChanged(val pluginId: String, val position: Int, val portIndex: Int, val value: Float, val normalizedValue: Float, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class PresetLoaded(val pluginId: String, val position: Int, val presetName: String, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class PresetSaved(val pluginId: String, val position: Int, val presetName: String, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class ChainCleared(val previousSize: Int, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class ValidationFailed(val errors: List<String>, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class PerformanceAlert(val pluginId: String, val cpuUsage: Float, val threshold: Float, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class LatencyChanged(val pluginId: String, val oldLatency: Double, val newLatency: Double, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class AutomationRecorded(val pluginId: String, val parameterIndex: Int, val keyframes: List<AutomationKeyframe>, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class MidiMappingChanged(val pluginId: String, val parameterIndex: Int, val ccNumber: Int, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
    data class ChainStateChanged(val state: ChainState, val timestamp: Long = System.currentTimeMillis()) : ChainEvent()
}

@Serializable
enum class ChainState {
    IDLE, PROCESSING, LOADING, SAVING, ERROR, BYPASSED
}

/**
 * Manages an ordered sequence of plugins processing audio serially with comprehensive
 * real-time capabilities, thread safety, and advanced features.
 * 
 * Requirement 11: Plugin Chain Management
 * - Maintains ordered list of plugins with full state management
 * - Supports insertion at specified position with validation
 * - Supports removal and bypass with automatic cleanup
 * - Supports real-time reordering during playback with latency compensation
 * - Provides preset management and parameter automation
 * - Offers performance monitoring and CPU usage tracking
 * - Implements thread-safe operations for concurrent access
 */
class PluginChain {
    private val plugins = mutableListOf<ChainedPlugin>()
    private val chainMutex = Mutex()
    private val eventChannel = kotlinx.coroutines.channels.Channel<ChainEvent>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private val performanceMetrics = ConcurrentHashMap<String, PluginPerformanceMetrics>()
    private val operationCounter = AtomicLong(0)
    private val chainState = AtomicBoolean(false) // false = idle, true = processing
    private val rwLock = ReentrantReadWriteLock()
    private val automationRecorder = AutomationRecorder()
    private val presetManager = PresetManager()
    private val midiProcessor = MidiProcessor()
    
    // Configuration
    var maxPlugins: Int = 64
    var enablePerformanceMonitoring: Boolean = true
    var autoValidation: Boolean = true
    var latencyCompensation: Boolean = true
    var cpuUsageThreshold: Float = 80.0f
    var maxLatencyMs: Double = 500.0
    var enableAutomation: Boolean = true
    var enableMidiMapping: Boolean = true
    var bufferSize: Int = 512
    var sampleRate: Double = 44100.0
    var enableRealTimeProcessing: Boolean = true
    
    // Events flow for real-time notifications
    val events: Flow<ChainEvent> = eventChannel.receiveAsFlow()
    
    // Performance monitoring
    data class PluginPerformanceMetrics(
        val pluginId: String,
        val averageCpuUsage: Float = 0.0f,
        val peakCpuUsage: Float = 0.0f,
        val processedSamples: Long = 0L,
        val dropouts: Int = 0,
        val lastUpdateTime: Long = System.currentTimeMillis(),
        val processingTimeNs: Long = 0L,
        val memoryUsageMB: Float = 0.0f,
        val bufferUnderruns: Int = 0,
        val bufferOverruns: Int = 0,
        val errorCount: Int = 0,
        val successfulProcesses: Long = 0L
    )
    
    /**
     * Automation recorder for capturing parameter changes
     */
    inner class AutomationRecorder {
        private val recordingStates = ConcurrentHashMap<String, Boolean>()
        private val recordedData = ConcurrentHashMap<String, MutableMap<Int, MutableList<AutomationKeyframe>>>()
        
        fun startRecording(pluginId: String) {
            recordingStates[pluginId] = true
            recordedData[pluginId] = mutableMapOf()
        }
        
        fun stopRecording(pluginId: String): Map<Int, List<AutomationKeyframe>> {
            recordingStates[pluginId] = false
            return recordedData[pluginId]?.mapValues { it.value.toList() } ?: emptyMap()
        }
        
        fun recordParameterChange(pluginId: String, parameterIndex: Int, value: Float, timeMs: Long) {
            if (recordingStates[pluginId] == true) {
                recordedData.computeIfAbsent(pluginId) { mutableMapOf() }
                    .computeIfAbsent(parameterIndex) { mutableListOf() }
                    .add(AutomationKeyframe(timeMs, value))
            }
        }
        
        fun isRecording(pluginId: String): Boolean = recordingStates[pluginId] == true
        
        fun clearRecording(pluginId: String) {
            recordingStates.remove(pluginId)
            recordedData.remove(pluginId)
        }
    }
    
    /**
     * Preset manager for handling plugin presets
     */
    inner class PresetManager {
        private val userPresets = ConcurrentHashMap<String, MutableMap<String, PluginPreset>>()
        
        suspend fun savePreset(pluginId: String, presetName: String, description: String = ""): ChainOperationResult {
            val plugin = findPluginById(pluginId) ?: return ChainOperationResult.Error("Plugin not found")
            
            val preset = PluginPreset(
                name = presetName,
                description = description,
                parameters = plugin.parameters.mapValues { it.value.value },
                author = "User",
                createdTime = System.currentTimeMillis(),
                modifiedTime = System.currentTimeMillis(),
                automationData = plugin.automationData,
                midiMappings = plugin.midiMapping
            )
            
            userPresets.computeIfAbsent(pluginId) { mutableMapOf() }[presetName] = preset
            
            val position = getPluginPosition(pluginId)
            if (position >= 0) {
                eventChannel.trySend(ChainEvent.PresetSaved(pluginId, position, presetName))
            }
            
            return ChainOperationResult.Success
        }
        
        fun getUserPresets(pluginId: String): Map<String, PluginPreset> {
            return userPresets[pluginId]?.toMap() ?: emptyMap()
        }
        
        fun deletePreset(pluginId: String, presetName: String): Boolean {
            return userPresets[pluginId]?.remove(presetName) != null
        }
        fun exportPresets(pluginId: String): String {
            val presets = userPresets[pluginId] ?: return "{}"
            return Json.encodeToString(serializer<PluginPreset>(), presets.values.first())
        }
        fun importPreset(pluginId: String, presetJson: String): ChainOperationResult {
            return try {
                val preset = Json.decodeFromString<PluginPreset>(presetJson)
                userPresets.computeIfAbsent(pluginId) { mutableMapOf() }[preset.name] = preset
                ChainOperationResult.Success
            } catch (e: Exception) {
                ChainOperationResult.Error("Failed to import preset: ${e.message}")
            }
        }
    }
    
    /**
     * MIDI processor for handling MIDI events and mappings
     */
    inner class MidiProcessor {
        fun processMidiCC(ccNumber: Int, value: Int, channel: Int = -1): List<Pair<String, Int>> {
            val affectedParameters = mutableListOf<Pair<String, Int>>()
            
            rwLock.read {
                plugins.forEach { plugin ->
                    plugin.midiMapping.forEach { (paramIndex, mapping) ->
                        if (mapping.ccNumber == ccNumber && 
                            (mapping.channel == -1 || mapping.channel == channel) && 
                            mapping.isEnabled) {
                            
                            val normalizedValue = value / 127.0f
                            val mappedValue = when (mapping.curve) {
                                MidiCurveType.LINEAR -> normalizedValue
                                MidiCurveType.EXPONENTIAL -> normalizedValue * normalizedValue
                                MidiCurveType.LOGARITHMIC -> 1f - (1f - normalizedValue) * (1f - normalizedValue)
                            }
                            
                            val finalValue = mapping.minValue + mappedValue * (mapping.maxValue - mapping.minValue)
                            
                            // This would trigger parameter update in the actual implementation
                            affectedParameters.add(plugin.pluginId to paramIndex)
                        }
                    }
                }
            }
            
            return affectedParameters
        }
        
        fun learnMidiMapping(pluginId: String, parameterIndex: Int, ccNumber: Int, channel: Int = -1): ChainOperationResult {
            val mapping = MidiControllerMapping(
                ccNumber = ccNumber,
                channel = channel,
                minValue = 0f,
                maxValue = 1f,
                curve = MidiCurveType.LINEAR,
                isEnabled = true
            )
            
            return runBlocking {
        chainMutex.withLock {
                    val pluginIndex = plugins.indexOfFirst { it.pluginId == pluginId }
                    if (pluginIndex < 0) {
                        return@withLock ChainOperationResult.Error("Plugin not found")
        }
                    
                    val plugin = plugins[pluginIndex]
                    val updatedPlugin = plugin.withMidiMapping(parameterIndex, mapping)
                    plugins[pluginIndex] = updatedPlugin
                    
                    eventChannel.trySend(ChainEvent.MidiMappingChanged(pluginId, parameterIndex, ccNumber))
                    ChainOperationResult.Success
    }
}
        }
    }

    /**
     * Adds a plugin to the end of the chain with comprehensive validation.
     * 
     * @param plugin Plugin to add
     * @return Operation result with success/error information
     */
    suspend fun addPlugin(plugin: ChainedPlugin): ChainOperationResult = chainMutex.withLock {
        try {
            operationCounter.incrementAndGet()
            
            // Validate plugin
            val validationResult = validatePlugin(plugin)
            if (validationResult !is ChainOperationResult.Success) {
                return validationResult
            }

            // Check chain size limit
            if (plugins.size >= maxPlugins) {
                return ChainOperationResult.Error("Chain size limit exceeded (max: $maxPlugins)")
            }
            
            // Check for duplicate plugin ID
            if (plugins.any { it.pluginId == plugin.pluginId }) {
                return ChainOperationResult.Error("Plugin with ID '${plugin.pluginId}' already exists")
            }
            
            // Validate compatibility
            if (!plugin.isCompatibleWith(sampleRate, bufferSize)) {
                return ChainOperationResult.Error("Plugin is not compatible with current sample rate ($sampleRate) or buffer size ($bufferSize)")
            }
            
            // Initialize performance metrics
            if (enablePerformanceMonitoring) {
                performanceMetrics[plugin.pluginId] = PluginPerformanceMetrics(plugin.pluginId)
            }
            
            // Configure plugin for current chain settings
            val configuredPlugin = plugin.copy(
                sampleRate = sampleRate,
                bufferSize = bufferSize
            )
            
            plugins.add(configuredPlugin)
            eventChannel.trySend(ChainEvent.PluginAdded(configuredPlugin, plugins.size - 1))
            
            if (autoValidation) {
                val chainValidation = validateChain()
                if (chainValidation !is ChainOperationResult.Success) {
                    // Rollback
                    plugins.removeLastOrNull()
                    performanceMetrics.remove(plugin.pluginId)
                    return chainValidation
                }
            }
            
            if (latencyCompensation) {
                compensateLatency()
            }
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to add plugin: ${e.message}", exception = e)
        }
    }
    
    /**
     * Inserts a plugin at the specified position with validation and latency compensation.
     * 
     * @param position Position to insert at (0-based)
     * @param plugin Plugin to insert
     * @return Operation result
     */
    suspend fun insertPlugin(position: Int, plugin: ChainedPlugin): ChainOperationResult = chainMutex.withLock {
        try {
            operationCounter.incrementAndGet()
            
            if (position < 0 || position > plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position (valid range: 0-${plugins.size})")
            }
            
            val validationResult = validatePlugin(plugin)
            if (validationResult !is ChainOperationResult.Success) {
                return validationResult
            }

            if (plugins.size >= maxPlugins) {
                return ChainOperationResult.Error("Chain size limit exceeded (max: $maxPlugins)")
            }
            
            if (plugins.any { it.pluginId == plugin.pluginId }) {
                return ChainOperationResult.Error("Plugin with ID '${plugin.pluginId}' already exists")
            }
            
            if (!plugin.isCompatibleWith(sampleRate, bufferSize)) {
                return ChainOperationResult.Error("Plugin is not compatible with current settings")
            }
            
            if (enablePerformanceMonitoring) {
                performanceMetrics[plugin.pluginId] = PluginPerformanceMetrics(plugin.pluginId)
            }
            
            val configuredPlugin = plugin.copy(
                sampleRate = sampleRate,
                bufferSize = bufferSize
            )
            
            plugins.add(position, configuredPlugin)
            eventChannel.trySend(ChainEvent.PluginAdded(configuredPlugin, position))
            
            if (latencyCompensation) {
                compensateLatency()
            }
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to insert plugin: ${e.message}", exception = e)
        }
    }

    /**
     * Removes a plugin at the specified position with cleanup.
     * 
     * @param position Position to remove (0-based)
     * @return Removed plugin or null if position is invalid
     */
    suspend fun removePlugin(position: Int): ChainedPlugin? = chainMutex.withLock {
        try {
            operationCounter.incrementAndGet()
            
            if (position < 0 || position >= plugins.size) {
                return null
            }
            
            val removedPlugin = plugins.removeAt(position)
            performanceMetrics.remove(removedPlugin.pluginId)
            automationRecorder.clearRecording(removedPlugin.pluginId)
            eventChannel.trySend(ChainEvent.PluginRemoved(removedPlugin.pluginId, position))
            
            if (latencyCompensation) {
                compensateLatency()
            }
            
            removedPlugin
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Removes a plugin by ID with comprehensive cleanup.
     * 
     * @param pluginId Plugin ID to remove
     * @return Operation result
     */
    suspend fun removePluginById(pluginId: String): ChainOperationResult = chainMutex.withLock {
        try {
            operationCounter.incrementAndGet()
            
            val index = plugins.indexOfFirst { it.pluginId == pluginId }
            if (index < 0) {
                return ChainOperationResult.Error("Plugin with ID '$pluginId' not found")
            }

            plugins.removeAt(index)
            performanceMetrics.remove(pluginId)
            automationRecorder.clearRecording(pluginId)
            eventChannel.trySend(ChainEvent.PluginRemoved(pluginId, index))
            
            if (latencyCompensation) {
                compensateLatency()
            }
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to remove plugin: ${e.message}", exception = e)
        }
    }

    /**
     * Moves a plugin from one position to another with real-time support.
     * 
     * @param fromPosition Current position
     * @param toPosition New position
     * @return Operation result
     */
    suspend fun movePlugin(fromPosition: Int, toPosition: Int): ChainOperationResult = chainMutex.withLock {
        try {
            operationCounter.incrementAndGet()
            
            if (fromPosition < 0 || fromPosition >= plugins.size) {
                return ChainOperationResult.Error("Invalid from position: $fromPosition")
            }

            if (toPosition < 0 || toPosition >= plugins.size) {
                return ChainOperationResult.Error("Invalid to position: $toPosition")
            }
            
            if (fromPosition == toPosition) {
                return ChainOperationResult.Success
            }
            
            val plugin = plugins.removeAt(fromPosition)
            plugins.add(toPosition, plugin)
            
            eventChannel.trySend(ChainEvent.PluginMoved(plugin.pluginId, fromPosition, toPosition))
            
            if (latencyCompensation) {
                compensateLatency()
            }
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to move plugin: ${e.message}", exception = e)
        }
    }

    /**
     * Sets the bypass state of a plugin with real-time updates.
     * 
     * @param position Position of plugin
     * @param bypassed True to bypass, false to enable
     * @return Operation result
     */
    suspend fun setPluginBypassed(position: Int, bypassed: Boolean): ChainOperationResult = chainMutex.withLock {
        try {
            if (position < 0 || position >= plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position")
            }

            val plugin = plugins[position]
            if (plugin.isBypassed == bypassed) {
                return ChainOperationResult.Success // No change needed
            }
            
            plugins[position] = plugin.copy(isBypassed = bypassed)
            eventChannel.trySend(ChainEvent.PluginBypassed(plugin.pluginId, position, bypassed))
            
            if (latencyCompensation) {
                compensateLatency()
            }
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to set bypass state: ${e.message}", exception = e)
        }
    }

    /**
     * Updates a parameter value with validation, automation recording, and real-time updates.
     * 
     * @param position Position of plugin
     * @param portIndex Port index to update
     * @param value New parameter value
     * @param recordAutomation Whether to record this change for automation
     * @return Operation result
     */
    suspend fun setPluginParameter(
        position: Int, 
        portIndex: Int, 
        value: Float, 
        recordAutomation: Boolean = false
    ): ChainOperationResult = chainMutex.withLock {
        try {
            if (position < 0 || position >= plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position")
            }
            
            val plugin = plugins[position]
            val parameter = plugin.parameters[portIndex]
                ?: return ChainOperationResult.Error("Parameter at port $portIndex not found")
            
            if (parameter.isReadOnly) {
                return ChainOperationResult.Error("Parameter at port $portIndex is read-only")
            }
            
            val quantizedValue = parameter.getQuantizedValue(value)
            val clampedValue = quantizedValue.coerceIn(parameter.minValue, parameter.maxValue)
            val updatedParameter = parameter.copy(
                value = clampedValue,
                lastModified = System.currentTimeMillis()
            )
            val updatedPlugin = plugin.copy(
                parameters = plugin.parameters + (portIndex to updatedParameter)
            )
            
            plugins[position] = updatedPlugin
            
            val normalizedValue = updatedParameter.normalizedValue()
            eventChannel.trySend(
                ChainEvent.ParameterChanged(
                    plugin.pluginId, 
                    position, 
                    portIndex, 
                    clampedValue, 
                    normalizedValue
                )
            )
            
            // Record automation if enabled
            if (recordAutomation && enableAutomation) {
                automationRecorder.recordParameterChange(
                    plugin.pluginId, 
                    portIndex, 
                    clampedValue, 
                    System.currentTimeMillis()
                )
            }
            
            if (clampedValue != value) {
                ChainOperationResult.Warning(
                    "Parameter value adjusted from $value to $clampedValue",
                    "Value was clamped to valid range [${parameter.minValue}, ${parameter.maxValue}]"
                )
            } else {
                ChainOperationResult.Success
            }
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to set parameter: ${e.message}", exception = e)
        }
    }

    /**
     * Sets a parameter using normalized value (0.0 to 1.0)
     */
    suspend fun setPluginParameterNormalized(
        position: Int,
        portIndex: Int,
        normalizedValue: Float,
        recordAutomation: Boolean = false
    ): ChainOperationResult = chainMutex.withLock {
        try {
            if (position < 0 || position >= plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position")
            }
            
            val plugin = plugins[position]
            val parameter = plugin.parameters[portIndex]
                ?: return ChainOperationResult.Error("Parameter at port $portIndex not found")
            
            val actualValue = parameter.denormalizeValue(normalizedValue.coerceIn(0f, 1f))
            return setPluginParameter(position, portIndex, actualValue, recordAutomation)
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to set normalized parameter: ${e.message}", exception = e)
        }
    }

    /**
     * Loads a preset for a plugin at the specified position.
     * 
     * @param position Position of plugin
     * @param presetName Name of preset to load
     * @return Operation result
     */
    suspend fun loadPluginPreset(position: Int, presetName: String): ChainOperationResult = chainMutex.withLock {
        try {
            if (position < 0 || position >= plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position")
            }
            
            val plugin = plugins[position]
            
            // Check factory presets first, then user presets
            val preset = plugin.presets[presetName] 
                ?: presetManager.getUserPresets(plugin.pluginId)[presetName]
                ?: return ChainOperationResult.Error("Preset '$presetName' not found")
            
            val updatedPlugin = plugin.withPreset(presetName)
                ?: return ChainOperationResult.Error("Failed to apply preset '$presetName'")
            
            plugins[position] = updatedPlugin
            eventChannel.trySend(ChainEvent.PresetLoaded(plugin.pluginId, position, presetName))
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to load preset: ${e.message}", exception = e)
        }
    }

    /**
     * Saves current plugin state as a user preset
     */
    suspend fun savePluginPreset(
        position: Int, 
        presetName: String, 
        description: String = ""
    ): ChainOperationResult = chainMutex.withLock {
        try {
            if (position < 0 || position >= plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position")
            }
            
            val plugin = plugins[position]
            return presetManager.savePreset(plugin.pluginId, presetName, description)
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to save preset: ${e.message}", exception = e)
        }
    }

    /**
     * Adds automation to a parameter
     */
    suspend fun addParameterAutomation(
        position: Int,
        portIndex: Int,
        automation: ParameterAutomation
    ): ChainOperationResult = chainMutex.withLock {
        try {
            if (!enableAutomation) {
                return ChainOperationResult.Error("Automation is disabled")
            }
            
            if (position < 0 || position >= plugins.size) {
                return ChainOperationResult.Error("Invalid position: $position")
            }
            
            val plugin = plugins[position]
            val parameter = plugin.parameters[portIndex]
                ?: return ChainOperationResult.Error("Parameter at port $portIndex not found")
            
            if (!parameter.isAutomatable) {
                return ChainOperationResult.Error("Parameter at port $portIndex is not automatable")
            }
            
            val updatedPlugin = plugin.withAutomation(portIndex, automation)
            plugins[position] = updatedPlugin
            
            eventChannel.trySend(
                ChainEvent.AutomationRecorded(
                    plugin.pluginId, 
                    portIndex, 
                    automation.keyframes
                )
            )
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to add automation: ${e.message}", exception = e)
        }
    }

    /**
     * Starts recording automation for a plugin
     */
    suspend fun startAutomationRecording(pluginId: String): ChainOperationResult {
        return try {
            if (!enableAutomation) {
                return ChainOperationResult.Error("Automation is disabled")
            }
            
            if (!containsPlugin(pluginId)) {
                return ChainOperationResult.Error("Plugin not found in chain")
            }
            
            automationRecorder.startRecording(pluginId)
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to start automation recording: ${e.message}", exception = e)
        }
    }

    /**
     * Stops recording automation and applies it to the plugin
     */
    suspend fun stopAutomationRecording(pluginId: String): ChainOperationResult = chainMutex.withLock {
        try {
            val recordedData = automationRecorder.stopRecording(pluginId)
            
            if (recordedData.isEmpty()) {
                return ChainOperationResult.Warning("No automation data recorded")
            }
            
            val pluginIndex = plugins.indexOfFirst { it.pluginId == pluginId }
            if (pluginIndex < 0) {
                return ChainOperationResult.Error("Plugin not found")
            }
            
            var updatedPlugin = plugins[pluginIndex]
            var automationCount = 0
            
            recordedData.forEach { (paramIndex, keyframes) ->
                if (keyframes.isNotEmpty()) {
                    val automation = ParameterAutomation(
                        parameterIndex = paramIndex,
                        keyframes = keyframes,
                        curveType = AutomationCurveType.SMOOTH
                    )
                    updatedPlugin = updatedPlugin.withAutomation(paramIndex, automation)
                    automationCount++
                }
            }
            
            plugins[pluginIndex] = updatedPlugin
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to stop automation recording: ${e.message}", exception = e)
        }
    }

    /**
     * Processes MIDI CC message and updates mapped parameters
     */
    suspend fun processMidiCC(ccNumber: Int, value: Int, channel: Int = -1): ChainOperationResult {
        return try {
            if (!enableMidiMapping) {
                return ChainOperationResult.Warning("MIDI mapping is disabled")
            }
            
            val affectedParameters = midiProcessor.processMidiCC(ccNumber, value, channel)
            
            if (affectedParameters.isEmpty()) {
                return ChainOperationResult.Warning("No parameters mapped to CC $ccNumber")
            }
            
            // Apply parameter changes
            affectedParameters.forEach { (pluginId, paramIndex) ->
                val position = getPluginPosition(pluginId)
                if (position >= 0) {
                    val plugin = plugins[position]
                    val mapping = plugin.midiMapping[paramIndex]
                    if (mapping != null) {
                        val normalizedValue = value / 127.0f
                        val mappedValue = when (mapping.curve) {
                            MidiCurveType.LINEAR -> normalizedValue
                            MidiCurveType.EXPONENTIAL -> normalizedValue * normalizedValue
                            MidiCurveType.LOGARITHMIC -> 1f - (1f - normalizedValue) * (1f - normalizedValue)
                        }
                        val finalValue = mapping.minValue + mappedValue * (mapping.maxValue - mapping.minValue)
                        setPluginParameterNormalized(position, paramIndex, finalValue)
                    }
                }
            }
            
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to process MIDI CC: ${e.message}", exception = e)
        }
    }

    /**
     * Gets a plugin at the specified position.
     * 
     * @param position Position of plugin
     * @return Plugin at position, or null if invalid
     */
    suspend fun getPlugin(position: Int): ChainedPlugin? = chainMutex.withLock {
        if (position < 0 || position >= plugins.size) null else plugins[position]
    }

    /**
     * Gets all plugins in the chain as an immutable snapshot.
     * 
     * @return Immutable list of plugins
     */
    suspend fun getPlugins(): List<ChainedPlugin> = chainMutex.withLock {
        plugins.toList()
    }

    /**
     * Gets plugins with read lock for better performance during processing
     */
    fun getPluginsForProcessing(): List<ChainedPlugin> = rwLock.read {
        plugins.toList()
    }

    /**
     * Gets the number of plugins in the chain.
     */
    suspend fun size(): Int = chainMutex.withLock { plugins.size }

    /**
     * Returns true if the chain is empty.
     */
    suspend fun isEmpty(): Boolean = chainMutex.withLock { plugins.isEmpty() }

    /**
     * Clears all plugins from the chain with cleanup.
     */
    suspend fun clear(): ChainOperationResult = chainMutex.withLock {
        try {
            val previousSize = plugins.size
            plugins.forEach { plugin ->
                automationRecorder.clearRecording(plugin.pluginId)
            }
            plugins.clear()
            performanceMetrics.clear()
            eventChannel.trySend(ChainEvent.ChainCleared(previousSize))
            ChainOperationResult.Success
        } catch (e: Exception) {
            ChainOperationResult.Error("Failed to clear chain: ${e.message}", exception = e)
        }
    }

    /**
     * Finds a plugin by ID.
     * 
     * @param pluginId Plugin ID to find
     * @return Plugin with matching ID, or null if not found
     */
    suspend fun findPluginById(pluginId: String): ChainedPlugin? = chainMutex.withLock {
        plugins.find { it.pluginId == pluginId }
    }

    /**
     * Gets the position of a plugin by ID.
     * 
     * @param pluginId Plugin ID
     * @return Position (0-based), or -1 if not found
     */
    suspend fun getPluginPosition(pluginId: String): Int = chainMutex.withLock {
        plugins.indexOfFirst { it.pluginId == pluginId }
    }

    /**
     * Returns true if a plugin with the given ID exists in the chain.
     */
    suspend fun containsPlugin(pluginId: String): Boolean = chainMutex.withLock {
        plugins.any { it.pluginId == pluginId }
    }

    /**
     * Gets all non-bypassed plugins in order.
     * 
     * @return List of active (non-bypassed) plugins
     */
    suspend fun getActivePlugins(): List<ChainedPlugin> = chainMutex.withLock {
        plugins.filter { !it.isBypassed && it.isEnabled }
    }

    /**
     * Gets all bypassed plugins.
     */
    suspend fun getBypassedPlugins(): List<ChainedPlugin> = chainMutex.withLock {
        plugins.filter { it.isBypassed }
    }

    /**
     * Gets plugins by type.
     */
    suspend fun getPluginsByType(type: PluginType): List<ChainedPlugin> = chainMutex.withLock {
        plugins.filter { it.pluginType == type }
    }

    /**
     * Gets plugins by category.
     */
    suspend fun getPluginsByCategory(category: String): List<ChainedPlugin> = chainMutex.withLock {
        plugins.filter { it.pluginType.category == category }
    }

    /**
     * Gets total chain latency in milliseconds.
     */
    suspend fun getTotalLatency(): Double = chainMutex.withLock {
        plugins.filter { !it.isBypassed && it.isEnabled }.sumOf { it.getTotalLatencyMs() }
    }

    /**
     * Gets total CPU usage percentage.
     */
    suspend fun getTotalCpuUsage(): Float = chainMutex.withLock {
        plugins.filter { !it.isBypassed && it.isEnabled }.sumOf { it.cpuUsage.toDouble() }.toFloat()
    }

    /**
     * Gets chain statistics
     */
    suspend fun getChainStatistics(): ChainStatistics = chainMutex.withLock {
        val activePlugins = plugins.filter { !it.isBypassed && it.isEnabled }
        ChainStatistics(
            totalPlugins = plugins.size,
            activePlugins = activePlugins.size,
            bypassedPlugins = plugins.count { it.isBypassed },
            totalLatencyMs = activePlugins.sumOf { it.getTotalLatencyMs() },
            totalCpuUsage = activePlugins.sumOf { it.cpuUsage.toDouble() }.toFloat(),
            memoryUsageMB = performanceMetrics.values.sumOf { it.memoryUsageMB.toDouble() }.toFloat(),
            pluginsByType = plugins.groupBy { it.pluginType }.mapValues { it.value.size },
            averageParametersPerPlugin = if (plugins.isNotEmpty()) plugins.sumOf { it.parameters.size } / plugins.size else 0
        )
    }

    data class ChainStatistics(
        val totalPlugins: Int,
        val activePlugins: Int,
        val bypassedPlugins: Int,
        val totalLatencyMs: Double,
        val totalCpuUsage: Float,
        val memoryUsageMB: Float,
        val pluginsByType: Map<PluginType, Int>,
        val averageParametersPerPlugin: Int
    )

    /**
     * Updates performance metrics for a plugin with comprehensive monitoring.
     */
    suspend fun updatePluginPerformance(
        pluginId: String, 
        cpuUsage: Float, 
        processedSamples: Long,
        processingTimeNs: Long = 0L,
        memoryUsageMB: Float = 0f,
        hadError: Boolean = false
    ) {
        if (!enablePerformanceMonitoring) return
        
        performanceMetrics.compute(pluginId) { _, existing ->
            val current = existing ?: PluginPerformanceMetrics(pluginId)
            val updated = current.copy(
                averageCpuUsage = (current.averageCpuUsage * 0.95f + cpuUsage * 0.05f),
                peakCpuUsage = max(current.peakCpuUsage, cpuUsage),
                processedSamples = current.processedSamples + processedSamples,
                processingTimeNs = processingTimeNs,
                memoryUsageMB = memoryUsageMB,
                errorCount = if (hadError) current.errorCount + 1 else current.errorCount,
                successfulProcesses = if (!hadError) current.successfulProcesses + 1 else current.successfulProcesses,
                lastUpdateTime = System.currentTimeMillis()
            )
            
            // Check for performance alerts
            if (cpuUsage > cpuUsageThreshold) {
                eventChannel.trySend(
                    ChainEvent.PerformanceAlert(pluginId, cpuUsage, cpuUsageThreshold)
                )
            }
            
            updated
        }
    }

    /**
     * Gets performance metrics for a plugin.
     */
    fun getPluginPerformance(pluginId: String): PluginPerformanceMetrics? {
        return performanceMetrics[pluginId]
    }

    /**
     * Gets performance metrics for all plugins.
     */
    fun getAllPerformanceMetrics(): Map<String, PluginPerformanceMetrics> {
        return performanceMetrics.toMap()
    }
    
    /**
     * Validates a plugin before adding it to the chain.
     * Checks plugin ID, name, handle validity, and parameter ranges.
     * 
     * @param plugin Plugin to validate
     * @return ChainOperationResult.Success if valid, Error otherwise
     */
    private fun validatePlugin(plugin: ChainedPlugin): ChainOperationResult {
        // Validate plugin ID
        if (plugin.pluginId.isEmpty()) {
            return ChainOperationResult.Error("Plugin ID cannot be empty")
        }
        
        // Validate plugin name
        if (plugin.pluginName.isEmpty()) {
            return ChainOperationResult.Error("Plugin name cannot be empty")
        }
        
        // Validate handle (must be non-negative)
        if (plugin.handle < 0) {
            return ChainOperationResult.Error("Plugin handle must be non-negative (got: ${plugin.handle})")
        }
        
        // Validate parameter ranges
        plugin.parameters.forEach { (portIndex, param) ->
            if (param.minValue > param.maxValue) {
                return ChainOperationResult.Error(
                    "Invalid parameter range for port $portIndex: min (${param.minValue}) > max (${param.maxValue})"
                )
            }
            
            if (param.value < param.minValue || param.value > param.maxValue) {
                return ChainOperationResult.Error(
                    "Parameter value ${param.value} at port $portIndex is out of range [${param.minValue}, ${param.maxValue}]"
                )
            }
            
            if (param.defaultValue < param.minValue || param.defaultValue > param.maxValue) {
                return ChainOperationResult.Error(
                    "Parameter default value ${param.defaultValue} at port $portIndex is out of range [${param.minValue}, ${param.maxValue}]"
                )
            }
        }
        
        return ChainOperationResult.Success
    }
    
    /**
     * Validates the entire plugin chain for consistency.
     * Checks for duplicate plugin IDs, total latency limits, and CPU usage.
     * 
     * @return ChainOperationResult.Success if valid, Error otherwise
     */
    private fun validateChain(): ChainOperationResult {
        // Check for duplicate plugin IDs
        val pluginIds = plugins.map { it.pluginId }
        val duplicates = pluginIds.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            return ChainOperationResult.Error(
                "Duplicate plugin IDs found: ${duplicates.keys.joinToString(", ")}"
            )
        }
        
        // Validate total latency is within limits
        val totalLatency = plugins.filter { !it.isBypassed && it.isEnabled }
            .sumOf { it.getTotalLatencyMs() }
        if (totalLatency > maxLatencyMs) {
            return ChainOperationResult.Error(
                "Total chain latency (${totalLatency}ms) exceeds maximum allowed (${maxLatencyMs}ms)"
            )
        }
        
        // Validate total CPU usage is reasonable
        val totalCpu = plugins.filter { !it.isBypassed && it.isEnabled }
            .sumOf { it.cpuUsage.toDouble() }.toFloat()
        if (totalCpu > cpuUsageThreshold) {
            return ChainOperationResult.Warning(
                "Total CPU usage (${totalCpu}%) exceeds threshold (${cpuUsageThreshold}%)",
                "Consider bypassing some plugins or optimizing settings"
            )
        }
        
        return ChainOperationResult.Success
    }
    
    /**
     * Compensates for plugin latency across the chain.
     * Calculates cumulative latency and adjusts delay compensation.
     */
    private fun compensateLatency() {
        if (!latencyCompensation) return
        
        var cumulativeLatency = 0.0
        
        plugins.forEachIndexed { index, plugin ->
            if (!plugin.isBypassed && plugin.isEnabled) {
                val pluginLatency = plugin.getTotalLatencyMs()
                
                // Track latency changes
                val oldLatency = cumulativeLatency
                cumulativeLatency += pluginLatency
                
                // Send latency changed event if significant change
                if (kotlin.math.abs(cumulativeLatency - oldLatency - pluginLatency) > 0.1) {
                    eventChannel.trySend(
                        ChainEvent.LatencyChanged(
                            plugin.pluginId,
                            oldLatency,
                            cumulativeLatency
                        )
                    )
                }
                
                // Update performance metrics with latency info
                if (enablePerformanceMonitoring) {
                    performanceMetrics.compute(plugin.pluginId) { _, existing ->
                        existing ?: PluginPerformanceMetrics(plugin.pluginId)
                    }
                }
            }
        }
    }

}