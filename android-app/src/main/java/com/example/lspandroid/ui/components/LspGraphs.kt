package com.example.lspandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Professional frequency response graph component for LSP plugins.
 * Displays accurate frequency response curves with proper scaling and grid.
 * 
 * @param frequencies Array of frequency values in Hz (20-20000 Hz range)
 * @param magnitudes Array of magnitude values in dB (-48 to +48 dB range)
 * @param phases Array of phase values in degrees (optional)
 * @param label Graph title
 * @param showPhase Whether to display phase response
 * @param gridDensity Grid line density (1=sparse, 2=normal, 3=dense)
 * @param curveColor Color of the frequency response curve
 * @param phaseColor Color of the phase response curve
 */
@Composable
fun LspFreqGraph(
    frequencies: FloatArray = floatArrayOf(),
    magnitudes: FloatArray = floatArrayOf(),
    phases: FloatArray = floatArrayOf(),
    label: String = "Frequency Response",
    showPhase: Boolean = false,
    gridDensity: Int = 2,
    curveColor: Color = Color(0xFF00FFFF),
    phaseColor: Color = Color(0xFFFF6B35),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            FrequencyGraphCanvas(
                frequencies = frequencies,
                magnitudes = magnitudes,
                phases = phases,
                showPhase = showPhase,
                gridDensity = gridDensity,
                curveColor = curveColor,
                phaseColor = phaseColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Comprehensive frequency scale labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("20", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("50", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("100", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("200", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("500", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("1k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("2k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("5k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("10k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("20k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Magnitude scale on the left
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("+24", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+12", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("-12", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("-24", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (showPhase) {
                Column(
                    modifier = Modifier.width(40.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("+180°", fontSize = 9.sp, color = phaseColor)
                    Text("+90°", fontSize = 9.sp, color = phaseColor)
                    Text("0°", fontSize = 9.sp, color = phaseColor, fontWeight = FontWeight.Bold)
                    Text("-90°", fontSize = 9.sp, color = phaseColor)
                    Text("-180°", fontSize = 9.sp, color = phaseColor)
                }
            }
        }
    }
}

/**
 * Canvas for drawing professional frequency response graph.
 */
@Composable
private fun FrequencyGraphCanvas(
    frequencies: FloatArray,
    magnitudes: FloatArray,
    phases: FloatArray,
    showPhase: Boolean,
    gridDensity: Int,
    curveColor: Color,
    phaseColor: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawFrequencyGraph(
            frequencies, magnitudes, phases, showPhase, 
            gridDensity, curveColor, phaseColor
        )
    }
}

/**
 * Draws a professional frequency response graph with accurate scaling.
 */
private fun DrawScope.drawFrequencyGraph(
    frequencies: FloatArray,
    magnitudes: FloatArray,
    phases: FloatArray,
    showPhase: Boolean,
    gridDensity: Int,
    curveColor: Color,
    phaseColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2

    // Professional color scheme
    val majorGridColor = Color(0xFF404040)
    val minorGridColor = Color(0xFF2A2A2A)
    val zeroLineColor = Color(0xFF606060)

    // Draw frequency grid lines (logarithmic scale)
    val majorFreqs = listOf(20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)
    val minorFreqs = listOf(30f, 40f, 60f, 80f, 150f, 300f, 400f, 600f, 800f, 1500f, 3000f, 4000f, 6000f, 8000f, 15000f)

    // Major frequency grid lines
    for (freq in majorFreqs) {
        val x = frequencyToX(freq, width)
        drawLine(
            color = majorGridColor,
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x, height),
            strokeWidth = 1f
        )
    }

    // Minor frequency grid lines (if high density)
    if (gridDensity >= 2) {
        for (freq in minorFreqs) {
            val x = frequencyToX(freq, width)
    drawLine(
                color = minorGridColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, height),
                strokeWidth = 0.5f
    )
        }
    }

    // Magnitude grid lines
    val majorDbSteps = listOf(-24f, -12f, 0f, 12f, 24f)
    val minorDbSteps = listOf(-18f, -6f, 6f, 18f)

    // Major dB grid lines
    for (db in majorDbSteps) {
        val y = magnitudeToY(db, height)
        val color = if (db == 0f) zeroLineColor else majorGridColor
        val strokeWidth = if (db == 0f) 1.5f else 1f
            drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(width, y),
            strokeWidth = strokeWidth
            )
        }

    // Minor dB grid lines
    if (gridDensity >= 2) {
        for (db in minorDbSteps) {
            val y = magnitudeToY(db, height)
            drawLine(
                color = minorGridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 0.5f
            )
    }
}
    // Extra fine grid for maximum density
    if (gridDensity >= 3) {
        for (db in -24..24 step 3) {
            val dbF = db.toFloat()
            if (dbF !in majorDbSteps && dbF !in minorDbSteps) {
                val y = magnitudeToY(dbF, height)
                drawLine(
                    color = Color(0xFF1F1F1F),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 0.25f
                )
            }
        }
    }

    // Draw magnitude response curve
    if (frequencies.isNotEmpty() && magnitudes.isNotEmpty() && frequencies.size == magnitudes.size) {
        val path = Path()
        var firstPoint = true

        for (i in frequencies.indices) {
            val freq = frequencies[i]
            val mag = magnitudes[i]
            
            if (freq in 20f..20000f && mag in -48f..48f) {
                val x = frequencyToX(freq, width)
                val y = magnitudeToY(mag, height)
                
                if (firstPoint) {
                    path.moveTo(x, y)
                    firstPoint = false
                } else {
                    path.lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = curveColor,
            style = Stroke(width = 2.5f)
        )
    }

    // Draw phase response curve
    if (showPhase && phases.isNotEmpty() && frequencies.size == phases.size) {
        val phasePath = Path()
        var firstPhasePoint = true

        for (i in frequencies.indices) {
            val freq = frequencies[i]
            val phase = phases[i]
            
            if (freq in 20f..20000f && phase in -180f..180f) {
                val x = frequencyToX(freq, width)
                val y = phaseToY(phase, height)
                
                if (firstPhasePoint) {
                    phasePath.moveTo(x, y)
                    firstPhasePoint = false
                } else {
                    phasePath.lineTo(x, y)
                }
            }
        }

        drawPath(
            path = phasePath,
            color = phaseColor,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Converts frequency to X coordinate using logarithmic scale.
 */
private fun frequencyToX(frequency: Float, width: Float): Float {
    val minFreq = 20f
    val maxFreq = 20000f
    val logFreq = log10(frequency.coerceIn(minFreq, maxFreq))
    val logMin = log10(minFreq)
    val logMax = log10(maxFreq)
    return width * (logFreq - logMin) / (logMax - logMin)
}

/**
 * Converts magnitude (dB) to Y coordinate.
 */
private fun magnitudeToY(magnitude: Float, height: Float): Float {
    val centerY = height / 2
    val dbRange = 48f // ±24 dB range
    return centerY - (magnitude * height / (2 * dbRange))
}

/**
 * Converts phase (degrees) to Y coordinate.
 */
private fun phaseToY(phase: Float, height: Float): Float {
    val phaseRange = 360f // ±180 degrees
    return height * (1f - (phase + 180f) / phaseRange)
}

/**
 * Professional dynamics curve graph component for compressors, limiters, and gates.
 * Displays accurate input/output transfer characteristics.
 * 
 * @param inputLevels Array of input level values in dB (-80 to +20 dB range)
 * @param outputLevels Array of output level values in dB (-80 to +20 dB range)
 * @param threshold Threshold level in dB
 * @param ratio Compression ratio (1.0 = no compression, >1.0 = compression)
 * @param kneeWidth Knee width in dB for soft knee compression
 * @param label Graph title
 * @param showThreshold Whether to display threshold line
 * @param showKnee Whether to display knee region
 * @param curveColor Color of the dynamics curve
 * @param thresholdColor Color of the threshold line
 */
@Composable
fun LspDynamicsGraph(
    inputLevels: FloatArray = floatArrayOf(),
    outputLevels: FloatArray = floatArrayOf(),
    threshold: Float = -12f,
    ratio: Float = 4f,
    kneeWidth: Float = 6f,
    label: String = "Dynamics Curve",
    showThreshold: Boolean = true,
    showKnee: Boolean = true,
    curveColor: Color = Color(0xFFFFD700),
    thresholdColor: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            DynamicsGraphCanvas(
                inputLevels = inputLevels,
                outputLevels = outputLevels,
                threshold = threshold,
                ratio = ratio,
                kneeWidth = kneeWidth,
                showThreshold = showThreshold,
                showKnee = showKnee,
                curveColor = curveColor,
                thresholdColor = thresholdColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Input level scale (bottom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-80", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-60", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-40", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-20", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("+20", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Output level scale (left) and ratio info (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("+20", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("-20", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("-40", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("-60", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("-80", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(
                modifier = Modifier.width(60.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Ratio: ${ratio}:1", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Thresh: ${threshold}dB", fontSize = 9.sp, color = thresholdColor)
                if (showKnee && kneeWidth > 0f) {
                    Text("Knee: ${kneeWidth}dB", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Canvas for drawing professional dynamics curve graph.
 */
@Composable
private fun DynamicsGraphCanvas(
    inputLevels: FloatArray,
    outputLevels: FloatArray,
    threshold: Float,
    ratio: Float,
    kneeWidth: Float,
    showThreshold: Boolean,
    showKnee: Boolean,
    curveColor: Color,
    thresholdColor: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawDynamicsGraph(
            inputLevels, outputLevels, threshold, ratio, kneeWidth,
            showThreshold, showKnee, curveColor, thresholdColor
        )
    }
}

/**
 * Draws a professional dynamics curve with accurate compression characteristics.
 */
private fun DrawScope.drawDynamicsGraph(
    inputLevels: FloatArray,
    outputLevels: FloatArray,
    threshold: Float,
    ratio: Float,
    kneeWidth: Float,
    showThreshold: Boolean,
    showKnee: Boolean,
    curveColor: Color,
    thresholdColor: Color
) {
    val width = size.width
    val height = size.height
    val minLevel = -80f
    val maxLevel = 20f
    val levelRange = maxLevel - minLevel

    // Professional color scheme
    val majorGridColor = Color(0xFF404040)
    val minorGridColor = Color(0xFF2A2A2A)
    val unityGainColor = Color(0xFF606060)
    val kneeColor = Color(0xFF808080)

    // Draw grid
    val majorSteps = listOf(-80f, -60f, -40f, -20f, 0f, 20f)
    val minorSteps = listOf(-70f, -50f, -30f, -10f, 10f)

    // Major grid lines
    for (level in majorSteps) {
        val pos = levelToPosition(level, minLevel, maxLevel, width)
        val posY = levelToPosition(level, minLevel, maxLevel, height, true)
        
        // Vertical lines
        drawLine(
            color = if (level == 0f) unityGainColor else majorGridColor,
            start = androidx.compose.ui.geometry.Offset(pos, 0f),
            end = androidx.compose.ui.geometry.Offset(pos, height),
            strokeWidth = if (level == 0f) 1.5f else 1f
        )
        
        // Horizontal lines
        drawLine(
            color = if (level == 0f) unityGainColor else majorGridColor,
            start = androidx.compose.ui.geometry.Offset(0f, posY),
            end = androidx.compose.ui.geometry.Offset(width, posY),
            strokeWidth = if (level == 0f) 1.5f else 1f
        )
    }

    // Minor grid lines
    for (level in minorSteps) {
        val pos = levelToPosition(level, minLevel, maxLevel, width)
        val posY = levelToPosition(level, minLevel, maxLevel, height, true)
        
        drawLine(
            color = minorGridColor,
            start = androidx.compose.ui.geometry.Offset(pos, 0f),
            end = androidx.compose.ui.geometry.Offset(pos, height),
            strokeWidth = 0.5f
        )
        
        drawLine(
            color = minorGridColor,
            start = androidx.compose.ui.geometry.Offset(0f, posY),
            end = androidx.compose.ui.geometry.Offset(width, posY),
            strokeWidth = 0.5f
        )
    }

    // Draw unity gain line (1:1 ratio)
    drawLine(
        color = unityGainColor,
        start = androidx.compose.ui.geometry.Offset(0f, height),
        end = androidx.compose.ui.geometry.Offset(width, 0f),
        strokeWidth = 2f
    )

    // Draw threshold line
    if (showThreshold) {
        val thresholdX = levelToPosition(threshold, minLevel, maxLevel, width)
        val thresholdY = levelToPosition(threshold, minLevel, maxLevel, height, true)
        
        // Vertical threshold line
        drawLine(
            color = thresholdColor,
            start = androidx.compose.ui.geometry.Offset(thresholdX, 0f),
            end = androidx.compose.ui.geometry.Offset(thresholdX, height),
            strokeWidth = 2f
        )
        
        // Horizontal threshold line
        drawLine(
            color = thresholdColor,
            start = androidx.compose.ui.geometry.Offset(0f, thresholdY),
            end = androidx.compose.ui.geometry.Offset(width, thresholdY),
            strokeWidth = 2f
        )
    }

    // Draw knee region
    if (showKnee && kneeWidth > 0f) {
        val kneeStart = threshold - kneeWidth / 2
        val kneeEnd = threshold + kneeWidth / 2
        val kneeStartX = levelToPosition(kneeStart, minLevel, maxLevel, width)
        val kneeEndX = levelToPosition(kneeEnd, minLevel, maxLevel, width)
        
        drawRect(
            color = kneeColor.copy(alpha = 0.1f),
            topLeft = androidx.compose.ui.geometry.Offset(kneeStartX, 0f),
            size = androidx.compose.ui.geometry.Size(kneeEndX - kneeStartX, height)
        )
    }

    // Draw actual dynamics curve if provided
    if (inputLevels.isNotEmpty() && outputLevels.isNotEmpty() && inputLevels.size == outputLevels.size) {
        val path = Path()
        var firstPoint = true

        for (i in inputLevels.indices) {
            val input = inputLevels[i]
            val output = outputLevels[i]
            
            if (input in minLevel..maxLevel && output in minLevel..maxLevel) {
                val x = levelToPosition(input, minLevel, maxLevel, width)
                val y = levelToPosition(output, minLevel, maxLevel, height, true)
                
                if (firstPoint) {
                    path.moveTo(x, y)
                    firstPoint = false
                } else {
                    path.lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = curveColor,
            style = Stroke(width = 3f)
        )
    } else {
        // Generate theoretical compression curve
        val theoreticalPath = Path()
        var firstTheoretical = true
        
        for (inputDb in minLevel.toInt()..maxLevel.toInt() step 1) {
            val input = inputDb.toFloat()
            val output = calculateCompressedOutput(input, threshold, ratio, kneeWidth)
            
            val x = levelToPosition(input, minLevel, maxLevel, width)
            val y = levelToPosition(output, minLevel, maxLevel, height, true)
            
            if (firstTheoretical) {
                theoreticalPath.moveTo(x, y)
                firstTheoretical = false
            } else {
                theoreticalPath.lineTo(x, y)
            }
        }

        drawPath(
            path = theoreticalPath,
            color = curveColor,
            style = Stroke(width = 3f)
        )
    }
}

/**
 * Converts level (dB) to position coordinate.
 */
private fun levelToPosition(level: Float, minLevel: Float, maxLevel: Float, dimension: Float, invert: Boolean = false): Float {
    val normalized = (level - minLevel) / (maxLevel - minLevel)
    return if (invert) dimension * (1f - normalized) else dimension * normalized
}

/**
 * Calculates compressed output level based on compressor parameters.
 */
private fun calculateCompressedOutput(input: Float, threshold: Float, ratio: Float, kneeWidth: Float): Float {
    return when {
        input <= threshold - kneeWidth / 2 -> input // Below knee
        input >= threshold + kneeWidth / 2 -> { // Above knee
            threshold + (input - threshold) / ratio
        }
        else -> { // In knee region - soft knee interpolation
            val kneeRatio = (input - (threshold - kneeWidth / 2)) / kneeWidth
            val softKneeGain = 1f - (1f - 1f / ratio) * kneeRatio * kneeRatio
            input * softKneeGain + threshold * (1f - softKneeGain)
        }
    }
}
