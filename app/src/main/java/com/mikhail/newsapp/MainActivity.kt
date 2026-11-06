
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
                            onThemeChange = {
                                appTheme = it
                                viewModel.setTheme(it)
                            },
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            )
        }
    }
}

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
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
                if (viewModel.isRead(post.id)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Read", tint = MaterialTheme.colorScheme.primary.copy(0.5f), modifier = Modifier.size(20.dp))
                }
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
                        contentDescription = "Favorite",
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

    val safeTitle = post.title.takeIf { it.isNotBlank() } ?: "Без названия"
    val safeDesc = post.description.takeIf { it.isNotBlank() } ?: ""
    val safeEmoji = post.source.emoji.takeIf { it.isNotBlank() } ?: "📰"
    val safeDisplayName = post.source.displayName.takeIf { it.isNotBlank() } ?: "Источник"

    DisposableEffect(Unit) {
        onDispose {
            // Cleanup TTS on dispose
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(bgColor))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = Color(textColor))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Чтение", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(textColor), modifier = Modifier.weight(1f))
            IconButton(onClick = {
                if (isSpeaking) {
                    // Stop TTS
                    isSpeaking = false
                } else {
                    // Start TTS
                    isSpeaking = true
                    android.speech.tts.TextToSpeech(context) { tts ->
                        if (tts == android.speech.tts.TextToSpeech.SUCCESS) {
                            tts.language = java.util.Locale("ru")
                            tts.speak(safeTitle + ". " + safeDesc, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                }
            }) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "TTS",
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
    val sources by viewModel.sources.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("➕ Добавить источник", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        Text("Тип источника:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
        Text("URL источника:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                    viewModel.addSource(sourceType, sourceUrl)
                    sourceUrl = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
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
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortsScreen(viewModel: MainViewModel, navController: androidx.navigation.NavController) {
    val shorts = remember {
        listOf(
            UnifiedPost("shorts_1", "Новости технологий", "Новые тенденции в разработке мобильных приложений на 2026 год", "", SourceType.RSS, "https://example.com/tech-news"),
            UnifiedPost("shorts_2", "Android 16 обновление", "Что нового в последней версии Android", "", SourceType.YOUTUBE, "https://example.com/android-16"),
            UnifiedPost("shorts_3", "Компоуз уроки", "Jetpack Compose для начинающих", "", SourceType.TELEGRAM, "https://example.com/compose-lessons"),
            UnifiedPost("shorts_4", "Kotlin корутины", "Асинхронное программирование на Kotlin", "", SourceType.RSS, "https://example.com/kotlin-coroutines"),
            UnifiedPost("shorts_5", "Дизайн системы", "Создание дизайн-системы для приложений", "", SourceType.REDDIT, "https://example.com/design-system")
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
                        .clickable {
                            viewModel.selectPost(post)
                            navController.navigate("article_reader")
                        },
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
                            Text(text = post.source.emoji, fontSize = 48.sp)
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
    currentTheme: MainViewModel.AppTheme,
    onThemeChange: (MainViewModel.AppTheme) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearReadHistory: () -> Unit,
    readHistoryCount: Int
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

        // Источники
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

        // Тема
        Text("Тема оформления", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        listOf(
            MainViewModel.AppTheme.SYSTEM to "Системная",
            MainViewModel.AppTheme.LIGHT to "Светлая",
            MainViewModel.AppTheme.DARK to "Тёмная",
            MainViewModel.AppTheme.AMOLED to "AMOLED"
        ).forEach { (theme, name) ->
            RadioButton(
                selected = currentTheme == theme,
                onClick = { onThemeChange(theme) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(if (currentTheme == theme) 1f else 0.3f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Язык
        Text("Язык", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        listOf(
            "system" to "Системный",
            "ru" to "Русский",
            "en" to "Английский",
            "uk" to "Українська"
        ).forEach { (code, name) ->
            RadioButton(
                selected = currentLanguage == code,
                onClick = { onLanguageChange(code) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
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

        // Данные
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

        // О приложении
        Text("О приложении", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Text("NewsApp v1.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("GitHub: turunovmk-svg/Newsapp", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}