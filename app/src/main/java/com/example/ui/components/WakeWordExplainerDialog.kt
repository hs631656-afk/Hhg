package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WakeWordExplainerDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("wake_word_explainer_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisCardBg),
            border = BorderStroke(1.dp, JarvisCyanPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = JarvisCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Wake-Word & Background Architecture",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Local Detection & Privacy
                TechDocCard(
                    title = "1. Local Wake-Word (\"Hey JARVIS\")",
                    badge = "PRIVACY PRESERVING",
                    content = "Local on-device wake-word detection (Keyword Spotting / KWS) scans a circular audio buffer entirely on the device CPU/NPU without sending raw microphone audio to remote cloud servers. Cloud speech recognition or Gemini is only contacted after the wake keyword (\"Hey JARVIS\", \"Suno JARVIS\") is triggered."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Model Requirements
                TechDocCard(
                    title = "2. Wake-Word Model Requirements",
                    badge = "EMBEDDED ML",
                    content = "• Architecture: Quantized CNN / Streaming Conformer or Depthwise Separable RNN (e.g. Porcupine, TensorFlow Lite Micro, or Vosk).\n• Footprint: Model size < 500 KB and RAM usage < 2 MB to run within real-time battery constraints.\n• Audio Features: 16kHz 16-bit mono PCM converted into 40-dimensional Mel-Frequency Cepstral Coefficients (MFCC) every 10–20ms."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Android OS Restrictions
                TechDocCard(
                    title = "3. Android OS Background Restrictions",
                    badge = "ANDROID 14+",
                    content = "• Microphone Background Policy: Since Android 9 (and strictly enforced in Android 14+), apps cannot access the microphone when in the background unless running as an active FOREGROUND_SERVICE with FOREGROUND_SERVICE_MICROPHONE type and a visible persistent notification.\n• Doze Mode: When the screen turns off and the device is idle, Android halts background CPU wake-locks unless the app is exempt from Battery Optimization.\n• Lock Screen Behavior: For user privacy and security, lock screens prevent speech recognition from triggering private intents or reading locked notifications."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_explainer_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TechDocCard(
    title: String,
    badge: String,
    content: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = JarvisBackground,
        border = BorderStroke(1.dp, Color(0xFF22304A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = JarvisCyanPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyanPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
