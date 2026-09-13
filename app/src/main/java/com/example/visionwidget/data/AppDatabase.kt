package com.example.visionwidget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VisionEntity::class,
        MilestoneEntity::class,
        RuleOfThreeSlotEntity::class,
        RuleOfThreeStateEntity::class,
        RuleOfThreeHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visionDao(): VisionDao
    abstract fun ruleOfThreeDao(): RuleOfThreeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vision.db"
                ).build().also { instance = it }
            }
    }
}
