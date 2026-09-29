package com.example.aidrivencompetencyplatform.data

import android.util.Log
import com.example.aidrivencompetencyplatform.model.AnalysisHistoryRecord
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
import com.example.aidrivencompetencyplatform.model.User
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Backend API Client for Persistent MongoDB Account + Resume Analysis storage.
 * Communicates with the local or cloud Express/MongoDB backend service.
 * Never stores or exposes database credentials on the Android client.
 */
class BackendRepository {

    companion object {
        private const val TAG = "BackendRepository"
        // 10.0.2.2 maps to localhost of the host development machine in Android emulator
        // 127.0.0.1 / localhost for unit tests / desktop JVM
        private const val BASE_URL_EMULATOR = "http://10.0.2.2:5000/api"
        private const val BASE_URL_LOCAL = "http://127.0.0.1:5000/api"
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5000, TimeUnit.MILLISECONDS)
        .readTimeout(5000, TimeUnit.MILLISECONDS)
        .writeTimeout(5000, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var activeBaseUrl: String = BASE_URL_EMULATOR

    /**
     * Attempts a request with emulator base URL, falling back to local IP if needed.
     */
    private fun executeRequest(requestBuilder: (baseUrl: String) -> Request): String? {
        val primaryUrl = activeBaseUrl
        try {
            val request = requestBuilder(primaryUrl)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend attempt on $primaryUrl: ${e.message}")
        }
        val fallbackUrl = if (primaryUrl == BASE_URL_EMULATOR) BASE_URL_LOCAL else BASE_URL_EMULATOR
        try {
            val request = requestBuilder(fallbackUrl)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    activeBaseUrl = fallbackUrl
                    return response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend attempt on $fallbackUrl: ${e.message}")
        }
        return null
    }

    /**
     * Registers a new user on the MongoDB backend.
     */
    suspend fun registerUser(user: User): Boolean = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "name" to user.name,
            "email" to user.email.trim().lowercase(),
            "password" to user.password,
            "targetRole" to (user.targetRole ?: "Android Developer")
        )
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val response = executeRequest { baseUrl ->
            Request.Builder()
                .url("$baseUrl/auth/register")
                .post(body)
                .build()
        }
        if (response != null) {
            try {
                val obj = gson.fromJson(response, JsonObject::class.java)
                return@withContext obj.get("success")?.asBoolean == true
            } catch (e: Exception) {
                Log.e(TAG, "Parse error on register response: ${e.message}")
            }
        }
        false
    }

    /**
     * Authenticates a user with the MongoDB backend and retrieves their stored profile & latest analysis.
     */
    suspend fun loginUser(email: String, password: String): Pair<User?, ResumeAnalysisResult?> = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "email" to email.trim().lowercase(),
            "password" to password
        )
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val response = executeRequest { baseUrl ->
            Request.Builder()
                .url("$baseUrl/auth/login")
                .post(body)
                .build()
        }
        if (response != null) {
            try {
                val obj = gson.fromJson(response, JsonObject::class.java)
                if (obj.get("success")?.asBoolean == true) {
                    val userObj = obj.getAsJsonObject("user")
                    val user = if (userObj != null) {
                        User(
                            name = userObj.get("name")?.asString ?: "",
                            email = userObj.get("email")?.asString ?: email,
                            password = password,
                            targetRole = userObj.get("targetRole")?.asString
                        )
                    } else null

                    val analysisObj = obj.get("latestAnalysis")
                    val analysis = if (analysisObj != null && !analysisObj.isJsonNull) {
                        gson.fromJson(analysisObj, ResumeAnalysisResult::class.java)
                    } else null

                    return@withContext Pair(user, analysis)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse error on login response: ${e.message}")
            }
        }
        Pair(null, null)
    }

    /**
     * Fetches the user profile from the MongoDB backend.
     */
    suspend fun fetchUserProfile(email: String): User? = withContext(Dispatchers.IO) {
        val normalized = email.trim().lowercase()
        val response = executeRequest { baseUrl ->
            Request.Builder()
                .url("$baseUrl/user/profile?email=$normalized")
                .get()
                .build()
        }
        if (response != null) {
            try {
                val obj = gson.fromJson(response, JsonObject::class.java)
                if (obj.get("success")?.asBoolean == true) {
                    val userObj = obj.getAsJsonObject("user")
                    if (userObj != null) {
                        return@withContext User(
                            name = userObj.get("name")?.asString ?: "",
                            email = userObj.get("email")?.asString ?: normalized,
                            password = "",
                            targetRole = userObj.get("targetRole")?.asString
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user profile: ${e.message}")
            }
        }
        null
    }

    /**
     * Saves a resume analysis result to MongoDB associated with the user's account.
     */
    suspend fun saveResumeAnalysis(email: String, result: ResumeAnalysisResult, fileName: String?): Boolean = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "email" to email.trim().lowercase(),
            "fileName" to (fileName ?: "Resume.pdf"),
            "analysisResult" to result
        )
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val response = executeRequest { baseUrl ->
            Request.Builder()
                .url("$baseUrl/resume/analysis")
                .post(body)
                .build()
        }
        if (response != null) {
            try {
                val obj = gson.fromJson(response, JsonObject::class.java)
                return@withContext obj.get("success")?.asBoolean == true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving resume analysis: ${e.message}")
            }
        }
        false
    }

    /**
     * Fetches the latest resume analysis from MongoDB for the given user.
     */
    suspend fun fetchLatestAnalysis(email: String): ResumeAnalysisResult? = withContext(Dispatchers.IO) {
        val normalized = email.trim().lowercase()
        val response = executeRequest { baseUrl ->
            Request.Builder()
                .url("$baseUrl/resume/latest?email=$normalized")
                .get()
                .build()
        }
        if (response != null) {
            try {
                val obj = gson.fromJson(response, JsonObject::class.java)
                if (obj.get("success")?.asBoolean == true && obj.has("analysis") && !obj.get("analysis").isJsonNull) {
                    return@withContext gson.fromJson(obj.get("analysis"), ResumeAnalysisResult::class.java)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest analysis: ${e.message}")
            }
        }
        null
    }

    /**
     * Fetches the analysis history records from MongoDB.
     */
    suspend fun fetchAnalysisHistory(email: String): List<AnalysisHistoryRecord> = withContext(Dispatchers.IO) {
        val normalized = email.trim().lowercase()
        val response = executeRequest { baseUrl ->
            Request.Builder()
                .url("$baseUrl/resume/history?email=$normalized")
                .get()
                .build()
        }
        if (response != null) {
            try {
                val obj = gson.fromJson(response, JsonObject::class.java)
                if (obj.get("success")?.asBoolean == true && obj.has("history")) {
                    val type = object : TypeToken<List<AnalysisHistoryRecord>>() {}.type
                    return@withContext gson.fromJson(obj.get("history"), type) ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching history: ${e.message}")
            }
        }
        emptyList()
    }
}
