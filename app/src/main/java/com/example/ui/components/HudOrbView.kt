package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBlueSecondary
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisNeonGreen
import com.example.viewmodel.MicState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudOrbView(
    micState: MicState,
    soundRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_infinite")

    // Slow rotation for HUD outer ring
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse fast rotation for processing
    val processingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "processing_rotation"
    )

    // Breathing pulse for idle
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Animated dynamic scale responding to sound volume
    val animatedRms = remember { Animatable(0f) }
    LaunchedEffect(soundRms) {
        animatedRms.animateTo(soundRms, animationSpec = tween(80))
    }

    val coreColor = when (micState) {
        MicState.LISTENING -> JarvisCyanPrimary
        MicState.SPEAKING -> JarvisNeonGreen
        MicState.PROCESSING -> JarvisAmber
        MicState.IDLE -> JarvisCyanPrimary.copy(alpha = 0.8f)
    }

    val stateLabel = when (micState) {
        MicState.LISTENING -> "LISTENING..."
        MicState.SPEAKING -> "JARVIS SPEAKING"
        MicState.PROCESSING -> "PROCESSING INTENT"
        MicState.IDLE -> "TAP TO SPEAK / 'HEY JARVIS'"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("hud_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.width / 2f - 20.dp.toPx()
                val dynamicRadius = when (micState) {
                    MicState.LISTENING -> baseRadius * (0.85f + animatedRms.value * 0.25f)
                    MicState.SPEAKING -> baseRadius * (0.88f + idlePulse * 0.08f)
                    MicState.PROCESSING -> baseRadius * 0.88f
                    MicState.IDLE -> baseRadius * 0.82f * idlePulse
                }

                // Outer decorative HUD dotted ring
                drawCircle(
                    color = coreColor.copy(alpha = 0.35f),
                    radius = baseRadius + 10.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), ringRotation)
                    )
                )

                // Secondary inner tech notches
                val angleOffset = if (micState == MicState.PROCESSING) processingRotation else ringRotation
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 + angleOffset).toDouble())
                    val notchInner = baseRadius - 6.dp.toPx()
                    val notchOuter = baseRadius + 2.dp.toPx()
                    val start = Offset(
                        (center.x + notchInner * cos(angle)).toFloat(),
                        (center.y + notchInner * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        (center.x + notchOuter * cos(angle)).toFloat(),
                        (center.y + notchOuter * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = coreColor.copy(alpha = 0.4f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Ambient Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.45f),
                            coreColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius * 1.3f
                    ),
                    radius = dynamicRadius * 1.3f,
                    center = center
                )

                // High-tech Arc Reactor Inner Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            coreColor,
                            JarvisBlueSecondary
                        ),
                        center = center,
                        radius = dynamicRadius
                    ),
                    radius = dynamicRadius,
                    center = center
                )

                // Central inner ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = dynamicRadius * 0.45f,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Center core dot
                drawCircle(
                    color = Color.White,
                    radius = dynamicRadius * 0.18f,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // State indicator pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = coreColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, coreColor.copy(alpha = 0.6f)),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = coreColor)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stateLabel,
                    color = coreColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
