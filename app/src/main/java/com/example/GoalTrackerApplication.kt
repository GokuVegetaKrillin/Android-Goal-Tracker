package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.GoalRepository

class GoalTrackerApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        GoalRepository(
            database.goalDao(),
            database.timeLogDao(),
            database.goalNoteDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Goal Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing timer progress and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "goal_tracker_timer_channel"
        lateinit var instance: GoalTrackerApplication
            private set
    }
}
