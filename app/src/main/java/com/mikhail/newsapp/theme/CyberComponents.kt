package com.mikhail.newsapp.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
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
 * Премиальная кнопка: градиент + мягкая синяя тень + блик по верхней кромке.
 * Нажатие — лёгкое уменьшение (press-scale), отклик ощущается «физически».
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
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (enabled) 12.dp else 0.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = primaryColor.copy(alpha = 0.5f),
                    spotColor = primaryColor.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = if (enabled) {
                            listOf(primaryColor, secondaryColor)
                        } else {
                            listOf(Color(0xFF232936), Color(0xFF1A1F2B))
                        },
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    )
                )
                .pointerInput(enabled) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        }
                    )
                }
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Внутренний блик по верхней кромке
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (enabled) 0.25f else 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = if (enabled) Color.White else Color(0xFF8A93A5),
                )
            }
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
