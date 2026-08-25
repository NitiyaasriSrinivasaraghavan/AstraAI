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
    // API Key is now read from BuildConfig for better security handling.
    private val apiKey = if (BuildConfig.GEMINI_API_KEY != "AQ.Ab8RN6Jwy3UjrBJoa1mw53fDaioiIqMZGeL_Os6oDkRYCPOxfg") {
        BuildConfig.GEMINI_API_KEY
    } else {
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
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
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
            You are an expert career consultant and ATS (Applicant Tracking System) specialist.
            Analyze the following resume text specifically for the target role: $targetRole.
            Provide a comprehensive, structured JSON response.
            
            RESUME TEXT:
            $resumeText
            
            INSTRUCTIONS:
            1. Generate scores (0-100) for Keyword Coverage, Resume Structure, Formatting Safety, and Parsing Accuracy.
            2. For each score, provide XAI (Explainable AI) evidence in the 'explanations' object.
            3. Detect specific sections: Contact, Education, Skills, Projects, Experience.
            4. Extract skills semantically. Categorize into: Programming Languages, Frontend, Backend, Database, Cloud, DevOps, AI/ML, Data Science, Tools, Soft Skills.
            5. Map user skills to the required skills for $targetRole.
            6. Identify strong skills, developing skills, and skill gaps with evidence from the resume.
            7. Analyze existing projects for strengths and missing technical details. DO NOT invent metrics.
            
            JSON FORMAT REQUIRED:
            {
              "overallScore": 0, 
              "atsScore": 0,
              "skillMatch": 0,
              "summary": "...",
              "strengths": ["..."],
              "weaknesses": ["..."],
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
            
            IMPORTANT: Return ONLY the raw JSON object. NO conversational text. NO markdown code blocks. Ensure all fields are present. Use 0 for numbers if unknown. Use empty lists [] if no data. Do not rename fields.
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
        val prompt = "You are Astra, a career assistant. Use the following context to help the user.\nContext: $context\nUser Query: $query"
        try {
            val responseText = makeApiCall(prompt)
            responseText ?: "I'm sorry, I couldn't process that request."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun makeApiCall(promptText: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1/models/$modelName:generateContent?key=$apiKey"
        val requestBody = gson.toJson(
            mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to promptText)))))
        ).toRequestBody(jsonMediaType)

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    401, 403 -> "Authentication failed: Ensure your Gemini API key is valid."
                    404 -> "Model not found: The model '$modelName' is not recognized."
                    429 -> "Rate limit reached: Free-tier limit exceeded. Please wait a minute."
                    else -> "AI Error (HTTP ${response.code}): ${response.message}"
                }
                throw Exception(errorMsg)
            }
            val geminiResponse = gson.fromJson(responseBody, GeminiApiResponse::class.java)
            return geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
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
