package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _soundRms = MutableStateFlow(0f)
    val soundRms: StateFlow<Float> = _soundRms.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    var onCommandRecognized: ((text: String, isWakeTriggered: Boolean) -> Unit)? = null

    private val wakeWords = listOf(
        "hey jarvis", "jarvis", "suno jarvis", "namaste jarvis",
        "hello jarvis", "ok jarvis", "jarvis suno", "arey jarvis"
    )

    fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _lastError.value = "Speech recognition is not available on this device"
            return
        }
        recreateRecognizer()
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _lastError.value = null
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _soundRms.value = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _soundRms.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _soundRms.value = 0f
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection required for speech service"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                        else -> "Speech recognition error ($error)"
                    }
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        _lastError.value = errorMessage
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _soundRms.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: return

                    _partialText.value = spoken
                    val (cleanedText, isWake) = checkWakeWord(spoken)
                    onCommandRecognized?.invoke(cleanedText, isWake)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        _partialText.value = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun checkWakeWord(rawText: String): Pair<String, Boolean> {
        val lower = rawText.lowercase(Locale.ROOT).trim()
        for (wake in wakeWords) {
            if (lower.startsWith(wake)) {
                val command = lower.removePrefix(wake).trim()
                return (if (command.isEmpty()) lower else command) to true
            }
            if (lower.contains(wake)) {
                val command = lower.replace(wake, "").trim()
                return (if (command.isEmpty()) lower else command) to true
            }
        }
        return rawText to false
    }

    fun startListening(languageCode: String = "en-IN") {
        if (speechRecognizer == null) {
            recreateRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            _partialText.value = ""
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _lastError.value = e.localizedMessage
            recreateRecognizer()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _soundRms.value = 0f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _isListening.value = false
            _soundRms.value = 0f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
