package com.example.model

enum class NavType {
    HOME,
    BACK,
    RECENTS,
    NOTIFICATIONS,
    QUICK_SETTINGS,
    LOCK
}

enum class ScrollDirection {
    UP,
    DOWN
}

sealed class JarvisAction {
    data class OpenApp(val appQuery: String, val packageName: String? = null) : JarvisAction()
    data class OpenSettings(val settingType: String) : JarvisAction()
    data class SystemNav(val navType: NavType) : JarvisAction()
    data class ScreenClick(val targetText: String) : JarvisAction()
    data class ScreenScroll(val direction: ScrollDirection) : JarvisAction()
    data class ScreenInput(val text: String) : JarvisAction()
    data class SendSms(val contact: String, val message: String) : JarvisAction()
    data class MakeCall(val contact: String) : JarvisAction()
    data class WebSearch(val query: String) : JarvisAction()
    data class Clarify(val question: String) : JarvisAction()
    data class ChatResponse(val responseText: String) : JarvisAction()
}

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val details: String? = null
)

data class PendingConfirmation(
    val action: JarvisAction,
    val title: String,
    val prompt: String,
    val isSensitive: Boolean = true
)
