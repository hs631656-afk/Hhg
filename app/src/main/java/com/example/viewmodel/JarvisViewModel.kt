package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiIntentParser
import com.example.ai.ParsedIntent
import com.example.model.ActionHistoryItem
import com.example.model.ExecutionResult
import com.example.model.ExecutionStatus
import com.example.model.JarvisAction
import com.example.model.PendingConfirmation
import com.example.phone.AppLauncher
import com.example.phone.SettingsLauncher
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisVoiceService
import com.example.service.VoiceServiceState
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MicState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

data class JarvisUiState(
    val micState: MicState = MicState.IDLE,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val soundRms: Float = 0f,
    val currentTranscription: String = "",
    val lastResponseText: String = "JARVIS Online. Awaiting your voice command.",
    val pendingConfirmation: PendingConfirmation? = null,
    val isAccessibilityEnabled: Boolean = false,
    val isForegroundServiceRunning: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val hasMicPermission: Boolean = false,
    val history: List<ActionHistoryItem> = emptyList(),
    val showPrivacyDashboard: Boolean = false,
    val showWakeWordExplainer: Boolean = false,
    val showAccessibilityConsentDialog: Boolean = false,
    val activeLanguage: String = "Hinglish",
    val installedAppsCount: Int = 0
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val speechManager = SpeechManager(context)
    private val ttsManager = TtsManager(context)
    private val appLauncher = AppLauncher(context)
    private val settingsLauncher = SettingsLauncher(context)
    private val geminiParser = GeminiIntentParser()

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        speechManager.initialize()

        // Speech recognition callback
        speechManager.onCommandRecognized = { command, isWake ->
            processSpokenCommand(command, isWake)
        }

        // Monitor speech live states
        viewModelScope.launch {
            speechManager.isListening.collect { listening ->
                _uiState.update { current ->
                    current.copy(
                        isListening = listening,
                        micState = if (listening) MicState.LISTENING else if (current.isSpeaking) MicState.SPEAKING else MicState.IDLE
                    )
                }
            }
        }

        viewModelScope.launch {
            speechManager.partialText.collect { text ->
                if (text.isNotEmpty()) {
                    _uiState.update { it.copy(currentTranscription = text) }
                }
            }
        }

        viewModelScope.launch {
            speechManager.soundRms.collect { rms ->
                _uiState.update { it.copy(soundRms = rms) }
            }
        }

        // Monitor TTS speaking state
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                _uiState.update { current ->
                    current.copy(
                        isSpeaking = speaking,
                        micState = if (speaking) MicState.SPEAKING else if (current.isListening) MicState.LISTENING else MicState.IDLE
                    )
                }
            }
        }

        // Monitor Foreground Voice Service state
        viewModelScope.launch {
            JarvisVoiceService.serviceState.collect { state ->
                _uiState.update { it.copy(isForegroundServiceRunning = state != VoiceServiceState.STOPPED) }
            }
        }

        // Monitor Accessibility Service state
        viewModelScope.launch {
            JarvisAccessibilityService.isServiceConnected.collect { connected ->
                _uiState.update { it.copy(isAccessibilityEnabled = connected || JarvisAccessibilityService.isAccessibilityEnabled(context)) }
            }
        }

        refreshStatus()
    }

    fun refreshStatus() {
        val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val isA11y = JarvisAccessibilityService.instance != null || JarvisAccessibilityService.isAccessibilityEnabled(context)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true

        val appsCount = try {
            appLauncher.getInstalledApps().size
        } catch (e: Exception) { 0 }

        _uiState.update {
            it.copy(
                hasMicPermission = hasMic,
                isAccessibilityEnabled = isA11y,
                isBatteryOptimizationIgnored = isIgnoringBattery,
                installedAppsCount = appsCount
            )
        }
    }

    fun startListening() {
        if (!_uiState.value.hasMicPermission) return

        ttsManager.stop()
        val langCode = when (_uiState.value.activeLanguage) {
            "Hindi" -> "hi-IN"
            "English" -> "en-US"
            else -> "en-IN" // Hinglish / Indian English
        }

        speechManager.startListening(langCode)
        JarvisVoiceService.start(context)
    }

    fun pauseListening() {
        speechManager.stopListening()
        ttsManager.stop()
        JarvisVoiceService.pause(context)
        _uiState.update { it.copy(micState = MicState.IDLE) }
    }

    fun stopListening() {
        speechManager.cancel()
        ttsManager.stop()
        JarvisVoiceService.stop(context)
        _uiState.update { it.copy(micState = MicState.IDLE, currentTranscription = "") }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun processSpokenCommand(commandText: String, isWake: Boolean) {
        if (commandText.isBlank()) return

        _uiState.update {
            it.copy(
                currentTranscription = commandText,
                micState = MicState.PROCESSING
            )
        }

        viewModelScope.launch {
            val appsHint = try {
                appLauncher.getInstalledApps().map { it.appName }
            } catch (e: Exception) { emptyList() }

            val parsed: ParsedIntent = geminiParser.parseUserCommand(commandText, appsHint)

            if (parsed.isSensitive) {
                // Sensitive action: Prompt confirmation before executing!
                _uiState.update {
                    it.copy(
                        micState = MicState.IDLE,
                        lastResponseText = parsed.speechResponse,
                        pendingConfirmation = PendingConfirmation(
                            action = parsed.action,
                            title = "Security Confirmation Required",
                            prompt = parsed.confirmationPrompt ?: parsed.speechResponse,
                            isSensitive = true
                        )
                    )
                }
                ttsManager.speak(parsed.speechResponse)
                recordHistory(
                    userPrompt = commandText,
                    actionType = parsed.action::class.java.simpleName,
                    actionDetails = parsed.action.toString(),
                    status = ExecutionStatus.REQUIRES_CONFIRMATION,
                    feedback = "Awaiting user confirmation for sensitive action."
                )
            } else {
                _uiState.update {
                    it.copy(
                        lastResponseText = parsed.speechResponse,
                        micState = MicState.SPEAKING
                    )
                }
                ttsManager.speak(parsed.speechResponse)
                executeAction(commandText, parsed.action, parsed.speechResponse)
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingConfirmation ?: return
        _uiState.update { it.copy(pendingConfirmation = null) }
        executeAction("Confirmed action", pending.action, "Action confirmed. Executing now.")
    }

    fun cancelPendingAction() {
        val pending = _uiState.value.pendingConfirmation
        _uiState.update { it.copy(pendingConfirmation = null) }
        val msg = "Action cancelled by user."
        ttsManager.speak("Thik hai Sir, action cancel kar diya.")
        if (pending != null) {
            recordHistory(
                userPrompt = "Action cancelled",
                actionType = pending.action::class.java.simpleName,
                actionDetails = pending.action.toString(),
                status = ExecutionStatus.CANCELLED,
                feedback = msg
            )
        }
    }

    private fun executeAction(userPrompt: String, action: JarvisAction, speechReply: String) {
        val result: ExecutionResult = when (action) {
            is JarvisAction.OpenApp -> {
                appLauncher.launchAppByQuery(action.appQuery)
            }
            is JarvisAction.OpenSettings -> {
                settingsLauncher.openSetting(action.settingType)
            }
            is JarvisAction.SystemNav -> {
                val service = JarvisAccessibilityService.instance
                if (service != null) {
                    service.performNavigation(action.navType)
                } else {
                    _uiState.update { it.copy(showAccessibilityConsentDialog = true) }
                    ExecutionResult(
                        success = false,
                        message = "Accessibility service is required for system navigation. Please enable JARVIS in Accessibility Settings."
                    )
                }
            }
            is JarvisAction.ScreenClick -> {
                val service = JarvisAccessibilityService.instance
                if (service != null) {
                    service.clickElementByText(action.targetText)
                } else {
                    _uiState.update { it.copy(showAccessibilityConsentDialog = true) }
                    ExecutionResult(
                        success = false,
                        message = "Accessibility service required for screen clicks. Please enable JARVIS."
                    )
                }
            }
            is JarvisAction.ScreenScroll -> {
                val service = JarvisAccessibilityService.instance
                if (service != null) {
                    service.scrollActiveWindow(action.direction)
                } else {
                    _uiState.update { it.copy(showAccessibilityConsentDialog = true) }
                    ExecutionResult(
                        success = false,
                        message = "Accessibility service required for scrolling. Please enable JARVIS."
                    )
                }
            }
            is JarvisAction.ScreenInput -> {
                val service = JarvisAccessibilityService.instance
                if (service != null) {
                    service.inputTextIntoFocusedField(action.text)
                } else {
                    _uiState.update { it.copy(showAccessibilityConsentDialog = true) }
                    ExecutionResult(
                        success = false,
                        message = "Accessibility service required for typing text. Please enable JARVIS."
                    )
                }
            }
            is JarvisAction.MakeCall -> {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(action.contact)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                    ExecutionResult(success = true, message = "Opened dialer for ${action.contact}")
                } catch (e: Exception) {
                    ExecutionResult(success = false, message = "Could not open dialer: ${e.localizedMessage}")
                }
            }
            is JarvisAction.SendSms -> {
                try {
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(action.contact)}")).apply {
                        putExtra("sms_body", action.message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    ExecutionResult(success = true, message = "Opened SMS composer for ${action.contact}")
                } catch (e: Exception) {
                    ExecutionResult(success = false, message = "Could not open SMS: ${e.localizedMessage}")
                }
            }
            is JarvisAction.WebSearch -> {
                appLauncher.openWebSearch(action.query)
            }
            is JarvisAction.Clarify -> {
                ExecutionResult(success = true, message = "Asked user for clarification: ${action.question}")
            }
            is JarvisAction.ChatResponse -> {
                ExecutionResult(success = true, message = action.responseText)
            }
        }

        val status = if (result.success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED
        recordHistory(
            userPrompt = userPrompt,
            actionType = action::class.java.simpleName,
            actionDetails = action.toString(),
            status = status,
            feedback = result.message
        )
    }

    private fun recordHistory(
        userPrompt: String,
        actionType: String,
        actionDetails: String,
        status: ExecutionStatus,
        feedback: String
    ) {
        val item = ActionHistoryItem(
            userPrompt = userPrompt,
            language = _uiState.value.activeLanguage,
            actionType = actionType,
            actionDetails = actionDetails,
            executionStatus = status,
            feedbackMessage = feedback
        )
        _uiState.update { current ->
            current.copy(history = listOf(item) + current.history.take(49))
        }
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(activeLanguage = language) }
    }

    fun setPrivacyDashboardVisible(visible: Boolean) {
        _uiState.update { it.copy(showPrivacyDashboard = visible) }
    }

    fun setWakeWordExplainerVisible(visible: Boolean) {
        _uiState.update { it.copy(showWakeWordExplainer = visible) }
    }

    fun setAccessibilityConsentDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAccessibilityConsentDialog = visible) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
