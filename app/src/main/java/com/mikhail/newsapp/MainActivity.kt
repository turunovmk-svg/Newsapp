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
import androidx.compose.foundation.lazy.LazyRow
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
import com.mikhail.newsapp.theme.SourceGradientPlaceholder
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
            val settingsViewModel: MainViewModel = viewModel()
            val darkThemeEnabled by settingsViewModel.darkTheme.collectAsState()
            CyberTheme(darkTheme = darkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (darkThemeEnabled) CyberColors.BackgroundDeep else Color(0xFFF7F8FA)
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
    var videoPost by remember { mutableStateOf<Post?>(null) }
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
                        if (isYouTubePost(post)) {
                            videoPost = post
                            navController.navigate("video") { launchSingleTop = true }
                        } else {
                            selectedPost = post
                            navController.navigate("article") { launchSingleTop = true }
                        }
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
                        if (isYouTubePost(post)) {
                            videoPost = post
                            navController.navigate("video") { launchSingleTop = true }
                        } else {
                            selectedPost = post
                            navController.navigate("article") { launchSingleTop = true }
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("shorts") {
                ShortsScreen(
                    viewModel = viewModel,
                    onAddSource = { navController.navigate("add_source") },
                    onArticleClick = { post ->
                        if (isYouTubePost(post)) {
                            videoPost = post
                            navController.navigate("video") { launchSingleTop = true }
                        } else {
                            selectedPost = post
                            navController.navigate("article") { launchSingleTop = true }
                        }
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
                        if (isYouTubePost(post)) {
                            videoPost = post
                            navController.navigate("video") { launchSingleTop = true }
                        } else {
                            selectedPost = post
                            navController.navigate("article") { launchSingleTop = true }
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("video") {
                val post = videoPost
                if (post != null) {
                    YouTubePlayerScreen(
                        post = post,
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    Text(
                        text = "Видео не найдено",
                        style = CyberTypography.HeadlineMedium,
                        color = CyberColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(UiSizes.ScreenPadding)
                    )
                }
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
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues),
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
    val context = LocalContext.current

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
    var filterType by remember { mutableStateOf("Все") }
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

    // Чипы фильтра по типу источника
    val filterOptions = remember(posts) {
        val types = mutableListOf("Все")
        val has = { pred: (Post) -> Boolean -> posts.any(pred) }
        if (has { p -> (p.sourceUrl ?: "").contains("youtube.com") || (p.sourceUrl ?: "").contains("youtu.be") }) types.add("YouTube")
        if (has { p -> (p.sourceUrl ?: "").contains("t.me") }) types.add("Telegram")
        if (has { p -> (p.sourceUrl ?: "").contains("reddit.com") }) types.add("Reddit")
        types.add("Статьи")
        types
    }
    if (filterOptions.size > 2) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerXSmall),
            horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
        ) {
            items(filterOptions.size) { i ->
                val opt = filterOptions[i]
                val selected = filterType == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) CyberColors.AccentBlue
                            else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                        .clickable { filterType = opt }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = opt,
                        style = CyberTypography.BodySmall,
                        color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

    PullToRefreshBox(
        state = refreshState,
        isRefreshing = isLoading,
        onRefresh = { viewModel.loadPosts() },
        modifier = modifier.fillMaxSize(),
    ) {
        fun typeOf(post: Post): String {
            val u = post.sourceUrl ?: ""
            return when {
                u.contains("youtube.com") || u.contains("youtu.be") -> "YouTube"
                u.contains("t.me") -> "Telegram"
                u.contains("reddit.com") -> "Reddit"
                else -> "Статьи"
            }
        }
        val searchedPosts = if (searchQuery.isNotBlank()) {
            posts.filter { post ->
                post.title.contains(searchQuery, ignoreCase = true)
                    || (post.description ?: "").contains(searchQuery, ignoreCase = true)
            }
        } else {
            posts
        }
        val filteredPosts = if (filterType == "Все") searchedPosts
        else searchedPosts.filter { typeOf(it) == filterType }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (filteredPosts.isNotEmpty()) {
                val first = filteredPosts.first()
                item(key = "featured") {
                    FeaturedNewsCard(
                        post = first,
                        isFavorite = viewModel.isFavorite(first),
                        onFavoriteClick = { viewModel.toggleFavorite(first) },
                        onShareClick = {
                            first.sourceUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "$url")
                                }
                                context.startActivity(Intent.createChooser(intent, "Поделиться"))
                            }
                        },
                        onClick = { onArticleClick(first) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerSmall)
                    )
                }
            }
            if (filteredPosts.size > 1) {
                item(key = "section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = UiSizes.ScreenPadding,
                                end = UiSizes.ScreenPadding,
                                top = UiSizes.SpacerMedium,
                                bottom = UiSizes.SpacerXSmall
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CyberColors.AccentBlue)
                        )
                        Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                        Text(
                            text = stringResource(id = R.string.section_more_news, filteredPosts.size - 1),
                            style = CyberTypography.TitleMedium,
                            color = CyberColors.TextSecondary
                        )
                    }
                }
            }
            items(
                items = filteredPosts.drop(1),
                key = { it.id }
            ) { post ->
                val ctx = LocalContext.current
                NewsCard(
                    post = post,
                    isFavorite = viewModel.isFavorite(post),
                    isRead = viewModel.isRead(post),
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

    // Состояние ошибки — показываем под лентой
    if (error != null && posts.isEmpty()) {
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

/** Крупная featured-карточка: изображение с градиентом, заголовок поверх. */
@Composable
fun FeaturedNewsCard(
    post: Post,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    GlassCard(
        modifier = modifier
            .clip(RoundedCornerShape(UiSizes.CardCornerRadiusLarge))
            .clickable(onClick = onClick),
        borderColor = CyberColors.BorderGlassBright,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiSizes.FeaturedImageHeight)
            ) {
                post.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CyberColors.BackgroundDeep.copy(alpha = 0.35f),
                                        CyberColors.BackgroundDeep.copy(alpha = 0.92f)
                                    ),
                                    startY = 120f
                                )
                            )
                    )
                } ?: run {
                    SourceGradientPlaceholder(
                        sourceUrl = post.sourceUrl,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 0.dp,
                    )
                }

                // Чип источника поверх изображения
                post.sourceUrl?.let { url ->
                    val sourceName = url
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .substringBefore('/')
                        .take(26)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(UiSizes.SpacerSmall)
                            .clip(RoundedCornerShape(50))
                            .background(CyberColors.AccentBlue)
                            .padding(horizontal = UiSizes.SpacerSmall, vertical = UiSizes.SpacerXSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sourceName,
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextInverse,
                        )
                    }
                }
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(UiSizes.SpacerSmall)
                            .clip(RoundedCornerShape(50))
                            .background(CyberColors.AccentBlueBright)
                            .padding(UiSizes.SpacerSmall)
                    ) {
                        Text(text = "★", color = CyberColors.TextInverse, style = CyberTypography.BodySmall)
                    }
                }

                // Заголовок поверх градиента (внизу)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(UiSizes.CardPadding)
                ) {
                    Text(
                        text = post.title,
                        style = CyberTypography.HeadlineLarge,
                        color = CyberColors.TextPrimary,
                        maxLines = 3,
                    )
                    post.publishedAt?.let { date ->
                        Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                        Text(
                            text = date,
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Действия featured-карточки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.SpacerSmall),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (isFavorite) CyberColors.AccentBlueBright else CyberColors.TextSecondary
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Поделиться",
                        tint = CyberColors.TextSecondary
                    )
                }
                IconButton(onClick = onClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "Читать",
                        tint = CyberColors.AccentBlue
                    )
                }
            }
        }
    }
}


@Composable
fun NewsCard(
    post: Post,
    isFavorite: Boolean = false,
    isRead: Boolean = false,
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
                // Градиентный плейсхолдер в цветах источника
                SourceGradientPlaceholder(
                    sourceUrl = post.sourceUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.HomeCardMinHeight),
                    cornerRadius = UiSizes.CardCornerRadius,
                )
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
                color = if (isRead) CyberColors.TextSecondary else CyberColors.TextPrimary,
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
                        isRead = viewModel.isRead(post),
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
                        isRead = viewModel.isRead(post),
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
