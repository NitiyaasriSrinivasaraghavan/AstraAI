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
    // API Key read from BuildConfig
    private val apiKey = BuildConfig.GEMINI_API_KEY

    init {
        // Safe diagnostic log to confirm key presence without exposing it
        if (apiKey.isBlank()) {
            Log.e("GeminiService", "CRITICAL: Gemini API Key is empty in BuildConfig!")
        } else {
            Log.d("GeminiService", "Gemini API Key is configured (length: ${apiKey.length})")
        }
    }

    // Prioritized model cascade with confirmed high-performance and available endpoints
    private val modelCascade = listOf(
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
            You are an expert, high-precision ATS resume analysis and information extraction system.
            The extracted text below is the EXCLUSIVE source of truth.
            
            EXTRACTED RESUME TEXT:
            $resumeText
            
            TARGET ROLE:
            $targetRole
            
            STRICT EXTRACTION & CLASSIFICATION RULES:
            1. SINGLE SOURCE OF TRUTH, SECTION BOUNDARIES & NO HALLUCINATION:
               - ONLY extract facts actually present in the resume. Respect the actual semantic section in which information appears.
               - Do not move content between Education, Certifications, Skills, Projects, Work Experience, or Summary simply because another field is empty.
               - Never fill an empty field with unrelated content from other sections. If a field or section is not present in the resume, return null or an empty list [] - never invent or hallucinate information.

            2. CONTACT INFORMATION & LOCATION:
               - candidateName: Extract the candidate's actual personal name ONLY from the resume header/name area.
                 * STRICTLY FORBIDDEN: NEVER use education qualifications, degree names (such as 'Bachelor of Engineering', 'Bachelor of Technology', 'B.E.', 'B.Tech', 'M.E.', 'M.Tech', 'B.Sc', 'M.Sc', 'MCA', 'BCA', 'MBA', etc.), department/course names (such as 'Computer Science and Engineering', 'Information Technology', etc.), college/university/school names, job titles (such as 'Software Engineer', 'Android Developer', etc.), or section headings as the candidate's name.
                 * If the resume contains a name with an initial (e.g. 'Tharshika A', 'A. Tharshika', 'S. Priya', 'Karthik R'), preserve the name and initial exactly as written. Do NOT concatenate unrelated characters or merge initials into words (do NOT change 'Tharshika A' into 'Tharshikaa').
                 * Never infer or construct the candidate name from email address (e.g., do not turn 'tharshikaa2005@gmail.com' into 'Tharshikaa').
                 * If no valid candidate name is present in the resume header, return null.
               - Extract candidateEmail, candidatePhone, and candidateLocation.
               - Detect candidateLocation from the contact header block (e.g., 'City, State', 'City, Country', 'City - Pincode', under name or beside email/phone). Do not confuse college or company locations with the candidate's location. If not present in the resume, return null.

            3. SUMMARY / OBJECTIVE (Semantic Classification):
               - Extract the professional summary, profile statement, career objective, career summary, executive summary, overview, or introductory professional paragraph based on content and semantic meaning.
               - Semantically classify equivalent headings such as 'Professional Summary', 'Summary', 'Career Objective', 'Objective', 'Career Summary', 'Professional Profile', 'Profile', 'Profile Summary', 'Executive Summary', 'About Me', 'Career Profile', 'Personal Profile', 'Overview', or any introductory career paragraph into the 'summary' field.
               - If no summary/objective paragraph exists in the resume, return null. Never invent one.
            
            4. WORK EXPERIENCE (Semantic Classification & Strict Category Isolation):
               - 'experience' MUST contain ONLY genuine employment or internship entries (Company/Organization, Job/Internship Title, Dates/Duration, Responsibilities/Achievements).
               - Semantically classify equivalent headings such as 'Work Experience', 'Professional Experience', 'Employment History', 'Career History', 'Work History', 'Experience', 'Internships', 'Industrial Experience'.
               - If the resume contains only internships and no full-time jobs, include only those internships.
               - STRICTLY FORBIDDEN: NEVER put Summary text, Education degrees, Certifications, Skills, Locations, or Coursework into 'experience'.
               - If the candidate is a fresher or student with NO work experience or internships, 'experience' MUST be an empty list []. Never fill it with other sections.
            
            5. EDUCATION (Strict Formal Academic Qualifications & Preserve ALL Tiers):
               - Semantically classify equivalent headings such as 'Education', 'Academic Background', 'Academic Qualifications', 'Educational Qualifications', 'Educational Details'.
               - Extract ONLY formal academic qualifications explicitly presented as education in the resume (e.g., 10th/Secondary/SSLC, 12th/HSC/Intermediate, Diploma/Polytechnic, Undergraduate Degree B.Tech/B.E./B.Sc/BCA/B.Com/BBA, Postgraduate Degree M.Tech/M.E./M.Sc/MCA/MBA, Ph.D/Doctorate).
               - STRICTLY FORBIDDEN: Do NOT classify certifications, NPTEL courses, online courses (Coursera, Udemy, edX, LinkedIn Learning, etc.), workshops, training programs, bootcamps, or professional certificates as formal education. If no formal education is present in the resume, return an empty list [] for education. Never create an Education entry from information that is not explicitly supported by the resume.
               - 'education' MUST preserve ALL formal educational qualifications present in the resume completely without truncation:
                 * 10th / Secondary School / SSLC / Matriculation / CBSE Class 10 / ICSE Class 10
                 * 12th / Higher Secondary / HSC / Intermediate / CBSE Class 12 / ISC Class 12 / Pre-University
                 * Diploma / Polytechnic
                 * Undergraduate Degree (B.Tech, B.E., B.Sc., BCA, B.Com, etc.)
                 * Postgraduate Degree (M.Tech, M.S., MCA, MBA, etc.)
               - Include Degree/Standard, School/College/University, Location, Graduation Year/Dates, and CGPA/Percentage for each qualification.
               - If the resume contains 10th, 12th, and college, ALL THREE must be extracted as separate items in the 'education' array.
            
            6. PROJECTS (Semantic Classification & Strict Project Validation):
               - 'projectAnalysis' MUST contain ONLY genuine projects (academic, personal, capstone, client, or open-source) with actual project titles, descriptions, and technologies used.
               - Extract only genuine projects explicitly represented in the resume. Respect section boundaries.
               - Do NOT convert skills, certifications, education, training, work responsibilities or unrelated paragraphs into projects.
               - Do NOT split a single project's subheadings (such as Frontend, Backend, Tools, Database, Description, Role, Environment, Outcome) into multiple separate projects. Group all subheadings and metadata of a project under that single project entry.
               - Preserve legitimate technical and nontechnical/community projects when explicitly presented as projects.
               - If the resume does NOT contain a Projects section or project entries, return 'projectAnalysis': []. NEVER fabricate projects from other text.
               - Certifications MUST remain under certifications, NEVER in projects.
            
            7. SKILLS (Extract ALL Explicit Skills without Artificial Limits):
               - Semantically classify headings such as 'Technical Skills', 'Skills', 'Technical Expertise', 'Core Skills', 'Technical Competencies', 'Technologies', 'Tech Stack', 'Tools & Technologies' into 'extractedSkills'.
               - Extract ALL programming languages, frameworks, developer tools, and technologies explicitly listed in the resume.
               - DO NOT impose any artificial limit (e.g. do NOT limit to 3 skills; extract 10, 15, 20+ if explicitly listed).
               - Do NOT invent skills not present in the resume.
               - Keep Certifications (e.g., 'Certifications', 'Certificates', 'Licenses & Certifications') separate from Skills.

            8. PERSONAL & FAMILY INFORMATION (Strict Isolation):
               - Fields like Father's Name, DOB, Marital Status, Nationality, and Permanent Address must be treated as personal info.
               - NEVER include these details in Education, Work Experience, Projects, or Professional Summary.
            
            JSON OUTPUT SCHEMA (Valid RFC 8259 JSON ONLY):
            {
              "overallScore": 0, 
              "atsScore": 0,
              "skillMatch": 0,
              "candidateName": "string or null",
              "candidateEmail": "string or null",
              "candidatePhone": "string or null",
              "candidateLocation": "string or null",
              "personalInfo": ["string"],
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

        val candidateName = getSafeString(root, "candidateName")?.takeIf {
            !ResumeParser.isDegreeOrEducationTitle(it)
        }
        val candidateEmail = getSafeString(root, "candidateEmail")
        val candidatePhone = getSafeString(root, "candidatePhone")
        val candidateLocation = getSafeString(root, "candidateLocation")
        val personalInfo = getSafeStringList(root, "personalInfo")
        val summary = getSafeString(root, "summary")
            ?: getSafeString(root, "careerObjective")
            ?: getSafeString(root, "objective")
            ?: getSafeString(root, "professionalSummary")
            ?: getSafeString(root, "profile")
            ?: getSafeString(root, "careerProfile")
            ?: getSafeString(root, "aboutMe")
            ?: ""

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
            personalInfo = personalInfo,
            rawResumeText = rawResumeText
        )
    }

    private fun sanitizeAnalysisResult(result: ResumeAnalysisResult): ResumeAnalysisResult {
        return result.copy(
            candidateName = sanitizeField(result.candidateName)?.takeIf { !ResumeParser.isDegreeOrEducationTitle(it) },
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

    suspend fun getAiAssistantResponse(
        query: String,
        history: List<ChatMessage>,
        userContext: String
    ): String = withContext(Dispatchers.IO) {
        val systemInstructions = """
            You are Nova, a highly intelligent, natural, and context-aware AI Career Assistant for the NoviQ app.
            
            USER CONTEXT:
            $userContext
            
            YOUR CORE PRINCIPLES:
            1. BE NATURAL & CONVERSATIONAL: Communicate like a real person. Be professional, empathetic, and encouraging. Avoid robotic "Input accepted" style responses.
            2. TYPO TOLERANCE: Ignore spelling mistakes and poor grammar (e.g., "scor" -> "score"). Focus 100% on the user's intent.
            3. CONTEXT AWARENESS: Remember previous messages. Resolve references like "it", "them", "why is it low?" using history and context.
            4. ACCURACY: Use ONLY the provided 'USER CONTEXT'. Never hallucinate user data (jobs, skills, companies). If info is missing, say you don't have it yet.
            5. CONDITIONAL ADVICE: If suggesting a missing skill, always say "If you have experience with [Skill], add it..." or "Consider learning [Skill] to bridge the gap...".
            6. DYNAMIC LENGTH: Concise for simple questions ("What is ATS?"), detailed and structured for career strategy advice.
            7. FORMATTING: Use clean, plain-text formatting. ABSOLUTELY NO raw markdown like '###', '**', or '---'. Use bullet points with simple characters like '•' if needed.
            
            CURRENT CONVERSATION HISTORY:
            ${history.joinToString("\n") { if (it.isFromUser) "USER: ${it.text}" else "NOVA: ${it.text}" }}
            
            CURRENT USER QUERY: $query
        """.trimIndent()

        try {
            val responseText = executeWithFallbackAndRetry(systemInstructions, isJson = false)
            // Strip any accidental markdown formatting to ensure clean plain-text display as requested
            responseText?.replace(Regex("[#*\\-_]{2,}"), "")
                ?.replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
                ?.trim() ?: "I'm sorry, I couldn't process that right now. Could you rephrase your question?"
        } catch (e: Exception) {
            "I encountered a temporary connection issue. Please try your question again."
        }
    }

    suspend fun generateInterviewQuestions(
        targetRole: String,
        resumeData: ResumeAnalysisResult,
        jobDescription: String?
    ): List<InterviewQuestion> = withContext(Dispatchers.IO) {
        val candidateSkills = resumeData.extractedSkills?.map { it.name } ?: emptyList()
        val candidateExp = resumeData.experience ?: emptyList()
        val candidateProjects = resumeData.projectAnalysis?.map { it.name } ?: emptyList()

        val prompt = """
            You are an expert technical interviewer for NoviQ. Generate 5 personalized interview questions for a candidate.
            
            CONTEXT:
            - Target Role: $targetRole
            - Resume Skills: ${candidateSkills.joinToString(", ")}
            - Resume Experience: ${candidateExp.joinToString(" | ")}
            - Resume Projects: ${candidateProjects.joinToString(", ")}
            - Job Description: ${jobDescription ?: "Not provided"}
            
            QUESTION TYPES TO COVER:
            1. TECHNICAL: Based on skills and JD requirements.
            2. BEHAVIORAL: Based on experience and soft skills.
            3. PROJECT_BASED: Based on specific projects in the resume.
            4. SITUATIONAL: "What would you do if..." scenarios related to the role.
            
            RULES:
            - Ground questions in actual resume content. Do not invent experience.
            - Provide professional model guidance on what a strong answer should cover.
            - Return ONLY valid JSON.
            
            JSON OUTPUT SCHEMA (MUST BE A RAW ARRAY OF OBJECTS):
            [
              {
                "text": "string (the question text)",
                "type": "TECHNICAL | BEHAVIORAL | PROJECT_BASED | SITUATIONAL",
                "category": "string (e.g. Kotlin, Soft Skills, System Design)",
                "modelGuidance": "string (professional tips for answering)"
              }
            ]
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        Log.d("INTERVIEW_DEBUG", "INTERVIEW_GEMINI_RAW_RESPONSE: $responseText")
        
        val jsonString = extractJson(responseText)
        Log.d("INTERVIEW_DEBUG", "INTERVIEW_GEMINI_CLEANED_RESPONSE: $jsonString")
        
        if (jsonString == null) {
            throw Exception("AI response failed to provide valid JSON. Response starts with: ${responseText?.take(50)}")
        }
        
        try {
            parseInterviewQuestionsJson(jsonString)
        } catch (e: Exception) {
            Log.e("GeminiService", "Interview parsing error: ${e.message}. Raw: $jsonString")
            throw Exception("Failed to parse interview questions. Response starts with: ${jsonString.take(50)}")
        }
    }

    private fun parseInterviewQuestionsJson(jsonString: String): List<InterviewQuestion> {
        val element = try {
            JsonParser.parseString(jsonString)
        } catch (e: Exception) {
            val repaired = repairJson(jsonString)
            JsonParser.parseString(repaired)
        }

        val jsonArray = when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject -> {
                val obj = element.asJsonObject
                // Try to find any array field (e.g., "questions", "data", "items")
                obj.entrySet().firstOrNull { it.value.isJsonArray }?.value?.asJsonArray
                    ?: throw Exception("JSON object does not contain an array of questions.")
            }
            else -> throw Exception("Unexpected JSON format: expected array or object.")
        }

        return jsonArray.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val qObj = item.asJsonObject
            
            // Highly defensive key extraction (handles question, text, content, and snake_case variations)
            val text = (qObj.get("text") ?: qObj.get("question") ?: qObj.get("questionText") ?: qObj.get("content") ?: qObj.get("question_text"))?.asString 
                ?: return@mapNotNull null
                
            val typeStr = (qObj.get("type") ?: qObj.get("questionType") ?: qObj.get("kind") ?: qObj.get("question_type"))?.asString ?: "TECHNICAL"
            val category = (qObj.get("category") ?: qObj.get("topic") ?: qObj.get("subject") ?: qObj.get("question_category"))?.asString ?: typeStr
            val modelGuidance = (qObj.get("modelGuidance") ?: qObj.get("guidance") ?: qObj.get("tips") ?: qObj.get("suggestedAnswer") ?: qObj.get("model_guidance"))?.asString 
                ?: "Focus on providing a structured, evidence-based response."

            InterviewQuestion(
                text = text,
                type = when {
                    typeStr.contains("TECHNICAL", ignoreCase = true) -> InterviewQuestionType.TECHNICAL
                    typeStr.contains("BEHAVIORAL", ignoreCase = true) -> InterviewQuestionType.BEHAVIORAL
                    typeStr.contains("PROJECT", ignoreCase = true) -> InterviewQuestionType.PROJECT_BASED
                    typeStr.contains("SITUATIONAL", ignoreCase = true) -> InterviewQuestionType.SITUATIONAL
                    else -> InterviewQuestionType.TECHNICAL
                },
                category = category,
                modelGuidance = modelGuidance
            )
        }
    }

    suspend fun generateDynamicOpeningQuestion(
        targetRole: String,
        resumeData: ResumeAnalysisResult?
    ): InterviewQuestion = withContext(Dispatchers.IO) {
        val candidateSkills = resumeData?.extractedSkills?.map { it.name } ?: emptyList()
        val candidateExp = resumeData?.experience ?: emptyList()
        val candidateProjects = resumeData?.projectAnalysis?.map { it.name } ?: emptyList()
        val candidateEducation = resumeData?.education ?: emptyList()
        val candidateCerts = resumeData?.certifications ?: emptyList()

        val prompt = """
            You are a real-world senior technical hiring manager conducting a mock interview for a candidate applying for the position of '$targetRole'.
            
            CANDIDATE PROFILE:
            - Target Role: $targetRole
            - Skills: ${candidateSkills.joinToString(", ").ifBlank { "Not specified" }}
            - Projects: ${candidateProjects.joinToString(", ").ifBlank { "Not specified" }}
            - Experience: ${candidateExp.joinToString(" | ").ifBlank { "Not specified" }}
            - Education: ${candidateEducation.joinToString(", ").ifBlank { "Not specified" }}
            - Certifications: ${candidateCerts.joinToString(", ").ifBlank { "Not specified" }}
            - Summary: ${resumeData?.summary ?: "Not provided"}

            REQUIREMENTS:
            1. Generate the FIRST interview question tailored directly to the candidate's background and '$targetRole'.
            2. If the candidate has specific projects or technologies in their resume, ask an insightful question exploring their technical approach, architectural choices, or trade-offs on those specific items.
            3. If candidate details are missing, formulate an engaging opening question exploring their experience with core engineering competencies in '$targetRole'.
            4. Do NOT ask generic textbook questions like 'What is Android?' or 'What is Java?'.
            5. Return ONLY a valid JSON object.

            JSON OUTPUT SCHEMA:
            {
              "text": "string (the question text)",
              "category": "Introduction | Resume-based | Technical | Project-based | Problem-solving | Behavioural | Role-specific",
              "difficulty": "Entry | Mid-Level | Senior | Advanced",
              "expectedTopic": "string",
              "modelGuidance": "string (brief guidance on what a strong answer should demonstrate)"
            }
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText) ?: throw Exception("Failed to generate opening question")

        val obj = JsonParser.parseString(jsonString).asJsonObject
        val text = (obj.get("text") ?: obj.get("question"))?.asString ?: throw Exception("Missing question text")
        val category = obj.get("category")?.asString ?: "Resume-based"
        val difficulty = obj.get("difficulty")?.asString ?: "Mid-Level"
        val expectedTopic = obj.get("expectedTopic")?.asString ?: "Project Architecture & Technical Implementation"
        val modelGuidance = obj.get("modelGuidance")?.asString ?: "Focus on concrete architecture choices, challenges faced, and results."

        InterviewQuestion(
            text = text,
            type = mapCategoryToType(category),
            category = category,
            difficulty = difficulty,
            expectedTopic = expectedTopic,
            questionNumber = 1,
            modelGuidance = modelGuidance
        )
    }

    suspend fun evaluateAnswerAndGenerateAdaptiveQuestion(
        targetRole: String,
        resumeData: ResumeAnalysisResult?,
        questionNumber: Int,
        currentQuestion: InterviewQuestion,
        candidateAnswer: String,
        conversationHistory: List<InterviewExchange>
    ): AdaptiveInterviewResponse = withContext(Dispatchers.IO) {
        val candidateSkills = resumeData?.extractedSkills?.map { it.name } ?: emptyList()
        val candidateProjects = resumeData?.projectAnalysis?.map { it.name } ?: emptyList()
        val candidateExp = resumeData?.experience ?: emptyList()

        val historySummary = conversationHistory.takeLast(4).joinToString("\n\n") { exchange ->
            "Q (${exchange.question.category}): ${exchange.question.text}\nA: ${exchange.answer}"
        }

        val prompt = """
            You are a professional technical interviewer conducting an adaptive mock interview for a '$targetRole' candidate.
            
            CANDIDATE PROFILE:
            - Role: $targetRole
            - Skills: ${candidateSkills.joinToString(", ")}
            - Projects: ${candidateProjects.joinToString(", ")}
            - Experience: ${candidateExp.joinToString(" | ")}

            PREVIOUS EXCHANGES:
            ${historySummary.ifBlank { "None (this is the first answer)" }}

            CURRENT QUESTION (#$questionNumber):
            Category: ${currentQuestion.category}
            Question: ${currentQuestion.text}

            CANDIDATE'S ANSWER:
            $candidateAnswer

            INSTRUCTIONS:
            1. EVALUATE the candidate's answer internally:
               - relevance, clarity, completeness, technical accuracy, strengths, weaknesses, and potential improvements.
            2. ADAPTIVE QUESTIONING:
               - If the candidate gave a strong answer or mentioned a specific library/design decision/project detail, formulate a deeper follow-up question.
               - If the answer was vague or incomplete, ask a clarifying question on that concept.
               - If the topic was adequately explored, seamlessly pivot to the next competency area for '$targetRole' (e.g. system design, concurrency, testing, behavioral, performance optimization).
            3. SPOKEN INTERVIEWER RESPONSE:
               - Provide a natural conversational response in 'interviewerSpokenResponse'.
               - Briefly acknowledge the candidate's point (e.g. 'That makes sense regarding...', 'You mentioned X in your answer...').
               - Avoid over-praising or repetitive enthusiastic greetings. Maintain a natural, professional interview demeanor.
               - Transition immediately into the next question.
               - ABSOLUTELY NO raw markdown like '###', '**', or raw JSON in 'interviewerSpokenResponse'.
            4. Return ONLY a valid JSON object.

            JSON OUTPUT SCHEMA:
            {
              "interviewerSpokenResponse": "string (Brief acknowledgement + natural transition + next question)",
              "nextQuestion": {
                "text": "string (the standalone text of the next question)",
                "category": "Resume-based | Technical | Project-based | Problem-solving | Behavioural | Role-specific",
                "difficulty": "Entry | Mid-Level | Senior | Advanced",
                "expectedTopic": "string",
                "modelGuidance": "string",
                "isFollowUp": boolean
              },
              "evaluation": {
                "relevance": "string",
                "clarity": "string",
                "completeness": "string",
                "technicalUnderstanding": "string",
                "strengths": ["string"],
                "weaknesses": ["string"],
                "improvements": ["string"]
              }
            }
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText) ?: throw Exception("Failed to generate adaptive question")

        parseAdaptiveResponseJson(jsonString, questionNumber + 1, currentQuestion.id)
    }

    private fun mapCategoryToType(category: String): InterviewQuestionType {
        val lower = category.lowercase()
        return when {
            lower.contains("project") -> InterviewQuestionType.PROJECT_BASED
            lower.contains("behavior") || lower.contains("behaviour") -> InterviewQuestionType.BEHAVIORAL
            lower.contains("problem") -> InterviewQuestionType.PROBLEM_SOLVING
            lower.contains("resume") -> InterviewQuestionType.RESUME_BASED
            lower.contains("intro") -> InterviewQuestionType.INTRODUCTION
            lower.contains("role") -> InterviewQuestionType.ROLE_SPECIFIC
            lower.contains("situation") -> InterviewQuestionType.SITUATIONAL
            else -> InterviewQuestionType.TECHNICAL
        }
    }

    private fun parseAdaptiveResponseJson(jsonString: String, nextQuestionNumber: Int, parentQuestionId: String?): AdaptiveInterviewResponse {
        val element = try {
            JsonParser.parseString(jsonString)
        } catch (e: Exception) {
            val repaired = repairJson(jsonString)
            JsonParser.parseString(repaired)
        }

        val obj = element.asJsonObject
        val spoken = (obj.get("interviewerSpokenResponse") ?: obj.get("spokenResponse") ?: obj.get("response"))?.asString
            ?: "Thank you for your response. Let's move to the next question."
        
        val cleanSpoken = spoken.replace(Regex("[#*\\-_`]{2,}"), "").trim()

        val nextQObj = if (obj.has("nextQuestion") && obj.get("nextQuestion").isJsonObject) obj.getAsJsonObject("nextQuestion") else JsonObject()
        val nextText = (nextQObj.get("text") ?: nextQObj.get("question"))?.asString
            ?: "Could you describe another significant technical challenge you faced in your recent work?"
        val nextCategory = (nextQObj.get("category") ?: nextQObj.get("type"))?.asString ?: "Technical"
        val nextDifficulty = nextQObj.get("difficulty")?.asString ?: "Mid-Level"
        val nextExpectedTopic = nextQObj.get("expectedTopic")?.asString ?: nextCategory
        val nextGuidance = nextQObj.get("modelGuidance")?.asString ?: "Provide a structured, evidence-based answer."
        val isFollowUp = nextQObj.get("isFollowUp")?.asBoolean ?: false

        val evalObj = if (obj.has("evaluation") && obj.get("evaluation").isJsonObject) obj.getAsJsonObject("evaluation") else JsonObject()
        val relevance = evalObj.get("relevance")?.asString ?: "Good relevance to the question."
        val clarity = evalObj.get("clarity")?.asString ?: "Clear explanation."
        val completeness = evalObj.get("completeness")?.asString ?: "Addressed key points."
        val techUnderstanding = evalObj.get("technicalUnderstanding")?.asString ?: "Solid technical understanding demonstrated."
        val strengths = getSafeStringListFromJson(evalObj, "strengths")
        val weaknesses = getSafeStringListFromJson(evalObj, "weaknesses")
        val improvements = getSafeStringListFromJson(evalObj, "improvements")

        val nextQuestion = InterviewQuestion(
            text = nextText,
            type = mapCategoryToType(nextCategory),
            category = nextCategory,
            difficulty = nextDifficulty,
            expectedTopic = nextExpectedTopic,
            questionNumber = nextQuestionNumber,
            modelGuidance = nextGuidance,
            parentQuestionId = parentQuestionId,
            isFollowUp = isFollowUp
        )

        val evaluation = InterviewEvaluation(
            questionId = parentQuestionId ?: "",
            relevance = relevance,
            clarity = clarity,
            completeness = completeness,
            technicalUnderstanding = techUnderstanding,
            strengths = strengths,
            weaknesses = weaknesses,
            improvements = improvements
        )

        return AdaptiveInterviewResponse(
            interviewerSpokenResponse = cleanSpoken,
            nextQuestion = nextQuestion,
            evaluation = evaluation
        )
    }

    suspend fun generateFinalInterviewReport(
        questions: List<InterviewQuestion>,
        answers: Map<String, String>,
        targetRole: String,
        resumeData: ResumeAnalysisResult
    ): InterviewPerformanceSummary = withContext(Dispatchers.IO) {
        val sessionData = questions.joinToString("\n\n") { q ->
            "QUESTION (${q.type}): ${q.text}\nCANDIDATE ANSWER: ${answers[q.id] ?: "No answer provided"}"
        }

        val prompt = """
            Analyze the candidate's interview session for the position of $targetRole.
            
            CANDIDATE PROFILE SUMMARY:
            ${resumeData.summary}
            
            INTERVIEW SESSION DATA:
            $sessionData
            
            TASKS:
            1. Evaluate overall performance across all 5 answers.
            2. Identify 3-4 Key Strengths.
            3. Identify 2-3 Main Weaknesses.
            4. Provide specific "What to Improve" points (qualitative only).
            5. Provide a concise Overall Feedback observation.
            
            STRICT RULES:
            - NO NUMERICAL SCORES, percentages, or marks.
            - Provide qualitative, constructive feedback only.
            - Ensure "What to Improve" focuses on actionable communication and technical clarity.
            
            JSON OUTPUT SCHEMA:
            {
              "overallPerformance": "string (qualitative executive summary)",
              "keyStrengths": ["string"],
              "mainWeaknesses": ["string"],
              "recurringWeaknesses": ["string"],
              "improvementSuggestions": ["string"],
              "areasForPreparation": ["string"]
            }
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText) ?: throw Exception("Failed to generate final report.")
        
        gson.fromJson(jsonString, InterviewPerformanceSummary::class.java)
    }

    suspend fun generateTechnicalMcqs(
        targetRole: String,
        resumeData: ResumeAnalysisResult,
        jobDescription: String?
    ): List<TechnicalMcq> = withContext(Dispatchers.IO) {
        val candidateSkills = resumeData.extractedSkills?.map { it.name } ?: emptyList()
        val candidateProjects = resumeData.projectAnalysis?.map { it.name } ?: emptyList()

        val prompt = """
            You are an expert technical recruiter at NoviQ. Generate EXACTLY 10 technical multiple-choice questions (MCQs) for the role of $targetRole.
            
            CONTEXT:
            - Target Role: $targetRole
            - Candidate Skills: ${candidateSkills.joinToString(", ")}
            - Candidate Projects: ${candidateProjects.joinToString(", ")}
            - Job Description: ${jobDescription ?: "Not provided"}
            
            DIFFICULTY DISTRIBUTION:
            - 3 Easy, 4 Medium, 3 Difficult.
            
            RULES:
            - Use the candidate's resume and target JD for personalization.
            - Do NOT invent candidate experience.
            - Every question must have EXACTLY 4 options (A, B, C, D).
            - Every question must have EXACTLY 1 correct answer.
            - Provide a technically accurate explanation for the correct answer.
            - Ensure questions are role-specific and cover topics like Programming, Frameworks, Architecture, Databases, etc.
            
            JSON OUTPUT SCHEMA (RAW ARRAY OF OBJECTS):
            [
              {
                "question": "string",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctAnswerIndex": 0,
                "explanation": "string",
                "topic": "string",
                "difficulty": "Easy | Medium | Difficult"
              }
            ]
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText) ?: throw Exception("Failed to generate MCQs.")
        
        val type = object : com.google.gson.reflect.TypeToken<List<TechnicalMcq>>() {}.type
        gson.fromJson(jsonString, type)
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

    suspend fun generateModificationReport(
        jobDescription: String,
        resumeData: ResumeAnalysisResult
    ): ResumeOptimizationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is missing.")
        }

        fun isAddressOrContact(text: String): Boolean {
            val lw = text.lowercase().trim()
            val contactKeywords = listOf(
                "phone", "mobile", "mob", "tel", "telephone", "cell", "contact", "ph", "whatsapp",
                "email", "e-mail", "mail", "gmail",
                "github", "linkedin", "portfolio", "website", "www.", "http",
                "address", "location", "residence", "domicile", "permanent", "correspondence", "city", "state", "country", "pincode", "pin code", "zip"
            )
            if (contactKeywords.any { lw.startsWith("$it ") || lw.startsWith("$it:") || lw.startsWith("$it-") || lw.startsWith("$it –") }) return true
            if (lw.contains("@") || lw.contains(".com") || lw.contains(".in") || lw.contains(".net") || lw.contains(".org")) return true
            val addressKeywords = listOf("street", "nagar", "road", "colony", "avenue", "layout", "cross", "main", "floor", "building", "apartment", "h.no", "house no", "plot no", "door no")
            if (addressKeywords.any { lw.contains(" $it ") || lw.contains(" $it,") || lw.startsWith("$it ") }) return true
            val personalKeywords = listOf("father's name", "mother's name", "date of birth", "dob", "marital status", "nationality", "gender", "sex", "languages known", "hobbies", "interests")
            if (personalKeywords.any { lw.contains(it) }) return true
            if (Regex("\\b\\d{6}\\b").containsMatchIn(lw)) return true
            if (Regex("\\b[6-9]\\d{9}\\b").containsMatchIn(lw)) return true
            if (Regex("\\b\\d{3}[-.\\s]??\\d{3}[-.\\s]??\\d{4}\\b").containsMatchIn(lw)) return true
            return false
        }

        val candidateSkills = resumeData.extractedSkills?.map { it.name } ?: emptyList()
        val candidateEdu = resumeData.education?.filter { !isAddressOrContact(it) } ?: emptyList()
        val candidateExp = resumeData.experience?.filter { !isAddressOrContact(it) } ?: emptyList()
        val candidateProjects = resumeData.projectAnalysis?.map { proj ->
            val tech = if (!proj.technologies.isNullOrEmpty()) " (Technologies: ${proj.technologies.joinToString(", ")})" else ""
            val desc = if (!proj.strengths.isNullOrEmpty()) " - ${proj.strengths.joinToString("; ")}" else ""
            "${proj.name}$tech$desc"
        } ?: emptyList()
        val candidateSummary = resumeData.summary ?: ""

        val prompt = """
            You are an expert AI Technical Interviewer and Career Strategist. Generate a structured Resume Modification Report by comparing the candidate's ACTUAL resume content against the provided Job Description (JD).
            
            STRICT DATA INTEGRITY & ISOLATION PROTOCOL:
            1. SOURCE OF TRUTH: Use ONLY the Candidate Resume Data below. If a section below contains "none listed" or is empty, DO NOT assume the candidate has those skills or projects.
            2. SECTION ISOLATION (ZERO CROSS-CONTAMINATION):
               - PROFESSIONAL SUMMARY: Use ONLY the candidate's profile/objective provided below.
               - TECHNICAL SKILLS: Use ONLY the explicit skills list provided.
               - WORK EXPERIENCE: Use ONLY actual employment/internship history. NEVER include contact info, address, education, or projects in "Work Experience".
               - PROJECTS: Use ONLY the specific candidate projects provided below. If projects are listed, you MUST analyze them. NEVER say "no projects found" if there are projects in the data below.
               - EDUCATION: Use ONLY the candidate's academic degrees/institutions.
            3. NO FABRICATION: Do not invent missing experience. If a JD requirement is missing from the resume, list it as a GAP.
            4. CONDITIONAL ADVICE: Only suggest adding a missing skill if you use a disclaimer like "If you have experience with [Skill], ensure it is visible...".
            
            CANDIDATE RESUME DATA (ISOLATED):
            - Candidate Name: ${resumeData.candidateName ?: "Candidate"}
            - Professional Summary: ${if (candidateSummary.isNotBlank()) candidateSummary else "None provided"}
            - Work Experience & Internships: ${if (candidateExp.isNotEmpty()) candidateExp.joinToString("\n| ") else "No formal experience listed (Fresher / Student)"}
            - Technical Skills: ${if (candidateSkills.isNotEmpty()) candidateSkills.joinToString(", ") else "None listed"}
            - Projects (Built by Candidate): ${if (candidateProjects.isNotEmpty()) candidateProjects.joinToString("\n| ") else "None listed"}
            - Education: ${if (candidateEdu.isNotEmpty()) candidateEdu.joinToString("\n| ") else "None listed"}
            
            JOB DESCRIPTION:
            $jobDescription
            
            REQUIRED ANALYSIS SECTIONS:
            1. MATCHES: Requirements from JD already supported by the resume.
            2. MODIFICATIONS: Existing resume content that should be emphasized or rewritten for better JD alignment.
            3. GAPS: JD requirements with no evidence in the resume.
            
            TASKS:
            1. Calculate an "Overall Match" percentage (0-100) based on actual evidence.
            2. Provide "overallReasoning" for the score.
            3. Identify "matchedAreas", "recommendedChanges", and "potentialGaps".
            4. Generate "Section Analysis" for: Professional Summary, Technical Skills, Projects, Work Experience, Education.
            
            For each section analysis:
            - sectionId: "SUMMARY" | "SKILLS" | "PROJECTS" | "EXPERIENCE" | "EDUCATION"
            - sectionName: Professional Summary | Technical Skills | Projects | Work Experience | Education
            - priority: HIGH | RECOMMENDED | ALIGNED
            - currentContent: Provide the ACTUAL text from the candidate's resume for this section.
            - jdRequirement: What the JD asks for regarding this section.
            - reason: Why this section is a match or needs alignment.
            - modificationRequired: Strategic advice on what to change.
            - specificAction: CLEAR, STEP-BY-STEP instruction for the candidate.
            - whatNotToClaim: Explicit warning NOT to add skills they don't have.
            
            JSON OUTPUT SCHEMA:
            {
              "jdTitle": "string",
              "companyName": "string or null",
              "overallMatch": 0,
              "overallReasoning": "string",
              "summary": "Overall alignment overview...",
              "strongMatchCount": 0,
              "recommendedImprovementCount": 0,
              "potentialGapCount": 0,
              "matchedAreas": ["string"],
              "recommendedChanges": ["string"],
              "potentialGaps": ["string"],
              "sections": [
                {
                  "sectionId": "SUMMARY | SKILLS | PROJECTS | EXPERIENCE | EDUCATION",
                  "sectionName": "string",
                  "priority": "HIGH | RECOMMENDED | ALIGNED",
                  "currentContent": "string",
                  "jdRequirement": "string",
                  "reason": "string",
                  "modificationRequired": "string",
                  "specificAction": "string",
                  "suggestedConsiderations": ["string"],
                  "whatNotToClaim": "string",
                  "matchedKeywords": ["string"]
                }
              ]
            }
        """.trimIndent()

        val responseText = executeWithFallbackAndRetry(prompt, isJson = true)
        val jsonString = extractJson(responseText) ?: throw Exception("Failed to generate modification report.")
        
        parseReportJson(jsonString, jobDescription, resumeData, candidateExp, candidateProjects, candidateSkills, candidateEdu, candidateSummary)
    }

    private fun parseReportJson(
        jsonString: String,
        originalJd: String,
        resumeData: ResumeAnalysisResult,
        candidateExp: List<String>,
        candidateProjects: List<String>,
        candidateSkills: List<String>,
        candidateEdu: List<String>,
        candidateSummary: String
    ): ResumeOptimizationResult {
        val root = try { JsonParser.parseString(jsonString).asJsonObject } catch (_: Exception) { JsonObject() }
        
        val jdTitle = if (root.has("jdTitle") && !root.get("jdTitle").isJsonNull) root.get("jdTitle").asString else "Target Role"
        val companyName = if (root.has("companyName") && !root.get("companyName").isJsonNull) root.get("companyName").asString else null
        
        val matchedAreas = getSafeStringListFromJson(root, "matchedAreas")
        val recommendedChanges = getSafeStringListFromJson(root, "recommendedChanges")
        val potentialGaps = getSafeStringListFromJson(root, "potentialGaps")

        fun isAddressContent(text: String): Boolean {
            val lw = text.lowercase().trim()
            val addressKeywords = listOf("street", "nagar", "road", "colony", "avenue", "layout", "cross", "main", "floor", "building", "pincode", "pin code", "pin:", "dist:")
            if (addressKeywords.any { lw.contains(" $it ") || lw.contains(" $it,") || lw.startsWith("$it ") || lw.contains(",$it ") }) return true
            if (lw.contains("@") || lw.contains("github.com") || lw.contains("linkedin.com")) return true
            if (Regex("\\b\\d{6}\\b").containsMatchIn(lw) || Regex("\\b[6-9]\\d{9}\\b").containsMatchIn(lw)) return true
            return false
        }

        val report = com.example.aidrivencompetencyplatform.model.ResumeModificationReport(
            targetRole = jdTitle,
            originalJobDescription = originalJd,
            overallMatch = if (root.has("overallMatch") && !root.get("overallMatch").isJsonNull) root.get("overallMatch").asInt else 0,
            summary = if (root.has("summary") && !root.get("summary").isJsonNull) root.get("summary").asString else "",
            overallReasoning = if (root.has("overallReasoning") && !root.get("overallReasoning").isJsonNull) root.get("overallReasoning").asString else null,
            strongMatchCount = matchedAreas.size,
            recommendedImprovementCount = recommendedChanges.size,
            potentialGapCount = potentialGaps.size,
            matchedAreas = matchedAreas,
            recommendedChanges = recommendedChanges,
            potentialGaps = potentialGaps,
            sections = mutableListOf<com.example.aidrivencompetencyplatform.model.SectionAnalysis>().also { list ->
                if (root.has("sections") && root.get("sections").isJsonArray) {
                    root.getAsJsonArray("sections").forEach { item ->
                        try {
                            val sObj = item.asJsonObject
                            val rawName = if (sObj.has("sectionName") && !sObj.get("sectionName").isJsonNull) sObj.get("sectionName").asString else "Section"
                            
                            val normalizedName = when {
                                rawName.contains("Summary", ignoreCase = true) || rawName.contains("Objective", ignoreCase = true) -> "Professional Summary"
                                rawName.contains("Skill", ignoreCase = true) || rawName.contains("Competencies", ignoreCase = true) -> "Technical Skills"
                                rawName.contains("Project", ignoreCase = true) -> "Projects"
                                rawName.contains("Experience", ignoreCase = true) || rawName.contains("Employment", ignoreCase = true) || rawName.contains("Work", ignoreCase = true) -> "Work Experience"
                                rawName.contains("Education", ignoreCase = true) || rawName.contains("Academic", ignoreCase = true) -> "Education"
                                rawName.contains("Certif", ignoreCase = true) -> "Certifications"
                                else -> rawName
                            }

                            var currentContent = if (sObj.has("currentContent") && !sObj.get("currentContent").isJsonNull) sObj.get("currentContent").asString.trim() else ""
                            
                            // Reconcile and guarantee strict section data integrity
                            when (normalizedName) {
                                "Work Experience" -> {
                                    if (currentContent.isBlank() || isAddressContent(currentContent) || currentContent.equals("none", ignoreCase = true)) {
                                        currentContent = if (candidateExp.isNotEmpty()) candidateExp.joinToString("\n")
                                            else "No formal employment/internship experience listed in resume (Fresher / Student)"
                                    }
                                }
                                "Projects" -> {
                                    if ((currentContent.isBlank() || currentContent.equals("none", ignoreCase = true) || currentContent.contains("no projects", ignoreCase = true)) && candidateProjects.isNotEmpty()) {
                                        currentContent = candidateProjects.joinToString("\n\n")
                                    }
                                }
                                "Professional Summary" -> {
                                    if (currentContent.isBlank() && candidateSummary.isNotBlank()) {
                                        currentContent = candidateSummary
                                    }
                                }
                                "Technical Skills" -> {
                                    if (currentContent.isBlank() && candidateSkills.isNotEmpty()) {
                                        currentContent = candidateSkills.joinToString(", ")
                                    }
                                }
                                "Education" -> {
                                    if (currentContent.isBlank() && candidateEdu.isNotEmpty()) {
                                        currentContent = candidateEdu.joinToString(" | ")
                                    }
                                }
                            }

                            list.add(com.example.aidrivencompetencyplatform.model.SectionAnalysis(
                                sectionId = if (sObj.has("sectionId") && !sObj.get("sectionId").isJsonNull) sObj.get("sectionId").asString else "",
                                sectionName = normalizedName,
                                priority = try { com.example.aidrivencompetencyplatform.model.ReportPriority.valueOf(sObj.get("priority").asString) } catch (_: Exception) { com.example.aidrivencompetencyplatform.model.ReportPriority.ALIGNED },
                                currentContent = currentContent,
                                jdRequirement = if (sObj.has("jdRequirement") && !sObj.get("jdRequirement").isJsonNull) sObj.get("jdRequirement").asString else null,
                                reason = if (sObj.has("reason") && !sObj.get("reason").isJsonNull) sObj.get("reason").asString else "",
                                modificationRequired = if (sObj.has("modificationRequired") && !sObj.get("modificationRequired").isJsonNull) sObj.get("modificationRequired").asString else "",
                                specificAction = if (sObj.has("specificAction") && !sObj.get("specificAction").isJsonNull) sObj.get("specificAction").asString else null,
                                suggestedConsiderations = getSafeStringListFromJson(sObj, "suggestedConsiderations"),
                                caution = if (sObj.has("caution") && !sObj.get("caution").isJsonNull) sObj.get("caution").asString else null,
                                whatNotToClaim = if (sObj.has("whatNotToClaim") && !sObj.get("whatNotToClaim").isJsonNull) sObj.get("whatNotToClaim").asString else null,
                                matchedKeywords = getSafeStringListFromJson(sObj, "matchedKeywords")
                            ))
                        } catch (_: Exception) {}
                    }
                }
            }
        )
        
        return ResumeOptimizationResult(jdTitle, companyName, report = report)
    }

    private fun getSafeStringListFromJson(obj: JsonObject, key: String): List<String> {
        val list = mutableListOf<String>()
        if (obj.has(key) && obj.get(key).isJsonArray) {
            obj.getAsJsonArray(key).forEach { if (it.isJsonPrimitive) list.add(it.asString) }
        }
        return list
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
        // Use v1beta endpoint for Gemini Developer API generateContent
        val apiVersion = "v1beta"
        val url = "https://generativelanguage.googleapis.com/$apiVersion/models/$model:generateContent"

        val requestBodyMap = mutableMapOf<String, Any>(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to promptText))))
        )
        val genConfig = mutableMapOf<String, Any>(
            "maxOutputTokens" to 8192,
            "temperature" to 0.1,
            "thinkingConfig" to mapOf(
                "thinkingLevel" to "LOW"
            )
        )
        if (isJson) {
            genConfig["response_mime_type"] = "application/json"
        }
        requestBodyMap["generationConfig"] = genConfig

        val requestBody = gson.toJson(requestBodyMap).toRequestBody(jsonMediaType)
        
        // Build request using the official header-based authentication style.
        // Using .header() ensures that any previous value is replaced.
        // We explicitly trim the key to prevent hidden whitespace from causing 401 errors.
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey.trim()) 
            .header("Content-Type", "application/json")
            .removeHeader("Authorization") // Prevent 401 UNAUTHENTICATED: ACCESS_TOKEN_TYPE_UNSUPPORTED
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    val code = response.code
                    Log.e("GeminiService", "API Error: $code. Body: $responseBody")
                    
                    // Specific handling for common Gemini error codes
                    val errorMsg = when (code) {
                        400 -> "Bad Request (400): Invalid request parameter. Model: $model. Body: $responseBody"
                        401 -> "Unauthenticated (401): Request had invalid authentication credentials. Expected API key in x-goog-api-key header or key query parameter. Body: $responseBody"
                        403 -> "Permission Denied (403): Your API key might be restricted or the Generative Language API is not enabled. Body: $responseBody"
                        404 -> "Model not found (404): Model '$model' is unavailable or the endpoint is incorrect."
                        429 -> "Rate limit (429): Quota exceeded. Retrying shortly."
                        500, 502, 503, 504 -> "Server error ($code): Gemini service temporarily unavailable."
                        else -> "API Error (HTTP $code). Body: $responseBody"
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

        val startBrace = cleaned.indexOf("{")
        val startBracket = cleaned.indexOf("[")
        
        val start = when {
            startBrace == -1 -> startBracket
            startBracket == -1 -> startBrace
            else -> minOf(startBrace, startBracket)
        }
        
        if (start == -1) return null

        val endBrace = cleaned.lastIndexOf("}")
        val endBracket = cleaned.lastIndexOf("]")
        val end = maxOf(endBrace, endBracket)
        
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
