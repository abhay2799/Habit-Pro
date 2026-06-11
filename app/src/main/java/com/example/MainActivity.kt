package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ads.AdManager
import com.example.data.local.AppDatabase
import com.example.data.repository.HabitRepository
import com.example.notifications.NotificationHelper
import com.example.ui.HabitViewModel
import com.example.ui.HabitViewModelFactory
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SoundscapesScreen
import com.example.ui.theme.MyApplicationTheme

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // 1. Initialize AdMob Ads, Notifications, and RevenueCat
        AdManager.initialize(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)
        
        // Initialize RevenueCat (Google Play Billing Wrapper)
        Purchases.debugLogsEnabled = true
        val revenueCatApiKey = BuildConfig.REVENUECAT_PUBLIC_API_KEY
        if (revenueCatApiKey.isNotEmpty() && revenueCatApiKey != "MY_REVENUECAT_API_KEY") {
            Purchases.configure(PurchasesConfiguration.Builder(this, revenueCatApiKey).build())
        }

        // 2. Build Database and Repository
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = HabitRepository(database.habitDao(), applicationContext)

        setContent {
            // Initialize MVVM Architecture
            val context = LocalContext.current
            val viewModel: HabitViewModel = remember<HabitViewModel> {
                ViewModelProvider(
                    this@MainActivity,
                    HabitViewModelFactory(repository, context.applicationContext)
                )[HabitViewModel::class.java]
            }

            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()

            MyApplicationTheme(isDarkMode = isDarkMode, accentColor = accentColor) {
                val userSession by viewModel.userSession.collectAsState()
                val alarmOverlayState by viewModel.alarmOverlayState.collectAsState()

                // Catch background alarms on open
                LaunchedEffect(isDarkMode, accentColor) {
                    val show = this@MainActivity.intent?.getBooleanExtra("show_alarm_overlay", false) ?: false
                    if (show) {
                        val titleStr = this@MainActivity.intent?.getStringExtra("alarm_overlay_title") ?: "Chime Alert"
                        val msgStr = this@MainActivity.intent?.getStringExtra("alarm_overlay_message") ?: "Daily Goal Check-In!"
                        viewModel.triggerAlarmOverlay(titleStr, msgStr)
                        this@MainActivity.intent?.removeExtra("show_alarm_overlay")
                    }
                }

                // Render dynamic wake-up pop-on overlay over lock screen or app layout
                if (alarmOverlayState != null) {
                    val overlayDetails = alarmOverlayState!!
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissAlarmOverlay() },
                        title = {
                            Text(
                                "🚨 " + overlayDetails.second,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                overlayDetails.third,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.dismissAlarmOverlay() }
                            ) {
                                Text("Dismiss Alarm")
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                
                // Track onboarding completed preference state locally
                val onboardingCompletePref = remember { 
                    context.getSharedPreferences("habit_tracker_prefs", MODE_PRIVATE) 
                }
                var isOnboardingComplete by remember { 
                    mutableStateOf(onboardingCompletePref.getBoolean("tutorial_completed", false)) 
                }

                val isDarkTheme = isDarkMode
                val gradientBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0F4F8), Color(0xFFE2E8F0))
                )
                val backgroundModifier = if (isDarkTheme) {
                    Modifier.background(MaterialTheme.colorScheme.background)
                } else {
                    Modifier.background(brush = gradientBrush)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(backgroundModifier),
                    color = Color.Transparent
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else if (!isOnboardingComplete) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onOnboardingComplete = {
                                onboardingCompletePref.edit().putBoolean("tutorial_completed", true).apply()
                                isOnboardingComplete = true
                            }
                        )
                    } else {
                        MainAppContent(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val show = intent.getBooleanExtra("show_alarm_overlay", false)
        if (show) {
            val titleStr = intent.getStringExtra("alarm_overlay_title") ?: "Chime Alert"
            val msgStr = intent.getStringExtra("alarm_overlay_message") ?: "Time to check in!"
            try {
                val db = com.example.data.local.AppDatabase.getDatabase(applicationContext, lifecycleScope)
                val repo = com.example.data.repository.HabitRepository(db.habitDao(), applicationContext)
                val vm = androidx.lifecycle.ViewModelProvider(this, HabitViewModelFactory(repo, applicationContext))[HabitViewModel::class.java]
                vm.triggerAlarmOverlay(titleStr, msgStr)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to delegate onNewIntent VM", e)
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    var showDivider by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val mediaPlayer = remember {
        try {
            android.media.MediaPlayer.create(context, R.raw.devlance_studio_sound)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
        label = "textAlpha"
    )
    val textScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
        label = "textScale"
    )
    val devlanceSpacing by animateFloatAsState(
        targetValue = if (isVisible) 2f else 0f,
        animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
        label = "devlanceSpacing"
    )
    val studioSpacing by animateFloatAsState(
        targetValue = if (isVisible) 12f else 4f,
        animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
        label = "studioSpacing"
    )
    val dividerWidth by animateFloatAsState(
        targetValue = if (showDivider) 220f else 0f,
        animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
        label = "dividerWidth"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
        showDivider = true
        mediaPlayer?.start() // Sync exact with yellow line and text animation
        kotlinx.coroutines.delay(3500)
        onTimeout()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = textScale
                    scaleY = textScale
                    alpha = textAlpha
                }
        ) {
            Text(
                "DEVLANCE",
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                color = Color.White,
                letterSpacing = devlanceSpacing.sp
            )
            Text(
                "S T U D I O",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color.LightGray,
                letterSpacing = studioSpacing.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.width(dividerWidth.dp),
                color = Color(0xFFFFB300),
                thickness = 3.dp
            )
        }
    }
}

@Composable
fun MainAppContent(viewModel: HabitViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF13131A).copy(alpha = 0.85f))
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_navigation_bar"),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF2E68FF), // GradientStart replacement
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Transparent
                    )

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Active Trackers") },
                        label = { if (selectedTab == 0) Text("Tracker") },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_tracker")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Audiotrack, contentDescription = "Active Soundscapes") },
                        label = { if (selectedTab == 1) Text("Sounds") },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_sounds")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Visual Progress") },
                        label = { if (selectedTab == 2) Text("Analytics") },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile Record") },
                        label = { if (selectedTab == 3) Text("Profile") },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> com.example.ui.screens.DashboardScreen(viewModel = viewModel)
                1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sounds coming soon", color = MaterialTheme.colorScheme.onBackground) }
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Analytics coming soon", color = MaterialTheme.colorScheme.onBackground) }
                3 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profile coming soon", color = MaterialTheme.colorScheme.onBackground) }
            }
        }
    }
}
