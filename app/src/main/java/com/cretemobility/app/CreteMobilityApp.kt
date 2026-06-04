package com.cretemobility.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cretemobility.app.core.notification.NotificationChannels
import com.cretemobility.app.data.sync.ScheduleSyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CreteMobilityApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        initLogging()
        createNotificationChannels()
        ScheduleSyncManager.schedulePeriodicSync(this)
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        NotificationChannels.DEPARTURE_ALERTS,
                        "Departure Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Real-time departure and delay notifications"
                        enableVibration(true)
                    },
                    NotificationChannel(
                        NotificationChannels.LAST_BUS_WARNING,
                        "Last Bus Warning",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Warning when last bus is approaching"
                        enableVibration(true)
                    },
                    NotificationChannel(
                        NotificationChannels.SYNC_STATUS,
                        "Schedule Sync",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Background schedule sync status"
                    }
                )
            )
        }
    }
}
