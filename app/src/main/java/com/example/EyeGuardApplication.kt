package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.EyeGuardRepository

class EyeGuardApplication : Application() {

    companion object {
        const val CHANNEL_ID_TIMER = "eyeguard_timer"
        const val CHANNEL_ID_BREAK = "eyeguard_break"
        const val CHANNEL_ID_WARNING = "eyeguard_warning"
        lateinit var instance: EyeGuardApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        EyeGuardRepository.initialize(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Timer channel (Ongoing, silent, low importance)
            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMER,
                getString(R.string.notif_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_timer_desc)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            // Break channel (High importance alert with sound and heads-up)
            val breakChannel = NotificationChannel(
                CHANNEL_ID_BREAK,
                getString(R.string.notif_channel_break_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_break_desc)
                setShowBadge(true)
                enableVibration(true)
            }

            // 1-minute warning channel (High importance heads-up reminder)
            val warningChannel = NotificationChannel(
                CHANNEL_ID_WARNING,
                getString(R.string.notif_channel_warning_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_warning_desc)
                setShowBadge(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(breakChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }
}
