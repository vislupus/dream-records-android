package com.vislupus.dream_records.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vislupus.dream_records.config.AppConfig

/**
 * SQLite database (via Room). The file lives inside the app sandbox:
 *   /data/data/com.vislupus.dream_records/databases/dream_records.db
 */
@Database(
    entities = [Recording::class, Tag::class, RecordingTag::class, Night::class],
    version = AppConfig.Database.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun tagDao(): TagDao
    abstract fun nightDao(): NightDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, AppConfig.Database.NAME)
                .fallbackToDestructiveMigration(false)
                .build()
                .also { instance = it }
        }
    }
}
