package com.example.lspandroid.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.*
/**
 * Professional LSP Audio Plugin UI Components Library
 * 
 * This library provides a comprehensive set of touch-optimized audio control components
 * designed for professional audio applications on Android. Each component implements
 * industry-standard behaviors with enhanced mobile interaction patterns.
 * 
 * Features:
 * - Precision control with multi-touch gestures
 * - Haptic feedback for tactile response
 * - Logarithmic and linear scaling support
 * - Professional audio parameter ranges
 * - Accessibility compliance
 * - Material Design 3 theming
 * - Real-time visual feedback
 * - Parameter automation support
 */
/**
 * Professional rotary knob control with advanced interaction patterns.
 * 
 * This component provides industry-standard knob behavior with:
 * - Circular drag gesture recognition
 * - Precision mode with modifier keys
 * - Double-tap reset functionality
 * - Long-press for fine adjustment
 * - Visual parameter indication
 * - Haptic feedback integration
 * - Logarithmic scaling support
 * - Parameter value display with units
 * 
 * @param value Current parameter value (0.0 to 1.0 normalized)
 * @param onValueChange Callback invoked when value changes
 * @param label Display label for the parameter
 * @param minValue Minimum parameter value for display scaling
 * @param maxValue Maximum parameter value for display scaling
 * @param defaultValue Default value for reset operations (0.0 to 1.0)
 * @param isLogScale Enable logarithmic scaling for frequency/gain parameters
 * @param precision Base sensitivity for drag operations
 * @param onShowKeypad Optional callback for external numeric input
 * @param unit Unit suffix for value display (Hz, dB, %, etc.)
 * @param steps Optional discrete step count for quantized parameters
 * @param bipolar Enable bipolar display (-/+ around center)
 * @param modifier Compose modifier for layout customization
 */
@Composable
fun LspKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String = "",
    minValue: Float = 0f,
    maxValue: Float = 100f,
    defaultValue: Float = 0f,
    isLogScale: Boolean = false,
    precision: Float = 0.005f,
    onShowKeypad: ((Float, (Float) -> Unit) -> Unit)? = null,
    unit: String = "",
    steps: Int? = null,
    bipolar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    var isDragging by remember { mutableStateOf(false) }
    var isPrecisionMode by remember { mutableStateOf(false) }
    var dragStartAngle by remember { mutableStateOf(0f) }
    var dragStartValue by remember { mutableStateOf(value) }
    var showKeypad by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastHapticValue by remember { mutableStateOf(0f) }
    var centerPoint by remember { mutableStateOf(Offset.Zero) }

    val precisionMultiplier = if (isPrecisionMode) 0.1f else 1f
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "knob_rotation"
    )

    // Calculate display value with proper scaling
    val displayValue = remember(value, minValue, maxValue, isLogScale) {
        if (isLogScale && minValue > 0f) {
            val minLog = ln(minValue)
            val maxLog = ln(maxValue)
            val valueLog = minLog + value * (maxLog - minLog)
            exp(valueLog)
        } else {
            minValue + value * (maxValue - minValue)
        }
    }

    // Format display value based on range and precision
    val formattedValue = remember(displayValue, isPrecisionMode, unit) {
        val decimals = when {
            isPrecisionMode -> 3
            abs(displayValue) >= 1000f -> 0
            abs(displayValue) >= 100f -> 1
            abs(displayValue) >= 10f -> 2
            else -> 3
        }
        String.format("%.${decimals}f", displayValue)
    }

    // Calculate knob rotation angle (270 degrees total range)
    val rotationAngle = remember(animatedValue, bipolar) {
        if (bipolar) {
            -135f + (animatedValue - 0.5f) * 270f
        } else {
            -135f + animatedValue * 270f
        }
    }

    Column(
        modifier = modifier
            .width(88.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 300) {
                            // Double-tap reset
                            onValueChange(defaultValue)
                            performResetFeedback(context)
                        } else {
                            // Single tap on value area shows keypad
                            val knobCenter = centerPoint
                            val distance = sqrt(
                                (offset.x - knobCenter.x).pow(2) + 
                                (offset.y - knobCenter.y).pow(2)
                            )
                            if (distance < with(density) { 25.dp.toPx() }) {
                                if (onShowKeypad != null) {
                                    onShowKeypad(displayValue) { newValue ->
                                        val normalizedValue = if (isLogScale && minValue > 0f) {
                                            val minLog = ln(minValue)
                                            val maxLog = ln(maxValue)
                                            val newLog = ln(newValue.coerceAtLeast(minValue))
                                            ((newLog - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
                                        } else {
                                            ((newValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                                        }
                                        onValueChange(normalizedValue)
                                    }
                                } else {
                                    showKeypad = true
                                }
                            }
                        }
                        lastTapTime = currentTime
                    },
                    onLongPress = {
                        isPrecisionMode = true
                        performPrecisionModeFeedback(context)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        centerPoint = Offset(size.width / 2f, size.height / 2f)
                        val deltaX = offset.x - centerPoint.x
                        val deltaY = offset.y - centerPoint.y
                        dragStartAngle = atan2(deltaY, deltaX)
                        dragStartValue = value
                    },
                    onDrag = { change, _ ->
                        val deltaX = change.position.x - centerPoint.x
                        val deltaY = change.position.y - centerPoint.y
                        val currentAngle = atan2(deltaY, deltaX)
                        
                        var angleDelta = currentAngle - dragStartAngle
                        
                        // Handle angle wraparound
                        if (angleDelta > PI.toFloat()) angleDelta -= (2 * PI).toFloat()
                        if (angleDelta < -PI.toFloat()) angleDelta += (2 * PI).toFloat()
                        
                        val sensitivity = precision * precisionMultiplier * 2f
                        val valueDelta = angleDelta * sensitivity
                        
                        val newValue = if (steps != null) {
                            val stepSize = 1f / (steps - 1)
                            val steppedValue = (dragStartValue + valueDelta).coerceIn(0f, 1f)
                            (round(steppedValue / stepSize) * stepSize).coerceIn(0f, 1f)
                        } else {
                            (dragStartValue + valueDelta).coerceIn(0f, 1f)
                        }
                        
                        onValueChange(newValue)

                        // Haptic feedback on significant value changes
                        if (abs(newValue - lastHapticValue) > 0.05f) {
                            performControlAdjustFeedback(context)
                            lastHapticValue = newValue
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        isPrecisionMode = false
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main knob assembly
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when {
                                isPrecisionMode -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                                isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            when {
                                isPrecisionMode -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            }
                        ),
                        radius = with(density) { 36.dp.toPx() }
                    ),
                    shape = RoundedCornerShape(50)
                )
                .then(
                    if (isPrecisionMode) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(50)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // Pre-capture colors for DrawScope
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val primaryColor = MaterialTheme.colorScheme.primary
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface

            // Knob track and indicator
            Canvas(
                modifier = Modifier.size(72.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 8.dp.toPx()
                
                // Draw track arc
                drawArc(
                    color = Color.Gray.copy(alpha = 0.3f),
                    startAngle = -225f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    ),
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
                
                // Draw value arc
                val sweepAngle = if (bipolar) {
                    val centerAngle = -90f
                    val valueAngle = -135f + animatedValue * 270f
                    if (valueAngle > centerAngle) {
                        valueAngle - centerAngle
                    } else {
                        centerAngle - valueAngle
                    }
                } else {
                    animatedValue * 270f
                }
                
                val startAngle = if (bipolar && (-135f + animatedValue * 270f) < -90f) {
                    -135f + animatedValue * 270f
                } else if (bipolar) {
                    -90f
                } else {
                    -225f
                }
                
                drawArc(
                    color = when {
                        isPrecisionMode -> tertiaryColor
                        else -> primaryColor
                    },
                    startAngle = startAngle,
                    sweepAngle = abs(sweepAngle),
                    useCenter = false,
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    ),
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
                
                // Draw pointer indicator
                val pointerAngle = Math.toRadians(rotationAngle.toDouble())
                val pointerRadius = radius * 0.7f
                val pointerEnd = Offset(
                    center.x + cos(pointerAngle).toFloat() * pointerRadius,
                    center.y + sin(pointerAngle).toFloat() * pointerRadius
                )
                
                drawLine(
                    color = onSurfaceColor,
                    start = center,
                    end = pointerEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Center dot
                drawCircle(
                    color = onSurfaceColor,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
            
            // Value display overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = formattedValue,
                    fontSize = if (formattedValue.length > 6) 10.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (unit.isNotEmpty() && !isPrecisionMode) {
                    Text(
                        text = unit,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Parameter label
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }

        // Status indicators
        if (isPrecisionMode) {
            Text(
                text = "PRECISION",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        
        if (steps != null) {
            Text(
                text = "STEPPED",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }

    // Numeric keypad dialog
    if (showKeypad) {
        NumericKeypadDialog(
            currentValue = displayValue,
            minValue = minValue,
            maxValue = maxValue,
            unit = unit,
            isLogScale = isLogScale,
            onValueConfirmed = { newValue ->
                val normalizedValue = if (isLogScale && minValue > 0f) {
                    val minLog = ln(minValue)
                    val maxLog = ln(maxValue)
                    val newLog = ln(newValue.coerceAtLeast(minValue))
                    ((newLog - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
                } else {
                    ((newValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                }
                onValueChange(normalizedValue)
                showKeypad = false
            },
            onDismiss = { showKeypad = false }
        )
    }
}

/**
 * Professional horizontal slider with advanced interaction patterns.
 * 
 * Features:
 * - Horizontal drag gesture recognition
 * - Precision mode activation
 * - Double-tap reset functionality
 * - Visual feedback and animation
 * - Logarithmic scaling support
 * - Step quantization
 * - Bipolar display mode
 * 
 * @param value Current parameter value (0.0 to 1.0 normalized)
 * @param onValueChange Callback invoked when value changes
 * @param label Display label for the parameter
 * @param minValue Minimum parameter value for display scaling
 * @param maxValue Maximum parameter value for display scaling
 * @param defaultValue Default value for reset operations (0.0 to 1.0)
 * @param isLogScale Enable logarithmic scaling
 * @param precision Base sensitivity for drag operations
 * @param unit Unit suffix for value display
 * @param steps Optional discrete step count
 * @param bipolar Enable bipolar display mode
 * @param showValue Display current value on slider
 * @param modifier Compose modifier for layout customization
 */
@Composable
fun LspSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String = "",
    minValue: Float = 0f,
    maxValue: Float = 100f,
    defaultValue: Float = 0f,
    isLogScale: Boolean = false,
    precision: Float = 0.01f,
    unit: String = "",
    steps: Int? = null,
    bipolar: Boolean = false,
    showValue: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDragging by remember { mutableStateOf(false) }
    var isPrecisionMode by remember { mutableStateOf(false) }
    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartValue by remember { mutableStateOf(value) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastHapticValue by remember { mutableStateOf(0f) }

    val precisionMultiplier = if (isPrecisionMode) 0.1f else 1f
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "slider_value"
    )

    // Calculate display value
    val displayValue = remember(value, minValue, maxValue, isLogScale) {
        if (isLogScale && minValue > 0f) {
            val minLog = ln(minValue)
            val maxLog = ln(maxValue)
            val valueLog = minLog + value * (maxLog - minLog)
            exp(valueLog)
        } else {
            minValue + value * (maxValue - minValue)
        }
    }

    val formattedValue = remember(displayValue, isPrecisionMode) {
        val decimals = if (isPrecisionMode) 3 else when {
            abs(displayValue) >= 100f -> 1
            abs(displayValue) >= 10f -> 2
            else -> 3
        }
        String.format("%.${decimals}f", displayValue)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with label and value
        if (label.isNotEmpty() || showValue) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (showValue) {
                    Text(
                        text = "$formattedValue $unit".trim(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isPrecisionMode) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Slider track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < 300) {
                                // Double-tap reset
                                onValueChange(defaultValue)
                                performResetFeedback(context)
                            } else {
                                // Single tap to set value
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                val finalValue = if (steps != null) {
                                    val stepSize = 1f / (steps - 1)
                                    (round(newValue / stepSize) * stepSize).coerceIn(0f, 1f)
                                } else {
                                    newValue
                                }
                                onValueChange(finalValue)
                                performControlAdjustFeedback(context)
                            }
                            lastTapTime = currentTime
                        },
                        onLongPress = {
                            isPrecisionMode = true
                            performPrecisionModeFeedback(context)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragStartX = offset.x
                            dragStartValue = value
                        },
                        onDrag = { change, _ ->
                            val delta = change.position.x - dragStartX
                            val sensitivity = precision * precisionMultiplier / size.width
                            val newValue = (dragStartValue + delta * sensitivity).coerceIn(0f, 1f)
                            
                            val finalValue = if (steps != null) {
                                val stepSize = 1f / (steps - 1)
                                (round(newValue / stepSize) * stepSize).coerceIn(0f, 1f)
                            } else {
                                newValue
                            }
                            
                            onValueChange(finalValue)

                            // Haptic feedback
                            if (abs(finalValue - lastHapticValue) > 0.05f) {
                                performControlAdjustFeedback(context)
                                lastHapticValue = finalValue
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            isPrecisionMode = false
                        }
                    )
                }
        ) {
            // Progress fill
            val fillFraction = if (bipolar) {
                if (animatedValue > 0.5f) {
                    animatedValue - 0.5f
                } else {
                    0.5f - animatedValue
                }
            } else {
                animatedValue
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFraction.coerceIn(0.001f, 1f))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                when {
                                    isPrecisionMode -> MaterialTheme.colorScheme.tertiary
                                    isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                when {
                                    isPrecisionMode -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                    isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                }
                            )
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            )

            // Center line for bipolar mode
            if (bipolar) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.Center)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                )
            }

            // Thumb indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(
                        x = (animatedValue * 200).dp
                    )
                    .align(Alignment.CenterStart)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50)
                    )
                    .border(
                        width = if (isDragging || isPrecisionMode) 3.dp else 2.dp,
                        color = when {
                            isPrecisionMode -> MaterialTheme.colorScheme.tertiary
                            isDragging -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        },
                        shape = RoundedCornerShape(50)
                    )
            )

            // Step indicators
            if (steps != null && steps > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(steps) { index ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = if (index <= (animatedValue * (steps - 1))) 0.8f else 0.3f
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        }

        // Status indicators
        if (isPrecisionMode) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PRECISION MODE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * Professional toggle switch with enhanced visual feedback.
 * 
 * @param value Current toggle state
 * @param onValueChange Callback when toggled
 * @param label Control label
 * @param enabled Whether the toggle is interactive
 * @param modifier Compose modifier for layout customization
 */
@Composable
fun LspToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    label: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatedOffset by animateFloatAsState(
        targetValue = if (value) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toggle_position"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.6f
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onValueChange(!value)
                performControlAdjustFeedback(context)
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (enabled) 1f else 0.6f
            )
        )

        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 28.dp)
                .background(
                    color = if (value) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = (animatedOffset * 24).dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

/**
 * Professional level meter with peak hold and multiple display modes.
 * 
 * @param value Current level (0.0 to 1.0 normalized)
 * @param peakValue Peak level held
 * @param label Meter label
 * @param unit Unit string (dB, %, etc.)
 * @param orientation Horizontal or vertical orientation
 * @param showPeak Display peak hold indicator
 * @param showValue Display numeric value
 * @param segments Number of LED-style segments
 * @param modifier Compose modifier for layout customization
 */
@Composable
fun LspMeter(
    value: Float,
    peakValue: Float = value,
    label: String = "",
    unit: String = "",
    orientation: MeterOrientation = MeterOrientation.Horizontal,
    showPeak: Boolean = true,
    showValue: Boolean = true,
    segments: Int = 20,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 50),
        label = "meter_level"
    )
    
    val animatedPeak by animateFloatAsState(
        targetValue = peakValue,
        animationSpec = tween(durationMillis = 100),
        label = "meter_peak"
    )

    when (orientation) {
        MeterOrientation.Horizontal -> {
            Column(modifier = modifier.fillMaxWidth()) {
                // Header
                if (label.isNotEmpty() || showValue) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (showValue) {
                            Text(
                                text = String.format("%.1f %s", animatedValue * 100, unit),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Meter bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    // Segmented display
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        repeat(segments) { index ->
                            val segmentValue = (index + 1).toFloat() / segments
                            val isActive = animatedValue >= segmentValue
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (isActive) {
                                            when {
                                                segmentValue > 0.9f -> Color(0xFFFF4444) // Red
                                                segmentValue > 0.7f -> Color(0xFFFFAA00) // Orange
                                                segmentValue > 0.5f -> Color(0xFFFFDD00) // Yellow
                                                else -> Color(0xFF44FF44) // Green
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        },
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }

                    // Peak indicator
                    if (showPeak && animatedPeak > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedPeak)
                                .padding(end = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight(0.8f)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        MeterOrientation.Vertical -> {
            Row(
                modifier = modifier.height(120.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Meter bar
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    // Segmented display
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.Bottom)
                    ) {
                        repeat(segments) { index ->
                            val segmentValue = (segments - index).toFloat() / segments
                            val isActive = animatedValue >= segmentValue
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        color = if (isActive) {
                                            when {
                                                segmentValue > 0.9f -> Color(0xFFFF4444)
                                                segmentValue > 0.7f -> Color(0xFFFFAA00)
                                                segmentValue > 0.5f -> Color(0xFFFFDD00)
                                                else -> Color(0xFF44FF44)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        },
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }

                    // Peak indicator
                    if (showPeak && animatedPeak > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedPeak)
                                .padding(top = 2.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(2.dp)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Labels
                Column {
                    if (label.isNotEmpty()) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (showValue) {
                        Text(
                            text = String.format("%.1f", animatedValue * 100),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (unit.isNotEmpty()) {
                            Text(
                                text = unit,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class MeterOrientation {
    Horizontal,
    Vertical
}

/**
 * Professional enumeration picker with dropdown and button modes.
 * 
 * @param value Current selected index
 * @param options List of option labels
 * @param onValueChange Callback when selection changes
 * @param label Control label
 * @param mode Display mode (dropdown or buttons)
 * @param modifier Compose modifier for layout customization
 */
@Composable
fun LspEnumPicker(
    value: Int,
    options: List<String>,
    onValueChange: (Int) -> Unit,
    label: String = "",
    mode: EnumPickerMode = EnumPickerMode.Buttons,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        when (mode) {
            EnumPickerMode.Buttons -> {
                if (options.size <= 4) {
                    // Single row for few options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        options.forEachIndexed { index, option ->
                            EnumButton(
                                text = option,
                                isSelected = index == value,
                                onClick = {
                                    onValueChange(index)
                                    performControlAdjustFeedback(context)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Grid layout for many options
                    val rows = (options.size + 2) / 3
                    repeat(rows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) { col ->
                                val index = row * 3 + col
                                if (index < options.size) {
                                    EnumButton(
                                        text = options[index],
                                        isSelected = index == value,
                                        onClick = {
                                            onValueChange(index)
                                            performControlAdjustFeedback(context)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (row < rows - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            EnumPickerMode.Dropdown -> {
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (value in options.indices) options[value] else "Select...",
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp 
                                            else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        color = if (index == value) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    onValueChange(index)
                                    expanded = false
                                    performControlAdjustFeedback(context)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnumButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

enum class EnumPickerMode {
    Buttons,
    Dropdown
}

/**
 * Advanced numeric keypad dialog for direct parameter value entry.
 * 
 * @param currentValue Current value to display
 * @param minValue Minimum allowed value
 * @param maxValue Maximum allowed value
 * @param unit Unit string for display
 * @param isLogScale Whether the parameter uses logarithmic scaling
 * @param onValueConfirmed Callback when value is confirmed
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun NumericKeypadDialog(
    currentValue: Float,
    minValue: Float,
    maxValue: Float,
    unit: String,
    isLogScale: Boolean = false,
    onValueConfirmed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var inputValue by remember { 
        mutableStateOf(String.format("%.3f", currentValue).trimEnd('0').trimEnd('.'))
    }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Validate input value
    val validateInput = { input: String ->
        val parsed = input.toFloatOrNull()
        when {
            parsed == null -> {
                isError = true
                errorMessage = "Invalid number format"
                false
            }
            parsed < minValue -> {
                isError = true
                errorMessage = "Value below minimum ($minValue)"
                false
            }
            parsed > maxValue -> {
                isError = true
                errorMessage = "Value above maximum ($maxValue)"
                false
            }
            else -> {
                isError = false
                errorMessage = ""
                true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Enter Value",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Value display with error state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${inputValue} $unit".trim(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center
                        )
                        
                        if (isError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Keypad grid
                val keypadLayout = listOf(
                    listOf("7", "8", "9"),
                    listOf("4", "5", "6"),
                    listOf("1", "2", "3"),
                    listOf("±", "0", "."),
                    listOf("C", "⌫", "")
                )

                keypadLayout.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            if (key.isNotEmpty()) {
                                KeypadButton(
                                    label = key,
                                    onClick = {
                                        when (key) {
                                            "⌫" -> {
                                                if (inputValue.length > 1) {
                                                    inputValue = inputValue.dropLast(1)
                                                } else {
                                                    inputValue = "0"
                                                }
                                                validateInput(inputValue)
                                            }
                                            "C" -> {
                                                inputValue = "0"
                                                isError = false
                                                errorMessage = ""
                                            }
                                            "." -> {
                                                if (!inputValue.contains(".")) {
                                                    inputValue += "."
                                                }
                                            }
                                            "±" -> {
                                                if (inputValue.startsWith("-")) {
                                                    inputValue = inputValue.drop(1)
                                                } else if (inputValue != "0") {
                                                    inputValue = "-$inputValue"
                                                }
                                                validateInput(inputValue)
                                            }
                                            else -> {
                                                if (inputValue == "0") {
                                                    inputValue = key
                                                } else {
                                                    inputValue += key
                                                }
                                                validateInput(inputValue)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    isSpecial = key in listOf("C", "⌫", "±")
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            if (validateInput(inputValue)) {
                                val parsedValue = inputValue.toFloat()
                                onValueConfirmed(parsedValue)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isError,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Range information
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Range: $minValue - $maxValue $unit".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSpecial) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSpecial) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Advanced haptic feedback system for professional audio control feel.
 */
private fun performControlAdjustFeedback(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(
            12, // Short, crisp feedback
            (VibrationEffect.DEFAULT_AMPLITUDE * 0.4f).toInt()
        )
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(12)
    }
}

private fun performPrecisionModeFeedback(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Distinctive double-pulse for precision mode activation
        val timings = longArrayOf(0, 25, 40, 25)
        val amplitudes = intArrayOf(0, 120, 0, 80)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 25, 40, 25), -1)
    }
}

private fun performResetFeedback(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Firm, satisfying feedback for reset action
        val effect = VibrationEffect.createOneShot(
            45,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(45)
    }
}
