package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import java.net.URL

data class UnifiedPost(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val source: SourceType,
    val link: String,
    val videoUrl: String? = null
)

enum class SourceType(val displayName: String, val emoji: String) {
    RSS("Новости", "📰"),
    YOUTUBE("YouTube", "📺"),
    TELEGRAM("Telegram", "✈️"),
    REDDIT("Reddit", "🔴"),
    TWITTER("Twitter", "🐦")
}

data class SourceItem(
    val id: String,
    val type: SourceType,
    val url: String
)

class AppState {
    var sources: List<SourceItem> = emptyList()
    var posts: List<UnifiedPost> = emptyList()
    var isRefreshing = false
    var selectedPost: UnifiedPost? = null
    var favorites: Set<String> = emptySet()
    
    private val prefs = android.app.Application.registerOnProvideAssistData(nil)  // заглушка, не используется
}

@Composable
fun rememberAppState(context: android.content.Context): AppState {
    return remember(context) {
        AppState().apply {
            // Загружаем источники из SharedPreferences
            val prefs = context.getSharedPreferences("news_app_prefs", android.content.Context.MODE_PRIVATE)
            val json = prefs.getString("sources", "[]") ?: "[]"
            try {
                val gson = com.google.gson.Gson()
                val type = com.google.gson.reflect.TypeToken<List<SourceItem>>() {}.type
                val result = gson.fromJson(json, type) as? List<SourceItem>
                if (result != null) sources = result
            } catch (e: Exception) {
                // игнорируем ошибку чтения
            }
            favorites = prefs.getStringSet("favorites", emptySet())?.toMutableSet()?.toSet() ?: emptySet()
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val appState = rememberAppState(context)
    val navController = rememberNavController()
    
    // Состояние для отображения
    val posts by remember(appState.posts) { mutableStateListOf<UnifiedPost>().apply { addAll(appState.posts) } }
    val sources by remember(appState.sources) { mutableStateListOf<SourceItem>().apply { addAll(appState.sources) } }
    val isRefreshing by remember { mutableStateOf(false) }
    
    fun loadPosts() {
        isRefreshing = true
        val currentSources = appState.sources
        if (currentSources.isEmpty()) {
            appState.posts = emptyList()
            posts.clear()
            isRefreshing = false
            return
        }
        
        // Запускаем загрузку в фоновом потоке
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val allPosts = mutableListOf<UnifiedPost>()
                val gson = com.google.gson.Gson()
                
                for (source in currentSources) {
                    try {
                        if (source.url.isNullOrBlank() || !source.url.startsWith("http")) continue
                        
                        val encodedUrl = java.net.URLEncoder.encode(source.url, "UTF-8")
                        val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=$encodedUrl"
                        val url = java.net.URL(apiUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.requestMethod = "GET"
                        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
                        
                        if (connection.responseCode != 200) {
                            connection.disconnect()
                            continue
                        }
                        
                        val json = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()
                        
                        if (json.isNullOrBlank()) continue
                        
                        val root = gson.fromJson(json, Map::class.java)
                        if (root == null || root["status"] as? String != "ok") continue
                        
                        val items = root["items"] as? List<Map<String, Any>> ?: emptyList()
                        
                        for (item in items.take(5)) {
                            try {
                                val title = (item["title"] as? String)?.takeIf { it.isNotBlank() } ?: continue
                                val link = (item["link"] as? String)?.takeIf { it.isNotBlank() } ?: continue
                                val description = (item["description"] as? String) ?: ""
                                val thumbnail = (item["thumbnail"] as? String) ?: ""
                                
                                val rawImageUrl = thumbnail ?: extractImageUrlFromHtml(description)
                                val imageUrl = if (!rawImageUrl.isNullOrEmpty() && rawImageUrl.startsWith("http")) {
                                    rawImageUrl.trim().replace(" ", "%20")
                                } else {
                                    getFallbackImage(source.type)
                                }
                                
                                val postId = (item["guid"] as? String) ?: link
                                
                                allPosts.add(UnifiedPost(
                                    id = postId,
                                    title = title,
                                    description = description.replace(Regex("<[^>]*>"), "").take(150),
                                    imageUrl = imageUrl,
                                    source = source.type,
                                    link = link
                                ))
                            } catch (e: Exception) {
                                // пропускаем проблемный элемент
                            }
                        }
                    } catch (e: Exception) {
                        // пропускаем проблемный источник
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    appState.posts = allPosts.distinctBy { it.id }.shuffled()
                    posts.clear()
                    posts.addAll(appState.posts)
                    isRefreshing = false
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isRefreshing = false
                }
            }
        }
    }
    
    fun addSource(type: SourceType, url: String) {
        val finalUrl = if (type == SourceType.TELEGRAM) convertTelegramUrl(url) else url
        val newSource = SourceItem("${type.name}_${System.currentTimeMillis()}", type, finalUrl)
        appState.sources = appState.sources + newSource
        sources.clear()
        sources.addAll(appState.sources)
        saveSources()
        loadPosts()
    }
    
    fun removeSource(sourceId: String) {
        appState.sources = appState.sources.filter { it.id != sourceId }
        sources.clear()
        sources.addAll(appState.sources)
        saveSources()
        loadPosts()
    }
    
    fun saveSources() {
        val prefs = context.getSharedPreferences("news_app_prefs", android.content.Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = gson.toJson(appState.sources)
        prefs.edit().putString("sources", json).apply()
    }
    
    fun saveFavorites() {
        val prefs = context.getSharedPreferences("news_app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("favorites", appState.favorites).apply()
    }
    
    fun toggleFavorite(postId: String) {
        val newFav = if (appState.favorites.contains(postId)) appState.favorites - postId else appState.favorites + postId
        appState.favorites = newFav
        saveFavorites()
    }
    
    fun isFavorite(postId: String): Boolean = appState.favorites.contains(postId)
    
    fun selectPost(post: UnifiedPost) {
        appState.selectedPost = post
        navController.navigate("article_reader")
    }
    
    // Загружаем посты при первом открытии если есть источники
    LaunchedEffect(Unit) {
        if (appState.sources.isNotEmpty() && appState.posts.isEmpty()) {
            loadPosts()
        }
    }
    
    Scaffold(bottomBar = { BottomNav(navController) }) { padding ->
        NavHost(navController, Screen.Home.route, Modifier.padding(padding)) {
            composable(Screen.Home.route) { 
                HomeScreen(posts, sources, isRefreshing, ::loadPosts, ::selectPost, ::toggleFavorite, ::isFavorite, navController) 
            }
            composable(Screen.Favorites.route) { 
                FavoritesScreen(posts, appState.favorites, ::selectPost, ::toggleFavorite, ::isFavorite, navController) 
            }
            composable(Screen.Shorts.route) { ShortsScreen() }
            composable(Screen.AddSource.route) { 
                AddSourceScreen(::addSource, appState.sources, ::removeSource) 
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable("article_reader") { 
                val post = appState.selectedPost
                if (post != null) {
                    ArticleReaderScreen(post, ::navController.popBackStack)
                } else {
                    navController.popBackStack()
                }
            }
        }
    }
}

@Composable
fun BottomNav(navController: androidx.navigation.NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), ambientColor = Color.Black.copy(0.08f), spotColor = Color.Black.copy(0.08f))
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        Screen.allScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(26.dp)) },
                label = { Text(screen.title, fontSize = 11.sp, fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Medium) },
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            )
        }
    }
}

@Composable
fun HomeScreen(
    posts: List<UnifiedPost>,
    sources: List<SourceItem>,
    isRefreshing: Boolean,
    onLoadPosts: () -> Unit,
    onSelectPost: (UnifiedPost) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onIsFavorite: (String) -> Boolean,
    navController: androidx.navigation.NavController
) {
    if (sources.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(32.dp))
                Text("Добавьте источники", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Перейдите во вкладку \"Добавить\" и укажите RSS, YouTube или Telegram", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else if (posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Загрузка новостей...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("🔥 Главные события", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Свежие новости из ваших источников", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                IconButton(onClick = onLoadPosts, modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Update", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(
                    items = posts.distinctBy { it.id },
                    key = { "${it.source.name}_${it.id}" }
                ) { post -> PostCard(post, onSelectPost, onToggleFavorite, onIsFavorite, navController) }
            }
        }
    }
}

@Composable
fun PostCard(
    post: UnifiedPost,
    onSelectPost: (UnifiedPost) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onIsFavorite: (String) -> Boolean,
    navController: androidx.navigation.NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { 
            onSelectPost(post)
            navController.navigate("article_reader") 
        },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(post.source.emoji, fontSize = 16.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(post.source.displayName.takeIf { it.isNotBlank() } ?: "Источник", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                Text(post.title.takeIf { it.isNotBlank() } ?: "Без названия", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp)
                Spacer(Modifier.height(12.dp))
                Text(post.description.takeIf { it.isNotBlank() } ?: "", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, lineHeight = 22.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { onToggleFavorite(post.id) }, modifier = Modifier.size(44.dp).clip(CircleShape).background(if (onIsFavorite(post.id)) Color(0xFFFF4757).copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                        Icon(imageVector = if (onIsFavorite(post.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Favorite", tint = if (onIsFavorite(post.id)) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleReaderScreen(post: UnifiedPost, onBack: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    
    val bgColor = if (isDark) 0xFF0F172A else 0xFFFAFAFA
    val textColor = if (isDark) 0xFFF8FAFC else 0xFF0F172A
    
    val safeTitle = post.title.takeIf { it.isNotBlank() } ?: "Без названия"
    val safeDesc = post.description.takeIf { it.isNotBlank() } ?: ""
    val safeEmoji = post.source.emoji.takeIf { it.isNotBlank() } ?: "📰"
    val safeDisplayName = post.source.displayName.takeIf { it.isNotBlank() } ?: "Источник"
    
    Column(modifier = Modifier.fillMaxSize().background(Color(bgColor))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { 
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = Color(textColor)) 
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Чтение", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(textColor), modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(safeTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(textColor), modifier = Modifier.padding(horizontal = 20.dp))
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(safeEmoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(safeDisplayName, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(safeDesc, fontSize = 16.sp, color = Color(textColor), lineHeight = 24.sp, modifier = Modifier.padding(horizontal = 20.dp))
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Button(onClick = { 
            val safeLink = post.link.takeIf { it.startsWith("http") } ?: "https://example.com"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeLink))) 
        }, modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Открыть оригинальный источник", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun FavoritesScreen(
    posts: List<UnifiedPost>,
    favorites: Set<String>,
    onSelectPost: (UnifiedPost) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onIsFavorite: (String) -> Boolean,
    navController: androidx.navigation.NavController
) {
    val favPosts = posts.filter { it.id in favorites }
    
    if (favPosts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.FavoriteBorder, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(32.dp))
                Text("Нет избранных постов", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Нажмите на сердечко, чтобы добавить", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Text("❤️ Избранное", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(
                    items = favPosts.distinctBy { it.id },
                    key = { "${it.source.name}_${it.id}" }
                ) { post -> PostCard(post, onSelectPost, onToggleFavorite, onIsFavorite, navController) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(
    onAddSource: (SourceType, String) -> Unit,
    sources: List<SourceItem>,
    onRemoveSource: (String) -> Unit
) {
    var sourceUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(SourceType.RSS) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("➕ Добавить источник", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        Text("Тип источника:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceType.values().forEach { type ->
                FilterChip(selected = sourceType == type, onClick = { sourceType = type }, label = { Text(type.displayName) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("URL источника:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(when(sourceType) { SourceType.RSS -> "https://example.com/rss"; SourceType.YOUTUBE -> "https://www.youtube.com/feeds/videos.xml?channel_id=..."; SourceType.TELEGRAM -> "https://t.me/channelname или https://rsshub.app/telegram/channel/..."; SourceType.REDDIT -> "https://www.reddit.com/r/technology/.rss"; SourceType.TWITTER -> "https://rsshub.app/twitter/user/username" }) }, singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { if (sourceUrl.isNotBlank()) { onAddSource(sourceType, sourceUrl); sourceUrl = "" } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("Добавить источник", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Мои источники:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (sources.isEmpty()) {
            Text("Нет добавленных источников", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.id }) { source ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${source.type.emoji} ${source.type.displayName}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(source.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            IconButton(onClick = { onRemoveSource(source.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🎬 Шортс (в разработке)", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SettingsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("️ Настройки (в разработке)", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}

// Вспомогательные функции
fun extractImageUrlFromHtml(html: String?): String? {
    if (html.isNullOrEmpty()) return null
    return try {
        Regex("""<img[^>]+src=["']([^"']+)["']""")
            .find(html)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.startsWith("http") }
    } catch (e: Exception) { null }
}

fun getFallbackImage(type: SourceType): String {
    return when (type) {
        SourceType.RSS -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800"
        SourceType.YOUTUBE -> "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800"
        SourceType.TELEGRAM -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800"
        SourceType.REDDIT -> "https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?w=800"
        SourceType.TWITTER -> "https://images.unsplash.com/photo-1611605698335-8b85b3781d76?w=800"
    }
}

fun convertTelegramUrl(url: String): String {
    return try {
        if (url.contains("t.me/")) {
            val channelName = url.substringAfter("t.me/")
                .takeIf { it.isNotBlank() }
                ?.takeWhile { it != '/' && it != '?' }
                ?.trim()
                ?: url
            "https://rsshub.app/telegram/channel/$channelName"
        } else {
            url
        }
    } catch (e: Exception) {
        url
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark = isSystemInDarkTheme()
            val lightScheme = lightColorScheme(
                primary = Color(0xFF6366F1), onPrimary = Color.White, primaryContainer = Color(0xFFE0E7FF),
                secondary = Color(0xFFEC4899), background = Color(0xFFFAFAFA), surface = Color.White,
                onSurface = Color(0xFF0F172A), surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF64748B)
            )
            val darkScheme = darkColorScheme(
                primary = Color(0xFF818CF8), onPrimary = Color(0xFF1E1B4B), primaryContainer = Color(0xFF312E81),
                secondary = Color(0xFFF472B6), background = Color(0xFF0F172A), surface = Color(0xFF1E293B),
                onSurface = Color(0xFFF8FAFC), surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8)
            )
            MaterialTheme(colorScheme = if (isDark) darkScheme else lightScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainAppContent()
                }
            }
        }
    }
}