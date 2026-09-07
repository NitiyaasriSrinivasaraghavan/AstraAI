package com.example.aidrivencompetencyplatform.data

import com.google.gson.*
import com.example.aidrivencompetencyplatform.model.*
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
import kotlin.math.roundToInt
import com.example.aidrivencompetencyplatform.viewmodel.RoleSkillsData

class GeminiService {
    // API Key read from BuildConfig with fallback
    private val apiKey = if (BuildConfig.GEMINI_API_KEY != "YOUR_KEY_HERE" && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
        BuildConfig.GEMINI_API_KEY
    } else {
        "AQ.Ab8RN6Jwy3UjrBJoa1mw53fDaioiIqMZGeL_Os6oDkRYCPOxfg"
    }

    // Prioritized model cascade with confirmed high-performance and available endpoints
    private val modelCascade = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-flash-lite-latest"
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
            You are an expert, high-precision ATS resume analysis and information extraction system.
            The extracted text below is the EXCLUSIVE source of truth.
            
            EXTRACTED RESUME TEXT:
            $resumeText
            
            TARGET ROLE:
            $targetRole
            
            STRICT EXTRACTION & CLASSIFICATION RULES:
            1. SINGLE SOURCE OF TRUTH: ONLY extract facts present in the resume. Do NOT fabricate, invent, or hallucinate names, companies, roles, dates, or projects.
            
            2. SUMMARY (Semantic Extraction):
               - Extract the professional summary, profile statement, career objective, overview, or introductory professional paragraph based on content and semantic meaning.
               - Look for headings such as 'Summary', 'Professional Summary', 'Profile', 'Objective', 'Career Objective', 'About Me', 'Overview', or any introductory career paragraph near the top.
               - If no summary/intro paragraph exists in the resume, return null. Never invent one.
            
            3. WORK EXPERIENCE (Strict Category Isolation):
               - 'experience' MUST contain ONLY genuine employment or internship entries (Company/Organization, Job/Internship Title, Dates/Duration, Responsibilities/Achievements).
               - If the resume contains only internships and no full-time jobs, include only those internships.
               - STRICTLY FORBIDDEN: NEVER put Summary text, Education degrees, Certifications, Skills, Locations, or Coursework into 'experience'.
               - If the candidate is a fresher or student with NO work experience or internships, 'experience' MUST be an empty list []. Never fill it with other sections.
            
            4. EDUCATION (Preserve ALL Tiers):
               - 'education' MUST preserve ALL educational qualifications present in the resume without truncation:
                 * 10th / Secondary School / SSLC / Matriculation / CBSE Class 10 / ICSE Class 10
                 * 12th / Higher Secondary / HSC / Intermediate / CBSE Class 12 / ISC Class 12 / Pre-University
                 * Diploma / Polytechnic
                 * Undergraduate Degree (B.Tech, B.E., B.Sc., BCA, B.Com, etc.)
                 * Postgraduate Degree (M.Tech, M.S., MCA, MBA, etc.)
               - Include Degree/Standard, School/College/University, Board, Graduation Year, and CGPA/Percentage for each qualification.
               - If the resume contains 10th, 12th, and college, ALL THREE must be extracted as separate items in the 'education' array.
            
            5. PROJECTS (Strict Semantic Project Validation):
               - 'projectAnalysis' MUST contain ONLY genuine projects (academic, personal, capstone, or open-source) with actual project titles, descriptions, and technologies used.
               - STRICTLY FORBIDDEN: Do NOT classify lone technical terms (e.g., 'Database Management', 'Machine Learning', 'SQL', 'Operating Systems', 'PALM', 'MySQL'), courses, subjects, or certifications as projects.
               - Certifications MUST remain under certifications, NEVER in projects.
               - The number of projects in 'projectAnalysis' MUST match the actual number of projects in the resume (if 2 exist, return 2; if 0, return []).
            
            6. SKILLS & CERTIFICATIONS:
               - Extract all technical skills, frameworks, programming languages, and developer tools into 'extractedSkills'.
            
            JSON OUTPUT SCHEMA (Valid RFC 8259 JSON ONLY):
            {
              "overallScore": 0, 
              "atsScore": 0,
              "skillMatch": 0,
              "candidateName": "string or null",
              "candidateEmail": "string or null",
              "candidatePhone": "string or null",
              "candidateLocation": "string or null",
              "summary": "string or null",
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
                {"name": "...", "level": 80, "category": "...", "importance": "...", "evidence": "..."}
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
            
            IMPORTANT: Return ONLY valid JSON.
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText)
            ?: throw Exception("AI response did not contain valid JSON analysis data.")

        try {
            parseResumeAnalysisJson(jsonString, targetRole, resumeText)
        } catch (e: Exception) {
            Log.e("GeminiService", "Parsing error: ${e.message}", e)
            throw Exception("Failed to parse AI response into structured analysis. Format was unexpected.", e)
        }
    }

    /**
     * Resilient JSON parser that gracefully handles polymorphic fields,
     * mixed types (objects vs strings), string levels, and unclosed arrays.
     */
    fun parseResumeAnalysisJson(
        jsonString: String,
        targetRole: String? = null,
        rawResumeText: String? = null
    ): ResumeAnalysisResult {
        val rootElement = try {
            JsonParser.parseString(jsonString)
        } catch (e: Exception) {
            val repaired = repairJson(jsonString)
            JsonParser.parseString(repaired)
        }

        if (!rootElement.isJsonObject) {
            throw Exception("Expected JSON object root, but got ${rootElement.javaClass.simpleName}")
        }
        val root = rootElement.asJsonObject

        fun getSafeInt(obj: JsonObject, key: String, default: Int = 0): Int {
            val elem = obj.get(key) ?: return default
            if (elem.isJsonPrimitive) {
                val prim = elem.asJsonPrimitive
                if (prim.isNumber) return prim.asInt
                if (prim.isString) {
                    val clean = prim.asString.trim().replace("%", "")
                    return clean.toIntOrNull() ?: clean.toDoubleOrNull()?.roundToInt() ?: default
                }
            }
            return default
        }

        fun getSafeString(obj: JsonObject, key: String): String? {
            val elem = obj.get(key) ?: return null
            if (elem.isJsonNull) return null
            if (elem.isJsonPrimitive) {
                return sanitizeField(elem.asString)
            }
            if (elem.isJsonObject) {
                val parts = elem.asJsonObject.entrySet().mapNotNull { entry ->
                    val v = entry.value
                    if (v.isJsonPrimitive) sanitizeField(v.asString) else null
                }
                return if (parts.isNotEmpty()) parts.joinToString(", ") else null
            }
            return null
        }

        fun getSafeStringList(obj: JsonObject, key: String): List<String> {
            val elem = obj.get(key) ?: return emptyList()
            if (elem.isJsonArray) {
                return elem.asJsonArray.mapNotNull { item ->
                    when {
                        item.isJsonPrimitive -> sanitizeField(item.asString)
                        item.isJsonObject -> {
                            val subParts = item.asJsonObject.entrySet().mapNotNull { entry ->
                                val v = entry.value
                                if (v.isJsonPrimitive) "${entry.key}: ${v.asString}" else null
                            }
                            if (subParts.isNotEmpty()) subParts.joinToString(" | ") else null
                        }
                        else -> null
                    }
                }
            }
            if (elem.isJsonPrimitive) {
                val str = sanitizeField(elem.asString)
                return if (str != null) listOf(str) else emptyList()
            }
            return emptyList()
        }

        fun parseSkillLevel(elem: JsonElement?): Int {
            if (elem == null || elem.isJsonNull) return 75
            if (elem.isJsonPrimitive) {
                val prim = elem.asJsonPrimitive
                if (prim.isNumber) return prim.asInt.coerceIn(0, 100)
                if (prim.isString) {
                    val str = prim.asString.trim().lowercase()
                    val num = str.replace("%", "").toIntOrNull()
                    if (num != null) return num.coerceIn(0, 100)
                    return when {
                        str.contains("expert") || str.contains("lead") -> 95
                        str.contains("advanc") || str.contains("senior") -> 85
                        str.contains("proficient") || str.contains("intermed") || str.contains("mid") -> 75
                        str.contains("basic") || str.contains("beginn") || str.contains("junior") -> 50
                        else -> 75
                    }
                }
            }
            return 75
        }

        fun getSafeSkills(obj: JsonObject): List<Skill> {
            val elem = obj.get("extractedSkills") ?: return emptyList()
            if (elem.isJsonArray) {
                return elem.asJsonArray.mapNotNull { item ->
                    when {
                        item.isJsonObject -> {
                            val sObj = item.asJsonObject
                            val name = getSafeString(sObj, "name") ?: return@mapNotNull null
                            val level = parseSkillLevel(sObj.get("level"))
                            val category = getSafeString(sObj, "category") ?: "Technical"
                            val importance = getSafeString(sObj, "importance")
                            val evidence = getSafeString(sObj, "evidence")
                            val reason = getSafeString(sObj, "reason")
                            Skill(
                                name = name,
                                level = level,
                                category = category,
                                importance = importance,
                                evidence = evidence,
                                reason = reason
                            )
                        }
                        item.isJsonPrimitive -> {
                            val name = item.asString.trim()
                            if (name.isNotBlank()) Skill(name = name, level = 75, category = "Technical") else null
                        }
                        else -> null
                    }
                }
            }
            return emptyList()
        }

        fun getSafeAtsBreakdown(obj: JsonObject): AtsBreakdown {
            val atsObj = obj.getAsJsonObject("atsBreakdown") ?: JsonObject()
            val keyword = getSafeInt(atsObj, "keywordCoverage", 75)
            val structure = getSafeInt(atsObj, "resumeStructure", 75)
            val formatting = getSafeInt(atsObj, "formattingSafety", 80)
            val parsing = getSafeInt(atsObj, "parsingAccuracy", 80)

            val explanationsObj = atsObj.getAsJsonObject("explanations") ?: JsonObject()
            fun parseEvidenceList(expObj: JsonObject, key: String): List<AtsEvidence> {
                val arr = expObj.getAsJsonArray(key) ?: return emptyList()
                return arr.mapNotNull { item ->
                    when {
                        item.isJsonObject -> {
                            val iObj = item.asJsonObject
                            val label = getSafeString(iObj, "label") ?: "Check"
                            val isDetected = iObj.get("isDetected")?.let { d ->
                                if (d.isJsonPrimitive && d.asJsonPrimitive.isBoolean) d.asBoolean
                                else d.asString.equals("true", ignoreCase = true) || d.asString.equals("yes", ignoreCase = true)
                            } ?: true
                            val detail = getSafeString(iObj, "detail")
                            val suggestion = getSafeString(iObj, "suggestion")
                            AtsEvidence(label = label, isDetected = isDetected, detail = detail, suggestion = suggestion)
                        }
                        item.isJsonPrimitive -> {
                            val text = item.asString.trim()
                            if (text.isNotBlank()) AtsEvidence(label = text.take(35), isDetected = true, detail = text) else null
                        }
                        else -> null
                    }
                }
            }

            val explanations = AtsExplanations(
                keywords = parseEvidenceList(explanationsObj, "keywords"),
                structure = parseEvidenceList(explanationsObj, "structure"),
                formatting = parseEvidenceList(explanationsObj, "formatting"),
                parsing = parseEvidenceList(explanationsObj, "parsing")
            )

            return AtsBreakdown(
                keywordCoverage = keyword,
                resumeStructure = structure,
                formattingSafety = formatting,
                parsingAccuracy = parsing,
                explanations = explanations
            )
        }

        fun getSafeProjectAnalysis(obj: JsonObject): List<ProjectAnalysis> {
            val elem = obj.get("projectAnalysis") ?: obj.get("projects") ?: return emptyList()
            val technicalBlacklist = setOf(
                "sql", "mysql", "java", "python", "management", "palm", "ml", "database", "database management",
                "cloud computing", "machine learning", "artificial intelligence", "data structures", "algorithms",
                "operating systems", "computer networks", "software engineering", "web technologies"
            )

            if (elem.isJsonArray) {
                return elem.asJsonArray.mapNotNull { item ->
                    when {
                        item.isJsonObject -> {
                            val pObj = item.asJsonObject
                            val name = getSafeString(pObj, "name") ?: getSafeString(pObj, "title") ?: return@mapNotNull null
                            val cleanName = name.trim()
                            if (technicalBlacklist.any { cleanName.equals(it, ignoreCase = true) } ||
                                cleanName.lowercase().contains("certification") ||
                                cleanName.lowercase().contains("certified")) {
                                return@mapNotNull null
                            }
                            val tech = getSafeStringList(pObj, "technologies")
                            val demoSkills = getSafeStringList(pObj, "demonstratedSkills")
                            val strengths = getSafeStringList(pObj, "strengths")
                            val weaknesses = getSafeStringList(pObj, "weaknesses")
                            val suggestions = getSafeStringList(pObj, "improvementSuggestions")
                            ProjectAnalysis(
                                name = cleanName,
                                technologies = tech,
                                demonstratedSkills = demoSkills,
                                strengths = strengths,
                                weaknesses = weaknesses,
                                improvementSuggestions = suggestions
                            )
                        }
                        item.isJsonPrimitive -> {
                            val name = item.asString.trim()
                            if (name.isNotBlank() && !technicalBlacklist.any { name.equals(it, ignoreCase = true) } &&
                                !name.lowercase().contains("certification") && !name.lowercase().contains("certified")) {
                                ProjectAnalysis(name = name)
                            } else null
                        }
                        else -> null
                    }
                }
            }
            return emptyList()
        }

        fun getSafePrioritizedSuggestions(obj: JsonObject): PrioritizedSuggestions {
            val pObj = obj.getAsJsonObject("prioritizedSuggestions") ?: JsonObject()
            return PrioritizedSuggestions(
                highPriority = getSafeStringList(pObj, "highPriority"),
                mediumPriority = getSafeStringList(pObj, "mediumPriority"),
                lowPriority = getSafeStringList(pObj, "lowPriority")
            )
        }

        fun getSafeParsingAccuracyDetails(obj: JsonObject): ParsingAccuracyDetails {
            val pObj = obj.getAsJsonObject("parsingAccuracyDetails") ?: JsonObject()
            fun getBool(k: String, def: Boolean = true): Boolean {
                val e = pObj.get(k) ?: return def
                if (e.isJsonPrimitive && e.asJsonPrimitive.isBoolean) return e.asBoolean
                if (e.isJsonPrimitive && e.asJsonPrimitive.isString) return e.asString.equals("true", ignoreCase = true)
                return def
            }
            return ParsingAccuracyDetails(
                nameDetected = getBool("nameDetected"),
                contactDetected = getBool("contactDetected"),
                educationDetected = getBool("educationDetected"),
                skillsDetected = getBool("skillsDetected"),
                experienceDetected = getBool("experienceDetected"),
                projectsDetected = getBool("projectsDetected")
            )
        }

        val overallScore = getSafeInt(root, "overallScore", 75)
        val atsScore = getSafeInt(root, "atsScore", overallScore)
        val skillMatch = getSafeInt(root, "skillMatch", 70)

        val candidateName = getSafeString(root, "candidateName")
        val candidateEmail = getSafeString(root, "candidateEmail")
        val candidatePhone = getSafeString(root, "candidatePhone")
        val candidateLocation = getSafeString(root, "candidateLocation")
        val summary = getSafeString(root, "summary") ?: ""

        val rawEducation = getSafeStringList(root, "education")
        val rawExperience = getSafeStringList(root, "experience")

        // Cross-contamination filter for experience
        val cleanedExperience = rawExperience.filter { exp ->
            val lw = exp.lowercase().trim()
            val isDegree = lw.startsWith("bachelor") || lw.startsWith("b.tech") || lw.startsWith("b.e") ||
                           lw.startsWith("10th") || lw.startsWith("12th") || lw.startsWith("sslc") || lw.startsWith("hsc")
            val isCert = lw.startsWith("certified") || lw.startsWith("certification") || lw.startsWith("google associate")
            !isDegree && !isCert && exp.length > 5
        }

        val strengths = getSafeStringList(root, "strengths")
        val weaknesses = getSafeStringList(root, "weaknesses")
        val missingSections = getSafeStringList(root, "missingSections")
        val formattingRisks = getSafeStringList(root, "formattingRisks")

        val extractedSkills = getSafeSkills(root)
        val atsBreakdown = getSafeAtsBreakdown(root)
        val projectAnalysis = getSafeProjectAnalysis(root)
        val prioritizedSuggestions = getSafePrioritizedSuggestions(root)
        val parsingAccuracyDetails = getSafeParsingAccuracyDetails(root)

        return ResumeAnalysisResult(
            id = java.util.UUID.randomUUID().toString(),
            overallScore = overallScore,
            atsScore = atsScore,
            skillMatch = skillMatch,
            targetRole = targetRole,
            summary = summary,
            education = rawEducation,
            experience = cleanedExperience,
            strengths = strengths,
            weaknesses = weaknesses,
            atsBreakdown = atsBreakdown,
            extractedSkills = extractedSkills,
            missingSections = missingSections,
            formattingRisks = formattingRisks,
            projectAnalysis = projectAnalysis,
            prioritizedSuggestions = prioritizedSuggestions,
            parsingAccuracyDetails = parsingAccuracyDetails,
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            candidatePhone = candidatePhone,
            candidateLocation = candidateLocation,
            rawResumeText = rawResumeText
        )
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

    suspend fun matchJobDescription(
        jobDescriptionText: String,
        resumeData: ResumeAnalysisResult
    ): JdMatchResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is missing. Please provide a valid key from Google AI Studio.")
        }

        val candidateSkills = resumeData.extractedSkills?.map { it.name } ?: emptyList()
        val candidateEdu = resumeData.education ?: emptyList()
        val candidateExp = resumeData.experience ?: emptyList()
        val candidateProjects = resumeData.projectAnalysis?.map {
            "${it.name} (Tech: ${it.technologies?.joinToString(", ") ?: "N/A"})"
        } ?: emptyList()
        val candidateCerts = resumeData.certifications ?: emptyList()
        val candidateSummary = resumeData.summary ?: ""

        val prompt = """
            You are an expert, high-precision ATS (Applicant Tracking System) recruiter and Job Description matcher.
            Analyze the provided COMPANY JOB DESCRIPTION and compare it against the CANDIDATE'S CANONICAL RESUME DATA.

            ==============================
            CANDIDATE CANONICAL RESUME DATA (STRICT SOURCE OF TRUTH FOR CANDIDATE - DO NOT FABRICATE/INVENT SKILLS OR EXPERIENCE):
            - Candidate Name: ${resumeData.candidateName ?: "Candidate"}
            - Current/Target Role: ${resumeData.targetRole ?: "Software Engineer"}
            - Summary: $candidateSummary
            - Extracted Candidate Skills: ${if (candidateSkills.isEmpty()) "None extracted" else candidateSkills.joinToString(", ")}
            - Candidate Education: ${if (candidateEdu.isEmpty()) "None listed" else candidateEdu.joinToString(" | ")}
            - Candidate Experience: ${if (candidateExp.isEmpty()) "None (Fresher / Student)" else candidateExp.joinToString(" | ")}
            - Candidate Projects: ${if (candidateProjects.isEmpty()) "None" else candidateProjects.joinToString(" | ")}
            - Candidate Certifications: ${if (candidateCerts.isEmpty()) "None" else candidateCerts.joinToString(", ")}

            ==============================
            COMPANY JOB DESCRIPTION (SOURCE OF TRUTH FOR JOB REQUIREMENTS):
            $jobDescriptionText

            ==============================
            STRICT COMPARISON & EXTRACTION RULES:
            1. SINGLE SOURCE OF TRUTH:
               - The Candidate Resume Data above is the ONLY source of candidate capabilities. Do NOT assume a skill or experience exists unless explicitly stated.
               - The Job Description above is the ONLY source of role requirements. Do NOT invent requirements.
            2. JOB TITLE & SUMMARY:
               - Extract the target job title (e.g., 'Machine Learning Engineer', 'Android Developer', 'Data Analyst').
               - Extract the company name if explicitly present, else null.
               - Extract a concise 2-3 sentence summary of the role.
            3. SKILLS MATCHING:
               - 'matchedSkills': Explicit skills that appear in BOTH the candidate's resume and the JD requirements.
               - 'missingSkills': Critical/required skills listed in the JD that are NOT supported by the candidate's resume.
               - 'preferredSkillsMatched': Nice-to-have/preferred JD skills present in candidate resume.
               - 'preferredSkillsMissing': Nice-to-have/preferred JD skills absent in candidate resume.
               - STRICT CONSTRAINT: Do NOT mark a skill as matched merely because it is conceptually related (e.g., Git does NOT mean Docker; Java does NOT mean Spring Boot unless Spring is explicitly in resume).
            4. RESPONSIBILITIES:
               - Extract 4-6 key responsibilities from the JD.
            5. EXPERIENCE MATCHING:
               - Extract the COMPLETE experience requirements (e.g. '3+ years in Android development with Kotlin, Compose and Coroutines', 'Entry level / New graduate'). Do not truncate or abbreviate.
               - State candidate actual experience accurately.
               - 'experienceMatchStatus': MUST be one of 'Match', 'Partial Match', 'Gap', or 'Not Specified'.
               - Provide a clear 1-2 sentence explanation.
            6. EDUCATION MATCHING:
               - Extract the COMPLETE, UNABRIDGED education requirements exactly as stated in the job description (e.g., "Bachelor's degree in Computer Science, Information Technology, Software Engineering, or a related field", "Master's degree preferred"). Do NOT truncate, shorten, or cut off degree disciplines or requirements.
               - State candidate actual education accurately and completely.
               - 'educationMatchStatus': MUST be one of 'Match', 'Partial Match', or 'Not Required'.
               - Provide a clear explanation.
            7. CERTIFICATION MATCHING:
               - Extract JD required/preferred certifications (if none explicitly mentioned, state 'No certification requirement specified').
               - State candidate actual certifications COMPLETELY and INDEPENDENTLY from the JD requirements. List all certifications found in the candidate resume even if they do not match the JD.
               - 'certificationMatchStatus': MUST be one of 'Match', 'Missing', or 'Not Required'.
               - Provide a clear 1-2 sentence explanation.
            8. STRENGTHS & GAPS:
               - 'strengths': 3-5 specific, genuine strengths of the candidate directly relevant to this job description.
               - 'priorityGaps': 2-5 top priority missing competencies required for this role. Use concise skill names.
               - 'recommendedActions': 3-4 practical, constructive steps to bridge the gaps.
            9. SUGGESTED LEARNING COURSES:
               - 'suggestedCourses': For each priority gap, generate a high-quality, professional course recommendation.
               - COURSE TITLE RULES:
                 * Generate a concise, professional course title (2-8 words).
                 * Describe WHAT to learn, not the candidate's weakness.
                 * Avoid phrases like "Lack of...", "Candidate needs...", "Missing...", "Insufficient experience".
                 * Use attractive, professional titles as seen on Udemy or Coursera.
                 * Example: Instead of "Lack of cloud experience", use "Cloud Architecture Fundamentals".
                 * Never include candidate-specific years of experience in the title.
            10. DYNAMIC JOB MATCH SCORE:
               - Calculate a dynamic percentage score (0-100) strictly from the actual comparison:
                 * Required Skills (60% weight)
                 * Preferred Skills (15% weight)
                 * Experience Alignment (15% weight)
                 * Education Alignment (10% weight)
               - Never hardcode or return arbitrary fixed numbers.

            ==============================
            JSON OUTPUT SCHEMA (Valid RFC 8259 JSON ONLY):
            {
              "jobTitle": "string",
              "companyName": "string or null",
              "roleSummary": "string",
              "matchScore": 78,
              "matchedSkills": ["string"],
              "missingSkills": ["string"],
              "preferredSkillsMatched": ["string"],
              "preferredSkillsMissing": ["string"],
              "responsibilities": ["string"],
              "experienceRequirement": "string",
              "candidateExperience": "string",
              "experienceMatchStatus": "Match",
              "experienceMatchExplanation": "string",
              "educationRequirement": "string",
              "candidateEducation": "string",
              "educationMatchStatus": "Match",
              "educationMatchExplanation": "string",
              "certificationRequirement": "string",
              "candidateCertifications": "string",
              "certificationMatchStatus": "Not Required",
              "certificationMatchExplanation": "string",
              "strengths": ["string"],
              "priorityGaps": ["string"],
              "recommendedActions": ["string"],
              "suggestedCourses": [
                {
                  "skillName": "string",
                  "courseTitle": "string",
                  "description": "string",
                  "reason": "string"
                }
              ]
            }

            IMPORTANT: Return ONLY valid JSON.
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText)
            ?: throw Exception("AI response did not contain valid JSON JD matching data.")

        try {
            parseJdMatchJson(jsonString, jobDescriptionText, resumeData)
        } catch (e: Exception) {
            Log.e("GeminiService", "JD Match parsing error: ${e.message}", e)
            throw Exception("Failed to parse JD Match AI response: ${e.message}", e)
        }
    }

    fun parseJdMatchJson(
        jsonString: String,
        jobDescriptionText: String,
        resumeData: ResumeAnalysisResult
    ): JdMatchResult {
        val rootElement = try {
            JsonParser.parseString(jsonString)
        } catch (e: Exception) {
            val repaired = repairJson(jsonString)
            JsonParser.parseString(repaired)
        }

        if (!rootElement.isJsonObject) {
            throw Exception("Expected JSON object root for JD Match, but got ${rootElement.javaClass.simpleName}")
        }
        val root = rootElement.asJsonObject

        fun getSafeInt(obj: JsonObject, key: String, default: Int = 0): Int {
            val elem = obj.get(key) ?: return default
            if (elem.isJsonPrimitive) {
                val prim = elem.asJsonPrimitive
                if (prim.isNumber) return prim.asInt.coerceIn(0, 100)
                if (prim.isString) {
                    val clean = prim.asString.trim().replace("%", "")
                    return (clean.toIntOrNull() ?: clean.toDoubleOrNull()?.roundToInt() ?: default).coerceIn(0, 100)
                }
            }
            return default
        }

        fun getSafeString(obj: JsonObject, key: String): String? {
            val elem = obj.get(key) ?: return null
            if (elem.isJsonNull) return null
            if (elem.isJsonPrimitive) {
                return sanitizeField(elem.asString)
            }
            if (elem.isJsonObject) {
                val parts = elem.asJsonObject.entrySet().mapNotNull { entry ->
                    val v = entry.value
                    if (v.isJsonPrimitive) sanitizeField(v.asString) else null
                }
                return if (parts.isNotEmpty()) parts.joinToString(", ") else null
            }
            return null
        }

        fun getSafeStringList(obj: JsonObject, key: String): List<String> {
            val elem = obj.get(key) ?: return emptyList()
            if (elem.isJsonArray) {
                return elem.asJsonArray.mapNotNull { item ->
                    when {
                        item.isJsonPrimitive -> sanitizeField(item.asString)
                        item.isJsonObject -> {
                            val subParts = item.asJsonObject.entrySet().mapNotNull { entry ->
                                val v = entry.value
                                if (v.isJsonPrimitive) sanitizeField(v.asString) else null
                            }
                            if (subParts.isNotEmpty()) subParts.joinToString(" ") else null
                        }
                        else -> null
                    }
                }
            }
            if (elem.isJsonPrimitive) {
                val str = sanitizeField(elem.asString)
                return if (str != null) listOf(str) else emptyList()
            }
            return emptyList()
        }

        val jobTitle = getSafeString(root, "jobTitle") ?: resumeData.targetRole ?: "Target Role"
        val companyName = getSafeString(root, "companyName")
        val roleSummary = getSafeString(root, "roleSummary") ?: "Analysis of job description requirements versus candidate competencies."

        val matchedSkills = getSafeStringList(root, "matchedSkills")
        val missingSkills = getSafeStringList(root, "missingSkills")
        val preferredSkillsMatched = getSafeStringList(root, "preferredSkillsMatched")
        val preferredSkillsMissing = getSafeStringList(root, "preferredSkillsMissing")
        val responsibilities = getSafeStringList(root, "responsibilities")

        val experienceRequirement = getSafeString(root, "experienceRequirement") ?: "Not specified"
        val candidateExperience = getSafeString(root, "candidateExperience") ?: (if (resumeData.experience.isNullOrEmpty()) "Fresher / Student" else resumeData.experience.joinToString(" | "))
        val experienceMatchStatus = getSafeString(root, "experienceMatchStatus") ?: (if (resumeData.experience.isNullOrEmpty()) "Gap" else "Match")
        val experienceMatchExplanation = getSafeString(root, "experienceMatchExplanation") ?: ""

        val educationRequirement = getSafeString(root, "educationRequirement") ?: "Degree in relevant discipline"
        val candidateEducation = getSafeString(root, "candidateEducation") ?: (resumeData.education?.joinToString(" | ") ?: "Not provided")
        val educationMatchStatus = getSafeString(root, "educationMatchStatus") ?: (if (resumeData.education.isNullOrEmpty()) "Partial Match" else "Match")
        val educationMatchExplanation = getSafeString(root, "educationMatchExplanation") ?: ""

        val certificationRequirement = getSafeString(root, "certificationRequirement")?.let {
            if (it.equals("None", ignoreCase = true) || it.equals("Not specified", ignoreCase = true)) {
                "No certification requirement specified"
            } else it
        } ?: "No certification requirement specified"

        val rawCandidateCertifications = getSafeString(root, "candidateCertifications")?.trim()
        val candidateCertifications = when {
            !rawCandidateCertifications.isNullOrBlank() &&
            !rawCandidateCertifications.equals("null", ignoreCase = true) &&
            !rawCandidateCertifications.equals("[]", ignoreCase = true) &&
            !rawCandidateCertifications.equals("none", ignoreCase = true) &&
            !rawCandidateCertifications.equals("none listed", ignoreCase = true) &&
            !rawCandidateCertifications.equals("not specified", ignoreCase = true) &&
            !rawCandidateCertifications.equals("not provided", ignoreCase = true) &&
            !rawCandidateCertifications.contains("No certifications matching", ignoreCase = true) -> rawCandidateCertifications
            !resumeData.certifications.isNullOrEmpty() -> resumeData.certifications.joinToString(", ")
            else -> "None listed"
        }
        val certificationMatchStatus = getSafeString(root, "certificationMatchStatus") ?: "Not Required"
        val certificationMatchExplanation = getSafeString(root, "certificationMatchExplanation") ?: ""

        val strengths = getSafeStringList(root, "strengths")
        val priorityGaps = getSafeStringList(root, "priorityGaps")
        val recommendedActions = getSafeStringList(root, "recommendedActions")

        // Dynamic match score calculation fallback if AI missed score
        val calculatedFallbackScore: Int = run {
            val totalReq = matchedSkills.size + missingSkills.size
            val skillRatio = if (totalReq > 0) matchedSkills.size.toDouble() / totalReq else 0.75
            val expRatio = if (experienceMatchStatus.contains("Match", ignoreCase = true)) 1.0 else if (experienceMatchStatus.contains("Partial", ignoreCase = true)) 0.6 else 0.3
            val eduRatio = if (educationMatchStatus.contains("Match", ignoreCase = true) || educationMatchStatus.contains("Not Required", ignoreCase = true)) 1.0 else 0.5
            val prefTotal = preferredSkillsMatched.size + preferredSkillsMissing.size
            val prefRatio = if (prefTotal > 0) preferredSkillsMatched.size.toDouble() / prefTotal else 0.8
            ((skillRatio * 60) + (prefRatio * 15) + (expRatio * 15) + (eduRatio * 10)).roundToInt().coerceIn(10, 98)
        }

        val matchScore = getSafeInt(root, "matchScore", calculatedFallbackScore)

        // 1. Try to parse AI-generated structured course recommendations
        val aiCourses = mutableListOf<RecommendedCourse>()
        val suggestedArr = root.getAsJsonArray("suggestedCourses")
        if (suggestedArr != null && suggestedArr.isJsonArray) {
            suggestedArr.forEach { item ->
                if (item.isJsonObject) {
                    val cObj = item.asJsonObject
                    val skillName = getSafeString(cObj, "skillName") ?: ""
                    val courseTitle = getSafeString(cObj, "courseTitle") ?: ""
                    val description = getSafeString(cObj, "description") ?: ""
                    val reason = getSafeString(cObj, "reason") ?: ""
                    
                    if (skillName.isNotBlank() && courseTitle.isNotBlank()) {
                        // Use the AI title/desc/reason, but try to get a real provider/URL if it matches a hardcoded skill
                        val fallbackCourse = RoleSkillsData.getRecommendedFreeCourse(skillName, jobTitle, reason)
                        aiCourses.add(fallbackCourse.copy(
                            title = courseTitle,
                            description = description,
                            explanation = reason,
                            missingSkill = skillName
                        ))
                    }
                }
            }
        }

        // 2. Fallback / Enrichment: If AI didn't provide enough courses, or for all missing skills
        val matchedSkillSet = (matchedSkills + preferredSkillsMatched).map { it.trim().lowercase() }.toSet()
        val missingSkillsToLearn = (priorityGaps + missingSkills + preferredSkillsMissing)
            .map { it.trim() }
            .filter { skill ->
                skill.isNotBlank() && !matchedSkillSet.contains(skill.lowercase())
            }
            .distinctBy { it.lowercase() }

        val finalLearningResources = if (aiCourses.isNotEmpty()) {
            aiCourses.take(6)
        } else {
            missingSkillsToLearn.take(6).map { skill ->
                val cleanSkill = if (skill.length > 40) {
                    // Try to extract just the first few words if it looks like a sentence
                    skill.split(".")[0].split(",")[0].take(35)
                } else {
                    skill
                }
                val customExp = "Recommended because this competency is listed in the job description but was not identified in your resume."
                RoleSkillsData.getRecommendedFreeCourse(cleanSkill, jobTitle, customExp)
            }
        }

        return JdMatchResult(
            jobTitle = jobTitle,
            companyName = companyName,
            roleSummary = roleSummary,
            matchScore = matchScore,
            matchedSkills = matchedSkills,
            missingSkills = missingSkills,
            preferredSkillsMatched = preferredSkillsMatched,
            preferredSkillsMissing = preferredSkillsMissing,
            responsibilities = responsibilities,
            experienceRequirement = experienceRequirement,
            candidateExperience = candidateExperience,
            experienceMatchStatus = experienceMatchStatus,
            experienceMatchExplanation = experienceMatchExplanation,
            educationRequirement = educationRequirement,
            candidateEducation = candidateEducation,
            educationMatchStatus = educationMatchStatus,
            educationMatchExplanation = educationMatchExplanation,
            certificationRequirement = certificationRequirement,
            candidateCertifications = candidateCertifications,
            certificationMatchStatus = certificationMatchStatus,
            certificationMatchExplanation = certificationMatchExplanation,
            strengths = strengths,
            priorityGaps = priorityGaps,
            recommendedActions = recommendedActions,
            freeLearningResources = finalLearningResources
        )
    }

    suspend fun optimizeResume(
        jobDescription: String,
        resumeData: ResumeAnalysisResult
    ): ResumeOptimizationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is missing.")
        }

        val candidateSkills = resumeData.extractedSkills?.map { it.name } ?: emptyList()
        val candidateEdu = resumeData.education ?: emptyList()
        val candidateExp = resumeData.experience ?: emptyList()
        val candidateProjects = resumeData.projectAnalysis?.map {
            "${it.name}: ${it.strengths?.joinToString("; ") ?: ""}"
        } ?: emptyList()
        val candidateSummary = resumeData.summary ?: ""

        val prompt = """
            You are an expert AI Resume Optimizer. Your goal is to suggest minimal, targeted improvements to a candidate's resume to better align it with a specific Job Description (JD).
            
            STRICT RULES:
            1. MINIMAL CHANGE: Only suggest changes that are actually useful for alignment. Do NOT rewrite the entire resume.
            2. NO FABRICATION: Never invent experience, skills, technologies, certifications, or projects. Only suggest wording improvements IF AND ONLY IF the existing resume evidence supports it.
            3. TARGETED: Suggest specific wording changes (words, phrases, sentences, bullets).
            4. FORMAT: Provide results as structured JSON.
            
            5. EXACT ORIGINAL TEXT: The "originalText" field MUST contain the EXACT substring from the "rawResumeText" provided below. This is critical for locating the text in the document.
            
            CANDIDATE RESUME DATA (STRICT SOURCE OF TRUTH):
            - Name: ${resumeData.candidateName ?: "Candidate"}
            - Raw Resume Text: ${resumeData.rawResumeText ?: ""}
            - Summary: $candidateSummary
            - Experience: ${candidateExp.joinToString(" | ")}
            - Education: ${candidateEdu.joinToString(" | ")}
            - Skills: ${candidateSkills.joinToString(", ")}
            - Projects: ${candidateProjects.joinToString(" | ")}
            
            JOB DESCRIPTION:
            $jobDescription
            
            Identify:
            1. Keyword Opportunities: Terms in JD that could be better represented using existing resume evidence.
            2. Content Improvements: Targeted wording improvements for summary, experience, or projects.
            3. Skill Gaps: Genuine requirements missing from resume (list them only, do NOT suggest changes for them).
            
            JSON OUTPUT SCHEMA:
            {
              "jdTitle": "string",
              "companyName": "string or null",
              "skillGaps": ["string"],
              "keywordOpportunities": ["string"],
              "suggestions": [
                {
                  "section": "SUMMARY | EXPERIENCE | PROJECTS | SKILLS | CERTIFICATION",
                  "originalText": "exact text from resume to be replaced",
                  "suggestedText": "improved targeted text",
                  "changeType": "WORD | PHRASE | SENTENCE | BULLET | SKILL | PROJECT | EXPERIENCE | SUMMARY | CERTIFICATION",
                  "reason": "why this change improves alignment",
                  "relatedKeyword": "string",
                  "priority": "CRITICAL | HIGH | MEDIUM | LOW",
                  "confidence": 100,
                  "resumeEvidence": "string",
                  "supportedByResume": true
                }
              ]
            }
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText) ?: throw Exception("Failed to get optimization suggestions.")
        
        parseOptimizationJson(jsonString)
    }

    private fun parseOptimizationJson(jsonString: String): ResumeOptimizationResult {
        val root = JsonParser.parseString(jsonString).asJsonObject
        
        val jdTitle = root.get("jdTitle")?.asString ?: "Target Role"
        val companyName = if (root.has("companyName") && !root.get("companyName").isJsonNull) root.get("companyName").asString else null
        
        val skillGaps = mutableListOf<String>()
        root.getAsJsonArray("skillGaps")?.forEach { skillGaps.add(it.asString) }
        
        val keywordOpportunities = mutableListOf<String>()
        root.getAsJsonArray("keywordOpportunities")?.forEach { keywordOpportunities.add(it.asString) }
        
        val suggestions = mutableListOf<OptimizationSuggestion>()
        root.getAsJsonArray("suggestions")?.forEach { 
            val sObj = it.asJsonObject
            suggestions.add(OptimizationSuggestion(
                section = sObj.get("section").asString,
                originalText = sObj.get("originalText").asString,
                suggestedText = sObj.get("suggestedText").asString,
                changeType = try { ChangeType.valueOf(sObj.get("changeType").asString) } catch (_: Exception) { ChangeType.OTHER },
                reason = sObj.get("reason").asString,
                relatedKeyword = if (sObj.has("relatedKeyword") && !sObj.get("relatedKeyword").isJsonNull) sObj.get("relatedKeyword").asString else null,
                priority = try { SuggestionPriority.valueOf(sObj.get("priority").asString) } catch (_: Exception) { SuggestionPriority.MEDIUM },
                confidence = if (sObj.has("confidence")) sObj.get("confidence").asInt else 100,
                resumeEvidence = if (sObj.has("resumeEvidence")) sObj.get("resumeEvidence").asString else null,
                supportedByResume = if (sObj.has("supportedByResume")) sObj.get("supportedByResume").asBoolean else true
            ))
        }
        
        return ResumeOptimizationResult(jdTitle, companyName, suggestions, skillGaps, keywordOpportunities)
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
                    
                    if (errorMsg.contains("503") || errorMsg.contains("429") || errorMsg.contains("unavailable", ignoreCase = true)) {
                        if (attempt < maxAttempts) {
                            delay(400L * attempt)
                        }
                    } else if (errorMsg.contains("404") || errorMsg.contains("400")) {
                        break
                    }
                }
            }
        }

        throw lastException ?: Exception("Network error: Could not reach Gemini AI. Please check your internet connection.")
    }

    private fun callGeminiApi(model: String, promptText: String, isJson: Boolean): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val requestBodyMap = mutableMapOf<String, Any>(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to promptText))))
        )
        if (isJson) {
            requestBodyMap["generationConfig"] = mapOf(
                "response_mime_type" to "application/json",
                "maxOutputTokens" to 8192,
                "temperature" to 0.1
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
        cleaned = cleaned.replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("\\s*```\\s*$"), "")
        cleaned = cleaned.trim()

        val start = cleaned.indexOf("{")
        if (start == -1) return null

        val end = cleaned.lastIndexOf("}")
        val candidateJson = if (end > start) cleaned.substring(start, end + 1) else cleaned.substring(start)
        return repairJson(candidateJson)
    }

    private fun repairJson(rawJson: String): String {
        var repaired = rawJson.replace(Regex(",\\s*([}\\]])"), "$1").trim()

        val stack = mutableListOf<Char>()
        var inString = false
        var isEscaped = false

        for (ch in repaired) {
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (ch == '\\') {
                isEscaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                when (ch) {
                    '{' -> stack.add('}')
                    '[' -> stack.add(']')
                    '}' -> if (stack.isNotEmpty() && stack.last() == '}') stack.removeAt(stack.size - 1)
                    ']' -> if (stack.isNotEmpty() && stack.last() == ']') stack.removeAt(stack.size - 1)
                }
            }
        }

        if (inString) {
            repaired += "\""
        }

        while (stack.isNotEmpty()) {
            repaired += stack.removeAt(stack.size - 1)
        }

        return repaired
    }

    private data class GeminiApiResponse(val candidates: List<Candidate>?)
    private data class Candidate(val content: Content?, val finishReason: String? = null)
    private data class Content(val parts: List<Part>?, val role: String? = null)
    private data class Part(val text: String?, val thought: Boolean? = null)
}
