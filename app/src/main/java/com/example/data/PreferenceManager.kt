package com.example.data

import android.content.Context
import android.content.SharedPreferences

enum class StartScreen(val key: String, val title: String) {
    ALL_GOALS("all_goals", "All Goals"),
    FAVORITE_GOALS("favorite_goals", "Favorite Goals")
}

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("goal_tracker_prefs", Context.MODE_PRIVATE)

    fun getStartScreen(): StartScreen {
        val raw = prefs.getString(KEY_START_SCREEN, StartScreen.ALL_GOALS.key)
        return if (raw == StartScreen.FAVORITE_GOALS.key) {
            StartScreen.FAVORITE_GOALS
        } else {
            StartScreen.ALL_GOALS
        }
    }

    fun setStartScreen(screen: StartScreen) {
        prefs.edit().putString(KEY_START_SCREEN, screen.key).apply()
    }

    companion object {
        private const val KEY_START_SCREEN = "start_screen"
    }
}
