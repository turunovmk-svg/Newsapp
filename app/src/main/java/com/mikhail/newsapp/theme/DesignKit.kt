package com.mikhail.newsapp.theme

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhail.newsapp.UiSizes
import com.mikhail.newsapp.data.CatalogSource
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.data.Source

/**
 * Дизайн-кит по референсам (Discover / Search / Add feed / Reading).
 * Единые компоненты, чтобы все экраны выглядели как одно приложение.
 */

/** Мета-строка карточки: источник · время · N мин чтения (как в референсах). */
@Composable
fun RefMetaLine(
    sourceName: String?,
    timeText: String?,
    modifier: Modifier = Modifier
) {
    val parts = listOfNotNull(
        sourceName?.takeIf { it.isNotBlank() },
        timeText?.takeIf { it.isNotBlank() }
    )
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString("  ·  "),
        style = CyberTypography.BodySmall.copy(fontSize = 12.sp, letterSpacing = 0.2.sp),
        color = CyberColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Круглый цветной бейдж источника (буква/символ), как в каталоге референса. */
@Composable
fun RefLogoBadge(
    letter: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = UiSizes.RefLogoCircle
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.take(1).uppercase(),
            color = Color.White,
            style = CyberTypography.TitleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
    }
}

/** Заголовок секции с опциональным действием справа (как "Daily News /March 8"). */
@Composable
fun RefSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = UiSizes.RefSectionSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = CyberTypography.TitleMedium,
            color = CyberColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = CyberTypography.BodySmall,
                color = CyberColors.AccentBlue,
                modifier = if (onTrailingClick != null) Modifier.clickable { onTrailingClick() } else Modifier
            )
        }
    }
}

/** Таб-переключатель с подчёркиванием активного (как News/Topics/Sources). */
@Composable
fun RefUnderlinedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerLarge)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(index) }
            ) {
                Text(
                    text = label,
                    style = CyberTypography.BodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (selected) CyberColors.TextPrimary else CyberColors.TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(
                            if (selected) CyberColors.AccentBlue else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

/** Пилюля-чип (категории, популярные запросы). */
@Composable
fun RefChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.painter.Painter? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(UiSizes.RefChipRadius))
            .background(
                if (selected) CyberColors.AccentBlue
                else CyberColors.SurfaceElevated.copy(alpha = 0.55f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                tint = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = CyberTypography.BodySmall.copy(fontSize = 13.sp),
            color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
            maxLines = 1
        )
    }
}

/** Строка настроек: иконка в тонком круге + заголовок + подпись + действие. */
@Composable
fun RefSettingRow(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = CyberColors.AccentBlue,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = UiSizes.SpacerMedium, vertical = UiSizes.RefRowSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(UiSizes.RefIconCircle)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CyberTypography.BodyLarge.copy(fontSize = 15.sp),
                color = CyberColors.TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextSecondary
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Text(
                text = "›",
                style = CyberTypography.TitleMedium,
                color = CyberColors.TextMuted
            )
        }
    }
}

/** Строка каталога источников: лого, название, описание, кнопка Follow. */
@Composable
fun RefCatalogRow(
    source: CatalogSource,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UiSizes.SpacerMedium, vertical = UiSizes.SpacerSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RefLogoBadge(letter = source.letter, color = Color(source.brandColor))
        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                style = CyberTypography.BodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = CyberColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = source.description,
                style = CyberTypography.BodySmall,
                color = CyberColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
        if (isFollowing) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(UiSizes.RefChipRadius))
                    .background(CyberColors.Success.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✓ Добавлен",
                    style = CyberTypography.BodySmall.copy(fontSize = 12.sp),
                    color = CyberColors.Success
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(UiSizes.RefChipRadius))
                    .background(CyberColors.AccentBlue)
                    .clickable { onFollowClick() }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Подписаться",
                    style = CyberTypography.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

/** Большая карточка новости с изображением, градиентом и чипом источника — как на референсе Discover. */
@Composable
fun RefNewsCard(
    title: String,
    imageUrl: String?,
    sourceChip: String?,
    siteUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    metaTime: String? = null,
    description: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiSizes.RefCardRadius))
            .background(CyberColors.SurfaceElevated)
            .clickable { onClick() }
    ) {
        // Изображение с градиентом и чипом
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(UiSizes.RefCardImageHeight)
        ) {
            SmartNewsImage(
                imageUrl = imageUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CyberColors.BackgroundDeep.copy(alpha = 0.55f))
                        )
                    )
            )
            if (sourceChip != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(UiSizes.SpacerSmall)
                        .clip(RoundedCornerShape(UiSizes.RefChipRadius))
                        .background(CyberColors.BackgroundDeep.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sourceChip.take(26),
                        style = CyberTypography.BodySmall.copy(fontSize = 11.sp),
                        color = CyberColors.TextPrimary
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(UiSizes.SpacerMedium)
            ) {
                Text(
                    text = title,
                    style = CyberTypography.TitleLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberColors.TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Мета + описание под изображением
        Column(modifier = Modifier.padding(UiSizes.SpacerMedium)) {
            RefMetaLine(sourceName = sourceChip, timeText = metaTime)
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Post.stripHtml(description) ?: "",
                    style = CyberTypography.BodyMedium,
                    color = CyberColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Пустое состояние в стиле референса (иконка + заголовок + подсказка). */
@Composable
fun RefEmptyState(
    iconRes: Int,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UiSizes.SpacerLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CyberColors.AccentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = CyberColors.AccentBlue,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        Text(
            text = title,
            style = CyberTypography.TitleMedium,
            color = CyberColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hint,
            style = CyberTypography.BodyMedium,
            color = CyberColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            action()
        }
    }
}
