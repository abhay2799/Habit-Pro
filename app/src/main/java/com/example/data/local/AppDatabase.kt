package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.Badge
import com.example.data.model.SocialPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Habit::class, HabitCompletion::class, Badge::class, SocialPost::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabase(database.habitDao())
                    }
                }
            }

            private suspend fun seedDatabase(habitDao: HabitDao) {
                // Seed initial badges
                val initialBadges = listOf(
                    Badge(
                        id = "first_step",
                        title = "First Step",
                        description = "Complete your very first habit tracking check-in.",
                        iconName = "check_circle",
                        requirementType = "TOTAL_COMPLETIONS",
                        requirementValue = 1,
                        isPremium = false
                    ),
                    Badge(
                        id = "week_streak",
                        title = "Consistency Champion",
                        description = "Maintain a steady 7-day habit streak.",
                        iconName = "local_fire_department",
                        requirementType = "STREAK",
                        requirementValue = 7,
                        isPremium = false
                    ),
                    Badge(
                        id = "xp_100",
                        title = "XP Initiate",
                        description = "Amass 100 XP points by finishing tasks.",
                        iconName = "military_tech",
                        requirementType = "XP",
                        requirementValue = 100,
                        isPremium = false
                    ),
                    Badge(
                        id = "xp_500",
                        title = "Power Habitier",
                        description = "Reach 500 total XP to join the elite track.",
                        iconName = "workspace_premium",
                        requirementType = "XP",
                        requirementValue = 500,
                        isPremium = true
                    ),
                    Badge(
                        id = "social_first",
                        title = "Team Player",
                        description = "Interact with the Community Accountability Feed.",
                        iconName = "people",
                        requirementType = "SOCIAL",
                        requirementValue = 1,
                        isPremium = false
                    ),
                    Badge(
                        id = "month_master",
                        title = "Habits Architect",
                        description = "Complete a demanding 30-day streak of any healthy action.",
                        iconName = "star",
                        requirementType = "STREAK",
                        requirementValue = 30,
                        isPremium = true
                    )
                )
                habitDao.insertBadges(initialBadges)

                // Seed some placeholder social feed posts
                val initialSocialPosts = listOf(
                    SocialPost(
                        username = "ZenMaster99",
                        userAvatarUrl = "avatar1",
                        habitName = "Daily Meditation & Mindfulness",
                        achievementText = "attained an impressive 14-day streak! 🧘🏽‍♀️",
                        cheerCount = 12,
                        timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
                    ),
                    SocialPost(
                        username = "CodeRunner",
                        userAvatarUrl = "avatar2",
                        habitName = "Leetcode Challenge",
                        achievementText = "completed 5 check-ins this week! 💻",
                        cheerCount = 4,
                        timestamp = System.currentTimeMillis() - 3600000 * 5 // 5 hours ago
                    ),
                    SocialPost(
                        username = "ActiveAlice",
                        userAvatarUrl = "avatar3",
                        habitName = "Morning Hydration (1.5L)",
                        achievementText = "unlocked the 'First Step' badge! 💧",
                        cheerCount = 8,
                        timestamp = java.lang.System.currentTimeMillis() - 3600000 * 12 // 12 hours ago
                    )
                )
                for (post in initialSocialPosts) {
                    habitDao.insertSocialPost(post)
                }

                // Seed a starter habit for new users
                habitDao.insertHabit(
                    Habit(
                        name = "Stay Hydrated",
                        description = "Drink at least 8 glasses of water today",
                        category = "Health",
                        frequency = "Daily",
                        xpReward = 15
                    )
                )
                habitDao.insertHabit(
                    Habit(
                        name = "Read 10 Pages",
                        description = "Read a helpful, non-fiction book to learn something new",
                        category = "Focus",
                        frequency = "Daily",
                        xpReward = 20
                    )
                )
            }
        }
    }
}
