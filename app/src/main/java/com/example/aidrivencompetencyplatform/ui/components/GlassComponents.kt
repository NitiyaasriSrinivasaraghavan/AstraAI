package com.example.aidrivencompetencyplatform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.aidrivencompetencyplatform.ui.theme.GlassBorder
import com.example.aidrivencompetencyplatform.ui.theme.GlassWhite

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundBrush = if (containerColor != null) {
        Brush.verticalGradient(listOf(containerColor, containerColor.copy(alpha = 0.8f)))
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.7f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush = backgroundBrush)
            .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius))
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
