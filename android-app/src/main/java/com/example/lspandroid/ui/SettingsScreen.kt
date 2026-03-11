package com.example.lspandroid.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Settings screen with comprehensive app configuration options.
 * Includes onboarding reset, haptic feedback, audio preferences, and system settings.
 * 
 * Requirement 27.5: Settings toggle to reset onboarding
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Preferences
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var hapticEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_enabled", true)) }
    var audioLatency by remember { mutableStateOf(prefs.getInt("audio_latency", 128)) }
    var sampleRate by remember { mutableStateOf(prefs.getInt("sample_rate", 44100)) }
    var bufferSize by remember { mutableStateOf(prefs.getInt("buffer_size", 512)) }
    var enableOpenSL by remember { mutableStateOf(prefs.getBoolean("enable_opensl", false)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var autoSave by remember { mutableStateOf(prefs.getBoolean("auto_save", true)) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Dialog states
    var showResetDialog by remember { mutableStateOf(false) }
    var showLatencyDialog by remember { mutableStateOf(false) }
    var showSampleRateDialog by remember { mutableStateOf(false) }
    var showBufferSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
        )
                )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
            Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Experience Section
            SettingsSection(
                title = "User Experience",
                icon = Icons.Default.Settings
            ) {
                SettingsToggleItem(
                    title = "Haptic Feedback",
                    description = "Vibrate when adjusting controls and interacting with UI elements",
                    checked = hapticEnabled,
                    onCheckedChange = { enabled ->
                        hapticEnabled = enabled
                        prefs.edit().putBoolean("haptic_enabled", enabled).apply()
                        if (enabled) {
                            HapticFeedback.performControlAdjustFeedback(context)
    }
}
            )
                
                SettingsToggleItem(
                    title = "Dark Mode",
                    description = "Use dark theme for better visibility in low light",
                    checked = darkMode,
                    onCheckedChange = { enabled ->
                        darkMode = enabled
                        prefs.edit().putBoolean("dark_mode", enabled).apply()
                        scope.launch {
                            snackbarHostState.showSnackbar("Theme will change on app restart")
                        }
                    }
                )
                
                SettingsToggleItem(
                    title = "Auto Save",
                    description = "Automatically save plugin settings and presets",
                    checked = autoSave,
                    onCheckedChange = { enabled ->
                        autoSave = enabled
                        prefs.edit().putBoolean("auto_save", enabled).apply()
        }
            )
                
                SettingsItem(
                    title = "Reset Onboarding",
                    description = "Show the welcome tutorial again on next app launch",
                    icon = Icons.Default.Refresh,
                    onClick = { showResetDialog = true }
                )
        }

            // Audio Configuration Section
            SettingsSection(
                title = "Audio Configuration",
                icon = Icons.Default.Settings
            ) {
                SettingsItem(
                    title = "Audio Latency",
                    description = "${audioLatency}ms - Lower values reduce delay but may cause audio dropouts",
                    onClick = { showLatencyDialog = true }
        )
                
                SettingsItem(
                    title = "Sample Rate",
                    description = "${sampleRate}Hz - Higher rates improve quality but use more CPU",
                    onClick = { showSampleRateDialog = true }
            )
                
                SettingsItem(
                    title = "Buffer Size",
                    description = "${bufferSize} samples - Smaller buffers reduce latency",
                    onClick = { showBufferSizeDialog = true }
                )
                
                SettingsToggleItem(
                    title = "OpenSL ES Audio",
                    description = "Use low-latency audio driver (experimental, may cause instability)",
                    checked = enableOpenSL,
                    onCheckedChange = { enabled ->
                        enableOpenSL = enabled
                        prefs.edit().putBoolean("enable_opensl", enabled).apply()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (enabled) "OpenSL ES enabled - restart app to apply"
                                else "OpenSL ES disabled - restart app to apply"
            )
        }
    }
                )
}
            // Advanced Settings Section
            SettingsSection(
                title = "Advanced",
                icon = Icons.Default.Settings
            ) {
                SettingsToggleItem(
                    title = "Show Advanced Options",
                    description = "Display developer and debugging options",
                    checked = showAdvanced,
                    onCheckedChange = { showAdvanced = it }
                )
                
                if (showAdvanced) {
                    SettingsItem(
                        title = "Clear Cache",
                        description = "Remove temporary files and cached data",
                        onClick = {
                            context.cacheDir.deleteRecursively()
                            scope.launch {
                                snackbarHostState.showSnackbar("Cache cleared successfully")
                            }
                        }
                    )
                    
                    SettingsItem(
                        title = "Reset All Settings",
                        description = "Restore all settings to default values",
                        onClick = {
                            prefs.edit().clear().apply()
                            scope.launch {
                                snackbarHostState.showSnackbar("Settings reset - restart app to apply")
                            }
                        }
                    )
                    
                    SettingsItem(
                        title = "Export Settings",
                        description = "Save current configuration to file",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Export feature coming soon")
                            }
                        }
                    )
                }
            }

            // About Section
            SettingsSection(
                title = "About",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "App Version",
                    description = "1.2.3 (Build 456) - Latest stable release",
                    onClick = { }
                )

                SettingsItem(
                    title = "LSP Plugins",
                    description = "Professional open source audio plugins by Vladimir Sadovnikov",
                    onClick = {
                        uriHandler.openUri("https://lsp-plug.in/")
                    }
                )

                SettingsItem(
                    title = "Source Code",
                    description = "View project on GitHub - contributions welcome",
                    onClick = {
                        uriHandler.openUri("https://github.com/lsp-plugins/lsp-plugins")
                    }
                )

                SettingsItem(
                    title = "License",
                    description = "GNU General Public License v3.0 - Free and open source",
                    onClick = {
                        uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                )
                
                SettingsItem(
                    title = "Privacy Policy",
                    description = "How we handle your data and privacy",
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("No data is collected or transmitted")
                        }
                    }
                )
            }
        }
    }

    // Reset Onboarding Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Onboarding Tutorial?") },
            text = {
                Text("This will show the welcome tutorial and feature introduction again the next time you open the app. Your settings and presets will not be affected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetOnboarding()
                        showResetDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Onboarding will show on next app launch")
                        }
                    }
                ) {
                    Text("Reset Tutorial")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Latency Dialog
    if (showLatencyDialog) {
        val latencyOptions = listOf(32, 64, 128, 256, 512, 1024)
        AlertDialog(
            onDismissRequest = { showLatencyDialog = false },
            title = { Text("Select Audio Latency") },
            text = {
                Column {
                    Text("Lower latency reduces delay but may cause audio dropouts on slower devices.")
                    Spacer(modifier = Modifier.height(16.dp))
                    latencyOptions.forEach { latency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    audioLatency = latency
                                    prefs.edit().putInt("audio_latency", latency).apply()
                                    showLatencyDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = audioLatency == latency,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${latency}ms")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLatencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Sample Rate Dialog
    if (showSampleRateDialog) {
        val sampleRateOptions = listOf(22050, 44100, 48000, 88200, 96000)
        AlertDialog(
            onDismissRequest = { showSampleRateDialog = false },
            title = { Text("Select Sample Rate") },
            text = {
                Column {
                    Text("Higher sample rates improve audio quality but use more CPU and memory.")
                    Spacer(modifier = Modifier.height(16.dp))
                    sampleRateOptions.forEach { rate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sampleRate = rate
                                    prefs.edit().putInt("sample_rate", rate).apply()
                                    showSampleRateDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sampleRate == rate,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${rate}Hz")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSampleRateDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Buffer Size Dialog
    if (showBufferSizeDialog) {
        val bufferSizeOptions = listOf(128, 256, 512, 1024, 2048, 4096)
        AlertDialog(
            onDismissRequest = { showBufferSizeDialog = false },
            title = { Text("Select Buffer Size") },
            text = {
                Column {
                    Text("Smaller buffers reduce latency but may cause audio dropouts. Larger buffers are more stable.")
                    Spacer(modifier = Modifier.height(16.dp))
                    bufferSizeOptions.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    bufferSize = size
                                    prefs.edit().putInt("buffer_size", size).apply()
                                    showBufferSizeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = bufferSize == size,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$size samples")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBufferSizeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Settings section with title and icon.
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

/**
 * Settings item with title, description, and optional icon.
 */
@Composable
private fun SettingsItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Configure",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .rotate(180f)
        )
    }
}

/**
 * Settings item with toggle switch.
 */
@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

/**
 * Comprehensive haptic feedback utility for audio control applications.
 * Provides different feedback patterns for various user interactions.
 * 
 * Requirement 16.4: Haptic feedback when controls are adjusted
 */
object HapticFeedback {

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun isHapticEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("haptic_enabled", true)
    }

    /**
     * Triggers subtle haptic feedback for fine control adjustments.
     * Used for knobs, sliders, and continuous parameter changes.
     * 
     * @param context Android context
     * @param intensity Intensity of the haptic feedback (0.0 to 1.0)
     */
    fun performControlAdjustFeedback(context: Context, intensity: Float = 0.3f) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)
        val clampedIntensity = intensity.coerceIn(0f, 1f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                8,
                (VibrationEffect.DEFAULT_AMPLITUDE * clampedIntensity * 0.6f).toInt().coerceIn(1, 255)
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(8)
        }
    }

    /**
     * Triggers haptic feedback for precision mode activation.
     * Double pulse pattern to indicate mode change.
     * 
     * @param context Android context
     */
    fun performPrecisionModeFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 25, 40, 25)
            val amplitudes = intArrayOf(0, 120, 0, 80)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 25, 40, 25), -1)
        }
    }

    /**
     * Triggers haptic feedback for value reset or snap-to-default.
     * Strong single pulse to confirm reset action.
     * 
     * @param context Android context
     */
    fun performResetFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                40,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    /**
     * Triggers haptic feedback for button presses and UI interactions.
     * Light tap feedback for general interface elements.
     * 
     * @param context Android context
     */
    fun performButtonFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                12,
                (VibrationEffect.DEFAULT_AMPLITUDE * 0.4f).toInt()
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(12)
        }
    }

    /**
     * Triggers haptic feedback for reaching parameter limits.
     * Sharp pulse to indicate boundary hit.
     * 
     * @param context Android context
     */
    fun performLimitFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                20,
                255
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    /**
     * Triggers haptic feedback for successful operations.
     * Pleasant confirmation pattern.
     * 
     * @param context Android context
     */
    fun performSuccessFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 15, 20, 30)
            val amplitudes = intArrayOf(0, 80, 0, 120)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 15, 20, 30), -1)
        }
    }

    /**
     * Triggers haptic feedback for error conditions.
     * Distinct error pattern to alert user.
     * 
     * @param context Android context
     */
    fun performErrorFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 30, 50, 30, 50)
            val amplitudes = intArrayOf(0, 200, 0, 150, 0, 200)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 30, 50, 30, 50), -1)
        }
    }

    /**
     * Triggers haptic feedback for preset or setting changes.
     * Medium pulse for mode/state transitions.
     * 
     * @param context Android context
     */
    fun performSelectionFeedback(context: Context) {
        if (!isHapticEnabled(context)) return
        
        val vibrator = getVibrator(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                30,
                (VibrationEffect.DEFAULT_AMPLITUDE * 0.8f).toInt()
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
}
