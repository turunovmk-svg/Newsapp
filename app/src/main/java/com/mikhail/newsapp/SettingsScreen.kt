package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistry
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activityResultRegistry = LocalActivityResultRegistry.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                val sources = OpmlManager(context).importFromXml(inputStream)
                if (sources.isNotEmpty()) {
                    val current = viewModel.sources.value
                    val combined = (current + sources).distinctBy { it.url }
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
                val sources = viewModel.sources.value
                if (sources.isNotEmpty()) {
                    OpmlManager(context).exportToXml(sources, outputStream)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            importLauncher.launch(arrayOf("application/xml", "text/xml"))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Импорт OPML")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            exportLauncher.launch("newsapp_subscriptions.opml")
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Экспорт OPML")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text("Назад")
        }
    }
}
