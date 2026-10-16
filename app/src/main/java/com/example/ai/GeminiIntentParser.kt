package com.example.ai

import com.example.BuildConfig
import com.example.model.JarvisAction
import com.example.model.NavType
import com.example.model.ScrollDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiIntentParser {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun parseUserCommand(userText: String, installedAppsHint: List<String> = emptyList()): ParsedIntent =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // No valid API key configured yet, use sophisticated local engine
                return@withContext LocalIntentEngine.parse(userText)
            }

            try {
                val appsContext = if (installedAppsHint.isNotEmpty()) {
                    "Installed apps on phone: ${installedAppsHint.take(30).joinToString(", ")}"
                } else ""

                val systemPrompt = """
                    You are JARVIS CORE, a voice-controlled personal Android assistant.
                    You understand Hindi, Hinglish, and English voice commands and map them to phone actions.
                    $appsContext

                    Always return a valid JSON object with NO MARKDOWN formatting, following this exact schema:
                    {
                      "action_type": "OPEN_APP" | "OPEN_SETTINGS" | "SYSTEM_NAV" | "SCREEN_CLICK" | "SCREEN_SCROLL" | "SCREEN_INPUT" | "CALL_PHONE" | "SEND_SMS" | "WEB_SEARCH" | "CLARIFY" | "CHAT_RESPONSE",
                      "parameters": {
                         "app_name": "name of application",
                         "setting_type": "wifi" | "bluetooth" | "display" | "sound" | "battery" | "accessibility" | "apps" | "general",
                         "nav_type": "HOME" | "BACK" | "RECENTS" | "NOTIFICATIONS" | "QUICK_SETTINGS" | "LOCK",
                         "target_text": "text or button label on screen",
                         "direction": "UP" | "DOWN",
                         "text": "text to type",
                         "contact": "name or number",
                         "message": "message content",
                         "query": "search query",
                         "question": "clarifying question if ambiguous"
                      },
                      "speech_response": "Polite, crisp response in Hindi or Hinglish matching user language (e.g. 'Ji Sir, YouTube open kar raha hoon')",
                      "is_sensitive": boolean (true ONLY for placing calls, sending messages, deleting data, purchases, or altering sensitive settings),
                      "confirmation_prompt": "Confirmation question shown to user before execution if is_sensitive is true, else null"
                    }
                """.trimIndent()

                val rootJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val turn = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", "User voice command: \"$userText\"") })
                            }
                            put("parts", parts)
                        }
                        put(turn)
                    }
                    put("contents", contentsArray)

                    val systemInstruction = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemPrompt) })
                        }
                        put("parts", parts)
                    }
                    put("systemInstruction", systemInstruction)

                    val generationConfig = JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.2)
                    }
                    put("generationConfig", generationConfig)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = rootJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    return@withContext LocalIntentEngine.parse(userText)
                }

                val parsedRoot = JSONObject(responseBody)
                val candidates = parsedRoot.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text")

                if (rawText.isNullOrBlank()) {
                    return@withContext LocalIntentEngine.parse(userText)
                }

                // Extract clean JSON
                val cleanedJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)

                val actionType = json.optString("action_type")
                val params = json.optJSONObject("parameters") ?: JSONObject()
                val speech = json.optString("speech_response", "Command process kar raha hoon.")
                val isSensitive = json.optBoolean("is_sensitive", false)
                val confirmPrompt = if (json.has("confirmation_prompt") && !json.isNull("confirmation_prompt")) {
                    json.optString("confirmation_prompt")
                } else null

                val action = when (actionType) {
                    "OPEN_APP" -> JarvisAction.OpenApp(params.optString("app_name", "app"))
                    "OPEN_SETTINGS" -> JarvisAction.OpenSettings(params.optString("setting_type", "general"))
                    "SYSTEM_NAV" -> {
                        val navStr = params.optString("nav_type", "HOME").uppercase()
                        val nav = try {
                            NavType.valueOf(navStr)
                        } catch (e: Exception) {
                            NavType.HOME
                        }
                        JarvisAction.SystemNav(nav)
                    }
                    "SCREEN_CLICK" -> JarvisAction.ScreenClick(params.optString("target_text", ""))
                    "SCREEN_SCROLL" -> {
                        val dirStr = params.optString("direction", "DOWN").uppercase()
                        val dir = if (dirStr == "UP") ScrollDirection.UP else ScrollDirection.DOWN
                        JarvisAction.ScreenScroll(dir)
                    }
                    "SCREEN_INPUT" -> JarvisAction.ScreenInput(params.optString("text", ""))
                    "CALL_PHONE" -> JarvisAction.MakeCall(params.optString("contact", ""))
                    "SEND_SMS" -> JarvisAction.SendSms(params.optString("contact", ""), params.optString("message", ""))
                    "WEB_SEARCH" -> JarvisAction.WebSearch(params.optString("query", userText))
                    "CLARIFY" -> JarvisAction.Clarify(params.optString("question", "Sir, kya aap repeat kar sakte hain?"))
                    else -> JarvisAction.ChatResponse(speech)
                }

                ParsedIntent(
                    action = action,
                    speechResponse = speech,
                    isSensitive = isSensitive,
                    confirmationPrompt = confirmPrompt
                )
            } catch (e: Exception) {
                e.printStackTrace()
                LocalIntentEngine.parse(userText)
            }
        }
}
