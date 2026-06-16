package com.example.ui.screens

import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.example.data.model.Habit
import com.example.ui.HabitViewModel
import com.example.ui.theme.LocalAccentPalette
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
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val categories = listOf("All", "Health", "Work", "Personal", "Fitness", "Productivity", "Wellness")

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
            Box(
                modifier = Modifier
                    .testTag("add_habit_fab")
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFB130FF))
                        )
                    )
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("Add New Habit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Hello, ",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val words = userSession.username.split(" ")
                                    val displayName = if (words.size > 10) words.take(10).joinToString(" ") + "..." else userSession.username
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("👋", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Level ${1 + (userSession.totalXp / 100)} >",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Streak pill - use actual max streak
                                val maxStreakForPill = if (habits.isEmpty()) 0 else habits.maxOf { it.currentStreak }
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
                                            text = "$maxStreakForPill Day Streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
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
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }

                        // Top Cards Row
                        val waterLogged by viewModel.dailyWaterLogged.collectAsState()
                        val waterGoal by viewModel.dailyWaterGoal.collectAsState()
                        val waterProgress by animateFloatAsState(
                            targetValue = (waterLogged.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "waterProgress"
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().height(180.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "DAILY HYDRATION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF21D4D4),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${waterLogged}/${waterGoal} Glasses",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.addWaterGlass() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E68FF)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Log Drink", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        if (waterLogged > 0) {
                                            OutlinedButton(
                                                onClick = { viewModel.removeWaterGlass() },
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    
                                    if (waterLogged > 0) {
                                        Text(
                                            text = "Reset all",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.clickable { viewModel.resetWaterDaily() }.padding(4.dp)
                                        )
                                    }
                                }
                                
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                                    CircularProgressIndicator(
                                        progress = { waterProgress },
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 12.dp,
                                        color = Color(0xFF2E68FF),
                                        trackColor = Color(0xFF0A182E),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = Color(0xFF2E68FF),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (waterLogged >= waterGoal) Color(0xFF0F2615) else Color(0xFF0F2615))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (waterLogged >= waterGoal) "✓ Complete" else "● In Progress",
                                                color = Color(0xFF1DD75B),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        // Add icon removed
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
                            onEdit = { editingHabit = it },
                            viewModel = viewModel
                        )
                    }
                }
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


    // Modal dialogue to ADD new habit
    if (showAddDialog) {
        HabitFormDialog(
            viewModel = viewModel,
            editHabit = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, complexDesc, category, finalFreq, xpPoints, reminderHour, reminderMinute ->
                // Extract alertType from the encoded description before saving
                val alertType = viewModel.getHabitAlertType(complexDesc)
                viewModel.addHabit(
                    name = name,
                    description = complexDesc,
                    category = category,
                    frequency = finalFreq,
                    isPremium = false,
                    xpPenalty = xpPoints,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    alertType = alertType
                )
                showAddDialog = false
            }
        )
    }

    // Modal dialogue to EDIT existing habit
    if (editingHabit != null) {
        val habit = editingHabit!!
        HabitFormDialog(
            viewModel = viewModel,
            editHabit = habit,
            onDismiss = { editingHabit = null },
            onSave = { name, complexDesc, category, finalFreq, xpPoints, reminderHour, reminderMinute ->
                val updatedHabit = habit.copy(
                    name = name,
                    description = complexDesc,
                    category = category,
                    frequency = finalFreq,
                    xpReward = xpPoints
                )
                viewModel.updateHabitEntity(updatedHabit)
                // Extract alertType from encoded description and schedule with correct type
                val alertType = viewModel.getHabitAlertType(complexDesc)
                if (alertType != "None" && reminderHour >= 0 && reminderMinute >= 0) {
                    viewModel.scheduleExactHabitAlarm(habit.id, name, reminderHour, reminderMinute, alertType)
                } else if (alertType == "None") {
                    viewModel.cancelExactHabitAlarm(habit.id)
                }
                editingHabit = null
            }
        )
    }



    if (showConfetti) {
        ConfettiOverlay()
    }
}
        }
    }
}


// ==========================================
// REUSABLE HABIT FORM DIALOG (Add + Edit)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitFormDialog(
    viewModel: HabitViewModel,
    editHabit: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, complexDesc: String, category: String, freq: String, xpPoints: Int, reminderHour: Int, reminderMinute: Int) -> Unit
) {
    val context = LocalContext.current
    val isEditing = editHabit != null

    // Pre-fill values for editing
    var name by remember { mutableStateOf(editHabit?.name ?: "") }
    var desc by remember { mutableStateOf(if (editHabit != null) viewModel.getCleanDescription(editHabit.description) else "") }
    var category by remember { mutableStateOf(editHabit?.category ?: "Fitness") }
    var freq by remember { mutableStateOf(editHabit?.frequency ?: "Daily") }
    var xpPoints by remember { mutableStateOf((editHabit?.xpReward ?: 15).toString()) }
    val categoryOptions = listOf("Health", "Work", "Personal", "Fitness", "Productivity", "Wellness")

    // Custom styling modifiers
    var selectedPalette by remember { mutableStateOf(if (editHabit != null) viewModel.getHabitPalette(editHabit.description) else "Default") }
    var selectedEmoji by remember { mutableStateOf(if (editHabit != null) viewModel.getHabitEmoji(editHabit.description) else "Default") }
    var alertType by remember { mutableStateOf(if (editHabit != null) viewModel.getHabitAlertType(editHabit.description) else "None") }
    val palettes = remember { mutableStateListOf("Default", "Sapphire Blue", "Emerald Green", "Velvet Amethyst", "Rose Pink", "Amber Orange") }
    val emojis = listOf("Default", "🏃‍♂️", "🧘‍♂️", "📚", "💡", "💧", "🍳", "🧠", "☕", "🔋")

    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var customThemeInput by remember { mutableStateOf("") }

    // --- Flexible Days Scheduler Option ---
    var customizeWeeklyDays by remember { mutableStateOf(editHabit?.frequency?.startsWith("Schedule:") == true) }
    val daysChecklist = remember { 
        val initial = if (editHabit?.frequency?.startsWith("Schedule:") == true) {
            editHabit.frequency.removePrefix("Schedule: ").split(", ").filter { it.isNotBlank() }.toMutableList()
        } else {
            mutableListOf("Mon", "Wed", "Fri")
        }
        mutableStateListOf<String>().also { it.addAll(initial) }
    }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Reminder time picker state
    val existingAlarmTime = if (editHabit != null) viewModel.getHabitAlarmTime(editHabit.id) else ""
    var reminderHour by remember { mutableStateOf(if (existingAlarmTime.isNotBlank()) existingAlarmTime.split(":").getOrNull(0)?.toIntOrNull() ?: -1 else -1) }
    var reminderMinute by remember { mutableStateOf(if (existingAlarmTime.isNotBlank()) existingAlarmTime.split(":").getOrNull(1)?.toIntOrNull() ?: -1 else -1) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isDark = false // Forced light mode for popup
    val accentColor by viewModel.accentColor.collectAsState()

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Notification permission is necessary for receiving alerts", android.widget.Toast.LENGTH_LONG).show()
            alertType = "None"
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.ui.theme.MyApplicationTheme(isDarkMode = false, accentColor = accentColor) {
            val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
            dialogWindowProvider?.window?.let { window ->
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setDimAmount(0.45f)
            }

            Surface(
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Box {
            // Background Layer for Glassmorphism
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
                    .graphicsLayer {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                50f, 50f, android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0xFF111116).copy(alpha = 0.96f),
                                    Color(0xFF000000).copy(alpha = 0.94f)
                                )
                            } else {
                                listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.98f),
                                    Color(0xFFF1F5F9).copy(alpha = 0.96f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                            } else {
                                listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.3f))
                            }
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            )

            // Content Layer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isEditing) "Edit Habit" else "Create New Habit",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEditing) "Modify your habit details" else "Build a new daily ritual",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scrollable form content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // ── Name & Description ──
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g. Wake up at 6AM", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_habit_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    placeholder = { Text("Brief description of your habit", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Category ──
                Text("📂 Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryOptions.forEach { cat ->
                        val sel = category == cat
                        FilterChip(
                            selected = sel,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Schedule ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅 Custom Weekly Schedule", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                                label = { Text(dayLabel, fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    freq = "Schedule: " + daysChecklist.joinToString(", ")
                }

                if (!customizeWeeklyDays) {
                    OutlinedTextField(
                        value = freq,
                        onValueChange = { freq = it },
                        label = { Text("Frequency") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Reminders & Alerts ──
                Text(
                    "🔔 Reminder & Alert",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Alert type — clear segmented buttons (None / Notification / Alarm)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        Triple("None", Icons.Default.NotificationsOff, "Off"),
                        Triple("Notification", Icons.Default.Notifications, "Notif"),
                        Triple("Alarm", Icons.Default.Alarm, "Alarm")
                    ).forEachIndexed { idx, (value, icon, label) ->
                        val isSelected = alertType == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (value == "Notification" || value == "Alarm") {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            val permissionState = androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            )
                                            if (permissionState != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    }
                                    alertType = value 
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = value,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (idx < 2) {
                            Box(modifier = Modifier.width(1.dp).height(44.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        }
                    }
                }

                // Time picker — only visible when alert is active
                if (alertType != "None") {
                    // Display selected time or prompt
                    val timeDisplay = if (reminderHour >= 0 && reminderMinute >= 0) {
                        val amPm = if (reminderHour < 12) "AM" else "PM"
                        val h12 = when {
                            reminderHour == 0 -> 12
                            reminderHour > 12 -> reminderHour - 12
                            else -> reminderHour
                        }
                        String.format("%d:%02d %s", h12, reminderMinute, amPm)
                    } else null

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (timeDisplay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (timeDisplay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (timeDisplay != null) "⏰ $timeDisplay" else "Tap to set time (AM/PM)",
                                fontSize = 14.sp,
                                fontWeight = if (timeDisplay != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (timeDisplay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (reminderHour < 0) {
                        Text(
                            text = "⚠️ Please set a time for your ${alertType.lowercase()} to activate",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showTimePicker) {
                        val initialHour = if (reminderHour >= 0) reminderHour else 8
                        val initialMinute = if (reminderMinute >= 0) reminderMinute else 0
                        val timePickerState = androidx.compose.material3.rememberTimePickerState(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            is24Hour = false
                        )
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    reminderHour = timePickerState.hour
                                    reminderMinute = timePickerState.minute
                                    if (editHabit != null) {
                                        viewModel.scheduleExactHabitAlarm(editHabit.id, editHabit.name, reminderHour, reminderMinute, alertType)
                                    }
                                    showTimePicker = false
                                }) { Text("OK") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                            },
                            text = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.TimePicker(state = timePickerState)
                                }
                            }
                        )
                    }
                }
            } // end scrollable column

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
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

                                onSave(name, complexDesc, category, finalFreq, xpPoints.toIntOrNull() ?: 15, reminderHour, reminderMinute)
                            }
                        },
                        modifier = Modifier.testTag("submit_habit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditing) "Save Changes" else "Create Habit", fontWeight = FontWeight.Bold)
                    }
                }
            } // end outer Column
        } // end Box
        } // end Surface
        } // end MyApplicationTheme
    } // end Dialog
}


@Composable
fun HabitItemCard(
    habit: Habit,
    isCompleted: Boolean,
    onCheckToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Habit) -> Unit,
    viewModel: HabitViewModel
) {
    val categoryColor = when (habit.category) {
        "Health" -> Color(0xFF10B981)
        "Work" -> Color(0xFF8B5CF6)
        "Personal" -> Color(0xFF3B82F6)
        "Fitness" -> Color(0xFFEC4899)
        "Productivity" -> Color(0xFFF59E0B)
        "Wellness" -> Color(0xFF06B6D4)
        else -> Color(0xFF1DD75B)
    }

    val isDark = isSystemInDarkTheme()
    val categoryContainer = categoryColor.copy(alpha = if (isDark) 0.15f else 0.1f)

    val dynamicIcon = when (habit.category) {
        "Health" -> Icons.Default.DirectionsRun
        "Fitness" -> Icons.Default.FitnessCenter
        "Work" -> Icons.Default.Work
        "Personal" -> Icons.Default.MenuBook
        "Productivity" -> Icons.Default.TrendingUp
        "Wellness" -> Icons.Default.SelfImprovement
        else -> Icons.Default.Star
    }

    // Clean description for display (strip metadata prefixes)
    val cleanDescription = viewModel.getCleanDescription(habit.description)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("habit_card_${habit.id}")
            .clickable { onEdit(habit) }
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = cleanDescription.ifBlank { "Complete your habit everyday." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                        // Micro-Step — clean text only, no distracting box background
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isCompleted) Color(0xFF1DD75B) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isCompleted) "Done today" else "Pending",
                                fontSize = 10.sp,
                                color = if (isCompleted) Color(0xFF1DD75B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Right Action Elements (Delete button + Check Circle)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Delete button - replaces confusing MoreVert
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Habit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = onCheckToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete Habit Checkmark",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
