package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mikhail.newsapp.theme.CyberTheme
import com.mikhail.newsapp.theme.CyberColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Favorite as FilledFavorite
import androidx.compose.material.icons.outlined.Favorite as OutlinedFavorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mikhail.newsapp.data.Post
import coil.compose.AsyncImage
import coil.request.ImageRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberTheme {
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
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NewsBottomNavBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_source") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Добавить источник",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onAddSource = { navController.navigate("add_source") },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("shorts") {
                ShortsScreen(viewModel = viewModel, onAddSource = { navController.navigate("add_source") })
            }
            composable("add_source") {
                AddSourceScreen(viewModel = viewModel, onNavigateBack = { navController.navigate("home") })
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.navigate("home") }
                )
            }
            composable("favorites") {
                FavoritesScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NewsBottomNavBar(navController: NavHostController) {
    val selectedScreenState = remember { mutableStateOf("home") }
    var selectedScreen by selectedScreenState

    NavigationBar {
        NavigationBarItem(
            selected = selectedScreen == "home",
            onClick = {
                selectedScreen = "home"
                navController.navigate("home") { launchSingleTop = true }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "Главная"
                )
            },
            label = { Text("Главная") }
        )
        NavigationBarItem(
            selected = selectedScreen == "favorites",
            onClick = {
                selectedScreen = "favorites"
                navController.navigate("favorites") { launchSingleTop = true }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_favorite),
                    contentDescription = "Избранное"
                )
            },
            label = { Text("Избранное") }
        )
        NavigationBarItem(
            selected = selectedScreen == "shorts",
            onClick = {
                selectedScreen = "shorts"
                navController.navigate("shorts") { launchSingleTop = true }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shorts),
                    contentDescription = "Шортс"
                )
            },
            label = { Text("Шортс") }
        )
        NavigationBarItem(
            selected = selectedScreen == "add_source",
            onClick = {
                selectedScreen = "add_source"
                navController.navigate("add_source") { launchSingleTop = true }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Добавить"
                )
            },
            label = { Text("Добавить") }
        )
        NavigationBarItem(
            selected = selectedScreen == "settings",
            onClick = {
                selectedScreen = "settings"
                navController.navigate("settings") { launchSingleTop = true }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Настройки"
                )
            },
            label = { Text("Настройки") }
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .padding(UiSizes.ScreenPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (posts.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(UiSizes.ScreenPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (error != null) {
                    Column(
                        modifier = Modifier
                            .padding(UiSizes.ScreenPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.error_loading_news, error ?: ""),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(UiSizes.ShortSpacerMedium))
                        Button(
                            onClick = { viewModel.loadPosts(); viewModel.clearError() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.retry_button))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.empty_shorts),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(UiSizes.ShortSpacerMedium))
                    Button(
                        onClick = onAddSource,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.add_source_button))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts) { post ->
                    ShortCard(
                        post = post,
                        isFavorite = viewModel.isFavorite(post),
                        onFavoriteClick = { viewModel.toggleFavorite(post) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShortCard(
    post: Post,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = UiSizes.ShortCardHorizontalPadding, vertical = UiSizes.ShortCardVerticalPadding)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(UiSizes.ShortCardInnerPadding)) {
            post.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.ShortImageHeight)
                        .clip(RoundedCornerShape(UiSizes.ShortImageCornerRadius))
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.ShortSpacerMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = if (isFavorite) FilledFavorite else OutlinedFavorite,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.ShortSpacerSmall))
            post.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.ShortSpacerSmall))
            post.publishedAt?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val favorites by viewModel.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(UiSizes.ScreenPadding)) {
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(id = R.string.favorites_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(favorites) { post ->
                    ShortCard(
                        post = post,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.toggleFavorite(post) }
                    )
                }
            }
        }
    }
}
// Rebuild fix
