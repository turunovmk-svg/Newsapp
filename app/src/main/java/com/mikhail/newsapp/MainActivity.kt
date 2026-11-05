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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
            // Кастомная премиум цветовая палитра
            val customLightColorScheme = lightColorScheme(
                primary = Color(0xFF6366F1),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0E7FF),
                onPrimaryContainer = Color(0xFF312E81),
                secondary = Color(0xFFEC4899),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFCE7F3),
                onSecondaryContainer = Color(0xFF831843),
                background = Color(0xFFFAFAFA),
                onBackground = Color(0xFF0F172A),
                surface = Color.White,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFF1F5F9),
                onSurfaceVariant = Color(0xFF64748B),
                outline = Color(0xFFE2E8F0),
                outlineVariant = Color(0xFFF1F5F9)
            )

            val customDarkColorScheme = darkColorScheme(
                primary = Color(0xFF818CF8),
                onPrimary = Color(0xFF1E1B4B),
                primaryContainer = Color(0xFF312E81),
                onPrimaryContainer = Color(0xFFE0E7FF),
                secondary = Color(0xFFF472B6),
                onSecondary = Color(0xFF500724),
                secondaryContainer = Color(0xFF831843),
                onSecondaryContainer = Color(0xFFFCE7F3),
                background = Color(0xFF0F172A),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF1E293B),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF334155),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF475569),
                outlineVariant = Color(0xFF334155)
            )

            val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme) customDarkColorScheme else customLightColorScheme
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
    object HomeScreen : Screen("home", "Главная", Home)
    object FavoritesScreen : Screen("favorites", "Избранное", Favorite)
    object ShortsScreen : Screen("shorts", "Шортс", PlayCircle)
    object AddSourceScreen : Screen("add_source", "Добавить", Add)
    object SettingsScreen : Screen("settings", "Настройки", Settings)

    companion object {
        val allScreens = listOf(HomeScreen, FavoritesScreen, ShortsScreen, AddSourceScreen, SettingsScreen)
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
            startDestination = Screen.HomeScreen.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.HomeScreen.route) {
                val viewModel: MainViewModel = viewModel()
                HomeScreen(viewModel)
            }
            composable(Screen.FavoritesScreen.route) {
                val viewModel: MainViewModel = viewModel()
                FavoritesScreen(viewModel)
            }
            composable(Screen.ShortsScreen.route) {
                ShortsScreen()
            }
            composable(Screen.AddSourceScreen.route) {
                val viewModel: MainViewModel = viewModel()
                AddSourceScreen(viewModel)
            }
            composable(Screen.SettingsScreen.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Screen.allScreens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "🔥 Главные события",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Свежие новости из ваших источников",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.loadPosts() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Home,
                        contentDescription = "Обновить",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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
                modifier = Modifier.padding(40.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Нет избранных постов",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                FilterChip(
                    selected = sourceType == type,
                    onClick = { sourceType = type },
                    label = {
                        Text(
                            type,
                            fontWeight = if (sourceType == type) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (sourceType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
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
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                "Добавить источник",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
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
                            Icon(Delete, contentDescription = "Удалить")
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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(post.source.emoji, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        post.source.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    post.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    post.description,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { FavoritesRepository.toggleFavorite(post.id) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (FavoritesRepository.isFavorite(post.id))
                                    Color(0xFFFF4757).copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = if (FavoritesRepository.isFavorite(post.id))
                                Favorite
                            else
                                FavoriteBorder,
                            contentDescription = "В избранное",
                            tint = if (FavoritesRepository.isFavorite(post.id))
                                Color(0xFFFF4757)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- ШОРТСЫ ---
@Composable
fun ShortsScreen() {
    val shortsList = remember { ShortsRepository.getShorts() }

    if (shortsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Шорты скоро появятся",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Добавьте YouTube источники в настройках",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(shortsList) { short -> ShortItem(short) }
        }
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
            ActionButton(Favorite, "12K")
            ActionButton(ChatBubble, "342")
            ActionButton(Share, "89")
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
            Icon(Add, contentDescription = null)
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
    fun getShorts(): List<ShortItem> {
        return emptyList() // Убираем тестовые данные
    }
}
