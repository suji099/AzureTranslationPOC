package com.example.poctranslation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poctranslation.ui.theme.POCTranslationTheme
import com.example.poctranslation.viewmodel.VoiceTranslatorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VoiceTranslatorViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(VoiceTranslatorViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return VoiceTranslatorViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            })
            VoiceTranslatorApp(viewModel)
        }
    }
}
@Composable
fun VoiceTranslatorApp(viewModel: VoiceTranslatorViewModel) {
    val context = LocalContext.current
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val showBluetoothError by viewModel.showBluetoothError.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            //viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
        }
    }
    val bluetoothPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // All Bluetooth permissions granted
        } else {
            Toast.makeText(context, "Bluetooth permissions denied.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
        val bluetoothPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION, // Add this if needed
            Manifest.permission.ACCESS_COARSE_LOCATION // Add this if needed
        )
        bluetoothPermissionsLauncher.launch(bluetoothPermissions)

    }
    LaunchedEffect(translatedText) {

    }
    if (showBluetoothError) {
        AlertDialog(
            onDismissRequest = { viewModel.clearBluetoothError() },
            title = { Text("Bluetooth Headset Not Connected") },
            text = { Text("Please connect a Bluetooth headset to use this feature.") },
            confirmButton = {
                Button(onClick = { viewModel.clearBluetoothError() }) {
                    Text("OK")
                }
            }
        )
    }



    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Source Language Dropdown
        Text("Select Source Language")
        DropdownMenuBox(
            selectedValue = sourceLanguage,
            options = viewModel.languages,
            label = "Choose an option",
            onValueChangedEvent = { viewModel.setSourceLanguage(it) },
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Target Language Dropdown
        Text("Select Target Language")
        DropdownMenuBox(
            selectedValue = targetLanguage,
            options = viewModel.languages,
            label = "Choose an option",
            onValueChangedEvent = { viewModel.setTargetLanguage(it) },
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Recognized Speech Output
        Text("Recognized Speech:", style = MaterialTheme.typography.headlineLarge)
        Text(text = recognizedText, modifier = Modifier.padding(8.dp))
        Text("Translated  text:", style = MaterialTheme.typography.headlineLarge)
        Text(text = translatedText, modifier = Modifier.padding(8.dp))

        Spacer(modifier = Modifier.height(16.dp))



        Spacer(modifier = Modifier.weight(1f))


        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.toggleSpeechToText()
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }

            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)

        ) {
            Text(if (isListening) "Stop Listening" else "Speak")
        }

    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    selectedValue: String,
    options: List<String>,
    label: String,
    onValueChangedEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedValue,
            onValueChange = {},
            label = { Text(text = label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        expanded = false
                        onValueChangedEvent(option)
                    }
                )
            }
        }
    }
}