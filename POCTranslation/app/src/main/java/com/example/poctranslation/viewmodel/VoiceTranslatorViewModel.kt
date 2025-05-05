package com.example.poctranslation.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poctranslation.api.AzureTranslationService
import com.example.poctranslation.api.RetrofitClient
import com.example.poctranslation.api.TranslationResponse
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class VoiceTranslatorViewModel(application: Application) : ViewModel() {
    private val context = application.applicationContext
    //private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val languages = listOf("English", "German", "Spanish", "Malayalam", "Hindi")

    private val _sourceLanguage = MutableStateFlow(languages.first())
    val sourceLanguage: StateFlow<String> = _sourceLanguage

    private val _targetLanguage = MutableStateFlow(languages.last())
    val targetLanguage: StateFlow<String> = _targetLanguage

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cognitive.microsofttranslator.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val translationService = retrofit.create(AzureTranslationService::class.java)

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var currentTargetLanguageCode: String? = null

    private var audioManager: AudioManager? = null
    private var bluetoothHeadset: BluetoothDevice? = null

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText
    val speechConfig = SpeechConfig.fromSubscription("FUW9zvxOJIbjEgrBhvXoGVVsuOkB10QV3W69gsoZVTtMMMzvKRuZJQQJ99BBACGhslBXJ3w3AAAYACOGSJqg", "centralindia")
   private  val recognizer = com.microsoft.cognitiveservices.speech.SpeechRecognizer(speechConfig)
    private val _showBluetoothError = MutableStateFlow(false)
    val showBluetoothError: StateFlow<Boolean> = _showBluetoothError
    init {

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set the language based on the initial target language

            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
    }



    fun setSourceLanguage(language: String) {
        _sourceLanguage.value = language
    }

    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
    }
    fun clearBluetoothError() {
        _showBluetoothError.value = false
    }



    fun stopListening() {
        recognizer.stopContinuousRecognitionAsync()
        _isListening.value = false
    }

    override fun onCleared() {
        super.onCleared()
       // speechRecognizer.destroy()

    }


    fun translateText(fromLang: String, toLang: String, text: String) {
        val jsonBody = "[{\"Text\": \"$text\"}]"  // Manually construct JSON
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

        val call = RetrofitClient.instance.translateText(
            apiVersion = "3.0",
            fromLang = fromLang,
            toLang = toLang,
            requestBody = requestBody
        )
        call.enqueue(object : Callback<List<TranslationResponse>> {
            override fun onResponse(
                call: Call<List<TranslationResponse>>,
                response: Response<List<TranslationResponse>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.firstOrNull()?.translations?.forEach {
                        println("Translated text: ${it.text} (Language: ${it.to})")
                        _translatedText.value =it.text
                        speakTranslatedTextOverBluetooth(translatedText.value)
                    }
                } else {
                    println("Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<TranslationResponse>>, t: Throwable) {
                println("Failed: ${t.message}")
            }
        })

    }
    fun startSpeechToText() {
        recognizer.recognized.addEventListener { _, event ->
            if (event.result.reason == ResultReason.RecognizedSpeech) {
                _recognizedText.value = event.result.text
                viewModelScope.launch {
                    translateText(getLanguageCode(sourceLanguage.value),getLanguageCode(targetLanguage.value),recognizedText.value)

                }
            }
        }

        recognizer.startContinuousRecognitionAsync()
        _isListening.value = true
    }
    fun toggleSpeechToText() {
        if (_isListening.value) {
            stopListening()
        } else {
            startSpeechToText()
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> "en"
            "Spanish" -> "es"
            "German" -> "de"
            "Hindi" -> "hi"
            "Malayalam" -> "ml"
            else -> "en"
        }
    }
    @SuppressLint("MissingPermission")
    fun speakTranslatedTextOverBluetooth(text: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Bluetooth", "Missing BLUETOOTH_CONNECT permission!")
            return
        }

        // First: Check A2DP connection (Bluetooth speaker)
        val isA2dpOn = audioManager.isBluetoothA2dpOn

        if (isA2dpOn) {
            Log.d("Bluetooth", "Bluetooth speaker connected. Using media audio.")
            speakWithTTS(text, AudioAttributes.USAGE_MEDIA)
            return
        }

        // Next: Check SCO connection (Bluetooth headset)
        bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HEADSET) {
                    val bluetoothHeadset = proxy as BluetoothHeadset?
                    val connectedDevices = bluetoothHeadset?.connectedDevices ?: emptyList()

                    if (connectedDevices.isNotEmpty()) {
                        Log.d("Bluetooth", "Bluetooth headset is connected.")

                        // Route through SCO (headset)
                        audioManager.mode = AudioManager.MODE_IN_CALL
                        audioManager.startBluetoothSco()
                        audioManager.isBluetoothScoOn = true

                        speakWithTTS(text, AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    } else {
                        Log.e("Bluetooth", "No Bluetooth headset connected!")
                        speakWithTTS(text, AudioAttributes.USAGE_MEDIA)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.d("Bluetooth", "Bluetooth headset disconnected.")
            }
        }, BluetoothProfile.HEADSET)
    }

    private fun speakWithTTS(text: String, usage: Int) {
        if (tts == null) {
            Log.e("TTS", "TTS not initialized")
            return
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isBelowAndroid12 = Build.VERSION.SDK_INT < Build.VERSION_CODES.S

        if (isBelowAndroid12) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        }

        // Set language for TTS (You can change this if needed based on the target language)
        tts?.language = Locale.US

        // Set AudioAttributes based on usage
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        tts?.setAudioAttributes(audioAttributes)

        // Speak the text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")


    }


    fun swapLanguages() {
        val currentSource = _sourceLanguage.value
        val currentTarget = _targetLanguage.value
        _sourceLanguage.value = currentTarget
        _targetLanguage.value = currentSource
    }

}