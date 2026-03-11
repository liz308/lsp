package com.example.lspandroid.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Extension properties for creating sp units
val Int.spUnit: TextUnit get() = TextUnit(this.toFloat(), TextUnitType.Sp)
val Float.spUnit: TextUnit get() = TextUnit(this, TextUnitType.Sp)

// Extension for creating dp units
val Int.dpUnit: Dp get() = this.dp
val Float.dpUnit: Dp get() = this.dp

// Extension for corner radius
val Int.cornerRadius: Dp get() = this.dp
val Float.cornerRadius: Dp get() = this.dp

// Extension for padding and margin
val Int.padding: Dp get() = this.dp
val Float.padding: Dp get() = this.dp
val Int.margin: Dp get() = this.dp
val Float.margin: Dp get() = this.dp

// Extension for elevation
val Int.elevation: Dp get() = this.dp
val Float.elevation: Dp get() = this.dp

// Extension for alpha
val Float.alpha: Float get() = this.coerceIn(0f, 1f)
val Double.alpha: Float get() = this.toFloat().coerceIn(0f, 1f)

// Extension for rotation
val Float.rotation: Float get() = this
val Int.rotation: Float get() = this.toFloat()

// Extension for scale
val Float.scale: Float get() = this
val Int.scale: Float get() = this.toFloat()

// Extension for translation
val Float.translation: Float get() = this
val Int.translation: Float get() = this.toFloat()

// Extension for animation duration
val Int.animationDuration: Int get() = this
val Long.animationDuration: Long get() = this

// Extension for haptic feedback
val Boolean.hapticFeedback: Boolean get() = this

// Extension for accessibility
val String.contentDescription: String get() = this
val String.role: String get() = this
val String.hint: String get() = this
val Boolean.isImportantForAccessibility: Boolean get() = this
val String.traversalOrder: String get() = this

// Extension for validation
val Boolean.required: Boolean get() = this
val Float.minValue: Float get() = this
val Float.maxValue: Float get() = this
val String.pattern: String get() = this

// Extension for formatting
val Int.decimalPlaces: Int get() = this
val String.unit: String get() = this
val String.prefix: String get() = this
val String.suffix: String get() = this
val Float.multiplier: Float get() = this
val Boolean.logarithmic: Boolean get() = this

// Extension for binding
val Boolean.bidirectional: Boolean get() = this
val String.updateMode: String get() = this
val Int.throttleMs: Int get() = this

// Extension for tooltip and help text
val String.tooltip: String get() = this
val String.helpText: String get() = this
val String.icon: String get() = this

// Extension for show/hide values
val Boolean.showValue: Boolean get() = this
val Boolean.showLabel: Boolean get() = this

// Extension for gestures
val Boolean.gestureEnabled: Boolean get() = this