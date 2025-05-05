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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
            // HomeScreen()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
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
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        bluetoothPermissionsLauncher.launch(bluetoothPermissions)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Icon(
                        painter = painterResource(id = R.drawable.phonak_logo), // Your SVG/vector
                        contentDescription = "App Logo",
                        tint = Color.Unspecified, // Keeps original colors
                        modifier = Modifier
                            .height(32.dp)
                            .padding(start = 4.dp)
                    )
                },
                colors = topAppBarColors(
                    containerColor = Color(0xFF81BE2A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DropdownMenuBox(
                selectedValue = sourceLanguage,
                options = viewModel.languages,
                label = "Source Language",
                onValueChangedEvent = { viewModel.setSourceLanguage(it) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            IconButton(
                onClick = { viewModel.swapLanguages() },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap languages",
                    tint = Color(0xFF81BE2A)
                )
            }

            DropdownMenuBox(
                selectedValue = targetLanguage,
                options = viewModel.languages,
                label = "Target Language",
                onValueChangedEvent = { viewModel.setTargetLanguage(it) },
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Recognized Speech",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF696868)
                    ),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "Translated Text",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF696868)
                    ),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Text(
                    text = translatedText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81BE2A))
            ) {
                Text(if (isListening) "Stop Listening" else "Speak", color = Color.White)
            }
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
    val themeColor = Color(0xFF81BE2A)

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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = themeColor,
                cursorColor = themeColor
            ),
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
@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize()

    ) {
        // Background Image with opacity, shifted down
        Image(
            painter = painterResource(id = R.drawable.phonak_bg), // Replace with your image resource
            contentDescription = "Background Image",
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 50.dp), // Adjust this value to move the image down
            contentScale = ContentScale.Fit, // Adjust how the image scales
            alpha = 0.1f // Set opacity (0.0f is fully transparent, 1.0f is fully opaque)
        )
    }
}




