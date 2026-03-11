package com.example.lspandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lspandroid.audio.EqualizerSettings
import com.example.lspandroid.ui.theme.LSPAndroidTheme

class SimpleMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SimpleMainActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val modifyAudioGranted = permissions[Manifest.permission.MODIFY_AUDIO_SETTINGS] ?: false
        
        if (recordAudioGranted && modifyAudioGranted) {
            Log.d(TAG, "Audio permissions granted")
        } else {
            Log.w(TAG, "Audio permissions not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            LSPAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EqualizerScreen()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val recordAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        )
        val modifyAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        if (recordAudioPermission != PackageManager.PERMISSION_GRANTED ||
            modifyAudioPermission != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
            )
        }
    }
}

@Composable
fun EqualizerScreen() {
    var equalizerSettings by remember { mutableStateOf(EqualizerSettings.getDefaultSettings()) }
    var isAudioRunning by remember { mutableStateOf(false) }
    var masterVolume by remember { mutableStateOf(1.0f) }
    var isBypassed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LSP Audio Equalizer",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Equalizer bands
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Equalizer Bands",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            for (i in 0 until 10) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = equalizerSettings.getBandLabel(i),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Slider(
                        value = equalizerSettings.getBandGain(i),
                        onValueChange = { newValue ->
                            equalizerSettings = equalizerSettings.withBandGain(i, newValue)
                        },
                        valueRange = -24f..24f,
                        modifier = Modifier.weight(2f)
                    )
                    
                    Text(
                        text = "${equalizerSettings.getBandGain(i).toInt()} dB",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Master volume
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Master Volume")
            Slider(
                value = masterVolume,
                onValueChange = { masterVolume = it },
                valueRange = 0f..2f,
                modifier = Modifier.weight(1f)
            )
            Text(text = "${(masterVolume * 100).toInt()}%")
        }

        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isAudioRunning = !isAudioRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAudioRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isAudioRunning) "Stop Audio" else "Start Audio")
            }

            Button(
                onClick = { isBypassed = !isBypassed }
            ) {
                Text(if (isBypassed) "Enable EQ" else "Bypass EQ")
            }

            Button(
                onClick = { equalizerSettings = equalizerSettings.reset() }
            ) {
                Text("Reset")
            }
        }

        // Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("Audio: ${if (isAudioRunning) "Running" else "Stopped"}")
                Text("Bypass: ${if (isBypassed) "Enabled" else "Disabled"}")
                Text("Volume: ${(masterVolume * 100).toInt()}%")
                Text("EQ Preset: ${equalizerSettings.name}")
            }
        }
    }
}