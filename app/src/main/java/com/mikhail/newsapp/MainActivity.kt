package com.mikhail.newsapp

import android.app.Activity
import android.content.Intent
import com.mikhail.newsapp.DataLayer.MainViewModel.AppTheme
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.pointerInput
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.decay
import androidx.compose.animation.core.snapTo
import androidx.compose.animation.core.VectorSpec
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = remember { MainViewModel(context) }
            
            AppNavigation(viewModel = viewModel)
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

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appTheme by viewModel.theme.collectAsState()
    
    val colorScheme = when (appTheme) {
        AppTheme.SYSTEM -> {
            val isDark = isSystemInDarkTheme()
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        AppTheme.LIGHT -> lightColorScheme(
            primary = Color(0xFF6366F1), onPrimary = Color.White, primaryContainer = Color(0xFFE0E7FF),
            secondary = Color(0xFFEC4899), background = Color(0xFFFAFAFA), surface = Color.White,
            onSurface = Color(0xFF0F172A), surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF64748B)
        )
        AppTheme.DARK -> darkColorScheme(
            primary = Color(0xFF818CF8), onPrimary = Color(0xFF1E1B4B), primaryContainer = Color(0xFF312E81),
            secondary = Color(0xFFF472B6), background = Color(0xFF0F172A), surface = Color(0xFF1E293B),
            onSurface = Color(0xFFF8FAFC), surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8)
        )
        AppTheme.AMOLED -> darkColorScheme(
            primary = Color(0xFFBB86FC), onPrimary = Color(0xFF000000), primaryContainer = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC6), background = Color(0xFF000000), surface = Color(0xFF121212),
            onSurface = Color(0xFFE1E1E1), surfaceVariant = Color(0xFF1E1E1E), onSurfaceVariant = Color(0xFF03DAC6)
        )
    }
    
    var exportFileUri by remember { mutableStateOf<Uri?>(null) }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val opmlContent = viewModel.exportSourcesToOpml()
                    if (opmlContent.isNotBlank()) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(opmlContent.toByteArray(Charsets.UTF_8))
                        }
                        Toast.makeText(context, "Источники экспортированы", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Нет источников для экспорта", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
                    val opmlContent = inputStream.bufferedReader().use { it.readText() }
                    val imported = viewModel.importSourcesFromOpml(opmlContent)
                    if (imported >= 0) {
                        Toast.makeText(context, "Импортировано источников: $imported", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Ошибка импорта OPML", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNav(navController) }
    ) { padding ->
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NavHost(navController, Screen.Home.route, Modifier.padding(padding)) {
                    composable(Screen.Home.route) { 
                        HomeScreen(viewModel, navController) 
                    }
                    composable(Screen.Favorites.route) { 
                        FavoritesScreen(viewModel, navController) 
                    }
                    composable(Screen.Shorts.route) { ShortsScreen() }
                    composable(Screen.AddSource.route) { 
                        AddSourceScreen(viewModel) 
                    }
                    composable(Screen.Settings.route) { 
                        SettingsScreen(
                            onExportSources = { 
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    type = "application/xml"
                                    putExtra(Intent.EXTRA_TITLE, "news_app_sources.opml")
                                }
                                exportLauncher.launch(intent)
                            },
                            onImportSources = { 
                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                importLauncher.launch(intent)
                            },
                            onClearAllSources = { 
                                viewModel.clearAllSources()
                                Toast.makeText(context, "Источники удалены", Toast.LENGTH_SHORT).show()
                            },
                            sourcesCount = viewModel.getSources().size,
                            currentTheme = appTheme,
                            onThemeChange = { viewModel.setTheme(it) },
                            currentLanguage = viewModel.getAppLanguage(),
                            onLanguageChange = { viewModel.setAppLanguage(it) },
                            onClearCache = { viewModel.clearCache() },
                            onClearReadHistory = { viewModel.clearReadHistory() },
                            readHistoryCount = viewModel.getReadHistory().size
                        ) 
                    }
                    composable("article_reader") { 
                        val post = viewModel.selectedPost.value
                        if (post != null) {
                            ArticleReaderScreen(post = post, onBack = { navController.popBackStack() })
                        } else {
                            navController.popBackStack()
                        }
                    }
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
    val sources by viewModel.sources.collectAsState()
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
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
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
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(
                    items = posts.distinctBy { it.id },
                    key = { "${it.source.name}_${it.id}" }
                ) { post -> PostCard(post, viewModel, navController) }
            }
        }
    }
}

@Composable
fun PostCard(post: UnifiedPost, viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val isFavorite = viewModel.isFavorite(post.id)
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var swipeOffset by remember { mutableStateOf(0f) }
    var isSwipedRight by remember { mutableStateOf(false) }
    var isSwipedLeft by remember { mutableStateOf(false) }
    
    fun resetCard() {
        launch {
            offsetX.animateTo(0f)
            offsetY.animateTo(0f)
        }
        swipeOffset = 0f
        isSwipedRight = false
        isSwipedLeft = false
    }
    
    fun completeSwipeRight() {
        launch {
            offsetX.animateTo(1000f)
        }
        isSwipedRight = true
        viewModel.toggleFavorite(post.id)
    }
    
    fun completeSwipeLeft() {
        launch {
            offsetX.animateTo(-1000f)
        }
        isSwipedLeft = true
        viewModel.removeSource(post.id)
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { change, dragAmount ->
                            change.consume()
                            offsetX.value = swipeOffset
                            offsetY.value = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset += dragAmount.x
                            offsetX.value = swipeOffset
                            offsetY.value = dragAmount.y
                        },
                        onDragEnd = { change, dragAmount ->
                            change.consume()
                            val velocity = dragAmount.velocity
                            val targetValue = swipeOffset + velocity.x * 1000f
                            if (swipeOffset > 150f) {
                                completeSwipeRight()
                            } else if (swipeOffset < -150f) {
                                completeSwipeLeft()
                            } else {
                                launch {
                                    snapTo(
                                        targetValue = 0f,
                                        initialValue = swipeOffset,
                                        decayThreshold = 0f
                                    ).update(swipeOffset)
                                }
                                offsetX.value = swipeOffset
                                offsetY.value = 0f
                            }
                        }
                    )
                }
                .clickable { 
                    viewModel.selectPost(post)
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
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSwipedRight) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF4757), modifier = Modifier.size(32.dp))
                        } else if (isSwipedLeft) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(post.title.takeIf { it.isNotBlank() } ?: "Без названия", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(post.description.takeIf { it.isNotBlank() } ?: "", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, lineHeight = 22.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { viewModel.toggleFavorite(post.id) }, modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isFavorite) Color(0xFFFF4757).copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                            Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        }
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
    val sources = viewModel.getSources()
    
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
    val shorts = remember {
        listOf(
            UnifiedPost(
                id = "shorts_1",
                title = "Новости технологий",
                description = "Новые тенденции в разработке мобильных приложений на 2026 год",
                imageUrl = "",
                source = SourceType.RSS,
                link = "https://example.com/tech-news"
            ),
            UnifiedPost(
                id = "shorts_2",
                title = "Android 16 обновление",
                description = "Что нового в последней версии Android",
                imageUrl = "",
                source = SourceType.YOUTUBE,
                link = "https://example.com/android-16"
            ),
            UnifiedPost(
                id = "shorts_3",
                title = "Компоуз уроки",
                description = "Jetpack Compose для начинающих",
                imageUrl = "",
                source = SourceType.TELEGRAM,
                link = "https://example.com/compose-lessons"
            ),
            UnifiedPost(
                id = "shorts_4",
                title = "Kotlin корутины",
                description = "Асинхронное программирование на Kotlin",
                imageUrl = "",
                source = SourceType.RSS,
                link = "https://example.com/kotlin-coroutines"
            ),
            UnifiedPost(
                id = "shorts_5",
                title = "Дизайн системы",
                description = "Создание дизайн-системы для приложений",
                imageUrl = "",
                source = SourceType.REDDIT,
                link = "https://example.com/design-system"
            )
        )
    }

    if (shorts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет контента для Шортс", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shorts, key = { it.id }) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clickable { viewModel.selectPost(post); navController.navigate("article_reader") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = post.source.emoji,
                                fontSize = 48.sp
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = post.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = post.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleFavorite(post.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isFavorite(post.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (viewModel.isFavorite(post.id)) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.selectPost(post); navController.navigate("article_reader") },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onExportSources: () -> Unit,
    onImportSources: () -> Unit,
    onClearAllSources: () -> Unit,
    sourcesCount: Int,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearReadHistory: () -> Unit,
    readHistoryCount: Int
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        
        Text("Источники", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Text("Количество источников: $sourcesCount", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onExportSources,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Экспортировать источники (OPML)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onImportSources,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("Импортировать источники (OPML)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onClearAllSources,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("Удалить все источники", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Тема оформления", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        
        AppTheme.values().forEach { theme ->
            RadioButton(
                selected = currentTheme == theme,
                onClick = { onThemeChange(theme) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(if (currentTheme == theme) 1f else 0.3f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        when (theme) {
                            AppTheme.SYSTEM -> "Системная"
                            AppTheme.LIGHT -> "Светлая"
                            AppTheme.DARK -> "Тёмная"
                            AppTheme.AMOLED -> "AMOLED"
                        },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Язык", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        
        listOf(
            Pair("system", "Системный"),
            Pair("ru", "Русский"),
            Pair("en", "Английский"),
            Pair("uk", "Украинский")
        ).forEach { (code, name) ->
            RadioButton(
                selected = currentLanguage == code,
                onClick = { onLanguageChange(code) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(if (currentLanguage == code) 1f else 0.3f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Данные", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        
        Button(
            onClick = onClearCache,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Очистить кэш", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onClearReadHistory,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Очистить историю прочтения ($readHistoryCount)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("О приложении", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Text("NewsApp v1.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("GitHub: turunovmk-svg/Newsapp", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Политика конфиденциальности", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    }
}