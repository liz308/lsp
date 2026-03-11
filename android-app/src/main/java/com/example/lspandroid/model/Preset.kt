package com.example.lspandroid.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents a saved preset for a plugin.
 * Presets store parameter values and metadata for later recall.
 * 
 * Requirement 7: Preset Management System
 */
@Serializable
data class PresetData(
    val id: String = generatePresetId(),
    val pluginId: String,
    val pluginVersion: String,
    val presetName: String,
    val description: String = "",
    val category: String = "User",
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val parameters: Map<Int, Float> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val isReadOnly: Boolean = false,
    val author: String = "Unknown",
    val version: Int = 1,
    val checksum: String = ""
) {
    companion object {
        private fun generatePresetId(): String {
            return "preset_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }
    }

    /**
     * Validates that the preset is compatible with a given plugin version.
     * Supports semantic versioning compatibility checks.
     */
    fun isCompatibleWith(pluginVersion: String): Boolean {
        if (this.pluginVersion == pluginVersion) return true
        
        // Parse semantic versions (major.minor.patch)
        val thisVersion = parseVersion(this.pluginVersion)
        val targetVersion = parseVersion(pluginVersion)
        
        if (thisVersion == null || targetVersion == null) {
            return this.pluginVersion == pluginVersion
        }

        // Compatible if major version matches and target version is newer or equal
        return thisVersion.first == targetVersion.first && 
               (targetVersion.second > thisVersion.second || 
                (targetVersion.second == thisVersion.second && targetVersion.third >= thisVersion.third))
    }

    private fun parseVersion(version: String): Triple<Int, Int, Int>? {
        return try {
            val parts = version.split(".")
            if (parts.size >= 3) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Returns a formatted timestamp string.
     */
    fun getFormattedTimestamp(): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
/**
     * Returns a formatted last modified timestamp string.
 */
    fun getFormattedLastModified(): String {
        return Instant.ofEpochMilli(lastModified)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
    /**
     * Calculates and returns the checksum for this preset.
     */
    fun calculateChecksum(): String {
        val content = "$pluginId$pluginVersion$presetName${parameters.toSortedMap()}$version"
        return MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }
    /**
     * Validates the integrity of this preset using its checksum.
     */
    fun validateIntegrity(): Boolean {
        return checksum.isEmpty() || checksum == calculateChecksum()
    }
    /**
     * Creates a copy of this preset with updated parameters.
     */
    fun withUpdatedParameters(newParameters: Map<Int, Float>): PresetData {
        return copy(
            parameters = newParameters,
            lastModified = System.currentTimeMillis(),
            version = version + 1,
            checksum = ""
        ).let { it.copy(checksum = it.calculateChecksum()) }
    }

    /**
     * Creates a copy of this preset with a new name and ID.
     */
    fun duplicate(newName: String): PresetData {
        return copy(
            id = generatePresetId(),
            presetName = newName,
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            version = 1,
            isReadOnly = false,
            checksum = ""
        ).let { it.copy(checksum = it.calculateChecksum()) }
    }
}

/**
 * Exception thrown when preset operations fail.
 */
sealed class PresetException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ValidationError(message: String) : PresetException(message)
    class SerializationError(message: String, cause: Throwable) : PresetException(message, cause)
    class StorageError(message: String, cause: Throwable) : PresetException(message, cause)
    class NotFoundError(presetId: String) : PresetException("Preset not found: $presetId")
    class ReadOnlyError(presetId: String) : PresetException("Cannot modify read-only preset: $presetId")
    class CompatibilityError(message: String) : PresetException(message)
}

/**
 * Comprehensive preset manager for saving and loading plugin presets.
 * Handles JSON serialization, file storage, caching, and validation.
 */
class PresetManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PresetManager"
        private const val PREFS_NAME = "preset_manager_prefs"
        private const val PRESETS_DIR = "presets"
        private const val PRESET_FILE_EXTENSION = ".preset"
        private const val MAX_CACHE_SIZE = 100
        private const val BACKUP_SUFFIX = ".backup"
        
        @Volatile
        private var INSTANCE: PresetManager? = null
        
        fun getInstance(context: Context): PresetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PresetManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val presetsDir: File = File(context.filesDir, PRESETS_DIR)
    private val presetCache = ConcurrentHashMap<String, PresetData>()
    private val cacheMutex = Mutex()
    private val fileMutex = Mutex()
    
    init {
        if (!presetsDir.exists()) {
            presetsDir.mkdirs()
        }
        loadPresetIndex()
    }

    /**
     * Creates a preset from current plugin parameter values.
     */
    suspend fun createPreset(
        pluginId: String,
        pluginVersion: String,
        presetName: String,
        parameters: Map<Int, Float>,
        description: String = "",
        category: String = "User",
        tags: List<String> = emptyList(),
        author: String = "User"
    ): PresetData = withContext(Dispatchers.IO) {
        val preset = PresetData(
            pluginId = pluginId,
            pluginVersion = pluginVersion,
            presetName = presetName,
            description = description,
            category = category,
            tags = tags,
            parameters = parameters.toMap(),
            author = author,
            metadata = mapOf(
                "created_by" to "PresetManager",
                "android_version" to android.os.Build.VERSION.RELEASE,
                "app_version" to getAppVersion()
            )
        ).let { it.copy(checksum = it.calculateChecksum()) }

        if (!validatePreset(preset)) {
            throw PresetException.ValidationError("Invalid preset data")
        }

        savePresetToFile(preset)
        updatePresetIndex(preset)
        
        cacheMutex.withLock {
            presetCache[preset.id] = preset
            trimCache()
        }
        
        Log.d(TAG, "Created preset: ${preset.presetName} for plugin: $pluginId")
        preset
    }

    /**
     * Saves a preset to persistent storage.
     */
    suspend fun savePreset(preset: PresetData): PresetData = withContext(Dispatchers.IO) {
        if (preset.isReadOnly) {
            throw PresetException.ReadOnlyError(preset.id)
        }

        val updatedPreset = preset.copy(
            lastModified = System.currentTimeMillis(),
            version = preset.version + 1,
            checksum = ""
        ).let { it.copy(checksum = it.calculateChecksum()) }

        if (!validatePreset(updatedPreset)) {
            throw PresetException.ValidationError("Invalid preset data")
        }

        savePresetToFile(updatedPreset)
        updatePresetIndex(updatedPreset)
        
        cacheMutex.withLock {
            presetCache[updatedPreset.id] = updatedPreset
        }
        
        Log.d(TAG, "Saved preset: ${updatedPreset.presetName}")
        updatedPreset
    }

    /**
     * Loads a preset by ID.
     */
    suspend fun loadPreset(presetId: String): PresetData = withContext(Dispatchers.IO) {
        // Check cache first
        cacheMutex.withLock {
            presetCache[presetId]?.let { return@withContext it }
        }

        // Load from file
        val preset = loadPresetFromFile(presetId)
            ?: throw PresetException.NotFoundError(presetId)

        // Validate integrity
        if (!preset.validateIntegrity()) {
            Log.w(TAG, "Preset integrity check failed: $presetId")
        }

        // Cache the loaded preset
        cacheMutex.withLock {
            presetCache[presetId] = preset
            trimCache()
        }

        preset
    }

    /**
     * Loads all presets for a specific plugin.
     */
    suspend fun loadPresetsForPlugin(pluginId: String): List<PresetData> = withContext(Dispatchers.IO) {
        val presetIds = getPresetIdsForPlugin(pluginId)
        presetIds.mapNotNull { presetId ->
            try {
                loadPreset(presetId)
            } catch (e: PresetException) {
                Log.w(TAG, "Failed to load preset $presetId: ${e.message}")
                null
            }
        }.sortedBy { it.presetName }
    }

    /**
     * Deletes a preset.
     */
    suspend fun deletePreset(presetId: String): Boolean = withContext(Dispatchers.IO) {
        val preset = try {
            loadPreset(presetId)
        } catch (e: PresetException.NotFoundError) {
            return@withContext false
        }

        if (preset.isReadOnly) {
            throw PresetException.ReadOnlyError(presetId)
        }

        val file = File(presetsDir, "$presetId$PRESET_FILE_EXTENSION")
        val deleted = file.delete()
        
        if (deleted) {
            removeFromPresetIndex(presetId)
            cacheMutex.withLock {
                presetCache.remove(presetId)
            }
            Log.d(TAG, "Deleted preset: ${preset.presetName}")
        }
        
        deleted
    }

    /**
     * Duplicates a preset with a new name.
     */
    suspend fun duplicatePreset(presetId: String, newName: String): PresetData = withContext(Dispatchers.IO) {
        val original = loadPreset(presetId)
        val duplicate = original.duplicate(newName)
        
        savePresetToFile(duplicate)
        updatePresetIndex(duplicate)
        
        cacheMutex.withLock {
            presetCache[duplicate.id] = duplicate
            trimCache()
        }
        
        Log.d(TAG, "Duplicated preset: ${original.presetName} -> $newName")
        duplicate
    }

    /**
     * Exports a preset to JSON string.
     */
    suspend fun exportPreset(presetId: String): String = withContext(Dispatchers.IO) {
        val preset = loadPreset(presetId)
        try {
            json.encodeToString(preset)
        } catch (e: Exception) {
            throw PresetException.SerializationError("Failed to export preset", e)
        }
    }

    /**
     * Imports a preset from JSON string.
     */
    suspend fun importPreset(jsonString: String, overwriteExisting: Boolean = false): PresetData = withContext(Dispatchers.IO) {
        val preset = try {
            json.decodeFromString<PresetData>(jsonString)
        } catch (e: Exception) {
            throw PresetException.SerializationError("Failed to import preset", e)
        }

        if (!validatePreset(preset)) {
            throw PresetException.ValidationError("Invalid imported preset data")
        }

        // Check if preset already exists
        val existingPreset = try {
            loadPreset(preset.id)
        } catch (e: PresetException.NotFoundError) {
            null
        }

        val finalPreset = if (existingPreset != null && !overwriteExisting) {
            // Create a new preset with different ID
            preset.duplicate(preset.presetName + " (Imported)")
        } else {
            preset
        }

        savePresetToFile(finalPreset)
        updatePresetIndex(finalPreset)
        
        cacheMutex.withLock {
            presetCache[finalPreset.id] = finalPreset
            trimCache()
        }
        
        Log.d(TAG, "Imported preset: ${finalPreset.presetName}")
        finalPreset
    }

    /**
     * Searches presets by name, description, or tags.
     */
    suspend fun searchPresets(
        query: String,
        pluginId: String? = null,
        category: String? = null,
        tags: List<String> = emptyList()
    ): List<PresetData> = withContext(Dispatchers.IO) {
        val allPresets = if (pluginId != null) {
            loadPresetsForPlugin(pluginId)
        } else {
            getAllPresetIds().mapNotNull { presetId ->
                try {
                    loadPreset(presetId)
                } catch (e: PresetException) {
                    null
                }
            }
        }

        allPresets.filter { preset ->
            val matchesQuery = query.isBlank() || 
                preset.presetName.contains(query, ignoreCase = true) ||
                preset.description.contains(query, ignoreCase = true) ||
                preset.tags.any { it.contains(query, ignoreCase = true) }
            
            val matchesCategory = category == null || preset.category == category
            
            val matchesTags = tags.isEmpty() || tags.all { tag ->
                preset.tags.any { presetTag -> presetTag.equals(tag, ignoreCase = true) }
            }
            
            matchesQuery && matchesCategory && matchesTags
        }.sortedBy { it.presetName }
    }

    /**
     * Gets all available categories.
     */
    suspend fun getCategories(): List<String> = withContext(Dispatchers.IO) {
        getAllPresetIds().mapNotNull { presetId ->
            try {
                loadPreset(presetId).category
            } catch (e: PresetException) {
                null
            }
        }.distinct().sorted()
    }

    /**
     * Gets all available tags.
     */
    suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        getAllPresetIds().flatMap { presetId ->
            try {
                loadPreset(presetId).tags
            } catch (e: PresetException) {
                emptyList()
            }
        }.distinct().sorted()
    }

    /**
     * Creates a backup of all presets.
     */
    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val backupFile = File(context.cacheDir, "presets_backup_${System.currentTimeMillis()}.json")
        val allPresets = getAllPresetIds().mapNotNull { presetId ->
            try {
                loadPreset(presetId)
            } catch (e: PresetException) {
                null
            }
        }
        
        try {
            backupFile.writeText(json.encodeToString(allPresets))
            Log.d(TAG, "Created backup with ${allPresets.size} presets")
            backupFile
        } catch (e: Exception) {
            throw PresetException.StorageError("Failed to create backup", e)
        }
    }

    /**
     * Restores presets from a backup file.
     */
    suspend fun restoreFromBackup(backupFile: File, overwriteExisting: Boolean = false): Int = withContext(Dispatchers.IO) {
        val presets = try {
            val jsonContent = backupFile.readText()
            json.decodeFromString<List<PresetData>>(jsonContent)
        } catch (e: Exception) {
            throw PresetException.SerializationError("Failed to read backup file", e)
        }

        var restoredCount = 0
        for (preset in presets) {
            try {
                importPreset(json.encodeToString(preset), overwriteExisting)
                restoredCount++
            } catch (e: PresetException) {
                Log.w(TAG, "Failed to restore preset ${preset.presetName}: ${e.message}")
            }
        }
        
        Log.d(TAG, "Restored $restoredCount out of ${presets.size} presets")
        restoredCount
    }

    /**
     * Validates a preset for consistency and integrity.
     */
    fun validatePreset(preset: PresetData): Boolean {
        // Check that required fields are not empty
        if (preset.pluginId.isBlank() || preset.pluginVersion.isBlank() || 
            preset.presetName.isBlank()) {
                return false
            }

        // Check that timestamp is reasonable (not in the future, not too old)
        val now = System.currentTimeMillis()
        if (preset.timestamp > now || preset.timestamp < (now - 365L * 24 * 60 * 60 * 1000)) {
            return false
    }

        // Check that lastModified is not before timestamp
        if (preset.lastModified < preset.timestamp) {
            return false
}
        // Check that parameters are valid (no NaN or Infinity values)
        for ((index, value) in preset.parameters) {
            if (value.isNaN() || value.isInfinite() || index < 0) {
                return false
            }
        }

        // Check that version is positive
        if (preset.version < 1) {
            return false
        }

        // Validate checksum if present
        if (preset.checksum.isNotEmpty() && !preset.validateIntegrity()) {
            return false
        }

        return true
    }

    /**
     * Performs round-trip validation: save then load should produce equivalent preset.
     */
    fun validateRoundTrip(original: PresetData, loaded: PresetData): Boolean {
        // Check all critical fields match
        if (original.id != loaded.id ||
            original.pluginId != loaded.pluginId ||
            original.pluginVersion != loaded.pluginVersion ||
            original.presetName != loaded.presetName ||
            original.description != loaded.description ||
            original.category != loaded.category ||
            original.isReadOnly != loaded.isReadOnly ||
            original.author != loaded.author ||
            original.version != loaded.version) {
            return false
        }

        // Check tags match
        if (original.tags.size != loaded.tags.size || 
            !original.tags.containsAll(loaded.tags)) {
            return false
        }

        // Check metadata match
        if (original.metadata.size != loaded.metadata.size ||
            original.metadata != loaded.metadata) {
            return false
        }

        // Check parameters match (allowing for floating-point precision)
        if (original.parameters.size != loaded.parameters.size) {
            return false
        }

        for ((index, value) in original.parameters) {
            val loadedValue = loaded.parameters[index] ?: return false
            // Allow small floating-point differences
            if (kotlin.math.abs(value - loadedValue) > 1e-6f) {
                return false
            }
        }

        return true
    }

    // Private helper methods

    private suspend fun savePresetToFile(preset: PresetData) {
        fileMutex.withLock {
            val file = File(presetsDir, "${preset.id}$PRESET_FILE_EXTENSION")
            val backupFile = File(presetsDir, "${preset.id}$PRESET_FILE_EXTENSION$BACKUP_SUFFIX")
            
            try {
                // Create backup if file exists
                if (file.exists()) {
                    file.copyTo(backupFile, overwrite = true)
                }
                
                // Write new content
                file.writeText(json.encodeToString(preset))
                
                // Remove backup on success
                if (backupFile.exists()) {
                    backupFile.delete()
                }
            } catch (e: Exception) {
                // Restore from backup on failure
                if (backupFile.exists()) {
                    backupFile.copyTo(file, overwrite = true)
                    backupFile.delete()
                }
                throw PresetException.StorageError("Failed to save preset to file", e)
            }
        }
    }

    private suspend fun loadPresetFromFile(presetId: String): PresetData? {
        fileMutex.withLock {
            val file = File(presetsDir, "$presetId$PRESET_FILE_EXTENSION")
            if (!file.exists()) return null
            
            return try {
                val jsonContent = file.readText()
                json.decodeFromString<PresetData>(jsonContent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load preset from file: $presetId", e)
                null
            }
        }
    }

    private fun loadPresetIndex() {
        // Load preset index from SharedPreferences for quick lookups
        // This is a simple implementation - in production you might use a database
    }

    private fun updatePresetIndex(preset: PresetData) {
        val pluginPresets = getPresetIdsForPlugin(preset.pluginId).toMutableSet()
        pluginPresets.add(preset.id)
        prefs.edit()
            .putStringSet("plugin_${preset.pluginId}", pluginPresets)
            .apply()
    }

    private fun removeFromPresetIndex(presetId: String) {
        // Remove from all plugin indices
        val allKeys = prefs.all.keys.filter { it.startsWith("plugin_") }
        for (key in allKeys) {
            val presetIds = prefs.getStringSet(key, emptySet())?.toMutableSet()
            if (presetIds?.remove(presetId) == true) {
                prefs.edit().putStringSet(key, presetIds).apply()
            }
        }
    }

    private fun getPresetIdsForPlugin(pluginId: String): Set<String> {
        return prefs.getStringSet("plugin_$pluginId", emptySet()) ?: emptySet()
    }

    private fun getAllPresetIds(): Set<String> {
        return prefs.all.values
            .filterIsInstance<Set<String>>()
            .flatten()
            .toSet()
    }

    private fun trimCache() {
        if (presetCache.size > MAX_CACHE_SIZE) {
            // Remove oldest entries (simple LRU approximation)
            val toRemove = presetCache.size - MAX_CACHE_SIZE
            presetCache.keys.take(toRemove).forEach { key ->
                presetCache.remove(key)
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
