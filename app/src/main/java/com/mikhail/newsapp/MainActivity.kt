package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

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
    companion object { val allScreens = listOf(Home, Favorites, Shorts, AddSource, Settings) }
}

object ArticleNavigator {
    var currentPost: UnifiedPost? = null
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BottomNav(navController) }) { padding ->
        NavHost(navController, Screen.Home.route, Modifier.padding(padding)) {
            composable(Screen.Home.route) { 
                val viewModel: MainViewModel = viewModel()
                HomeScreen(viewModel, navController) 
            }
            composable(Screen.Favorites.route) { 
                val viewModel: MainViewModel = viewModel()
                FavoritesScreen(viewModel, navController) 
            }
            composable(Screen.Shorts.route) { ShortsScreen() }
            composable(Screen.AddSource.route) { 
                val viewModel: MainViewModel = viewModel()
                AddSourceScreen(viewModel) 
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable("article_reader/{postId}") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId")
                val viewModel: MainViewModel = viewModel()
                val posts by viewModel.posts.collectAsState()
                val post = posts.find { it.id == postId }
                
                if (post != null) {
                    ArticleReaderScreen(post = post, onBack = { navController.popBackStack() })
                } else {
                    navController.popBackStack()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val posts by viewModel.posts.collectAsState()
    val sources = viewModel.getSources()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
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
                IconButton(onClick = { viewModel.loadPosts() }, modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Update", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadPosts() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(
                        items = posts.distinctBy { it.id },
                        key = { "${it.source.name}_${it.id}" }
                    ) { post -> PostCard(post, viewModel, navController) }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: UnifiedPost, viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { 
            navController.navigate("article_reader/${post.id}") 
        },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(model = post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)), contentScale = ContentScale.Crop, placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant), error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant))
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(post.source.emoji, fontSize = 16.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(post.source.displayName, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                Text(post.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp)
                Spacer(Modifier.height(12.dp))
                Text(post.description, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, lineHeight = 22.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { viewModel.toggleFavorite(post.id) }, modifier = Modifier.size(44.dp).clip(CircleShape).background(if (viewModel.isFavorite(post.id)) Color(0xFFFF4757).copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                        Icon(imageVector = if (viewModel.isFavorite(post.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Favorite", tint = if (viewModel.isFavorite(post.id)) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
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
    
    val bgColor = if (isDark) "#0F172A" else "#FAFAFA"
    val textColor = if (isDark) "#F8FAFC" else "#0F172A"
    val linkColor = "#6366F1"

    val htmlContent = """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: $bgColor; color: $textColor; line-height: 1.6; padding: 16px; margin: 0; font-size: 16px; }
                img { max-width: 100%; height: auto; border-radius: 16px; margin: 16px 0; display: block; }
                video, iframe { max-width: 100%; border-radius: 16px; margin: 16px 0; }
                a { color: $linkColor; text-decoration: none; font-weight: bold; }
                p { margin-bottom: 16px; }
                h1 { font-size: 24px; margin-bottom: 8px; }
            </style>
        </head>
        <body>
            <h1>${post.title}</h1>
            <p style="color: #64748B; font-size: 14px; margin-bottom: 24px;">${post.source.emoji} ${post.source.displayName}</p>
            ${post.description}
        </body>
        </html>
    """.trimIndent()

    Column(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(bgColor))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { 
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = androidx.compose.ui.graphics.Color(textColor)) 
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Чтение", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(textColor), modifier = Modifier.weight(1f))
        }
        
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        }, modifier = Modifier.weight(1f).fillMaxWidth())
        
        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.link))) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Открыть оригинальный источник", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val posts by viewModel.posts.collectAsState()
    val favorites = viewModel.getFavorites()
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
                ) { post -> PostCard(post, viewModel, navController) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(viewModel: MainViewModel) {
    var sourceUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(SourceType.RSS) }
    val userSources = viewModel.getSources()
    
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
        Button(onClick = { if (sourceUrl.isNotBlank()) { viewModel.addSource(sourceType, sourceUrl); sourceUrl = "" } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("Добавить источник", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Мои источники:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (userSources.isEmpty()) {
            Text("Нет добавленных источников", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(userSources, key = { it.id }) { source ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${source.type.emoji} ${source.type.displayName}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(source.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            IconButton(onClick = { viewModel.removeSource(source.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error) }
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