package com.mikhail.newsapp

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.data.SourceType
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.UiSizes

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                val imported = OpmlManager(context).importFromXml(inputStream)
                if (imported.isNotEmpty()) {
                    val current = viewModel.sources.value
                    val combined = (current + imported).distinctBy { it.url }
                    viewModel.saveSources(combined)
                    viewModel.loadPosts()
                }
            }
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            context.contentResolver.openOutputStream(selectedUri)?.use { outputStream ->
                OpmlManager(context).exportToXml(viewModel.sources.value, outputStream)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
            .padding(horizontal = UiSizes.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerMedium)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary,
                modifier = Modifier.padding(top = UiSizes.SpacerMedium)
            )
        }

        // ---------- Внешний вид ----------
        item {
            SectionTitle("Внешний вид")
        }
        item {
            GlassCard {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                    // Размер шрифта
                    Text(
                        text = "Размер шрифта статьи",
                        style = CyberTypography.TitleMedium,
                        color = CyberColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                    ) {
                        listOf(
                            "Мелкий" to 0.9f,
                            "Средний" to 1.0f,
                            "Крупный" to 1.2f
                        ).forEach { (label, scale) ->
                            val selected = fontScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) CyberColors.AccentBlue
                                        else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { viewModel.setFontScale(scale) }
                                    .padding(vertical = UiSizes.SpacerSmall),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = CyberTypography.BodySmall,
                                    color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                    // Тёмная тема
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Тёмная тема",
                                style = CyberTypography.TitleMedium,
                                color = CyberColors.TextPrimary
                            )
                            Text(
                                text = if (darkTheme) "OBSIDIAN — графит и электрический синий"
                                else "Светлая тема",
                                style = CyberTypography.BodySmall,
                                color = CyberColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { viewModel.setDarkTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberColors.TextInverse,
                                checkedTrackColor = CyberColors.AccentBlue,
                                uncheckedThumbColor = CyberColors.TextSecondary,
                                uncheckedTrackColor = CyberColors.SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // ---------- Данные ----------
        item { SectionTitle("Данные") }
        item {
            GlassCard {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.markAllRead() }
                            .padding(vertical = UiSizes.SpacerSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = CyberColors.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                        Text(
                            text = "Отметить всё прочитанным",
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.loadPosts() }
                            .padding(vertical = UiSizes.SpacerSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = CyberColors.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                        Text(
                            text = "Обновить все ленты",
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.TextPrimary
                        )
                    }
                }
            }
        }

        // ---------- OPML ----------
        item { SectionTitle("Импорт и экспорт") }
        item {
            GlassCard {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                    Text(
                        text = "OPML",
                        style = CyberTypography.TitleMedium,
                        color = CyberColors.TextPrimary,
                    )
                    Text(
                        text = "Сохранение и восстановление списка источников",
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                    ) {
                        NeonButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/xml", "text/xml"))
                            },
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.import_opml),
                        )
                        NeonButton(
                            onClick = {
                                exportLauncher.launch("newsapp_subscriptions.opml")
                            },
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.export_opml),
                            primaryColor = CyberColors.AccentBlueBright,
                        )
                    }
                }
            }
        }

        // ---------- Источники ----------
        item {
            SectionTitle("Источники (${sources.size})")
        }
        items(sources.size) { index ->
            val source = sources[index]
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyberColors.BorderGlass,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UiSizes.CardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Чип типа
                        val typeLabel = when (source.type) {
                            SourceType.REDDIT -> "Reddit"
                            SourceType.YOUTUBE -> "YouTube"
                            SourceType.TELEGRAM -> "Telegram"
                            SourceType.TWITTER -> "X (Twitter)"
                            SourceType.GNEWS -> "Сайт"
                            else -> "RSS"
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CyberColors.AccentBlue.copy(alpha = 0.12f))
                                .padding(horizontal = UiSizes.SpacerSmall, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                style = CyberTypography.BodySmall,
                                color = CyberColors.AccentBlueBright,
                            )
                        }
                        Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                        Text(
                            text = when {
                                !source.label.isNullOrBlank() -> source.label
                                source.type == SourceType.GNEWS -> source.url
                                    .substringAfter("site:")
                                    .substringBefore('&')
                                else -> source.url
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                            }.take(60),
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary,
                            maxLines = 2,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.removeSource(source.url) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Удалить источник",
                            tint = CyberColors.Error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            NeonButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.back),
                primaryColor = CyberColors.SurfaceElevated,
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = UiSizes.SpacerSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CyberColors.AccentBlue)
        )
        Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
        Text(
            text = text,
            style = CyberTypography.TitleMedium,
            color = CyberColors.TextSecondary,
        )
    }
}
