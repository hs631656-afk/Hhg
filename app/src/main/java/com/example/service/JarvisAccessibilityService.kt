package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.ExecutionResult
import com.example.model.NavType
import com.example.model.ScrollDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _serviceConnectedFlow.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Observes accessibility events without logging sensitive data
    }

    override fun onInterrupt() {
        // Handle accessibility interrupt
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
            _serviceConnectedFlow.value = false
        }
    }

    /**
     * Executes global system navigation (Back, Home, Recents, Notifications, Quick Settings, Lock Screen)
     */
    fun performNavigation(navType: NavType): ExecutionResult {
        val action = when (navType) {
            NavType.HOME -> GLOBAL_ACTION_HOME
            NavType.BACK -> GLOBAL_ACTION_BACK
            NavType.RECENTS -> GLOBAL_ACTION_RECENTS
            NavType.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
            NavType.QUICK_SETTINGS -> GLOBAL_ACTION_QUICK_SETTINGS
            NavType.LOCK -> GLOBAL_ACTION_LOCK_SCREEN
        }

        val success = performGlobalAction(action)
        return if (success) {
            ExecutionResult(
                success = true,
                message = "Executed ${navType.name.lowercase().replace('_', ' ')} action"
            )
        } else {
            ExecutionResult(
                success = false,
                message = "Failed to execute ${navType.name} global navigation"
            )
        }
    }

    /**
     * Taps on an element matching text, label, or content description.
     * Respects security barriers (never taps on password or biometric prompts).
     */
    fun clickElementByText(query: String): ExecutionResult {
        val root = rootInActiveWindow ?: return ExecutionResult(
            success = false,
            message = "Screen window content not accessible or screen is turned off"
        )

        // Safety check: Avoid interacting with system credential guards
        val packageName = root.packageName?.toString() ?: ""
        if (packageName.contains("keyguard") || packageName.contains("credentials") || packageName.contains("biometric")) {
            return ExecutionResult(
                success = false,
                message = "Action blocked: JARVIS will not interact with lock screen or authentication screens for your security."
            )
        }

        val matchingNodes = root.findAccessibilityNodeInfosByText(query)
        if (matchingNodes.isNullOrEmpty()) {
            return ExecutionResult(
                success = false,
                message = "No button or element with text \"$query\" found on screen"
            )
        }

        // Find the most appropriate clickable node
        for (node in matchingNodes) {
            // Check if node itself is a password field
            if (node.isPassword) {
                return ExecutionResult(
                    success = false,
                    message = "Security guard: Cannot interact with password fields"
                )
            }

            var target: AccessibilityNodeInfo? = node
            while (target != null && !target.isClickable) {
                target = target.parent
            }

            val finalTarget = target ?: node
            if (finalTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return ExecutionResult(
                    success = true,
                    message = "Tapped element \"$query\"",
                    details = "Class: ${finalTarget.className}"
                )
            }
        }

        return ExecutionResult(
            success = false,
            message = "Found element \"$query\" but could not execute click action"
        )
    }

    /**
     * Scrolls active window forward (down) or backward (up).
     */
    fun scrollActiveWindow(direction: ScrollDirection): ExecutionResult {
        val root = rootInActiveWindow ?: return ExecutionResult(
            success = false,
            message = "Screen window content not accessible"
        )

        val scrollAction = when (direction) {
            ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

        val scrollableNode = findScrollableNode(root)
        if (scrollableNode == null) {
            return ExecutionResult(
                success = false,
                message = "No scrollable list or view detected on current screen"
            )
        }

        val success = scrollableNode.performAction(scrollAction)
        return if (success) {
            ExecutionResult(
                success = true,
                message = "Scrolled ${direction.name.lowercase()} successfully"
            )
        } else {
            ExecutionResult(
                success = false,
                message = "Scroll action reached edge or was not accepted by the app view"
            )
        }
    }

    /**
     * Types text into currently focused editable input field.
     */
    fun inputTextIntoFocusedField(text: String): ExecutionResult {
        val root = rootInActiveWindow ?: return ExecutionResult(
            success = false,
            message = "Screen window content not accessible"
        )

        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(root)

        if (focusedNode == null) {
            return ExecutionResult(
                success = false,
                message = "No active text field or input box found on screen to type into"
            )
        }

        if (focusedNode.isPassword) {
            return ExecutionResult(
                success = false,
                message = "Security guard: Cannot input text into password fields"
            )
        }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        return if (success) {
            ExecutionResult(
                success = true,
                message = "Entered text: \"$text\""
            )
        } else {
            ExecutionResult(
                success = false,
                message = "Target view declined text insertion"
            )
        }
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val scrollable = findScrollableNode(child)
            if (scrollable != null) return scrollable
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val editable = findFirstEditableNode(child)
            if (editable != null) return editable
        }
        return null
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        private val _serviceConnectedFlow = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _serviceConnectedFlow.asStateFlow()

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(':').any {
                it.equals(expectedServiceName, ignoreCase = true) ||
                        it.contains(JarvisAccessibilityService::class.java.simpleName, ignoreCase = true)
            }
        }
    }
}
