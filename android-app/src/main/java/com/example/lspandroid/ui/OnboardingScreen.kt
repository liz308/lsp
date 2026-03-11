package com.example.lspandroid.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

/**
 * Comprehensive onboarding flow for LSP Audio Plugins Android app.
 * Provides interactive demonstrations of plugin chain concepts, control gestures,
 * and plugin browser functionality with real-world examples and animations.
 * 
 * Requirement 27: Onboarding Experience
 * Requirement 9.3: Double-tap reset demonstration
 * Requirement 9.4: Long-press precision mode demonstration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 7 })
    val scope = rememberCoroutineScope()
    var isSkipping by remember { mutableStateOf(false) }

    LaunchedEffect(isSkipping) {
        if (isSkipping) {
            pagerState.animateScrollToPage(6)
            isSkipping = false
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header with skip button
            Row(
            modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = "LSP Audio Setup",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(
                    onClick = { isSkipping = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
    ) {
                    Text("Skip Tutorial", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1) / 7f },
            modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pager content
            HorizontalPager(
                state = pagerState,
            modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false
            ) { page ->
                OnboardingPage(
                    page = page,
                    modifier = Modifier.fillMaxSize()
                )
        }

            // Page indicators with labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
        ) {
                val pageLabels = listOf("Welcome", "Chains", "Controls", "Gestures", "Browser", "Presets", "Ready")
                repeat(7) { index ->
            Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                        Box(
                            modifier = Modifier
                                .size(
                                    if (pagerState.currentPage == index) 12.dp else 8.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else if (index < pagerState.currentPage)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                )
                        )
                        
                        if (pagerState.currentPage == index) {
                            Text(
                                text = pageLabels[index],
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
            }
        }
    }
}
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous", fontSize = 14.sp)
                }

                // Next/Done button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 6) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage < 6) "Continue" else "Start Using LSP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (pagerState.currentPage < 6) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual onboarding page with comprehensive content.
 */
@Composable
private fun OnboardingPage(
    page: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (page) {
            0 -> WelcomePage()
            1 -> PluginChainPage()
            2 -> ControlsPage()
            3 -> GesturesPage()
            4 -> PluginBrowserPage()
            5 -> PresetsPage()
            6 -> CompletionPage()
        }
    }
}

/**
 * Welcome page with app introduction and key features.
 */
@Composable
private fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Animated logo
        var isAnimating by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            delay(500)
            isAnimating = true
        }
        
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                )
                .scale(if (isAnimating) 1f else 0.8f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(80.dp)
            ) {
                val waveformPoints = listOf(
                    Offset(0f, size.height * 0.5f),
                    Offset(size.width * 0.2f, size.height * 0.3f),
                    Offset(size.width * 0.4f, size.height * 0.7f),
                    Offset(size.width * 0.6f, size.height * 0.2f),
                    Offset(size.width * 0.8f, size.height * 0.6f),
                    Offset(size.width, size.height * 0.5f)
                )
                
                val path = Path().apply {
                    moveTo(waveformPoints[0].x, waveformPoints[0].y)
                    for (i in 1 until waveformPoints.size) {
                        lineTo(waveformPoints[i].x, waveformPoints[i].y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color(0xFF1976D2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = "LSP Audio Plugins",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Professional Audio Processing",
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Studio-Grade Processing",
                    description = "Same algorithms used in professional DAWs, optimized for mobile"
                )
                
                FeatureItem(
                    icon = Icons.Default.Link,
                    title = "Flexible Plugin Chains",
                    description = "Connect multiple effects in any order for complex processing"
                )
                
                FeatureItem(
                    icon = Icons.Default.TouchApp,
                    title = "Intuitive Touch Controls",
                    description = "Precision parameter control designed for touchscreens"
                )
                
                FeatureItem(
                    icon = Icons.Default.Tune,
                    title = "Real-time Processing",
                    description = "Ultra-low latency audio processing with hardware acceleration"
                )
            }
        }

        Text(
            text = "Let's explore how to build professional audio processing chains on your Android device.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Feature item for welcome page.
 */
@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Plugin chain concept with interactive visualization.
 */
@Composable
private fun PluginChainPage() {
    var animationPhase by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            animationPhase = (animationPhase + 1) % 4
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Understanding Plugin Chains",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Audio flows through multiple processors in sequence, each adding its own character to the sound.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Interactive chain visualization
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Signal Flow Example",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Animated signal flow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Input
                    PluginChainNode(
                        label = "INPUT",
                        sublabel = "Raw Audio",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        isActive = animationPhase >= 0
                    )

                    AnimatedArrow(isActive = animationPhase >= 1)

                    // High-pass filter
                    PluginChainNode(
                        label = "HPF",
                        sublabel = "80Hz",
                        color = MaterialTheme.colorScheme.primaryContainer,
                        isActive = animationPhase >= 1
                    )

                    AnimatedArrow(isActive = animationPhase >= 2)

                    // Parametric EQ
                    PluginChainNode(
                        label = "EQ",
                        sublabel = "4-Band",
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        isActive = animationPhase >= 2
                    )

                    AnimatedArrow(isActive = animationPhase >= 3)

                    // Compressor
                    PluginChainNode(
                        label = "COMP",
                        sublabel = "3:1",
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        isActive = animationPhase >= 3
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chain description
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (animationPhase) {
                                0 -> "Raw audio signal enters the chain"
                                1 -> "High-pass filter removes low-frequency rumble"
                                2 -> "Parametric EQ shapes the frequency response"
                                3 -> "Compressor controls dynamics and adds punch"
                                else -> "Processed audio output with enhanced clarity"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Chain benefits
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Why Use Plugin Chains?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                ChainBenefit(
                    title = "Modular Processing",
                    description = "Each plugin handles a specific task optimally"
                )
                
                ChainBenefit(
                    title = "Order Matters",
                    description = "Different arrangements create different sounds"
                )
                
                ChainBenefit(
                    title = "CPU Efficient",
                    description = "Only active plugins consume processing power"
                )
                
                ChainBenefit(
                    title = "Easy Experimentation",
                    description = "Add, remove, or reorder plugins instantly"
                )
            }
        }
    }
}

/**
 * Plugin chain node visualization.
 */
@Composable
private fun PluginChainNode(
    label: String,
    sublabel: String,
    color: Color,
    isActive: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = tween(300)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = alpha))
                .border(
                    width = if (isActive) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
        }
        
        Text(
            text = sublabel,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Animated arrow for chain visualization.
 */
@Composable
private fun AnimatedArrow(isActive: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(300)
    )
    
    Icon(
        Icons.Default.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        modifier = Modifier.size(20.dp)
    )
}

/**
 * Chain benefit item.
 */
@Composable
private fun ChainBenefit(
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(top = 6.dp)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Controls overview page.
 */
@Composable
private fun ControlsPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Plugin Controls",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Each plugin provides various types of controls for precise parameter adjustment.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Control types demonstration
        ControlTypeDemo(
            title = "Rotary Knobs",
            description = "Continuous parameters like frequency, gain, and time",
            example = { RotaryKnobExample() }
        )

        ControlTypeDemo(
            title = "Linear Sliders",
            description = "Level controls and linear parameter ranges",
            example = { LinearSliderExample() }
        )

        ControlTypeDemo(
            title = "Toggle Switches",
            description = "On/off states and mode selection",
            example = { ToggleSwitchExample() }
        )

        ControlTypeDemo(
            title = "Numeric Displays",
            description = "Precise value readouts and direct entry",
            example = { NumericDisplayExample() }
        )
    }
}

/**
 * Control type demonstration card.
 */
@Composable
private fun ControlTypeDemo(
    title: String,
    description: String,
    example: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    example()
                }
            }
        }
    }
}

/**
 * Rotary knob example.
 */
@Composable
private fun RotaryKnobExample() {
    var value by remember { mutableStateOf(0.3f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            value = 0.2f + Random.nextFloat() * (0.8f - 0.2f)
        }
    }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val boxSize = 60.dp

    Box(
        modifier = Modifier.size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.8f
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Value arc
            val sweepAngle = 270f * value
            drawArc(
                color = primaryColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Pointer
            val angle = Math.toRadians((135 + sweepAngle).toDouble())
            val pointerEnd = Offset(
                center.x + (radius * 0.7f * cos(angle)).toFloat(),
                center.y + (radius * 0.7f * sin(angle)).toFloat()
            )
            
            drawLine(
                color = primaryColor,
                start = center,
                end = pointerEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        Text(
            text = "${(value * 100).toInt()}%",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 70.dp)
        )
    }
}

/**
 * Linear slider example.
 */
@Composable
private fun LinearSliderExample() {
    var value by remember { mutableStateOf(0.6f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            value = 0.3f + Random.nextFloat() * (0.9f - 0.3f)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1976D2))
                    .align(Alignment.BottomCenter)
            )
        }
        
        Text(
            text = "${(value * 100).toInt()}%",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Toggle switch example.
 */
@Composable
private fun ToggleSwitchExample() {
    var isOn by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            isOn = !isOn
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isOn) Color(0xFF1976D2) else Color.Gray.copy(alpha = 0.3f)
                )
                .padding(2.dp),
            contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        
        Text(
            text = if (isOn) "ON" else "OFF",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Numeric display example.
 */
@Composable
private fun NumericDisplayExample() {
    var value by remember { mutableStateOf(440.0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            value = listOf(220.0f, 440.0f, 880.0f, 1000.0f).random()
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "${value.toInt()} Hz",
            fontSize = 12.sp,
            color = Color(0xFF00FF00),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Interactive gesture demonstrations.
 */
@Composable
private fun GesturesPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Touch Gestures",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Master these gestures for precise parameter control and efficient workflow.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Interactive gesture demonstrations
        InteractiveGestureDemo(
            title = "Vertical Drag",
            description = "Primary method for adjusting parameter values",
            gestureType = GestureType.DRAG
        )

        InteractiveGestureDemo(
            title = "Double-Tap Reset",
            description = "Quickly return to default value",
            gestureType = GestureType.DOUBLE_TAP
        )

        InteractiveGestureDemo(
            title = "Long-Press Precision",
            description = "10x finer control for precise adjustments",
            gestureType = GestureType.LONG_PRESS
        )

        InteractiveGestureDemo(
            title = "Value Tap Entry",
            description = "Direct numeric input for exact values",
            gestureType = GestureType.VALUE_TAP
        )

        // Gesture tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pro Tips",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                GestureTip(
                    tip = "Drag sensitivity adapts to parameter range automatically"
                )
                
                GestureTip(
                    tip = "Long-press works with drag for ultra-precise control"
                )
                
                GestureTip(
                    tip = "Double-tap works on any control type"
                )
                
                GestureTip(
                    tip = "Value displays show units and accept typed input"
                )
            }
        }
    }
}

/**
 * Gesture types for demonstration.
 */
private enum class GestureType {
    DRAG, DOUBLE_TAP, LONG_PRESS, VALUE_TAP
}

/**
 * Interactive gesture demonstration.
 */
@Composable
private fun InteractiveGestureDemo(
    title: String,
    description: String,
    gestureType: GestureType
) {
    var isActive by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(0.5f) }
    var showPrecisionMode by remember { mutableStateOf(false) }
    var showValueEntry by remember { mutableStateOf(false) }
    
    LaunchedEffect(gestureType) {
        while (true) {
            delay(4000)
            when (gestureType) {
                GestureType.DRAG -> {
                    isActive = true
                    delay(100)
                    repeat(10) {
                        value = (value + (Random.nextFloat() * 0.2f - 0.1f)).coerceIn(0f, 1f)
                        delay(100)
                    }
                    isActive = false
                }
                GestureType.DOUBLE_TAP -> {
                    isActive = true
                    delay(200)
                    value = 0.5f // Default value
                    delay(300)
                    isActive = false
                }
                GestureType.LONG_PRESS -> {
                    showPrecisionMode = true
                    isActive = true
                    delay(500)
                    repeat(5) {
                        value = (value + (Random.nextFloat() * 0.04f - 0.02f)).coerceIn(0f, 1f)
                        delay(200)
                    }
                    isActive = false
                    showPrecisionMode = false
                }
                GestureType.VALUE_TAP -> {
                    showValueEntry = true
                    delay(1000)
                    value = 0.2f + Random.nextFloat() * (0.8f - 0.2f)
                    delay(500)
                    showValueEntry = false
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (showPrecisionMode) {
                        Text(
                            text = "Precision Mode Active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Interactive control
                Box(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    when (gestureType) {
                        GestureType.DRAG, GestureType.LONG_PRESS -> {
                            InteractiveKnob(
                                value = value,
                                isActive = isActive,
                                isPrecisionMode = showPrecisionMode
                            )
                        }
                        GestureType.DOUBLE_TAP -> {
                            InteractiveKnob(
                                value = value,
                                isActive = isActive,
                                showReset = true
                            )
                        }
                        GestureType.VALUE_TAP -> {
                            InteractiveValueDisplay(
                                value = value,
                                showEntry = showValueEntry
                            )
                        }
                    }
                }
            }
            
            // Gesture visualization
            GestureVisualization(gestureType, isActive)
        }
    }
}

/**
 * Interactive knob for gesture demonstration.
 */
@Composable
private fun InteractiveKnob(
    value: Float,
    isActive: Boolean,
    isPrecisionMode: Boolean = false,
    showReset: Boolean = false
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val boxSize = 80.dp

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.7f
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            
            // Value arc
            val sweepAngle = 270f * value
            drawArc(
                color = if (isPrecisionMode) Color(0xFFFF6B35) else primaryColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = if (isActive) 8.dp.toPx() else 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Pointer
            val angle = Math.toRadians((135 + sweepAngle).toDouble())
            val pointerEnd = Offset(
                center.x + (radius * 0.8f * cos(angle)).toFloat(),
                center.y + (radius * 0.8f * sin(angle)).toFloat()
            )
            
            drawLine(
                color = if (isPrecisionMode) Color(0xFFFF6B35) else primaryColor,
                start = center,
                end = pointerEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Center dot
            drawCircle(
                color = surfaceColor,
                radius = 4.dp.toPx(),
                center = center
            )
        }
        
        if (showReset) {
            Text(
                text = "RESET",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 90.dp)
            )
        }
        
        Text(
            text = "${(value * 100).toInt()}%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = if (showReset) 110.dp else 90.dp)
        )
    }
}

/**
 * Interactive value display for gesture demonstration.
 */
@Composable
private fun InteractiveValueDisplay(
    value: Float,
    showEntry: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (showEntry) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else 
                    Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(
                width = if (showEntry) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Text(
                text = if (showEntry) "1.2 kHz" else "${(value * 2000 + 200).toInt()} Hz",
                fontSize = 14.sp,
                color = if (showEntry) MaterialTheme.colorScheme.primary else Color(0xFF00FF00),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        if (showEntry) {
            Text(
                text = "Keyboard Input",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Gesture visualization.
 */
@Composable
private fun GestureVisualization(
    gestureType: GestureType,
    isActive: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(300)
    )
    
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (gestureType) {
            GestureType.DRAG -> {
                Icon(
                    Icons.Default.SwipeVertical,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Drag up/down",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            GestureType.DOUBLE_TAP -> {
                Text(
                    text = "👆👆",
                    fontSize = 20.sp,
                    modifier = Modifier.scale(if (isActive) 1.2f else 1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Double-tap",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            GestureType.LONG_PRESS -> {
                Text(
                    text = "👆",
                    fontSize = 20.sp,
                    modifier = Modifier.scale(if (isActive) 1.3f else 1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hold & drag",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            GestureType.VALUE_TAP -> {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap value",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
    }
}

/**
 * Gesture tip item.
 */
@Composable
private fun GestureTip(tip: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(top = 6.dp)
        )
        
        Text(
            text = tip,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Plugin browser demonstration with real categories and search.
 */
@Composable
private fun PluginBrowserPage() {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Plugin Browser",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Discover and add professional audio plugins to your processing chain.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Search and categories
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        isSearching = it.isNotEmpty()
                    },
                    placeholder = { Text("Search plugins...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                isSearching = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category tabs
                if (!isSearching) {
                    val categories = listOf("All", "EQ", "Dynamics", "Modulation", "Delay", "Reverb", "Utility")
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                onClick = { selectedCategory = category },
                                label = { Text(category, fontSize = 12.sp) },
                                selected = selectedCategory == category,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Plugin grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isSearching) "Search Results" else "$selectedCategory Plugins",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                // Plugin items
                val plugins = getPluginsForCategory(selectedCategory, searchQuery)
                
                plugins.take(6).forEach { plugin ->
                    PluginBrowserItem(plugin)
                }
                
                if (plugins.size > 6) {
                    Text(
                        text = "+${plugins.size - 6} more plugins available",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Browser tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Browser Features",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                BrowserFeature(
                    icon = Icons.Default.Category,
                    title = "Category Filtering",
                    description = "Browse plugins by type and function"
                )
                
                BrowserFeature(
                    icon = Icons.Default.Search,
                    title = "Smart Search",
                    description = "Find plugins by name, manufacturer, or function"
                )
                
                BrowserFeature(
                    icon = Icons.Default.Star,
                    title = "Favorites",
                    description = "Mark frequently used plugins for quick access"
                )
                
                BrowserFeature(
                    icon = Icons.Default.Info,
                    title = "Plugin Info",
                    description = "Detailed specifications and parameter descriptions"
                )
            }
        }
    }
}

/**
 * Plugin data class for browser demonstration.
 */
private data class OnboardingPluginInfo(
    val name: String,
    val manufacturer: String,
    val category: String,
    val description: String,
    val cpuUsage: String,
    val isFavorite: Boolean = false
)

/**
 * Get plugins for category and search.
 */
private fun getPluginsForCategory(category: String, searchQuery: String): List<OnboardingPluginInfo> {
    val allPlugins = listOf(
        OnboardingPluginInfo("Parametric EQ", "LSP", "EQ", "32-band parametric equalizer", "Low"),
        OnboardingPluginInfo("Graphic EQ", "LSP", "EQ", "31-band graphic equalizer", "Medium"),
        OnboardingPluginInfo("Dynamic EQ", "LSP", "EQ", "Frequency-dependent dynamics", "High"),
        OnboardingPluginInfo("Compressor", "LSP", "Dynamics", "Transparent optical compressor", "Low"),
        OnboardingPluginInfo("Multiband Compressor", "LSP", "Dynamics", "4-band dynamics processor", "High"),
        OnboardingPluginInfo("Gate", "LSP", "Dynamics", "Noise gate with sidechain", "Low"),
        OnboardingPluginInfo("Limiter", "LSP", "Dynamics", "Transparent peak limiter", "Medium"),
        OnboardingPluginInfo("Chorus", "LSP", "Modulation", "Classic chorus effect", "Medium"),
        OnboardingPluginInfo("Flanger", "LSP", "Modulation", "Vintage flanger with feedback", "Medium"),
        OnboardingPluginInfo("Phaser", "LSP", "Modulation", "Multi-stage phaser", "Low"),
        OnboardingPluginInfo("Delay", "LSP", "Delay", "Stereo delay with feedback", "Medium"),
        OnboardingPluginInfo("Echo", "LSP", "Delay", "Tape echo simulation", "High"),
        OnboardingPluginInfo("Reverb", "LSP", "Reverb", "Algorithmic reverb", "High"),
        OnboardingPluginInfo("Room Reverb", "LSP", "Reverb", "Natural room simulation", "High"),
        OnboardingPluginInfo("Spectrum Analyzer", "LSP", "Utility", "Real-time frequency analysis", "Medium"),
        OnboardingPluginInfo("Oscilloscope", "LSP", "Utility", "Waveform visualization", "Low"),
        OnboardingPluginInfo("Tuner", "LSP", "Utility", "Chromatic instrument tuner", "Low")
    )
    
    return allPlugins.filter { plugin ->
        val matchesCategory = category == "All" || plugin.category == category
        val matchesSearch = searchQuery.isEmpty() || 
            plugin.name.contains(searchQuery, ignoreCase = true) ||
            plugin.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }
}

/**
 * Plugin browser item.
 */
@Composable
private fun PluginBrowserItem(plugin: OnboardingPluginInfo) {
    var isAdding by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Plugin icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (plugin.category) {
                                "EQ" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                "Dynamics" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                "Modulation" -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                                "Delay" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                "Reverb" -> Color(0xFF00BCD4).copy(alpha = 0.2f)
                                else -> Color(0xFF607D8B).copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (plugin.category) {
                            "EQ" -> "EQ"
                            "Dynamics" -> "DYN"
                            "Modulation" -> "MOD"
                            "Delay" -> "DLY"
                            "Reverb" -> "REV"
                            else -> "UTL"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (plugin.category) {
                            "EQ" -> Color(0xFF4CAF50)
                            "Dynamics" -> Color(0xFFFF9800)
                            "Modulation" -> Color(0xFF9C27B0)
                            "Delay" -> Color(0xFF2196F3)
                            "Reverb" -> Color(0xFF00BCD4)
                            else -> Color(0xFF607D8B)
                        }
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plugin.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (plugin.isFavorite) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = plugin.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plugin.manufacturer,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "${plugin.cpuUsage} CPU",
                            fontSize = 10.sp,
                            color = when (plugin.cpuUsage) {
                                "Low" -> Color(0xFF4CAF50)
                                "Medium" -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                }
            }
            
            // Add button
            Button(
                onClick = { 
                    isAdding = true
                    // Simulate adding delay
                },
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdding) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.primary
                ),
                enabled = !isAdding
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add", fontSize = 12.sp)
                }
            }
        }
    }
    
    LaunchedEffect(isAdding) {
        if (isAdding) {
            delay(1500)
            isAdding = false
        }
    }
}

/**
 * Browser feature item.
 */
@Composable
private fun BrowserFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Presets page demonstration.
 */
@Composable
private fun PresetsPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Presets", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Demonstrate preset management here.")
    }
}

/**
 * Completion page.
 */
@Composable
private fun CompletionPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("You're all set!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Happy mixing!")
    }
}

