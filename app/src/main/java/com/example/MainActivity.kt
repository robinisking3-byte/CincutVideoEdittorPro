package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.TimelineController
import com.example.core.model.Project
import com.example.core.repository.CineCutRepository
import com.example.ui.screens.*
import com.example.ui.theme.CineBackground
import com.example.ui.theme.CineCutTheme
import com.example.ui.theme.CinePrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineTextSecondary
import com.example.ui.theme.LocalFestivalConfig

class MainActivity : ComponentActivity() {

    private lateinit var repository: CineCutRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CineCutRepository(applicationContext)
        com.example.core.notification.CineCutMessagingService.createNotificationChannel(applicationContext)
        enableEdgeToEdge()
        setContent {
            val festivalConfig by repository.festivalConfig.collectAsState()
            val currentAuthUser by repository.currentAuthUser.collectAsState()

            // Request Notification Permission on Android 13+ (TIRAMISU)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { /* Permission result handled */ }

                LaunchedEffect(Unit) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CompositionLocalProvider(LocalFestivalConfig provides festivalConfig) {
                CineCutTheme {
                    if (currentAuthUser == null) {
                        AuthScreen(
                            repository = repository,
                            onAuthSuccess = {
                                // Successfully authenticated session
                            }
                        )
                    } else {
                        CineCutApp(
                            repository = repository,
                            onLogout = {
                                repository.signOut()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CineCutApp(
    repository: CineCutRepository,
    onLogout: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf("home") } // "home", "editor", "rooms", "live", "social", "messages", "membership", "coins", "profile", "admin", "festival_offers"
    var activeTimelineController by remember { mutableStateOf<TimelineController?>(null) }

    // Intercept hardware back button when on sub-screens
    if (currentScreen != "home") {
        BackHandler {
            currentScreen = when (currentScreen) {
                "admin", "support" -> "profile"
                "festival_offers" -> "home"
                else -> "home"
            }
        }
    }

    // Layer 2 Navigation Security: Disallow admin route for unauthorized users
    if (currentScreen == "admin" && currentUser.adminRole == com.example.core.model.AdminRole.NONE) {
        val isDesignatedAdmin = currentUser.email.trim().equals("robinisking3@gmail.com", ignoreCase = true)
        if (!isDesignatedAdmin) {
            currentScreen = "profile"
        }
    }

    val showBottomNav = currentScreen in listOf("home", "live", "social", "profile")

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = CineSurface,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("cinecut_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "home",
                        onClick = { currentScreen = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Studio", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CinePrimary,
                            selectedTextColor = CinePrimary,
                            indicatorColor = CinePrimary.copy(alpha = 0.15f),
                            unselectedIconColor = CineTextSecondary,
                            unselectedTextColor = CineTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "live",
                        onClick = { currentScreen = "live" },
                        icon = { Icon(Icons.Default.LiveTv, contentDescription = "CineLive") },
                        label = { Text("CineLive", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CinePrimary,
                            selectedTextColor = CinePrimary,
                            indicatorColor = CinePrimary.copy(alpha = 0.15f),
                            unselectedIconColor = CineTextSecondary,
                            unselectedTextColor = CineTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "social",
                        onClick = { currentScreen = "social" },
                        icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Feed") },
                        label = { Text("Feed", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CinePrimary,
                            selectedTextColor = CinePrimary,
                            indicatorColor = CinePrimary.copy(alpha = 0.15f),
                            unselectedIconColor = CineTextSecondary,
                            unselectedTextColor = CineTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "profile",
                        onClick = { currentScreen = "profile" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CinePrimary,
                            selectedTextColor = CinePrimary,
                            indicatorColor = CinePrimary.copy(alpha = 0.15f),
                            unselectedIconColor = CineTextSecondary,
                            unselectedTextColor = CineTextSecondary
                        )
                    )
                }
            }
        },
        containerColor = CineBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentScreen) {
            "home" -> {
                HomeScreen(
                    repository = repository,
                    onOpenProject = { project ->
                        activeTimelineController = TimelineController(project)
                        currentScreen = "editor"
                    },
                    onNavigateToRooms = { currentScreen = "rooms" },
                    onNavigateToLive = { currentScreen = "live" },
                    onNavigateToCoins = { currentScreen = "coins" },
                    onNavigateToAdmin = { currentScreen = "admin" },
                    onNavigateToFestivalOffers = { currentScreen = "festival_offers" },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "editor" -> {
                val controller = activeTimelineController ?: remember {
                    TimelineController(Project(title = "Cinematic Odyssey"))
                }
                EditorScreen(
                    timelineController = controller,
                    onBack = { currentScreen = "home" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "rooms" -> {
                CineRoomsScreen(
                    repository = repository,
                    onBack = { currentScreen = "home" },
                    onNavigateToMembership = { currentScreen = "membership" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "live" -> {
                CineLiveScreen(
                    repository = repository,
                    onBack = { currentScreen = "home" },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "social" -> {
                SocialFeedScreen(
                    repository = repository,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "messages" -> {
                MessagesScreen(
                    repository = repository,
                    onBack = { currentScreen = "home" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "membership" -> {
                MembershipScreen(
                    repository = repository,
                    onBack = { currentScreen = "home" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "coins" -> {
                CineCoinsScreen(
                    repository = repository,
                    onBack = { currentScreen = "home" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "profile" -> {
                ProfileScreen(
                    repository = repository,
                    onNavigateToMembership = { currentScreen = "membership" },
                    onNavigateToCoins = { currentScreen = "coins" },
                    onNavigateToAdmin = { currentScreen = "admin" },
                    onNavigateToSupport = { currentScreen = "support" },
                    onSignOut = onLogout,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "support" -> {
                SupportScreen(
                    repository = repository,
                    onBack = { currentScreen = "profile" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "admin" -> {
                AdminDashboardScreen(
                    repository = repository,
                    onBack = { currentScreen = "profile" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "festival_offers" -> {
                FestivalOffersScreen(
                    repository = repository,
                    onBack = { currentScreen = "home" },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
