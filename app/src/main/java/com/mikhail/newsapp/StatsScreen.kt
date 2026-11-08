package com.mikhail.newsapp

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.UiSizes
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Статистика чтения: сколько статей прочитано сегодня/за неделю,
 * полоски по дням, топ источников.
 */
@Composable
fun StatsScreen(
    posts: List<Post>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
    }

    // читаем метки времени чтения
    val readTs = remember {
        val raw = prefs.getString("read_ts_json", null) ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, Long>()
        obj.keys().forEach { key -> map[key] = obj.getLong(key) }
        map
    }
    val postsById = remember(posts) { posts.associateBy { it.id } }

    val now = System.currentTimeMillis()
    val dayMs = TimeUnit.DAYS.toMillis(1)

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val todayStart = startOfDay(now)
    val weekStart = todayStart - 6 * dayMs

    val readToday = readTs.values.count { it >= todayStart }
    val readWeek = readTs.values.count { it >= weekStart }
    val readTotal = readTs.size

    // Полоски по дням (7 дней)
    val perDay = (0..6).map { dayAgo ->
        val dayStart = weekStart + dayAgo * dayMs
        val dayEnd = dayStart + dayMs
        readTs.values.count { it in dayStart until dayEnd }
    }
    val maxPerDay = maxOf(perDay.maxOrNull() ?: 0, 1)

    // Топ источников прочитанного
    val topSources = readTs.keys
        .mapNotNull { id ->
            (postsById[id] ?: return@mapNotNull null)
        }
        .groupBy { it.siteUrl ?: it.sourceUrl ?: "?" }
        .mapValues { it.value.size }
        .entries.sortedByDescending { it.value }
        .take(5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UiSizes.ScreenPadding)
    ) {
        // Хедер
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = UiSizes.SpacerSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Назад",
                    tint = CyberColors.TextPrimary
                )
            }
            Text(
                text = "Статистика чтения",
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary
            )
        }

        // Счётчики
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = UiSizes.SpacerSmall),
            horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
        ) {
            StatCard("Сегодня", readToday, Modifier.weight(1f))
            StatCard("За неделю", readWeek, Modifier.weight(1f))
            StatCard("Всего", readTotal, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

        // График по дням
        GlassCard {
            Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                Text(
                    text = "По дням",
                    style = CyberTypography.TitleMedium,
                    color = CyberColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                ) {
                    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    perDay.forEachIndexed { i, count ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height((count * 90 / maxPerDay).dp + 2.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (count > 0) CyberColors.AccentBlue
                                        else CyberColors.SurfaceElevated
                                    )
                            )
                            Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                            Text(
                                text = dayNames[i],
                                style = CyberTypography.BodySmall,
                                color = CyberColors.TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

        // Топ источников
        if (topSources.isNotEmpty()) {
            GlassCard {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                    Text(
                        text = "Чаще всего читаете",
                        style = CyberTypography.TitleMedium,
                        color = CyberColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    topSources.forEach { (source, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = UiSizes.SpacerXSmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = source.removePrefix("https://").removePrefix("www."),
                                style = CyberTypography.BodyMedium,
                                color = CyberColors.TextSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count",
                                style = CyberTypography.BodyMedium,
                                color = CyberColors.AccentBlueBright
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
    }
}

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = UiSizes.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.AccentBlueBright
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
            Text(
                text = label,
                style = CyberTypography.BodySmall,
                color = CyberColors.TextSecondary
            )
        }
    }
}
