package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    val selectedPost by remember { mutableStateOf<Post?>(null) }

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
                    onArticleClick = { post ->
                        selectedPost.value = post
                        navController.navigate("article") { launchSingleTop = true }
                    },
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
                FavoritesScreen(
                    viewModel = viewModel,
                    onArticleClick = { post ->
                        selectedPost.value = post
                        navController.navigate("article") { launchSingleTop = true }
                    }
                )
            }
            composable("article") {
                val post = selectedPost.value
                if (post != null) {
                    ArticleScreen(
                        post = post,
                        onNavigateBack = { navController.popBackStack() },
                    )
                } else {
                    Text(
                        text = "Статья не найдена",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(UiSizes.ScreenPadding)
                    )
                }
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
    onArticleClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val refreshState = rememberPullToRefreshState(refreshed = { viewModel.loadPosts() })

    if (posts.isEmpty() && error == null && !isLoading) {
        // Нет источников — показываем экран с предложением добавить источник
        Column(
            modifier = modifier.fillMaxSize().padding(UiSizes.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.empty_shorts),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            Text(
                text = stringResource(id = R.string.add_sources_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
            Button(
                onClick = onAddSource,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.add_source_button))
            }
        }
        return
    }

    PullToRefreshBox(
        state = refreshState,
        refreshing = isLoading,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = posts,
                key = { it.id }
            ) { post ->
                NewsCard(
                    post = post,
                    isFavorite = viewModel.isFavorite(post),
                    onFavoriteClick = { viewModel.toggleFavorite(post) },
                    onShareClick = { /* TODO: реализовать поделиться */ },
                    onClick = { onArticleClick(post) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerXSmall)
                )
            }
        }
    }

    // Состояние ошибки — показываем поверх ленты, не заменяя её
    if (error != null) {
        androidx.compose.material3.SnackbarHost(
            hostState = remember { SnackbarHostState() }
        ) { data ->
            Snackbar(
                snackbarData = data,
                action = { Text(stringResource(id = R.string.retry_button)) },
                onActionClick = { viewModel.loadPosts(); viewModel.clearError() }
            )
        }
    }
}

@Composable
fun NewsCard(
    post: Post,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
            // Изображение или типографическая заглушка
            post.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.HomeCardMaxHeight)
                        .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } ?: run {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.HomeCardMinHeight)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
                        .padding(UiSizes.SpacerLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📰",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Text(
                        text = stringResource(id = R.string.no_image),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

            // Источник и время
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                post.sourceUrl?.let { url ->
                    Text(
                        text = url.replace("https://", "").replace("http://", "").take(30),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                post.publishedAt?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

            // Заголовок
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

            // Описание
            post.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            }

            // Действия: Избранное и Поделиться
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onShareClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Поделиться",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Карточка без изображения — типографическая заглушка. */
@Composable
fun TypographyCard(
    post: Post,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerXSmall)
            .background(MaterialTheme.colorScheme.surface)
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
    ) {
        Text(
            text = post.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        post.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
            )
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel, onArticleClick: (Post) -> Unit) {
    val favorites by viewModel.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(UiSizes.ScreenPadding)) {
        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.favorites_empty),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = favorites,
                    key = { it.id }
                ) { post ->
                    NewsCard(
                        post = post,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.toggleFavorite(post) },
                        onClick = { onArticleClick(post) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerXSmall)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    post: Post,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Верхняя панель с кнопкой назад
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiSizes.ArticlePadding)
                .height(UiSizes.ButtonHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
            Column {
                post.sourceUrl?.let { url ->
                    Text(
                        text = url.replace("https://", "").replace("http://", "").take(40),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
        }

        Divider(modifier = Modifier.fillMaxWidth())

        // Изображение статьи
        post.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiSizes.ArticleImageMaxHeight)
                    .clip(RoundedCornerShape(UiSizes.ArticleCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = UiSizes.ArticlePadding)
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        }

        // Основной текст статьи
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ArticlePadding)
                .weight(1f)
        ) {
            post.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        // Кнопка открытия оригинала
        Button(
            onClick = { /* TODO: открыть оригинал в браузере */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ArticlePadding)
        ) {
            Text(stringResource(id = R.string.open_original))
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
    }
}
// Rebuild fix
