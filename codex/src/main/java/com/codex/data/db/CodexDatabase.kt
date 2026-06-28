package com.codex.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codex.data.db.dao.*
import com.codex.data.db.entity.*

@Database(
    entities = [
        ProgressEntity::class,
        MasteryEntity::class,
        SrsCardEntity::class,
        AchievementEntity::class,
        PlayerStatsEntity::class,
        SessionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CodexDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun masteryDao(): MasteryDao
    abstract fun srsDao(): SrsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun sessionLogDao(): SessionLogDao

    companion object {
        @Volatile private var INSTANCE: CodexDatabase? = null

        fun get(context: Context): CodexDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CodexDatabase::class.java,
                    "codex.db"
                ).build().also { INSTANCE = it }
            }
    }
}
