package com.mikhail.newsapp.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * Карточка в стиле "matte glass" — полупрозрачная поверхность с неоновой границей.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = CyberColors.GlowCyan,
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
                        CyberColors.SurfaceGlassHigh.copy(alpha = 0.75f)
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
            .graphicsLayer {
                shadow(
                    color = glowColor.copy(alpha = 0.35f),
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp)
                )
            }
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
    primaryColor: Color = CyberColors.NeonCyan,
    secondaryColor: Color = CyberColors.NeonPurple,
    enabled: Boolean = true
) {
    val isPressed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
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
            .graphicsLayer {
                if (enabled && isPressed) {
                    scaleX = 0.96f
                    scaleY = 0.96f
                    alpha = 0.85f
                } else if (enabled) {
                    scaleX = 1f
                    scaleY = 1f
                    alpha = 1f
                    elevation = 8.dp
                    shadow(
                        color = primaryColor.copy(alpha = 0.35f),
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
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
    color: Color = CyberColors.TextOnSurface,
    glowColor: Color = CyberColors.NeonCyan,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier
            .graphicsLayer {
                shadow(
                    color = glowColor.copy(alpha = 0.35f),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            },
    )
}

/**
 * Разделитель с неоновым градиентом.
 */
@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color1: Color = CyberColors.NeonCyan,
    color2: Color = CyberColors.NeonPurple,
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

/**
 * Карточка новости в стиле cyberpunk с неоновым акцентом слева.
 */
@Composable
fun NewsCard(
    post: com.mikhail.newsapp.data.Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        glowColor = CyberColors.NeonCyan
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Неоновый акцент слева (маленькая вертикальная полоска)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CyberColors.NeonCyan, CyberColors.NeonPurple)
                        )
                    )
                    .padding(bottom = 12.dp)
            )

            post.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22223A))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = CyberColors.TextOnSurface,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            post.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberColors.TextMuted,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            post.publishedAt?.let { date ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberColors.TextDim
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberColors.NeonCyan
                    )
                }
            }
        }
    }
}
