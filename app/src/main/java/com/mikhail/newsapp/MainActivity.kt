package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.NeonButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
                    color = CyberColors.BackgroundDeep
                ) {
                    NewsAppNavigator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAppNavigator() {
    val viewModel: MainViewModel = viewModel<MainViewModel>()
    val navController = rememberNavController()
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screenTitle = when (currentRoute) {
        "home" -> "Главная"
        "favorites" -> "Избранное"
        "shorts" -> "Шортс"
        "add_source" -> "Добавить источник"
        "settings" -> "Настройки"
        "search" -> "Поиск"
        "article" -> "Статья"
        else -> "NewsApp"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = CyberTypography.TitleLarge,
                        color = CyberColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberColors.SurfaceGlassStrong,
                    titleContentColor = CyberColors.TextPrimary
                ),
                navigationIcon = {
                    if (currentRoute == "article" || currentRoute == "search") {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Назад",
                                tint = CyberColors.TextPrimary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NewsBottomNavBar(navController = navController, currentRoute = currentRoute)
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = { navController.navigate("add_source") },
                    containerColor = CyberColors.AccentBlue,
                    contentColor = CyberColors.TextInverse
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Добавить источник",
                        tint = CyberColors.TextInverse
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = CyberColors.BackgroundDeep
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
                    modifier = Modifier.padding(paddingValues)
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
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("add_source") {
                AddSourceScreen(viewModel = viewModel, onNavigateBack = { navController.navigate("home") }, modifier = Modifier.padding(paddingValues))
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.navigate("home") },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onArticleClick = { post ->
                        selectedPost = post
                        navController.navigate("article") { launchSingleTop = true }
                    },
                    modifier = Modifier.padding(paddingValues)
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
                        style = CyberTypography.HeadlineMedium,
                        color = CyberColors.TextPrimary,
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
fun NewsBottomNavBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = CyberColors.SurfaceGlassStrong,
        contentColor = CyberColors.TextPrimary
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    launchSingleTop = true
                    popUpTo("home") { inclusive = false }
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "Главная",
                    tint = if (currentRoute == "home") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            },
            label = {
                Text(
                    "Главная",
                    color = if (currentRoute == "home") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "favorites",
            onClick = {
                navController.navigate("favorites") {
                    launchSingleTop = true
                    popUpTo("home") { inclusive = false }
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_favorite),
                    contentDescription = "Избранное",
                    tint = if (currentRoute == "favorites") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            },
            label = {
                Text(
                    "Избранное",
                    color = if (currentRoute == "favorites") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "shorts",
            onClick = {
                navController.navigate("shorts") {
                    launchSingleTop = true
                    popUpTo("home") { inclusive = false }
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shorts),
                    contentDescription = "Шортс",
                    tint = if (currentRoute == "shorts") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            },
            label = {
                Text(
                    "Шортс",
                    color = if (currentRoute == "shorts") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "add_source",
            onClick = {
                navController.navigate("add_source") {
                    launchSingleTop = true
                    popUpTo("home") { inclusive = false }
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Добавить",
                    tint = if (currentRoute == "add_source") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            },
            label = {
                Text(
                    "Добавить",
                    color = if (currentRoute == "add_source") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    launchSingleTop = true
                    popUpTo("home") { inclusive = false }
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Настройки",
                    tint = if (currentRoute == "settings") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            },
            label = {
                Text(
                    "Настройки",
                    color = if (currentRoute == "settings") CyberColors.AccentBlueBright else CyberColors.TextSecondary
                )
            }
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
                text = stringResource(id = R.string.empty_home),
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            Text(
                text = stringResource(id = R.string.add_sources_hint),
                style = CyberTypography.BodyMedium,
                color = CyberColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
            NeonButton(
                onClick = onAddSource,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.add_source_button)
            )
        }
        return
    }

    // Поисковое поле
    var searchQuery by remember { mutableStateOf("") }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerSmall),
        borderColor = CyberColors.BorderGlassBright,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск новостей") },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Поиск",
                    tint = CyberColors.TextSecondary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
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
                val ctx = LocalContext.current
                NewsCard(
                    post = post,
                    isFavorite = viewModel.isFavorite(post),
                    onFavoriteClick = { viewModel.toggleFavorite(post) },
                    onShareClick = {
                        post.sourceUrl?.let { url ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "$url")
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
                        }
                    },
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
                style = CyberTypography.TitleMedium,
                color = CyberColors.Error
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
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
            .clickable(onClick = onClick),
        borderColor = if (isFavorite) CyberColors.AccentBlueBright else CyberColors.BorderGlass,
    ) {
        // Тонкая акцентная линия сверху — визуальный якорь карточки
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (isFavorite) CyberColors.AccentBlueBright
                    else CyberColors.AccentBlue.copy(alpha = 0.5f)
                )
        )
        Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
            // Изображение или типографическая заглушка
            post.imageUrl?.let { imageUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.HomeCardMaxHeight)
                        .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Градиентная подложка снизу изображения
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CyberColors.BackgroundDeep.copy(alpha = 0.45f)
                                    ),
                                    startY = 200f
                                )
                            )
                    )
                }
            } ?: run {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.HomeCardMinHeight)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CyberColors.SurfaceElevated.copy(alpha = 0.5f),
                                    CyberColors.SurfaceGlass.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
                        .padding(UiSizes.SpacerLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📰",
                        style = CyberTypography.HeadlineLarge,
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Text(
                        text = stringResource(id = R.string.no_image),
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

            // Источник (чип) и время
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                post.sourceUrl?.let { url ->
                    val sourceName = url
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .substringBefore('/')
                        .take(30)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CyberColors.AccentBlue.copy(alpha = 0.12f))
                            .padding(horizontal = UiSizes.SpacerSmall, vertical = UiSizes.SpacerXSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sourceName,
                            style = CyberTypography.BodySmall,
                            color = CyberColors.AccentBlueBright,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                post.publishedAt?.let { date ->
                    Text(
                        text = date,
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

            // Заголовок — крупный редакционный
            Text(
                text = post.title,
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

            // Описание (с очисткой HTML-тегов)
            post.description?.let { desc ->
                val cleanDesc = Post.stripHtml(desc)
                if (cleanDesc?.isNotEmpty() == true) {
                    Text(
                        text = cleanDesc ?: "",
                        style = CyberTypography.BodyMedium,
                        color = CyberColors.TextSecondary,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                }
            }

            // Разделитель и действия
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CyberColors.Divider)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (isFavorite) CyberColors.AccentBlueBright else CyberColors.TextSecondary
                    )
                }
                IconButton(
                    onClick = onShareClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Поделиться",
                        tint = CyberColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel, onArticleClick: (Post) -> Unit, modifier: Modifier = Modifier) {
    val favorites by viewModel.favorites.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(UiSizes.ScreenPadding)) {
        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.favorites_empty),
                    style = CyberTypography.HeadlineMedium,
                    color = CyberColors.TextSecondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = favorites,
                    key = { it.id }
                ) { post ->
                    val ctx = LocalContext.current
                    NewsCard(
                        post = post,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.toggleFavorite(post) },
                        onShareClick = {
                            post.sourceUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "$url")
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
                            }
                        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    post: Post,
    onNavigateBack: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onPrevious, onNext) {
                var accumulatedX = 0f
                detectDragGestures(
                    onDragStart = { accumulatedX = 0f },
                    onDrag = { _, dragAmount -> accumulatedX += dragAmount.x },
                    onDragEnd = {
                        if (accumulatedX < -80 && onNext != null) {
                            onNext()
                        } else if (accumulatedX > 80 && onPrevious != null) {
                            onPrevious()
                        }
                        accumulatedX = 0f
                    },
                )
            },
    ) {
        item {
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
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Назад",
                        tint = CyberColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                Column {
                    post.sourceUrl?.let { url ->
                        Text(
                            text = url.replace("https://", "").replace("http://", "").take(40),
                            style = CyberTypography.LabelLarge,
                            color = CyberColors.AccentBlue,
                        )
                    }
                    Text(
                        text = post.title,
                        style = CyberTypography.HeadlineMedium,
                        color = CyberColors.TextPrimary,
                        maxLines = 2,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CyberColors.BorderGlass.copy(alpha = 0.3f))
            )
        }

        item {
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
                        .background(CyberColors.SurfaceElevated)
                        .padding(horizontal = UiSizes.ArticlePadding)
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            }
        }

        item {
            // Основной текст статьи
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.ArticlePadding)
            ) {
                post.description?.let { desc ->
                    val cleanDesc = Post.stripHtml(desc)
                    if (cleanDesc?.isNotEmpty() == true) {
                        Text(
                            text = cleanDesc ?: "",
                            style = CyberTypography.BodyLarge,
                            color = CyberColors.TextPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.article_no_content),
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.TextSecondary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }

        item {
            // Кнопка открытия оригинала
            NeonButton(
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
                    .padding(horizontal = UiSizes.ArticlePadding),
                text = stringResource(id = R.string.open_original)
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }
    }
}
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onArticleClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
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
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Назад",
                    tint = CyberColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
            Text(
                text = "Поиск",
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск по заголовкам и описаниям") },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Поиск",
                    tint = CyberColors.TextSecondary
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
            posts
        }

        if (searchQuery.isNotBlank() && filteredPosts.isEmpty()) {
            Text(
                text = "Ничего не найдено",
                style = CyberTypography.TitleMedium,
                color = CyberColors.TextSecondary,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = filteredPosts,
                    key = { it.id }
                ) { post ->
                    val ctx = LocalContext.current
                    NewsCard(
                        post = post,
                        isFavorite = viewModel.isFavorite(post),
                        onFavoriteClick = { viewModel.toggleFavorite(post) },
                        onShareClick = {
                            post.sourceUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "$url")
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
                            }
                        },
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
// Rebuild fix
