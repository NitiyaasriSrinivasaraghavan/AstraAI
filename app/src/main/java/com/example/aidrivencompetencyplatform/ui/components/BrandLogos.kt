package com.example.aidrivencompetencyplatform.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidrivencompetencyplatform.ui.theme.*

// Brand Color Palette extracted from official assets
val NoviQDarkGreen = Color(0xFF2C4A3E)
val NoviQDeepGreen = Color(0xFF1E352B)
val NoviQRibbonLight = Color(0xFFD8EAD9)
val NoviQRibbonMid = Color(0xFFB2D3B8)
val NoviQRibbonDark = Color(0xFF7CA786)
val NoviQSparkle = Color(0xFFE8F7EB)
val NoviQSubtextColor = Color(0xFF335345)

val NovaSageBg = Color(0xFFD3E2D0)
val NovaDarkGreen = Color(0xFF2B483B)
val NovaRobotWhite = Color(0xFFF3F8F2)
val NovaVisorDark = Color(0xFF1D3328)
val NovaEyesMint = Color(0xFFD4E7D6)
val NovaHeadphones = Color(0xFF476956)
val NovaLeafGreen = Color(0xFF5D846D)

/**
 * Official NoviQ App Icon Tile
 * Visual elements: Dark forest green rounded tile, upward-swooping ribbon "N" with arrow, and 4-point sparkle star.
 */
@Composable
fun NoviQLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    elevation: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation, RoundedCornerShape(size * 0.28f))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NoviQDarkGreen, NoviQDeepGreen)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // 1. Draw 4-point Sparkle Star in top right corner
            val starCenterX = w * 0.68f
            val starCenterY = h * 0.16f
            val starSize = w * 0.18f
            drawSparkleStar(starCenterX, starCenterY, starSize, NoviQSparkle)

            // 2. Draw 3D Ribbon "N" with Arrow Tip
            val ribbonPath = Path().apply {
                // Start from bottom left upright base
                moveTo(w * 0.23f, h * 0.76f)
                // Left curve upward
                cubicTo(
                    w * 0.20f, h * 0.40f,
                    w * 0.28f, h * 0.18f,
                    w * 0.46f, h * 0.18f
                )
                // Top loop arch
                cubicTo(
                    w * 0.58f, h * 0.18f,
                    w * 0.58f, h * 0.32f,
                    w * 0.52f, h * 0.48f
                )
                // Downward diagonal loop to bottom right
                cubicTo(
                    w * 0.46f, h * 0.64f,
                    w * 0.52f, h * 0.80f,
                    w * 0.66f, h * 0.78f
                )
                // Ascend to arrow shaft
                cubicTo(
                    w * 0.75f, h * 0.75f,
                    w * 0.76f, h * 0.45f,
                    w * 0.76f, h * 0.34f
                )
            }

            // Draw ribbon main body stroke with gradient
            drawPath(
                path = ribbonPath,
                brush = Brush.linearGradient(
                    colors = listOf(NoviQRibbonLight, NoviQRibbonMid, NoviQRibbonDark, NoviQRibbonLight),
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.8f)
                ),
                style = Stroke(
                    width = w * 0.17f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw inner loop fold highlight (left pillar)
            val leftPillarPath = Path().apply {
                moveTo(w * 0.31f, h * 0.75f)
                cubicTo(
                    w * 0.29f, h * 0.45f,
                    w * 0.33f, h * 0.26f,
                    w * 0.45f, h * 0.25f
                )
            }
            drawPath(
                path = leftPillarPath,
                color = NoviQRibbonLight.copy(alpha = 0.95f),
                style = Stroke(
                    width = w * 0.13f,
                    cap = StrokeCap.Round
                )
            )

            // Draw Arrow Head pointing to top-right at (w * 0.76f, h * 0.24f)
            val arrowPath = Path().apply {
                val tipX = w * 0.76f
                val tipY = h * 0.23f
                moveTo(tipX, tipY)
                lineTo(tipX - w * 0.12f, tipY + h * 0.08f)
                lineTo(tipX - w * 0.04f, tipY + h * 0.07f)
                lineTo(tipX - w * 0.03f, tipY + h * 0.15f)
                lineTo(tipX + w * 0.05f, tipY + h * 0.14f)
                lineTo(tipX + w * 0.04f, tipY + h * 0.06f)
                lineTo(tipX + w * 0.12f, tipY + h * 0.07f)
                close()
            }
            drawPath(
                path = arrowPath,
                color = NoviQRibbonLight,
                style = Fill
            )
        }
    }
}

/**
 * Official NoviQ Full Logo with App Icon, Typography, Tagline, and Flourish
 * Matches the official NoviQ branding asset.
 */
@Composable
fun NoviQFullLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 100.dp,
    showSubtitle: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Official NoviQ Icon Tile
        NoviQLogoTile(size = iconSize, elevation = 6.dp)

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Official Brand Name: NoviQ
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Novi",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (iconSize.value * 0.40f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = NoviQDarkGreen
            )
            Text(
                text = "Q",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (iconSize.value * 0.40f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color(0xFF568367)
            )
        }

        if (showSubtitle) {
            Spacer(modifier = Modifier.height(4.dp))

            // 3. Official Tagline: YOUR AI CAREER COMPANION
            Text(
                text = "YOUR AI CAREER COMPANION",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.4.sp
                ),
                color = NoviQSubtextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Decorative Flourish with Sparkle Star
            BrandFlourish(width = iconSize * 1.5f, color = Color(0xFF88A790))
        }
    }
}

/**
 * Official Nova AI Chatbot Avatar / Logo Tile
 * Visual elements: Soft sage rounded card, cute robot head with smiling eyes, green headphones, sprout/leaf, speech bubble, and chest sparkle badge.
 */
@Composable
fun NovaAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showBorder: Boolean = true,
    elevation: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation, RoundedCornerShape(size * 0.28f))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(NovaSageBg)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = (size * 0.03f).coerceAtLeast(1.dp),
                        color = NovaDarkGreen.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(size * 0.28f)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // 1. Sprout/Leaf Antenna on top of head
            val leafPath = Path().apply {
                moveTo(w * 0.50f, h * 0.24f)
                cubicTo(
                    w * 0.50f, h * 0.12f,
                    w * 0.62f, h * 0.08f,
                    w * 0.66f, h * 0.10f
                )
                cubicTo(
                    w * 0.64f, h * 0.20f,
                    w * 0.55f, h * 0.24f,
                    w * 0.50f, h * 0.24f
                )
            }
            drawPath(path = leafPath, color = NovaLeafGreen, style = Fill)
            drawLine(
                color = NovaHeadphones,
                start = Offset(w * 0.50f, h * 0.24f),
                end = Offset(w * 0.51f, h * 0.21f),
                strokeWidth = w * 0.035f,
                cap = StrokeCap.Round
            )

            // 2. Speech Bubble at top right with 3 dots
            val bubbleLeft = w * 0.62f
            val bubbleTop = h * 0.14f
            val bubbleWidth = w * 0.26f
            val bubbleHeight = h * 0.17f
            drawRoundRect(
                color = NovaHeadphones,
                topLeft = Offset(bubbleLeft, bubbleTop),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = CornerRadius(bubbleHeight * 0.45f, bubbleHeight * 0.45f)
            )
            // Speech bubble tail
            val tailPath = Path().apply {
                moveTo(bubbleLeft + bubbleWidth * 0.15f, bubbleTop + bubbleHeight)
                lineTo(bubbleLeft - w * 0.02f, bubbleTop + bubbleHeight + h * 0.05f)
                lineTo(bubbleLeft + bubbleWidth * 0.40f, bubbleTop + bubbleHeight)
                close()
            }
            drawPath(path = tailPath, color = NovaHeadphones, style = Fill)

            // 3 dots in speech bubble
            val dotRadius = bubbleHeight * 0.13f
            val dotY = bubbleTop + bubbleHeight * 0.5f
            drawCircle(color = Color.White, radius = dotRadius, center = Offset(bubbleLeft + bubbleWidth * 0.30f, dotY))
            drawCircle(color = Color.White, radius = dotRadius, center = Offset(bubbleLeft + bubbleWidth * 0.50f, dotY))
            drawCircle(color = Color.White, radius = dotRadius, center = Offset(bubbleLeft + bubbleWidth * 0.70f, dotY))

            // 3. Robot Torso / Shoulders
            val torsoPath = Path().apply {
                moveTo(w * 0.24f, h * 0.94f)
                cubicTo(
                    w * 0.26f, h * 0.70f,
                    w * 0.74f, h * 0.70f,
                    w * 0.76f, h * 0.94f
                )
                close()
            }
            drawPath(path = torsoPath, color = NovaRobotWhite, style = Fill)
            drawPath(
                path = torsoPath,
                color = NovaHeadphones.copy(alpha = 0.5f),
                style = Stroke(width = w * 0.02f)
            )

            // Green Straps & Chest Sparkle Badge
            val badgeCenterX = w * 0.50f
            val badgeCenterY = h * 0.79f
            drawCircle(
                color = NovaHeadphones,
                radius = w * 0.085f,
                center = Offset(badgeCenterX, badgeCenterY)
            )
            drawSparkleStar(badgeCenterX, badgeCenterY, w * 0.09f, NovaEyesMint)

            // 4. Robot Headphones / Earcups
            val earRadius = w * 0.08f
            val earY = h * 0.46f
            // Left ear
            drawCircle(color = NovaHeadphones, radius = earRadius, center = Offset(w * 0.20f, earY))
            // Right ear
            drawCircle(color = NovaHeadphones, radius = earRadius, center = Offset(w * 0.80f, earY))
            // Headband connector
            drawArc(
                color = NovaHeadphones,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.22f, h * 0.22f),
                size = Size(w * 0.56f, h * 0.40f),
                style = Stroke(width = w * 0.035f)
            )

            // 5. Robot Head (White Outer Shell)
            val headLeft = w * 0.22f
            val headTop = h * 0.26f
            val headWidth = w * 0.56f
            val headHeight = h * 0.44f
            drawRoundRect(
                color = NovaRobotWhite,
                topLeft = Offset(headLeft, headTop),
                size = Size(headWidth, headHeight),
                cornerRadius = CornerRadius(headWidth * 0.44f, headHeight * 0.44f)
            )
            drawRoundRect(
                color = NovaHeadphones.copy(alpha = 0.35f),
                topLeft = Offset(headLeft, headTop),
                size = Size(headWidth, headHeight),
                cornerRadius = CornerRadius(headWidth * 0.44f, headHeight * 0.44f),
                style = Stroke(width = w * 0.02f)
            )

            // 6. Visor / Face Screen (Dark Screen)
            val visorLeft = w * 0.28f
            val visorTop = h * 0.33f
            val visorWidth = w * 0.44f
            val visorHeight = h * 0.29f
            drawRoundRect(
                color = NovaVisorDark,
                topLeft = Offset(visorLeft, visorTop),
                size = Size(visorWidth, visorHeight),
                cornerRadius = CornerRadius(visorWidth * 0.38f, visorHeight * 0.38f)
            )

            // 7. Happy Smiling Eyes (^ ^)
            val eyeStrokeWidth = w * 0.038f
            val leftEyeCenterX = w * 0.41f
            val rightEyeCenterX = w * 0.59f
            val eyeCenterY = h * 0.47f
            val eyeRadius = w * 0.055f

            // Left curved happy eye
            drawArc(
                color = NovaEyesMint,
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(leftEyeCenterX - eyeRadius, eyeCenterY - eyeRadius),
                size = Size(eyeRadius * 2, eyeRadius * 1.5f),
                style = Stroke(width = eyeStrokeWidth, cap = StrokeCap.Round)
            )

            // Right curved happy eye
            drawArc(
                color = NovaEyesMint,
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(rightEyeCenterX - eyeRadius, eyeCenterY - eyeRadius),
                size = Size(eyeRadius * 2, eyeRadius * 1.5f),
                style = Stroke(width = eyeStrokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Official Nova Full Logo with Chatbot Avatar, Typography, Tagline, and Flourish
 * Matches the official Nova AI chatbot branding asset.
 */
@Composable
fun NovaFullLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 100.dp,
    showSubtitle: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Official Nova Chatbot Avatar Tile
        NovaAvatar(size = iconSize, elevation = 6.dp)

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Official Brand Name: Nova
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Nov",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (iconSize.value * 0.40f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = NovaDarkGreen
            )
            Text(
                text = "a",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (iconSize.value * 0.40f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color(0xFF4E775F)
            )
        }

        if (showSubtitle) {
            Spacer(modifier = Modifier.height(4.dp))

            // 3. Official Tagline: YOUR AI CAREER ASSISTANT
            Text(
                text = "YOUR AI CAREER ASSISTANT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.4.sp
                ),
                color = NovaDarkGreen.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Decorative Flourish with Sparkle Star
            BrandFlourish(width = iconSize * 1.5f, color = Color(0xFF88A790))
        }
    }
}

/**
 * Reusable Decorative Flourish with horizontal lines and centered 4-point star
 */
@Composable
fun BrandFlourish(
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    color: Color = Color(0xFF88A790)
) {
    Row(
        modifier = modifier.width(width),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, color)
                    )
                )
        )
        Canvas(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(10.dp)
        ) {
            drawSparkleStar(size.width / 2f, size.height / 2f, size.width * 0.9f, color)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color, Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Helper to draw a crisp 4-point diamond sparkle star
 */
fun DrawScope.drawSparkleStar(
    centerX: Float,
    centerY: Float,
    diameter: Float,
    color: Color
) {
    val radius = diameter / 2f
    val innerRadius = radius * 0.22f
    val path = Path().apply {
        moveTo(centerX, centerY - radius) // Top point
        quadraticBezierTo(centerX, centerY, centerX + radius, centerY) // Right point
        quadraticBezierTo(centerX, centerY, centerX, centerY + radius) // Bottom point
        quadraticBezierTo(centerX, centerY, centerX - radius, centerY) // Left point
        quadraticBezierTo(centerX, centerY, centerX, centerY - radius) // Close back to Top
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}
