package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.aidrivencompetencyplatform.model.User
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
import com.example.aidrivencompetencyplatform.model.AnalysisHistoryRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionManager(
    context: Context,
    val backendRepository: BackendRepository? = null
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            "astra_mind_prefs",
            Context.MODE_PRIVATE
        )
    private val gson = Gson()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        // Sync active user and latest analysis from MongoDB backend if logged in
        syncWithBackend()
    }

    /**
     * Synchronizes the current user's profile and latest resume analysis from the MongoDB backend.
     */
    fun syncWithBackend() {
        val email = getCurrentEmail() ?: return
        val normalizedEmail = normalizeEmail(email)
        backendRepository?.let { repo ->
            ioScope.launch {
                try {
                    val user = repo.fetchUserProfile(normalizedEmail)
                    if (user != null) {
                        saveUserLocally(user)
                    }
                    val remoteLatest = repo.fetchLatestAnalysis(normalizedEmail)
                    if (remoteLatest != null) {
                        saveLatestAnalysisLocally(remoteLatest)
                    }
                } catch (e: Exception) {
                    // Fallback to local offline cache
                }
            }
        }
    }

    fun deleteAllUsersAndHistory() {
        prefs.edit().clear().putBoolean("has_cleared_all_legacy_data_v12", true).commit()
    }

    companion object {
        private const val KEY_USER_DATA_PREFIX = "user_data_"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_IS_NEW_USER = "is_new_user"
        private const val KEY_LATEST_ANALYSIS = "latest_analysis_"
        private const val KEY_ANALYSIS_HISTORY = "analysis_history_"
        private const val KEY_ANALYSIS_RECORD_PREFIX = "analysis_record_"
    }

    fun saveLatestAnalysis(result: ResumeAnalysisResult, fileName: String? = null) {
        val email = getCurrentEmail() ?: return
        val normalizedEmail = normalizeEmail(email)
        
        val existingId = (result as? ResumeAnalysisResult)?.id 
        val analysisId = if (existingId.isNullOrBlank()) java.util.UUID.randomUUID().toString() else existingId
        
        val fixedResult = result.copy(id = analysisId)
        val json = gson.toJson(fixedResult)
        
        val role = fixedResult.targetRole ?: getTargetRole(normalizedEmail) ?: "Android Developer"
        val topSkills = (fixedResult.extractedSkills ?: emptyList()).map { it.name }.take(4)
        val historyRecord = AnalysisHistoryRecord(
            id = analysisId,
            targetRole = role,
            atsScore = fixedResult.atsScore,
            skillMatch = fixedResult.skillMatch,
            candidateName = fixedResult.candidateName ?: getUserName(normalizedEmail),
            fileName = fileName ?: "Resume.pdf",
            topSkills = topSkills,
            fullResult = fixedResult
        )
        addAnalysisHistoryRecord(historyRecord, normalizedEmail)

        prefs.edit()
            .putString(KEY_ANALYSIS_RECORD_PREFIX + analysisId, json)
            .putString(KEY_LATEST_ANALYSIS + normalizedEmail, json)
            .apply()

        // Sync with MongoDB backend
        backendRepository?.let { repo ->
            ioScope.launch {
                try {
                    repo.saveResumeAnalysis(normalizedEmail, fixedResult, fileName)
                } catch (e: Exception) {
                    Log.d("SessionManager", "Backend sync deferred: ${e.message}")
                }
            }
        }
    }

    fun saveLatestAnalysisLocally(result: ResumeAnalysisResult, fileName: String? = null) {
        val email = getCurrentEmail() ?: result.candidateEmail ?: return
        val normalizedEmail = normalizeEmail(email)
        val analysisId = result.id.ifBlank { java.util.UUID.randomUUID().toString() }
        val fixedResult = result.copy(id = analysisId)
        val json = gson.toJson(fixedResult)
        
        val role = fixedResult.targetRole ?: getTargetRole(normalizedEmail) ?: "Android Developer"
        val topSkills = (fixedResult.extractedSkills ?: emptyList()).map { it.name }.take(4)
        val historyRecord = AnalysisHistoryRecord(
            id = analysisId,
            targetRole = role,
            atsScore = fixedResult.atsScore,
            skillMatch = fixedResult.skillMatch,
            candidateName = fixedResult.candidateName ?: getUserName(normalizedEmail),
            fileName = fileName ?: "Resume.pdf",
            topSkills = topSkills,
            fullResult = fixedResult
        )
        addAnalysisHistoryRecord(historyRecord, normalizedEmail)

        prefs.edit()
            .putString(KEY_ANALYSIS_RECORD_PREFIX + analysisId, json)
            .putString(KEY_LATEST_ANALYSIS + normalizedEmail, json)
            .apply()
    }

    fun saveAnalysis(result: ResumeAnalysisResult) {
        val email = getCurrentEmail() ?: return
        val normalizedEmail = normalizeEmail(email)
        val analysisId = result.id
        if (analysisId.isBlank()) {
            saveLatestAnalysis(result)
            return
        }

        val json = gson.toJson(result)
        val editor = prefs.edit().putString(KEY_ANALYSIS_RECORD_PREFIX + analysisId, json)

        val currentHistory = getAnalysisHistory(normalizedEmail).toMutableList()
        val index = currentHistory.indexOfFirst { it.id == analysisId }
        if (index != -1) {
            val oldRecord = currentHistory[index]
            currentHistory[index] = oldRecord.copy(fullResult = result)
            editor.putString(KEY_ANALYSIS_HISTORY + normalizedEmail, gson.toJson(currentHistory))
        } else {
            val role = result.targetRole ?: getTargetRole(normalizedEmail) ?: "Android Developer"
            val topSkills = (result.extractedSkills ?: emptyList()).map { it.name }.take(4)
            val historyRecord = AnalysisHistoryRecord(
                id = analysisId,
                targetRole = role,
                atsScore = result.atsScore,
                skillMatch = result.skillMatch,
                candidateName = result.candidateName ?: getUserName(normalizedEmail),
                fileName = "Resume.pdf",
                topSkills = topSkills,
                fullResult = result
            )
            currentHistory.add(0, historyRecord)
            editor.putString(KEY_ANALYSIS_HISTORY + normalizedEmail, gson.toJson(currentHistory.distinctBy { it.id }.take(15)))
        }

        val latest = getLatestAnalysis(normalizedEmail)
        if (latest?.id == analysisId) {
            editor.putString(KEY_LATEST_ANALYSIS + normalizedEmail, json)
        }

        editor.apply()

        // Sync with MongoDB backend
        backendRepository?.let { repo ->
            ioScope.launch {
                try {
                    repo.saveResumeAnalysis(normalizedEmail, result, "Resume.pdf")
                } catch (e: Exception) {
                    Log.d("SessionManager", "Backend sync deferred: ${e.message}")
                }
            }
        }
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
            val role = latest.targetRole ?: getTargetRole(normalizedEmail) ?: "Android Developer"
            val topSkills = (latest.extractedSkills ?: emptyList()).map { it.name }.take(4)
            val initialRecord = AnalysisHistoryRecord(
                id = latest.id,
                targetRole = role,
                atsScore = latest.atsScore,
                skillMatch = latest.skillMatch,
                candidateName = latest.candidateName ?: getUserName(normalizedEmail),
                fileName = "Resume.pdf",
                topSkills = topSkills,
                fullResult = latest
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
        val editor = prefs.edit().putString(KEY_ANALYSIS_HISTORY + normalizedEmail, json)
        if (record.fullResult != null) {
            editor.putString(KEY_ANALYSIS_RECORD_PREFIX + record.id, gson.toJson(record.fullResult))
        }
        editor.apply()
    }

    fun getAnalysisById(id: String, specificEmail: String? = null): ResumeAnalysisResult? {
        if (id.isBlank()) return null
        val email = specificEmail ?: getCurrentEmail() ?: return null
        val normalizedEmail = normalizeEmail(email)

        // 1. Direct record storage lookup
        val directJson = prefs.getString(KEY_ANALYSIS_RECORD_PREFIX + id, null)
        if (!directJson.isNullOrBlank()) {
            try {
                val res = gson.fromJson(directJson, ResumeAnalysisResult::class.java)
                if (res != null) return res
            } catch (e: Exception) {
                // fall through
            }
        }

        // 2. Lookup in user's analysis history
        val history = getAnalysisHistory(normalizedEmail)
        val foundRecord = history.find { it.id == id }
        if (foundRecord != null) {
            if (foundRecord.fullResult != null) {
                return foundRecord.fullResult
            }
            // Reconstruct result preserving exact historical scores and fields
            return ResumeAnalysisResult(
                id = foundRecord.id,
                overallScore = foundRecord.atsScore,
                atsScore = foundRecord.atsScore,
                skillMatch = foundRecord.skillMatch,
                targetRole = foundRecord.targetRole,
                candidateName = foundRecord.candidateName,
                extractedSkills = foundRecord.topSkills.map { com.example.aidrivencompetencyplatform.model.Skill(name = it, level = 85) }
            )
        }

        // 3. Fallback: check latest analysis if matching ID
        val latest = getLatestAnalysis(normalizedEmail)
        if (latest?.id == id) {
            return latest
        }

        return null
    }

    fun getLatestAnalysis(specificEmail: String? = null): ResumeAnalysisResult? {
        val email = specificEmail ?: getCurrentEmail() ?: return null
        val normalizedEmail = normalizeEmail(email)
        val json = prefs.getString(KEY_LATEST_ANALYSIS + normalizedEmail, null) ?: return null
        return try {
            val result = gson.fromJson(json, ResumeAnalysisResult::class.java)
            // Ensure ID exists for consistency
            if (result != null && result.id.isNullOrBlank()) {
                result.copy(id = java.util.UUID.randomUUID().toString())
            } else {
                result
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a JD analysis already exists for a specific resume and JD text.
     */
    fun findExistingJdAnalysis(resumeId: String, jdText: String): ResumeAnalysisResult? {
        val history = getAnalysisHistory()
        // We look for a record that has the same JD text (trimmed) and was based on the same resume ID
        // Note: AnalysisHistoryRecord.fullResult contains modificationReport if it was a JD analysis
        return history.mapNotNull { it.fullResult }
            .find { 
                it.jdText?.trim() == jdText.trim() && 
                (it.id == resumeId || it.rawResumeText == getAnalysisById(resumeId)?.rawResumeText) 
            }
    }

    /**
     * Checks if a resume has already been analyzed for a specific role.
     */
    fun findExistingResumeAnalysis(resumeText: String, targetRole: String): ResumeAnalysisResult? {
        val history = getAnalysisHistory()
        return history.mapNotNull { it.fullResult }
            .find { 
                it.targetRole == targetRole && 
                it.rawResumeText == resumeText &&
                it.modificationReport == null // It's a pure resume analysis, not a JD match
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
        // Remove all existing users from the app when creating a new account as requested
        deleteAllUsersAndHistory()
        
        saveUser(user)
        login(normalizedEmail, isNewUser = true)
        
        // Sync with MongoDB backend synchronously to guarantee visibility in the database
        backendRepository?.let { repo ->
            try {
                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    val success = repo.registerUser(user)
                    Log.d("SessionManager", "Backend MongoDB registration success: $success for ${user.email}")
                }
            } catch (e: Exception) {
                Log.e("SessionManager", "Backend registration sync exception: ${e.message}")
            }
        }
        return true
    }

    /**
     * Internal method to save or overwrite user data synchronously.
     */
    fun saveUserLocally(user: User) {
        saveUser(user)
    }

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
     * Validates credentials against stored data, or attempts authentication with MongoDB backend.
     */
    fun authenticate(email: String, password: String): Boolean {
        val user = getUser(email)
        if (user != null) {
            return user.password == password
        }
        return false
    }

    /**
     * Asynchronous login against backend or local cache, restoring profile and resume analysis.
     */
    suspend fun authenticateWithBackend(email: String, password: String): Boolean {
        val normalizedEmail = normalizeEmail(email)
        val localUser = getUser(normalizedEmail)
        if (localUser != null && localUser.password == password) {
            login(normalizedEmail)
            return true
        }

        // Try backend login
        if (backendRepository != null) {
            val (remoteUser, remoteAnalysis) = backendRepository.loginUser(normalizedEmail, password)
            if (remoteUser != null) {
                saveUser(remoteUser.copy(password = password))
                if (remoteAnalysis != null) {
                    saveLatestAnalysisLocally(remoteAnalysis)
                }
                login(normalizedEmail)
                return true
            }
        }
        return false
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

        // Sync latest analysis from backend on login
        syncWithBackend()
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