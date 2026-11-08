@file:OptIn(androidx.compose.material3.ExperimentalMaterial3WindowSizeClassApi::class)

package com.mikhail.newsapp

import android.content.Context
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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlin.math.abs
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.mikhail.newsapp.theme.SmartNewsImage
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FabPosition
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalActivity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.ExperimentalMaterial3WindowSizeClassApi
import android.content.res.Configuration
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

/** Подсвечивает вхождения запроса в тексте (жёлтый маркер). */
private fun highlightText(text: String, query: String, markColor: Color): AnnotatedString {
    if (query.isBlank() || text.isEmpty()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    return androidx.compose.ui.text.buildAnnotatedString {
        var pos = 0
        var start = lowerText.indexOf(lowerQuery)
        while (start >= 0) {
            if (start > pos) append(text.substring(pos, start))
            withStyle(SpanStyle(background = markColor.copy(alpha = 0.35f), fontWeight = FontWeight.Bold)) {
                append(text.substring(start, (start + query.length).coerceAtMost(text.length)))
            }
            pos = (start + query.length).coerceAtMost(text.length)
            start = lowerText.indexOf(lowerQuery, pos)
        }
        if (pos < text.length) append(text.substring(pos))
    }
}

/** Русская плюрализация для «новость/новости/новостей». */
private fun pluralNew(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "новостей"
        mod10 == 1 -> "новая статья"
        mod10 in 2..4 -> "новые статьи"
        else -> "новостей"
    }
}

/** Брендированный экран запуска: монограмма на градиенте. */
@Composable
fun NewsSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0F18),
                        CyberColors.BackgroundDeep
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Монограмма в градиентном круге с мягкой пульсацией (pattern interrupt —
            // внимание приковано к бренду в первые 700мс)
            val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
            val pulseScale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CyberColors.AccentBlue,
                                CyberColors.AccentBlueBright
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = CyberColors.TextInverse,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
            Text(
                text = "NewsApp",
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = CyberTypography.BodyMedium,
                color = CyberColors.TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NewsAppNavigator() {
    val viewModel: MainViewModel = viewModel<MainViewModel>()
    val navController = rememberNavController()
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var videoPost by remember { mutableStateOf<Post?>(null) }
    var viewerImageUrl by remember { mutableStateOf<String?>(null) }
    var homeScrollTrigger by remember { mutableStateOf(0) }

    // Splash — брендированный старт
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        showSplash = false
    }
    if (showSplash) {
        NewsSplashScreen()
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Адаптивность: телефон vs планшет, портрет vs ландшафт
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = LocalContext.current as? Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val isTablet = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            style = CyberTypography.TitleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = CyberColors.TextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CyberColors.SurfaceGlassStrong,
                        titleContentColor = CyberColors.TextPrimary
                    )
                )
                // Акцентная линия под топбаром
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyberColors.AccentBlue.copy(alpha = 0.4f),
                                    CyberColors.AccentBlueBright.copy(alpha = 0.55f),
                                    CyberColors.AccentBlue.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        },
        bottomBar = {
            NewsBottomNavBar(
                navController = navController,
                currentRoute = currentRoute,
                onTabRepeat = { route ->
                    when (route) {
                        "shorts" -> viewModel.setCurrentShortsIndex(0)
                        "home" -> homeScrollTrigger = homeScrollTrigger + 1
                        "favorites" -> {}
                        else -> {}
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                var fabPressed by remember { mutableStateOf(false) }
                val fabScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (fabPressed) 0.92f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f),
                    label = "fabScale"
                )
                Box(
                    modifier = Modifier
                        .padding(end = UiSizes.SpacerSmall, bottom = UiSizes.SpacerSmall)
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate("add_source") },
                        containerColor = CyberColors.AccentBlue,
                        contentColor = CyberColors.TextInverse,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 14.dp
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer(scaleX = fabScale, scaleY = fabScale)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Добавить источник",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .widthIn(max = if (isTablet || isLandscape) 900.dp else 10000.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),              HomeScreen(
                            viewModel = viewModel,
                            scrollToTopTrigger = homeScrollTrigger,
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
                            modifier = Modifier.fillMaxWidth(),
                            isTablet = isTablet,
                            isLandscape = isLandscape
                        )
                    }
                }
            }
            composable("search") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .widthIn(max = if (isTablet || isLandscape) 900.dp else 10000.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            composable("shorts") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .widthIn(max = if (isTablet || isLandscape) 900.dp else 10000.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            composable("add_source") {
                AddSourceScreen(viewModel = viewModel, onNavigateBack = { navController.navigate("home") }, modifier = Modifier.padding(paddingValues))
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.navigate("home") },
                    onOpenStats = { navController.navigate("stats") },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            composable("favorites") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .widthIn(max = if (isTablet || isLandscape) 900.dp else 10000.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            composable("stats") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .widthIn(max = if (isTablet || isLandscape) 900.dp else 10000.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val posts by viewModel.posts.collectAsState()
                        StatsScreen(
                            posts = posts,
                            onNavigateBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            composable("image") {
                val url = viewerImageUrl
                if (url != null) {
                    ImageViewerScreen(
                        imageUrl = url,
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
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
                        onImageClick = { url ->
                            viewerImageUrl = url
                            navController.navigate("image") { launchSingleTop = true }
                        },
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
                        isTablet = isTablet,
                        isLandscape = isLandscape
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
fun NewsBottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
    onTabRepeat: (String) -> Unit
) {
    Surface(
        color = CyberColors.SurfaceGlassStrong,
        shadowElevation = 16.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            // Тонкая акцентная линия сверху
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CyberColors.AccentBlue.copy(alpha = 0.35f),
                                CyberColors.AccentBlueBright.copy(alpha = 0.5f),
                                CyberColors.AccentBlue.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = CyberColors.TextPrimary,
                tonalElevation = 0.dp
            ) {
                val items = listOf(
                    Triple("home", "Главная", R.drawable.ic_home),
                    Triple("favorites", "Избранное", R.drawable.ic_favorite),
                    Triple("shorts", "Шортс", R.drawable.ic_shorts),
                    Triple("add_source", "Добавить", R.drawable.ic_add),
                    Triple("settings", "Настройки", R.drawable.ic_settings)
                )
                items.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            // Тап по уже активному табу — возврат в начало (как в топ-лентах)
                            if (selected) {
                                onTabRepeat(route)
                            }
                            navController.navigate(route) {
                                launchSingleTop = true
                                popUpTo("home") { inclusive = false }
                            }
                        },
                        icon = {
                            // Пилюля-индикатор активного таба (как топ-агрегаторы)
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) CyberColors.AccentBlue.copy(alpha = 0.18f)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = icon),
                                    contentDescription = label,
                                    tint = if (selected) CyberColors.AccentBlueBright else CyberColors.TextSecondary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                style = CyberTypography.BodySmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (selected) CyberColors.AccentBlueBright else CyberColors.TextSecondary,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberColors.AccentBlueBright,
                            selectedTextColor = CyberColors.AccentBlueBright,
                            unselectedIconColor = CyberColors.TextSecondary,
                            unselectedTextColor = CyberColors.TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    scrollToTopTrigger: Int = 0,
    onAddSource: () -> Unit,
    onArticleClick: (Post) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false,
    isLandscape: Boolean = false
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

    // ---------- Офлайн-индикатор ----------
    var isOffline by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            isOffline = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
            kotlinx.coroutines.delay(5000)
        }
    }

    if (isOffline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerXSmall)
                .clip(RoundedCornerShape(50))
                .background(CyberColors.Warning.copy(alpha = 0.18f))
                .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Нет сети — показаны сохранённые копии",
                style = CyberTypography.BodySmall,
                color = CyberColors.Warning,
            )
        }
    }

    // Поисковое поле
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("Все") }

    // ---------- Баннер новых статей при обновлении ----------
    var knownIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var feedInitialized by remember { mutableStateOf(false) }
    var bannerNewCount by remember { mutableStateOf(0) }
    val showNewBanner = bannerNewCount > 0
    LaunchedEffect(posts) {
        val current = posts.map { it.id }.toSet()
        if (!feedInitialized) {
            if (current.isNotEmpty()) {
                knownIds = current
                feedInitialized = true
            }
        } else if (current != knownIds) {
            val fresh = current - knownIds
            if (fresh.isNotEmpty()) {
                bannerNewCount = fresh.size
            }
            knownIds = current
        }
    }
    // Баннер скрываем при смене фильтра/поиска (пользователь взаимодействует)
    LaunchedEffect(searchQuery, filterType) { bannerNewCount = 0 }

    if (showNewBanner) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerXSmall)
                .clip(RoundedCornerShape(50))
                .background(CyberColors.AccentBlue)
                .clickable { bannerNewCount = 0 }
                .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "↑ +$bannerNewCount " + pluralNew(bannerNewCount),
                style = CyberTypography.BodyMedium,
                color = CyberColors.TextInverse,
            )
        }
    }
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    // Чипы папок (если есть)
    if (folders.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerXSmall),
            horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
        ) {
            item {
                val selected = selectedFolder.isEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) CyberColors.AccentBlueBright else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                        .clickable { viewModel.setSelectedFolder("") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Все источники",
                        style = CyberTypography.BodySmall,
                        color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
                    )
                }
            }
            items(folders.size) { i ->
                val folder = folders[i]
                val selected = selectedFolder == folder
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) CyberColors.AccentBlueBright else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                        .clickable { viewModel.setSelectedFolder(folder) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = folder,
                        style = CyberTypography.BodySmall,
                        color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
                    )
                }
            }
        }
    }

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

    // Строка управления: режим отображения + сортировка
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerXSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
    ) {
        // Режимы отображения
        listOf(
            "cards" to R.drawable.ic_view_cards,
            "list" to R.drawable.ic_view_list,
            "grid" to R.drawable.ic_view_grid
        ).forEach { (mode, icon) ->
            val selected = viewMode == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) CyberColors.AccentBlue else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { viewModel.setViewMode(mode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = when (mode) {
                        "cards" -> "Карточки"
                        "list" -> "Список"
                        else -> "Сетка"
                    },
                    tint = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        // Сортировка
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CyberColors.SurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable {
                    viewModel.setSortMode(if (sortMode == "date") "source" else "date")
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (sortMode == "date") "По дате ↓" else "По источнику",
                style = CyberTypography.BodySmall,
                color = CyberColors.TextSecondary,
            )
        }
    }

    // Чипы фильтра по типу источника
    val filterOptions = remember(posts) {
        val types = mutableListOf("Все")
        val has = { pred: (Post) -> Boolean -> posts.any(pred) }
        if (has { p -> (p.sourceUrl"В "").contains("youtube.com") || (p.sourceUrl ?: "").contains("youtu.be") }) types.add("YouTube")
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

    val feedListState = rememberLazyListState()
    // Прокрутка в начало по табу «Главная»
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            try { feedListState.animateScrollToItem(0) } catch (e: Exception) { }
        }
    }
    val showToTop by remember { androidx.compose.runtime.derivedStateOf { feedListState.firstVisibleItemIndex > 3 } }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
    PullToRefreshBox(
        state = refreshState,
        isRefreshing = isLoading,
        onRefresh = { viewModel.loadPosts() },
        modifier = Modifier.fillMaxSize(),
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

        // Сортировка
        val sortedPosts = if (sortMode == "source") {
            filteredPosts.sortedWith(compareBy({ it.siteName ?: it.sourceUrl ?: "" }, { it.publishedAt ?: "" }))
        } else {
            filteredPosts
        }

        when (viewMode) {
            "list" -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = UiSizes.ScreenPadding,
                    vertical = UiSizes.SpacerSmall
                ),
                verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
            ) {
                items(items = sortedPosts, key = { it.id }) { post ->
                    val ctx = LocalContext.current
                    CompactNewsCard(
                        post = post,
                        isRead = viewModel.isRead(post),
                        onClick = { onArticleClick(post) },
                        onRemove = { viewModel.toggleReadLater(post) },
                        onShare = {
                            post.sourceUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "$url")
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
                            }
                        }
                    )
                }
            }
            "grid" -> {
                val gridColumns = if (isTablet || isLandscape) 4 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = UiSizes.SpacerSmall,
                    vertical = UiSizes.SpacerSmall
                ),
                horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall),
                verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
            ) {
                items(items = sortedPosts, key = { it.id }) { post ->
                    GridNewsCard(
                        post = post,
                        onClick = { onArticleClick(post) }
                    )
                }
            }
            }
            else -> {
                // Автоотметка прочитанным при прокрутке мимо (как в Reeder)
                LaunchedEffect(feedListState.firstVisibleItemIndex, sortedPosts.size) {
                    val passed = feedListState.firstVisibleItemIndex - 2 // featured + header
                    if (passed > 0) {
                        sortedPosts.take(passed).forEach { viewModel.markAsRead(it) }
                    }
                }
                LazyColumn(
                    state = feedListState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                if (sortedPosts.isNotEmpty()) {
                    val first = sortedPosts.first()
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
                if (sortedPosts.size > 1) {
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
                                text = stringResource(id = R.string.section_more_news, sortedPosts.size - 1),
                                style = CyberTypography.TitleMedium,
                                color = CyberColors.TextSecondary
                            )
                        }
                    }
                }
                items(
                    items = sortedPosts.drop(1),
                    key = { it.id }
                ) { post ->
                    val ctx = LocalContext.current
                    SwipeableNewsCard(
                        post = post,
                        isFavorite = viewModel.isFavorite(post),
                        isRead = viewModel.isRead(post),
                        isReadLater = viewModel.isReadLater(post),
                        onFavoriteClick = { viewModel.toggleFavorite(post) },
                        onReadClick = { viewModel.markAsRead(post) },
                        onReadLaterClick = { viewModel.toggleReadLater(post) },
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

    // Кнопка «Наверх» при прокрутке — левый нижний угол, чтобы не мешать FAB «+»
    if (showToTop) {
        androidx.compose.material3.SmallFloatingActionButton(
            onClick = { scope.launch { feedListState.animateScrollToItem(0) } },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = UiSizes.ScreenPadding, bottom = UiSizes.SpacerLarge),
            containerColor = CyberColors.SurfaceElevated,
            contentColor = CyberColors.AccentBlueBright
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_up),
                contentDescription = "Наверх",
                modifier = Modifier.size(20.dp)
            )
        }
    }
    } // конец Box

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
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
            Text(
                text = stringResource(id = R.string.error_gentle_hint),
                style = CyberTypography.BodySmall,
                color = CyberColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

/** Компактная карточка для сетки (2 колонки). */
@Composable
fun GridNewsCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
            .background(CyberColors.SurfaceElevated.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            SmartNewsImage(
                    imageUrl = post.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp
                ) ?: run {
                SourceGradientPlaceholder(
                    sourceUrl = post.siteUrl ?: post.sourceUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp
                )
            }
        }
        Column(modifier = Modifier.padding(UiSizes.SpacerSmall)) {
            Text(
                text = post.title,
                style = CyberTypography.BodyMedium,
                color = CyberColors.TextPrimary,
                maxLines = 3,
            )
            post.publishedAt?.let {
                Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                Text(
                    text = it,
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Карточка новости с горизонтальными свайпами:
 * вправо — в избранное, влево — прочитано (в стиле Reeder).
 */
@Composable
fun SwipeableNewsCard(
    post: Post,
    isFavorite: Boolean,
    isRead: Boolean,
    isReadLater: Boolean,
    onFavoriteClick: () -> Unit,
    onReadClick: () -> Unit,
    onReadLaterClick: () -> Unit,
    onShareClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animSpec = androidx.compose.animation.core.tween<Float>(180)
    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetX,
        animationSpec = animSpec,
        label = "swipeOffset"
    )
    val animatedOffsetDp = with(LocalDensity.current) { animatedOffset.toDp() }
    val haptic = LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
            .background(
                brush = Brush.horizontalGradient(
                    colors = when {
                        animatedOffset > 0f -> listOf(
                            CyberColors.AccentBlue.copy(alpha = 0.25f),
                            CyberColors.SurfaceElevated.copy(alpha = 0f)
                        )
                        animatedOffset < 0f -> listOf(
                            CyberColors.SurfaceElevated.copy(alpha = 0f),
                            CyberColors.Success.copy(alpha = 0.2f)
                        )
                        else -> listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
            // Системный draggable: сам определяет направление (горизонталь/вертикаль),
            // длинное нажатие без движения не смещает карточку, отмена всегда возвращает.
            .draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX = (offsetX + delta).coerceIn(-260f, 260f)
                },
                onDragStopped = {
                    val actionX = offsetX
                    // ВСЕГДА плавно возвращаем на место
                    offsetX = 0f
                    whenА плавно возв   actionX > 90f -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteClick()
                        }
                        actionX < -90f -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReadClick()
                        }
                    }
                }
            )
    ) {
        NewsCard(
            post = post,
            isFavorite = isFavorite,
            isRead = isRead,
            isReadLater = isReadLater,
            onFavoriteClick = onFavoriteClick,
            onReadLaterClick = onReadLaterClick,
            onShareClick = onShareClick,
            onClick = onClick,
            modifier = Modifier
                .offset(x = animatedOffsetDp)
        )
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
                if (post.imageUrl != null) {
                    SmartNewsImage(
                        imageUrl = post.imageUrl,
                        modifier = Modifier.fillMaxSize()
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
                } else {
                    SourceGradientPlaceholder(
                        sourceUrl = post.siteUrl ?: post.sourceUrl,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 0.dp,
                    )
                }

                // Чип источника поверх изображения
                val chipName = post.siteName
                    ?: post.siteUrl?.removePrefix("https://")?.removePrefix("www.")?.removePrefix("http://")?.substringBefore('/')
                    ?: post.sourceUrl
                        ?.removePrefix("https://")
                        ?.removePrefix("http://")
                        ?.removePrefix("www.")
                        ?.substringBefore('/')
                if (chipName != null) {
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
                            text = chipName.take(26),
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
    isReadLater: Boolean = false,
    highlightQuery: String = "",
    onReadLaterClick: () -> Unit = {},
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
            if (post.imageUrl != null) {
                Box(
                    modifier = Modiглу                       .fillMaxWidth()
                        .height(UiSizes.HomeCardMaxHeight)
                        .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
                ) {
                    SmartNewsImage(
                        imageUrl = post.imageUrl,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = UiSizes.CardCornerRadius,
                        blurRadius = 20.dp
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
            } else {
                // Градиентный плейсхолдер в цветах источника
                SourceGradientPlaceholder(
                    sourceUrl = post.siteUrl ?: post.sourceUrl,
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
                val chipName = post.siteName
                    ?: post.siteUrl?.removePrefix("https://")?.removePrefix("www.")?.removePrefix("http://")
                        ?.substringBefore('/')
                    ?: post.sourceUrl
                        ?.removePrefix("https://")
                        ?.removePrefix("http://")
                        ?.removePrefix("www.")
                        ?.substringBefore('/')
                if (chipName != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CyberColors.AccentBlue.copy(alpha = 0.12f))
                            .padding(horizontal = UiSizes.SpacerSmall, vertical = UiSizes.SpacerXSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chipName.take(30),
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

            // Заголовок — крупный редакционный (с подсветкой поиска)
            Text(
                text = highlightText(post.title, highlightQuery, CyberColors.Warning),
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
                        text = highlightText(cleanDesc ?: "", highlightQuery, CyberColors.Warning),
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
                    onClick = onReadLaterClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bookmark),
                        contentDescription = if (isReadLater) "Убрать из 'Читать позже'" else "Читать позже",
                        tint = if (isReadLater) CyberColors.AccentBlueBright else CyberColors.TextMuted
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
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Очистить",
                            tint = CyberColors.TextSecondary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

        val filteredPosts = if (searchQuery.isNotBlank()) {
            posts.filter { post ->
                post.title.contains(searchQuery, ignoreCase = true)
                    || (post.description ?: "").contains(searchQuery, ignoreCase = true)
            }
        } else {
            posts
        }

        // Счётчик результатов
        Text(
            text = if (searchQuery.isBlank()) "Все новости (${posts.size})"
            else "Найдено: ${filteredPosts.size} из ${posts.size}",
            style = CyberTypography.BodySmall,
            color = CyberColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

        if (searchQuery.isNotBlank() && filteredPosts.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_search_results, searchQuery),
                style = CyberTypography.TitleMedium,
                color = CyberColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        isReadLater = viewModel.isReadLater(post),
                        highlightQuery = searchQuery,
                        onReadLaterClick = { viewModel.toggleReadLater(post) },
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
 UiSizes.SpacerXSmall)
                    )
                }
            }
        }
    }
}
// Rebuild fix
