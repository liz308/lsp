# Port Metadata Model

This package contains Kotlin data classes for representing plugin port metadata. These classes are designed to be easily populated from JNI calls and used by the UI layer for dynamic control generation.

## Architecture

```
C++ Layer (lsp_android_bridge.h)
    ↓
JNI Bridge
    ↓
Kotlin Data Classes (PortMetadata.kt)
    ↓
UI Layer (Jetpack Compose)
```

## Core Classes

### PortDescriptor
Core port metadata matching the C++ `lsp_android_port_descriptor` struct. Contains:
- Port index, type, name
- Value range (min, max, default)
- Scaling information (linear/logarithmic)
- Unit and section for UI grouping

### PortMetadata
Extended metadata with inferred properties:
- Port direction (input/output)
- Port role (control/meter/bypass)
- Convenience methods for type checking

### PluginMetadata
Top-level container for all plugin port information:
- Plugin identification
- List of all ports
- Helper methods for filtering and grouping ports

## Usage Example

### From JNI (C++ → Kotlin)

```kotlin
// In JNI bridge code (pseudocode)
external fun getPluginMetadata(pluginHandle: Long): PluginMetadata

// Implementation in C++:
// 1. Call lsp_android_get_port_count()
// 2. Call lsp_android_get_port_descriptors()
// 3. For each descriptor, call PortMetadataParser.createPortDescriptor()
// 4. Call PortMetadataParser.createPluginMetadata()
```

### In Kotlin/UI Layer

```kotlin
// Get plugin metadata
val metadata = getPluginMetadata(pluginHandle)

// Filter control ports for UI generation
val controlPorts = metadata.getControlPorts()
controlPorts.forEach { port ->
    when {
        port.isLogScale -> createLogKnob(port)
        port.unit == "dB" -> createDecibelSlider(port)
        else -> createLinearSlider(port)
    }
}

// Get meter ports for visualization
val meterPorts = metadata.getMeterPorts()
meterPorts.forEach { port ->
    createMeter(port)
}

// Group ports by section for layout
val portsBySection = metadata.getPortsBySection()
portsBySection.forEach { (section, ports) ->
    createSection(section ?: "General", ports)
}
```

## Port Type Classification

### PortType (from C++)
- **CONTROL**: User-adjustable parameters (knobs, sliders, toggles)
- **AUDIO**: Audio signal ports (input/output channels)
- **CV**: Control Voltage ports (modulation sources)

### PortDirection (inferred)
- **INPUT**: Receives data (user controls, audio inputs)
- **OUTPUT**: Produces data (meters, audio outputs)
- **UNKNOWN**: Direction not determined

### PortRole (inferred)
- **CONTROL**: Standard parameter (knob, slider, toggle)
- **METER**: Read-only visualization (level meter, spectrum)
- **BYPASS**: Bypass/enable control
- **UNKNOWN**: Role not determined

## Inference Logic

The `PortMetadataParser` infers direction and role from:
1. Port type (CONTROL, AUDIO, CV)
2. Port name keywords ("meter", "level", "bypass", "input", "output")
3. Value characteristics (read-only patterns)

This allows the UI layer to automatically generate appropriate controls without manual configuration.

## Thread Safety

These data classes are immutable and thread-safe. They can be safely passed between:
- JNI thread (metadata parsing)
- UI thread (control rendering)
- Audio thread (parameter reading - via separate mechanism)

## Future Extensions

Planned additions for future tasks:
- Enum port support (discrete value lists)
- Port grouping metadata (for complex layouts)
- Custom UI hints (widget type overrides)
- Port dependencies (enable/disable relationships)
