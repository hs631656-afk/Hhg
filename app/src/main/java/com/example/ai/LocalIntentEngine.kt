package com.example.ai

import com.example.model.JarvisAction
import com.example.model.NavType
import com.example.model.ScrollDirection

data class ParsedIntent(
    val action: JarvisAction,
    val speechResponse: String,
    val isSensitive: Boolean = false,
    val confirmationPrompt: String? = null
)

object LocalIntentEngine {

    fun parse(userText: String): ParsedIntent {
        val lower = userText.trim().lowercase()

        // 1. System Navigation
        if (lower.contains("home jao") || lower.contains("go home") || lower == "home" || lower.contains("home screen")) {
            return ParsedIntent(
                action = JarvisAction.SystemNav(NavType.HOME),
                speechResponse = "Home screen par jaa raha hoon, Sir."
            )
        }
        if (lower.contains("back jao") || lower.contains("go back") || lower == "back" || lower.contains("piche jao")) {
            return ParsedIntent(
                action = JarvisAction.SystemNav(NavType.BACK),
                speechResponse = "Back jaa raha hoon."
            )
        }
        if (lower.contains("recents") || lower.contains("recent apps") || lower.contains("overview")) {
            return ParsedIntent(
                action = JarvisAction.SystemNav(NavType.RECENTS),
                speechResponse = "Recent applications khol raha hoon."
            )
        }
        if (lower.contains("notification") || lower.contains("suchna")) {
            return ParsedIntent(
                action = JarvisAction.SystemNav(NavType.NOTIFICATIONS),
                speechResponse = "Notifications panel dikha raha hoon."
            )
        }
        if (lower.contains("quick setting") || lower.contains("control center")) {
            return ParsedIntent(
                action = JarvisAction.SystemNav(NavType.QUICK_SETTINGS),
                speechResponse = "Quick settings panel open kar raha hoon."
            )
        }
        if (lower.contains("lock screen") || lower.contains("phone lock karo") || lower.contains("screen band karo")) {
            return ParsedIntent(
                action = JarvisAction.SystemNav(NavType.LOCK),
                speechResponse = "Device lock kar raha hoon, Sir."
            )
        }

        // 2. Scrolling
        if (lower.contains("scroll down") || lower.contains("niche scroll") || lower.contains("niche jao")) {
            return ParsedIntent(
                action = JarvisAction.ScreenScroll(ScrollDirection.DOWN),
                speechResponse = "Niche scroll kar raha hoon."
            )
        }
        if (lower.contains("scroll up") || lower.contains("upar scroll") || lower.contains("upar jao")) {
            return ParsedIntent(
                action = JarvisAction.ScreenScroll(ScrollDirection.UP),
                speechResponse = "Upar scroll kar raha hoon."
            )
        }

        // 3. Screen Clicking & Tapping
        val clickPrefixes = listOf("click on ", "click ", "tap on ", "tap ", "par click karo", "button dabao")
        for (prefix in listOf("click on ", "click ", "tap on ", "tap ")) {
            if (lower.startsWith(prefix)) {
                val target = lower.removePrefix(prefix).trim()
                if (target.isNotEmpty()) {
                    return ParsedIntent(
                        action = JarvisAction.ScreenClick(target),
                        speechResponse = "\"$target\" par click karne ki koshish kar raha hoon."
                    )
                }
            }
        }
        if (lower.contains("par click karo") || lower.contains("pe click karo")) {
            val target = lower.substringBefore("par click").substringBefore("pe click").trim()
            if (target.isNotEmpty()) {
                return ParsedIntent(
                    action = JarvisAction.ScreenClick(target),
                    speechResponse = "\"$target\" par click kar raha hoon."
                )
            }
        }

        // 4. Text Input
        if (lower.startsWith("type ") || lower.startsWith("write ") || lower.startsWith("enter ")) {
            val textToType = userText.trim().substringAfter(" ").trim()
            return ParsedIntent(
                action = JarvisAction.ScreenInput(textToType),
                speechResponse = "Active field me type kar raha hoon: $textToType"
            )
        }
        if (lower.contains("likho") || lower.contains("type karo")) {
            val textToType = userText.substringBefore("likho").substringBefore("type karo").trim()
            if (textToType.isNotEmpty()) {
                return ParsedIntent(
                    action = JarvisAction.ScreenInput(textToType),
                    speechResponse = "\"$textToType\" type kar raha hoon."
                )
            }
        }

        // 5. Settings
        if (lower.contains("setting") || lower.contains("settings")) {
            val type = when {
                lower.contains("wifi") || lower.contains("wi-fi") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("display") || lower.contains("brightness") -> "display"
                lower.contains("sound") || lower.contains("volume") || lower.contains("audio") -> "sound"
                lower.contains("battery") -> "battery"
                lower.contains("accessibility") -> "accessibility"
                lower.contains("app") -> "apps"
                else -> "general"
            }
            return ParsedIntent(
                action = JarvisAction.OpenSettings(type),
                speechResponse = "$type settings open kar raha hoon, Sir."
            )
        }

        // 6. Sensitive actions (Call & SMS) -> Require explicit confirmation!
        if (lower.startsWith("call ") || lower.contains("ko call lagao") || lower.contains("ko call karo")) {
            val contact = if (lower.startsWith("call ")) {
                lower.removePrefix("call ").trim()
            } else {
                lower.substringBefore("ko call").trim()
            }
            return ParsedIntent(
                action = JarvisAction.MakeCall(contact),
                speechResponse = "Kya aap sach me $contact ko call lagana chahte hain?",
                isSensitive = true,
                confirmationPrompt = "Do you confirm placing a phone call to \"$contact\"?"
            )
        }

        if (lower.startsWith("message ") || lower.startsWith("send sms") || lower.contains("message bhejo")) {
            val parts = lower.removePrefix("message").removePrefix("send sms").trim()
            val contact = parts.substringBefore("ko").substringBefore(" ").trim().ifEmpty { "Recipient" }
            val message = parts.substringAfter("that").substringAfter("ko").trim().ifEmpty { "Hello from JARVIS" }
            return ParsedIntent(
                action = JarvisAction.SendSms(contact, message),
                speechResponse = "Message confirmation chahiye: $contact ko \"$message\" bhejna hai?",
                isSensitive = true,
                confirmationPrompt = "Confirm sending SMS to $contact:\n\"$message\""
            )
        }

        // 7. Web search
        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.contains("khojo") || lower.contains("search karo")) {
            val query = lower.removePrefix("search").removePrefix("google").replace("search karo", "").replace("khojo", "").trim()
            return ParsedIntent(
                action = JarvisAction.WebSearch(query),
                speechResponse = "\"$query\" ke liye Google search kar raha hoon."
            )
        }

        // 8. Open Apps
        val openWords = listOf("open ", "launch ", "start ", "kholo", "chalao", "dikhao", "on karo")
        for (w in listOf("open ", "launch ", "start ")) {
            if (lower.startsWith(w)) {
                val appName = lower.removePrefix(w).trim()
                if (appName.isNotEmpty()) {
                    return ParsedIntent(
                        action = JarvisAction.OpenApp(appName),
                        speechResponse = "$appName open kar raha hoon, Sir."
                    )
                }
            }
        }
        for (w in listOf("kholo", "chalao", "on karo")) {
            if (lower.endsWith(w)) {
                val appName = lower.removeSuffix(w).trim()
                if (appName.isNotEmpty()) {
                    return ParsedIntent(
                        action = JarvisAction.OpenApp(appName),
                        speechResponse = "$appName start kar raha hoon."
                    )
                }
            }
        }

        // 9. Conversational Chitchat
        if (lower.contains("kaise ho") || lower.contains("how are you")) {
            return ParsedIntent(
                action = JarvisAction.ChatResponse("Main bilkul theek hoon, Sir. Sabhi systems online hain. Aap bataiye main aapki kya madad kar sakta hoon?"),
                speechResponse = "Main bilkul theek hoon, Sir. Sabhi systems online hain. Aap bataiye main aapki kya madad kar sakta hoon?"
            )
        }
        if (lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("kya ho tum")) {
            return ParsedIntent(
                action = JarvisAction.ChatResponse("Main JARVIS hoon, aapka personal voice assistant. Main aapke phone ke apps khol sakta hoon, settings badal sakta hoon, aur screen par tap kar sakta hoon."),
                speechResponse = "Main JARVIS hoon, aapka personal voice assistant. Main apps khol sakta hoon, system navigate kar sakta hoon, aur screen automate kar sakta hoon."
            )
        }

        // Ambiguous command -> Ask clarifying question
        return ParsedIntent(
            action = JarvisAction.Clarify("Sir, kya aap kisi app ko kholna chahte hain ya Google search karna chahte hain? Kripya bataiye."),
            speechResponse = "Sir, kya aap kisi app ko kholna chahte hain ya Google search karna chahte hain? Kripya bataiye."
        )
    }
}
