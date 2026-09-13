package com.example.visionwidget.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VisionDao {
    /** Every vision with its milestones, oldest first — the switcher's own order. */
    @Transaction
    @Query("SELECT * FROM visions ORDER BY id")
    fun observeVisionsWithMilestones(): Flow<List<VisionWithMilestones>>

    /** Null once no vision is main — the fallback to the oldest one lives in the caller. */
    @Query("SELECT id FROM visions WHERE isMain = 1 LIMIT 1")
    fun observeMainVisionId(): Flow<Long?>

    @Insert
    suspend fun insertVision(vision: VisionEntity): Long

    @Query("UPDATE visions SET goal = :goal, why = :why, targetDateMillis = :targetDateMillis WHERE id = :id")
    suspend fun updateVision(id: Long, goal: String, why: String, targetDateMillis: Long)

    @Query("DELETE FROM visions WHERE id = :id")
    suspend fun deleteVision(id: Long)

    @Query("SELECT COUNT(*) FROM visions")
    suspend fun countVisions(): Int

    @Query("SELECT COUNT(*) FROM visions WHERE isMain = 1")
    suspend fun countMain(): Int

    @Query("SELECT id FROM visions ORDER BY id LIMIT 1")
    suspend fun firstVisionId(): Long?

    @Query("UPDATE visions SET isMain = 0")
    suspend fun clearMain()

    @Query("UPDATE visions SET isMain = 1 WHERE id = :id")
    suspend fun setMain(id: Long)

    /** Only one vision can be main — clearing the rest first keeps that true. */
    @Transaction
    suspend fun setMainVision(id: Long) {
        clearMain()
        setMain(id)
    }

    @Insert
    suspend fun insertMilestone(milestone: MilestoneEntity): Long

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteMilestone(id: Long)

    /** SQLite reads a 0/1 column as boolean-false/true, so NOT flips it in place. */
    @Query("UPDATE milestones SET checked = NOT checked WHERE id = :id")
    suspend fun toggleMilestone(id: Long)
}
