# Multi-Column Layout for Tablets

## Overview

The LSP Android app now supports responsive multi-column layouts for tablet devices, providing better space utilization on larger screens.

## Implementation

### Screen Size Detection

The `ScreenSizeUtils` utility class provides methods to detect tablet devices and calculate optimal column counts:

- **Phone (< 600dp)**: 1 column
- **Tablet (600-719dp)**: 2 columns  
- **Large Tablet (≥ 720dp)**: 3 columns

### Responsive Layout

The `LayoutGenerator` automatically adapts the plugin UI layout based on screen size:

1. **Phone Layout**: Single column with full-width controls
2. **Tablet Layout**: Multi-column grid for control parameters
3. **Adaptive Spacing**: Larger padding and spacing on tablets

### Key Features

- **Automatic Detection**: Uses Material 3's WindowSizeClass API
- **Responsive Columns**: Control parameters arranged in 2-3 columns on tablets
- **Full-Width Meters**: Visualization components (meters, graphs) remain full-width
- **Adaptive Spacing**: Padding and spacing scale with screen size
- **Override Support**: Custom layouts can specify column counts that override defaults

## Usage

The multi-column layout is applied automatically when the app detects a tablet device. No configuration is required.

### Custom Layout Overrides

Layout override JSON files can specify custom column counts:

```json
{
  "pluginId": "com.lsp-plugins.para-eq",
  "sections": [
    {
      "name": "Equalizer Bands",
      "columns": 3,
      "items": [...]
    }
  ]
}
```

On phones, the `columns` value is ignored and single-column layout is used. On tablets, the specified column count is respected.

## Technical Details

### ScreenSizeUtils API

```kotlin
// Detect tablet device (≥ 600dp)
val isTablet = ScreenSizeUtils.isTablet()

// Get optimal column count (1-3)
val columnCount = ScreenSizeUtils.getColumnCount()

// Get adaptive spacing
val spacing = ScreenSizeUtils.getColumnSpacing()
val padding = ScreenSizeUtils.getContentPadding()

// Use WindowSizeClass for granular control
val columns = ScreenSizeUtils.getColumnCountFromWindowSize(windowSizeClass)
```

### Layout Behavior

1. **Control Parameters**: Arranged in multi-column grid on tablets
2. **Meter Components**: Always full-width for better visualization
3. **Section Headers**: Full-width across all columns
4. **Incomplete Rows**: Filled with spacers to maintain alignment

## Testing

Run the unit tests to verify column count calculations:

```bash
./gradlew test --tests "ScreenSizeUtilsTest"
```

## Requirements

This feature implements **Requirement 16: Theme and Accessibility**:

> WHERE a tablet device is detected, THE UI_Generator SHALL use a multi-column layout

## Future Enhancements

- Landscape orientation optimization
- Foldable device support
- User-configurable column counts
- Per-plugin column preferences
