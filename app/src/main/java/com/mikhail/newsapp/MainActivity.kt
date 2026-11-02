package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Feed : Screen("feed", "Лента", Icons.Filled.Article)
    object Shorts : Screen("shorts", "Шортс", Icons.Filled.PlayCircle)
    object YouTube : Screen("youtube", "YouTube", Icons.Filled.PlayArrow)
    object Telegram : Screen("telegram", "Telegram", Icons.Filled.Send)
    object Settings : Screen("settings", "Ещё", Icons.Filled.Settings)
    
    companion object {
        val allScreens = listOf(Feed, Shorts, YouTube, Telegram, Settings)
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Feed.route) { FeedScreen() }
            composable(Screen.Shorts.route) { PlaceholderScreen("🎬 Шортс", "Здесь будет вертикальная лента как в TikTok") }
            composable(Screen.YouTube.route) { PlaceholderScreen("📺 YouTube", "Здесь будут видео из твоих любимых каналов") }
            composable(Screen.Telegram.route) { PlaceholderScreen("✈️ Telegram", "Здесь будут посты из выбранных каналов") }
            composable(Screen.Settings.route) { PlaceholderScreen("⚙️ Ещё", "Здесь можно будет добавить свои источники") }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        Screen.allScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, fontSize = 10.sp, maxLines = 1) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// --- ЭКРАН ЛЕНТЫ НОВОСТЕЙ ---
@Composable
fun FeedScreen() {
    val viewModel = remember { FeedViewModel() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("📰 Главные новости", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(viewModel.newsList) { news ->
            NewsCard(news)
        }
    }
}

// --- КАРТОЧКА НОВОСТИ ---
@Composable
fun NewsCard(news: NewsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            AsyncImage(
                model = news.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.top),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(news.source, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(news.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(modifier = Modifier.height(8.dp))
                Text(news.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = subtitle, fontSize = 16.sp, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
