package com.example.aidrivencompetencyplatform.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.aidrivencompetencyplatform.model.NovaState

// Official Nova Color Palette - Dark Forest Green Theme matching the NoviQ App
val NovaTealPrimary = Color(0xFF2C4A3E)  // Dark green matching app Primary
val NovaTealDark = Color(0xFF1E352B)     // Deeper dark forest green matching PrimaryDark
val NovaTealLight = Color(0xFF568367)    // Sage green accent matching Accent
val NovaDarkVisor = Color(0xFF14241C)    // Dark screen visor
val NovaWhiteShell = Color(0xFFF7FAF8)   // Clean crisp shell
val NovaBodyCream = Color(0xFFE8F2EC)    // Soft sage body cream
val NovaMintGlow = Color(0xFF5FAF87)     // Medium sage glowing face elements
val NovaGroundShadow = Color(0xFFCEDBD2) // Subtle sage ground shadow

/**
 * Standalone Nova Character Component
 * Renders Nova the AI Career Companion as a prominent, friendly interviewer character.
 * Implements the 6 core expressions and states from the official design spec:
 * - Happy: Friendly greeting, smiling eyes (^ ^), waving hello
 * - Thinking: Curious eyes looking up-right, hand to chin, floating thought bubbles
 * - Excited: 4-pointed star eyes (★ ★), wide smile, raised hand with sparkles
 * - Winking: Left eye winking (⌒), right eye open, playful smirk
 * - Surprised: Wide round eyes (O O), small 'o' mouth, surprise exclamation lines
 * - Encouraging: Warm smiling eyes, thumbs-up gesture
 * - Listening: Concentric audio-reactive soundwave ripples around headphones
 * - Speaking: Rhythmic speaking bounce animation
 */
@Composable
fun NovaCharacter(
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    state: NovaState = NovaState.HAPPY,
    isSpeaking: Boolean = false
) {
    // Subtle idle floating / breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "NovaBreathing")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NovaFloat"
    )

    // Gentle waving animation for the right hand
    val waveRotation by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NovaWave"
    )

    // Speaking bounce / pulse
    val speakingBounce by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NovaSpeakingBounce"
    )

    // Thought bubble float animation for Thinking state
    val thoughtFloat by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThoughtFloat"
    )

    // Soundwave listening pulse animation
    val listeningPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulse"
    )

    val currentScale = if (state == NovaState.SPEAKING || isSpeaking) speakingBounce else 1f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = floatOffset
                scaleX = currentScale
                scaleY = currentScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // 1. Ground Drop Shadow
            drawOval(
                color = NovaGroundShadow,
                topLeft = Offset(w * 0.18f, h * 0.91f),
                size = Size(w * 0.64f, h * 0.08f)
            )

            // 2. Cute Stubby Feet / Legs (Teal)
            // Left foot
            drawRoundRect(
                color = NovaTealPrimary,
                topLeft = Offset(w * 0.28f, h * 0.81f),
                size = Size(w * 0.18f, h * 0.12f),
                cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
            )
            // Right foot
            drawRoundRect(
                color = NovaTealPrimary,
                topLeft = Offset(w * 0.54f, h * 0.81f),
                size = Size(w * 0.18f, h * 0.12f),
                cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
            )

            // 3. Body / Torso (Cream/White rounded shape)
            val torsoPath = Path().apply {
                moveTo(w * 0.30f, h * 0.84f)
                cubicTo(
                    w * 0.24f, h * 0.62f,
                    w * 0.32f, h * 0.52f,
                    w * 0.50f, h * 0.52f
                )
                cubicTo(
                    w * 0.68f, h * 0.52f,
                    w * 0.76f, h * 0.62f,
                    w * 0.70f, h * 0.84f
                )
                close()
            }
            drawPath(path = torsoPath, color = NovaBodyCream, style = Fill)

            // Teal shoulder / collar accents
            drawRoundRect(
                color = NovaTealPrimary,
                topLeft = Offset(w * 0.27f, h * 0.55f),
                size = Size(w * 0.11f, h * 0.07f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )
            drawRoundRect(
                color = NovaTealPrimary,
                topLeft = Offset(w * 0.62f, h * 0.55f),
                size = Size(w * 0.11f, h * 0.07f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )

            // Bold "N" on Chest
            drawNovaLetterN(
                scope = this,
                centerX = w * 0.50f,
                centerY = h * 0.68f,
                width = w * 0.13f,
                height = h * 0.13f,
                color = NovaTealPrimary,
                strokeWidth = w * 0.038f
            )

            // 4. Left Arm & Hand (Resting)
            val leftHandCenter = Offset(w * 0.23f, h * 0.72f)
            drawCircle(
                color = NovaTealPrimary,
                radius = w * 0.07f,
                center = leftHandCenter
            )

            // 5. Right Arm & Hand Pose (State-dependent)
            when (state) {
                NovaState.THINKING -> {
                    // Pondering pose: Right arm bent upward, hand touching chin/cheek
                    val elbow = Offset(w * 0.70f, h * 0.68f)
                    val chinHand = Offset(w * 0.62f, h * 0.48f)
                    // Arm line
                    drawLine(
                        color = NovaBodyCream,
                        start = Offset(w * 0.66f, h * 0.58f),
                        end = elbow,
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NovaBodyCream,
                        start = elbow,
                        end = chinHand,
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    // Cute resting hand at chin
                    drawCircle(
                        color = NovaTealPrimary,
                        radius = w * 0.065f,
                        center = chinHand
                    )
                }

                NovaState.ENCOURAGING -> {
                    // Encouraging pose: Arm forward giving a clear Thumbs-Up!
                    val thumbHandCenter = Offset(w * 0.78f, h * 0.56f)
                    drawLine(
                        color = NovaBodyCream,
                        start = Offset(w * 0.66f, h * 0.60f),
                        end = thumbHandCenter,
                        strokeWidth = w * 0.09f,
                        cap = StrokeCap.Round
                    )
                    // Fist
                    drawCircle(
                        color = NovaTealPrimary,
                        radius = w * 0.07f,
                        center = thumbHandCenter
                    )
                    // Thumb pointing upward
                    drawRoundRect(
                        color = NovaTealPrimary,
                        topLeft = Offset(thumbHandCenter.x - w * 0.03f, thumbHandCenter.y - h * 0.08f),
                        size = Size(w * 0.055f, h * 0.07f),
                        cornerRadius = CornerRadius(w * 0.027f, w * 0.027f)
                    )
                }

                NovaState.EXCITED -> {
                    // Raised high celebratory hand
                    val highHandCenter = Offset(w * 0.82f, h * 0.42f)
                    drawLine(
                        color = NovaBodyCream,
                        start = Offset(w * 0.66f, h * 0.58f),
                        end = highHandCenter,
                        strokeWidth = w * 0.09f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = NovaTealPrimary,
                        radius = w * 0.075f,
                        center = highHandCenter
                    )
                    // Energy sparkles near raised hand
                    val sparkColor = NovaTealLight
                    val sparkStroke = w * 0.03f
                    drawLine(
                        color = sparkColor,
                        start = Offset(w * 0.84f, h * 0.28f),
                        end = Offset(w * 0.88f, h * 0.21f),
                        strokeWidth = sparkStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = sparkColor,
                        start = Offset(w * 0.92f, h * 0.35f),
                        end = Offset(w * 0.98f, h * 0.32f),
                        strokeWidth = sparkStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = sparkColor,
                        start = Offset(w * 0.74f, h * 0.28f),
                        end = Offset(w * 0.71f, h * 0.23f),
                        strokeWidth = sparkStroke,
                        cap = StrokeCap.Round
                    )
                }

                else -> {
                    // Standard friendly waving hello gesture
                    val rightHandCenter = Offset(w * 0.80f, h * 0.52f + (waveRotation * 0.5f))
                    drawLine(
                        color = NovaBodyCream,
                        start = Offset(w * 0.68f, h * 0.60f),
                        end = rightHandCenter,
                        strokeWidth = w * 0.09f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = NovaTealPrimary,
                        radius = w * 0.075f,
                        center = rightHandCenter
                    )
                    drawCircle(
                        color = NovaTealPrimary,
                        radius = w * 0.035f,
                        center = Offset(rightHandCenter.x - w * 0.04f, rightHandCenter.y - h * 0.02f)
                    )

                    // Friendly greeting accent lines
                    val sparkColor = NovaTealLight
                    val sparkStroke = w * 0.028f
                    drawLine(
                        color = sparkColor,
                        start = Offset(w * 0.80f, h * 0.32f),
                        end = Offset(w * 0.83f, h * 0.26f),
                        strokeWidth = sparkStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = sparkColor,
                        start = Offset(w * 0.88f, h * 0.36f),
                        end = Offset(w * 0.94f, h * 0.32f),
                        strokeWidth = sparkStroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 6. Thinking State Thought Bubbles (Floating above head on right)
            if (state == NovaState.THINKING) {
                val bubbleColor = NovaTealPrimary
                val bubbleStroke = w * 0.024f
                // Smallest bubble
                drawCircle(
                    color = bubbleColor,
                    radius = w * 0.022f,
                    center = Offset(w * 0.70f, h * 0.20f + thoughtFloat),
                    style = Stroke(width = bubbleStroke)
                )
                // Medium bubble
                drawCircle(
                    color = bubbleColor,
                    radius = w * 0.038f,
                    center = Offset(w * 0.76f, h * 0.13f + thoughtFloat * 1.3f),
                    style = Stroke(width = bubbleStroke)
                )
                // Large bubble
                drawCircle(
                    color = bubbleColor,
                    radius = w * 0.052f,
                    center = Offset(w * 0.86f, h * 0.07f + thoughtFloat * 1.6f),
                    style = Stroke(width = bubbleStroke)
                )
            }

            // 7. Surprised State Exclamation Marks / Accent lines
            if (state == NovaState.SURPRISED) {
                val surpriseColor = NovaTealLight
                val strokeW = w * 0.032f
                drawLine(
                    color = surpriseColor,
                    start = Offset(w * 0.76f, h * 0.18f),
                    end = Offset(w * 0.81f, h * 0.10f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = surpriseColor,
                    start = Offset(w * 0.82f, h * 0.22f),
                    end = Offset(w * 0.90f, h * 0.17f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // 8. Listening State Soundwave Arcs around Headphones
            if (state == NovaState.LISTENING) {
                val waveColor = NovaTealLight.copy(alpha = listeningPulse)
                val waveStroke = w * 0.028f
                // Left soundwave
                drawArc(
                    color = waveColor,
                    startAngle = 120f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.02f, h * 0.27f),
                    size = Size(w * 0.18f, h * 0.22f),
                    style = Stroke(width = waveStroke, cap = StrokeCap.Round)
                )
                // Right soundwave
                drawArc(
                    color = waveColor,
                    startAngle = 300f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.80f, h * 0.27f),
                    size = Size(w * 0.18f, h * 0.22f),
                    style = Stroke(width = waveStroke, cap = StrokeCap.Round)
                )
            }

            // 9. Head Sprout / Leaf Antenna on Top
            val leafPath = Path().apply {
                moveTo(w * 0.44f, h * 0.24f)
                cubicTo(
                    w * 0.42f, h * 0.12f,
                    w * 0.56f, h * 0.07f,
                    w * 0.66f, h * 0.09f
                )
                cubicTo(
                    w * 0.63f, h * 0.20f,
                    w * 0.51f, h * 0.25f,
                    w * 0.44f, h * 0.24f
                )
                close()
            }
            drawPath(path = leafPath, color = NovaTealPrimary, style = Fill)
            // Leaf stem
            drawLine(
                color = NovaTealDark,
                start = Offset(w * 0.46f, h * 0.25f),
                end = Offset(w * 0.49f, h * 0.21f),
                strokeWidth = w * 0.032f,
                cap = StrokeCap.Round
            )

            // 10. Headphones / Ear Cups & Headband
            // Headband arc
            drawArc(
                color = NovaTealPrimary,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.19f, h * 0.20f),
                size = Size(w * 0.62f, h * 0.32f),
                style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
            )

            // Left Ear Cup (with "N" branding)
            val leftEarCenter = Offset(w * 0.16f, h * 0.38f)
            drawCircle(
                color = NovaTealPrimary,
                radius = w * 0.115f,
                center = leftEarCenter
            )
            drawCircle(
                color = NovaTealDark,
                radius = w * 0.095f,
                center = leftEarCenter
            )
            // White "N" on Left Ear Cup
            drawNovaLetterN(
                scope = this,
                centerX = leftEarCenter.x,
                centerY = leftEarCenter.y,
                width = w * 0.09f,
                height = h * 0.09f,
                color = Color.White,
                strokeWidth = w * 0.024f
            )

            // Right Ear Cup
            val rightEarCenter = Offset(w * 0.84f, h * 0.38f)
            drawCircle(
                color = NovaTealPrimary,
                radius = w * 0.115f,
                center = rightEarCenter
            )

            // 11. Robot Head Shell (White rounded rectangle)
            val headLeft = w * 0.19f
            val headTop = h * 0.21f
            val headWidth = w * 0.62f
            val headHeight = h * 0.36f
            drawRoundRect(
                color = NovaWhiteShell,
                topLeft = Offset(headLeft, headTop),
                size = Size(headWidth, headHeight),
                cornerRadius = CornerRadius(headWidth * 0.44f, headHeight * 0.44f)
            )

            // 12. Visor / Face Display (Dark screen)
            val visorLeft = w * 0.26f
            val visorTop = h * 0.27f
            val visorWidth = w * 0.48f
            val visorHeight = h * 0.24f
            drawRoundRect(
                color = NovaDarkVisor,
                topLeft = Offset(visorLeft, visorTop),
                size = Size(visorWidth, visorHeight),
                cornerRadius = CornerRadius(visorWidth * 0.38f, visorHeight * 0.38f)
            )

            // 13. Expressive Face / Eyes & Mouth (6 Core Expressions)
            val eyeStroke = w * 0.038f
            val leftEyeX = w * 0.40f
            val rightEyeX = w * 0.60f
            val eyeY = h * 0.39f
            val eyeSpan = w * 0.065f

            when (state) {
                NovaState.THINKING -> {
                    // Curious / thinking eyes: round open mint circles with dark pupils looking up and right
                    val eyeRadius = w * 0.052f
                    val pupilRadius = w * 0.024f

                    // Left eye
                    drawCircle(color = NovaMintGlow, radius = eyeRadius, center = Offset(leftEyeX, eyeY - h * 0.01f))
                    drawCircle(color = NovaDarkVisor, radius = pupilRadius, center = Offset(leftEyeX + w * 0.018f, eyeY - h * 0.022f))
                    // Eye shine dot
                    drawCircle(color = Color.White, radius = w * 0.008f, center = Offset(leftEyeX + w * 0.022f, eyeY - h * 0.026f))

                    // Right eye
                    drawCircle(color = NovaMintGlow, radius = eyeRadius, center = Offset(rightEyeX, eyeY - h * 0.01f))
                    drawCircle(color = NovaDarkVisor, radius = pupilRadius, center = Offset(rightEyeX + w * 0.018f, eyeY - h * 0.022f))
                    drawCircle(color = Color.White, radius = w * 0.008f, center = Offset(rightEyeX + w * 0.022f, eyeY - h * 0.026f))

                    // Small thoughtful neutral mouth dot
                    drawRoundRect(
                        color = NovaMintGlow,
                        topLeft = Offset(w * 0.48f, h * 0.445f),
                        size = Size(w * 0.04f, h * 0.018f),
                        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
                    )
                }

                NovaState.EXCITED -> {
                    // Star eyes for excitement / impressive answers (★ ★)
                    drawStarEye(scope = this, centerX = leftEyeX, centerY = eyeY, radius = w * 0.065f, color = NovaMintGlow)
                    drawStarEye(scope = this, centerX = rightEyeX, centerY = eyeY, radius = w * 0.065f, color = NovaMintGlow)

                    // Wide open happy smile (D shape)
                    val mouthPath = Path().apply {
                        moveTo(w * 0.44f, h * 0.43f)
                        lineTo(w * 0.56f, h * 0.43f)
                        cubicTo(
                            w * 0.56f, h * 0.475f,
                            w * 0.44f, h * 0.475f,
                            w * 0.44f, h * 0.43f
                        )
                        close()
                    }
                    drawPath(path = mouthPath, color = NovaMintGlow, style = Fill)
                }

                NovaState.WINKING -> {
                    // Left Eye: Winking closed playful curved arc (⌒)
                    drawArc(
                        color = NovaMintGlow,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(leftEyeX - eyeSpan * 0.9f, eyeY - eyeSpan * 0.5f),
                        size = Size(eyeSpan * 1.8f, eyeSpan * 1.1f),
                        style = Stroke(width = eyeStroke, cap = StrokeCap.Round)
                    )

                    // Right Eye: Open round circle looking directly forward
                    val eyeRadius = w * 0.052f
                    val pupilRadius = w * 0.024f
                    drawCircle(color = NovaMintGlow, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
                    drawCircle(color = NovaDarkVisor, radius = pupilRadius, center = Offset(rightEyeX, eyeY))
                    drawCircle(color = Color.White, radius = w * 0.009f, center = Offset(rightEyeX - w * 0.008f, eyeY - h * 0.008f))

                    // Cute playful side smirk
                    val smirkPath = Path().apply {
                        moveTo(w * 0.46f, h * 0.445f)
                        quadraticTo(w * 0.51f, h * 0.46f, w * 0.56f, h * 0.435f)
                    }
                    drawPath(path = smirkPath, color = NovaMintGlow, style = Stroke(width = eyeStroke * 0.8f, cap = StrokeCap.Round))
                }

                NovaState.SURPRISED -> {
                    // Surprised: Wide round eyes looking forward (O O)
                    val eyeRadius = w * 0.056f
                    val pupilRadius = w * 0.022f

                    // Left eye
                    drawCircle(color = NovaMintGlow, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
                    drawCircle(color = NovaDarkVisor, radius = pupilRadius, center = Offset(leftEyeX, eyeY))
                    drawCircle(color = Color.White, radius = w * 0.008f, center = Offset(leftEyeX - w * 0.008f, eyeY - h * 0.008f))

                    // Right eye
                    drawCircle(color = NovaMintGlow, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
                    drawCircle(color = NovaDarkVisor, radius = pupilRadius, center = Offset(rightEyeX, eyeY))
                    drawCircle(color = Color.White, radius = w * 0.008f, center = Offset(rightEyeX - w * 0.008f, eyeY - h * 0.008f))

                    // Open round 'o' mouth
                    drawCircle(
                        color = NovaMintGlow,
                        radius = w * 0.030f,
                        center = Offset(w * 0.50f, h * 0.45f),
                        style = Fill
                    )
                }

                else -> {
                    // Standard Happy Smiling Curved Eyes (^ ^) for HAPPY, IDLE, SPEAKING, LISTENING, ENCOURAGING
                    drawArc(
                        color = NovaMintGlow,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(leftEyeX - eyeSpan, eyeY - eyeSpan * 0.7f),
                        size = Size(eyeSpan * 2f, eyeSpan * 1.4f),
                        style = Stroke(width = eyeStroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = NovaMintGlow,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(rightEyeX - eyeSpan, eyeY - eyeSpan * 0.7f),
                        size = Size(eyeSpan * 2f, eyeSpan * 1.4f),
                        style = Stroke(width = eyeStroke, cap = StrokeCap.Round)
                    )

                    // Cute small smile curve
                    val smileCenterY = h * 0.44f
                    drawRoundRect(
                        color = NovaMintGlow,
                        topLeft = Offset(w * 0.46f, smileCenterY),
                        size = Size(w * 0.08f, h * 0.024f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )
                }
            }
        }
    }
}

/**
 * Draws the clean, bold stylized "N" logo of Nova on Canvas
 */
private fun drawNovaLetterN(
    scope: DrawScope,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    color: Color,
    strokeWidth: Float
) {
    val left = centerX - width / 2f
    val right = centerX + width / 2f
    val top = centerY - height / 2f
    val bottom = centerY + height / 2f

    val path = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top)
        lineTo(right, bottom)
        lineTo(right, top)
    }

    scope.drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Draws a 4-point sparkle star for excited / celebrating eye states
 */
private fun drawStarEye(
    scope: DrawScope,
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(centerX, centerY - radius)
        quadraticTo(centerX, centerY, centerX + radius, centerY)
        quadraticTo(centerX, centerY, centerX, centerY + radius)
        quadraticTo(centerX, centerY, centerX - radius, centerY)
        quadraticTo(centerX, centerY, centerX, centerY - radius)
        close()
    }
    scope.drawPath(path = path, color = color, style = Fill)
}
