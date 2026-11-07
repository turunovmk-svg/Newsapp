package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.mikhail.newsapp.theme.CyberTheme
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.theme.NeonButton
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
    var selectedPost by remember { mutableStateOf<Post?>(null) }

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
                        selectedPost = post
                        navController.navigate("article") { launchSingleTop = true }
                    },
                    onSearchClick = { navController.navigate("search") },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onArticleClick = { post ->
                        selectedPost = post
                        navController.navigate("article") { launchSingleTop = true }
                    },
                )
            }
            composable("shorts") {
                ShortsScreen(
                    viewModel = viewModel,
                    onAddSource = { navController.navigate("add_source") },
                    onArticleClick = { post ->
                        selectedPost = post
                        navController.navigate("article") { launchSingleTop = true }
                    },
                )
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
                        selectedPost = post
                        navController.navigate("article") { launchSingleTop = true }
                    }
                )
            }
            composable("article") {
                val viewModel: MainViewModel = viewModel<MainViewModel>()
                val posts by viewModel.posts.collectAsState()
                val post = selectedPost
                if (post != null) {
                    val currentIndex = posts.indexOfFirst { it.id == post.id }
                    val hasPrevious = currentIndex > 0
                    val hasNext = currentIndex < posts.size - 1
                    ArticleScreen(
                        post = post,
                        onNavigateBack = { navController.popBackStack() },
                        onPrevious = if (hasPrevious) {
                            {
                                val prevIndex = currentIndex - 1
                                selectedPost = posts[prevIndex]
                            }
                        } else null,
                        onNext = if (hasNext) {
                            {
                                val nextIndex = currentIndex + 1
                                selectedPost = posts[nextIndex]
                            }
                        } else null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddSource: () -> Unit,
    onArticleClick: (Post) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val refreshState = rememberPullToRefreshState()

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

    // Поисковое поле
    var searchQuery by remember { mutableStateOf("") }
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Поиск новостей") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Поиск"
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerSmall),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

    PullToRefreshBox(
        state = refreshState,
        isRefreshing = isLoading,
        onRefresh = { viewModel.loadPosts() },
        modifier = modifier.fillMaxSize(),
    ) {
        val filteredPosts = if (searchQuery.isNotBlank()) {
            posts.filter { post ->
                post.title.contains(searchQuery, ignoreCase = true)
                    || (post.description ?: "").contains(searchQuery, ignoreCase = true)
            }
        } else {
            posts
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = filteredPosts,
                key = { it.id }
            ) { post ->
                NewsCard(
                    post = post,
                    isFavorite = viewModel.isFavorite(post),
                    onFavoriteClick = { viewModel.toggleFavorite(post) },
                    onClick = { onArticleClick(post) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerXSmall)
                )
            }
        }
    }

    // Состояние ошибки — показываем поверх ленты
    if (error != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiSizes.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.error_loading_news, error ?: ""),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            Button(
                onClick = { viewModel.loadPosts(); viewModel.clearError() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.retry_button))
            }
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
    GlassCard(
        modifier = modifier
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius)),
        borderColor = if (isFavorite) CyberColors.AccentBlueBright else CyberColors.BorderGlass,
    ) {
        Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
            // Акцентная полоса слева для избранного
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(CyberColors.AccentBlueBright)
                        .padding(bottom = UiSizes.SpacerMedium)
                )
                Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
            }

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
                        .background(CyberColors.SurfaceElevated.copy(alpha = 0.5f))
                )
            } ?: run {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.HomeCardMinHeight)
                        .background(CyberColors.SurfaceElevated.copy(alpha = 0.5f))
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

            // Заголовок — крупный редакционный
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineMedium,
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
                        tint = if (isFavorite) CyberColors.AccentBlueBright else MaterialTheme.colorScheme.onSurfaceVariant
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
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerXSmall),
    ) {
        Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
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
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        if (dragAmount.x < -80 && onNext != null) {
                            onNext()
                        } else if (dragAmount.x > 80 && onPrevious != null) {
                            onPrevious()
                        }
                    },
                )
            },
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

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )

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
            onClick = {
                post.sourceUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                    }
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ArticlePadding)
        ) {
            Text(stringResource(id = R.string.open_original))
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
    }
}
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onArticleClick: (Post) -> Unit,
) {
    val posts by viewModel.posts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = "Поиск",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск по заголовкам и описаниям") },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Поиск"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        val filteredPosts = if (searchQuery.isNotBlank()) {
            posts.filter { post ->
                post.title.contains(searchQuery, ignoreCase = true)
                    || (post.description ?: "").contains(searchQuery, ignoreCase = true)
            }
        } else {
            emptyList()
        }

        if (searchQuery.isNotBlank() && filteredPosts.isEmpty()) {
            Text(
                text = "Ничего не найдено",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = filteredPosts,
                    key = { it.id }
                ) { post ->
                    NewsCard(
                        post = post,
                        isFavorite = viewModel.isFavorite(post),
                        onFavoriteClick = { viewModel.toggleFavorite(post) },
                        onClick = { onArticleClick(post) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = UiSizes.SpacerXSmall)
                    )
                }
            }
        }
    }
}
// Rebuild fix
