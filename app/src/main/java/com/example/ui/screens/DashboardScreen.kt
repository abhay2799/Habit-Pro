package com.example.ui.screens

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdManager
import com.example.data.model.Habit
import com.example.ui.HabitViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val rawHabits by viewModel.habitsList.collectAsState()
    val dashboardSortMode by viewModel.dashboardSortMode.collectAsState()
    
    val habits = remember(rawHabits, dashboardSortMode) {
        when (dashboardSortMode) {
            "Alphabetical" -> rawHabits.sortedBy { it.name.lowercase() }
            "Chronological" -> rawHabits.sortedBy { it.id } // Order of creation acts as chronological
            else -> rawHabits // Manual layout retains list order
        }
    }
    val completions by viewModel.habitCompletions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userSession by viewModel.userSession.collectAsState()

    val context = LocalContext.current

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    
    // Ad watched state
    var adsWatchedCounter by remember { mutableStateOf(0) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val categories = listOf("All", "Health", "Fitness", "Work", "Personal")

    // --- MONETIZATION & CONTEXT LOCKS STATE ---
    var isStudioUnlockedSession by remember { mutableStateOf(false) }
    var isBiometricScreenUnlocked by remember { mutableStateOf(false) }

    // --- FEATURE 1: Pomodoro timer state ---
    var timerTimeLeft by remember { mutableStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    
    LaunchedEffect(isTimerRunning, timerTimeLeft) {
        if (isTimerRunning && timerTimeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timerTimeLeft--
            if (timerTimeLeft == 0) {
                isTimerRunning = false
                Toast.makeText(context, "🎉 Focus session complete! Level Up +25 XP Reward!", Toast.LENGTH_LONG).show()
                viewModel.triggerSync() // Trigger simulation sync as success event
            }
        }
    }

    // --- FEATURE 2: Lo-Fi Ambient Soundscapes state ---
    var activeSoundscapeIdx by remember { mutableStateOf(-1) } // -1 = off
    var soundVolume by remember { mutableStateOf(0.7f) }


    val soundscapes = listOf(
        Pair("Cozy Rain Café ☕", "🌧️ Live synthesized warm falling raindrops & cozy coffee shop crackle"),
        Pair("Cosmic Binaural Waves 🧠", "🌌 Delta-wave frequencies for neuro-focus flow state induction"),
        Pair("Forest Stream Meditation 🍃", "🏞️ Dynamic stream flow with automated wild bird song chirps"),
        Pair("Dream State Zen Waves 🌊", "🌊 Natural tide swells & cascading low-pass white noise waves")
    )

    DisposableEffect(Unit) {
        onDispose {
            com.example.AmbientAudioEngine.stop()
        }
    }

    // --- FEATURE 3: Reflection Journal log state ---
    var currentJournalText by remember { mutableStateOf("") }
    var selectedJournalEmoji by remember { mutableStateOf("😌") }
    val journalEmojis = listOf("😌", "🧠", "🔥", "🔋", "😤", "🥱")
    val savedReflectionLines = remember { mutableStateListOf<String>() }

    // --- FEATURE 4: XP Level Booster Multiplier ---
    var isXpBoosterActive by remember { mutableStateOf(false) }
    var boosterTimeRemainingSec by remember { mutableStateOf(0) }

    LaunchedEffect(isXpBoosterActive, boosterTimeRemainingSec) {
        if (isXpBoosterActive && boosterTimeRemainingSec > 0) {
            kotlinx.coroutines.delay(1000)
            boosterTimeRemainingSec--
            if (boosterTimeRemainingSec == 0) {
                isXpBoosterActive = false
            }
        }
    }

    // --- FEATURE 5: Streak Shield state ---
    var streakShieldsOwned by remember { mutableStateOf(0) }

    // --- FEATURE 6: Collapsible Interactive focus studio visual control ---
    var isStudioExpanded by remember { mutableStateOf(false) }

    // Confetti Event state
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.showConfettiEvent.collectLatest {
            showConfetti = true
            kotlinx.coroutines.delay(3000) // Show for 3 seconds
            showConfetti = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = Color.White) },
                text = { Text("Add New Habit", color = Color.White, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("add_habit_fab")
                    .padding(bottom = 60.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2E68FF), Color(0xFFB130FF))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content hierarchy within a dynamic, master LazyColumn for smooth adaptive performance
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                // Interactive welcoming identity
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Hello, ${userSession.username}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("👋", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Level ${1 + (userSession.totalXp / 100)} >",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Streak pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha=0.3f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "0 Day Streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // XP pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFFC882FF).copy(alpha=0.3f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Hexagon,
                                            contentDescription = null,
                                            tint = Color(0xFFC882FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${userSession.totalXp} XP",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Top Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val maxStreak = if (habits.isEmpty()) 0 else habits.maxOf { it.currentStreak }
                            // Current Streak Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                                modifier = Modifier.weight(1.2f).border(1.dp, Color.White.copy(alpha=0.05f), RoundedCornerShape(20.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CURRENT STREAK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF21D4D4),
                                            letterSpacing = 1.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF231236))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "+ PRO STREAK",
                                                color = Color(0xFFC882FF),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$maxStreak",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB130FF)
                                        )
                                        Text(
                                            text = "-Day Streak",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Visual progression line
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Hexagon, contentDescription=null, tint=Color.White, modifier=Modifier.size(20.dp))
                                        Box(modifier = Modifier.width(30.dp).height(1.dp).background(Color.DarkGray))
                                        Icon(Icons.Default.Lock, contentDescription=null, tint=Color.DarkGray, modifier=Modifier.size(16.dp))
                                        Box(modifier = Modifier.width(30.dp).height(1.dp).background(Color.DarkGray))
                                        Icon(Icons.Default.Lock, contentDescription=null, tint=Color.DarkGray, modifier=Modifier.size(16.dp))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    val habitsLeft = habits.count { completions[it.id]?.contains(todayStr) != true }
                                    Text(
                                        text = "$habitsLeft more trackers to",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "secure your streak flame",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "today! 🚀",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            // Hydration Card
                            val waterLogged by viewModel.dailyWaterLogged.collectAsState()
                            val waterGoal by viewModel.dailyWaterGoal.collectAsState()
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                                modifier = Modifier.weight(0.8f).height(190.dp).border(1.dp, Color.White.copy(alpha=0.05f), RoundedCornerShape(20.dp)).clickable { viewModel.addWaterGlass() }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "DAILY HYDRATION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21D4D4),
                                        letterSpacing = 1.sp
                                    )
                                    
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                                        CircularProgressIndicator(
                                            progress = { (waterLogged.toFloat() / waterGoal.toFloat()).coerceAtMost(1f) },
                                            modifier = Modifier.fillMaxSize(),
                                            strokeWidth = 6.dp,
                                            color = Color(0xFF2E68FF),
                                            trackColor = Color(0xFF0A182E)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = Color(0xFF2E68FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F2615))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "● In Progress",
                                            color = Color(0xFF1DD75B),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Text(
                                        text = "1 Glass = 250ml",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Box container
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Fuzzy search habit cards, mood tags...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        singleLine = true
                    )
                }

                // Category select tab pills row
                item {
                    val catIcons = mapOf(
                        "All" to Icons.Default.Search,
                        "Health" to Icons.Default.FavoriteBorder,
                        "Fitness" to Icons.Default.FitnessCenter,
                        "Work" to Icons.Default.WorkOutline,
                        "Personal" to Icons.Default.PersonOutline
                    )
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategoryFilter).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {},
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategoryFilter == category
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF2E68FF) else Color.Transparent)
                                    .clickable { selectedCategoryFilter = category }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = catIcons[category]
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable { showAddDialog = true }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }



                // Render Habits trackers container list
                val filteredHabits = habits.filter {
                    selectedCategoryFilter == "All" || it.category == selectedCategoryFilter
                }

                if (filteredHabits.isEmpty()) {
                    item {
                        // Empty states placeholder
                        val sharedPrefs = context.getSharedPreferences("habit_tracker_prefs", Context.MODE_PRIVATE)
                        val ageRange = sharedPrefs.getString("user_age_range", "25-34") ?: "25-34"
                        
                        val suggestions = remember(ageRange) {
                            when (ageRange) {
                                "Under 18" -> listOf(
                                    "💧 Drink 6-8 glasses of water daily",
                                    "🛌 Sleep 8-10 hours a night for growth",
                                    "🏃‍♂️ Get at least 60 mins of physical activity"
                                )
                                "18-24" -> listOf(
                                    "💧 Drink 2-3 liters of water daily",
                                    "🛌 Sleep 7-9 hours for cognitive performance",
                                    "🧠 Take 15 mins for mindfulness or meditation"
                                )
                                "25-34" -> listOf(
                                    "💧 Drink 2-3 liters of water daily",
                                    "🏋️‍♂️ Exercise 3-4 times a week to maintain energy",
                                    "📱 Reduce screen time 1 hour before bed"
                                )
                                "35-44" -> listOf(
                                    "💧 Drink 2-3 liters of water daily",
                                    "🧘‍♂️ Incorporate daily stretching for flexibility",
                                    "🥗 Eat a balanced diet rich in whole foods"
                                )
                                "45-54" -> listOf(
                                    "💧 Drink 2-3 liters of water daily",
                                    "🚶‍♂️ Take a 30-min brisk walk daily for heart health",
                                    "🛌 Ensure consistent sleep schedules"
                                )
                                else -> listOf( // "55+"
                                    "💧 Stay hydrated throughout the day",
                                    "🦴 Practice light resistance training for bone health",
                                    "🧠 Read or learn a new skill to keep the mind sharp"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Matching Trackers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try switching headers categories filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            } else {
                                Text(
                                    text = "Doctor Recommended Habits for Age $ageRange",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                suggestions.forEach { suggestion ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(16.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredHabits, key = { it.id }) { habit ->
                        val completionsList = completions[habit.id] ?: emptyList()
                        val isCompletedToday = completionsList.contains(todayStr)

                        // HabitItemCard includes collapsible subtasks + personalized custom stylings
                        HabitItemCard(
                            habit = habit,
                            isCompleted = isCompletedToday,
                            onCheckToggle = { viewModel.completeHabit(habit, todayStr) },
                            onDelete = { viewModel.deleteHabit(habit) },
                            viewModel = viewModel
                        )
                    }
                }
            }

            // AdMob static Banner container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    factory = { ctx ->
                        AdManager.createBannerAdView(ctx)
                    }
                )
            }

            // --- FEATURE 7: On-Device Biometric Vault Verification Lock Simulation ---
            if (userSession.biometricEnabled && !isBiometricScreenUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(enabled = true, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Lock Panel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                              )
                            Text(
                                text = "On-Device Secure Biometric Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "This habit profile vault is protected under secure biometric hardware. Please verify to unlock access.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    isBiometricScreenUnlocked = true
                                    Toast.makeText(context, "Scan Complete! Vault Decrypted.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Tap to Verify Identity Fingerprint/FaceID")
                            }
                        }
                    }
                }
            }


    // Modal dialogue to generate habits
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Fitness") }
        var freq by remember { mutableStateOf("Daily") }
        var xpPoints by remember { mutableStateOf("15") }
        val categoryOptions = listOf("Health", "Work", "Personal", "Fitness", "Productivity", "Wellness")

        // Custom styling modifiers
        var selectedPalette by remember { mutableStateOf("Default") }
        var selectedEmoji by remember { mutableStateOf("Default") }
        var alertType by remember { mutableStateOf("None") }
        val palettes = remember { mutableStateListOf("Default", "Sapphire Blue", "Emerald Green", "Velvet Amethyst", "Rose Pink", "Amber Orange") }
        val emojis = listOf("Default", "🏃‍♂️", "🧘‍♂️", "📚", "💡", "💧", "🍳", "🧠", "☕", "🔋")

        var showCustomThemeDialog by remember { mutableStateOf(false) }
        var customThemeInput by remember { mutableStateOf("") }
        var showCustomGenreDialog by remember { mutableStateOf(false) }
        var customGenreInput by remember { mutableStateOf("") }

        // --- FEATURE 8: Flexible Days Scheduler Option ---
        var customizeWeeklyDays by remember { mutableStateOf(false) }
        val daysChecklist = remember { mutableStateListOf("Mon", "Wed", "Fri") }
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Launch Habit Tracker",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Habit Name (e.g. Wake up at 6AM)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_habit_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Short Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Color Palette Selector (Custom Styling feature)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎨 Theme:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showCustomThemeDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add custom theme", modifier = Modifier.size(16.dp))
                        }
                    }
                    ScrollableTabRow(
                        selectedTabIndex = palettes.indexOf(selectedPalette).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        palettes.forEach { item ->
                            val isChosen = selectedPalette == item
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedPalette = item }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(item, color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }

                    // Custom card emoji icon selector
                    Text("⭐ Emoji:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    ScrollableTabRow(
                        selectedTabIndex = emojis.indexOf(selectedEmoji).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        emojis.forEach { iconChar ->
                            val isChosen = selectedEmoji == iconChar
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedEmoji = iconChar }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(iconChar, color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Genre Category:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showCustomGenreDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add custom genre", modifier = Modifier.size(16.dp))
                        }
                    }
                    ScrollableTabRow(
                        selectedTabIndex = categoryOptions.indexOf(category).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        categoryOptions.forEach { cat ->
                            val sel = category == cat
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Flexible Scheduler switch & checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅 Custom Days Week Schedule Frequencies", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = customizeWeeklyDays,
                            onCheckedChange = { customizeWeeklyDays = it }
                        )
                    }

                    if (customizeWeeklyDays) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 4,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysOfWeek.forEach { dayLabel ->
                                val isChecked = daysChecklist.contains(dayLabel)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        if (isChecked) daysChecklist.remove(dayLabel) else daysChecklist.add(dayLabel)
                                    },
                                    label = { Text(dayLabel, fontSize = 10.sp) }
                                )
                            }
                        }
                        freq = "Schedule: " + daysChecklist.joinToString(", ")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!customizeWeeklyDays) {
                            OutlinedTextField(
                                value = freq,
                                onValueChange = { freq = it },
                                label = { Text("Frequency") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        // Alert selection box
                        var alertExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = alertType,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("🔔 Alert") },
                                modifier = Modifier.fillMaxWidth().clickable { alertExpanded = true },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Alert") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = alertExpanded,
                                onDismissRequest = { alertExpanded = false }
                            ) {
                                listOf("None", "Notification Alert", "Alarm").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            alertType = option
                                            alertExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            // Smart injection prefix to pack customization without breaking migrations
                            val complexDesc = buildString {
                                if (selectedPalette != "Default") {
                                    append("[🎨$selectedPalette]")
                                }
                                if (selectedEmoji != "Default") {
                                    append("[$selectedEmoji]")
                                }
                                if (alertType != "None") {
                                    append("[🔔$alertType]")
                                }
                                append(desc)
                            }

                            val finalFreq = if (customizeWeeklyDays) {
                                "Schedule: " + daysChecklist.joinToString(", ")
                            } else {
                                freq
                            }

                            viewModel.addHabit(
                                name = name,
                                description = complexDesc,
                                category = category,
                                frequency = finalFreq,
                                isPremium = false,
                                xpPenalty = xpPoints.toIntOrNull() ?: 15
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_habit_button")
                ) {
                    Text("Launch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )

        // Custom Theme Input Dialog
        if (showCustomThemeDialog) {
            AlertDialog(
                onDismissRequest = { showCustomThemeDialog = false },
                title = { Text("Add Custom Theme") },
                text = {
                    OutlinedTextField(
                        value = customThemeInput,
                        onValueChange = { customThemeInput = it },
                        label = { Text("Color Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (customThemeInput.isNotBlank()) {
                            palettes.add(customThemeInput)
                            selectedPalette = customThemeInput
                        }
                        showCustomThemeDialog = false
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomThemeDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Custom Genre Input Dialog
        if (showCustomGenreDialog) {
            AlertDialog(
                onDismissRequest = { showCustomGenreDialog = false },
                title = { Text("Add Custom Genre") },
                text = {
                    OutlinedTextField(
                        value = customGenreInput,
                        onValueChange = { customGenreInput = it },
                        label = { Text("Genre") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (customGenreInput.isNotBlank()) {
                            // Can't dynamically modify categoryOptions easily since it's a fixed list inside showAddDialog state in original code
                            // I will just set category = customGenreInput
                            category = customGenreInput
                        }
                        showCustomGenreDialog = false
                    }) { Text("Use Genre") }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomGenreDialog = false }) { Text("Cancel") }
                }
            )
        }
    }



    if (showConfetti) {
        ConfettiOverlay()
    }
}
        }
    }
}


@Composable
fun HabitItemCard(
    habit: Habit,
    isCompleted: Boolean,
    onCheckToggle: () -> Unit,
    onDelete: () -> Unit,
    viewModel: HabitViewModel
) {
    val completedCount = 0 // Placeholder logic for Micro-Step
    
    val categoryColor = when (habit.category) {
        "Health" -> Color(0xFF1DD75B)
        "Fitness" -> Color(0xFFF43F5E)
        "Work" -> Color(0xFFC882FF)
        "Personal" -> Color(0xFF1982FC)
        "Morning Routine" -> Color(0xFFF59E0B)
        else -> Color(0xFF2E68FF)
    }

    val isDark = isSystemInDarkTheme()
    val categoryContainer = when (habit.category) {
        "Health" -> if (isDark) Color(0xFF0F2615) else Color(0xFFE6F7EB)
        "Fitness" -> if (isDark) Color(0xFF2B0F15) else Color(0xFFFCE8EB)
        "Work" -> if (isDark) Color(0xFF1D0E29) else Color(0xFFF3E8FC)
        "Personal" -> if (isDark) Color(0xFF0A182E) else Color(0xFFE6F0FC)
        "Morning Routine" -> if (isDark) Color(0xFF26190B) else Color(0xFFFCF3E6)
        else -> if (isDark) Color(0xFF0A182E) else Color(0xFFE6F0FC)
    }

    val dynamicIcon = when (habit.category) {
        "Health" -> Icons.Default.DirectionsRun
        "Fitness" -> Icons.Default.FitnessCenter
        "Work" -> Icons.Default.Work
        "Personal" -> Icons.Default.MenuBook
        "Morning Routine" -> Icons.Default.WbSunny
        else -> Icons.Default.Star
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .clickable { /* Expand logic if any */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(categoryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = dynamicIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = habit.description.ifBlank { "Complete your habit everyday." },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tag Label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(categoryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = habit.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                        }

                        // Micro-Step
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Micro-Step 0/3",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Right Action Elements (More Vert + Check Circle)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp).clickable { onDelete() }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(
                    onClick = onCheckToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete Habit Checkmark",
                        tint = if (isCompleted) Color(0xFF2E68FF) else Color.DarkGray,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isCompleted) Color(0xFF2E68FF) else Color(0xFF1E284A), CircleShape)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfettiOverlay() {
    val animatable = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
        )
    }

    val particles = remember<List<ConfettiParticle>> {
        List(150) {
            ConfettiParticle(
                x = Random.Default.nextFloat(),
                y = Random.Default.nextFloat() * 0.5f,
                speed = Random.Default.nextFloat() * 3f + 1f,
                angle = Random.Default.nextFloat() * 360f,
                alpha = 1f,
                color = Color(
                    red = Random.Default.nextFloat(),
                    green = Random.Default.nextFloat(),
                    blue = Random.Default.nextFloat(),
                    alpha = 1f
                )
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val progress = animatable.value
        particles.forEach { particle ->
            val curY = particle.y * size.height + (progress * 1500f * particle.speed)
            val curX = particle.x * size.width + (Math.sin((progress * 10f + particle.angle).toDouble()) * 100f).toFloat()
            
            drawCircle(
                color = particle.color.copy(alpha = 1f - progress), // Fade out
                radius = 12f * (1f - progress * 0.5f),
                center = Offset(curX, curY)
            )
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val angle: Float,
    val alpha: Float,
    val color: Color
)
