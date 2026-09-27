package com.example.aidrivencompetencyplatform.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.aidrivencompetencyplatform.model.NovaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class VoiceSpeechManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Interaction State Machine (Single Source of Truth for Voice / Character)
    private val _novaState = MutableStateFlow(NovaState.IDLE)
    val novaState: StateFlow<NovaState> = _novaState.asStateFlow()

    // Speech Recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    // Active Callback references for the current recognition session
    private var activeOnFinalResult: ((String) -> Unit)? = null
    private var activeOnPartialResult: ((String) -> Unit)? = null
    private var activeOnErrorCallback: ((String) -> Unit)? = null
    private var accumulatedSpokenText = ""
    private var isFinalResultDelivered = false

    // Text To Speech
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.language = Locale.getDefault()
                    }
                    // Configure natural, warm, friendly AI career companion voice parameters
                    textToSpeech?.setSpeechRate(0.95f)
                    textToSpeech?.setPitch(1.0f)
                    isTtsInitialized = true
                    setupTtsListener()
                } else {
                    isTtsInitialized = false
                    Log.w("VoiceSpeechManager", "TTS initialization returned status: $status")
                }
            }
        } catch (e: Exception) {
            isTtsInitialized = false
            Log.e("VoiceSpeechManager", "TTS initialization failed", e)
        }
    }

    private fun setupTtsListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = true
                    _currentUtteranceId.value = utteranceId
                    if (_novaState.value != NovaState.LISTENING) {
                        _novaState.value = NovaState.SPEAKING
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (_currentUtteranceId.value == utteranceId) {
                        _isSpeaking.value = false
                        _currentUtteranceId.value = null
                        if (_novaState.value == NovaState.SPEAKING) {
                            _novaState.value = NovaState.IDLE
                        }
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (_currentUtteranceId.value == utteranceId) {
                        _isSpeaking.value = false
                        _currentUtteranceId.value = null
                        if (_novaState.value == NovaState.SPEAKING) {
                            _novaState.value = NovaState.IDLE
                        }
                    }
                }
            }
        })
    }

    fun setNovaState(state: NovaState) {
        _novaState.value = state
    }

    fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Starts listening to user speech using Android's SpeechRecognizer.
     * Guaranteed to execute cleanly on the Main thread without race conditions.
     */
    fun startListening(
        onPartialResult: ((String) -> Unit)? = null,
        onFinalResult: ((String) -> Unit)? = null,
        onErrorCallback: ((String) -> Unit)? = null
    ) {
        mainHandler.post {
            try {
                // Interruption/Barge-in: Stop TTS if speaking
                if (_isSpeaking.value) {
                    stopSpeaking()
                }

                _novaState.value = NovaState.LISTENING
                _speechError.value = null
                _recognizedText.value = ""
                accumulatedSpokenText = ""
                isFinalResultDelivered = false
                activeOnPartialResult = onPartialResult
                activeOnFinalResult = onFinalResult
                activeOnErrorCallback = onErrorCallback

                if (!isRecognitionAvailable()) {
                    val msg = "Speech recognition service is not available on this device."
                    _speechError.value = msg
                    _isListening.value = false
                    _novaState.value = NovaState.IDLE
                    onErrorCallback?.invoke(msg)
                    return@post
                }

                // Safely destroy previous recognizer synchronously on Main Looper
                destroyRecognizerInternal()

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _speechError.value = null
                        _novaState.value = NovaState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        if (_isSpeaking.value) {
                            stopSpeaking()
                        }
                        _isListening.value = true
                        _novaState.value = NovaState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioRms.value = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioRms.value = 0f

                        // If we already collected speech before the error (e.g. timeout or no-match after speaking), deliver it!
                        val fallbackText = accumulatedSpokenText.trim()
                        if (fallbackText.isNotBlank() && !isFinalResultDelivered) {
                            deliverFinalResult(fallbackText)
                            return
                        }

                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap mic to try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to speak."
                            else -> "Speech error ($error)"
                        }

                        _speechError.value = errorMsg
                        if (_novaState.value == NovaState.LISTENING) {
                            _novaState.value = NovaState.IDLE
                        }
                        activeOnErrorCallback?.invoke(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioRms.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: accumulatedSpokenText.trim()

                        if (text.isNotBlank()) {
                            deliverFinalResult(text)
                        } else {
                            if (_novaState.value == NovaState.LISTENING) {
                                _novaState.value = NovaState.IDLE
                            }
                            val msg = "No speech detected. Please tap mic and speak clearly."
                            _speechError.value = msg
                            activeOnErrorCallback?.invoke(msg)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            accumulatedSpokenText = text
                            _recognizedText.value = text
                            activeOnPartialResult?.invoke(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Failed to start speech recognition", e)
                _isListening.value = false
                _novaState.value = NovaState.IDLE
                val msg = "Failed to initialize microphone: ${e.localizedMessage ?: "Unknown error"}"
                _speechError.value = msg
                onErrorCallback?.invoke(msg)
            }
        }
    }

    /**
     * User taps mic to finish speaking.
     * Stops audio capture and asks recognizer to produce final results on current buffer.
     * If no results arrive within 1.2s, falls back to accumulated partial transcription.
     */
    fun stopListeningAndProcess() {
        mainHandler.post {
            try {
                _isListening.value = false
                _audioRms.value = 0f
                speechRecognizer?.stopListening()

                // Fallback check in case onResults does not fire
                mainHandler.postDelayed({
                    if (!isFinalResultDelivered && accumulatedSpokenText.isNotBlank()) {
                        deliverFinalResult(accumulatedSpokenText)
                    }
                }, 1200)
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Error in stopListeningAndProcess", e)
                if (accumulatedSpokenText.isNotBlank()) {
                    deliverFinalResult(accumulatedSpokenText)
                }
            }
        }
    }

    /**
     * Cancels active recognition without processing.
     */
    fun stopListening() {
        mainHandler.post {
            destroyRecognizerInternal()
            _isListening.value = false
            _audioRms.value = 0f
            if (_novaState.value == NovaState.LISTENING) {
                _novaState.value = NovaState.IDLE
            }
        }
    }

    private fun deliverFinalResult(text: String) {
        if (isFinalResultDelivered) return
        isFinalResultDelivered = true
        _recognizedText.value = text
        _isListening.value = false
        _audioRms.value = 0f
        _novaState.value = NovaState.THINKING
        activeOnFinalResult?.invoke(text)
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("VoiceSpeechManager", "Error destroying speech recognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()) {
        if (text.isBlank()) return

        if (!isTtsInitialized || textToSpeech == null) {
            initTts()
        }

        val cleanText = text
            .replace(Regex("[#*\\-_`]{1,}"), "")
            .replace(Regex("[•·]"), "")
            .trim()

        try {
            if (_isSpeaking.value && _currentUtteranceId.value == utteranceId) {
                stopSpeaking()
                return
            }
            _novaState.value = NovaState.SPEAKING
            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            _isSpeaking.value = false
            _currentUtteranceId.value = null
            _novaState.value = NovaState.IDLE
            Log.e("VoiceSpeechManager", "TTS speak failed", e)
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e("VoiceSpeechManager", "TTS stop failed", e)
        } finally {
            _isSpeaking.value = false
            _currentUtteranceId.value = null
            if (_novaState.value == NovaState.SPEAKING) {
                _novaState.value = NovaState.IDLE
            }
        }
    }

    fun release() {
        stopListening()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceSpeechManager", "TTS shutdown failed", e)
        } finally {
            _novaState.value = NovaState.IDLE
        }
    }
}
