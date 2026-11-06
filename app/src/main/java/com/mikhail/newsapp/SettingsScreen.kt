package com.mikhail.newsapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onImportOpml: () -> Unit,
    onExportOpml: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onImportOpml, modifier = Modifier.fillMaxWidth()) {
            Text("Импорт OPML")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onExportOpml, modifier = Modifier.fillMaxWidth()) {
            Text("Экспорт OPML")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack) {
            Text("Назад")
        }
    }
}