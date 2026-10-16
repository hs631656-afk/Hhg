package com.example.phone

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import com.example.model.ExecutionResult

data class AppInfo(
    val appName: String,
    val packageName: String
)

class AppLauncher(private val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in packages) {
                // Keep launchable apps or system apps that have launch intents
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    val label = pm.getApplicationLabel(app).toString()
                    apps.add(AppInfo(appName = label, packageName = app.packageName))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return apps
    }

    fun launchAppByQuery(query: String): ExecutionResult {
        val trimmed = query.trim().lowercase()
        val pm = context.packageManager

        // Known quick aliases
        val directPackage = when {
            trimmed.contains("youtube") -> "com.google.android.youtube"
            trimmed.contains("chrome") -> "com.android.chrome"
            trimmed.contains("whatsapp") -> "com.whatsapp"
            trimmed.contains("maps") -> "com.google.android.apps.maps"
            trimmed.contains("gmail") -> "com.google.android.gm"
            trimmed.contains("play store") || trimmed.contains("playstore") -> "com.android.vending"
            else -> null
        }

        if (directPackage != null) {
            val intent = pm.getLaunchIntentForPackage(directPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return ExecutionResult(
                    success = true,
                    message = "Opened $query successfully",
                    details = directPackage
                )
            }
        }

        // Special system actions fallback
        when {
            trimmed.contains("camera") || trimmed.contains("photo") || trimmed.contains("kamera") -> {
                val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (cameraIntent.resolveActivity(pm) != null) {
                    context.startActivity(cameraIntent)
                    return ExecutionResult(success = true, message = "Opened Camera")
                }
            }
            trimmed.contains("alarm") || trimmed.contains("clock") || trimmed.contains("ghadi") -> {
                val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (alarmIntent.resolveActivity(pm) != null) {
                    context.startActivity(alarmIntent)
                    return ExecutionResult(success = true, message = "Opened Alarms/Clock")
                }
            }
            trimmed.contains("dialer") || trimmed.contains("phone") || trimmed.contains("call app") -> {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (dialIntent.resolveActivity(pm) != null) {
                    context.startActivity(dialIntent)
                    return ExecutionResult(success = true, message = "Opened Phone Dialer")
                }
            }
        }

        // Search through all installed applications
        val installed = getInstalledApps()
        // Exact match or contains match
        val matched = installed.firstOrNull { it.appName.lowercase() == trimmed }
            ?: installed.firstOrNull { it.appName.lowercase().contains(trimmed) }
            ?: installed.firstOrNull { trimmed.contains(it.appName.lowercase()) }

        if (matched != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ExecutionResult(
                    success = true,
                    message = "Opened ${matched.appName}",
                    details = matched.packageName
                )
            }
        }

        return ExecutionResult(
            success = false,
            message = "Application \"$query\" not found on device",
            details = "Total checked installed apps: ${installed.size}"
        )
    }

    fun openWebSearch(query: String): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(success = true, message = "Searching Google for: $query")
        } catch (e: Exception) {
            ExecutionResult(success = false, message = "Failed to launch browser: ${e.localizedMessage}")
        }
    }
}
