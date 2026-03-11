package com.example.lspandroid.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.lspandroid.AppState
import com.example.lspandroid.model.ChainedPlugin
import com.example.lspandroid.model.PluginInfo
import com.example.lspandroid.model.PluginParameter
import com.example.lspandroid.model.ChainParameter
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val Orange = Color(0xFFFF9800)
val DarkGray = Color(0xFF1E1E1E)
val ControlGridColor = Color(0xFF333333)

/**
 * Main screen for the LSP Android app.
 * Displays plugin chain and controls with professional audio interface design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appState: AppState,
    onStartAudio: () -> Unit,
    onStopAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    val chainedPlugins by appState.chainedPlugins.collectAsState()
    val showPluginBrowser by appState.showPluginBrowser.collectAsState()
    val showSettings by appState.showSettings.collectAsState()
    val showPluginEditor by appState.showPluginEditor.collectAsState()
    val selectedPluginIndex by appState.selectedPluginIndex.collectAsState()
    
    // Animation states
    val audioRunningAnimation by animateFloatAsState(
        targetValue = if (appState.isAudioRunning.value) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "audioRunning"
    )

    // Background gradient based on audio state
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            if (appState.isAudioRunning.value) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
                        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Professional header with status indicators
            ProfessionalTopBar(
                appState = appState,
                audioRunningAnimation = audioRunningAnimation,
                onSettingsClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    appState.showSettings.value = true 
                }
            )

            // Main content with proper spacing
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Enhanced audio control section
                EnhancedAudioControlSection(
                    isRunning = appState.isAudioRunning.value,
                    onStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartAudio()
                    },
                    onStop = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStopAudio()
                    },
                    error = appState.audioError.value,
                    cpuUsage = appState.cpuUsage.value,
                    bufferSize = appState.bufferSize.value,
                    sampleRate = appState.sampleRate.value,
                    latency = appState.latency.value
                )

                // Professional plugin chain section
                ProfessionalPluginChainSection(
                    appState = appState,
                    chainedPlugins = chainedPlugins,
                    selectedPluginIndex = selectedPluginIndex,
                    showPluginEditor = showPluginEditor,
                    modifier = Modifier.weight(1f),
                    onPluginReorder = { from, to ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        appState.reorderPlugins(from, to)
                    }
                )

                // Enhanced add plugin section
                EnhancedAddPluginSection(
                    onAddPlugin = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        appState.setShowPluginBrowser(true)
                    },
                    onQuickAdd = { category ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            appState.addQuickPlugin(category)
                }
                    }
                )
                }
            }

        // Professional plugin browser dialog
        if (showPluginBrowser) {
            ProfessionalPluginBrowserDialog(
                appState = appState,
                onDismiss = { appState.setShowPluginBrowser(false) }
            )
        }

        // Enhanced settings screen
        if (showSettings) {
            EnhancedSettingsScreen(
                appState = appState,
                onDismiss = { appState.setShowSettings(false) },
                onResetOnboarding = {
                    appState.resetOnboarding()
                    appState.setShowSettings(false)
                }
            )
        }

        // Plugin parameter editor overlay
        if ((selectedPluginIndex ?: -1) >= 0 && showPluginEditor) {
            PluginParameterEditor(
                plugin = selectedPluginIndex?.let { chainedPlugins.getOrNull(it) } ?: chainedPlugins[0], // fallback for safety, though check should prevent this
                onDismiss = { appState.setShowPluginEditor(false) },
                onParameterChange = { paramId, value ->
                    appState.updatePluginParameter(selectedPluginIndex!!, paramId, value)
        }
    )
}
    }
}

/**
 * Professional top bar with real-time status indicators.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalTopBar(
    appState: AppState,
    audioRunningAnimation: Float,
    onSettingsClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
                Text(
                    "LSP Audio Engine",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // Real-time status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (appState.isAudioRunning.value) {
                                Color.Green.copy(alpha = pulseAlpha)
                            } else {
                                MaterialTheme.colorScheme.outline
    }
                        )
                )
}
        },
        actions = {
            // CPU usage indicator
            if (appState.isAudioRunning.value) {
                Text(
                    text = "${appState.cpuUsage.value.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        appState.cpuUsage.value > 80 -> MaterialTheme.colorScheme.error
                        appState.cpuUsage.value > 60 -> Orange
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    )
}

/**
 * Enhanced audio control section with professional monitoring.
 */
@Composable
private fun EnhancedAudioControlSection(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    error: String?,
    cpuUsage: Float,
    bufferSize: Int,
    sampleRate: Int,
    latency: Float,
    modifier: Modifier = Modifier
) {
    val animatedCpuUsage by animateFloatAsState(
        targetValue = cpuUsage / 100f,
        animationSpec = tween(durationMillis = 500),
        label = "cpuUsage"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Engine Control",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                StatusChip(
                    isRunning = isRunning,
                    error = error
                )
            }

            // Control buttons with enhanced styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedControlButton(
                    text = "START",
                    icon = Icons.Default.PlayArrow,
                    enabled = !isRunning,
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                )

                EnhancedControlButton(
                    text = "STOP",
                    icon = Icons.Default.Stop,
                    enabled = isRunning,
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336),
                        contentColor = Color.White
                    )
                )
            }

            // Real-time monitoring section
            if (isRunning) {
                MonitoringSection(
                    cpuUsage = animatedCpuUsage,
                    bufferSize = bufferSize,
                    sampleRate = sampleRate,
                    latency = latency
                )
            }

            // Error display
            if (error != null) {
                ErrorDisplay(error = error)
            }
        }
    }
}

/**
 * Status chip component for audio engine state.
 */
@Composable
private fun StatusChip(
    isRunning: Boolean,
    error: String?
) {
    val backgroundColor = when {
        error != null -> MaterialTheme.colorScheme.errorContainer
        isRunning -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    
    val contentColor = when {
        error != null -> MaterialTheme.colorScheme.onErrorContainer
        isRunning -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            
            Text(
                text = when {
                    error != null -> "ERROR"
                    isRunning -> "RUNNING"
                    else -> "STOPPED"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Enhanced control button with icon and animations.
 */
@Composable
private fun EnhancedControlButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = tween(durationMillis = 150),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        colors = colors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (enabled) 6.dp else 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Real-time monitoring section with meters and indicators.
 */
@Composable
private fun MonitoringSection(
    cpuUsage: Float,
    bufferSize: Int,
    sampleRate: Int,
    latency: Float
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "System Monitoring",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )

        // CPU Usage Meter
        CPUUsageMeter(
            usage = cpuUsage,
            modifier = Modifier.fillMaxWidth()
        )

        // System info grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SystemInfoCard(
                label = "Buffer",
                value = "${bufferSize}",
                unit = "samples",
                modifier = Modifier.weight(1f)
            )
            
            SystemInfoCard(
                label = "Sample Rate",
                value = "${sampleRate / 1000}",
                unit = "kHz",
                modifier = Modifier.weight(1f)
            )
            
            SystemInfoCard(
                label = "Latency",
                value = String.format("%.1f", latency),
                unit = "ms",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * CPU usage meter with animated progress bar.
 */
@Composable
private fun CPUUsageMeter(
    usage: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CPU Usage",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(usage * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = when {
                    usage > 0.8f -> MaterialTheme.colorScheme.error
                    usage > 0.6f -> Orange
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }

        LinearProgressIndicator(
            progress = usage,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                usage > 0.8f -> MaterialTheme.colorScheme.error
                usage > 0.6f -> Orange
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

/**
 * System information card component.
 */
@Composable
private fun SystemInfoCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error display component with enhanced styling.
 */
@Composable
private fun ErrorDisplay(error: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Professional plugin chain section with drag-and-drop reordering.
 */
@Composable
private fun ProfessionalPluginChainSection(
    appState: AppState,
    chainedPlugins: List<ChainedPlugin>,
    selectedPluginIndex: Int?,
    showPluginEditor: Boolean,
    modifier: Modifier = Modifier,
    onPluginReorder: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with chain info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plugin Chain",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                ChainInfoChip(
                    pluginCount = chainedPlugins.size,
                    activeCount = chainedPlugins.count { !it.isBypassed }
                )
            }

            // Plugin chain content
            if (chainedPlugins.isEmpty()) {
                EmptyChainPlaceholder(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = chainedPlugins,
                        key = { _, plugin -> plugin.pluginInstanceId }
                    ) { index, plugin ->
                        ProfessionalPluginChainItem(
                            plugin = plugin,
                            index = index,
                            isSelected = selectedPluginIndex == index,
                            onSelect = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                appState.selectPlugin(index) 
                            },
                            onRemove = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                appState.removePluginFromChain(index) 
                            },
                            onToggleBypass = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                appState.togglePluginBypass(index) 
                            },
                            onEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                appState.setShowPluginEditor(true)
                            },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 300)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chain information chip showing plugin counts.
 */
@Composable
private fun ChainInfoChip(
    pluginCount: Int,
    activeCount: Int
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Text(
            text = "$activeCount/$pluginCount active",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Empty chain placeholder with helpful instructions.
 */
@Composable
private fun EmptyChainPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Text(
                text = "No plugins in chain",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Add plugins to start building your audio processing chain",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Professional plugin chain item with enhanced controls and animations.
 */
@Composable
private fun ProfessionalPluginChainItem(
    plugin: ChainedPlugin,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onToggleBypass: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "elevation"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary)
            ) { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main plugin info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = plugin.pluginName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusIndicator(
                            isActive = !plugin.isBypassed,
                            cpuUsage = plugin.cpuUsage
                        )
                        
                        Text(
                            text = plugin.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PluginControlButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit Parameters",
                        onClick = onEdit,
                        enabled = !plugin.isBypassed
                    )
                    
                    PluginControlButton(
                        icon = if (plugin.isBypassed) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (plugin.isBypassed) "Enable" else "Bypass",
                        onClick = onToggleBypass,
                        tint = if (plugin.isBypassed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    
                    PluginControlButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Remove Plugin",
                        onClick = onRemove,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Parameter preview (if selected)
            if (isSelected && plugin.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ParameterPreview(
                    parameters = plugin.parameters.take(3),
                    onParameterChange = { paramId, value ->
                        // Handle parameter change
                    }
                )
            }
        }
    }
}

/**
 * Status indicator for plugin state and CPU usage.
 */
@Composable
private fun StatusIndicator(
    isActive: Boolean,
    cpuUsage: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.Green else MaterialTheme.colorScheme.outline
                )
        )
        
        Text(
            text = if (isActive) "Active" else "Bypassed",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        
        if (isActive && cpuUsage > 0) {
            Text(
                text = "• ${cpuUsage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Plugin control button component.
 */
@Composable
private fun PluginControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Parameter preview component for selected plugins.
 */
@Composable
private fun ParameterPreview(
    parameters: List<PluginParameter>,
    onParameterChange: (String, Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Controls",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            parameters.forEach { param ->
                ParameterSlider(
                    parameter = param,
                    onValueChange = { value ->
                        onParameterChange(param.id, value)
                    }
                )
            }
        }
    }
}

/**
 * Parameter slider component.
 */
@Composable
private fun ParameterSlider(
    parameter: PluginParameter,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = parameter.name,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = parameter.displayValue,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        Slider(
            value = parameter.value,
            onValueChange = onValueChange,
            valueRange = parameter.minValue..parameter.maxValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Enhanced add plugin section with quick add options.
 */
@Composable
private fun EnhancedAddPluginSection(
    onAddPlugin: () -> Unit,
    onQuickAdd: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main add button
        Button(
            onClick = onAddPlugin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Browse All Plugins",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Quick add options
        Text(
            text = "Quick Add",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("EQ", "Compressor", "Reverb", "Delay", "Distortion")) { category ->
                QuickAddChip(
                    category = category,
                    onClick = { onQuickAdd(category) }
                )
            }
        }
    }
}

/**
 * Quick add chip for plugin categories.
 */
@Composable
private fun QuickAddChip(
    category: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Professional plugin browser dialog with advanced filtering and search.
 */
@Composable
private fun ProfessionalPluginBrowserDialog(
    appState: AppState,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Name") }
    
    val filteredPlugins = remember(searchQuery, selectedCategory, sortBy) {
        appState.availablePlugins.value
            .filter { plugin ->
                (selectedCategory == "All" || plugin.category == selectedCategory) &&
                (searchQuery.isEmpty() || plugin.pluginName.contains(searchQuery, ignoreCase = true))
            }
            .sortedBy { plugin ->
                when (sortBy) {
                    "Name" -> plugin.pluginName
                    "Category" -> plugin.category
                    "Manufacturer" -> plugin.manufacturer
                    else -> plugin.pluginName
                }
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plugin Browser",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search and filters
                PluginBrowserFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                    categories = appState.availablePlugins.value.map { it.category }.distinct()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Plugin list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPlugins) { plugin ->
                        MainPluginBrowserItem(
                            plugin = plugin,
                            onSelect = {
                                appState.addPluginToChain(plugin)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredPlugins.size} plugins found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

/**
 * Plugin browser filters section.
 */
@Composable
private fun PluginBrowserFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    categories: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search plugins...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Category and sort filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    (listOf("All") + categories).forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onCategoryChange(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Sort dropdown
            var sortExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = sortBy,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sort by") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    listOf("Name", "Category", "Manufacturer").forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort) },
                            onClick = {
                                onSortByChange(sort)
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Plugin browser item component.
 */
@Composable
private fun MainPluginBrowserItem(
    plugin: PluginInfo,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = plugin.pluginName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Text(
                    text = plugin.manufacturer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = plugin.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    if (plugin.isInstrument) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Instrument",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.Add,
                contentDescription = "Add Plugin",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Enhanced settings screen with comprehensive options.
 */
@Composable
private fun EnhancedSettingsScreen(
    appState: AppState,
    onDismiss: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SettingsSection(title = "Audio Settings") {
                            AudioSettingsContent(appState = appState)
                        }
                    }
                    
                    item {
                        SettingsSection(title = "Interface Settings") {
                            InterfaceSettingsContent(appState = appState)
                        }
                    }
                    
                    item {
                        SettingsSection(title = "Performance Settings") {
                            PerformanceSettingsContent(appState = appState)
                        }
                    }
                    
                    item {
                        SettingsSection(title = "Advanced Settings") {
                            AdvancedSettingsContent(
                                appState = appState,
                                onResetOnboarding = onResetOnboarding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

/**
 * Settings section wrapper component.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            content()
        }
    }
}

/**
 * Audio settings content.
 */
@Composable
private fun AudioSettingsContent(appState: AppState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Buffer size setting
        SettingSlider(
            label = "Buffer Size",
            value = appState.bufferSize.value.toFloat(),
            valueRange = 64f..2048f,
            steps = 6,
            onValueChange = { appState.setBufferSize(it.toInt()) },
            valueFormatter = { "${it.toInt()} samples" }
        )
        
        // Sample rate setting
        SettingDropdown(
            label = "Sample Rate",
            value = "${appState.sampleRate.value} Hz",
            options = listOf("44100 Hz", "48000 Hz", "88200 Hz", "96000 Hz"),
            onValueChange = { value ->
                val sampleRate = value.replace(" Hz", "").toInt()
                appState.setSampleRate(sampleRate)
            }
        )
        
        // Audio device selection
        SettingDropdown(
            label = "Audio Device",
            value = appState.selectedAudioDevice.value,
            options = appState.availableAudioDevices.value,
            onValueChange = { appState.setAudioDevice(it) }
        )
    }
}

/**
 * Interface settings content.
 */
@Composable
private fun InterfaceSettingsContent(appState: AppState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingSwitch(
            label = "Dark Mode",
            checked = appState.isDarkMode.value,
            onCheckedChange = { appState.setDarkMode(it) }
        )
        
        SettingSwitch(
            label = "Show CPU Usage",
            checked = appState.showCpuUsage.value,
            onCheckedChange = { appState.setShowCpuUsage(it) }
        )
        
        SettingSwitch(
            label = "Haptic Feedback",
            checked = appState.hapticFeedbackEnabled.value,
            onCheckedChange = { appState.setHapticFeedback(it) }
        )
    }
}

/**
 * Performance settings content.
 */
@Composable
private fun PerformanceSettingsContent(appState: AppState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingSlider(
            label = "CPU Limit",
            value = appState.cpuLimit.value,
            valueRange = 50f..100f,
            onValueChange = { appState.setCpuLimit(it) },
            valueFormatter = { "${it.toInt()}%" }
        )
        
        SettingSwitch(
            label = "Auto-bypass on Overload",
            checked = appState.autoBypassOnOverload.value,
            onCheckedChange = { appState.setAutoBypassOnOverload(it) }
        )
        
        SettingSwitch(
            label = "Background Processing",
            checked = appState.backgroundProcessing.value,
            onCheckedChange = { appState.setBackgroundProcessing(it) }
        )
    }
}

/**
 * Advanced settings content.
 */
@Composable
private fun AdvancedSettingsContent(
    appState: AppState,
    onResetOnboarding: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingButton(
            label = "Reset Onboarding",
            description = "Show the welcome tutorial again",
            onClick = onResetOnboarding
        )
        
        SettingButton(
            label = "Clear Plugin Cache",
            description = "Remove cached plugin data",
            onClick = { appState.clearPluginCache() }
        )
        
        SettingButton(
            label = "Export Settings",
            description = "Save current settings to file",
            onClick = { appState.exportSettings() }
        )
        
        SettingButton(
            label = "Import Settings",
            description = "Load settings from file",
            onClick = { appState.importSettings() }
        )
    }
}

/**
 * Setting slider component.
 */
@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { it.toString() }
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Setting dropdown component.
 */
@Composable
private fun SettingDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Setting switch component.
 */
@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Setting button component.
 */
@Composable
private fun SettingButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Plugin parameter editor overlay.
 */
@Composable
private fun PluginParameterEditor(
    plugin: ChainedPlugin,
    onDismiss: () -> Unit,
    onParameterChange: (String, Float) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = plugin.pluginName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Parameter Editor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Parameter controls
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(plugin.parameters.entries.toList()) { (portIndex, parameter) ->
                        ParameterSliderControl(
                            parameter = parameter,
                            onValueChange = { value ->
                                onParameterChange(portIndex.toString(), value)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            // Reset to defaults
                            plugin.parameters.forEach { (portIndex, param) ->
                                onParameterChange(portIndex.toString(), param.defaultValue)
                            }
                        }
                    ) {
                        Text("Reset to Defaults")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

/**
 * Parameter slider control component.
 */
@Composable
private fun ParameterSliderControl(
    parameter: ChainParameter,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = parameter.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = parameter.getDisplayValue(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Slider(
            value = parameter.value,
            onValueChange = onValueChange,
            valueRange = parameter.minValue..parameter.maxValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
