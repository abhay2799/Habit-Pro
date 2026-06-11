package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.HabitDao
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.Badge
import com.example.data.model.SocialPost
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitRepository(
    private val habitDao: HabitDao,
    private val context: Context
) {
    val activeHabits: Flow<List<Habit>> = habitDao.getActiveHabits()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allBadges: Flow<List<Badge>> = habitDao.getAllBadges()
    val socialFeed: Flow<List<SocialPost>> = habitDao.getSocialFeed()

    // Preferences for dynamic simulation of offline-first accounts, XP, Coins, and Premium Lock
    private val sharedPrefs = context.getSharedPreferences("habit_tracker_prefs", Context.MODE_PRIVATE)

    private val _userSession = MutableStateFlow(loadSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    private fun loadSession(): UserSession {
        val userId = sharedPrefs.getString("user_id", "guest_user") ?: "guest_user"
        val username = sharedPrefs.getString("username", "Guest Tracker") ?: "Guest Tracker"
        val isGuest = sharedPrefs.getBoolean("is_guest", true)
        val role = sharedPrefs.getString("role", "User") ?: "User"
        val xp = sharedPrefs.getInt("total_xp", 0)
        val coins = sharedPrefs.getInt("coins", 100)
        val isPremium = sharedPrefs.getBoolean("is_premium", false)
        val biometric = sharedPrefs.getBoolean("biometric_enabled", false)
        val mfa = sharedPrefs.getBoolean("mfa_enabled", false)

        return UserSession(userId, username, isGuest, role, xp, coins, isPremium, biometric, mfa)
    }

    private fun saveSession(session: UserSession) {
        sharedPrefs.edit().apply {
            putString("user_id", session.userId)
            putString("username", session.username)
            putBoolean("is_guest", session.isGuest)
            putString("role", session.role)
            putInt("total_xp", session.totalXp)
            putInt("coins", session.coins)
            putBoolean("is_premium", session.isPremium)
            putBoolean("biometric_enabled", session.biometricEnabled)
            putBoolean("mfa_enabled", session.mfaEnabledOnCloud)
        }.apply()
        _userSession.value = session
    }

    // --- Authentication Actions ---
    fun loginAsGuest() {
        val current = loadSession()
        saveSession(current.copy(
            userId = "guest_" + System.currentTimeMillis().toString().takeLast(6),
            username = "Guest User",
            isGuest = true,
            role = "User"
        ))
    }

    fun updateUsername(newName: String) {
        val current = loadSession()
        saveSession(current.copy(
            username = newName
        ))
    }

    fun upgradeToCloudUser(email: String) {
        val current = loadSession()
        saveSession(current.copy(
            userId = "cloud_" + email.hashCode().toString(),
            username = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            isGuest = false,
            role = "User",
            coins = current.coins + 150 // Reward for cloud backup setup
        ))
    }

    fun makeAdmin() {
        val current = loadSession()
        saveSession(current.copy(role = "Admin"))
    }

    fun toggleBiometrics(enabled: Boolean) {
        val current = loadSession()
        saveSession(current.copy(biometricEnabled = enabled))
    }

    fun toggleMfa(enabled: Boolean) {
        val current = loadSession()
        saveSession(current.copy(mfaEnabledOnCloud = enabled))
    }

    fun unlockPremiumDirectly() {
        val current = loadSession()
        saveSession(current.copy(isPremium = true))
    }

    fun buyPremium(coinsRequired: Int): Boolean {
        val current = loadSession()
        if (current.coins >= coinsRequired) {
            saveSession(current.copy(
                coins = current.coins - coinsRequired,
                isPremium = true
            ))
            return true
        }
        return false
    }

    fun buyCoins(amount: Int, costMultiplier: Double): Boolean {
        // Simulated play-store purchase of in-app items (some paid items)
        val current = loadSession()
        saveSession(current.copy(
            coins = current.coins + amount
        ))
        return true
    }

    // --- Search Utility ---
    fun searchHabits(query: String): Flow<List<Habit>> {
        return if (query.isBlank()) activeHabits else habitDao.searchHabits(query)
    }

    // --- Habits CRUD ---
    suspend fun addHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteCompletionsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun archiveHabit(id: Long) {
        habitDao.archiveHabit(id)
    }

    // --- Completions Queries ---
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return habitDao.getCompletionsForHabit(habitId)
    }

    fun getAllCompletions(): Flow<List<HabitCompletion>> {
        return habitDao.getAllCompletions()
    }

    // --- Core Completion Engine & Gamification Streaks / Badges ---
    suspend fun toggleHabitCompletion(habit: Habit, dateString: String): Boolean {
        val completions = habitDao.getCompletionsForHabit(habit.id).first()
        val exists = completions.any { it.dateString == dateString }
        var isNewPr = false

        if (exists) {
            habitDao.removeCompletion(habit.id, dateString)
            recalculateStreakAndXP(habit, completions.filterNot { it.dateString == dateString }, xpCost = -habit.xpReward)
        } else {
            val newCompletion = HabitCompletion(habitId = habit.id, dateString = dateString)
            habitDao.insertCompletion(newCompletion)
            val updatedCompletions = completions + newCompletion
            isNewPr = recalculateStreakAndXP(habit, updatedCompletions, xpCost = habit.xpReward)
            
            // Post accountability record for significant ticks
            val session = _userSession.value
            if (habit.currentStreak + 1 >= 3) {
                val congratsMsg = when {
                    habit.currentStreak + 1 == 3 -> "is on fire with a 3-day streak! 🔥"
                    habit.currentStreak + 1 == 7 -> "crushed a solid 7-day milestone! 🏆"
                    habit.currentStreak + 1 > 10 -> "is unstoppable at ${habit.currentStreak + 1} days! 🚀"
                    else -> "completed check-in for '${habit.name}'!"
                }
                habitDao.insertSocialPost(
                    SocialPost(
                        username = session.username,
                        habitName = habit.name,
                        achievementText = congratsMsg
                    )
                )
            }
        }
        return isNewPr
    }

    private suspend fun recalculateStreakAndXP(habit: Habit, completions: List<HabitCompletion>, xpCost: Int): Boolean {
        val calculatedStreaks = calculateStreak(completions)
        val currentStr = calculatedStreaks.first
        val topStr = maxOf(habit.topStreak, calculatedStreaks.second)
        
        val isNewPr = topStr > habit.topStreak && currentStr == topStr && currentStr > 1

        val updatedHabit = habit.copy(
            currentStreak = currentStr,
            topStreak = topStr
        )
        habitDao.updateHabit(updatedHabit)

        // Award/Deduct XP and Coins (10% of XP as coins)
        val session = _userSession.value
        val newXp = maxOf(0, session.totalXp + xpCost)
        val newCoins = maxOf(0, session.coins + if (xpCost > 0) xpCost / 2 else xpCost / 4)
        saveSession(session.copy(totalXp = newXp, coins = newCoins))

        // Check badge parameters
        evaluateBadges(newXp, currentStr)
        
        return isNewPr
    }

    fun calculateStreak(completions: List<HabitCompletion>): Pair<Int, Int> {
        if (completions.isEmpty()) return Pair(0, 0)

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val completedDatesSet = completions.mapNotNull {
            try { format.parse(it.dateString) } catch (e: Exception) { null }
        }.map {
            val cal = Calendar.getInstance()
            cal.time = it
            // Reset time segment
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSortedSet()

        if (completedDatesSet.isEmpty()) return Pair(0, 0)

        // Find consecutive blocks leading back from today or yesterday
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.timeInMillis

        var currentStreak = 0
        var maxStreak = 0
        var streakAcc = 0
        
        var iterCal = Calendar.getInstance()
        iterCal.timeInMillis = completedDatesSet.first()

        var prevTime: Long? = null
        for (time in completedDatesSet) {
            if (prevTime == null) {
                streakAcc = 1
            } else {
                val diffDays = (time - prevTime) / (1000 * 60 * 60 * 24)
                if (diffDays == 1L) {
                    streakAcc++
                } else if (diffDays > 1L) {
                    if (streakAcc > maxStreak) maxStreak = streakAcc
                    streakAcc = 1
                }
            }
            prevTime = time
        }
        if (streakAcc > maxStreak) maxStreak = streakAcc

        // To find *current streak*: starting from today or yesterday, check days backward
        var checkTime = today
        if (completedDatesSet.contains(checkTime)) {
            while (completedDatesSet.contains(checkTime)) {
                currentStreak++
                val c = Calendar.getInstance()
                c.timeInMillis = checkTime
                c.add(Calendar.DAY_OF_YEAR, -1)
                checkTime = c.timeInMillis
            }
        } else if (completedDatesSet.contains(yesterday)) {
            checkTime = yesterday
            while (completedDatesSet.contains(checkTime)) {
                currentStreak++
                val c = Calendar.getInstance()
                c.timeInMillis = checkTime
                c.add(Calendar.DAY_OF_YEAR, -1)
                checkTime = c.timeInMillis
            }
        } else {
            currentStreak = 0
        }

        return Pair(currentStreak, maxStreak)
    }

    private suspend fun evaluateBadges(totalXp: Int, currentStreak: Int) {
        val allBadgesList = habitDao.getAllBadges().first()
        val totalComps = habitDao.getAllCompletions().first().size

        for (badge in allBadgesList) {
            if (badge.isUnlocked) continue

            var shouldUnlock = false
            var currentProgress = 0

            when (badge.requirementType) {
                "XP" -> {
                    currentProgress = minOf(badge.requirementValue, totalXp)
                    shouldUnlock = totalXp >= badge.requirementValue
                }
                "STREAK" -> {
                    currentProgress = minOf(badge.requirementValue, currentStreak)
                    shouldUnlock = currentStreak >= badge.requirementValue
                }
                "TOTAL_COMPLETIONS" -> {
                    currentProgress = minOf(badge.requirementValue, totalComps)
                    shouldUnlock = totalComps >= badge.requirementValue
                }
                "SOCIAL" -> {
                    // Triggered when interacting (cheering) with feed
                }
            }

            if (shouldUnlock) {
                habitDao.unlockBadge(badge.id, System.currentTimeMillis())
                // Throw social announcement
                val session = _userSession.value
                habitDao.insertSocialPost(
                    SocialPost(
                        username = session.username,
                        habitName = badge.title,
                        achievementText = "unlocked the awesome badge: '${badge.title}'! 🏅"
                    )
                )
            } else if (currentProgress != badge.progress) {
                habitDao.updateBadgeProgress(badge.id, currentProgress)
            }
        }
    }

    suspend fun incrementSocialBadgeProgress() {
        val badge = habitDao.getAllBadges().first().find { it.id == "social_first" }
        if (badge != null && !badge.isUnlocked) {
            habitDao.unlockBadge("social_first", System.currentTimeMillis())
        }
    }

    suspend fun cheerPost(postId: String, isPostCheered: Boolean) {
        val diff = if (isPostCheered) -1 else 1
        habitDao.updateCheerStatus(postId, !isPostCheered, diff)
        incrementSocialBadgeProgress()
    }

    // --- Simulated Cloud Sync Flow ---
    suspend fun triggerCloudSync(): Boolean {
        // Real cloud syncing simulate latency and response
        kotlinx.coroutines.delay(1200) // Aesthetic visual timing representation
        return true
    }
}
