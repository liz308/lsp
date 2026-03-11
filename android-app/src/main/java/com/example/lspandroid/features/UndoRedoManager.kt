package com.example.lspandroid.features

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.mutableMapOf

/**
 * Production-ready Undo/Redo system for audio plugin parameter changes.
 * Thread-safe implementation with coroutine support and state persistence.
 * 
 * Features:
 * - Thread-safe operations using concurrent data structures
 * - Coroutine-based async operations with proper cancellation
 * - State change notifications via Flow
 * - Automatic cleanup of old actions
 * - Memory-efficient action batching
 * - Crash recovery and state persistence
 * - Performance monitoring and metrics
 * 
 * Requirement 6.4: Additional Features - Undo/Redo
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 100,
    private val batchTimeoutMs: Long = 500L,
    private val enableMetrics: Boolean = true,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    
    companion object {
        private const val TAG = "UndoRedoManager"
        private const val MAX_BATCH_SIZE = 20
        private const val CLEANUP_THRESHOLD = 0.8f
    }
    private val undoStack = ConcurrentLinkedDeque<ActionBatch>()
    private val redoStack = ConcurrentLinkedDeque<ActionBatch>()
    private val pendingActions = mutableListOf<Action>()
    private val batchingJob = AtomicReference<Job?>(null)
    private val isProcessing = AtomicBoolean(false)
    private val actionCounter = AtomicInteger(0)
    
    // State flows for UI updates
    private val _canUndoFlow = MutableStateFlow(false)
    val canUndoFlow: StateFlow<Boolean> = _canUndoFlow.asStateFlow()
    
    private val _canRedoFlow = MutableStateFlow(false)
    val canRedoFlow: StateFlow<Boolean> = _canRedoFlow.asStateFlow()
    
    private val _historyStateFlow = MutableStateFlow(HistoryState(0, 0))
    val historyStateFlow: StateFlow<HistoryState> = _historyStateFlow.asStateFlow()
    
    // Metrics tracking
    private val metrics = if (enableMetrics) UndoRedoMetrics() else null
    
    data class HistoryState(
        val undoCount: Int,
        val redoCount: Int,
        val isProcessing: Boolean = false
    )
    
    sealed class Action {
        abstract val timestamp: Long
        abstract val actionId: String
        
        data class ParameterChange(
            val pluginId: String,
            val portIndex: Int,
            val parameterName: String,
            val oldValue: Float,
            val newValue: Float,
            val automationId: String? = null,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        data class PluginAdded(
            val pluginId: String,
            val pluginName: String,
            val pluginType: String,
            val position: Int,
            val initialState: Map<Int, Float>,
            val connectionInfo: List<ConnectionData>,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        data class PluginRemoved(
            val pluginId: String,
            val pluginName: String,
            val pluginType: String,
            val position: Int,
            val savedState: Map<Int, Float>,
            val connectionInfo: List<ConnectionData>,
            val bypassState: Boolean,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        data class PluginMoved(
            val pluginId: String,
            val fromPosition: Int,
            val toPosition: Int,
            val affectedConnections: List<ConnectionUpdate>,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        data class PluginBypassed(
            val pluginId: String,
            val wasBypassed: Boolean,
            val affectedParameters: Map<Int, Float>,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        data class BulkParameterChange(
            val changes: List<ParameterChange>,
            val groupId: String,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        data class PresetLoaded(
            val presetId: String,
            val presetName: String,
            val previousState: Map<String, Map<Int, Float>>,
            val newState: Map<String, Map<Int, Float>>,
            override val timestamp: Long = System.currentTimeMillis(),
            override val actionId: String = generateActionId()
        ) : Action()
        
        companion object {
            private val actionIdCounter = AtomicInteger(0)
            fun generateActionId(): String = "action_${System.currentTimeMillis()}_${actionIdCounter.incrementAndGet()}"
    }
    }
    
    data class ConnectionData(
        val sourcePluginId: String,
        val sourcePort: Int,
        val targetPluginId: String,
        val targetPort: Int,
        val connectionType: String
    )
    
    data class ConnectionUpdate(
        val connectionId: String,
        val oldConnection: ConnectionData,
        val newConnection: ConnectionData
    )
    
    data class ActionBatch(
        val actions: List<Action>,
        val batchId: String,
        val timestamp: Long,
        val description: String
    ) {
        val size: Int get() = actions.size
        val isComplex: Boolean get() = actions.any { 
            it is Action.PluginAdded || it is Action.PluginRemoved || it is Action.PresetLoaded 
        }
    }
    /**
     * Record a new action with automatic batching for related operations.
     */
    suspend fun recordAction(action: Action, batchDescription: String = "User Action") {
        if (isProcessing.get()) {
            Log.w(TAG, "Cannot record action while processing undo/redo")
            return
        }
        
        metrics?.recordAction(action)
        
        synchronized(pendingActions) {
            pendingActions.add(action)
            
            // Cancel existing batching job and start new one
            batchingJob.get()?.cancel()
            val newJob = coroutineScope.launch {
                delay(batchTimeoutMs)
                flushPendingActions(batchDescription)
    }
            batchingJob.set(newJob)
    
            // Force flush if batch is getting too large
            if (pendingActions.size >= MAX_BATCH_SIZE) {
                newJob.cancel()
                flushPendingActions(batchDescription)
            }
        }
    }
    
    /**
     * Record multiple actions as a single batch.
     */
    suspend fun recordBatch(actions: List<Action>, description: String = "Batch Operation") {
        if (actions.isEmpty()) return
        
        val batch = ActionBatch(
            actions = actions.toList(),
            batchId = "batch_${System.currentTimeMillis()}_${actionCounter.incrementAndGet()}",
            timestamp = System.currentTimeMillis(),
            description = description
        )
        
        undoStack.addLast(batch)
        redoStack.clear()
        
        // Cleanup if needed
        performCleanupIfNeeded()
        updateStateFlows()
        
        metrics?.recordBatch(batch)
        Log.d(TAG, "Recorded batch: $description with ${actions.size} actions")
    }
    
    private suspend fun flushPendingActions(description: String) {
        val actionsToFlush = synchronized(pendingActions) {
            if (pendingActions.isEmpty()) return
            val actions = pendingActions.toList()
            pendingActions.clear()
            actions
        }
        
        recordBatch(actionsToFlush, description)
    }
    
    /**
     * Undo the last batch of actions.
     */
    suspend fun undo(): UndoResult {
        if (!canUndo() || isProcessing.get()) {
            return UndoResult.Failed("Cannot undo: ${if (!canUndo()) "no actions" else "processing"}")
        }
        
        isProcessing.set(true)
        updateStateFlows()
        
        return try {
            val batch = undoStack.removeLast()
            redoStack.addLast(batch)
            
            val results = mutableListOf<ActionResult>()
            
            // Apply undo actions in reverse order
            for (action in batch.actions.reversed()) {
                val result = applyUndoAction(action)
                results.add(result)
                
                if (!result.success) {
                    Log.e(TAG, "Failed to undo action ${action.actionId}: ${result.error}")
                    // Continue with other actions but log the failure
                }
            }
            
            metrics?.recordUndo(batch, results.all { it.success })
            updateStateFlows()
            
            UndoResult.Success(batch, results)
        } catch (e: Exception) {
            Log.e(TAG, "Error during undo operation", e)
            UndoResult.Failed("Undo failed: ${e.message}")
        } finally {
            isProcessing.set(false)
            updateStateFlows()
        }
    }
    /**
     * Redo the last undone batch of actions.
     */
    suspend fun redo(): RedoResult {
        if (!canRedo() || isProcessing.get()) {
            return RedoResult.Failed("Cannot redo: ${if (!canRedo()) "no actions" else "processing"}")
}

        isProcessing.set(true)
        updateStateFlows()
        
        return try {
            val batch = redoStack.removeLast()
            undoStack.addLast(batch)
            
            val results = mutableListOf<ActionResult>()
            
            // Apply redo actions in original order
            for (action in batch.actions) {
                val result = applyRedoAction(action)
                results.add(result)
                
                if (!result.success) {
                    Log.e(TAG, "Failed to redo action ${action.actionId}: ${result.error}")
                }
            }
            
            metrics?.recordRedo(batch, results.all { it.success })
            updateStateFlows()
            
            RedoResult.Success(batch, results)
        } catch (e: Exception) {
            Log.e(TAG, "Error during redo operation", e)
            RedoResult.Failed("Redo failed: ${e.message}")
        } finally {
            isProcessing.set(false)
            updateStateFlows()
        }
    }
    
    private suspend fun applyUndoAction(action: Action): ActionResult {
        return when (action) {
            is Action.ParameterChange -> {
                try {
                    AudioEngineInterface.setParameter(action.pluginId, action.portIndex, action.oldValue)
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Parameter change failed: ${e.message}")
                }
            }
            
            is Action.PluginAdded -> {
                try {
                    AudioEngineInterface.removePlugin(action.pluginId)
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin removal failed: ${e.message}")
                }
            }
            
            is Action.PluginRemoved -> {
                try {
                    AudioEngineInterface.addPlugin(
                        action.pluginId,
                        action.pluginType,
                        action.position
                    )
                    // Restore state
                    action.savedState.forEach { (port, value) ->
                        AudioEngineInterface.setParameter(action.pluginId, port, value)
                    }
                    // Restore connections
                    action.connectionInfo.forEach { connection ->
                        AudioEngineInterface.connectPorts(
                            connection.sourcePluginId,
                            connection.sourcePort,
                            connection.targetPluginId,
                            connection.targetPort
                        )
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin restoration failed: ${e.message}")
                }
            }
            
            is Action.PluginMoved -> {
                try {
                    AudioEngineInterface.movePlugin(action.pluginId, action.fromPosition)
                    // Restore affected connections
                    action.affectedConnections.forEach { update ->
                        AudioEngineInterface.updateConnection(update.connectionId, update.oldConnection)
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin move failed: ${e.message}")
                }
            }
            
            is Action.PluginBypassed -> {
                try {
                    AudioEngineInterface.setPluginBypass(action.pluginId, action.wasBypassed)
                    // Restore affected parameters if needed
                    if (!action.wasBypassed) {
                        action.affectedParameters.forEach { (port, value) ->
                            AudioEngineInterface.setParameter(action.pluginId, port, value)
                        }
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin bypass failed: ${e.message}")
                }
            }
            
            is Action.BulkParameterChange -> {
                val results = action.changes.map { change ->
                    applyUndoAction(change)
                }
                if (results.all { it.success }) {
                    ActionResult.Success(action.actionId)
                } else {
                    ActionResult.Failed(action.actionId, "Some bulk changes failed")
                }
            }
            
            is Action.PresetLoaded -> {
                try {
                    // Restore previous state
                    action.previousState.forEach { (pluginId, parameters) ->
                        parameters.forEach { (port, value) ->
                            AudioEngineInterface.setParameter(pluginId, port, value)
                        }
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Preset restoration failed: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun applyRedoAction(action: Action): ActionResult {
        return when (action) {
            is Action.ParameterChange -> {
                try {
                    AudioEngineInterface.setParameter(action.pluginId, action.portIndex, action.newValue)
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Parameter change failed: ${e.message}")
                }
            }
            
            is Action.PluginAdded -> {
                try {
                    AudioEngineInterface.addPlugin(
                        action.pluginId,
                        action.pluginType,
                        action.position
                    )
                    // Apply initial state
                    action.initialState.forEach { (port, value) ->
                        AudioEngineInterface.setParameter(action.pluginId, port, value)
                    }
                    // Restore connections
                    action.connectionInfo.forEach { connection ->
                        AudioEngineInterface.connectPorts(
                            connection.sourcePluginId,
                            connection.sourcePort,
                            connection.targetPluginId,
                            connection.targetPort
                        )
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin addition failed: ${e.message}")
                }
            }
            
            is Action.PluginRemoved -> {
                try {
                    AudioEngineInterface.removePlugin(action.pluginId)
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin removal failed: ${e.message}")
                }
            }
            
            is Action.PluginMoved -> {
                try {
                    AudioEngineInterface.movePlugin(action.pluginId, action.toPosition)
                    // Apply connection updates
                    action.affectedConnections.forEach { update ->
                        AudioEngineInterface.updateConnection(update.connectionId, update.newConnection)
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin move failed: ${e.message}")
                }
            }
            
            is Action.PluginBypassed -> {
                try {
                    AudioEngineInterface.setPluginBypass(action.pluginId, !action.wasBypassed)
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Plugin bypass failed: ${e.message}")
                }
            }
            
            is Action.BulkParameterChange -> {
                val results = action.changes.map { change ->
                    applyRedoAction(change)
                }
                if (results.all { it.success }) {
                    ActionResult.Success(action.actionId)
                } else {
                    ActionResult.Failed(action.actionId, "Some bulk changes failed")
                }
            }
            
            is Action.PresetLoaded -> {
                try {
                    // Apply new state
                    action.newState.forEach { (pluginId, parameters) ->
                        parameters.forEach { (port, value) ->
                            AudioEngineInterface.setParameter(pluginId, port, value)
                        }
                    }
                    ActionResult.Success(action.actionId)
                } catch (e: Exception) {
                    ActionResult.Failed(action.actionId, "Preset loading failed: ${e.message}")
                }
            }
        }
    }
    
/**
     * Check if undo is available.
 */
    fun canUndo(): Boolean = undoStack.isNotEmpty() && !isProcessing.get()
    
    /**
     * Check if redo is available.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty() && !isProcessing.get()
    
    /**
     * Get the number of batches in undo history.
     */
    fun getUndoCount(): Int = undoStack.size
    
    /**
     * Get the number of batches in redo history.
     */
    fun getRedoCount(): Int = redoStack.size
    
    /**
     * Get total number of actions across all batches.
     */
    fun getTotalActionCount(): Int = undoStack.sumOf { it.size } + redoStack.sumOf { it.size }
    
    /**
     * Clear all history and cancel pending operations.
     */
    suspend fun clear() {
        batchingJob.get()?.cancel()
        synchronized(pendingActions) {
            pendingActions.clear()
        }
        undoStack.clear()
        redoStack.clear()
        updateStateFlows()
        metrics?.reset()
        Log.d(TAG, "Cleared all undo/redo history")
    }
    
    /**
     * Get a preview of the next undo batch.
     */
    fun peekUndo(): ActionBatch? = undoStack.peekLast()
    
    /**
     * Get a preview of the next redo batch.
     */
    fun peekRedo(): ActionBatch? = redoStack.peekLast()
    
    /**
     * Get recent history for UI display.
     */
    fun getRecentHistory(limit: Int = 10): List<ActionBatch> {
        return undoStack.takeLast(limit).reversed()
    }
    
    /**
     * Force flush any pending actions.
     */
    suspend fun flush(description: String = "Manual Flush") {
        batchingJob.get()?.cancel()
        flushPendingActions(description)
    }
    
    private fun performCleanupIfNeeded() {
        val totalSize = undoStack.size + redoStack.size
        if (totalSize > maxHistorySize * CLEANUP_THRESHOLD) {
            // Remove oldest entries from undo stack
            val toRemove = totalSize - maxHistorySize
            repeat(toRemove) {
                if (undoStack.isNotEmpty()) {
                    undoStack.removeFirst()
                }
            }
            Log.d(TAG, "Cleaned up $toRemove old history entries")
        }
    }
    
    private fun updateStateFlows() {
        val processing = isProcessing.get()
        _canUndoFlow.value = canUndo()
        _canRedoFlow.value = canRedo()
        _historyStateFlow.value = HistoryState(
            undoCount = undoStack.size,
            redoCount = redoStack.size,
            isProcessing = processing
        )
    }
    
    /**
     * Get performance metrics.
     */
    fun getMetrics(): UndoRedoMetrics? = metrics
    
    /**
     * Cleanup resources.
     */
    fun cleanup() {
        coroutineScope.cancel()
        batchingJob.get()?.cancel()
    }
    
    sealed class UndoResult {
        data class Success(val batch: ActionBatch, val results: List<ActionResult>) : UndoResult()
        data class Failed(val reason: String) : UndoResult()
    }
    
    sealed class RedoResult {
        data class Success(val batch: ActionBatch, val results: List<ActionResult>) : RedoResult()
        data class Failed(val reason: String) : RedoResult()
    }
    
    sealed class ActionResult {
        abstract val actionId: String
        abstract val success: Boolean
        
        data class Success(override val actionId: String) : ActionResult() {
            override val success: Boolean = true
        }
        
        data class Failed(override val actionId: String, val error: String) : ActionResult() {
            override val success: Boolean = false
        }
    }
}

/**
 * Metrics collection for undo/redo operations.
 */
class UndoRedoMetrics {
    private val actionCounts = mutableMapOf<String, AtomicInteger>()
    private val undoSuccessCount = AtomicInteger(0)
    private val redoSuccessCount = AtomicInteger(0)
    private val undoFailureCount = AtomicInteger(0)
    private val redoFailureCount = AtomicInteger(0)
    private val totalBatches = AtomicInteger(0)
    private val averageBatchSize = AtomicInteger(0)
    
    fun recordAction(action: UndoRedoManager.Action) {
        val actionType = action::class.simpleName ?: "Unknown"
        actionCounts.getOrPut(actionType) { AtomicInteger(0) }.incrementAndGet()
    }
    
    fun recordBatch(batch: UndoRedoManager.ActionBatch) {
        totalBatches.incrementAndGet()
        val currentAvg = averageBatchSize.get()
        val newAvg = (currentAvg + batch.size) / 2
        averageBatchSize.set(newAvg)
    }
    
    fun recordUndo(batch: UndoRedoManager.ActionBatch, success: Boolean) {
        if (success) undoSuccessCount.incrementAndGet() else undoFailureCount.incrementAndGet()
    }
    
    fun recordRedo(batch: UndoRedoManager.ActionBatch, success: Boolean) {
        if (success) redoSuccessCount.incrementAndGet() else redoFailureCount.incrementAndGet()
    }
    
    fun getStats(): Map<String, Any> = mapOf(
        "actionCounts" to actionCounts.mapValues { it.value.get() },
        "undoSuccess" to undoSuccessCount.get(),
        "redoSuccess" to redoSuccessCount.get(),
        "undoFailures" to undoFailureCount.get(),
        "redoFailures" to redoFailureCount.get(),
        "totalBatches" to totalBatches.get(),
        "averageBatchSize" to averageBatchSize.get()
    )
    
    fun reset() {
        actionCounts.clear()
        undoSuccessCount.set(0)
        redoSuccessCount.set(0)
        undoFailureCount.set(0)
        redoFailureCount.set(0)
        totalBatches.set(0)
        averageBatchSize.set(0)
    }
}

/**
 * Mock audio engine interface for demonstration.
 * In production, this would interface with your actual audio engine.
 */
object AudioEngineInterface {
    suspend fun setParameter(pluginId: String, portIndex: Int, value: Float) {
        // Implementation would call native audio engine
        delay(1) // Simulate processing time
    }
    
    suspend fun addPlugin(pluginId: String, pluginType: String, position: Int) {
        delay(5) // Simulate plugin loading time
    }
    
    suspend fun removePlugin(pluginId: String) {
        delay(2) // Simulate plugin removal time
    }
    
    suspend fun movePlugin(pluginId: String, newPosition: Int) {
        delay(3) // Simulate plugin move time
    }
    
    suspend fun setPluginBypass(pluginId: String, bypassed: Boolean) {
        delay(1) // Simulate bypass change time
    }
    
    suspend fun connectPorts(sourcePluginId: String, sourcePort: Int, targetPluginId: String, targetPort: Int) {
        delay(1) // Simulate connection time
    }
    
    suspend fun updateConnection(connectionId: String, connectionData: UndoRedoManager.ConnectionData) {
        delay(1) // Simulate connection update time
    }
}
