package com.example.phone

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.model.ExecutionResult

class SettingsLauncher(private val context: Context) {

    fun openSetting(settingType: String): ExecutionResult {
        val norm = settingType.trim().lowercase()

        val (action, label) = when {
            norm.contains("wifi") || norm.contains("wi-fi") || norm.contains("internet") -> {
                Settings.ACTION_WIFI_SETTINGS to "Wi-Fi Settings"
            }
            norm.contains("bluetooth") || norm.contains("bt") -> {
                Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth Settings"
            }
            norm.contains("display") || norm.contains("brightness") || norm.contains("screen") -> {
                Settings.ACTION_DISPLAY_SETTINGS to "Display Settings"
            }
            norm.contains("sound") || norm.contains("volume") || norm.contains("audio") || norm.contains("ring") -> {
                Settings.ACTION_SOUND_SETTINGS to "Sound & Vibration Settings"
            }
            norm.contains("battery") || norm.contains("power") -> {
                Settings.ACTION_BATTERY_SAVER_SETTINGS to "Battery Settings"
            }
            norm.contains("accessibility") -> {
                Settings.ACTION_ACCESSIBILITY_SETTINGS to "Accessibility Settings"
            }
            norm.contains("app") || norm.contains("application") -> {
                Settings.ACTION_APPLICATION_SETTINGS to "Installed Apps Settings"
            }
            norm.contains("date") || norm.contains("time") -> {
                Settings.ACTION_DATE_SETTINGS to "Date & Time Settings"
            }
            norm.contains("airplane") || norm.contains("flight") -> {
                Settings.ACTION_AIRPLANE_MODE_SETTINGS to "Airplane Mode Settings"
            }
            norm.contains("location") || norm.contains("gps") -> {
                Settings.ACTION_LOCATION_SOURCE_SETTINGS to "Location Settings"
            }
            else -> {
                Settings.ACTION_SETTINGS to "System Settings"
            }
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                message = "Opened $label",
                details = action
            )
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                message = "Could not open $label: ${e.localizedMessage}"
            )
        }
    }
}
