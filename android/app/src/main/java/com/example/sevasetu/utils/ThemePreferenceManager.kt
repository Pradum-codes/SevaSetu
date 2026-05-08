package com.example.sevasetu.utils

import android.content.Context
import androidx.core.content.edit

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

class ThemePreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)

    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
    }

    fun getTheme(): AppTheme {
        val name = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(name ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    companion object {
        private const val THEME_PREFS = "theme_prefs"
        private const val KEY_THEME = "app_theme"
    }
}
