package com.mikhail.newsapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URL

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
            // Запрос разрешения на уведомления
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted: Boolean ->
                if (granted) {
                    // Разрешение получено
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            val isSystemInDarkTheme = isSystemInDarkTheme()
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
    object Feed : Screen("feed", "Лента", Icons.Filled.Article)
    object Shorts : Screen("shorts", "Шортс", Icons.Filled.PlayCircle)
    object YouTube : Screen("youtube", "YouTube", Icons.Filled.PlayArrow)
    object Telegram : Screen("telegram", "Telegram", Icons.Filled.Send)
    object Search : Screen("search", "Поиск", Icons.Filled.Search)
    object Favorites : Screen("favorites", "Избранное", Icons.Filled.Bookmark)
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
            composable(Screen.Shorts.route) { ShortsScreen() }
            composable(Screen.YouTube.route) { YouTubeScreen() }
            composable(Screen.Telegram.route) { TelegramScreen() }
            composable(Screen.Search.route) {
                val viewModel: MainViewModel = viewModel()
                SearchScreen(viewModel)
            }
            composable(Screen.Favorites.route) {
                val viewModel: MainViewModel = viewModel()
                FavoritesScreen(viewModel)
            }
            composable("video/{url}") { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                VideoPlayerScreen(videoUrl = url, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
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

// --- ЛЕНТА ---
@Composable
fun FeedScreen() {
    val viewModel: MainViewModel = viewModel()
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔥 Главные события", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.loadPosts() }) {
                Icon(Icons.Filled.Refresh, "Обновить")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                UnifiedPostCard(post, navController = null)
            }
        }
    }
}

// --- ПОИСК ---
@Composable
fun SearchScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredPosts = remember(posts, searchQuery) {
        if (searchQuery.isBlank()) {
            posts
        } else {
            posts.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Поиск новостей...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { })
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredPosts, key = { it.id }) { post ->
                UnifiedPostCard(post, navController = null)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = androidx.compose.foundation.layout.height(16.dp))
                Text(
                    "Нет избранных постов",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(favoritePosts, key = { it.id }) { post ->
                UnifiedPostCard(post, navController = null)
            }
        }
    }
}

// --- ВИДЕОПЛЕЕР ---
@Composable
fun VideoPlayerScreen(videoUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(videoUrl))
                        setMediaController(MediaController(ctx))
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
        }
    }
}

// --- КАРТОЧКА ПОСТА ---
@Composable
fun UnifiedPostCard(post: UnifiedPost, navController: androidx.navigation.NavController? = null) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (post.videoUrl != null) {
                navController?.navigate(
                    "video/${java.net.URLEncoder.encode(post.videoUrl, "UTF-8")}"
                )
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                )
            }
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = android.R.drawable.ic_menu_report_image),
                onError = {
                    android.util.Log.e(
                        "COIL_ERROR",
                        "Failed to load image: ${post.imageUrl} for post: ${post.title}"
                    )
                }
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
                IconButton(onClick = { FavoritesRepository.toggleFavorite(post.id) }) {
                    Icon(
                        imageVector = if (FavoritesRepository.isFavorite(post.id))
                            Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (FavoritesRepository.isFavorite(post.id))
                            Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- YOUTUBE ЭКРАН ---
@Composable
fun YouTubeScreen() {
    val viewModel: MainViewModel = viewModel()
    val posts by viewModel.posts.collectAsState()
    val youtubePosts = posts.filter { it.source == SourceType.YOUTUBE }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "📺 YouTube видео",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(youtubePosts) { post ->
            UnifiedPostCard(post, navController = null)
        }
    }
}

// --- TELEGRAM ЭКРАН ---
@Composable
fun TelegramScreen() {
    val viewModel: MainViewModel = viewModel()
    val posts by viewModel.posts.collectAsState()
    val telegramPosts = posts.filter { it.source == SourceType.TELEGRAM }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "✈️ Telegram каналы",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(telegramPosts) { post ->
            UnifiedPostCard(post, navController = null)
        }
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
        Spacer(modifier = androidx.compose.foundation.layout.height(32.dp))

        Text("Версия 1.0", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = androidx.compose.foundation.layout.height(8.dp))
        Text(
            "Создано с ❤️ Михаилом",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = androidx.compose.foundation.layout.height(32.dp))
        Text("Мои источники", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = androidx.compose.foundation.layout.height(16.dp))

        Button(onClick = { showAddDialog = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить RSS")
        }

        Spacer(modifier = androidx.compose.foundation.layout.height(16.dp))

        if (customSources.isEmpty()) {
            Text(
                "Нет добавленных источников",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            customSources.forEach { url ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(0.8f))
                    )
                )
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
                color = Color.White.copy(0.9f),
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

data class ShortItem(val id: Int, val title: String, val description: String, val imageUrl: String, val source: String)
object ShortsRepository {
    fun getShorts(): List<ShortItem> = listOf(
        ShortItem(1, "Запуск ракеты SpaceX", "Невероятные кадры старта Starship", "https://images.unsplash.com/photo-1517976487492-5750f3195933?w=800", "SpaceX"),
        ShortItem(2, "Новый iPhone 16", "Первый взгляд на революционный дизайн", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800", "Apple"),
        ShortItem(3, "Марсианские пейзажи", "Фото с поверхности Красной планеты", "https://images.unsplash.com/photo-1614728853913-1e22ba0e982b?w=800", "NASA")
    )
}
