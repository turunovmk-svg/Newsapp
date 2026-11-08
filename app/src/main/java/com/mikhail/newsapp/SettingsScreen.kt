package com.mikhail.newsapp

import android.os.Build
import androidx.core.content.ContextCompat
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onOpenStats: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()
    val readingFont by viewModel.readingFont.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()
    val notifEnabled by viewModel.notifEnabled.collectAsState()
    val notifIntervalHours by viewModel.notifIntervalHours.collectAsState()
    val notifSound by viewModel.notifSound.collectAsState()
    val quietStart by viewModel.quietStart.collectAsState()
    val quietEnd by viewModel.quietEnd.collectAsState()
    val notifDisabledSources by viewModel.notifDisabledSources.collectAsState()

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(context, granted)
    }

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

                    // Шрифт чтения
                    Text(
                        text = "Шрифт статьи",
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                    ) {
                        listOf("sans" to "Без засечек", "serif" to "С засечками").forEach { (key, label) ->
                            val selected = readingFont == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) CyberColors.AccentBlue
                                        else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { viewModel.setReadingFont(key) }
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

                    // Тема чтения
                    Text(
                        text = "Тема чтения",
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                    ) {
                        listOf(
                            "light" to "Светлая",
                            "dark" to "Тёмная",
                            "sepia" to "Сепия"
                        ).forEach { (key, label) ->
                            val selected = readingTheme == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) CyberColors.AccentBlue
                                        else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { viewModel.setReadingTheme(key) }
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
                            .clickable { onOpenStats() }
                            .padding(vertical = UiSizes.SpacerSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_view_cards),
                            contentDescription = null,
                            tint = CyberColors.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                        Text(
                            text = "Статистика чтения",
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.TextPrimary
                        )
                    }
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

        // ---------- Уведомления ----------
        item { SectionTitle("Уведомления") }
        item {
            GlassCard {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Уведомления о новых статьях",
                                style = CyberTypography.TitleMedium,
                                color = CyberColors.TextPrimary
                            )
                            Text(
                                text = "Фоновая проверка лент и пуш при обновлениях",
                                style = CyberTypography.BodySmall,
                                color = CyberColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(context, enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberColors.TextInverse,
                                checkedTrackColor = CyberColors.AccentBlue,
                                uncheckedThumbColor = CyberColors.TextSecondary,
                                uncheckedTrackColor = CyberColors.SurfaceElevated
                            )
                        )
                    }
                    if (notifEnabled) {
                        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                        Text(
                            text = "Проверять каждые:",
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                        ) {
                            listOf(1, 3, 6, 12).forEach { h ->
                                val selected = notifIntervalHours == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) CyberColors.AccentBlue
                                            else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable { viewModel.setNotifIntervalHours(context, h) }
                                        .padding(vertical = UiSizes.SpacerSmall),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$h ч",
                                        style = CyberTypography.BodySmall,
                                        color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                        // Звук уведомлений
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Звук уведомлений",
                                style = CyberTypography.BodyMedium,
                                color = CyberColors.TextPrimary
                            )
                            Switch(
                                checked = notifSound,
                                onCheckedChange = { viewModel.setNotifSound(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberColors.TextInverse,
                                    checkedTrackColor = CyberColors.AccentBlue,
                                    uncheckedThumbColor = CyberColors.TextSecondary,
                                    uncheckedTrackColor = CyberColors.SurfaceElevated
                                )
                            )
                        }

                        // Тихие часы
                        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                        Text(
                            text = "Тихие часы: " + if (quietStart >= 0) formatMinutes(quietStart) + " – " + formatMinutes(quietEnd) else "выключены",
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
                        ) {
                            listOf(
                                "23–7" to (23 * 60 to 7 * 60),
                                "22–8" to (22 * 60 to 8 * 60),
                                "0–8" to (0 to 8 * 60)
                            ).forEach { (label, range) ->
                                val selected = quietStart >= 0 && quietStart == range.first && quietEnd == range.second
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) CyberColors.AccentBlue
                                            else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            if (selected) viewModel.setQuietHours(-1, -1)
                                            else viewModel.setQuietHours(range.first, range.second)
                                        }
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
                    }
                }
            }
        }

        // ---------- Источники ----------
        item {
            SectionTitle("Источники (${sources.size})")
        }
        items(
            items = sources,
            key = { it.url }
        ) { source ->
            var showFolderMenu by remember(source) { mutableStateOf(false) }
            val allFolders = remember(sources) {
                sources.map { it.folder }.filter { it.isNotBlank() }.distinct().sorted()
            }
            SourceFolderCard(
                source = source,
                allFolders = allFolders,
                showFolderMenu = showFolderMenu,
                onFolderMenuToggle = { showFolderMenu = it },
                onFolderSelected = { folder ->
                    viewModel.setSourceFolder(source.url, folder)
                    showFolderMenu = false
                },
                onNotifyToggle = { viewModel.toggleSourceNotify(source.url) },
                onRemove = { viewModel.removeSource(source.url) },
                notifDisabledSources = notifDisabledSources,
            )
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

/** Карточка источника с возможностью смены папки. */
@Composable
private fun SourceFolderCard(
    source: com.mikhail.newsapp.data.Source,
    allFolders: List<String>,
    showFolderMenu: Boolean,
    onFolderMenuToggle: (Boolean) -> Unit,
    onFolderSelected: (String) -> Unit,
    onNotifyToggle: () -> Unit,
    onRemove: () -> Unit,
    notifDisabledSources: Set<String>,
) {
    val currentFolder = source.folder.takeIf { it.isNotBlank() } ?: "—"
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
                // Папка
                Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = "Папка",
                        tint = CyberColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(UiSizes.SpacerXSmall))
                    Text(
                        text = "Папка: $currentFolder",
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                    IconButton(onClick = { onFolderMenuToggle(true) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "Сменить папку",
                            tint = CyberColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            // Переключатель уведомлений для источника
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onNotifyToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = if (source.url in notifDisabledSources) "Включить уведомления" else "Отключить уведомления",
                    tint = if (source.url in notifDisabledSources) CyberColors.TextMuted else CyberColors.AccentBlueBright,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { onRemove() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "Удалить источник",
                    tint = CyberColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Выпадающее меню смены папки
        DropdownMenu(
            expanded = showFolderMenu,
            onDismissRequest = { onFolderMenuToggle(false) }
        ) {
            DropdownMenuItem(
                text = { Text(text = "Без папки", style = CyberTypography.BodyMedium) },
                onClick = { onFolderSelected("") }
            )
            allFolders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(text = folder, style = CyberTypography.BodyMedium) },
                    onClick = { onFolderSelected(folder) }
                )
            }
        }
    }
}

/** Форматирует минуты от начала суток в «ЧЧ:ММ». */
private fun formatMinutes(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return String.format("%02d:%02d", h, m)
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
