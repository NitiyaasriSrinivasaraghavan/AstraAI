package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.content.SharedPreferences
import com.example.aidrivencompetencyplatform.model.User
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            "astra_mind_prefs",
            Context.MODE_PRIVATE
        )
    private val gson = Gson()

    companion object {
        private const val KEY_USER_DATA_PREFIX = "user_data_"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_IS_NEW_USER = "is_new_user"
        private const val KEY_LATEST_ANALYSIS = "latest_analysis_"
        private const val KEY_RESET_DONE = "auth_reset_done_v3"
    }

    fun saveLatestAnalysis(result: ResumeAnalysisResult) {
        val email = getCurrentEmail() ?: return
        val json = gson.toJson(result)
        prefs.edit().putString(KEY_LATEST_ANALYSIS + email, json).apply()
    }

    fun getLatestAnalysis(): ResumeAnalysisResult? {
        val email = getCurrentEmail() ?: return null
        val json = prefs.getString(KEY_LATEST_ANALYSIS + email, null) ?: return null
        return gson.fromJson(json, ResumeAnalysisResult::class.java)
    }

    /**
     * One-time reset for development phase to clear corrupted data.
     */
    fun clearAllDataIfNeeded() {
        if (!prefs.getBoolean(KEY_RESET_DONE, false)) {
            prefs.edit().clear().putBoolean(KEY_RESET_DONE, true).apply()
        }
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    /**
     * Registers a new user if the email doesn't already exist.
     */
    fun register(user: User): Boolean {
        val normalizedEmail = normalizeEmail(user.email)
        if (getUser(normalizedEmail) != null) return false
        saveUser(user)
        return true
    }

    /**
     * Internal method to save or overwrite user data.
     */
    private fun saveUser(user: User) {
        val normalizedEmail = normalizeEmail(user.email)
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString(KEY_USER_DATA_PREFIX + normalizedEmail, userJson)
            .apply()
    }

    /**
     * Retrieves user data by email.
     */
    fun getUser(email: String): User? {
        val normalizedEmail = normalizeEmail(email)
        val userJson = prefs.getString(KEY_USER_DATA_PREFIX + normalizedEmail, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else null
    }

    /**
     * Validates credentials against stored data.
     */
    fun authenticate(email: String, password: String): Boolean {
        val user = getUser(email) ?: return false
        return user.password == password
    }

    /**
     * Establishes the authenticated session.
     */
    fun login(email: String, isNewUser: Boolean = false) {
        val normalizedEmail = normalizeEmail(email)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_EMAIL, normalizedEmail)
            .putBoolean(KEY_IS_NEW_USER, isNewUser)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentEmail(): String? {
        return prefs.getString(KEY_CURRENT_EMAIL, null)
    }

    /**
     * Gets the name of the currently logged-in user.
     */
    fun getUserName(): String? {
        val email = getCurrentEmail() ?: return null
        return getUser(email)?.name
    }

    fun isNewUser(): Boolean {
        return prefs.getBoolean(KEY_IS_NEW_USER, false)
    }

    /**
     * Updates profile name for current user.
     */
    fun updateUserName(name: String) {
        val email = getCurrentEmail() ?: return
        val user = getUser(email) ?: return
        saveUser(user.copy(name = name))
    }

    /**
     * Updates target role for current user.
     */
    fun updateTargetRole(role: String) {
        val email = getCurrentEmail() ?: return
        val user = getUser(email) ?: return
        saveUser(user.copy(targetRole = role))
    }

    fun getTargetRole(): String? {
        val email = getCurrentEmail() ?: return null
        return getUser(email)?.targetRole
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_CURRENT_EMAIL)
            .remove(KEY_IS_NEW_USER)
            .apply()
    }
}