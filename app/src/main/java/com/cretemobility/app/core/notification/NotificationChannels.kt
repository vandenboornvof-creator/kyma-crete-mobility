package com.cretemobility.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.cretemobility.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object NotificationChannels {
    const val DEPARTURE_ALERTS = "departure_alerts"
    const val LAST_BUS_WARNING = "last_bus_warning"
    const val SYNC_STATUS = "sync_status"
}

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val departureChannel = NotificationChannel(
                NotificationChannels.DEPARTURE_ALERTS,
                context.getString(R.string.notif_channel_departures),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your bus or ferry is delayed"
                enableVibration(true)
            }

            val lastBusChannel = NotificationChannel(
                NotificationChannels.LAST_BUS_WARNING,
                context.getString(R.string.notif_channel_last_bus),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warning before the last bus departs"
                enableVibration(true)
            }

            val syncChannel = NotificationChannel(
                NotificationChannels.SYNC_STATUS,
                context.getString(R.string.notif_channel_sync),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background timetable sync status"
                setShowBadge(false)
            }

            manager.createNotificationChannels(listOf(departureChannel, lastBusChannel, syncChannel))
        }
    }
}
