package com.example.lspandroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Professional audio plugin UI components with smooth animations and real-world functionality.
 * 
 * Requirement 5.3: UI Polish - Smooth parameter animation with production-ready implementations
 */

/**
 * Animated parameter value display with smooth transitions and intelligent formatting.
 * Handles various parameter types including frequency, gain, time, and percentage values.
 */
@Composable
fun AnimatedParameterValue(
    value: Float,
    minValue: Float,
    maxValue: Float,
    unit: String = "",
    modifier: Modifier = Modifier,
    parameterType: ParameterType = ParameterType.LINEAR,
    precision: Int = 2,
    showSign: Boolean = false
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "parameter_value"
    )
    
    val displayValue = when (parameterType) {
        ParameterType.LINEAR -> minValue + animatedValue * (maxValue - minValue)
        ParameterType.LOGARITHMIC -> minValue * (maxValue / minValue).toDouble().pow(animatedValue.toDouble()).toFloat()
        ParameterType.EXPONENTIAL -> minValue + (maxValue - minValue) * animatedValue * animatedValue
        ParameterType.INVERSE -> maxValue - (maxValue - minValue) * animatedValue
    }
    
    val formattedValue = when (unit.lowercase()) {
        "hz", "khz" -> formatFrequency(displayValue)
        "db" -> formatDecibels(displayValue, showSign)
        "ms", "s" -> formatTime(displayValue, unit)
        "%" -> "${displayValue.roundToInt()}%"
        else -> String.format("%.${precision}f", displayValue)
}

    val textColor = when {
        unit.lowercase() == "db" && displayValue > 0 -> MaterialTheme.colorScheme.error
        unit.lowercase() == "db" && displayValue < -60 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
                Text(
        text = "$formattedValue $unit".trim(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        modifier = modifier
        )
}
        
/**
 * Bypassed plugin visual feedback with sophisticated overlay and status indication.
 */
@Composable
fun BypassedPluginOverlay(
    isBypassed: Boolean,
    modifier: Modifier = Modifier,
    showBypassLabel: Boolean = true,
    bypassType: BypassType = BypassType.SOFT,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isBypassed && bypassType == BypassType.HARD -> 0.2f
            isBypassed && bypassType == BypassType.SOFT -> 0.6f
            else -> 1.0f
        },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "bypass_alpha"
    )
    
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isBypassed) 0.8f else 0.0f,
        animationSpec = tween(
            durationMillis = 250,
            easing = LinearEasing
        ),
        label = "overlay_alpha"
    )
    
    Box(modifier = modifier.alpha(alpha)) {
        content()
        
        if (isBypassed && overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = when (bypassType) {
                            BypassType.HARD -> MaterialTheme.colorScheme.surface.copy(alpha = overlayAlpha)
                            BypassType.SOFT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = overlayAlpha * 0.7f)
                            BypassType.MUTE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = overlayAlpha * 0.5f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showBypassLabel) {
                    Text(
                        text = when (bypassType) {
                            BypassType.HARD -> "BYPASSED"
                            BypassType.SOFT -> "SOFT BYPASS"
                            BypassType.MUTE -> "MUTED"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (bypassType) {
                            BypassType.HARD -> MaterialTheme.colorScheme.onSurface
                            BypassType.SOFT -> MaterialTheme.colorScheme.primary
                            BypassType.MUTE -> MaterialTheme.colorScheme.error
}
                    )
                }
            }
        }
    }
}
/**
 * Professional CPU usage indicator with color-coded thresholds and performance warnings.
 */
@Composable
fun CpuUsageIndicator(
    cpuPercentage: Float,
    modifier: Modifier = Modifier,
    showWarnings: Boolean = true,
    bufferSize: Int = 512,
    sampleRate: Int = 44100
) {
    val animatedCpu by animateFloatAsState(
        targetValue = cpuPercentage.coerceIn(0f, 100f),
        animationSpec = tween(
            durationMillis = 800,
            easing = LinearOutSlowInEasing
        ),
        label = "cpu_usage"
    )
    
    val (color, warningLevel) = when {
        animatedCpu > 95f -> Color(0xFFD32F2F) to "CRITICAL"
        animatedCpu > 85f -> Color(0xFFFF9800) to "HIGH"
        animatedCpu > 70f -> Color(0xFFFFC107) to "MODERATE"
        animatedCpu > 50f -> MaterialTheme.colorScheme.primary to "NORMAL"
        else -> MaterialTheme.colorScheme.tertiary to "LOW"
    }
    
    val estimatedLatency = (bufferSize.toFloat() / sampleRate * 1000).let { baseLatency ->
        baseLatency * (1 + animatedCpu / 100f * 0.5f)
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CPU",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showWarnings && animatedCpu > 70f) {
                    Text(
                        text = warningLevel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Text(
                    text = String.format("%.1f%%", animatedCpu),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(3.dp)
                )
        ) {
            val barWidth by animateFloatAsState(
                targetValue = animatedCpu / 100f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                ),
                label = "cpu_bar_width"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(barWidth)
                    .fillMaxHeight()
                    .background(
                        color = color,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
        
        if (showWarnings) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Est. latency: ${String.format("%.1f", estimatedLatency)}ms",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Professional latency compensation display with buffer size and sample rate context.
 */
@Composable
fun LatencyDisplay(
    latencyMs: Float,
    modifier: Modifier = Modifier,
    bufferSize: Int = 512,
    sampleRate: Int = 44100,
    showDetails: Boolean = false,
    compensationEnabled: Boolean = false
) {
    val animatedLatency by animateFloatAsState(
        targetValue = latencyMs,
        animationSpec = tween(
            durationMillis = 400,
            easing = LinearOutSlowInEasing
        ),
        label = "latency"
    )
    
    val theoreticalLatency = (bufferSize.toFloat() / sampleRate * 1000 * 2) // Round-trip
    val additionalLatency = animatedLatency - theoreticalLatency
    
    val statusColor = when {
        animatedLatency > 100f -> MaterialTheme.colorScheme.error
        animatedLatency > 50f -> Color(0xFFFF9800)
        animatedLatency > 20f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latency:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format("%.1f ms", animatedLatency),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            
            if (compensationEnabled) {
                Text(
                    text = "COMP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        
        if (showDetails) {
            Spacer(modifier = Modifier.height(2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Buffer: ${bufferSize}smp @ ${sampleRate/1000}kHz",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (additionalLatency > 0) {
                    Text(
                        text = "Additional: +${String.format("%.1f", additionalLatency)}ms",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * Professional A/B comparison toggle with smooth transitions and state persistence.
 */
@Composable
fun ABComparisonToggle(
    currentState: ABState,
    onStateChange: (ABState) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabels: Boolean = true,
    comparisonMode: ComparisonMode = ComparisonMode.INSTANT
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    Row(
        modifier = modifier
            .height(44.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ABButton(
            label = if (showLabels) "A" else "",
            isSelected = currentState == ABState.A,
            onClick = { 
                if (enabled) {
                    onStateChange(ABState.A)
                }
            },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            comparisonMode = comparisonMode
        )
        
        ABButton(
            label = if (showLabels) "B" else "",
            isSelected = currentState == ABState.B,
            onClick = { 
                if (enabled) {
                    onStateChange(ABState.B)
                }
            },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            comparisonMode = comparisonMode
        )
    }
}

@Composable
private fun ABButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    comparisonMode: ComparisonMode
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            isSelected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = tween(
            durationMillis = if (comparisonMode == ComparisonMode.INSTANT) 100 else 250,
            easing = FastOutSlowInEasing
        ),
        label = "ab_button_bg"
    )
    
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = if (comparisonMode == ComparisonMode.INSTANT) 100 else 250,
            easing = FastOutSlowInEasing
        ),
        label = "ab_button_text"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected && enabled) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "ab_button_scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

// Enums and data classes for type safety and configuration

enum class ABState {
    A, B
}

enum class ParameterType {
    LINEAR,
    LOGARITHMIC,
    EXPONENTIAL,
    INVERSE
}

enum class BypassType {
    SOFT,
    HARD,
    MUTE
}

enum class ComparisonMode {
    INSTANT,
    CROSSFADE
}

// Utility functions for professional audio parameter formatting

private fun formatFrequency(frequency: Float): String {
    return when {
        frequency >= 1000f -> String.format("%.1f", frequency / 1000f)
        frequency >= 100f -> String.format("%.0f", frequency)
        else -> String.format("%.1f", frequency)
    }
}

private fun formatDecibels(db: Float, showSign: Boolean): String {
    val sign = if (showSign && db > 0) "+" else ""
    return when {
        abs(db) >= 100f -> String.format("%s%.0f", sign, db)
        abs(db) >= 10f -> String.format("%s%.1f", sign, db)
        else -> String.format("%s%.2f", sign, db)
    }
}

private fun formatTime(time: Float, unit: String): String {
    return when (unit.lowercase()) {
        "ms" -> when {
            time >= 1000f -> String.format("%.2f", time / 1000f)
            time >= 100f -> String.format("%.0f", time)
            else -> String.format("%.1f", time)
        }
        "s" -> String.format("%.3f", time)
        else -> String.format("%.2f", time)
    }
}
