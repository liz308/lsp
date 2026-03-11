package com.example.lspandroid.aap

import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import android.content.Context
import android.view.View
import android.util.Log
import com.example.lspandroid.model.PluginDescriptor
import com.example.lspandroid.model.PortMetadata
import com.example.lspandroid.model.PortType
import com.example.lspandroid.model.PortDirection
import com.example.lspandroid.ui.LayoutGenerator
import org.androidaudioplugin.AudioPluginGuiExtension
import org.androidaudioplugin.AudioPluginService
import org.androidaudioplugin.AudioPluginServiceConnection
import org.androidaudioplugin.AudioPluginServiceConnectionCallback
import org.androidaudioplugin.AudioPluginServiceInfo
import org.androidaudioplugin.AudioPluginServiceHelper
import org.androidaudioplugin.PluginInformation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.os.Handler
import android.os.Looper
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * AAP GUI extension providing Jetpack Compose UI for plugins.
 * Allows DAWs to display plugin UI within their interface.
 *
 * 
 * Requirement 14: AAP Service Integration - GUI Extension
 */
class LspAapGuiExtension : AudioPluginGuiExtension, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "LspAapGuiExtension"
        private const val PARAMETER_UPDATE_DEBOUNCE_MS = 16L // ~60fps for smooth UI
        private const val METER_UPDATE_INTERVAL_MS = 33L // ~30fps for meters
        private const val SERVICE_CONNECTION_TIMEOUT_MS = 5000L
        private const val PARAMETER_GESTURE_TIMEOUT_MS = 100L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        
        // Parameter change reasons for proper automation handling
        private const val PARAMETER_CHANGE_REASON_UI = 1
        private const val PARAMETER_CHANGE_REASON_AUTOMATION = 2
        private const val PARAMETER_CHANGE_REASON_PRESET = 3
    }
    
    private val activeViews = ConcurrentHashMap<Long, ViewState>()
    private val serviceConnections = ConcurrentHashMap<String, ServiceConnectionState>()
    private val parameterUpdateJobs = ConcurrentHashMap<String, Job>()
    private val meterPollingJobs = ConcurrentHashMap<Long, Job>()
    private val gestureTimeouts = ConcurrentHashMap<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionMutex = Mutex()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _parameterUpdates = MutableSharedFlow<ParameterUpdate>(
        replay = 0,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
                )

    private val _meterUpdates = MutableSharedFlow<MeterUpdate>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        startParameterUpdateProcessor()
        startMeterUpdateProcessor()
            }
    
    private data class ViewState(
        val composeView: ComposeView,
        val pluginId: String,
        val instanceId: Long,
        val metadata: PluginDescriptor,
        val parameterValues: MutableState<Map<Int, Float>>,
        val meterValues: MutableState<Map<Int, Float>>,
        val isDestroyed: AtomicBoolean = AtomicBoolean(false),
        val creationTime: Long = System.currentTimeMillis(),
        val lastInteractionTime: AtomicReference<Long> = AtomicReference(System.currentTimeMillis())
                )
        
    private data class ServiceConnectionState(
        val connection: AudioPluginServiceConnection,
        val service: AudioPluginService?,
        val isConnected: AtomicBoolean = AtomicBoolean(false),
        val connectionTime: Long = System.currentTimeMillis(),
        val retryCount: AtomicReference<Int> = AtomicReference(0),
        val lastError: AtomicReference<Exception?> = AtomicReference(null)
                    )
    
    private data class ParameterUpdate(
        val instanceId: Long,
        val portIndex: Int,
        val value: Float,
        val reason: Int = PARAMETER_CHANGE_REASON_UI,
        val timestamp: Long = System.currentTimeMillis()
                    )
    
    private data class MeterUpdate(
        val instanceId: Long,
        val values: Map<Int, Float>,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun createView(
        context: Context,
        pluginId: String,
        instanceId: Long
    ): View {
        Log.d(TAG, "Creating view for plugin: $pluginId, instance: $instanceId")
        
        val plugin = LspPluginRegistry.getPlugin(pluginId)
            ?: throw IllegalArgumentException("Unknown plugin: $pluginId")
        
        // Convert plugin descriptor to comprehensive metadata with full validation
        val metadata = createPluginMetadata(plugin)
        
        // Establish service connection with proper error handling and retries
        establishServiceConnection(context, pluginId, instanceId)

        // Initialize parameter and meter state
        val initialParameterValues = metadata.ports
            .filter { it.type == PortType.CONTROL && it.direction == PortDirection.INPUT }
            .associate { it.index to it.defaultValue }

        val initialMeterValues = metadata.ports
            .filter { it.type == PortType.METER }
            .associate { it.index to 0f }

        // Create Compose view with comprehensive state management
        val composeView = ComposeView(context).apply {
            setContent {
                val parameterValues = remember { mutableStateOf(initialParameterValues) }
                val meterValues = remember { mutableStateOf(initialMeterValues) }

                // Handle parameter updates from automation/presets
                LaunchedEffect(instanceId) {
                    _parameterUpdates
                        .filter { it.instanceId == instanceId && it.reason != PARAMETER_CHANGE_REASON_UI }
                        .collect { update ->
                            parameterValues.value = parameterValues.value + (update.portIndex to update.value)
    }
    }

                // Handle meter updates
                LaunchedEffect(instanceId) {
                    _meterUpdates
                        .filter { it.instanceId == instanceId }
                        .collect { update ->
                            meterValues.value = meterValues.value + update.values
                        }
                }

                // Start meter polling with proper lifecycle management
                LaunchedEffect(instanceId) {
                    startMeterPolling(instanceId, metadata)
                }

                // Initialize parameters from service
                LaunchedEffect(instanceId) {
                    initializeParametersFromService(instanceId, metadata, parameterValues)
                }

                LayoutGenerator.GeneratePluginLayout(
                    metadata = metadata,
                    parameterValues = parameterValues.value,
                    meterValues = meterValues.value,
                    onParameterChange = { portIndex, value ->
                        parameterValues.value = parameterValues.value + (portIndex to value)
                        onParameterChanged(instanceId, portIndex, value)
                    },
                    onParameterGestureStart = { portIndex ->
                        onParameterGestureStart(instanceId, portIndex)
                    },
                    onParameterGestureEnd = { portIndex ->
                        onParameterGestureEnd(instanceId, portIndex)
            }
                )
        // Store view state for cleanup and parameter updates
                val viewState = ViewState(
                    composeView = this@apply,
                    pluginId = pluginId,
                    instanceId = instanceId,
                    metadata = metadata,
                    parameterValues = parameterValues,
                    meterValues = meterValues
                )

                DisposableEffect(instanceId) {
        activeViews[instanceId] = viewState
                    onDispose {
                        // Cleanup handled in destroyView
    }
                }
            }
        }

        Log.d(TAG, "Created view for plugin: $pluginId, instance: $instanceId")
        return composeView
    }

    override fun destroyView(view: View) {
        val viewState = activeViews.values.find { it.composeView == view }
        if (viewState != null) {
            Log.d(TAG, "Destroying view for plugin: ${viewState.pluginId}, instance: ${viewState.instanceId}")

            viewState.isDestroyed.set(true)
            activeViews.remove(viewState.instanceId)

            // Cancel meter polling
            meterPollingJobs[viewState.instanceId]?.cancel()
            meterPollingJobs.remove(viewState.instanceId)

            // Cancel any pending parameter updates
            val parameterJobsToCancel = parameterUpdateJobs.keys.filter {
                it.startsWith("${viewState.instanceId}_")
            }
            parameterJobsToCancel.forEach { key ->
                parameterUpdateJobs[key]?.cancel()
                parameterUpdateJobs.remove(key)
            }

            // Cancel gesture timeouts
            val gestureJobsToCancel = gestureTimeouts.keys.filter {
                it.startsWith("${viewState.instanceId}_")
            }
            gestureJobsToCancel.forEach { key ->
                gestureTimeouts[key]?.cancel()
                gestureTimeouts.remove(key)
            }

            // Clean up service connection if no more views for this plugin
        coroutineScope.launch {
                cleanupServiceConnectionIfUnused(viewState.pluginId)
            establishServiceConnection(context, pluginId, instanceId)
            }
            
            Log.d(TAG, "Destroyed view for plugin: ${viewState.pluginId}, instance: ${viewState.instanceId}")
        }
    }

    private fun createPluginMetadata(plugin: LspPluginRegistry.PluginDescriptor): PluginDescriptor {
        return PluginDescriptor(
            pluginId = plugin.pluginId,
            pluginName = plugin.pluginName,
            version = plugin.version,
            category = plugin.category,
            ports = plugin.ports.map { port ->
                PortMetadata(
                    index = port.index,
                    name = port.name,
                    type = when (port.type) {
                        LspPluginRegistry.PortType.AUDIO -> PortType.AUDIO
                        LspPluginRegistry.PortType.CONTROL -> PortType.CONTROL
                        LspPluginRegistry.PortType.METER -> PortType.METER
                    },
                    direction = when (port.direction) {
                        LspPluginRegistry.PortDirection.INPUT -> PortDirection.INPUT
                        LspPluginRegistry.PortDirection.OUTPUT -> PortDirection.OUTPUT
                    },
                    minValue = port.minValue ?: 0f,
                    maxValue = port.maxValue ?: 1f,
                    defaultValue = port.defaultValue ?: 0.5f,
                    isLogScale = port.isLogScale ?: false,
                    unit = port.unit ?: "",
                    section = port.section ?: "General",
                    stepSize = port.stepSize,
                    enumValues = port.enumValues ?: emptyList(),
                    isToggle = port.isToggle ?: false,
                    displayName = port.displayName ?: port.name
                )
            }
        )
    }

    private suspend fun establishServiceConnection(context: Context, pluginId: String, instanceId: Long) {
        connectionMutex.withLock {
            if (serviceConnections.containsKey(pluginId) &&
                serviceConnections[pluginId]?.isConnected?.get() == true) {
                return // Already connected
            }

            var retryCount = 0
            while (retryCount < MAX_RETRY_ATTEMPTS) {
                            try {
                    val connection = AudioPluginServiceConnection(context)
                    val connectionState = ServiceConnectionState(
                        connection = connection,
                        service = null
                    )

                    serviceConnections[pluginId] = connectionState

                    val connectionCallback = object : AudioPluginServiceConnectionCallback {
                        override fun onConnected(service: AudioPluginService) {
                            Log.d(TAG, "Connected to service for plugin: $pluginId")
                            connectionState.isConnected.set(true)
                            connectionState.retryCount.set(0)
                            connectionState.lastError.set(null)
                            // Initialize plugin instance
                            backgroundScope.launch {
                                initializePluginInstance(service, pluginId, instanceId)
                            }
                        }

                        override fun onDisconnected() {
                            Log.w(TAG, "Disconnected from service for plugin: $pluginId")
                            connectionState.isConnected.set(false)
                        }

                        override fun onError(error: Exception) {
                            Log.e(TAG, "Service connection error for plugin: $pluginId", error)
                            connectionState.lastError.set(error)
                            connectionState.isConnected.set(false)
                    }
                }

                    withTimeout(SERVICE_CONNECTION_TIMEOUT_MS) {
                        connection.connect(pluginId, connectionCallback)
            }

                    // Wait for connection to be established
                    var waitTime = 0L
                    while (!connectionState.isConnected.get() && waitTime < SERVICE_CONNECTION_TIMEOUT_MS) {
                        delay(100)
                        waitTime += 100
        }

                    if (connectionState.isConnected.get()) {
                        Log.d(TAG, "Successfully connected to service for plugin: $pluginId")
            return
                } else {
                        throw Exception("Connection timeout for plugin: $pluginId")
                }

            } catch (e: Exception) {
                    Log.w(TAG, "Failed to establish service connection for plugin: $pluginId (attempt ${retryCount + 1})", e)
                    retryCount++

                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        delay(RETRY_DELAY_MS * retryCount) // Exponential backoff
      external/lsp-plugins/src/ui/ctl/ctl.cpp external/lsp-plugins/src/ui/ctl/CtlAlign.cpp external/lsp-plugins/src/ui/ctl/CtlAudioFile.cpp external/lsp-plugins/src/ui/ctl/CtlAudioSample.cpp external/lsp-plugins/src/ui/ctl/CtlAxis.cpp external/lsp-plugins/src/ui/ctl/CtlBox.cpp external/lsp-plugins/src/ui/ctl/CtlButton.cpp external/lsp-plugins/src/ui/ctl/CtlCapture3D.cpp external/lsp-plugins/src/ui/ctl/CtlCell.cpp external/lsp-plugins/src/ui/ctl/CtlCenter.cpp external/lsp-plugins/src/ui/ctl/CtlColor.cpp external/lsp-plugins/src/ui/ctl/CtlComboBox.cpp external/lsp-plugins/src/ui/ctl/CtlComboGroup.cpp external/lsp-plugins/src/ui/ctl/CtlConfigHandler.cpp external/lsp-plugins/src/ui/ctl/CtlConfigSource.cpp external/lsp-plugins/src/ui/ctl/CtlControlPort.cpp external/lsp-plugins/src/ui/ctl/CtlDot.cpp external/lsp-plugins/src/ui/ctl/CtlEdit.cpp external/lsp-plugins/src/ui/ctl/CtlExpression.cpp external/lsp-plugins/src/ui/ctl/CtlFader.cpp external/lsp-plugins/src/ui/ctl/CtlFraction.cpp external/lsp-plugins/src/ui/ctl/CtlFrameBuffer.cpp external/lsp-plugins/src/ui/ctl/CtlGraph.cpp external/lsp-plugins/src/ui/ctl/CtlGrid.cpp external/lsp-plugins/src/ui/ctl/CtlGroup.cpp external/lsp-plugins/src/ui/ctl/CtlHyperlink.cpp external/lsp-plugins/src/ui/ctl/CtlIndicator.cpp external/lsp-plugins/src/ui/ctl/CtlKnob.cpp external/lsp-plugins/src/ui/ctl/CtlKvtListener.cpp external/lsp-plugins/src/ui/ctl/CtlLabel.cpp external/lsp-plugins/src/ui/ctl/CtlLed.cpp external/lsp-plugins/src/ui/ctl/CtlListBox.cpp external/lsp-plugins/src/ui/ctl/CtlLoadFile.cpp external/lsp-plugins/src/ui/ctl/CtlMarker.cpp external/lsp-plugins/src/ui/ctl/CtlMesh.cpp external/lsp-plugins/src/ui/ctl/CtlMeter.cpp external/lsp-plugins/src/ui/ctl/CtlMidiNote.cpp external/lsp-plugins/src/ui/ctl/CtlPadding.cpp external/lsp-plugins/src/ui/ctl/CtlPathPort.cpp external/lsp-plugins/src/ui/ctl/CtlPluginWindow.cpp external/lsp-plugins/src/ui/ctl/CtlPort.cpp external/lsp-plugins/src/ui/ctl/CtlPortAlias.cpp external/lsp-plugins/src/ui/ctl/CtlPortHandler.cpp external/lsp-plugins/src/ui/ctl/CtlPortListener.cpp external/lsp-plugins/src/ui/ctl/CtlPortResolver.cpp external/lsp-plugins/src/ui/ctl/CtlProgressBar.cpp external/lsp-plugins/src/ui/ctl/CtlRegistry.cpp external/lsp-plugins/src/ui/ctl/CtlSaveFile.cpp external/lsp-plugins/src/ui/ctl/CtlScrollBar.cpp external/lsp-plugins/src/ui/ctl/CtlScrollBox.cpp external/lsp-plugins/src/ui/ctl/CtlSeparator.cpp external/lsp-plugins/src/ui/ctl/CtlSource3D.cpp external/lsp-plugins/src/ui/ctl/CtlStream.cpp external/lsp-plugins/src/ui/ctl/CtlSwitch.cpp external/lsp-plugins/src/ui/ctl/CtlSwitchedPort.cpp external/lsp-plugins/src/ui/ctl/CtlTempoTap.cpp external/lsp-plugins/src/ui/ctl/CtlText.cpp external/lsp-plugins/src/ui/ctl/CtlThreadComboBox.cpp external/lsp-plugins/src/ui/ctl/CtlValuePort.cpp external/lsp-plugins/src/ui/ctl/CtlViewer3D.cpp external/lsp-plugins/src/ui/ctl/CtlVoid.cpp external/lsp-plugins/src/ui/ctl/CtlWidget.cpp external/lsp-plugins/src/ui/ctl/Makefile external/lsp-plugins/src/ui/ctl/parse.cpp              } else {
                        Log.e(TAG, "Failed to establish service connection for plugin: $pluginId after $MAX_RETRY_ATTEMPTS attempts", e)
                        throw e
            }
        }
    }
        }
    }

    private suspend fun initializePluginInstance(service: AudioPluginService, pluginId: String, instanceId: Long) {
        try {
        val viewState = activeViews[instanceId]
            if (viewState == null || viewState.isDestroyed.get()) {
                Log.w(TAG, "ViewState not found or destroyed for instance: $instanceId")
                return
            }

            // Create plugin instance if it doesn't exist
            val pluginInfo = service.getPluginInformation(pluginId)
            if (pluginInfo != null) {
        try {
                    service.createInstance(pluginId, instanceId.toInt(), 44100f, 512)
                    Log.d(TAG, "Created plugin instance: $instanceId for plugin: $pluginId")
        } catch (e: Exception) {
                    // Instance might already exist, which is fine
                    Log.d(TAG, "Plugin instance might already exist: $instanceId", e)
        }

                // Initialize parameters with current values from service
                val controlPorts = viewState.metadata.ports.filter {
                    it.type == PortType.CONTROL && it.direction == PortDirection.INPUT
    }

                val currentValues = mutableMapOf<Int, Float>()
                for (port in controlPorts) {
            try {
                        val currentValue = service.getParameter(instanceId.toInt(), port.index)
                        currentValues[port.index] = currentValue

                        // Emit parameter update if different from default
                        if (currentValue != port.defaultValue) {
                            _parameterUpdates.tryEmit(
                                ParameterUpdate(
                                    instanceId = instanceId,
                                    portIndex = port.index,
                                    value = currentValue,
                                    reason = PARAMETER_CHANGE_REASON_AUTOMATION
                                )
                            )
            }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get parameter value for port ${port.index}", e)
                        currentValues[port.index] = port.defaultValue
        }
    }

                // Update UI state on main thread
                mainHandler.post {
                    viewState.parameterValues.value = currentValues
}
                Log.d(TAG, "Initialized plugin instance: $instanceId with ${currentValues.size} parameters")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize plugin instance: $instanceId", e)
        }
    }

    private suspend fun initializeParametersFromService(
        instanceId: Long,
        metadata: PluginDescriptor,
        parameterValues: MutableState<Map<Int, Float>>
    ) {
        val viewState = activeViews[instanceId] ?: return
        val connectionState = serviceConnections[viewState.pluginId] ?: return

        if (!connectionState.isConnected.get()) {
            // Wait for connection
            var waitTime = 0L
            while (!connectionState.isConnected.get() && waitTime < SERVICE_CONNECTION_TIMEOUT_MS) {
                delay(100)
                waitTime += 100
            }
        }

        val service = connectionState.service
        if (service != null && connectionState.isConnected.get()) {
            val controlPorts = metadata.ports.filter {
                it.type == PortType.CONTROL && it.direction == PortDirection.INPUT
            }

            val currentValues = mutableMapOf<Int, Float>()
            for (port in controlPorts) {
                try {
                    val value = service.getParameter(instanceId.toInt(), port.index)
                    currentValues[port.index] = value
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get initial parameter value for port ${port.index}", e)
                    currentValues[port.index] = port.defaultValue
                }
            }

            parameterValues.value = currentValues
        }
    }

    private fun startMeterPolling(instanceId: Long, metadata: PluginDescriptor) {
        val meterPorts = metadata.ports.filter { it.type == PortType.METER }
        if (meterPorts.isEmpty()) return

        val job = backgroundScope.launch {
            while (activeViews.containsKey(instanceId) &&
                   !activeViews[instanceId]?.isDestroyed?.get()!!) {
                try {
                    val viewState = activeViews[instanceId]
                    val connectionState = viewState?.pluginId?.let { serviceConnections[it] }
                    val service = connectionState?.service

                    if (service != null && connectionState.isConnected.get()) {
                        val meterValues = mutableMapOf<Int, Float>()
                        var hasValidValues = false

                        for (port in meterPorts) {
                            try {
                                val value = service.getParameter(instanceId.toInt(), port.index)
                                meterValues[port.index] = value
                                hasValidValues = true
                            } catch (e: Exception) {
                                Log.v(TAG, "Failed to read meter value for port ${port.index}", e)
                                meterValues[port.index] = 0f
                            }
                        }

                        if (hasValidValues) {
                            _meterUpdates.tryEmit(
                                MeterUpdate(
                                    instanceId = instanceId,
                                    values = meterValues
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.w(TAG, "Error during meter polling for instance $instanceId", e)
                    }
                }

                delay(METER_UPDATE_INTERVAL_MS)
            }
        }

        meterPollingJobs[instanceId] = job
    }

    private fun startParameterUpdateProcessor() {
        coroutineScope.launch {
        // Initialize parameter and meter state
        val initialParameterValues = metadata.ports
            .filter { it.type == PortType.CONTROL && it.direction == PortDirection.INPUT }
            .associate { it.index to it.defaultValue }
            
        val initialMeterValues = metadata.ports
            .filter { it.type == PortType.METER }
            .associate { it.index to 0f }
        
        // Create Compose view with comprehensive state management
        val composeView = ComposeView(context).apply {
            setContent {
                val parameterValues = remember { mutableStateOf(initialParameterValues) }
                val meterValues = remember { mutableStateOf(initialMeterValues) }
                
                // Handle parameter updates from automation/presets
                LaunchedEffect(instanceId) {
            _parameterUpdates
                .buffer(capacity = 1000)
                        .filter { it.instanceId == instanceId && it.reason != PARAMETER_CHANGE_REASON_UI }
                .collect { update ->
                    processParameterUpdate(update)
                            parameterValues.value = parameterValues.value + (update.portIndex to update.value)
                }
        }
    }

    private fun startMeterUpdateProcessor() {
        coroutineScope.launch {
                
                // Handle meter updates
                LaunchedEffect(instanceId) {
            _meterUpdates
                .buffer(capacity = 100)
                        .filter { it.instanceId == instanceId }
                .collect { update ->
                    processMeterUpdate(update)
                            meterValues.value = meterValues.value + update.values
                }
        }
                
                // Start meter polling with proper lifecycle management
                LaunchedEffect(instanceId) {
                    startMeterPolling(instanceId, metadata)
    }
    
    private suspend fun processParameterUpdate(update: ParameterUpdate) {
        val viewState = activeViews[update.instanceId]
        if (viewState == null || viewState.isDestroyed.get()) {
            return
                // Initialize parameters from service
                LaunchedEffect(instanceId) {
                    initializeParametersFromService(instanceId, metadata, parameterValues)
        }
        
        val connectionState = serviceConnections[viewState.pluginId]
        val service = connectionState?.service

        if (service != null && connectionState.isConnected.get()) {
            try {
                service.setParameter(update.instanceId.toInt(), update.portIndex, update.value)
                Log.v(TAG, "Set parameter ${update.portIndex} to ${update.value} for instance ${update.instanceId}")

                // Update interaction time
                viewState.lastInteractionTime.set(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set parameter ${update.portIndex} to ${update.value} for instance ${update.instanceId}", e)
            }
        }
    }

    private suspend fun processMeterUpdate(update: MeterUpdate) {
        val viewState = activeViews[update.instanceId]
        if (viewState != null && !viewState.isDestroyed.get()) {
            mainHandler.post {
                viewState.meterValues.value = viewState.meterValues.value + update.values
            }
        }
    }

    private fun onParameterChanged(instanceId: Long, portIndex: Int, value: Float) {
        val viewState = activeViews[instanceId]
        if (viewState == null || viewState.isDestroyed.get()) {
            Log.w(TAG, "Attempted to change parameter for destroyed view: $instanceId")
            return
        }

        val jobKey = "${instanceId}_${portIndex}"

        // Cancel previous update job for this parameter
        parameterUpdateJobs[jobKey]?.cancel()

        // Debounce parameter updates to avoid overwhelming the audio thread
        parameterUpdateJobs[jobKey] = coroutineScope.launch {
            delay(PARAMETER_UPDATE_DEBOUNCE_MS)

            _parameterUpdates.tryEmit(
                ParameterUpdate(
                    instanceId = instanceId,
                    portIndex = portIndex,
                    value = value,
                    reason = PARAMETER_CHANGE_REASON_UI
                )
            )

            parameterUpdateJobs.remove(jobKey)
        }
    }

    private fun onParameterGestureStart(instanceId: Long, portIndex: Int) {
        val viewState = activeViews[instanceId]
        if (viewState == null || viewState.isDestroyed.get()) return

        val connectionState = serviceConnections[viewState.pluginId]
        val service = connectionState?.service

        if (service != null && connectionState.isConnected.get()) {
            backgroundScope.launch {
                try {
                    service.beginParameterGesture(instanceId.toInt(), portIndex)
                    Log.v(TAG, "Started parameter gesture for port $portIndex, instance $instanceId")

                    // Set up gesture timeout
                    val timeoutKey = "${instanceId}_${portIndex}_gesture"
                    gestureTimeouts[timeoutKey]?.cancel()
                    gestureTimeouts[timeoutKey] = coroutineScope.launch {
                        delay(PARAMETER_GESTURE_TIMEOUT_MS)
                        // Auto-end gesture if not explicitly ended
                LayoutGenerator.GeneratePluginLayout(
                    metadata = metadata,
                    parameterValues = parameterValues.value,
                    meterValues = meterValues.value,
                    onParameterChange = { portIndex, value ->
                        parameterValues.value = parameterValues.value + (portIndex to value)
                        onParameterChanged(instanceId, portIndex, value)
                    },
                    onParameterGestureStart = { portIndex ->
                        onParameterGestureStart(instanceId, portIndex)
                    },
                    onParameterGestureEnd = { portIndex ->
                        onParameterGestureEnd(instanceId, portIndex)
                        gestureTimeouts.remove(timeoutKey)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to start parameter gesture for port $portIndex", e)
                }
            }
        }
    }

    private fun onParameterGestureEnd(instanceId: Long, portIndex: Int) {
        val viewState = activeViews[instanceId]
        if (viewState == null || viewState.isDestroyed.get()) return

        val connectionState = serviceConnections[viewState.pluginId]
        val service = connectionState?.service

        if (service != null && connectionState.isConnected.get()) {
            backgroundScope.launch {
                try {
                    service.endParameterGesture(instanceId.toInt(), portIndex)
                    Log.v(TAG, "Ended parameter gesture for port $portIndex, instance $instanceId")

                    // Cancel gesture timeout
                    val timeoutKey = "${instanceId}_${portIndex}_gesture"
                    gestureTimeouts[timeoutKey]?.cancel()
                    gestureTimeouts.remove(timeoutKey)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to end parameter gesture for port $portIndex", e)
                }
            }
        }
    }

    private suspend fun cleanupServiceConnectionIfUnused(pluginId: String) {
        connectionMutex.withLock {
            val hasActiveViews = activeViews.values.any {
                it.pluginId == pluginId && !it.isDestroyed.get()
            }

            if (!hasActiveViews) {
                serviceConnections[pluginId]?.let { connectionState ->
                    try {
                        connectionState.connection.disconnect()
                        Log.d(TAG, "Disconnected service for unused plugin: $pluginId")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error disconnecting service for plugin: $pluginId", e)
                    }
                }
                serviceConnections.remove(pluginId)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "Application stopped, pausing meter polling")
        meterPollingJobs.values.forEach { it.cancel() }
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Application started, resuming meter polling")
        activeViews.values.forEach { viewState ->
            if (!viewState.isDestroyed.get()) {
                startMeterPolling(viewState.instanceId, viewState.metadata)
            }
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up LspAapGuiExtension")

        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)

        // Cancel all coroutines
        coroutineScope.cancel()
        backgroundScope.cancel()

        // Cancel all jobs
        parameterUpdateJobs.values.forEach { it.cancel() }
        parameterUpdateJobs.clear()

        meterPollingJobs.values.forEach { it.cancel() }
        meterPollingJobs.clear()

        gestureTimeouts.values.forEach { it.cancel() }
        gestureTimeouts.clear()

        // Mark all views as destroyed
        activeViews.values.forEach { it.isDestroyed.set(true) }
        activeViews.clear()

        // Disconnect all service connections
        serviceConnections.values.forEach { connectionState ->
            try {
                connectionState.connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting service during cleanup", e)
            }
        }
        serviceConnections.clear()

        Log.d(TAG, "LspAapGuiExtension cleanup completed")
    }

    // Public API for external parameter automation
    fun setParameterFromAutomation(instanceId: Long, portIndex: Int, value: Float) {
        _parameterUpdates.tryEmit(
            ParameterUpdate(
                )
                
                // Store view state for cleanup and parameter updates
                val viewState = ViewState(
                    composeView = this@apply,
                    pluginId = pluginId,
                instanceId = instanceId,
                portIndex = portIndex,
                value = value,
                reason = PARAMETER_CHANGE_REASON_AUTOMATION
                    metadata = metadata,
                    parameterValues = parameterValues,
                    meterValues = meterValues
            )
        )
                
                DisposableEffect(instanceId) {
                    activeViews[instanceId] = viewState
                    onDispose {
                        // Cleanup handled in destroyView
    }
                }
            }
        }
    
    fun loadPreset(instanceId: Long, presetData: Map<Int, Float>) {
        presetData.forEach { (portIndex, value) ->
            _parameterUpdates.tryEmit(
                ParameterUpdate(
                    instanceId = instanceId,
                    portIndex = portIndex,
                    value = value,
                    reason = PARAMETER_CHANGE_REASON_PRESET
                )
            )
        Log.d(TAG, "Created view for plugin: $pluginId, instance: $instanceId")
        return composeView
    }

    override fun destroyView(view: View) {
        val viewState = activeViews.values.find { it.composeView == view }
        if (viewState != null) {
            Log.d(TAG, "Destroying view for plugin: ${viewState.pluginId}, instance: ${viewState.instanceId}")
            
            viewState.isDestroyed.set(true)
            activeViews.remove(viewState.instanceId)
            
            // Cancel meter polling
            meterPollingJobs[viewState.instanceId]?.cancel()
            meterPollingJobs.remove(viewState.instanceId)
            
            // Cancel any pending parameter updates
            val parameterJobsToCancel = parameterUpdateJobs.keys.filter { 
                it.startsWith("${viewState.instanceId}_") 
            }
            parameterJobsToCancel.forEach { key ->
                parameterUpdateJobs[key]?.cancel()
                parameterUpdateJobs.remove(key)
            }
            
            // Cancel gesture timeouts
            val gestureJobsToCancel = gestureTimeouts.keys.filter {
                it.startsWith("${viewState.instanceId}_")
            }
            gestureJobsToCancel.forEach { key ->
                gestureTimeouts[key]?.cancel()
                gestureTimeouts.remove(key)
            }
            
            // Clean up service connection if no more views for this plugin
            coroutineScope.launch {
                cleanupServiceConnectionIfUnused(viewState.pluginId)
            }
            
            Log.d(TAG, "Destroyed view for plugin: ${viewState.pluginId}, instance: ${viewState.instanceId}")
        }
    }
    
    private fun createPluginMetadata(plugin: LspPluginRegistry.PluginDescriptor): PluginDescriptor {
        return PluginDescriptor(
            pluginId = plugin.pluginId,
            pluginName = plugin.pluginName,
            version = plugin.version,
            category = plugin.category,
            ports = plugin.ports.map { port ->
                PortMetadata(
                    index = port.index,
                    name = port.name,
                    type = when (port.type) {
                        LspPluginRegistry.PortType.AUDIO -> PortType.AUDIO
                        LspPluginRegistry.PortType.CONTROL -> PortType.CONTROL
                        LspPluginRegistry.PortType.METER -> PortType.METER
                    },
                    direction = when (port.direction) {
                        LspPluginRegistry.PortDirection.INPUT -> PortDirection.INPUT
                        LspPluginRegistry.PortDirection.OUTPUT -> PortDirection.OUTPUT
                    },
                    minValue = port.minValue ?: 0f,
                    maxValue = port.maxValue ?: 1f,
                    defaultValue = port.defaultValue ?: 0.5f,
                    isLogScale = port.isLogScale ?: false,
                    unit = port.unit ?: "",
                    section = port.section ?: "General",
                    stepSize = port.stepSize,
                    enumValues = port.enumValues ?: emptyList(),
                    isToggle = port.isToggle ?: false,
                    displayName = port.displayName ?: port.name
                )
        }
        )
    }
    
    fun getCurrentParameterValues(instanceId: Long): Map<Int, Float>? {
        return activeViews[instanceId]?.parameterValues?.value
    private suspend fun establishServiceConnection(context: Context, pluginId: String, instanceId: Long) {
        connectionMutex.withLock {
            if (serviceConnections.containsKey(pluginId) && 
                serviceConnections[pluginId]?.isConnected?.get() == true) {
                return // Already connected
    }
    
    fun getCurrentMeterValues(instanceId: Long): Map<Int, Float>? {
        return activeViews[instanceId]?.meterValues?.value
            var retryCount = 0
            while (retryCount < MAX_RETRY_ATTEMPTS) {
                try {
                    val connection = AudioPluginServiceConnection(context)
                    val connectionState = ServiceConnectionState(
                        connection = connection,
                        service = null
                    )
                    
                    serviceConnections[pluginId] = connectionState
                    
                    val connectionCallback = object : AudioPluginServiceConnectionCallback {
                        override fun onConnected(service: AudioPluginService) {
                            Log.d(TAG, "Connected to service for plugin: $pluginId")
                            connectionState.isConnected.set(true)
                            connectionState.retryCount.set(0)
                            connectionState.lastError.set(null)
                            // Initialize plugin instance
                            backgroundScope.launch {
                                initializePluginInstance(service, pluginId, instanceId)
    }

    fun isInstanceActive(instanceId: Long): Boolean {
        return activeViews[instanceId]?.let { !it.isDestroyed.get() } ?: false
    }
    
    fun getActiveInstances(): List<Long> {
        return activeViews.values
            .filter { !it.isDestroyed.get() }
            .map { it.instanceId }
                        override fun onDisconnected() {
                            Log.w(TAG, "Disconnected from service for plugin: $pluginId")
                            connectionState.isConnected.set(false)
    }
    
    fun getConnectionStatus(pluginId: String): Boolean {
        return serviceConnections[pluginId]?.isConnected?.get() ?: false
                        override fun onError(error: Exception) {
                            Log.e(TAG, "Service connection error for plugin: $pluginId", error)
                            connectionState.lastError.set(error)
                            connectionState.isConnected.set(false)
    }
}

                    withTimeout(SERVICE_CONNECTION_TIMEOUT_MS) {
                        connection.connect(pluginId, connectionCallback)
                    }
                    
                    // Wait for connection to be established
                    var waitTime = 0L
                    while (!connectionState.isConnected.get() && waitTime < SERVICE_CONNECTION_TIMEOUT_MS) {
                        delay(100)
                        waitTime += 100
                    }
                    
                    if (connectionState.isConnected.get()) {
                        Log.d(TAG, "Successfully connected to service for plugin: $pluginId")
                        return
                    } else {
                        throw Exception("Connection timeout for plugin: $pluginId")
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to establish service connection for plugin: $pluginId (attempt ${retryCount + 1})", e)
                    retryCount++
                    
                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        delay(RETRY_DELAY_MS * retryCount) // Exponential backoff
                    } else {
                        Log.e(TAG, "Failed to establish service connection for plugin: $pluginId after $MAX_RETRY_ATTEMPTS attempts", e)
                        throw e
                    }
                }
            }
        }
    }
    
    private suspend fun initializePluginInstance(service: AudioPluginService, pluginId: String, instanceId: Long) {
        try {
            val viewState = activeViews[instanceId]
            if (viewState == null || viewState.isDestroyed.get()) {
                Log.w(TAG, "ViewState not found or destroyed for instance: $instanceId")
                return
            }
            
            // Create plugin instance if it doesn't exist
            val pluginInfo = service.getPluginInformation(pluginId)
            if (pluginInfo != null) {
                try {
                    service.createInstance(pluginId, instanceId.toInt(), 44100f, 512)
                    Log.d(TAG, "Created plugin instance: $instanceId for plugin: $pluginId")
                } catch (e: Exception) {
                    // Instance might already exist, which is fine
                    Log.d(TAG, "Plugin instance might already exist: $instanceId", e)
                }
                
                // Initialize parameters with current values from service
                val controlPorts = viewState.metadata.ports.filter { 
                    it.type == PortType.CONTROL && it.direction == PortDirection.INPUT 
                }
                
                val currentValues = mutableMapOf<Int, Float>()
                for (port in controlPorts) {
                    try {
                        val currentValue = service.getParameter(instanceId.toInt(), port.index)
                        currentValues[port.index] = currentValue
                        
                        // Emit parameter update if different from default
                        if (currentValue != port.defaultValue) {
                            _parameterUpdates.tryEmit(
                                ParameterUpdate(
                                    instanceId = instanceId,
                                    portIndex = port.index,
                                    value = currentValue,
                                    reason = PARAMETER_CHANGE_REASON_AUTOMATION
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get parameter value for port ${port.index}", e)
                        currentValues[port.index] = port.defaultValue
                    }
                }
                
                // Update UI state on main thread
                mainHandler.post {
                    viewState.parameterValues.value = currentValues
                }
                Log.d(TAG, "Initialized plugin instance: $instanceId with ${currentValues.size} parameters")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize plugin instance: $instanceId", e)
        }
    }
    
    private suspend fun initializeParametersFromService(
        instanceId: Long,
        metadata: PluginDescriptor,
        parameterValues: MutableState<Map<Int, Float>>
    ) {
        val viewState = activeViews[instanceId] ?: return
        val connectionState = serviceConnections[viewState.pluginId] ?: return
        
        if (!connectionState.isConnected.get()) {
            // Wait for connection
            var waitTime = 0L
            while (!connectionState.isConnected.get() && waitTime < SERVICE_CONNECTION_TIMEOUT_MS) {
                delay(100)
                waitTime += 100
            }
        }
        
        val service = connectionState.service
        if (service != null && connectionState.isConnected.get()) {
            val controlPorts = metadata.ports.filter { 
                it.type == PortType.CONTROL && it.direction == PortDirection.INPUT 
            }
            
            val currentValues = mutableMapOf<Int, Float>()
            for (port in controlPorts) {
                try {
                    val value = service.getParameter(instanceId.toInt(), port.index)
                    currentValues[port.index] = value
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get initial parameter value for port ${port.index}", e)
                    currentValues[port.index] = port.defaultValue
                }
            }
            
            parameterValues.value = currentValues
        }
    }
    
    private fun startMeterPolling(instanceId: Long, metadata: PluginDescriptor) {
        val meterPorts = metadata.ports.filter { it.type == PortType.METER }
        if (meterPorts.isEmpty()) return
        
        val job = backgroundScope.launch {
            while (activeViews.containsKey(instanceId) && 
                   !activeViews[instanceId]?.isDestroyed?.get()!!) {
                try {
                    val viewState = activeViews[instanceId]
                    val connectionState = viewState?.pluginId?.let { serviceConnections[it] }
                    val service = connectionState?.service
                    
                    if (service != null && connectionState.isConnected.get()) {
                        val meterValues = mutableMapOf<Int, Float>()
                        var hasValidValues = false
                        
                        for (port in meterPorts) {
                            try {
                                val value = service.getParameter(instanceId.toInt(), port.index)
                                meterValues[port.index] = value
                                hasValidValues = true
                            } catch (e: Exception) {
                                Log.v(TAG, "Failed to read meter value for port ${port.index}", e)
                                meterValues[port.index] = 0f
                            }
                        }
                        
                        if (hasValidValues) {
                            _meterUpdates.tryEmit(
                                MeterUpdate(
                                    instanceId = instanceId,
                                    values = meterValues
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.w(TAG, "Error during meter polling for instance $instanceId", e)
                    }
                }
                
                delay(METER_UPDATE_INTERVAL_MS)
            }
        }
        
        meterPollingJobs[instanceId] = job
    }
    
    private fun startParameterUpdateProcessor() {
        coroutineScope.launch {
            _parameterUpdates
                .buffer(capacity = 1000)
                .collect { update ->
                    processParameterUpdate(update)
                }
        }
    }
    
    private fun startMeterUpdateProcessor() {
        coroutineScope.launch {
            _meterUpdates
                .buffer(capacity = 100)
                .collect { update ->
                    processMeterUpdate(update)
                }
        }
    }
    
    private suspend fun processParameterUpdate(update: ParameterUpdate) {
        val viewState = activeViews[update.instanceId]
        if (viewState == null || viewState.isDestroyed.get()) {
            return
        }
        
        val connectionState = serviceConnections[viewState.pluginId]
        val service = connectionState?.service
        
        if (service != null && connectionState.isConnected.get()) {
            try {
                service.setParameter(update.instanceId.toInt(), update.portIndex, update.value)
                Log.v(TAG, "Set parameter ${update.portIndex} to ${update.value} for instance ${update.instanceId}")
                
                // Update interaction time
                viewState.lastInteractionTime.set(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set parameter ${update.portIndex} to ${update.value} for instance ${update.instanceId}", e)
            }
        }
    }
    
    private suspend fun processMeterUpdate(update: MeterUpdate) {
        val viewState = activeViews[update.instanceId]
        if (viewState != null && !viewState.isDestroyed.get()) {
            mainHandler.post {
                viewState.meterValues.value = viewState.meterValues.value + update.values
            }
        }
    }

    private fun onParameterChanged(instanceId: Long, portIndex: Int, value: Float) {
        val viewState = activeViews[instanceId]
        if (viewState == null || viewState.isDestroyed.get()) {
            Log.w(TAG, "Attempted to change parameter for destroyed view: $instanceId")
            return
        }
        
        val jobKey = "${instanceId}_${portIndex}"
        
        // Cancel previous update job for this parameter
        parameterUpdateJobs[jobKey]?.cancel()
        
        // Debounce parameter updates to avoid overwhelming the audio thread
        parameterUpdateJobs[jobKey] = coroutineScope.launch {
            delay(PARAMETER_UPDATE_DEBOUNCE_MS)
            
            _parameterUpdates.tryEmit(
                ParameterUpdate(
                    instanceId = instanceId,
                    portIndex = portIndex,
                    value = value,
                    reason = PARAMETER_CHANGE_REASON_UI
                )
            )
            
            parameterUpdateJobs.remove(jobKey)
        }
    }
    
    private fun onParameterGestureStart(instanceId: Long, portIndex: Int) {
        val viewState = activeViews[instanceId]
        if (viewState == null || viewState.isDestroyed.get()) return
        
        val connectionState = serviceConnections[viewState.pluginId]
        val service = connectionState?.service
        
        if (service != null && connectionState.isConnected.get()) {
            backgroundScope.launch {
                try {
                    service.beginParameterGesture(instanceId.toInt(), portIndex)
                    Log.v(TAG, "Started parameter gesture for port $portIndex, instance $instanceId")
                    
                    // Set up gesture timeout
                    val timeoutKey = "${instanceId}_${portIndex}_gesture"
                    gestureTimeouts[timeoutKey]?.cancel()
                    gestureTimeouts[timeoutKey] = coroutineScope.launch {
                        delay(PARAMETER_GESTURE_TIMEOUT_MS)
                        // Auto-end gesture if not explicitly ended
                        onParameterGestureEnd(instanceId, portIndex)
                        gestureTimeouts.remove(timeoutKey)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to start parameter gesture for port $portIndex", e)
                }
            }
        }
    }
    
    private fun onParameterGestureEnd(instanceId: Long, portIndex: Int) {
        val viewState = activeViews[instanceId]
        if (viewState == null || viewState.isDestroyed.get()) return
        
        val connectionState = serviceConnections[viewState.pluginId]
        val service = connectionState?.service
        
        if (service != null && connectionState.isConnected.get()) {
            backgroundScope.launch {
                try {
                    service.endParameterGesture(instanceId.toInt(), portIndex)
                    Log.v(TAG, "Ended parameter gesture for port $portIndex, instance $instanceId")
                    
                    // Cancel gesture timeout
                    val timeoutKey = "${instanceId}_${portIndex}_gesture"
                    gestureTimeouts[timeoutKey]?.cancel()
                    gestureTimeouts.remove(timeoutKey)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to end parameter gesture for port $portIndex", e)
                }
            }
        }
    }
    
    private suspend fun cleanupServiceConnectionIfUnused(pluginId: String) {
        connectionMutex.withLock {
            val hasActiveViews = activeViews.values.any { 
                it.pluginId == pluginId && !it.isDestroyed.get() 
            }
            
            if (!hasActiveViews) {
                serviceConnections[pluginId]?.let { connectionState ->
                    try {
                        connectionState.connection.disconnect()
                        Log.d(TAG, "Disconnected service for unused plugin: $pluginId")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error disconnecting service for plugin: $pluginId", e)
                    }
                }
                serviceConnections.remove(pluginId)
            }
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "Application stopped, pausing meter polling")
        meterPollingJobs.values.forEach { it.cancel() }
    }
    
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Application started, resuming meter polling")
        activeViews.values.forEach { viewState ->
            if (!viewState.isDestroyed.get()) {
                startMeterPolling(viewState.instanceId, viewState.metadata)
            }
        }
    }
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up LspAapGuiExtension")
        
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        
        // Cancel all coroutines
        coroutineScope.cancel()
        backgroundScope.cancel()
        
        // Cancel all jobs
        parameterUpdateJobs.values.forEach { it.cancel() }
        parameterUpdateJobs.clear()
        
        meterPollingJobs.values.forEach { it.cancel() }
        meterPollingJobs.clear()
        
        gestureTimeouts.values.forEach { it.cancel() }
        gestureTimeouts.clear()
        
        // Mark all views as destroyed
        activeViews.values.forEach { it.isDestroyed.set(true) }
        activeViews.clear()
        
        // Disconnect all service connections
        serviceConnections.values.forEach { connectionState ->
            try {
                connectionState.connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting service during cleanup", e)
            }
        }
        serviceConnections.clear()
        
        Log.d(TAG, "LspAapGuiExtension cleanup completed")
    }
    
    // Public API for external parameter automation
    fun setParameterFromAutomation(instanceId: Long, portIndex: Int, value: Float) {
        _parameterUpdates.tryEmit(
            ParameterUpdate(
                instanceId = instanceId,
                portIndex = portIndex,
                value = value,
                reason = PARAMETER_CHANGE_REASON_AUTOMATION
            )
        )
    }
    
    fun loadPreset(instanceId: Long, presetData: Map<Int, Float>) {
        presetData.forEach { (portIndex, value) ->
            _parameterUpdates.tryEmit(
                ParameterUpdate(
                    instanceId = instanceId,
                    portIndex = portIndex,
                    value = value,
                    reason = PARAMETER_CHANGE_REASON_PRESET
                )
            )
        }
    }
    
    fun getCurrentParameterValues(instanceId: Long): Map<Int, Float>? {
        return activeViews[instanceId]?.parameterValues?.value
    }
    
    fun getCurrentMeterValues(instanceId: Long): Map<Int, Float>? {
        return activeViews[instanceId]?.meterValues?.value
    }
    
    fun isInstanceActive(instanceId: Long): Boolean {
        return activeViews[instanceId]?.let { !it.isDestroyed.get() } ?: false
    }
    
    fun getActiveInstances(): List<Long> {
        return activeViews.values
            .filter { !it.isDestroyed.get() }
            .map { it.instanceId }
    }
    
    fun getConnectionStatus(pluginId: String): Boolean {
        return serviceConnections[pluginId]?.isConnected?.get() ?: false
    }
}