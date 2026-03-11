package com.example.lspandroid.aap

import android.content.Context
import android.util.Log
import org.androidaudioplugin.AudioPluginService
import org.androidaudioplugin.AudioPluginServiceInformation
import org.androidaudioplugin.PluginInformation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList

/**
 * Android Audio Plugin (AAP) service implementation for LSP plugins.
 * Enables third-party DAW integration with comprehensive plugin management.
 * 
 * Requirement 14: AAP Service Integration
 */
class LspAapService : AudioPluginService() {

    private val activeInstances = ConcurrentHashMap<Long, PluginInstance>()
    private val instanceIdGenerator = AtomicLong(1)
    
    private data class PluginInstance(
        val pluginId: String,
        val nativeHandle: Long,
        val sampleRate: Int,
        val bufferSize: Int,
        val inputChannels: Int,
        val outputChannels: Int,
        val parameterValues: FloatArray,
        val meterValues: FloatArray,
        var isActive: Boolean = true
    )
    
    override fun getPlugins(): List<PluginInformation> {
        return LspPluginRegistry.getAllPlugins().map { plugin ->
            PluginInformation(
                pluginId = plugin.pluginId,
                displayName = plugin.pluginName,
                manufacturerName = "LSP Project",
                version = plugin.version,
                category = plugin.category,
                portCount = plugin.ports.size,
                hasEditor = true,
                isSynth = plugin.isSynth,
                isEffect = plugin.isEffect
            )
        }
    }
    
    override fun getServiceInformation(): AudioPluginServiceInformation {
        return AudioPluginServiceInformation(
            label = "LSP Audio Plugins",
            packageName = packageName,
            className = this::class.java.name
        )
    }

    override fun instantiate(
        pluginId: String,
        sampleRate: Int
    ): Long {
        val plugin = LspPluginRegistry.getPlugin(pluginId)
            ?: throw IllegalArgumentException("Unknown plugin: $pluginId")
        
        val nativeHandle = nativeCreatePlugin(pluginId, sampleRate)
        if (nativeHandle == 0L) {
            Log.e(TAG, "Failed to create native plugin instance for $pluginId")
            throw RuntimeException("Failed to instantiate plugin: $pluginId")
        }
        
        val instanceId = instanceIdGenerator.getAndIncrement()
        val audioInputs = plugin.ports.count { it.type == PortType.AUDIO && it.direction == PortDirection.INPUT }
        val audioOutputs = plugin.ports.count { it.type == PortType.AUDIO && it.direction == PortDirection.OUTPUT }
        val controlPorts = plugin.ports.count { it.type == PortType.CONTROL }
        val meterPorts = plugin.ports.count { it.type == PortType.METER }
        
        val instance = PluginInstance(
            pluginId = pluginId,
            nativeHandle = nativeHandle,
            sampleRate = sampleRate,
            bufferSize = 512, // Default buffer size
            inputChannels = audioInputs,
            outputChannels = audioOutputs,
            parameterValues = FloatArray(controlPorts) { plugin.getDefaultValue(it) },
            meterValues = FloatArray(meterPorts)
        )
        
        activeInstances[instanceId] = instance
        
        // Initialize plugin with default parameters
        plugin.ports.forEachIndexed { index, port ->
            if (port.type == PortType.CONTROL && port.direction == PortDirection.INPUT) {
                nativeSetParameter(nativeHandle, index, plugin.getDefaultValue(index))
            }
        }
        
        Log.d(TAG, "Created plugin instance $instanceId for $pluginId")
        return instanceId
    }

    override fun destroy(instanceId: Long) {
        val instance = activeInstances.remove(instanceId)
        if (instance != null) {
            instance.isActive = false
            nativeDestroyPlugin(instance.nativeHandle)
            Log.d(TAG, "Destroyed plugin instance $instanceId")
        } else {
            Log.w(TAG, "Attempted to destroy non-existent instance $instanceId")
        }
    }

    override fun process(
        instanceId: Long,
        inputBuffers: Array<FloatArray>,
        outputBuffers: Array<FloatArray>,
        frameCount: Int
    ) {
        val instance = activeInstances[instanceId]
        if (instance == null || !instance.isActive) {
            Log.w(TAG, "Process called on invalid instance $instanceId")
            return
        }
        
        try {
            // Validate buffer dimensions
            if (inputBuffers.size != instance.inputChannels || 
                outputBuffers.size != instance.outputChannels) {
                Log.e(TAG, "Buffer channel count mismatch for instance $instanceId")
                return
            }
            
            // Ensure all buffers have correct frame count
            for (i in inputBuffers.indices) {
                if (inputBuffers[i].size < frameCount) {
                    Log.e(TAG, "Input buffer $i too small for instance $instanceId")
                    return
                }
            }
            
            for (i in outputBuffers.indices) {
                if (outputBuffers[i].size < frameCount) {
                    Log.e(TAG, "Output buffer $i too small for instance $instanceId")
                    return
                }
            }
            
            nativeProcess(instance.nativeHandle, inputBuffers, outputBuffers, frameCount)
            
            // Update meter values after processing
            val plugin = LspPluginRegistry.getPlugin(instance.pluginId)
            plugin?.ports?.forEachIndexed { index, port ->
                if (port.type == PortType.METER && port.direction == PortDirection.OUTPUT) {
                    val meterIndex = plugin.getMeterIndex(index)
                    if (meterIndex >= 0 && meterIndex < instance.meterValues.size) {
                        instance.meterValues[meterIndex] = nativeGetParameter(instance.nativeHandle, index)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio for instance $instanceId", e)
        }
    }

    override fun setParameter(
        instanceId: Long,
        portIndex: Int,
        value: Float
    ) {
        val instance = activeInstances[instanceId]
        if (instance == null || !instance.isActive) {
            Log.w(TAG, "SetParameter called on invalid instance $instanceId")
            return
        }
        
        val plugin = LspPluginRegistry.getPlugin(instance.pluginId)
        if (plugin == null) {
            Log.e(TAG, "Plugin not found for instance $instanceId")
            return
        }
        
        if (portIndex < 0 || portIndex >= plugin.ports.size) {
            Log.e(TAG, "Invalid port index $portIndex for instance $instanceId")
            return
        }
        
        val port = plugin.ports[portIndex]
        if (port.type != PortType.CONTROL || port.direction != PortDirection.INPUT) {
            Log.e(TAG, "Port $portIndex is not a control input for instance $instanceId")
            return
        }
        
        // Clamp value to valid range
        val clampedValue = value.coerceIn(port.minValue, port.maxValue)
        
        try {
            nativeSetParameter(instance.nativeHandle, portIndex, clampedValue)
            val paramIndex = plugin.getControlIndex(portIndex)
            if (paramIndex >= 0 && paramIndex < instance.parameterValues.size) {
                instance.parameterValues[paramIndex] = clampedValue
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting parameter $portIndex for instance $instanceId", e)
        }
    }

    override fun getParameter(
        instanceId: Long,
        portIndex: Int
    ): Float {
        val instance = activeInstances[instanceId]
        if (instance == null || !instance.isActive) {
            Log.w(TAG, "GetParameter called on invalid instance $instanceId")
            return 0.0f
        }
        
        val plugin = LspPluginRegistry.getPlugin(instance.pluginId)
        if (plugin == null) {
            Log.e(TAG, "Plugin not found for instance $instanceId")
            return 0.0f
        }
        
        if (portIndex < 0 || portIndex >= plugin.ports.size) {
            Log.e(TAG, "Invalid port index $portIndex for instance $instanceId")
            return 0.0f
        }
        
        val port = plugin.ports[portIndex]
        
        return try {
            when (port.type) {
                PortType.CONTROL -> {
                    if (port.direction == PortDirection.INPUT) {
                        val paramIndex = plugin.getControlIndex(portIndex)
                        if (paramIndex >= 0 && paramIndex < instance.parameterValues.size) {
                            instance.parameterValues[paramIndex]
                        } else {
                            nativeGetParameter(instance.nativeHandle, portIndex)
                        }
                    } else {
                        nativeGetParameter(instance.nativeHandle, portIndex)
                    }
                }
                PortType.METER -> {
                    val meterIndex = plugin.getMeterIndex(portIndex)
                    if (meterIndex >= 0 && meterIndex < instance.meterValues.size) {
                        instance.meterValues[meterIndex]
                    } else {
                        nativeGetParameter(instance.nativeHandle, portIndex)
                    }
                }
                else -> 0.0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting parameter $portIndex for instance $instanceId", e)
            0.0f
        }
    }

    // Native methods
    private external fun nativeCreatePlugin(pluginId: String, sampleRate: Int): Long
    private external fun nativeDestroyPlugin(instanceId: Long)
    private external fun nativeProcess(
        instanceId: Long,
        inputBuffers: Array<FloatArray>,
        outputBuffers: Array<FloatArray>,
        frameCount: Int
    )
    private external fun nativeSetParameter(instanceId: Long, portIndex: Int, value: Float)
    private external fun nativeGetParameter(instanceId: Long, portIndex: Int): Float

    companion object {
        private const val TAG = "LspAapService"
        init {
            System.loadLibrary("lsp_android_bridge")
        }
    }
}

/**
 * Comprehensive registry of available LSP plugins for AAP integration.
 * Contains detailed metadata for all supported plugins.
 */
object LspPluginRegistry {
    data class PluginDescriptor(
        val pluginId: String,
        val pluginName: String,
        val version: String,
        val category: String,
        val ports: List<PortDescriptor>,
        val isSynth: Boolean = false,
        val isEffect: Boolean = true,
        val description: String = "",
        val author: String = "LSP Project",
        val website: String = "https://lsp-plug.in"
    ) {
        fun getDefaultValue(portIndex: Int): Float {
            return if (portIndex >= 0 && portIndex < ports.size) {
                ports[portIndex].defaultValue
            } else 0.0f
        }
        
        fun getControlIndex(portIndex: Int): Int {
            var controlIndex = 0
            for (i in 0 until portIndex) {
                if (ports[i].type == PortType.CONTROL && ports[i].direction == PortDirection.INPUT) {
                    controlIndex++
                }
            }
            return if (ports[portIndex].type == PortType.CONTROL && 
                      ports[portIndex].direction == PortDirection.INPUT) controlIndex else -1
        }
        
        fun getMeterIndex(portIndex: Int): Int {
            var meterIndex = 0
            for (i in 0 until portIndex) {
                if (ports[i].type == PortType.METER) {
                    meterIndex++
                }
            }
            return if (ports[portIndex].type == PortType.METER) meterIndex else -1
        }
    }
    
    data class PortDescriptor(
        val index: Int,
        val name: String,
        val type: PortType,
        val direction: PortDirection,
        val minValue: Float = 0.0f,
        val maxValue: Float = 1.0f,
        val defaultValue: Float = 0.0f,
        val unit: String = "",
        val isLogarithmic: Boolean = false,
        val isInteger: Boolean = false
    )
    
    enum class PortType {
        AUDIO, CONTROL, METER
    }
    
    enum class PortDirection {
        INPUT, OUTPUT
    }
    
    private val pluginDatabase = listOf(
        // Parametric EQ x32 Stereo
            PluginDescriptor(
            pluginId = "lsp.eq.parametric_x32_lr",
            pluginName = "Parametric EQ x32 Left/Right",
            version = "1.2.14",
            category = "EQ",
            description = "32-band parametric equalizer with spectrum analyzer and comprehensive filtering options",
            ports = buildList {
                // Audio I/O
                add(PortDescriptor(0, "Input Left", PortType.AUDIO, PortDirection.INPUT))
                add(PortDescriptor(1, "Input Right", PortType.AUDIO, PortDirection.INPUT))
                add(PortDescriptor(2, "Output Left", PortType.AUDIO, PortDirection.OUTPUT))
                add(PortDescriptor(3, "Output Right", PortType.AUDIO, PortDirection.OUTPUT))
                
                // Global controls
                add(PortDescriptor(4, "Bypass", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(5, "Input Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                add(PortDescriptor(6, "Output Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                add(PortDescriptor(7, "Balance", PortType.CONTROL, PortDirection.INPUT, -100.0f, 100.0f, 0.0f, "%"))
                add(PortDescriptor(8, "Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 7.0f, 0.0f, "", false, true))
                add(PortDescriptor(9, "Slope", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true))
                add(PortDescriptor(10, "Listen", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(11, "FFT", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 1.0f, "", false, true))
                add(PortDescriptor(12, "Analyzer", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 1.0f, "", false, true))
                add(PortDescriptor(13, "Freeze", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(14, "Zoom", PortType.CONTROL, PortDirection.INPUT, 0.125f, 8.0f, 1.0f, "x"))
                add(PortDescriptor(15, "Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 50.0f, "%"))
                add(PortDescriptor(16, "Shift Gain", PortType.CONTROL, PortDirection.INPUT, -72.0f, 24.0f, 0.0f, "dB"))
                
                // Filter sections (16 bands for comprehensive EQ)
                for (band in 0 until 16) {
                    val baseIndex = 17 + band * 8
                    val defaultFreq = when (band) {
                        0 -> 16.0f; 1 -> 25.0f; 2 -> 40.0f; 3 -> 63.0f
                        4 -> 100.0f; 5 -> 160.0f; 6 -> 250.0f; 7 -> 400.0f
                        8 -> 630.0f; 9 -> 1000.0f; 10 -> 1600.0f; 11 -> 2500.0f
                        12 -> 4000.0f; 13 -> 6300.0f; 14 -> 10000.0f; 15 -> 16000.0f
                        else -> 1000.0f
                    }
                    add(PortDescriptor(baseIndex, "Filter ${band + 1} On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 1, "Filter ${band + 1} Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 7.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 2, "Filter ${band + 1} Slope", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 3, "Filter ${band + 1} Solo", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 4, "Filter ${band + 1} Mute", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 5, "Filter ${band + 1} Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, defaultFreq, "Hz", true))
                    add(PortDescriptor(baseIndex + 6, "Filter ${band + 1} Gain", PortType.CONTROL, PortDirection.INPUT, -36.0f, 36.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 7, "Filter ${band + 1} Quality", PortType.CONTROL, PortDirection.INPUT, 0.1f, 100.0f, 1.0f, "", true))
                }
                
                // High-pass and Low-pass filters
                add(PortDescriptor(145, "HPF On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(146, "HPF Slope", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true))
                add(PortDescriptor(147, "HPF Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, 10.0f, "Hz", true))
                add(PortDescriptor(148, "LPF On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(149, "LPF Slope", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true))
                add(PortDescriptor(150, "LPF Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, 20000.0f, "Hz", true))
                // Meters
                add(PortDescriptor(151, "Input Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                add(PortDescriptor(152, "Input Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                add(PortDescriptor(153, "Output Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                add(PortDescriptor(154, "Output Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                
                // Spectrum analyzer data (simplified representation)
                for (bin in 0 until 32) {
                    add(PortDescriptor(155 + bin, "Spectrum Bin ${bin + 1}", PortType.METER, PortDirection.OUTPUT, -120.0f, 12.0f, -120.0f, "dB"))
                }
            }
        ),
        
        // Compressor Stereo
        PluginDescriptor(
            pluginId = "lsp.dynamics.compressor_lr",
            pluginName = "Compressor Left/Right",
            version = "1.2.14",
            category = "Dynamics",
            description = "Professional stereo compressor with advanced sidechain processing, multiple detection modes, and comprehensive metering",
            ports = listOf(
                // Audio I/O
                PortDescriptor(0, "Input Left", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(1, "Input Right", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(2, "Sidechain Left", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(3, "Sidechain Right", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(4, "Output Left", PortType.AUDIO, PortDirection.OUTPUT),
                PortDescriptor(5, "Output Right", PortType.AUDIO, PortDirection.OUTPUT),
                
                // Main controls
                PortDescriptor(6, "Bypass", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(7, "Input Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(8, "Output Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(9, "Pause", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(10, "Clear", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                
                // Compression parameters
                PortDescriptor(11, "Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5.0f, 0.0f, "", false, true),
                PortDescriptor(12, "Threshold", PortType.CONTROL, PortDirection.INPUT, -60.0f, 0.0f, -12.0f, "dB"),
                PortDescriptor(13, "Ratio", PortType.CONTROL, PortDirection.INPUT, 1.0f, 100.0f, 4.0f, ":1", true),
                PortDescriptor(14, "Knee", PortType.CONTROL, PortDirection.INPUT, 0.0f, 30.0f, 2.0f, "dB"),
                PortDescriptor(15, "Attack", PortType.CONTROL, PortDirection.INPUT, 0.0f, 500.0f, 5.0f, "ms", true),
                PortDescriptor(16, "Release", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5000.0f, 100.0f, "ms", true),
                PortDescriptor(17, "Makeup Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(18, "Dry Amount", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 0.0f, "%"),
                PortDescriptor(19, "Wet Amount", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 100.0f, "%"),
                PortDescriptor(20, "Dry/Wet Balance", PortType.CONTROL, PortDirection.INPUT, -100.0f, 100.0f, 0.0f, "%"),
                
                // Advanced controls
                PortDescriptor(21, "Lookahead", PortType.CONTROL, PortDirection.INPUT, 0.0f, 20.0f, 0.0f, "ms"),
                PortDescriptor(22, "Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 250.0f, 10.0f, "ms"),
                PortDescriptor(23, "Boost Threshold", PortType.CONTROL, PortDirection.INPUT, -60.0f, 0.0f, -60.0f, "dB"),
                PortDescriptor(24, "Boost Ratio", PortType.CONTROL, PortDirection.INPUT, 1.0f, 100.0f, 1.0f, ":1", true),
                PortDescriptor(25, "Boost Knee", PortType.CONTROL, PortDirection.INPUT, 0.0f, 30.0f, 2.0f, "dB"),
                PortDescriptor(26, "Boost Makeup", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                
                // Sidechain controls
                PortDescriptor(27, "Sidechain Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true),
                PortDescriptor(28, "Sidechain Source", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(29, "Sidechain Preamp", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(30, "Sidechain Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 250.0f, 10.0f, "ms"),
                PortDescriptor(31, "Sidechain Lookahead", PortType.CONTROL, PortDirection.INPUT, 0.0f, 20.0f, 0.0f, "ms"),
                
                // Sidechain EQ
                PortDescriptor(32, "SC HPF On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(33, "SC HPF Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, 10.0f, "Hz", true),
                PortDescriptor(34, "SC LPF On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(35, "SC LPF Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, 20000.0f, "Hz", true),
                
                // Meters
                PortDescriptor(36, "Input Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(37, "Input Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(38, "Output Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(39, "Output Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(40, "Gain Reduction Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB"),
                PortDescriptor(41, "Gain Reduction Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB"),
                PortDescriptor(42, "Sidechain Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(43, "Sidechain Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(44, "Curve Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(45, "Envelope Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB")
            )
        ),
        
        // Limiter Stereo
        PluginDescriptor(
            pluginId = "lsp.dynamics.limiter_lr",
            pluginName = "Limiter Left/Right",
            version = "1.2.14",
            category = "Dynamics",
            description = "Transparent stereo limiter with ISP (Infinite Sample Precision) technology, advanced oversampling, and comprehensive dithering options",
            ports = listOf(
                // Audio I/O
                PortDescriptor(0, "Input Left", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(1, "Input Right", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(2, "Output Left", PortType.AUDIO, PortDirection.OUTPUT),
                PortDescriptor(3, "Output Right", PortType.AUDIO, PortDirection.OUTPUT),
                
                // Main controls
                PortDescriptor(4, "Bypass", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(5, "Input Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(6, "Output Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(7, "Pause", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(8, "Clear", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                
                // Limiting parameters
                PortDescriptor(9, "Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5.0f, 0.0f, "", false, true),
                PortDescriptor(10, "Threshold", PortType.CONTROL, PortDirection.INPUT, -60.0f, 0.0f, -0.1f, "dB"),
                PortDescriptor(11, "Lookahead", PortType.CONTROL, PortDirection.INPUT, 0.0f, 20.0f, 5.0f, "ms"),
                PortDescriptor(12, "Attack", PortType.CONTROL, PortDirection.INPUT, 0.01f, 50.0f, 0.5f, "ms", true),
                PortDescriptor(13, "Release", PortType.CONTROL, PortDirection.INPUT, 1.0f, 1000.0f, 50.0f, "ms", true),
                PortDescriptor(14, "Knee", PortType.CONTROL, PortDirection.INPUT, 0.0f, 10.0f, 0.5f, "dB"),
                PortDescriptor(15, "Stereo Link", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 100.0f, "%"),
                
                // Advanced processing
                PortDescriptor(16, "Oversampling", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 2.0f, "", false, true),
                PortDescriptor(17, "Dithering", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true),
                PortDescriptor(18, "Dither Bits", PortType.CONTROL, PortDirection.INPUT, 1.0f, 32.0f, 16.0f, "bits", false, true),
                PortDescriptor(19, "ISP", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 1.0f, "", false, true),
                PortDescriptor(20, "Pumping", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 0.0f, "%"),
                PortDescriptor(21, "Alr On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(22, "Alr Attack", PortType.CONTROL, PortDirection.INPUT, 0.1f, 100.0f, 5.0f, "ms", true),
                PortDescriptor(23, "Alr Release", PortType.CONTROL, PortDirection.INPUT, 1.0f, 5000.0f, 50.0f, "ms", true),
                
                // Meters
                PortDescriptor(24, "Input Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(25, "Input Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(26, "Output Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(27, "Output Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(28, "Gain Reduction Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB"),
                PortDescriptor(29, "Gain Reduction Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB"),
                PortDescriptor(30, "Alr Gain Reduction", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB")
    )
        ),
    
        // Gate Stereo
        PluginDescriptor(
            pluginId = "lsp.dynamics.gate_lr",
            pluginName = "Gate Left/Right",
            version = "1.2.14",
            category = "Dynamics",
            description = "Professional stereo noise gate with advanced sidechain processing, hysteresis control, and comprehensive envelope shaping",
            ports = listOf(
                // Audio I/O
                PortDescriptor(0, "Input Left", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(1, "Input Right", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(2, "Sidechain Left", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(3, "Sidechain Right", PortType.AUDIO, PortDirection.INPUT),
                PortDescriptor(4, "Output Left", PortType.AUDIO, PortDirection.OUTPUT),
                PortDescriptor(5, "Output Right", PortType.AUDIO, PortDirection.OUTPUT),
                
                // Main controls
                PortDescriptor(6, "Bypass", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(7, "Input Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(8, "Output Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(9, "Pause", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(10, "Clear", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                
                // Gate parameters
                PortDescriptor(11, "Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5.0f, 0.0f, "", false, true),
                PortDescriptor(12, "Threshold", PortType.CONTROL, PortDirection.INPUT, -80.0f, 0.0f, -24.0f, "dB"),
                PortDescriptor(13, "Zone", PortType.CONTROL, PortDirection.INPUT, 0.0f, 40.0f, 6.0f, "dB"),
                PortDescriptor(14, "Attack", PortType.CONTROL, PortDirection.INPUT, 0.0f, 500.0f, 1.0f, "ms", true),
                PortDescriptor(15, "Release", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5000.0f, 100.0f, "ms", true),
                PortDescriptor(16, "Hold", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5000.0f, 10.0f, "ms", true),
                PortDescriptor(17, "Reduction", PortType.CONTROL, PortDirection.INPUT, -80.0f, 0.0f, -80.0f, "dB"),
                PortDescriptor(18, "Makeup Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(19, "Dry Amount", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 0.0f, "%"),
                PortDescriptor(20, "Wet Amount", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 100.0f, "%"),
                PortDescriptor(21, "Dry/Wet Balance", PortType.CONTROL, PortDirection.INPUT, -100.0f, 100.0f, 0.0f, "%"),
                
                // Advanced controls
                PortDescriptor(22, "Lookahead", PortType.CONTROL, PortDirection.INPUT, 0.0f, 20.0f, 0.0f, "ms"),
                PortDescriptor(23, "Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 250.0f, 10.0f, "ms"),
                
                // Sidechain controls
                PortDescriptor(24, "Sidechain Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 0.0f, "", false, true),
                PortDescriptor(25, "Sidechain Source", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(26, "Sidechain Preamp", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"),
                PortDescriptor(27, "Sidechain Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 250.0f, 10.0f, "ms"),
                PortDescriptor(28, "Sidechain Lookahead", PortType.CONTROL, PortDirection.INPUT, 0.0f, 20.0f, 0.0f, "ms"),
                
                // Sidechain EQ
                PortDescriptor(29, "SC HPF On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(30, "SC HPF Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, 10.0f, "Hz", true),
                PortDescriptor(31, "SC LPF On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true),
                PortDescriptor(32, "SC LPF Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, 20000.0f, "Hz", true),
                
                // Meters
                PortDescriptor(33, "Input Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(34, "Input Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(35, "Output Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(36, "Output Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(37, "Gain Reduction Left", PortType.METER, PortDirection.OUTPUT, -80.0f, 0.0f, 0.0f, "dB"),
                PortDescriptor(38, "Gain Reduction Right", PortType.METER, PortDirection.OUTPUT, -80.0f, 0.0f, 0.0f, "dB"),
                PortDescriptor(39, "Sidechain Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(40, "Sidechain Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(41, "Curve Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"),
                PortDescriptor(42, "Envelope Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB")
            )
        ),
        
        // Spectrum Analyzer x12
        PluginDescriptor(
            pluginId = "lsp.analyzer.spectrum_analyzer_x12",
            pluginName = "Spectrum Analyzer x12",
            version = "1.2.14",
            category = "Utility",
            description = "12-channel spectrum analyzer with advanced visualization, measurement tools, and comprehensive frequency analysis capabilities",
            ports = buildList {
                // Audio I/O (12 stereo channels)
                for (ch in 0 until 12) {
                    add(PortDescriptor(ch * 2, "Input ${ch + 1} Left", PortType.AUDIO, PortDirection.INPUT))
                    add(PortDescriptor(ch * 2 + 1, "Input ${ch + 1} Right", PortType.AUDIO, PortDirection.INPUT))
                    add(PortDescriptor(24 + ch * 2, "Output ${ch + 1} Left", PortType.AUDIO, PortDirection.OUTPUT))
                    add(PortDescriptor(24 + ch * 2 + 1, "Output ${ch + 1} Right", PortType.AUDIO, PortDirection.OUTPUT))
    }
    
                // Global controls
                add(PortDescriptor(48, "Bypass", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(49, "Freeze", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(50, "Analysis Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 2.0f, 0.0f, "", false, true))
                add(PortDescriptor(51, "Window", PortType.CONTROL, PortDirection.INPUT, 0.0f, 6.0f, 3.0f, "", false, true))
                add(PortDescriptor(52, "Envelope", PortType.CONTROL, PortDirection.INPUT, 0.0f, 3.0f, 1.0f, "", false, true))
                add(PortDescriptor(53, "Tolerance", PortType.CONTROL, PortDirection.INPUT, 0.0f, 10.0f, 1.0f, "dB"))
                add(PortDescriptor(54, "FFT Rank", PortType.CONTROL, PortDirection.INPUT, 10.0f, 15.0f, 13.0f, "", false, true))
                add(PortDescriptor(55, "Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 50.0f, "%"))
                add(PortDescriptor(56, "Shift Gain", PortType.CONTROL, PortDirection.INPUT, -72.0f, 24.0f, 0.0f, "dB"))
                add(PortDescriptor(57, "Zoom", PortType.CONTROL, PortDirection.INPUT, 0.25f, 16.0f, 1.0f, "x"))
                add(PortDescriptor(58, "Selector", PortType.CONTROL, PortDirection.INPUT, 0.0f, 11.0f, 0.0f, "", false, true))
                
                // Channel controls (all 12 channels)
                for (ch in 0 until 12) {
                    val baseIndex = 59 + ch * 10
                    add(PortDescriptor(baseIndex, "Channel ${ch + 1} On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, if (ch == 0) 1.0f else 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 1, "Channel ${ch + 1} Solo", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 2, "Channel ${ch + 1} Freeze", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 3, "Channel ${ch + 1} Hue", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, (ch % 12) / 12.0f, ""))
                    add(PortDescriptor(baseIndex + 4, "Channel ${ch + 1} Alpha", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 1.0f, ""))
                    add(PortDescriptor(baseIndex + 5, "Channel ${ch + 1} Shift", PortType.CONTROL, PortDirection.INPUT, -72.0f, 24.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 6, "Channel ${ch + 1} Preamp", PortType.CONTROL, PortDirection.INPUT, -72.0f, 24.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 7, "Channel ${ch + 1} Source", PortType.CONTROL, PortDirection.INPUT, 0.0f, 2.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 8, "Channel ${ch + 1} Type", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 9, "Channel ${ch + 1} Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 2.0f, 0.0f, "", false, true))
    }
    
                // Meters for all channels
                for (ch in 0 until 12) {
                    add(PortDescriptor(179 + ch, "Channel ${ch + 1} Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                }
                
                // Spectrum data (64 frequency bins for detailed analysis)
                for (bin in 0 until 64) {
                    add(PortDescriptor(191 + bin, "Spectrum Bin ${bin + 1}", PortType.METER, PortDirection.OUTPUT, -120.0f, 12.0f, -120.0f, "dB"))
                }
            }
        ),
        
        // Multiband Compressor Stereo
        PluginDescriptor(
            pluginId = "lsp.dynamics.mb_compressor_lr",
            pluginName = "Multiband Compressor Left/Right",
            version = "1.2.14",
            category = "Dynamics",
            description = "Professional 8-band multiband compressor with linear phase crossovers, advanced envelope control, and comprehensive band processing",
            ports = buildList {
                // Audio I/O
                add(PortDescriptor(0, "Input Left", PortType.AUDIO, PortDirection.INPUT))
                add(PortDescriptor(1, "Input Right", PortType.AUDIO, PortDirection.INPUT))
                add(PortDescriptor(2, "Output Left", PortType.AUDIO, PortDirection.OUTPUT))
                add(PortDescriptor(3, "Output Right", PortType.AUDIO, PortDirection.OUTPUT))
                
                // Global controls
                add(PortDescriptor(4, "Bypass", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(5, "Input Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                add(PortDescriptor(6, "Output Gain", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                add(PortDescriptor(7, "Dry Amount", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 0.0f, "%"))
                add(PortDescriptor(8, "Wet Amount", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 100.0f, "%"))
                add(PortDescriptor(9, "Zoom", PortType.CONTROL, PortDirection.INPUT, 0.125f, 8.0f, 1.0f, "x"))
                add(PortDescriptor(10, "Mode", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5.0f, 0.0f, "", false, true))
                add(PortDescriptor(11, "Envelope Boost", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                add(PortDescriptor(12, "FFT Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 50.0f, "%"))
                add(PortDescriptor(13, "Shift Gain", PortType.CONTROL, PortDirection.INPUT, -72.0f, 24.0f, 0.0f, "dB"))
                
                // Crossover frequencies (7 splits for 8 bands)
                val defaultFreqs = floatArrayOf(40.0f, 100.0f, 252.0f, 632.0f, 1587.0f, 3984.0f, 10000.0f)
                for (split in 0 until 7) {
                    add(PortDescriptor(14 + split, "Split ${split + 1} Frequency", PortType.CONTROL, PortDirection.INPUT, 10.0f, 20000.0f, defaultFreqs[split], "Hz", true))
                    add(PortDescriptor(21 + split, "Split ${split + 1} On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 1.0f, "", false, true))
                }
                
                // Band controls (8 bands)
                for (band in 0 until 8) {
                    val baseIndex = 28 + band * 18
                    add(PortDescriptor(baseIndex, "Band ${band + 1} On", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 1.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 1, "Band ${band + 1} Solo", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 2, "Band ${band + 1} Mute", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 3, "Band ${band + 1} Phase", PortType.CONTROL, PortDirection.INPUT, 0.0f, 1.0f, 0.0f, "", false, true))
                    add(PortDescriptor(baseIndex + 4, "Band ${band + 1} Threshold", PortType.CONTROL, PortDirection.INPUT, -60.0f, 0.0f, -12.0f, "dB"))
                    add(PortDescriptor(baseIndex + 5, "Band ${band + 1} Ratio", PortType.CONTROL, PortDirection.INPUT, 1.0f, 100.0f, 4.0f, ":1", true))
                    add(PortDescriptor(baseIndex + 6, "Band ${band + 1} Knee", PortType.CONTROL, PortDirection.INPUT, 0.0f, 30.0f, 2.0f, "dB"))
                    add(PortDescriptor(baseIndex + 7, "Band ${band + 1} Attack", PortType.CONTROL, PortDirection.INPUT, 0.0f, 500.0f, 5.0f, "ms", true))
                    add(PortDescriptor(baseIndex + 8, "Band ${band + 1} Release", PortType.CONTROL, PortDirection.INPUT, 0.0f, 5000.0f, 100.0f, "ms", true))
                    add(PortDescriptor(baseIndex + 9, "Band ${band + 1} Makeup", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 10, "Band ${band + 1} Mix", PortType.CONTROL, PortDirection.INPUT, 0.0f, 100.0f, 100.0f, "%"))
                    add(PortDescriptor(baseIndex + 11, "Band ${band + 1} Preamp", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 12, "Band ${band + 1} Reactivity", PortType.CONTROL, PortDirection.INPUT, 0.0f, 250.0f, 10.0f, "ms"))
                    add(PortDescriptor(baseIndex + 13, "Band ${band + 1} Lookahead", PortType.CONTROL, PortDirection.INPUT, 0.0f, 20.0f, 0.0f, "ms"))
                    add(PortDescriptor(baseIndex + 14, "Band ${band + 1} Boost Threshold", PortType.CONTROL, PortDirection.INPUT, -60.0f, 0.0f, -60.0f, "dB"))
                    add(PortDescriptor(baseIndex + 15, "Band ${band + 1} Boost Ratio", PortType.CONTROL, PortDirection.INPUT, 1.0f, 100.0f, 1.0f, ":1", true))
                    add(PortDescriptor(baseIndex + 16, "Band ${band + 1} Boost Knee", PortType.CONTROL, PortDirection.INPUT, 0.0f, 30.0f, 2.0f, "dB"))
                    add(PortDescriptor(baseIndex + 17, "Band ${band + 1} Boost Makeup", PortType.CONTROL, PortDirection.INPUT, -60.0f, 60.0f, 0.0f, "dB"))
                }
                
                // Global meters
                add(PortDescriptor(172, "Input Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                add(PortDescriptor(173, "Input Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                add(PortDescriptor(174, "Output Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                add(PortDescriptor(175, "Output Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                
                // Band meters (8 bands)
                for (band in 0 until 8) {
                    val baseIndex = 176 + band * 6
                    add(PortDescriptor(baseIndex, "Band ${band + 1} Level Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                    add(PortDescriptor(baseIndex + 1, "Band ${band + 1} Level Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                    add(PortDescriptor(baseIndex + 2, "Band ${band + 1} Gain Reduction Left", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 3, "Band ${band + 1} Gain Reduction Right", PortType.METER, PortDirection.OUTPUT, -60.0f, 0.0f, 0.0f, "dB"))
                    add(PortDescriptor(baseIndex + 4, "Band ${band + 1} Curve Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                    add(PortDescriptor(baseIndex + 5, "Band ${band + 1} Envelope Level", PortType.METER, PortDirection.OUTPUT, -60.0f, 12.0f, -60.0f, "dB"))
                }
            }
        ),
        
        // Delay Compensator Stereo
        PluginDescriptor(
            pluginId = "lsp.delay.compensator_lr",
            pluginName = "Delay Compensator Left/Right",
            version = "1.2.14",
            category = "Delay",
            description = "Professional stereo delay compensator with distance calculation, temperature compensation, and advanced environmental modeling",
            ports = listOf(
                // Audio I/O
                PortDescriptor(0, "Input Left
