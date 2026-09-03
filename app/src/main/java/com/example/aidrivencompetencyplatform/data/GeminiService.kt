package com.example.aidrivencompetencyplatform.data

import com.google.gson.Gson
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.example.aidrivencompetencyplatform.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier
import android.util.Log

class GeminiService {
    // API Key read from BuildConfig with fallback
    private val apiKey = if (BuildConfig.GEMINI_API_KEY != "YOUR_KEY_HERE" && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
        BuildConfig.GEMINI_API_KEY
    } else {
        "AQ.Ab8RN6Jwy3UjrBJoa1mw53fDaioiIqMZGeL_Os6oDkRYCPOxfg"
    }

    // High-performance model cascade:
    // 1. gemini-3.5-flash-lite: ultra-fast (sub-second to 1.5s), reliable JSON extraction
    // 2. gemini-3.5-flash: fast (1-2s), balanced fallback
    // 3. gemini-3.6-flash: comprehensive fallback
    private val modelCascade = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.6-flash"
    )

    private val client = createOkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun createOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier(HostnameVerifier { _, _ -> true })
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    suspend fun analyzeResume(resumeText: String, targetRole: String): ResumeAnalysisResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is missing. Please provide a valid key from Google AI Studio.")
        }

        val prompt = """
            You are a strict resume parsing and career analysis system.
            The uploaded resume text below is the ONLY source of truth.
            
            EXTRACTED RESUME TEXT:
            $resumeText
            
            TARGET ROLE:
            $targetRole
            
            INSTRUCTIONS:
            1. Thoroughly extract candidate contact and personal details from headers, contact sections, address lines, and personal details:
               - candidateName: Full legal name or display name of the candidate.
               - candidateEmail: Email address.
               - candidatePhone: Phone or mobile number in any format (e.g. +1..., +91..., (xxx) xxx-xxxx, or raw digits).
               - candidateLocation: Candidate's city, state/province, region, and/or country (e.g. "San Francisco, CA", "Austin, TX", "Bangalore, India", "Trichy, Tamil Nadu").
            2. NEVER invent information, infer missing personal details, or use placeholder example values.
            3. If information is not present in the resume, return null (do NOT output strings like "Not detected", "N/A", or "None").
            4. Generate dynamic scores (0-100) for Keyword Coverage, Resume Structure, Formatting Safety, and Parsing Accuracy based strictly on this resume.
            5. Provide evidence for each score in the 'explanations' object.
            6. Extract all technical and soft skills semantically and categorize them.
            7. Extract work experience, education, and projects with high accuracy.
            
            REQUIRED FIELDS TO EXTRACT:
            - candidateName, candidateEmail, candidatePhone, candidateLocation
            - education (List of strings or institutions/degrees)
            - experience (List of strings summarizing work history)
            - extractedSkills (Full list of skills found)
            - projectAnalysis (List of projects found with their details)
            - summary (Professional summary if present)
            - missingSections (List of standard sections NOT found: "Contact Info", "Work Experience", "Education", "Skills", "Projects", "Summary")
            
            JSON FORMAT REQUIRED:
            {
              "overallScore": 0, 
              "atsScore": 0,
              "skillMatch": 0,
              "candidateName": "string or null",
              "candidateEmail": "string or null",
              "candidatePhone": "string or null",
              "candidateLocation": "string or null",
              "summary": "string",
              "strengths": ["string"],
              "weaknesses": ["string"],
              "education": ["string"],
              "experience": ["string"],
              "atsBreakdown": {
                "keywordCoverage": 0,
                "resumeStructure": 0,
                "formattingSafety": 0,
                "parsingAccuracy": 0,
                "explanations": {
                    "keywords": [{"label": "string", "isDetected": true, "detail": "...", "suggestion": "..."}],
                    "structure": [{"label": "string", "isDetected": true, "detail": "..."}],
                    "formatting": [{"label": "string", "isDetected": true, "detail": "..."}],
                    "parsing": [{"label": "string", "isDetected": true, "detail": "..."}]
                }
              },
              "extractedSkills": [
                {"name": "...", "level": 0, "category": "...", "importance": "...", "evidence": "..."}
              ],
              "missingSections": ["..."],
              "formattingRisks": ["..."],
              "parsingAccuracyDetails": {
                "nameDetected": true, "contactDetected": true, "educationDetected": true, "skillsDetected": true, "experienceDetected": true, "projectsDetected": true
              },
              "projectAnalysis": [
                {
                  "name": "...", "technologies": ["..."], "demonstratedSkills": ["..."], "strengths": ["..."], "weaknesses": ["..."], "improvementSuggestions": ["..."]
                }
              ],
              "prioritizedSuggestions": {
                "highPriority": ["..."], "mediumPriority": ["..."], "lowPriority": ["..."]
              }
            }
            
            IMPORTANT: Return ONLY valid structured JSON. NO markdown code blocks. NO additional text.
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText)
        if (jsonString != null) {
            try {
                val parsed = gson.fromJson(jsonString, ResumeAnalysisResult::class.java)
                sanitizeAnalysisResult(parsed)
            } catch (e: Exception) {
                Log.e("GeminiService", "Parsing error: ${e.message}")
                throw Exception("Failed to parse AI response into structured analysis. Format was unexpected.", e)
            }
        } else {
            Log.e("GeminiService", "No JSON found in response")
            throw Exception("AI response did not contain valid JSON analysis data.")
        }
    }

    private fun sanitizeAnalysisResult(result: ResumeAnalysisResult): ResumeAnalysisResult {
        return result.copy(
            candidateName = sanitizeField(result.candidateName),
            candidateEmail = sanitizeField(result.candidateEmail),
            candidatePhone = sanitizeField(result.candidatePhone),
            candidateLocation = sanitizeField(result.candidateLocation)
        )
    }

    private fun sanitizeField(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val clean = value.trim()
        val lower = clean.lowercase()
        val invalid = setOf(
            "null", "none", "n/a", "na", "not detected", "not specified",
            "not provided", "not found", "unknown", "nil", "-", "--", "undefined", "empty"
        )
        return if (invalid.contains(lower) || lower.startsWith("not detected") || lower.startsWith("not found")) null else clean
    }

    suspend fun getAiAssistantResponse(query: String, context: String): String = withContext(Dispatchers.IO) {
        val prompt = "You are Nova, an AI career assistant. Use the following context to help the user.\nContext: $context\nUser Query: $query"
        try {
            val responseText = executeWithFallbackAndRetry(prompt, isJson = false)
            responseText ?: "I'm sorry, I couldn't process that request."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Executes the Gemini request across the model cascade with retry and exponential backoff.
     * Prevents key exposure in logs by passing the key in HTTP headers.
     */
    private suspend fun executeWithFallbackAndRetry(promptText: String, isJson: Boolean): String? {
        var lastException: Exception? = null

        for (model in modelCascade) {
            val maxAttempts = 2
            for (attempt in 1..maxAttempts) {
                try {
                    val result = callGeminiApi(model, promptText, isJson)
                    if (!result.isNullOrBlank()) {
                        return result
                    }
                } catch (e: Exception) {
                    lastException = e
                    val errorMsg = e.message ?: ""
                    Log.w("GeminiService", "Attempt $attempt on model $model failed: $errorMsg")
                    
                    // If rate limited or 503 service unavailable, pause briefly before retry
                    if (errorMsg.contains("503") || errorMsg.contains("429") || errorMsg.contains("unavailable", ignoreCase = true)) {
                        if (attempt < maxAttempts) {
                            delay(400L * attempt)
                        }
                    } else if (errorMsg.contains("404") || errorMsg.contains("400")) {
                        // Bad model or request: immediately try next model
                        break
                    }
                }
            }
        }

        throw lastException ?: Exception("Network error: Could not reach Gemini AI. Please check your internet connection.")
    }

    private fun callGeminiApi(model: String, promptText: String, isJson: Boolean): String? {
        // Securely use header authentication without exposing API key in the URL
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val requestBodyMap = mutableMapOf<String, Any>(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to promptText))))
        )
        if (isJson) {
            requestBodyMap["generationConfig"] = mapOf(
                "response_mime_type" to "application/json"
            )
        }

        val requestBody = gson.toJson(requestBodyMap).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = when (code) {
                        400 -> "Bad Request (400): Invalid request parameter."
                        401, 403 -> "Authentication failed: Please verify your Gemini API key."
                        404 -> "Model not found (404): Model '$model' is unavailable."
                        429 -> "Rate limit (429): Quota exceeded. Retrying shortly."
                        500, 502, 503, 504 -> "Server error ($code): Gemini service temporarily unavailable."
                        else -> "API Error (HTTP $code)"
                    }
                    throw Exception(errorMsg)
                }

                val geminiResponse = gson.fromJson(responseBody, GeminiApiResponse::class.java)
                val candidate = geminiResponse.candidates?.firstOrNull()
                val parts = candidate?.content?.parts ?: emptyList()
                
                // Filter out thought tokens and concatenate text
                val textParts = parts.filter { it.thought != true }.mapNotNull { it.text }
                textParts.joinToString("\n").ifBlank {
                    parts.firstOrNull()?.text
                }
            }
        } catch (e: java.io.IOException) {
            throw Exception("Network connection timeout while contacting Gemini AI.", e)
        }
    }

    private fun extractJson(text: String?): String? {
        if (text == null) return null

        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        val start = cleaned.indexOf("{")
        val end = cleaned.lastIndexOf("}")
        return if (start != -1 && end != -1 && end > start) cleaned.substring(start, end + 1) else null
    }

    private data class GeminiApiResponse(val candidates: List<Candidate>?)
    private data class Candidate(val content: Content?, val finishReason: String? = null)
    private data class Content(val parts: List<Part>?, val role: String? = null)
    private data class Part(val text: String?, val thought: Boolean? = null)
}
