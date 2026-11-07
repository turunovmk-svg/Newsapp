package com.mikhail.newsapp.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Карточка в стиле "matte glass" — полупрозрачная поверхность с неоновой границей.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = CyberColors.GlowBlue,
    borderColor: Color = CyberColors.BorderGlass,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyberColors.SurfaceGlass.copy(alpha = 0.85f),
                        CyberColors.SurfaceElevated.copy(alpha = 0.75f)
                    ),
                    start = Offset(x = 0f, y = 0f),
                    end = Offset(x = 1f, y = 1f)
                )
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        content()
    }
}

/**
 * Неоновая кнопка с градиентом и свечением.
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    icon: @Composable (() -> Unit)? = null,
    primaryColor: Color = CyberColors.AccentBlue,
    secondaryColor: Color = CyberColors.AccentBlueBright,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = if (enabled) {
                        listOf(primaryColor, secondaryColor)
                    } else {
                        listOf(Color(0xFF333333), Color(0xFF222222))
                    },
                    start = Offset(x = 0f, y = 0f),
                    end = Offset(x = 1f, y = 0f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color(0xFFFFFFFF) else Color(0xFF888888),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Текст с неоновым свечением (для заголовков и важных меток).
 */
@Composable
fun CyberText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = CyberColors.TextPrimary,
    glowColor: Color = CyberColors.AccentBlue,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
    )
}

/**
 * Разделитель с неоновым градиентом.
 */
@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color1: Color = CyberColors.AccentBlue,
    color2: Color = CyberColors.AccentBlueBright,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(color1.copy(alpha = 0.4f), color1, color2, color1.copy(alpha = 0.4f))
                )
            )
    )
}

// Примечание: NewsCard определён в MainActivity.kt (com.mikhail.newsapp),
// чтобы избежать дублирования и конфликта имён.
