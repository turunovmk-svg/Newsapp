package com.mikhail.newsapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.brush.horizontalGradient
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.RoundedCorners
import com.mikhail.newsapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

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

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home)
    object Favorites : Screen("favorites", R.string.nav_favorites, Icons.Filled.Favorite)
    object Shorts : Screen("shorts", R.string.nav_shorts, Icons.Filled.PlayCircle)
    object AddSource : Screen("add_source", R.string.nav_add, Icons.Filled.Add)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)
    companion object { val allScreens = listOf(Home, Favorites, Shorts, AddSource, Settings) }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var appTheme by remember { mutableStateOf(viewModel.getTheme()) }

    val colorScheme = when (appTheme) {
        MainViewModel.AppTheme.SYSTEM -> {
            val isDark = isSystemInDarkTheme()
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        MainViewModel.AppTheme.LIGHT -> lightColorScheme(
            primary = Color(0xFF6366F1), onPrimary = Color.White, primaryContainer = Color(0xFFE0E7FF),
            secondary = Color(0xFFEC4899), background = Color(0xFFFAFAFA), surface = Color.White,
            onSurface = Color(0xFF0F172A), surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF64748B)
        )
        MainViewModel.AppTheme.DARK -> darkColorScheme(
            primary = Color(0xFF818CF8), onPrimary = Color(0xFF1E1B4B), primaryContainer = Color(0xFF312E81),
            secondary = Color(0xFFF472B6), background = Color(0xFF0F172A), surface = Color(0xFF1E293B),
            onSurface = Color(0xFFF8FAFC), surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8)
        )
        MainViewModel.AppTheme.AMOLED -> darkColorScheme(
            primary = Color(0xFFBB86FC), onPrimary = Color(0xFF000000), primaryContainer = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC6), background = Color(0xFF000000), surface = Color(0xFF121212),
            onSurface = Color(0xFFE1E1E1), surfaceVariant = Color(0xFF1E1E1E), onSurfaceVariant = Color(0xFF03DAC6)
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val opmlContent = viewModel.exportSourcesToOpml()
                    if (opmlContent.isNotBlank()) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(opmlContent.toByteArray(StandardCharsets.UTF_8)) }
                        Toast.makeText(context, "Источники экспортированы", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
                    val opmlContent = inputStream.bufferedReader().use { it.readText() }
                    val imported = viewModel.importSourcesFromOpml(opmlContent)
                    Toast.makeText(context, "Импортировано: $imported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка чтения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(bottomBar = { BottomNav(navController) }) { padding ->
                NavHost(navController, Screen.Home.route, Modifier.padding(padding)) {
                    composable(Screen.Home.route) { HomeScreen(viewModel, navController) }
                    composable(Screen.Favorites.route) { FavoritesScreen(viewModel, navController) }
                    composable(Screen.Shorts.route) { ShortsScreen(viewModel, navController) }
                    composable(Screen.AddSource.route) { AddSourceScreen(viewModel) }
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
                            onThemeChange = { appTheme = it; viewModel.setTheme(it) },
                            currentLanguage = viewModel.getAppLanguage(),
                            onLanguageChange = { viewModel.setAppLanguage(it) },
                            onClearCache = { viewModel.clearCache() },
                            onClearReadHistory = { viewModel.clearReadHistory() },
                            readHistoryCount = viewModel.getReadHistory().size
                        )
                    }
                    composable("article_reader") {
                        val post = viewModel.selectedPost.value
                        if (post != null) ArticleReaderScreen(post) { navController.popBackStack() }
                        else navController.popBackStack()
                    }
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
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        Screen.allScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, stringResource(screen.titleRes), modifier = Modifier.size(26.dp)) },
                label = { Text(stringResource(screen.titleRes), fontSize = 11.sp, fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Medium) },
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
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

@Composable
fun HomeScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val postsResult by viewModel.posts.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Box(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.main_events), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.fresh_news), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                IconButton(
                    onClick = { viewModel.loadPosts() },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.refresh), tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Content
            when {
                sources.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Add,
                    title = stringResource(R.string.no_sources),
                    subtitle = stringResource(R.string.no_sources_subtitle),
                    buttonText = stringResource(R.string.add_source),
                    onButtonClick = { navController.navigate(Screen.AddSource.route) }
                )
                postsResult is PostsResult.Loading -> SkeletonList(itemCount = 5)
                postsResult is PostsResult.Error -> ErrorState(
                    message = postsResult.message,
                    onRetry = { viewModel.loadPosts() }
                )
                postsResult is PostsResult.Success -> {
                    val posts = postsResult.posts
                    if (posts.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Newspaper,
                            title = stringResource(R.string.no_news),
                            subtitle = stringResource(R.string.check_connection),
                            buttonText = stringResource(R.string.refresh),
                            onButtonClick = { viewModel.loadPosts() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(items = posts.distinctBy { it.id }, key = { "${it.source.name}_${it.id}" }) { post ->
                                PostCard(post, viewModel, navController)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: UnifiedPost, viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val isFavorite = viewModel.isFavorite(post.id)

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            viewModel.selectPost(post)
            viewModel.markAsRead(post.id)
            navController.navigate("article_reader")
        },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(post.source.emoji, fontSize = 16.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Text(post.source.displayName.takeIf { it.isNotBlank() } ?: "Источник", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text(post.title.takeIf { it.isNotBlank() } ?: "Без названия", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp)
            Spacer(Modifier.height(12.dp))
            Text(post.description.takeIf { it.isNotBlank() } ?: "", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { viewModel.toggleFavorite(post.id) }, modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isFavorite) Color(0xFFFF4757).copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
                        tint = if (isFavorite) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleReaderScreen(post: UnifiedPost, onBack: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }

    val bgColor = if (isDark) 0xFF0F172A else 0xFFFAFAFA
    val textColor = if (isDark) 0xFFF8FAFC else 0xFF0F172A

    val safeTitle = post.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_title)
    val safeDesc = post.description.takeIf { it.isNotBlank() } ?: ""
    val safeEmoji = post.source.emoji.takeIf { it.isNotBlank() } ?: "📰"
    val safeDisplayName = post.source.displayName.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_source)

    Column(modifier = Modifier.fillMaxSize().background(Color(bgColor))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, stringResource(R.string.back), tint = Color(textColor))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(stringResource(R.string.article_reader_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(textColor), modifier = Modifier.weight(1f))
            IconButton(onClick = {
                isSpeaking = !isSpeaking
                try {
                    val tts = android.speech.tts.TextToSpeech(context) { status ->
                        if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                            // TTS ready
                        }
                    }
                    tts.language = java.util.Locale("ru")
                    if (isSpeaking) tts.speak("$safeTitle. $safeDesc", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    else tts.stop()
                } catch (e: Exception) { }
            }) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.tts),
                    tint = Color(textColor)
                )
            }
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
        Button(
            onClick = {
                val safeLink = post.link.takeIf { it.startsWith("http") } ?: "https://example.com"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeLink)))
            },
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.open_original), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val postsResult by viewModel.posts.collectAsState()
    val favorites = viewModel.getFavorites()

    val favPosts = when (postsResult) {
        is PostsResult.Success -> postsResult.posts.filter { it.id in favorites }
        else -> emptyList<UnifiedPost>()
    }

    if (favPosts.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.FavoriteBorder,
            title = stringResource(R.string.no_favorites),
            subtitle = stringResource(R.string.tap_heart),
            buttonText = stringResource(R.string.add_source),
            onButtonClick = { }
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.favorites_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(items = favPosts.distinctBy { it.id }, key = { "${it.source.name}_${it.id}" }) { post ->
                    PostCard(post, viewModel, navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var sourceUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(SourceType.RSS) }
    val sources by viewModel.sources.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.add_source_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        Text(stringResource(R.string.source_type), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceType.values().forEach { type ->
                FilterChip(
                    selected = sourceType == type,
                    onClick = { sourceType = type },
                    label = { Text(type.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.source_url), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sourceUrl,
            onValueChange = { sourceUrl = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(when (sourceType) {
                    SourceType.RSS -> "https://example.com/rss"
                    SourceType.YOUTUBE -> "https://www.youtube.com/feeds/videos.xml?channel_id=..."
                    SourceType.TELEGRAM -> "https://t.me/channelname"
                    SourceType.REDDIT -> "https://www.reddit.com/r/technology/.rss"
                    SourceType.TWITTER -> "https://rsshub.app/twitter/user/username"
                })
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (sourceUrl.isNotBlank()) {
                    if (!sourceUrl.startsWith("http://") && !sourceUrl.startsWith("https://")) {
                        Toast.makeText(context, stringResource(R.string.url_validation_error), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.addSource(sourceType, sourceUrl)
                    sourceUrl = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.add_source_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Popular Sources block (Part 3.2)
        Text(stringResource(R.string.popular_sources), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PopularSourceButton(
                name = "BBC Украина",
                url = "https://feeds.bbci.co.uk/ukrainian/rss.xml",
                type = SourceType.RSS,
                onClick = { viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml") }
            )
            PopularSourceButton(
                name = "Украинская правда",
                url = "https://www.pravda.com.ua/rss/",
                type = SourceType.RSS,
                onClick = { viewModel.addSource(SourceType.RSS, "https://www.pravda.com.ua/rss/") }
            )
            PopularSourceButton(
                name = "Reddit World News",
                url = "https://www.reddit.com/r/worldnews/.rss",
                type = SourceType.REDDIT,
                onClick = { viewModel.addSource(SourceType.REDDIT, "https://www.reddit.com/r/worldnews/.rss") }
            )
            PopularSourceButton(
                name = "YouTube Tech (Google Developers)",
                url = "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw",
                type = SourceType.YOUTUBE,
                onClick = { viewModel.addSource(SourceType.YOUTUBE, "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.my_sources), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (sources.isEmpty()) {
            Text(stringResource(R.string.no_sources_added), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.id }) { source ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${source.type.emoji} ${source.type.displayName}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(source.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            IconButton(onClick = { viewModel.removeSource(source.id) }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopularSourceButton(name: String, url: String, type: SourceType, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(type.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ShortsScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val postsResult by viewModel.posts.collectAsState()

    val shortsPosts = when (postsResult) {
        is PostsResult.Success -> postsResult.posts.take(10)
        else -> emptyList<UnifiedPost>()
    }

    when {
        postsResult is PostsResult.Loading -> {
            SkeletonList(itemCount = 3)
        }
        postsResult is PostsResult.Error -> {
            ErrorState(message = postsResult.message, onRetry = { viewModel.loadPosts() })
        }
        shortsPosts.isEmpty() -> {
            EmptyState(
                icon = Icons.Filled.PlayCircle,
                title = stringResource(R.string.shorts_empty_title),
                subtitle = stringResource(R.string.shorts_empty_subtitle),
                buttonText = stringResource(R.string.add_source),
                onButtonClick = { navController.navigate(Screen.AddSource.route) }
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp, bottom = 80.dp)
            ) {
                items(items = shortsPosts, key = { "${it.source.name}_${it.id}" }) { post ->
                    ShortCard(post, viewModel, navController)
                }
            }
        }
    }
}

@Composable
fun SkeletonList(itemCount: Int = 3) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Box(modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clip(RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(12.dp))
                    repeat(3) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onButtonClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(120.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ErrorOutline, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.retry), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ShortCard(post: UnifiedPost, viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val isFavorite = viewModel.isFavorite(post.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .combinedClickable(
                onClick = {
                    viewModel.selectPost(post)
                    navController.navigate("article_reader")
                },
                onDoubleClick = { viewModel.toggleFavorite(post.id) }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                if (post.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    androidx.compose.foundation.background(
                        modifier = Modifier.fillMaxSize(),
                        brush = androidx.compose.ui.graphics.brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.source.emoji, fontSize = 64.sp)
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = post.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.description.takeIf { it.isNotBlank() } ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { viewModel.toggleFavorite(post.id) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
                            tint = if (isFavorite) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.double_tap_like),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterVertically).padding(start = 8.dp)
                    )
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
    currentTheme: MainViewModel.AppTheme,
    onThemeChange: (MainViewModel.AppTheme) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearReadHistory: () -> Unit,
    readHistoryCount: Int
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.settings_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

        // Sources section
        Text(stringResource(R.string.settings_sources), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Text(stringResource(R.string.sources_count_fmt, sourcesCount), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = onExportSources,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text(stringResource(R.string.export_sources), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onImportSources,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) { Text(stringResource(R.string.import_sources), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) { Text(stringResource(R.string.clear_all_sources), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

        // Delete confirmation dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                confirmButton = {
                    TextButton(onClick = { onClearAllSources(); showDeleteConfirm = false; Toast.makeText(context, stringResource(R.string.sources_deleted), Toast.LENGTH_SHORT).show() }) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
                title = { Text(stringResource(R.string.clear_all_sources)) },
                text = { Text(stringResource(R.string.confirm_delete_all)) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Theme section
        Text(stringResource(R.string.theme_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        listOf(
            MainViewModel.AppTheme.SYSTEM to stringResource(R.string.theme_system),
            MainViewModel.AppTheme.LIGHT to stringResource(R.string.theme_light),
            MainViewModel.AppTheme.DARK to stringResource(R.string.theme_dark),
            MainViewModel.AppTheme.AMOLED to stringResource(R.string.theme_amoled)
        ).forEach { (theme, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeChange(theme) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = currentTheme == theme, onClick = { onThemeChange(theme) })
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language section
        Text(stringResource(R.string.language_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        listOf(
            "system" to stringResource(R.string.language_system),
            "ru" to stringResource(R.string.language_ru),
            "en" to stringResource(R.string.language_en),
            "uk" to stringResource(R.string.language_uk)
        ).forEach { (code, name) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onLanguageChange(code) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = currentLanguage == code, onClick = { onLanguageChange(code) })
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data section
        Text(stringResource(R.string.data_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Button(
            onClick = { onClearCache(); Toast.makeText(context, stringResource(R.string.cache_cleared), Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) { Text(stringResource(R.string.clear_cache), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onClearReadHistory(); Toast.makeText(context, stringResource(R.string.read_history_cleared), Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) { Text("${stringResource(R.string.clear_read_history)} ($readHistoryCount)", fontSize = 14.sp, fontWeight = FontWeight.Medium) }

        Spacer(modifier = Modifier.height(32.dp))

        // About
        Text(stringResource(R.string.about_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Text(stringResource(R.string.app_version), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.app_github), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
