package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.RecurrenceType

enum class StartScreen(val key: String, val title: String) {
    ALL_GOALS("all_goals", "All Goals"),
    FAVORITE_GOALS("favorite_goals", "Favorite Goals")
}

enum class GoalSortOrder(val title: String) {
    RECENTLY_ACTIVE("Recently Active"),
    ALPHABETICAL_ASC("Name (A–Z)"),
    ALPHABETICAL_DESC("Name (Z–A)"),
    PROGRESS_DESC("Progress (Highest)"),
    PROGRESS_ASC("Progress (Lowest)"),
    TARGET_TIME_DESC("Target (Longest)"),
    TARGET_TIME_ASC("Target (Shortest)"),
    NEWEST("Created (Newest)"),
    OLDEST("Created (Oldest)")
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

    fun getSortOrder(): GoalSortOrder {
        val raw = prefs.getString(KEY_SORT_ORDER, GoalSortOrder.RECENTLY_ACTIVE.name)
        return try {
            GoalSortOrder.valueOf(raw ?: GoalSortOrder.RECENTLY_ACTIVE.name)
        } catch (e: Exception) {
            GoalSortOrder.RECENTLY_ACTIVE
        }
    }

    fun setSortOrder(order: GoalSortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
    }

    fun isToolsHeaderExpanded(): Boolean {
        return prefs.getBoolean(KEY_TOOLS_HEADER_EXPANDED, true)
    }

    fun setToolsHeaderExpanded(expanded: Boolean) {
        prefs.edit().putBoolean(KEY_TOOLS_HEADER_EXPANDED, expanded).apply()
    }

    // Default settings for new goals
    fun getLastTargetSeconds(): Long {
        return prefs.getLong(KEY_LAST_TARGET_SECONDS, 3600L)
    }

    fun setLastTargetSeconds(seconds: Long) {
        prefs.edit().putLong(KEY_LAST_TARGET_SECONDS, seconds).apply()
    }

    fun getLastRecurrenceType(): RecurrenceType {
        val raw = prefs.getString(KEY_LAST_RECURRENCE_TYPE, RecurrenceType.DAILY.name)
        return try {
            RecurrenceType.valueOf(raw ?: RecurrenceType.DAILY.name)
        } catch (e: Exception) {
            RecurrenceType.DAILY
        }
    }

    fun setLastRecurrenceType(type: RecurrenceType) {
        prefs.edit().putString(KEY_LAST_RECURRENCE_TYPE, type.name).apply()
    }

    fun getLastCustomDaysInterval(): Int {
        return prefs.getInt(KEY_LAST_CUSTOM_DAYS, 2)
    }

    fun setLastCustomDaysInterval(days: Int) {
        prefs.edit().putInt(KEY_LAST_CUSTOM_DAYS, days).apply()
    }

    fun getLastColorHex(): String {
        return prefs.getString(KEY_LAST_COLOR_HEX, "#8B5CF6") ?: "#8B5CF6"
    }

    fun setLastColorHex(hex: String) {
        prefs.edit().putString(KEY_LAST_COLOR_HEX, hex).apply()
    }

    fun getLastCategory(): String {
        return prefs.getString(KEY_LAST_CATEGORY, "") ?: ""
    }

    fun setLastCategory(category: String) {
        prefs.edit().putString(KEY_LAST_CATEGORY, category).apply()
    }

    fun saveGoalCreationDefaults(
        targetSeconds: Long,
        recurrenceType: RecurrenceType,
        customDaysInterval: Int,
        colorHex: String,
        category: String
    ) {
        prefs.edit()
            .putLong(KEY_LAST_TARGET_SECONDS, targetSeconds)
            .putString(KEY_LAST_RECURRENCE_TYPE, recurrenceType.name)
            .putInt(KEY_LAST_CUSTOM_DAYS, customDaysInterval)
            .putString(KEY_LAST_COLOR_HEX, colorHex)
            .putString(KEY_LAST_CATEGORY, category)
            .apply()
    }

    companion object {
        private const val KEY_START_SCREEN = "start_screen"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_TOOLS_HEADER_EXPANDED = "tools_header_expanded"
        private const val KEY_LAST_TARGET_SECONDS = "last_target_seconds"
        private const val KEY_LAST_RECURRENCE_TYPE = "last_recurrence_type"
        private const val KEY_LAST_CUSTOM_DAYS = "last_custom_days"
        private const val KEY_LAST_COLOR_HEX = "last_color_hex"
        private const val KEY_LAST_CATEGORY = "last_category"
    }
}
