package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            val customLight = lightColorScheme(
                primary = Color(0xFF6366F1), onPrimary = Color.White, primaryContainer = Color(0xFFE0E7FF),
                secondary = Color(0xFFEC4899), onSecondary = Color.White, secondaryContainer = Color(0xFFFCE7F3),
                background = Color(0xFFFAFAFA), onBackground = Color(0xFF0F172A), surface = Color.White,
                onSurface = Color(0xFF0F172A), surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF64748B)
            )
            val customDark = darkColorScheme(
                primary = Color(0xFF818CF8), onPrimary = Color(0xFF1E1B4B), primaryContainer = Color(0xFF312E81),
                secondary = Color(0xFFF472B6), onSecondary = Color(0xFF500724), secondaryContainer = Color(0xFF831843),
                background = Color(0xFF0F172A), onBackground = Color(0xFFF8FAFC), surface = Color(0xFF1E293B),
                onSurface = Color(0xFFF8FAFC), surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8)
            )
            val isDark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (isDark) customDark else customLight) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
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
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BottomNav(navController) }) { padding ->
        NavHost(navController, Screen.Home.route, Modifier.padding(padding)) {
            composable(Screen.Home.route) { HomeScreen(viewModel()) }
            composable(Screen.Favorites.route) { FavoritesScreen(viewModel()) }
            composable(Screen.Shorts.route) { ShortsScreen() }
            composable(Screen.AddSource.route) { AddSourceScreen(viewModel()) }
            composable(Screen.Settings.route) { SettingsScreen() }
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
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes), modifier = Modifier.size(26.dp)) },
                label = { Text(stringResource(screen.titleRes), fontSize = 11.sp, fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Medium) },
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    if (posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.loading_news), style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.main_events),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.fresh_news),
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
                        Icons.Filled.Refresh,
                        contentDescription = "Update",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadPosts() },
                modifier = Modifier.fillMaxSize()
            ) {
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
}

@Composable
fun UnifiedPostCard(post: UnifiedPost) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { /* TODO: open link */ },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(post.source.emoji, fontSize = 16.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        post.source.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    post.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(12.dp))
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
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(if (FavoritesRepository.isFavorite(post.id)) Color(0xFFFF4757).copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                    ) {
                        Icon(
                            imageVector = if (FavoritesRepository.isFavorite(post.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (FavoritesRepository.isFavorite(post.id)) Color(0xFFFF4757) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    val favorites by FavoritesRepository.favorites.collectAsState()
    val favPosts = posts.filter { it.id in favorites }
    if (favPosts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        Icon(Icons.Filled.FavoriteBorder, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(R.string.no_favorites),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.tap_heart),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Text("❤️ " + stringResource(R.string.nav_favorites), fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favPosts, key = { it.id }) { post -> UnifiedPostCard(post) }
            }
        }
    }
}

@Composable
fun ShortsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.shorts_placeholder), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddSourceScreen(viewModel: MainViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.add_source_placeholder), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.settings_placeholder), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}