package com.vislupus.dream_records

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.data.Repository
import com.vislupus.dream_records.data.db.AppDatabase
import com.vislupus.dream_records.data.settings.SettingsStore
import com.vislupus.dream_records.transcription.TranscriptionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Tiny manual dependency container — no DI framework needed for an app this size. */
class DreamRecordsApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: Repository by lazy { Repository(this, database) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val transcription: TranscriptionService by lazy { TranscriptionService(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        appScope.launch {
            val lang = settingsStore.settings.first().languageCode
            repository.ensureDefaultTags(lang)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            AppConfig.Recording.NOTIFICATION_CHANNEL_ID,
            AppConfig.App.NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setSound(null, null); enableVibration(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        fun from(context: android.content.Context): DreamRecordsApp = context.applicationContext as DreamRecordsApp
    }
}
