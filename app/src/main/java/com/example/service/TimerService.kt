package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.GoalTrackerApplication
import com.example.MainActivity
import com.example.R
import com.example.data.GoalProgressCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null
    private var currentGoalId: Long = 0L
    private var currentGoalName: String = ""
    private var currentGoalColor: String = "#8B5CF6"
    private var elapsedSeconds: Long = 0L
    private var isPaused: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val goalId = intent.getLongExtra(EXTRA_GOAL_ID, 0L)
                val goalName = intent.getStringExtra(EXTRA_GOAL_NAME) ?: "Goal"
                val goalColor = intent.getStringExtra(EXTRA_GOAL_COLOR) ?: "#8B5CF6"
                startTimer(goalId, goalName, goalColor)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }

        return START_STICKY
    }

    private fun startTimer(goalId: Long, goalName: String, goalColor: String) {
        currentGoalId = goalId
        currentGoalName = goalName
        currentGoalColor = goalColor
        elapsedSeconds = 0L
        isPaused = false

        TimerStateManager.updateState {
            it.copy(
                goalId = goalId,
                goalName = goalName,
                goalColorHex = goalColor,
                isRunning = true,
                isPaused = false,
                elapsedSeconds = 0L
            )
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                if (!isPaused) {
                    elapsedSeconds++
                    TimerStateManager.updateState {
                        it.copy(elapsedSeconds = elapsedSeconds)
                    }
                    updateNotification()
                }
            }
        }
    }

    private fun pauseTimer() {
        isPaused = true
        TimerStateManager.updateState { it.copy(isPaused = true) }
        updateNotification()
    }

    private fun resumeTimer() {
        isPaused = false
        TimerStateManager.updateState { it.copy(isPaused = false) }
        updateNotification()
    }

    private fun stopTimer() {
        tickerJob?.cancel()
        val duration = elapsedSeconds
        val goalId = currentGoalId

        serviceScope.launch(Dispatchers.IO) {
            if (goalId > 0 && duration > 0) {
                GoalTrackerApplication.instance.repository.logTime(
                    goalId = goalId,
                    durationSeconds = duration,
                    note = "Timer session",
                    isManual = false
                )
            }
        }

        TimerStateManager.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val clock = GoalProgressCalculator.formatTimerClock(elapsedSeconds)
        val statusText = if (isPaused) "Paused • $clock" else "Stopwatch is running... • $clock"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume action
        val toggleActionIntent = Intent(this, TimerService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleLabel = if (isPaused) "Resume" else "Pause"

        // Stop action
        val stopActionIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GoalTrackerApplication.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentGoalName)
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, toggleLabel, togglePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ACTION_RESUME"
        const val ACTION_STOP = "com.example.ACTION_STOP"

        const val EXTRA_GOAL_ID = "extra_goal_id"
        const val EXTRA_GOAL_NAME = "extra_goal_name"
        const val EXTRA_GOAL_COLOR = "extra_goal_color"

        fun start(context: Context, goalId: Long, goalName: String, goalColor: String) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_GOAL_ID, goalId)
                putExtra(EXTRA_GOAL_NAME, goalName)
                putExtra(EXTRA_GOAL_COLOR, goalColor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
