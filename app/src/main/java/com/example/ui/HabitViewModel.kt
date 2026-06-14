package com.example.ui

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Habit
import com.example.data.model.UserSession
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HabitViewModel(
    private val repository: HabitRepository,
    private val context: Context
) : ViewModel() {

    val userSession: StateFlow<UserSession> = repository.userSession
    val allBadges = repository.allBadges
    val socialFeed = repository.socialFeed

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Fuzzy real-time search of habits (Category, Name, Description)
    val habitsList: StateFlow<List<Habit>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            repository.searchHabits(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Complete/Pending statistics computation mapping
    private val _habitCompletions = MutableStateFlow<Map<Long, List<String>>>(emptyMap())
    val habitCompletions: StateFlow<Map<Long, List<String>>> = _habitCompletions.asStateFlow()

    init {
        // Load completions periodically to compute analytics metrics
        viewModelScope.launch {
            repository.getAllCompletions().collect { list ->
                val grouped = list.groupBy { it.habitId }.mapValues { entry ->
                    entry.value.map { it.dateString }
                }
                _habitCompletions.value = grouped
            }
        }
    }

    // --- Search ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val prefs = context.getSharedPreferences("zenter_tracker_shared_prefs", Context.MODE_PRIVATE)

    // --- FEATURE 1: Theme State Management & New Settings ---
    private val _selectedThemeState = MutableStateFlow(prefs.getString("selected_theme", "Obsidian Dark") ?: "Obsidian Dark")
    val selectedThemeState: StateFlow<String> = _selectedThemeState.asStateFlow()

    fun selectTheme(themeName: String) {
        _selectedThemeState.value = themeName
        prefs.edit().putString("selected_theme", themeName).apply()
    }

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "System Default") ?: "System Default")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()
    fun setAppLanguage(language: String) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language).apply()
    }

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        prefs.edit().putBoolean("is_dark_mode", isDark).apply()
    }

    private val _accentColor = MutableStateFlow(prefs.getString("accent_color", "Blue") ?: "Blue")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()
    fun setAccentColor(color: String) {
        _accentColor.value = color
        prefs.edit().putString("accent_color", color).apply()
    }

    private val _vacationDays = MutableStateFlow(prefs.getInt("vacation_days", 0))
    val vacationDays: StateFlow<Int> = _vacationDays.asStateFlow()
    fun setVacationDays(days: Int) {
        _vacationDays.value = days
        prefs.edit().putInt("vacation_days", days).apply()
    }

    private val _quietHoursStart = MutableStateFlow(prefs.getString("quiet_hours_start", "22:00") ?: "22:00")
    val quietHoursStart: StateFlow<String> = _quietHoursStart.asStateFlow()
    fun setQuietHoursStart(time: String) {
        _quietHoursStart.value = time
        prefs.edit().putString("quiet_hours_start", time).apply()
        setupSmartNotifications()
    }

    private val _quietHoursEnd = MutableStateFlow(prefs.getString("quiet_hours_end", "07:00") ?: "07:00")
    val quietHoursEnd: StateFlow<String> = _quietHoursEnd.asStateFlow()
    fun setQuietHoursEnd(time: String) {
        _quietHoursEnd.value = time
        prefs.edit().putString("quiet_hours_end", time).apply()
        setupSmartNotifications()
    }

    private val _firstDayOfWeek = MutableStateFlow(prefs.getString("first_day_of_week", "Monday") ?: "Monday")
    val firstDayOfWeek: StateFlow<String> = _firstDayOfWeek.asStateFlow()
    fun setFirstDayOfWeek(day: String) {
        _firstDayOfWeek.value = day
        prefs.edit().putString("first_day_of_week", day).apply()
    }

    private val _dashboardSortMode = MutableStateFlow(prefs.getString("dashboard_sort_mode", "Chronological") ?: "Chronological")
    val dashboardSortMode: StateFlow<String> = _dashboardSortMode.asStateFlow()
    fun setDashboardSortMode(mode: String) {
        _dashboardSortMode.value = mode
        prefs.edit().putString("dashboard_sort_mode", mode).apply()
    }
    
    fun setupSmartNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, com.example.notifications.HabitAlarmReceiver::class.java).apply {
            putExtra("alarm_title", "Keep the momentum going!")
            putExtra("alarm_message", "Consistency builds discipline. Take 1 minute to check off your habits!")
            putExtra("is_smart_notification", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9998,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Every 2 hours interval
        alarmManager.setRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 2 * 60 * 60 * 1000L,
            2 * 60 * 60 * 1000L,
            pendingIntent
        )
    }

    // --- FEATURE 2: Water Tracker Widget Progress ---
    private val _dailyWaterLogged = MutableStateFlow(run {
        // Reset water count if it's a new day
        val storedDate = prefs.getString("water_last_date", "") ?: ""
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (storedDate != todayDate) {
            prefs.edit().putInt("water_logged_today", 0).putString("water_last_date", todayDate).apply()
            0
        } else {
            prefs.getInt("water_logged_today", 0)
        }
    })
    val dailyWaterLogged: StateFlow<Int> = _dailyWaterLogged.asStateFlow()

    private val _dailyWaterGoal = MutableStateFlow(prefs.getInt("water_goal", 10))
    val dailyWaterGoal: StateFlow<Int> = _dailyWaterGoal.asStateFlow()

    fun addWaterGlass() {
        val newVal = _dailyWaterLogged.value + 1
        _dailyWaterLogged.value = newVal
        prefs.edit().putInt("water_logged_today", newVal).apply()
        // Reward small XP for hydration habit integration
        viewModelScope.launch {
            repository.buyCoins(5, 0.0) // Give 5 free progress coins
        }
    }

    fun setWaterGoal(goal: Int) {
        _dailyWaterGoal.value = goal
        prefs.edit().putInt("water_goal", goal).apply()
    }

    fun removeWaterGlass() {
        val newVal = maxOf(0, _dailyWaterLogged.value - 1)
        _dailyWaterLogged.value = newVal
        prefs.edit().putInt("water_logged_today", newVal).apply()
    }

    fun resetWaterDaily() {
        _dailyWaterLogged.value = 0
        prefs.edit().putInt("water_logged_today", 0).apply()
    }

    // --- FEATURE 3: Streak Freezers Store Item ---
    private val _streakFreezerCount = MutableStateFlow(prefs.getInt("streak_freezers_count", 0))
    val streakFreezerCount: StateFlow<Int> = _streakFreezerCount.asStateFlow()

    fun buyStreakFreezer(): Boolean {
        val user = userSession.value
        if (user.coins >= 50) {
            val updatedCoins = user.coins - 50
            val updatedFreezers = _streakFreezerCount.value + 1
            _streakFreezerCount.value = updatedFreezers
            prefs.edit().putInt("streak_freezers_count", updatedFreezers).apply()
            // Deduct coins inside repository
            viewModelScope.launch {
                repository.buyPremium(50) // Deduct 50 coins
            }
            return true
        }
        return false
    }

    fun protectStreakWithFreezer(habit: Habit) {
        if (_streakFreezerCount.value > 0) {
            val updatedFreezers = _streakFreezerCount.value - 1
            _streakFreezerCount.value = updatedFreezers
            prefs.edit().putInt("streak_freezers_count", updatedFreezers).apply()
            
            // Increment streaks in-place as a cheat-code protector!
            viewModelScope.launch {
                val updatedHabit = habit.copy(
                    currentStreak = habit.currentStreak + 1,
                    topStreak = maxOf(habit.topStreak, habit.currentStreak + 1)
                )
                repository.addHabit(updatedHabit)
            }
        }
    }

    // --- FEATURE 4: Specific Days Scheduling ---
    fun getHabitActiveDays(habitId: Long): List<String> {
        val daysStr = prefs.getString("habit_days_$habitId", "") ?: ""
        return if (daysStr.isBlank()) emptyList() else daysStr.split(",")
    }

    fun saveHabitActiveDays(habitId: Long, days: List<String>) {
        val daysStr = days.joinToString(",")
        prefs.edit().putString("habit_days_$habitId", daysStr).apply()
    }

    // --- FEATURE 5: Habit Completion Progress Note ---
    fun getHabitNoteToday(habitId: Long, dateString: String): String {
        return prefs.getString("habit_note_${habitId}_$dateString", "") ?: ""
    }

    fun saveHabitNoteToday(habitId: Long, dateString: String, note: String) {
        prefs.edit().putString("habit_note_${habitId}_$dateString", note).apply()
    }

    // --- FEATURE 6: Habit Category Filter ---
    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    // --- FEATURE 7: Local JSON Backup Share ---
    fun exportBackupJSON(): String {
        val habits = habitsList.value
        val sb = java.lang.StringBuilder()
        sb.append("[\n")
        for (i in habits.indices) {
            val h = habits[i]
            val daysStr = getHabitActiveDays(h.id).joinToString("-")
            sb.append("  {\n")
            sb.append("    \"name\": \"${h.name.replace("\"", "\\\"")}\",\n")
            sb.append("    \"description\": \"${h.description.replace("\"", "\\\"")}\",\n")
            sb.append("    \"category\": \"${h.category}\",\n")
            sb.append("    \"frequency\": \"${h.frequency}\",\n")
            sb.append("    \"streak\": ${h.currentStreak},\n")
            sb.append("    \"days\": \"$daysStr\",\n")
            sb.append("    \"topStreak\": ${h.topStreak}\n")
            sb.append("  }${if (i < habits.size - 1) "," else ""}\n")
        }
        sb.append("]")
        return sb.toString()
    }

    // --- FEATURE 8: Habit Alarm scheduler (Exact reminds) ---
    fun getHabitAlarmTime(habitId: Long): String {
        return prefs.getString("habit_alarm_$habitId", "") ?: ""
    }

    fun scheduleExactHabitAlarm(habitId: Long, habitName: String, hour: Int, minute: Int, alertType: String = "Notification") {
        // Persist alarm time and metadata for BootReceiver to reschedule after reboot
        prefs.edit()
            .putString("habit_alarm_$habitId", String.format("%02d:%02d", hour, minute))
            .putString("habit_name_$habitId", habitName)
            .putString("habit_alert_type_$habitId", alertType)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, com.example.notifications.HabitAlarmReceiver::class.java).apply {
            // Pass all required extras so receiver knows what type and whose alarm this is
            putExtra(com.example.notifications.HabitAlarmReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(com.example.notifications.HabitAlarmReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(com.example.notifications.HabitAlarmReceiver.EXTRA_ALERT_TYPE, alertType)
            putExtra("alarm_title", if (alertType == "Alarm") "⏰ Alarm: $habitName!" else "🔔 Reminder: $habitName")
            putExtra("alarm_message", "Don't break your streak! Time to log \"$habitName\".")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 1000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("HabitViewModel", "Scheduled [$alertType] alarm for '$habitName' at $hour:$minute")
        } catch (e: Exception) {
            Log.e("HabitViewModel", "Failed to set exact alarm", e)
        }
    }

    fun cancelExactHabitAlarm(habitId: Long) {
        prefs.edit().remove("habit_alarm_$habitId").apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, com.example.notifications.HabitAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 1000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    // --- FEATURE 9: Full Screen Alarm Wake-up popup ---
    private val _alarmOverlayState = MutableStateFlow<Triple<Boolean, String, String>?>(null)
    val alarmOverlayState: StateFlow<Triple<Boolean, String, String>?> = _alarmOverlayState.asStateFlow()

    fun triggerAlarmOverlay(title: String, msg: String) {
        _alarmOverlayState.value = Triple(true, title, msg)
    }

    fun dismissAlarmOverlay() {
        _alarmOverlayState.value = null
    }

    // --- FEATURE 10: Global Daily Reminder Alarm ---
    private val _globalReminderEnabled = MutableStateFlow(prefs.getBoolean("global_reminder_enabled", false))
    val globalReminderEnabled: StateFlow<Boolean> = _globalReminderEnabled.asStateFlow()

    private val _globalReminderTime = MutableStateFlow(prefs.getString("global_reminder_time", "08:00") ?: "08:00")
    val globalReminderTime: StateFlow<String> = _globalReminderTime.asStateFlow()

    fun toggleGlobalReminder(enabled: Boolean, hour: Int = 8, minute: Int = 0) {
        _globalReminderEnabled.value = enabled
        prefs.edit().putBoolean("global_reminder_enabled", enabled).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, com.example.notifications.HabitAlarmReceiver::class.java).apply {
            putExtra(com.example.notifications.HabitAlarmReceiver.EXTRA_HABIT_ID, -1L)
            putExtra(com.example.notifications.HabitAlarmReceiver.EXTRA_ALERT_TYPE, "Notification")
            putExtra("alarm_title", "📅 Daily Habit Check-In!")
            putExtra("alarm_message", "It's time to log your progress for today! Lock in your streaks and build consistency.")
            putExtra("is_smart_notification", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (enabled) {
            val timeStr = String.format("%02d:%02d", hour, minute)
            _globalReminderTime.value = timeStr
            prefs.edit().putString("global_reminder_time", timeStr).apply()

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                // Use setExactAndAllowWhileIdle — receiver reschedules itself for next day
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d("HabitViewModel", "Scheduled exact global reminder at $hour:$minute")
            } catch (e: Exception) {
                Log.e("HabitViewModel", "Failed to set global reminder alarm", e)
            }
        } else {
            alarmManager.cancel(pendingIntent)
            Log.d("HabitViewModel", "Cancelled global daily reminder")
        }
    }

    // --- Metadata Parsing Helpers ---
    fun getCleanDescription(description: String): String {
        return description
            .replace(Regex("\\[🎨[^\\]]*\\]"), "")
            .replace(Regex("\\[🔔[^\\]]*\\]"), "")
            .replace(Regex("\\[[^\\]]{1,4}\\]"), "") // emoji tags like [🏃‍♂️]
            .trim()
    }

    fun getHabitAlertType(description: String): String {
        val match = Regex("\\[🔔([^\\]]*)\\]").find(description)
        return match?.groupValues?.get(1) ?: "None"
    }

    fun getHabitPalette(description: String): String {
        val match = Regex("\\[🎨([^\\]]*)\\]").find(description)
        return match?.groupValues?.get(1) ?: "Default"
    }

    fun getHabitEmoji(description: String): String {
        // Match emoji-only tags (1-4 chars that aren't 🎨 or 🔔 prefixed)
        val match = Regex("\\[([^🎨🔔][^\\]]{0,3})\\]").find(description)
        return match?.groupValues?.get(1) ?: "Default"
    }

    // --- Habits CRUD Actions ---
    fun addHabit(
        name: String,
        description: String,
        category: String,
        frequency: String,
        isPremium: Boolean,
        xpPenalty: Int = 10,
        reminderHour: Int = -1,
        reminderMinute: Int = -1,
        alertType: String = "None"
    ) {
        viewModelScope.launch {
            val newId = repository.addHabit(
                Habit(
                    name = name,
                    description = description,
                    category = category,
                    frequency = frequency,
                    xpReward = xpPenalty,
                    isPremiumOnly = isPremium
                )
            )
            // Schedule alarm immediately with the new habit's ID
            // (Previously read from prefs which was never set for new habits — now fixed)
            if (alertType != "None" && reminderHour >= 0 && reminderMinute >= 0) {
                scheduleExactHabitAlarm(newId, name, reminderHour, reminderMinute, alertType)
                Log.d("HabitViewModel", "Scheduled [$alertType] for new habit '$name' (id=$newId) at $reminderHour:$reminderMinute")
            }
        }
    }

    fun updateHabitEntity(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            // Schedule alarm/notification based on alert type if time is set
            val alertType = getHabitAlertType(habit.description)
            val alarmTime = getHabitAlarmTime(habit.id)
            if (alertType != "None" && alarmTime.isNotBlank()) {
                val parts = alarmTime.split(":")
                if (parts.size == 2) {
                    scheduleExactHabitAlarm(habit.id, habit.name, parts[0].toIntOrNull() ?: 8, parts[1].toIntOrNull() ?: 0, alertType)
                }
            } else if (alertType == "None") {
                cancelExactHabitAlarm(habit.id)
            }
        }
    }

    private val _showConfettiEvent = MutableSharedFlow<Boolean>()
    val showConfettiEvent: SharedFlow<Boolean> = _showConfettiEvent.asSharedFlow()

    fun completeHabit(habit: Habit, dateString: String) {
        viewModelScope.launch {
            val isNewPr = repository.toggleHabitCompletion(habit, dateString)
            if (isNewPr) {
                _showConfettiEvent.emit(true)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun archiveHabit(id: Long) {
        viewModelScope.launch {
            repository.archiveHabit(id)
        }
    }

    // --- Authentication Actions ---
    fun loginAsGuest() {
        repository.loginAsGuest()
    }

    fun updateUsername(newName: String) {
        repository.updateUsername(newName)
    }

    fun upgradeToCloud(email: String) {
        repository.upgradeToCloudUser(email)
    }

    fun enableAdminMode() {
        repository.makeAdmin()
    }

    fun toggleBiometrics(enabled: Boolean) {
        repository.toggleBiometrics(enabled)
    }

    fun toggleMfa(enabled: Boolean) {
        repository.toggleMfa(enabled)
    }

    fun unlockPremiumDirectly() {
        repository.unlockPremiumDirectly()
    }

    fun checkoutPremiumUpgrade(): Boolean {
        // Costs 150 coins to upgrade to Pro inside app
        return repository.buyPremium(150)
    }

    fun purchaseInAppPayment(amount: Int, cost: Double) {
        // Simulated play store purchase success
        repository.buyCoins(amount, cost)
    }

    fun cheerPost(postId: String, isPostCheered: Boolean) {
        viewModelScope.launch {
            repository.cheerPost(postId, isPostCheered)
        }
    }

    fun publishCustomStreakToFeed(habitName: String, streak: Int) {
        viewModelScope.launch {
            repository.addHabit(
                Habit(
                    name = habitName,
                    description = "Community Shared streak!",
                    category = "Social",
                    currentStreak = streak
                )
            )
        }
    }

    // --- Simulated Synchronizations ---
    private val _syncState = MutableStateFlow<String>("Synced") // Sycned, Syncing, Failed
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    fun triggerSync() {
        viewModelScope.launch {
            _syncState.value = "Syncing"
            val success = repository.triggerCloudSync()
            _syncState.value = if (success) "Synced" else "Failed"
        }
    }

    // --- Report Export Generators (PDF / CSV) ---
    fun exportReportAsCSV(context: Context): File? {
        try {
            val file = File(context.cacheDir, "habit_tracker_progress_report_${System.currentTimeMillis()}.csv")
            val outputStream = FileOutputStream(file)
            val writer = outputStream.bufferedWriter()

            writer.write("Habit Progress & Engagement Report\n")
            writer.write("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            writer.write("User: ${userSession.value.username} (${userSession.value.role})\n")
            writer.write("Total XP: ${userSession.value.totalXp} XP\n\n")

            writer.write("Habit Name,Category,Frequency,Current Streak,Top Streak,XP Reward\n")
            val habits = habitsList.value
            for (habit in habits) {
                writer.write("\"${habit.name}\",\"${habit.category}\",\"${habit.frequency}\",${habit.currentStreak},${habit.topStreak},${habit.xpReward}\n")
            }

            writer.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            Log.e("HabitViewModel", "Failed to write CSV", e)
            return null
        }
    }

    fun exportReportAsPDF(context: Context): File? {
        try {
            val file = File(context.cacheDir, "habit_tracker_progress_report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)

            val document = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
            val page = document.startPage(pageInfo)
            
            val canvas = page.canvas
            val paint = android.graphics.Paint()
            
            // Header Title
            paint.isFakeBoldText = true
            paint.textSize = 22f
            canvas.drawText("HABIT TRACKER PERFORMANCE REPORT", 40f, 60f, paint)
            
            // Divider
            paint.strokeWidth = 2f
            canvas.drawLine(40f, 75f, 555f, 75f, paint)
            
            // Meta info
            paint.isFakeBoldText = false
            paint.textSize = 12f
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText("Report Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}", 40f, 100f, paint)
            canvas.drawText("User Account: ${userSession.value.username} [${userSession.value.role}]", 40f, 120f, paint)
            canvas.drawText("Account Tier: ${if (userSession.value.isPremium) "👑 Premium Active" else "⚖️ Standard (Free)"}", 40f, 140f, paint)
            canvas.drawText("Aggregate Score: ${userSession.value.totalXp} XP Points Accumulated", 40f, 160f, paint)

            canvas.drawLine(40f, 185f, 555f, 185f, paint)

            // Table Header
            paint.isFakeBoldText = true
            paint.textSize = 11f
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("Habit Profile", 40f, 210f, paint)
            canvas.drawText("Category", 220f, 210f, paint)
            canvas.drawText("Frequency", 320f, 210f, paint)
            canvas.drawText("Streak (Current / Best)", 420f, 210f, paint)

            // Table Rows
            paint.isFakeBoldText = false
            paint.textSize = 9f
            var startY = 235f
            val habits = habitsList.value
            for (habit in habits) {
                if (startY > 800f) break // page break limits
                canvas.drawText(habit.name.take(24), 40f, startY, paint)
                canvas.drawText(habit.category, 220f, startY, paint)
                canvas.drawText(habit.frequency, 320f, startY, paint)
                canvas.drawText("${habit.currentStreak} days / ${habit.topStreak} days", 420f, startY, paint)
                startY += 25f
            }

            document.finishPage(page)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            Log.e("HabitViewModel", "Failed to write PDF", e)
            return null
        }
    }
}

class HabitViewModelFactory(
    private val repository: HabitRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
