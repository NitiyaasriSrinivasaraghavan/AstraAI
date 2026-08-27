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
    }

    fun saveLatestAnalysis(result: ResumeAnalysisResult) {
        val email = getCurrentEmail() ?: "default_user"
        val json = gson.toJson(result)
        prefs.edit()
            .putString(KEY_LATEST_ANALYSIS + email, json)
            .putString(KEY_LATEST_ANALYSIS + "global", json)
            .commit()
    }

    fun getLatestAnalysis(): ResumeAnalysisResult? {
        val email = getCurrentEmail() ?: "default_user"
        val json = prefs.getString(KEY_LATEST_ANALYSIS + email, null)
            ?: prefs.getString(KEY_LATEST_ANALYSIS + "global", null)
            ?: return null
        return try {
            gson.fromJson(json, ResumeAnalysisResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    /**
     * Registers a new user. Returns false if user already exists.
     */
    fun register(user: User): Boolean {
        val normalizedEmail = normalizeEmail(user.email)
        if (getUser(normalizedEmail) != null) return false
        saveUser(user)
        return true
    }

    /**
     * Internal method to save or overwrite user data synchronously.
     */
    private fun saveUser(user: User) {
        val normalizedEmail = normalizeEmail(user.email)
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString(KEY_USER_DATA_PREFIX + normalizedEmail, userJson)
            .commit()
    }

    /**
     * Retrieves user data by email.
     */
    fun getUser(email: String): User? {
        val normalizedEmail = normalizeEmail(email)
        val userJson = prefs.getString(KEY_USER_DATA_PREFIX + normalizedEmail, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * Validates credentials against stored data.
     */
    fun authenticate(email: String, password: String): Boolean {
        val user = getUser(email) ?: return false
        return user.password.trim() == password.trim()
    }

    /**
     * Establishes the authenticated session immediately.
     */
    fun login(email: String, isNewUser: Boolean = false) {
        val normalizedEmail = normalizeEmail(email)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_EMAIL, normalizedEmail)
            .putBoolean(KEY_IS_NEW_USER, isNewUser)
            .commit()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getCurrentEmail().isNullOrBlank()
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
            .commit()
    }
}