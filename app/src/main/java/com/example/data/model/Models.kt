package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Health", // Health, Focus, Mind, Social, Custom
    val frequency: String = "Daily", // Daily, Weekly
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val currentStreak: Int = 0,
    val topStreak: Int = 0,
    val xpReward: Int = 10,
    val isPremiumOnly: Boolean = false // Monetization filter
)

@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val dateString: String, // format "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val requirementType: String, // STREAK, XP, SOCIAL, TOTAL_COMPLETIONS
    val requirementValue: Int,
    val progress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val isPremium: Boolean = false
)

@Entity(tableName = "social_posts")
data class SocialPost(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val userAvatarUrl: String = "",
    val habitName: String,
    val achievementText: String, // "completed 10 day streak!"
    val isCheered: Boolean = false,
    val cheerCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserSession(
    val userId: String = "guest_user",
    val username: String = "Guest Tracker",
    val isGuest: Boolean = true,
    val role: String = "User", // User, Admin
    val totalXp: Int = 0,
    val coins: Int = 200,
    val isPremium: Boolean = false,
    val biometricEnabled: Boolean = false,
    val mfaEnabledOnCloud: Boolean = false
)
