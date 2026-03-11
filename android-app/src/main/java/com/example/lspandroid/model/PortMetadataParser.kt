package com.example.lspandroid.model

import android.util.Log
import java.util.regex.Pattern

// Port role classification
enum class PortRole {
    BYPASS,
    METER,
    PRESET,
    MIDI_CONTROL,
    MODULATION,
    FILTER,
    DYNAMICS,
    EFFECT,
    CONTROL,
    UNKNOWN
}

// Helper object for PortType conversion
object PortTypeConverter {
    fun fromValue(value: Int): PortType {
        return when (value) {
            0 -> PortType.AUDIO
            1 -> PortType.CONTROL
            2 -> PortType.CONTROL // CV mapped to CONTROL for compatibility
            else -> PortType.AUDIO // Default to AUDIO if unknown
        }
    }
}

/**
 * Parameter type classification for UI optimization.
 */
enum class ParameterType {
    UNKNOWN,
    BOOLEAN,
    ENUMERATED,
    CONTINUOUS,
    FREQUENCY,
    GAIN,
    TIME,
    PERCENTAGE
}

// Port descriptor containing raw port data
data class PortDescriptor(
    val index: Int,
    val type: PortType,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val isLogScale: Boolean,
    val name: String,
    val unit: String?,
    val section: String?
)

/**
 * Extended PortMetadata with additional inference results.
 */
data class ExtendedPortMetadata(
    val descriptor: PortDescriptor,
    val direction: PortDirection,
    val role: PortRole,
    val parameterType: ParameterType = ParameterType.UNKNOWN,
    val grouping: String? = null,
    val isAutomatable: Boolean = true,
    val displayPriority: Int = 50
)

/**
 * Plugin analysis metadata with comprehensive analysis.
 */
data class PluginAnalysisMetadata(
    val pluginId: String,
    val pluginName: String,
    val ports: List<ExtendedPortMetadata>,
    val audioInputCount: Int = 0,
    val audioOutputCount: Int = 0,
    val controlPortCount: Int = 0,
    val category: String = "Unknown",
    val complexity: Int = 1,
    val hasPresets: Boolean = false,
    val hasBypass: Boolean = false,
    val hasMeters: Boolean = false
)

/**
 * Validation result with detailed feedback.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Batch validation result for multiple ports.
 */
data class BatchValidationResult(
    val portResults: List<ValidationResult>,
    val globalWarnings: List<String>,
    val isValid: Boolean
)

/**
 * Advanced parser for LV2/VST/AU plugin port metadata with comprehensive inference algorithms.
 * Handles complex port classification, parameter grouping, and metadata extraction from
 * various plugin formats used in professional audio production.
 */
object PortMetadataParser {

    private const val TAG = "PortMetadataParser"
    
    // Comprehensive pattern matching for port classification
    private val INPUT_PATTERNS = listOf(
        Pattern.compile("\\b(input|in)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(L|Left|R|Right)\\s*In", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(source|from)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    private val OUTPUT_PATTERNS = listOf(
        Pattern.compile("\\b(output|out)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(L|Left|R|Right)\\s*Out", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(destination|to|send)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    private val METER_PATTERNS = listOf(
        Pattern.compile("\\b(meter|level|vu|peak|rms)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(gain\\s*reduction|gr|envelope|spectrum)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(analyzer|scope|display|graph|curve)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(led|indicator|status)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    private val BYPASS_PATTERNS = listOf(
        Pattern.compile("\\b(bypass|enable|on|off|active)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(switch|toggle|mute)\\b", Pattern.CASE_INSENSITIVE)
    )

    private val FREQUENCY_PATTERNS = listOf(
        Pattern.compile("\\b(freq|frequency|hz|khz)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(cutoff|corner|center)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    private val GAIN_PATTERNS = listOf(
        Pattern.compile("\\b(gain|level|volume|amplitude)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(boost|cut|trim)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    private val TIME_PATTERNS = listOf(
        Pattern.compile("\\b(time|delay|attack|release|decay)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(ms|sec|seconds|milliseconds)\\b", Pattern.CASE_INSENSITIVE)
        )

    /**
     * Creates a PortDescriptor from raw JNI data with enhanced validation and normalization.
     */
    @JvmStatic
    fun createPortDescriptor(
        index: Int,
        typeValue: Int,
        minValue: Float,
        maxValue: Float,
        defaultValue: Float,
        isLogScale: Boolean,
        name: String,
        unit: String?,
        section: String?
    ): PortDescriptor {
        // Normalize and validate input parameters
        val normalizedName = name.trim().takeIf { it.isNotEmpty() } ?: "Port $index"
        val normalizedUnit = unit?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedSection = section?.trim()?.takeIf { it.isNotEmpty() }
        
        // Clamp default value to valid range
        val clampedDefault = when {
            defaultValue < minValue -> {
                Log.w(TAG, "Default value $defaultValue below minimum $minValue for port $normalizedName")
                minValue
    }
            defaultValue > maxValue -> {
                Log.w(TAG, "Default value $defaultValue above maximum $maxValue for port $normalizedName")
                maxValue
            }
            else -> defaultValue
        }

        return PortDescriptor(
            index = index,
            type = PortTypeConverter.fromValue(typeValue),
            minValue = minValue,
            maxValue = maxValue,
            defaultValue = clampedDefault,
            isLogScale = isLogScale,
            name = normalizedName,
            unit = normalizedUnit,
            section = normalizedSection
        )
    }

    /**
     * Creates comprehensive PortMetadata with advanced inference algorithms.
     */
    @JvmStatic
    fun createPortMetadata(descriptor: PortDescriptor): ExtendedPortMetadata {
        val direction = inferPortDirection(descriptor)
        val role = inferPortRole(descriptor)
        val parameterType = inferParameterType(descriptor)
        val grouping = inferParameterGrouping(descriptor)
        
        return ExtendedPortMetadata(
            descriptor = descriptor,
            direction = direction,
            role = role,
            parameterType = parameterType,
            grouping = grouping,
            isAutomatable = isPortAutomatable(descriptor),
            displayPriority = calculateDisplayPriority(descriptor, role)
        )
        }

    /**
     * Advanced port direction inference using multiple heuristics.
     */
    private fun inferPortDirection(descriptor: PortDescriptor): PortDirection {
        val name = descriptor.name
        
        // Audio ports: comprehensive pattern matching
        if (descriptor.type == PortType.AUDIO) {
            // Check explicit input patterns
            if (INPUT_PATTERNS.any { it.matcher(name).find() }) {
                return PortDirection.INPUT
        }

            // Check explicit output patterns
            if (OUTPUT_PATTERNS.any { it.matcher(name).find() }) {
                return PortDirection.OUTPUT
    }
            
            // Stereo channel inference
            if (name.matches(Regex(".*\\b(L|Left)\\b.*", RegexOption.IGNORE_CASE))) {
                return if (name.contains("Out", ignoreCase = true)) PortDirection.OUTPUT else PortDirection.INPUT
}
            if (name.matches(Regex(".*\\b(R|Right)\\b.*", RegexOption.IGNORE_CASE))) {
                return if (name.contains("Out", ignoreCase = true)) PortDirection.OUTPUT else PortDirection.INPUT
            }
            // Default audio port direction based on index (convention: inputs first)
            return if (descriptor.index < 2) PortDirection.INPUT else PortDirection.OUTPUT
        }

        // Control ports: sophisticated classification
        if (descriptor.type == PortType.CONTROL) {
            // Meter ports are always outputs
            if (isMeterPort(descriptor)) {
                return PortDirection.OUTPUT
            }
            
            // Status/indicator ports are outputs
            if (isStatusPort(descriptor)) {
                return PortDirection.OUTPUT
            }
            
            // Parameter ports are inputs
            return PortDirection.INPUT
        }

        // Default to INPUT for unknown cases
        return PortDirection.INPUT
    }

    /**
     * Comprehensive port role inference with plugin-specific logic.
     */
    private fun inferPortRole(descriptor: PortDescriptor): PortRole {
        if (descriptor.type != PortType.CONTROL) {
            return PortRole.UNKNOWN
        }

        val name = descriptor.name.lowercase()

        // Bypass/enable controls
        if (BYPASS_PATTERNS.any { it.matcher(name).find() }) {
            return PortRole.BYPASS
        }

        // Meter/visualization ports
        if (isMeterPort(descriptor)) {
            return PortRole.METER
        }

        // Preset/program selection
        if (name.contains("preset") || name.contains("program") || name.contains("patch")) {
            return PortRole.PRESET
        }

        // MIDI-related controls
        if (name.contains("midi") || name.contains("note") || name.contains("velocity")) {
            return PortRole.MIDI_CONTROL
        }

        // Modulation sources/destinations
        if (name.contains("lfo") || name.contains("envelope") || name.contains("mod")) {
            return PortRole.MODULATION
        }

        // Filter controls
        if (name.contains("filter") || name.contains("eq") || name.contains("resonance")) {
            return PortRole.FILTER
        }

        // Dynamics controls
        if (name.contains("compressor") || name.contains("limiter") || name.contains("gate")) {
            return PortRole.DYNAMICS
        }

        // Effect-specific controls
        if (name.contains("reverb") || name.contains("delay") || name.contains("chorus")) {
            return PortRole.EFFECT
        }

        return PortRole.CONTROL
    }

    /**
     * Infers the parameter type for better UI representation.
     */
    private fun inferParameterType(descriptor: PortDescriptor): ParameterType {
        if (descriptor.type != PortType.CONTROL) {
            return ParameterType.UNKNOWN
        }

        val name = descriptor.name.lowercase()
        val range = descriptor.maxValue - descriptor.minValue

        // Boolean/toggle parameters
        if (range <= 1.0f && descriptor.minValue >= 0.0f && descriptor.maxValue <= 1.0f) {
            return ParameterType.BOOLEAN
        }

        // Frequency parameters
        if (FREQUENCY_PATTERNS.any { it.matcher(name).find() }) {
            return ParameterType.FREQUENCY
        }

        // Gain/level parameters
        if (GAIN_PATTERNS.any { it.matcher(name).find() }) {
            return ParameterType.GAIN
        }

        // Time-based parameters
        if (TIME_PATTERNS.any { it.matcher(name).find() }) {
            return ParameterType.TIME
        }

        // Percentage parameters
        if (descriptor.unit?.contains("%") == true || name.contains("percent")) {
            return ParameterType.PERCENTAGE
        }

        // Enumerated parameters (small integer ranges)
        if (range <= 10.0f && range == range.toInt().toFloat()) {
            return ParameterType.ENUMERATED
        }

        return ParameterType.CONTINUOUS
    }

    /**
     * Groups related parameters for better UI organization.
     */
    private fun inferParameterGrouping(descriptor: PortDescriptor): String? {
        val section = descriptor.section
        if (!section.isNullOrEmpty()) {
            return section
        }

        val name = descriptor.name.lowercase()

        // Common parameter groupings
        return when {
            name.contains("eq") || name.contains("filter") -> "EQ/Filter"
            name.contains("compressor") || name.contains("limiter") || name.contains("gate") -> "Dynamics"
            name.contains("reverb") || name.contains("delay") || name.contains("chorus") -> "Effects"
            name.contains("lfo") || name.contains("envelope") -> "Modulation"
            name.contains("input") || name.contains("output") -> "I/O"
            name.contains("midi") -> "MIDI"
            isMeterPort(descriptor) -> "Meters"
            else -> null
        }
    }

    /**
     * Enhanced meter port detection with comprehensive patterns.
     */
    private fun isMeterPort(descriptor: PortDescriptor): Boolean {
        val name = descriptor.name.lowercase()
        
        // Pattern-based detection
        if (METER_PATTERNS.any { it.matcher(name).find() }) {
            return true
        }
        
        // Range-based heuristics for meter ports
        val range = descriptor.maxValue - descriptor.minValue
        if (range > 100.0f && descriptor.minValue < 0.0f) {
            // Likely a dB meter
            return name.contains("db") || name.contains("level")
        }
        
        // Unit-based detection
        descriptor.unit?.let { unit ->
            if (unit.contains("dB", ignoreCase = true) || 
                unit.contains("VU", ignoreCase = true) ||
                unit.contains("%", ignoreCase = true)) {
                return name.contains("meter") || name.contains("level")
            }
        }
        
        return false
    }

    /**
     * Detects status/indicator ports.
     */
    private fun isStatusPort(descriptor: PortDescriptor): Boolean {
        val name = descriptor.name.lowercase()
        return name.contains("status") || 
               name.contains("indicator") || 
               name.contains("led") ||
               (name.contains("active") && descriptor.maxValue <= 1.0f)
    }

    /**
     * Determines if a port should be automatable.
     */
    private fun isPortAutomatable(descriptor: PortDescriptor): Boolean {
        if (descriptor.type != PortType.CONTROL) {
            return false
        }
        
        // Meters and status ports are not automatable
        if (isMeterPort(descriptor) || isStatusPort(descriptor)) {
            return false
        }
        
        // Preset selectors typically aren't automated
        val name = descriptor.name.lowercase()
        if (name.contains("preset") || name.contains("program")) {
            return false
        }
        
        return true
    }

    /**
     * Calculates display priority for UI ordering.
     */
    private fun calculateDisplayPriority(descriptor: PortDescriptor, role: PortRole): Int {
        return when (role) {
            PortRole.BYPASS -> 100
            PortRole.PRESET -> 90
            PortRole.CONTROL -> when {
                descriptor.name.lowercase().contains("gain") -> 80
                descriptor.name.lowercase().contains("freq") -> 70
                else -> 50
            }
            PortRole.METER -> 10
            else -> 30
        }
    }

    /**
     * Creates comprehensive PluginMetadata with advanced analysis.
     */
    @JvmStatic
    fun createPluginMetadata(
        pluginId: String,
        pluginName: String,
        descriptors: List<PortDescriptor>
    ): PluginAnalysisMetadata {
        val ports = descriptors.map { createPortMetadata(it) }
        
        // Analyze plugin characteristics
        val audioInputs = ports.count { it.descriptor.type == PortType.AUDIO && it.direction == PortDirection.INPUT }
        val audioOutputs = ports.count { it.descriptor.type == PortType.AUDIO && it.direction == PortDirection.OUTPUT }
        val controlPorts = ports.count { it.descriptor.type == PortType.CONTROL }
        
        val pluginCategory = inferPluginCategory(pluginName, ports)
        val complexity = calculateComplexity(ports)
        
        return PluginAnalysisMetadata(
            pluginId = pluginId,
            pluginName = pluginName,
            ports = ports,
            audioInputCount = audioInputs,
            audioOutputCount = audioOutputs,
            controlPortCount = controlPorts,
            category = pluginCategory,
            complexity = complexity,
            hasPresets = ports.any { it.role == PortRole.PRESET },
            hasBypass = ports.any { it.role == PortRole.BYPASS },
            hasMeters = ports.any { it.role == PortRole.METER }
        )
    }

    /**
     * Infers plugin category from name and port analysis.
     */
    private fun inferPluginCategory(pluginName: String, ports: List<ExtendedPortMetadata>): String {
        val name = pluginName.lowercase()
        
        // Direct name matching
        when {
            name.contains("eq") || name.contains("equalizer") -> return "EQ"
            name.contains("compressor") || name.contains("limiter") -> return "Dynamics"
            name.contains("reverb") -> return "Reverb"
            name.contains("delay") || name.contains("echo") -> return "Delay"
            name.contains("distortion") || name.contains("overdrive") -> return "Distortion"
            name.contains("chorus") || name.contains("flanger") || name.contains("phaser") -> return "Modulation"
            name.contains("synth") || name.contains("oscillator") -> return "Synthesizer"
            name.contains("filter") -> return "Filter"
            name.contains("analyzer") || name.contains("spectrum") -> return "Analysis"
        }
        
        // Port-based inference
        val filterPorts = ports.count { it.role == PortRole.FILTER }
        val dynamicsPorts = ports.count { it.role == PortRole.DYNAMICS }
        val effectPorts = ports.count { it.role == PortRole.EFFECT }
        
        return when {
            filterPorts > dynamicsPorts && filterPorts > effectPorts -> "Filter"
            dynamicsPorts > filterPorts && dynamicsPorts > effectPorts -> "Dynamics"
            effectPorts > 0 -> "Effect"
            else -> "Utility"
        }
    }

    /**
     * Calculates plugin complexity score for UI adaptation.
     */
    private fun calculateComplexity(ports: List<ExtendedPortMetadata>): Int {
        val controlPorts = ports.count { it.descriptor.type == PortType.CONTROL }
        val groups = ports.mapNotNull { it.grouping }.distinct().size
        val automatablePorts = ports.count { it.isAutomatable }
        
        return when {
            controlPorts <= 5 -> 1  // Simple
            controlPorts <= 15 -> 2 // Medium
            controlPorts <= 30 -> 3 // Complex
            else -> 4 // Very Complex
        }
    }

    /**
     * Comprehensive port descriptor validation with detailed error reporting.
     */
    @JvmStatic
    fun validatePortDescriptor(descriptor: PortDescriptor): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Range validation
        if (descriptor.minValue > descriptor.defaultValue) {
            errors.add("Default value ${descriptor.defaultValue} is below minimum ${descriptor.minValue}")
        }
        
        if (descriptor.defaultValue > descriptor.maxValue) {
            errors.add("Default value ${descriptor.defaultValue} is above maximum ${descriptor.maxValue}")
        }
        
        if (descriptor.minValue >= descriptor.maxValue) {
            errors.add("Minimum value ${descriptor.minValue} must be less than maximum ${descriptor.maxValue}")
        }

        // Name validation
        if (descriptor.name.isBlank()) {
            errors.add("Port name cannot be empty")
        } else if (descriptor.name.length > 64) {
            warnings.add("Port name is unusually long (${descriptor.name.length} characters)")
        }

        // Type-specific validation
        when (descriptor.type) {
            PortType.CONTROL -> {
                if (descriptor.isLogScale && descriptor.minValue <= 0) {
                    errors.add("Logarithmic scale requires positive minimum value")
                }
            }
            PortType.AUDIO -> {
                if (descriptor.minValue != -1.0f || descriptor.maxValue != 1.0f) {
                    warnings.add("Audio port has unusual range: [${descriptor.minValue}, ${descriptor.maxValue}]")
                }
            }
            PortType.CV -> {
                // CV ports typically have wider ranges
                if (descriptor.maxValue - descriptor.minValue < 1.0f) {
                    warnings.add("CV port has narrow range: [${descriptor.minValue}, ${descriptor.maxValue}]")
                }
            }
        }

        // Unit validation
        descriptor.unit?.let { unit ->
            if (unit.length > 16) {
                warnings.add("Unit string is unusually long: '$unit'")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Batch validation for multiple port descriptors.
     */
    @JvmStatic
    fun validatePortDescriptors(descriptors: List<PortDescriptor>): BatchValidationResult {
        val results = descriptors.map { validatePortDescriptor(it) }
        val globalWarnings = mutableListOf<String>()
        
        // Check for duplicate names
        val nameGroups = descriptors.groupBy { it.name }
        nameGroups.forEach { (name, ports) ->
            if (ports.size > 1) {
                globalWarnings.add("Duplicate port name '$name' found ${ports.size} times")
            }
        }
        
        // Check for index gaps
        val indices = descriptors.map { it.index }.sorted()
        for (i in 1 until indices.size) {
            if (indices[i] != indices[i-1] + 1) {
                globalWarnings.add("Gap in port indices: ${indices[i-1]} -> ${indices[i]}")
            }
        }
        
        return BatchValidationResult(
            portResults = results,
            globalWarnings = globalWarnings,
            isValid = results.all { it.isValid }
        )
    }
}
