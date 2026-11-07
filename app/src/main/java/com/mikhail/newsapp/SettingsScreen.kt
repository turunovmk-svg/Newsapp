package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.theme.NeonButton

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                val importedSources = OpmlManager(context).importFromXml(inputStream)
                if (importedSources.isNotEmpty()) {
                    val current = viewModel.sources.value
                    val combined = (current + importedSources).distinctBy { it.url }
                    viewModel.saveSources(combined)
                    viewModel.loadPosts()
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            context.contentResolver.openOutputStream(selectedUri)?.use { outputStream ->
                val currentSources = viewModel.sources.value
                if (currentSources.isNotEmpty()) {
                    OpmlManager(context).exportToXml(currentSources, outputStream)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
            .padding(UiSizes.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerMedium)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary,
            )
        }

        item {
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UiSizes.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerMedium)
                ) {
                    Text(
                        text = "OPML",
                        style = CyberTypography.TitleMedium,
                        color = CyberColors.TextPrimary,
                    )
                    Text(
                        text = "Импорт и экспорт списка источников в формате OPML",
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary,
                    )
                    NeonButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/xml", "text/xml"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.import_opml),
                    )
                    NeonButton(
                        onClick = {
                            exportLauncher.launch("newsapp_subscriptions.opml")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.export_opml),
                        primaryColor = CyberColors.AccentBlueBright,
                    )
                }
            }
        }

        item {
            Text(
                text = "Источники (${sources.size})",
                style = CyberTypography.TitleMedium,
                color = CyberColors.TextPrimary,
            )
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
                        Text(
                            text = source.type,
                            style = CyberTypography.LabelLarge,
                            color = CyberColors.AccentBlue,
                        )
                        Text(
                            text = source.url.take(60),
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary,
                            maxLines = 1,
                        )
                    }
                    TextButton(
                        onClick = { viewModel.removeSource(source.url) }
                    ) {
                        Text(
                            text = "Удалить",
                            color = CyberColors.Error,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
            NeonButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.back),
                primaryColor = CyberColors.SurfaceElevated,
            )
        }
    }
}
