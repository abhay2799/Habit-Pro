package com.example.data.local

import androidx.room.*
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.Badge
import com.example.data.model.SocialPost
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // --- Habits ---
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')")
    fun searchHabits(query: String): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveHabit(id: Long)

    // --- Completions ---
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateString = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateString = :date")
    suspend fun removeCompletion(habitId: Long, date: String)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: Long)

    // --- Badges ---
    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, requirementValue ASC")
    fun getAllBadges(): Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)

    @Update
    suspend fun updateBadge(badge: Badge)

    @Query("UPDATE badges SET isUnlocked = 1, unlockedAt = :timestamp, progress = requirementValue WHERE id = :id")
    suspend fun unlockBadge(id: String, timestamp: Long)

    @Query("UPDATE badges SET progress = :progress WHERE id = :id")
    suspend fun updateBadgeProgress(id: String, progress: Int)

    // --- Social Accountability Feed ---
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getSocialFeed(): Flow<List<SocialPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialPost(post: SocialPost)

    @Update
    suspend fun updateSocialPost(post: SocialPost)

    @Query("UPDATE social_posts SET isCheered = :isCheered, cheerCount = cheerCount + :diff WHERE id = :id")
    suspend fun updateCheerStatus(id: String, isCheered: Boolean, diff: Int)
}
