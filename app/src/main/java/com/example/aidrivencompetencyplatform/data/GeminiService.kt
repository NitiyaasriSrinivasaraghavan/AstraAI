package com.example.aidrivencompetencyplatform.data

import com.google.gson.Gson
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
import kotlinx.coroutines.Dispatchers
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

class GeminiService {
    // API Key is now read from BuildConfig with improved fallback logic.
    private val apiKey = if (BuildConfig.GEMINI_API_KEY != "YOUR_KEY_HERE" && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
        BuildConfig.GEMINI_API_KEY
    } else {
        // Fallback key if the user hasn't provided one in gradle.properties
        "AQ.Ab8RN6Jwy3UjrBJoa1mw53fDaioiIqMZGeL_Os6oDkRYCPOxfg"
    }

    private val modelName = "gemini-3.6-flash"
    private val client = createUnsafeOkHttpClient()

    private fun createUnsafeOkHttpClient(): OkHttpClient {
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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
            1. Extract candidate information ONLY from the supplied resume text.
            2. NEVER invent information, infer missing personal details, or use example values.
            3. If information is not explicitly present in the resume, return null for those fields.
            4. Generate dynamic scores (0-100) for Keyword Coverage, Resume Structure, Formatting Safety, and Parsing Accuracy based strictly on this resume.
            5. Provide XAI evidence for each score in the 'explanations' object.
            6. Extract skills semantically and categorize them.
            7. Analyze existing projects for strengths and missing technical details.
            
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
            
            IMPORTANT: Return ONLY valid structured JSON. NO markdown code blocks. NO additional text. If a field is not found in the resume, use null.
        """.trimIndent()

        try {
            val responseText = makeApiCall(prompt)
            val jsonString = extractJson(responseText)
            if (jsonString != null) {
                try {
                    android.util.Log.d("GeminiService", "Extracted JSON: $jsonString")
                    gson.fromJson(jsonString, ResumeAnalysisResult::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("GeminiService", "Parsing error: ${e.message}", e)
                    throw Exception("Failed to parse AI response. The data format was unexpected.")
                }
            } else {
                android.util.Log.e("GeminiService", "No JSON found in response: $responseText")
                throw Exception("AI response did not contain valid JSON analysis data.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getAiAssistantResponse(query: String, context: String): String = withContext(Dispatchers.IO) {
        val prompt = "You are Nova, a career assistant. Use the following context to help the user.\nContext: $context\nUser Query: $query"
        try {
            val responseText = makeApiCall(prompt)
            responseText ?: "I'm sorry, I couldn't process that request."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun makeApiCall(promptText: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        
        // Use JSON mode for faster and more reliable structured data extraction
        val requestBodyMap = mutableMapOf(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to promptText)))),
            "generationConfig" to mapOf(
                "response_mime_type" to "application/json"
            )
        )
        
        val requestBody = gson.toJson(requestBodyMap).toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        400 -> "Bad Request: The API request was invalid. Please check your prompt or API configuration."
                        401, 403 -> "Authentication failed: Ensure your Gemini API key is valid and has not expired."
                        404 -> "Model not found: The model '$modelName' is not recognized."
                        429 -> "Rate limit reached: Free-tier limit exceeded. Please wait a minute."
                        500, 503 -> "Server error: Gemini AI is currently unavailable. Please try again later."
                        else -> "AI Error (HTTP ${response.code}): ${response.message}"
                    }
                    throw Exception(errorMsg)
                }
                val geminiResponse = gson.fromJson(responseBody, GeminiApiResponse::class.java)
                geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }
        } catch (e: java.io.IOException) {
            throw Exception("Network error: Could not reach Gemini AI. Please check your internet connection.", e)
        }
    }

    private fun extractJson(text: String?): String? {
        if (text == null) return null
        
        // Remove markdown code blocks if present
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
    private data class Candidate(val content: Content?)
    private data class Content(val parts: List<Part>?)
    private data class Part(val text: String?)
}
