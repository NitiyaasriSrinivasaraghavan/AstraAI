package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.content.SharedPreferences
import com.example.aidrivencompetencyplatform.model.User
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
import com.example.aidrivencompetencyplatform.model.AnalysisHistoryRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        private const val KEY_ANALYSIS_HISTORY = "analysis_history_"
    }

    fun saveLatestAnalysis(result: ResumeAnalysisResult, fileName: String? = null) {
        val email = getCurrentEmail() ?: return
        val normalizedEmail = normalizeEmail(email)
        val json = gson.toJson(result)
        
        // Also create a history record
        val role = getTargetRole(normalizedEmail) ?: "Android Developer"
        val topSkills = (result.extractedSkills ?: emptyList()).map { it.name }.take(4)
        val historyRecord = AnalysisHistoryRecord(
            targetRole = role,
            atsScore = result.atsScore,
            skillMatch = result.skillMatch,
            candidateName = result.candidateName ?: getUserName(normalizedEmail),
            fileName = fileName ?: "Resume.pdf",
            topSkills = topSkills
        )
        addAnalysisHistoryRecord(historyRecord, normalizedEmail)

        prefs.edit()
            .putString(KEY_LATEST_ANALYSIS + normalizedEmail, json)
            .apply()
    }

    fun getAnalysisHistory(specificEmail: String? = null): List<AnalysisHistoryRecord> {
        val email = specificEmail ?: getCurrentEmail() ?: return emptyList()
        val normalizedEmail = normalizeEmail(email)
        val json = prefs.getString(KEY_ANALYSIS_HISTORY + normalizedEmail, null)
        
        if (json != null) {
            try {
                val type = object : TypeToken<List<AnalysisHistoryRecord>>() {}.type
                val list: List<AnalysisHistoryRecord> = gson.fromJson(json, type) ?: emptyList()
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // fall through
            }
        }

        // Synthesize a record from latest analysis if available for this specific user
        val latest = getLatestAnalysis(normalizedEmail)
        if (latest != null) {
            val role = getTargetRole(normalizedEmail) ?: "Android Developer"
            val topSkills = (latest.extractedSkills ?: emptyList()).map { it.name }.take(4)
            val initialRecord = AnalysisHistoryRecord(
                targetRole = role,
                atsScore = latest.atsScore,
                skillMatch = latest.skillMatch,
                candidateName = latest.candidateName ?: getUserName(normalizedEmail),
                fileName = "Resume.pdf",
                topSkills = topSkills
            )
            return listOf(initialRecord)
        }

        return emptyList()
    }

    fun addAnalysisHistoryRecord(record: AnalysisHistoryRecord, specificEmail: String? = null) {
        val email = specificEmail ?: getCurrentEmail() ?: return
        val normalizedEmail = normalizeEmail(email)
        val currentHistory = getAnalysisHistory(normalizedEmail).toMutableList()
        // Prepend to show latest first
        currentHistory.add(0, record)
        val trimmedHistory = currentHistory.distinctBy { it.id }.take(15)
        val json = gson.toJson(trimmedHistory)
        prefs.edit()
            .putString(KEY_ANALYSIS_HISTORY + normalizedEmail, json)
            .apply()
    }

    fun getLatestAnalysis(specificEmail: String? = null): ResumeAnalysisResult? {
        val email = specificEmail ?: getCurrentEmail() ?: return null
        val normalizedEmail = normalizeEmail(email)
        val json = prefs.getString(KEY_LATEST_ANALYSIS + normalizedEmail, null) ?: return null
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
    fun login(email: String, isNewUser: Boolean? = null) {
        val normalizedEmail = normalizeEmail(email)
        val hasLoggedInBeforeKey = "has_logged_in_before_" + normalizedEmail
        val hasLoggedInBefore = prefs.getBoolean(hasLoggedInBeforeKey, false)
        val determinedIsNew = isNewUser ?: (!hasLoggedInBefore)

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_EMAIL, normalizedEmail)
            .putBoolean(KEY_IS_NEW_USER, determinedIsNew)
            .putBoolean(hasLoggedInBeforeKey, true)
            .commit()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getCurrentEmail().isNullOrBlank()
    }

    fun getCurrentEmail(): String? {
        return prefs.getString(KEY_CURRENT_EMAIL, null)
    }

    /**
     * Gets the name of the currently logged-in user or specified email.
     */
    fun getUserName(specificEmail: String? = null): String? {
        val email = specificEmail ?: getCurrentEmail() ?: return null
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

    fun getTargetRole(specificEmail: String? = null): String? {
        val email = specificEmail ?: getCurrentEmail() ?: return null
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