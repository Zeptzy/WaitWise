package com.example.waitwise

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager
 * Handles saving, retrieving, and clearing the auth token
 * and user info from SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME    = "WaitWiseSession"
        private const val KEY_TOKEN     = "auth_token"
        private const val KEY_NAME      = "user_name"
        private const val KEY_EMAIL     = "user_email"
        private const val KEY_ROLE      = "user_role"
        private const val KEY_LOGGED_IN = "is_logged_in"
    }

    // ── Save session after login ───────────────────────────────────────────
    fun saveSession(token: String, name: String, email: String, role: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            putString(KEY_ROLE, role)
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────
    fun getToken(): String?  = prefs.getString(KEY_TOKEN, null)
    fun getUserName(): String = prefs.getString(KEY_NAME, "Student") ?: "Student"
    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_ROLE, "student") ?: "student"
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    // ── Clear session on logout ───────────────────────────────────────────
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}