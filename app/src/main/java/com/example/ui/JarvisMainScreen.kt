package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AccessibilityConsentDialog
import com.example.ui.components.ActionHistoryCard
import com.example.ui.components.HudOrbView
import com.example.ui.components.PrivacyDashboardSheet
import com.example.ui.components.SensitiveConfirmationDialog
import com.example.ui.components.WakeWordExplainerDialog
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.MicState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val languages = listOf("Hinglish", "Hindi", "English")

    val quickCommands = listOf(
        "YouTube kholo",
        "Wi-Fi setting kholo",
        "Home screen jao",
        "Recent apps dikhao",
        "Scroll down karo",
        "Call Amit",
        "Message bhejo Priya ko Hi",
        "Google search Space Station",
        "Kaise ho JARVIS?"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = JarvisCyanPrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, JarvisCyanPrimary),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "J",
                                    color = JarvisCyanPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "JARVIS CORE",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isAccessibilityEnabled) JarvisNeonGreen else JarvisAmber)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isAccessibilityEnabled) "SCREEN CONTROL READY" else "A11Y OFF",
                                    color = if (state.isAccessibilityEnabled) JarvisNeonGreen else JarvisAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Wake Word Explainer button
                    IconButton(
                        onClick = { viewModel.setWakeWordExplainerVisible(true) },
                        modifier = Modifier.testTag("wake_word_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Wake Word Architecture",
                            tint = JarvisCyanPrimary
                        )
                    }

                    // Privacy & Permissions Dashboard
                    IconButton(
                        onClick = { viewModel.setPrivacyDashboardVisible(true) },
                        modifier = Modifier.testTag("privacy_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Dashboard",
                            tint = if (state.hasMicPermission) JarvisCyanPrimary else JarvisAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisSurface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Language Selection Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOICE ENGINE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        languages.forEach { lang ->
                            FilterChip(
                                selected = state.activeLanguage == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisCyanPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = JarvisCardBg,
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(1.dp, if (state.activeLanguage == lang) JarvisCyanPrimary else Color(0xFF22304A))
                            )
                        }
                    }
                }
            }

            // Hero HUD Orb Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HudOrbView(
                        micState = state.micState,
                        soundRms = state.soundRms,
                        onClick = {
                            if (!state.hasMicPermission) {
                                onRequestMicPermission()
                            } else {
                                if (state.isListening) {
                                    viewModel.pauseListening()
                                } else {
                                    viewModel.startListening()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transcription / Response Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("ai_response_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = JarvisCardBg),
                        border = BorderStroke(1.dp, Color(0xFF22304A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // User Transcription (if any)
                            if (state.currentTranscription.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = JarvisCyanPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "YOU SAID:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisCyanPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = state.currentTranscription,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                                )
                            }

                            // AI Speech Response
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = JarvisNeonGreen.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = JarvisNeonGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "JARVIS RESPONSE:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisNeonGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (state.isSpeaking) {
                                    IconButton(
                                        onClick = { viewModel.stopSpeaking() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                            contentDescription = "Stop Speech",
                                            tint = JarvisAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = state.lastResponseText,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Voice Controls (Start, Pause, Stop)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!state.hasMicPermission) {
                                    onRequestMicPermission()
                                } else {
                                    viewModel.startListening()
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("start_listening_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.pauseListening() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pause_listening_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", color = TextSecondary)
                        }

                        OutlinedButton(
                            onClick = { viewModel.stopListening() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stop_listening_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Icon(imageVector = Icons.Default.MicOff, contentDescription = null, tint = JarvisRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", color = JarvisRed)
                        }
                    }
                }
            }

            // Quick Voice Command Simulator Chips
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = "TRY QUICK COMMANDS (HINDI & HINGLISH):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickCommands.forEach { cmd ->
                            Surface(
                                onClick = { viewModel.processSpokenCommand(cmd, isWake = false) },
                                shape = RoundedCornerShape(20.dp),
                                color = JarvisCardBg,
                                border = BorderStroke(1.dp, Color(0xFF22304A)),
                                modifier = Modifier.testTag("quick_cmd_${cmd.take(8)}")
                            ) {
                                Text(
                                    text = cmd,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = JarvisCyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Action History Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTION EXECUTION AUDIT LOG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyanPrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    if (state.history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear History",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Action History Items
            if (state.history.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = JarvisCardBg),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsAccessibility,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No actions executed yet",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap the glowing Arc Reactor or speak 'Hey JARVIS' to trigger voice commands.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(state.history, key = { it.id }) { historyItem ->
                    ActionHistoryCard(
                        item = historyItem,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet: Privacy & Security Dashboard
    if (state.showPrivacyDashboard) {
        PrivacyDashboardSheet(
            state = state,
            sheetState = sheetState,
            onDismiss = { viewModel.setPrivacyDashboardVisible(false) },
            onRequestMicPermission = onRequestMicPermission,
            onOpenAccessibilitySettings = {
                viewModel.setPrivacyDashboardVisible(false)
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    // Dialog: Wake-Word & Background Execution Architecture
    if (state.showWakeWordExplainer) {
        WakeWordExplainerDialog(
            onDismiss = { viewModel.setWakeWordExplainerVisible(false) }
        )
    }

    // Dialog: Accessibility Consent & Deep Link
    if (state.showAccessibilityConsentDialog) {
        AccessibilityConsentDialog(
            onDismiss = { viewModel.setAccessibilityConsentDialogVisible(false) },
            onOpenSettings = {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    // Dialog: Sensitive Operation User Confirmation
    state.pendingConfirmation?.let { confirmation ->
        SensitiveConfirmationDialog(
            confirmation = confirmation,
            onConfirm = { viewModel.confirmPendingAction() },
            onCancel = { viewModel.cancelPendingAction() }
        )
    }
}
