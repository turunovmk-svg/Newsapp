package com.mikhail.newsapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создание канала уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "news_channel",
                "Новости",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        setContent {
            val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Главная", Icons.Filled.Home)
    object Favorites : Screen("favorites", "Избранное", Icons.Filled.Favorite)
    object Shorts : Screen("shorts", "Шортс", Icons.Filled.PlayCircle)
    object AddSource : Screen("add_source", "Добавить", Icons.Filled.Add)
    object Settings : Screen("settings", "Настройки", Icons.Filled.Settings)

    companion object {
        val allScreens = listOf(Home, Favorites, Shorts, AddSource, Settings)
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                val viewModel: MainViewModel = viewModel()
                HomeScreen(viewModel)
            }
            composable(Screen.Favorites.route) {
                val viewModel: MainViewModel = viewModel()
                FavoritesScreen(viewModel)
            }
            composable(Screen.Shorts.route) {
                ShortsScreen()
            }
            composable(Screen.AddSource.route) {
                val viewModel: MainViewModel = viewModel()
                AddSourceScreen(viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
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

// --- ГЛАВНАЯ (общая лента) ---
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Загрузка новостей...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔥 Главные события",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { viewModel.loadPosts() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Обновить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    UnifiedPostCard(post)
                }
            }
        }
    }
}

// --- ИЗБРАННОЕ ---
object FavoritesRepository {
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    fun toggleFavorite(postId: String) {
        _favorites.value = if (_favorites.value.contains(postId)) {
            _favorites.value - postId
        } else {
            _favorites.value + postId
        }
    }

    fun isFavorite(postId: String): Boolean = _favorites.value.contains(postId)
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    val favorites by FavoritesRepository.favorites.collectAsState()
    val favoritePosts = posts.filter { it.id in favorites }

    if (favoritePosts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Нет избранных постов",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Нажмите на сердечко чтобы добавить",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "❤️ Избранное",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favoritePosts, key = { it.id }) { post ->
                    UnifiedPostCard(post)
                }
            }
        }
    }
}

// --- ДОБАВИТЬ ИСТОЧНИК ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(viewModel: MainViewModel) {
    var sourceUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("RSS") }
    var addedSources by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "➕ Добавить источник",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Тип источника:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("RSS", "YouTube", "Telegram").forEach { type ->
                androidx.compose.material3.FilterChip(
                    selected = sourceType == type,
                    onClick = { sourceType = type },
                    label = { Text(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("URL источника:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sourceUrl,
            onValueChange = { sourceUrl = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(when (sourceType) {
                    "RSS" -> "https://example.com/rss"
                    "YouTube" -> "https://www.youtube.com/feeds/videos.xml?channel_id=..."
                    "Telegram" -> "https://t.me/channel_name"
                    else -> "URL"
                })
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (sourceUrl.isNotBlank()) {
                    addedSources = addedSources + "$sourceType: $sourceUrl"
                    sourceUrl = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Добавить источник",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (addedSources.isNotEmpty()) {
            Text("Добавленные источники:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            addedSources.forEach { source ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(source, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            addedSources = addedSources.filter { it != source }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
}

// --- КАРТОЧКА ПОСТА ---
@Composable
fun UnifiedPostCard(post: UnifiedPost) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (post.videoUrl != null) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(post.videoUrl))
                    )
                } else {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                    )
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.source.emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        post.source.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    post.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { FavoritesRepository.toggleFavorite(post.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (FavoritesRepository.isFavorite(post.id))
                            Icons.Filled.Favorite
                        else
                            Icons.Filled.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (FavoritesRepository.isFavorite(post.id))
                            Color(0xFFFF4757)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- ШОРТСЫ ---
@Composable
fun ShortsScreen() {
    val shortsList = remember { ShortsRepository.getShorts() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(shortsList) { short -> ShortItem(short) }
    }
}

@Composable
fun ShortItem(short: ShortItem) {
    Box(modifier = Modifier.fillMaxWidth().height(700.dp).background(Color.Black)) {
        AsyncImage(
            model = short.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0f, 0f, 0f, 0.8f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 60.dp)
        ) {
            Text(
                text = short.source,
                color = Color(0xFF00D4FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = short.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = short.description,
                color = Color(1f, 1f, 1f, 0.9f),
                fontSize = 14.sp,
                maxLines = 3
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionButton(Icons.Filled.Favorite, "12K")
            ActionButton(Icons.Filled.ChatBubble, "342")
            ActionButton(Icons.Filled.Share, "89")
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Text(text = count, color = Color.White, fontSize = 12.sp)
    }
}

// --- НАСТРОЙКИ ---
@Composable
fun SettingsScreen() {
    var showAddDialog by remember { mutableStateOf(false) }
    var newSourceUrl by remember { mutableStateOf("") }
    var customSources by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("⚙️ Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Версия 1.0", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Создано с ❤️ Михаилом",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text("Мои источники", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showAddDialog = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить RSS")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (customSources.isEmpty()) {
            Text(
                "Нет добавленных источников",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            customSources.forEach { url ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(url, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить RSS источник") },
            text = {
                OutlinedTextField(
                    value = newSourceUrl,
                    onValueChange = { newSourceUrl = it },
                    placeholder = { Text("https://example.com/rss") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newSourceUrl.isNotBlank()) {
                        customSources = customSources + newSourceUrl
                        newSourceUrl = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

data class ShortItem(val id: Int, val title: String, val description: String, val imageUrl: String, val source: String)
object ShortsRepository {
    fun getShorts(): List<ShortItem> = listOf(
        ShortItem(1, "Запуск ракеты SpaceX", "Невероятные кадры старта Starship", "https://images.unsplash.com/photo-1517976487492-5750f3195933?w=800", "SpaceX"),
        ShortItem(2, "Новый iPhone 16", "Первый взгляд на революционный дизайн", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800", "Apple"),
        ShortItem(3, "Марсианские пейзажи", "Фото с поверхности Красной планеты", "https://images.unsplash.com/photo-1614728853913-1e22ba0e982b?w=800", "NASA")
    )
}
