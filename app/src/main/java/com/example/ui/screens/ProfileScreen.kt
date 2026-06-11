package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.userSession.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ViewModel States
    val language by viewModel.appLanguage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val vacationDays by viewModel.vacationDays.collectAsState()
    val quietHoursStart by viewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsState()
    val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
    val dashboardSortMode by viewModel.dashboardSortMode.collectAsState()

    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(session.username) }
    val sharedPrefs = remember { context.getSharedPreferences("habit_tracker_prefs", Context.MODE_PRIVATE) }
    var avatarIndex by remember { mutableStateOf(sharedPrefs.getInt("user_avatar_idx", 0)) }
    val avatars = listOf("🚀", "🦁", "🦊", "🥑", "👻", "⚡", "🐼", "🎯")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Identity Card
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatars.getOrElse(avatarIndex) { "🚀" }, fontSize = 40.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    avatars.forEachIndexed { index, avatar ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (avatarIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    avatarIndex = index
                                    sharedPrefs.edit().putInt("user_avatar_idx", index).apply()
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(avatar, fontSize = 14.sp) }
                    }
                }

                HorizontalDivider()

                if (isEditingName) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            singleLine = true,
                            label = { Text("Display Name") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (editedName.isNotBlank()) {
                                    viewModel.updateUsername(editedName.trim())
                                    isEditingName = false
                                }
                            }
                        ) { Icon(Icons.Default.Check, contentDescription = "Save Name", tint = MaterialTheme.colorScheme.primary) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(text = session.username, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = { editedName = session.username; isEditingName = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Username", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Modals
        var showVacationDialog by remember { mutableStateOf(false) }
        var vacationDaysInput by remember { mutableStateOf(vacationDays.toString()) }

        if (showVacationDialog) {
            AlertDialog(
                onDismissRequest = { showVacationDialog = false },
                title = { Text("Pause Streaks") },
                text = {
                    OutlinedTextField(
                        value = vacationDaysInput,
                        onValueChange = { vacationDaysInput = it },
                        label = { Text("Number of days") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setVacationDays(vacationDaysInput.toIntOrNull() ?: 0)
                        showVacationDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showVacationDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Clean iOS-Style Settings Groups
        SettingsGroup(title = "Appearance") {
            SettingsRowToggle(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                checked = isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
            SettingsRow(icon = Icons.Default.Palette, title = "Accent Color") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf("Blue", "Green", "Red", "Yellow")
                    val colorValues = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFEF4444), Color(0xFFEDE655))
                    colors.forEachIndexed { i, name ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colorValues[i])
                                .border(if (accentColor == name) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { viewModel.setAccentColor(name) }
                        )
                    }
                }
            }
        }

        SettingsGroup(title = "Preferences") {
            SettingsDropdown(
                icon = Icons.Default.Language,
                title = "Language",
                options = listOf("System Default", "English", "Hindi"),
                selectedValue = language,
                onValueChange = { viewModel.setAppLanguage(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
            SettingsDropdown(
                icon = Icons.Default.Sort,
                title = "Dashboard Sorting",
                options = listOf("Chronological", "Alphabetical", "Manual"),
                selectedValue = dashboardSortMode,
                onValueChange = { viewModel.setDashboardSortMode(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
            SettingsDropdown(
                icon = Icons.Default.CalendarToday,
                title = "First Day of Week",
                options = listOf("Monday", "Sunday"),
                selectedValue = firstDayOfWeek,
                onValueChange = { viewModel.setFirstDayOfWeek(it) }
            )
        }

        SettingsGroup(title = "Focus & Routines") {
            SettingsRowToggle(
                icon = Icons.Default.Pause,
                title = "Pause Streaks (Vacation)",
                subtitle = if (vacationDays > 0) "Paused for $vacationDays days" else "Freeze streaks temporarily",
                checked = vacationDays > 0,
                onCheckedChange = { if (it) { showVacationDialog = true } else { viewModel.setVacationDays(0) } }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.NotificationsPaused, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Quiet Hours", fontSize = 16.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quietHoursStart,
                        onValueChange = { viewModel.setQuietHoursStart(it) },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quietHoursEnd,
                        onValueChange = { viewModel.setQuietHoursEnd(it) },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Privacy Card
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Privacy Shield",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("100% Private & Secure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Your habits and personal data stay right here on your phone. We don't use external cloud servers, meaning your progress is completely private and for your eyes only.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 16.sp)
        }
        action()
    }
}

@Composable
fun SettingsRowToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 16.sp)
        }
        
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedValue, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
