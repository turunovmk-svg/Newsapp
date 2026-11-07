package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NewsAppNavigator()
                }
            }
        }
    }
}

@Composable
fun NewsAppNavigator() {
    val viewModel: MainViewModel = viewModel<MainViewModel>()
    var screen by remember { mutableStateOf<Screen>(Screen.Shorts) }

    Column(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.Shorts -> ShortsScreen(viewModel = viewModel)
            Screen.AddSource -> AddSourceScreen(viewModel = viewModel, onNavigateBack = { screen = Screen.Shorts })
            Screen.Settings -> SettingsScreen(
                onNavigateBack = { screen = Screen.Shorts },
                onImportOpml = { /* TODO */ },
                onExportOpml = { /* TODO */ }
            )
        }
    }
}

private enum class Screen {
    Shorts, AddSource, Settings
}
ate enum class Screen {
    Shorts, AddSource, Settings
}
