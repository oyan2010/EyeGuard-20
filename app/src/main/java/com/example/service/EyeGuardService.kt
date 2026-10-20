package com.example.service

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.EyeGuardApplication
import com.example.MainActivity
import com.example.R
import com.example.data.EyeGuardRepository
import com.example.model.Phase
import com.example.model.ServiceState
import com.example.ui.breakmode.BreakActivity
import com.example.util.PermissionHelper
import com.example.util.SoundVibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EyeGuardService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_PAUSE_10 = "com.example.action.PAUSE_10"
        const val ACTION_TRIGGER_BREAK_NOW = "com.example.action.TRIGGER_BREAK_NOW"
        const val ACTION_SKIP_BREAK = "com.example.action.SKIP_BREAK"
        const val ACTION_START_DEV_USAGE = "com.example.action.START_DEV_USAGE"
        const val ACTION_START_DEV_BREAK = "com.example.action.START_DEV_BREAK"
        const val ACTION_ALARM_WAKEUP = "com.example.action.ALARM_WAKEUP"

        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ID_WARNING = 1005
        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

        fun startService(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun pause10Minutes(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_PAUSE_10
            }
            context.startService(intent)
        }

        fun triggerBreakNow(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_TRIGGER_BREAK_NOW
            }
            context.startService(intent)
        }

        fun skipBreak(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_SKIP_BREAK
            }
            context.startService(intent)
        }

        fun startDevUsage(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_START_DEV_USAGE
            }
            context.startService(intent)
        }

        fun startDevBreak(context: Context) {
            val intent = Intent(context, EyeGuardService::class.java).apply {
                action = ACTION_START_DEV_BREAK
            }
            context.startService(intent)
        }
    }

    private val repository by lazy { EyeGuardRepository.getInstance() }
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    // State Variables
    private var currentPhase: Phase = Phase.IDLE
    private var targetElapsedRealtime: Long = 0L
    private var totalDurationMs: Long = 0L
    private var isScreenActive: Boolean = true
    private var screenOffTimestamp: Long = 0L
    private var pausedRemainingMs: Long = 0L
    private var isDevTestMode: Boolean = false
    private var hasSentOneMinuteWarning: Boolean = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    onScreenTurnedOff()
                }
                Intent.ACTION_SCREEN_ON -> {
                    onScreenTurnedOn()
                }
                Intent.ACTION_USER_PRESENT -> {
                    onUserUnlocked()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        // Always start foreground first to satisfy system requirements
        startForeground(NOTIFICATION_ID, buildTimerNotification())

        when (action) {
            ACTION_START -> {
                if (currentPhase == Phase.IDLE || currentPhase == Phase.PAUSED) {
                    startUsageCycle()
                }
            }
            ACTION_STOP -> {
                stopSelfAndReset()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_10 -> {
                pauseFor10Minutes()
            }
            ACTION_TRIGGER_BREAK_NOW -> {
                startBreakCycle(isDev15s = false)
            }
            ACTION_SKIP_BREAK -> {
                if (currentPhase == Phase.BREAK) {
                    startUsageCycle()
                }
            }
            ACTION_START_DEV_USAGE -> {
                startDevUsageCycle()
            }
            ACTION_START_DEV_BREAK -> {
                startBreakCycle(isDev15s = true)
            }
            ACTION_ALARM_WAKEUP -> {
                evaluateTimer()
            }
        }

        return START_STICKY
    }

    private fun isPhoneUnlocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenInteractive = pm?.isInteractive ?: true
        val isLocked = km?.isKeyguardLocked ?: false
        return isScreenInteractive && !isLocked
    }

    private fun onScreenTurnedOff() {
        if (currentPhase == Phase.USING) {
            val now = SystemClock.elapsedRealtime()
            screenOffTimestamp = now
            pausedRemainingMs = (targetElapsedRealtime - now).coerceAtLeast(0L)
            isScreenActive = false
            publishState()
        }
    }

    private fun onScreenTurnedOn() {
        if (currentPhase == Phase.BREAK) {
            // User opened screen during an active break: enforce break again!
            reEnforceBreak()
            return
        }

        if (currentPhase == Phase.USING) {
            val now = SystemClock.elapsedRealtime()
            if (screenOffTimestamp > 0L && (now - screenOffTimestamp) >= FIVE_MINUTES_MS) {
                // Screen was off 5+ minutes: reset cycle!
                startUsageCycle()
            }
        }
    }

    private fun onUserUnlocked() {
        if (currentPhase == Phase.BREAK) {
            // User unlocked phone during break: relaunch BreakActivity and lock
            reEnforceBreak()
            return
        }

        if (currentPhase == Phase.USING && !isScreenActive) {
            isScreenActive = true
            // Resume timer from where it paused
            val now = SystemClock.elapsedRealtime()
            if (pausedRemainingMs > 0L) {
                targetElapsedRealtime = now + pausedRemainingMs
                scheduleBackupAlarm(targetElapsedRealtime)
            }
            publishState()
            updateNotification()
        }
    }

    private fun startUsageCycle() {
        scope.launch {
            val settings = repository.getSettingsSnapshot()
            isDevTestMode = false
            hasSentOneMinuteWarning = false
            currentPhase = Phase.USING
            isScreenActive = isPhoneUnlocked()
            totalDurationMs = settings.usageMinutes * 60 * 1000L
            targetElapsedRealtime = SystemClock.elapsedRealtime() + totalDurationMs
            pausedRemainingMs = totalDurationMs

            scheduleBackupAlarm(targetElapsedRealtime)
            publishState()
            updateNotification()
            startTicker()
        }
    }

    private fun startDevUsageCycle() {
        isDevTestMode = true
        hasSentOneMinuteWarning = false
        currentPhase = Phase.USING
        isScreenActive = true
        totalDurationMs = 10 * 1000L // 10 seconds
        targetElapsedRealtime = SystemClock.elapsedRealtime() + totalDurationMs
        pausedRemainingMs = totalDurationMs

        scheduleBackupAlarm(targetElapsedRealtime)
        publishState()
        updateNotification()
        startTicker()
    }

    private fun startBreakCycle(isDev15s: Boolean) {
        scope.launch {
            // Dismiss 1-minute warning alert if displayed
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID_WARNING)

            // Check if phone call is active; do not interrupt!
            if (PermissionHelper.isCallActive(this@EyeGuardService)) {
                // Postpone break by 1 minute
                targetElapsedRealtime = SystemClock.elapsedRealtime() + 60 * 1000L
                scheduleBackupAlarm(targetElapsedRealtime)
                return@launch
            }

            val settings = repository.getSettingsSnapshot()
            isDevTestMode = isDev15s
            currentPhase = Phase.BREAK
            isScreenActive = true
            totalDurationMs = if (isDev15s) 15 * 1000L else settings.breakMinutes * 60 * 1000L
            targetElapsedRealtime = SystemClock.elapsedRealtime() + totalDurationMs

            scheduleBackupAlarm(targetElapsedRealtime)
            publishState()
            updateNotification()

            // Play alert sound & vibration
            SoundVibrationHelper.playBreakStartAlert(
                context = this@EyeGuardService,
                soundEnabled = settings.soundOn,
                vibrateEnabled = settings.vibrateOn
            )

            // Trigger LockNow via DevicePolicyManager
            triggerDeviceLock()

            // Launch BreakActivity
            launchBreakActivity()

            startTicker()
        }
    }

    private fun reEnforceBreak() {
        if (currentPhase != Phase.BREAK) return
        val remaining = targetElapsedRealtime - SystemClock.elapsedRealtime()
        if (remaining > 2000L) {
            triggerDeviceLock()
            launchBreakActivity()
        }
    }

    private fun triggerDeviceLock() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (dpm != null && PermissionHelper.isDeviceAdminActive(this)) {
                dpm.lockNow()
            }
        } catch (_: Exception) {
            // Safe fallback in emulator
        }
    }

    private fun launchBreakActivity() {
        try {
            val intent = Intent(this, BreakActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun onBreakFinished() {
        scope.launch {
            val settings = repository.getSettingsSnapshot()
            val breakMins = if (isDevTestMode) 1 else settings.breakMinutes
            repository.recordCompletedBreak(breakMins)

            SoundVibrationHelper.playBreakEndAlert(
                context = this@EyeGuardService,
                soundEnabled = settings.soundOn,
                vibrateEnabled = settings.vibrateOn
            )

            // Transition directly into new usage cycle
            startUsageCycle()
        }
    }

    private fun pauseFor10Minutes() {
        currentPhase = Phase.PAUSED
        totalDurationMs = 10 * 60 * 1000L
        targetElapsedRealtime = SystemClock.elapsedRealtime() + totalDurationMs
        scheduleBackupAlarm(targetElapsedRealtime)
        publishState()
        updateNotification()
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                evaluateTimer()
            }
        }
    }

    private fun evaluateTimer() {
        val now = SystemClock.elapsedRealtime()
        when (currentPhase) {
            Phase.USING -> {
                if (isScreenActive) {
                    val remaining = (targetElapsedRealtime - now).coerceAtLeast(0L)
                    val warningThresholdMs = if (isDevTestMode) 5_000L else 60_000L

                    if (remaining in 1..warningThresholdMs && !hasSentOneMinuteWarning) {
                        hasSentOneMinuteWarning = true
                        checkAndSendOneMinuteWarning()
                    }

                    if (now >= targetElapsedRealtime) {
                        startBreakCycle(isDev15s = isDevTestMode)
                    } else {
                        publishState()
                    }
                }
            }
            Phase.BREAK -> {
                if (now >= targetElapsedRealtime) {
                    onBreakFinished()
                } else {
                    publishState()
                }
            }
            Phase.PAUSED -> {
                if (now >= targetElapsedRealtime) {
                    startUsageCycle()
                } else {
                    publishState()
                }
            }
            Phase.IDLE -> {
                // Do nothing
            }
        }
    }

    private fun checkAndSendOneMinuteWarning() {
        scope.launch {
            val settings = repository.getSettingsSnapshot()
            if (!settings.oneMinuteWarningEnabled) return@launch

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return@launch
            val openIntent = PendingIntent.getActivity(
                this@EyeGuardService,
                4,
                Intent(this@EyeGuardService, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val warningNotif = NotificationCompat.Builder(this@EyeGuardService, EyeGuardApplication.CHANNEL_ID_WARNING)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(getString(R.string.notif_title_one_min_warning))
                .setContentText(getString(R.string.notif_text_one_min_warning))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()

            nm.notify(NOTIFICATION_ID_WARNING, warningNotif)

            SoundVibrationHelper.playOneMinuteWarningAlert(
                context = this@EyeGuardService,
                soundEnabled = settings.soundOn,
                vibrateEnabled = settings.vibrateOn
            )
        }
    }

    private fun publishState() {
        val now = SystemClock.elapsedRealtime()
        val remaining = if (currentPhase == Phase.USING && !isScreenActive) {
            pausedRemainingMs
        } else {
            (targetElapsedRealtime - now).coerceAtLeast(0L)
        }

        repository.updateServiceState(
            ServiceState(
                phase = currentPhase,
                targetElapsedRealtime = targetElapsedRealtime,
                totalDurationMs = totalDurationMs,
                remainingMs = remaining,
                isScreenOn = isScreenActive,
                isPaused = currentPhase == Phase.PAUSED,
                isDevTestCycle = isDevTestMode
            )
        )
    }

    private fun scheduleBackupAlarm(triggerElapsedRealtime: Long) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(this, EyeGuardService::class.java).apply {
                action = ACTION_ALARM_WAKEUP
            }
            val pendingIntent = PendingIntent.getService(
                this,
                1002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerElapsedRealtime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerElapsedRealtime,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildTimerNotification())
    }

    private fun buildTimerNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, EyeGuardService::class.java).apply { action = ACTION_PAUSE_10 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, EyeGuardService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMs = (targetElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val channelId = if (currentPhase == Phase.BREAK) {
            EyeGuardApplication.CHANNEL_ID_BREAK
        } else {
            EyeGuardApplication.CHANNEL_ID_TIMER
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        when (currentPhase) {
            Phase.BREAK -> {
                builder.setContentTitle(getString(R.string.notif_title_break))
                    .setContentText(getString(R.string.notif_text_break))
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(System.currentTimeMillis() + remainingMs)
                    .addAction(android.R.drawable.ic_media_pause, getString(R.string.notif_action_pause_10), pauseIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_action_turn_off), stopIntent)

                // Add full screen intent on break start
                val breakIntent = Intent(this, BreakActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val fullScreenPending = PendingIntent.getActivity(
                    this,
                    3,
                    breakIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setFullScreenIntent(fullScreenPending, true)
            }
            Phase.PAUSED -> {
                builder.setContentTitle(getString(R.string.status_paused))
                    .setContentText(getString(R.string.notif_text_usage))
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_action_turn_off), stopIntent)
            }
            else -> {
                // USING or IDLE
                builder.setContentTitle(getString(R.string.notif_title_usage))
                    .setContentText(getString(R.string.notif_text_usage))
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(System.currentTimeMillis() + remainingMs)
                    .addAction(android.R.drawable.ic_media_pause, getString(R.string.notif_action_pause_10), pauseIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_action_turn_off), stopIntent)
            }
        }

        return builder.build()
    }

    private fun stopSelfAndReset() {
        tickerJob?.cancel()
        currentPhase = Phase.IDLE
        repository.resetServiceStateToIdle()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
