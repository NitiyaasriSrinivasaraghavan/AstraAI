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

// Official Nova Color Palette (Duolingo-inspired flat illustration style)
val NovaTealPrimary = Color(0xFF2EBD85)
val NovaTealDark = Color(0xFF239E6D)
val NovaTealLight = Color(0xFF5CE1A6)
val NovaDarkVisor = Color(0xFF192A24)
val NovaWhiteShell = Color(0xFFF6FAF7)
val NovaBodyCream = Color(0xFFEDF5F0)
val NovaMintGlow = Color(0xFF4EE8A2)
val NovaGroundShadow = Color(0xFFD3E4DB)

/**
 * Standalone Nova Character Component
 * Renders Nova the AI Career Companion as a prominent, friendly, individual interviewer character.
 * Preserves Nova's visual identity:
 * - Rounded robot body & head
 * - Dark expressive visor with mint smiling eyes
 * - Leaf sprout on head
 * - Teal headphones with white "N" branding
 * - Chest with teal "N" branding
 * - Friendly waving pose
 */
@Composable
fun NovaCharacter(
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    state: NovaState = NovaState.IDLE
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
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NovaSpeakingBounce"
    )

    val currentScale = if (state == NovaState.SPEAKING) speakingBounce else 1f

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
            // Left collar
            drawRoundRect(
                color = NovaTealPrimary,
                topLeft = Offset(w * 0.27f, h * 0.55f),
                size = Size(w * 0.11f, h * 0.07f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )
            // Right collar
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

            // 5. Right Arm & Hand (Waving hello gesture)
            val rightHandCenter = Offset(w * 0.80f, h * 0.52f + (waveRotation * 0.5f))
            // Arm connector
            drawLine(
                color = NovaBodyCream,
                start = Offset(w * 0.68f, h * 0.60f),
                end = rightHandCenter,
                strokeWidth = w * 0.09f,
                cap = StrokeCap.Round
            )
            // Rounded waving hand
            drawCircle(
                color = NovaTealPrimary,
                radius = w * 0.075f,
                center = rightHandCenter
            )
            // Thumb curve
            drawCircle(
                color = NovaTealPrimary,
                radius = w * 0.035f,
                center = Offset(rightHandCenter.x - w * 0.04f, rightHandCenter.y - h * 0.02f)
            )

            // 6. Energy / Sparkle Accent Lines near Waving Hand
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

            // 7. Head Sprout / Leaf Antenna on Top
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

            // 8. Headphones / Ear Cups & Headband
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

            // 9. Robot Head Shell (White rounded rectangle)
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

            // 10. Visor / Face Display (Dark screen)
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

            // 11. Expressive Face / Eyes (Mint glowing curved eyes ^ ^)
            val eyeStroke = w * 0.038f
            val leftEyeX = w * 0.40f
            val rightEyeX = w * 0.60f
            val eyeY = h * 0.39f
            val eyeSpan = w * 0.065f

            when (state) {
                NovaState.THINKING -> {
                    // Curious / thinking eyes (round open circles looking up)
                    drawCircle(
                        color = NovaMintGlow,
                        radius = w * 0.042f,
                        center = Offset(leftEyeX, eyeY - h * 0.015f)
                    )
                    drawCircle(
                        color = NovaDarkVisor,
                        radius = w * 0.020f,
                        center = Offset(leftEyeX + w * 0.01f, eyeY - h * 0.022f)
                    )
                    drawCircle(
                        color = NovaMintGlow,
                        radius = w * 0.042f,
                        center = Offset(rightEyeX, eyeY - h * 0.015f)
                    )
                    drawCircle(
                        color = NovaDarkVisor,
                        radius = w * 0.020f,
                        center = Offset(rightEyeX + w * 0.01f, eyeY - h * 0.022f)
                    )
                }
                NovaState.ENCOURAGING -> {
                    // Star eyes for excitement / completion
                    drawStarEye(scope = this, centerX = leftEyeX, centerY = eyeY, radius = w * 0.05f, color = NovaMintGlow)
                    drawStarEye(scope = this, centerX = rightEyeX, centerY = eyeY, radius = w * 0.05f, color = NovaMintGlow)
                }
                else -> {
                    // Standard Happy Smiling Curved Eyes (^ ^)
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
                }
            }

            // Cute small smile curve
            val smileCenterY = h * 0.44f
            drawRoundRect(
                color = NovaMintGlow,
                topLeft = Offset(w * 0.47f, smileCenterY),
                size = Size(w * 0.06f, h * 0.022f),
                cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
            )
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
