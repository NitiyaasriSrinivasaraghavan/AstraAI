package com.example.aidrivencompetencyplatform.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidrivencompetencyplatform.data.AtsScoringEngine
import com.example.aidrivencompetencyplatform.data.GeminiService
import com.example.aidrivencompetencyplatform.data.PreferenceManager
import com.example.aidrivencompetencyplatform.data.ResumeParser
import com.example.aidrivencompetencyplatform.data.SessionManager
import com.example.aidrivencompetencyplatform.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "ResumeAnalysisFlow"

class DashboardViewModel(
    private val sessionManager: SessionManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _greetingPrefix = MutableStateFlow("Welcome")
    val greetingPrefix: StateFlow<String> = _greetingPrefix

    private val _hasAnalysis = MutableStateFlow(false)
    val hasAnalysis: StateFlow<Boolean> = _hasAnalysis

    private val _resumeScore = MutableStateFlow<Int?>(null)
    val resumeScore: StateFlow<Int?> = _resumeScore

    private val _atsScore = MutableStateFlow<Int?>(null)
    val atsScore: StateFlow<Int?> = _atsScore

    private val _skillMatch = MutableStateFlow<Int?>(null)
    val skillMatch: StateFlow<Int?> = _skillMatch

    private val _jobRecommendations = MutableStateFlow(emptyList<JobRecommendation>())
    val jobRecommendations: StateFlow<List<JobRecommendation>> = _jobRecommendations

    private val _hasCompletedTour = MutableStateFlow(true)
    val hasCompletedTour: StateFlow<Boolean> = _hasCompletedTour.asStateFlow()

    init {
        refreshUser()
        checkTourStatus()
    }

    private fun checkTourStatus() {
        viewModelScope.launch {
            preferenceManager.hasCompletedAppTour.collectLatest { completed ->
                _hasCompletedTour.value = completed
            }
        }
    }

    fun completeTour() {
        viewModelScope.launch {
            preferenceManager.setHasCompletedAppTour(true)
        }
    }

    fun refreshUser() {
        val name = sessionManager.getUserName()
        _userName.value = name ?: ""
        
        _greetingPrefix.value = if (sessionManager.isNewUser()) "Welcome" else "Welcome back"

        val analysis = sessionManager.getLatestAnalysis()
        if (analysis != null) {
            _hasAnalysis.value = true
            _resumeScore.value = analysis.overallScore
            _atsScore.value = analysis.atsScore
            _skillMatch.value = analysis.skillMatch
            
            // Fix NPE by using safe calls. Gson can set fields to null if missing in JSON.
            val extracted = analysis.extractedSkills ?: emptyList()
            val missing = analysis.missingSections ?: emptyList()
            
            val matchingSkills = extracted.map { it.name }.take(3)
            val missingItems = missing.take(1)

            _jobRecommendations.value = listOf(
                JobRecommendation(
                    sessionManager.getTargetRole() ?: "Target Role",
                    analysis.skillMatch,
                    "Matches based on extracted skills.",
                    matchingSkills,
                    missingItems
                )
            )
        } else {
            _hasAnalysis.value = false
            _resumeScore.value = null
            _atsScore.value = null
            _skillMatch.value = null
            _jobRecommendations.value = emptyList()
        }
    }
}

class AtsViewModel(
    private val sessionManager: SessionManager,
    private val scoringEngine: AtsScoringEngine = AtsScoringEngine()
) : ViewModel() {
    private val _analysisResult = MutableStateFlow<ResumeAnalysisResult?>(sessionManager.getLatestAnalysis())
    val analysisResult: StateFlow<ResumeAnalysisResult?> = _analysisResult

    private val _scoreResult = MutableStateFlow<AtsScoreResult?>(null)
    val scoreResult: StateFlow<AtsScoreResult?> = _scoreResult.asStateFlow()

    private val _atsBreakdownState = MutableStateFlow(AtsBreakdown())
    val atsBreakdownState: StateFlow<AtsBreakdown> = _atsBreakdownState.asStateFlow()

    private val _overallAtsScore = MutableStateFlow(0)
    val overallAtsScore: StateFlow<Int> = _overallAtsScore.asStateFlow()

    val targetRole: String
        get() = sessionManager.getTargetRole() ?: "Android Developer"

    init {
        calculateAtsScores()
    }

    fun refresh() {
        _analysisResult.value = sessionManager.getLatestAnalysis()
        calculateAtsScores()
    }

    private fun calculateAtsScores() {
        val analysis = _analysisResult.value ?: return
        val calculated = scoringEngine.calculateScore(analysis, targetRole)
        _scoreResult.value = calculated
        _overallAtsScore.value = calculated.overallScore
        _atsBreakdownState.value = AtsBreakdown(
            keywordCoverage = calculated.keywordCoverage.score,
            resumeStructure = calculated.resumeStructure.score,
            formattingSafety = calculated.formattingSafety.score,
            parsingAccuracy = calculated.parsingAccuracy.score
        )
    }
}

enum class SkillFilter {
    ALL, MATCHED, MISSING
}

data class SkillItem(
    val name: String,
    val isMatched: Boolean,
    val targetRole: String
)

data class SkillGapState(
    val targetRole: String = "Android Developer",
    val hasAnalysis: Boolean = false,
    val totalRequired: Int = 0,
    val matchedCount: Int = 0,
    val missingCount: Int = 0,
    val matchedSkills: List<String> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val allSkills: List<SkillItem> = emptyList(),
    val filter: SkillFilter = SkillFilter.ALL
)

object RoleSkillsData {
    val ROLE_SKILLS: Map<String, List<String>> = mapOf(
        "Android Developer" to listOf(
            "Kotlin", "Java", "Android SDK", "Jetpack Compose", "XML",
            "Android Studio", "Gradle", "REST APIs", "JSON", "Firebase",
            "Git", "SQLite", "Room Database", "Material Design"
        ),
        "Java Developer" to listOf(
            "Java", "OOP", "Collections", "Exception Handling", "Multithreading",
            "JDBC", "SQL", "Spring", "Spring Boot", "REST APIs",
            "Hibernate", "Maven", "Git"
        ),
        "Full Stack Developer" to listOf(
            "HTML", "CSS", "JavaScript", "React", "Node.js",
            "Express.js", "REST APIs", "SQL", "MongoDB", "Git",
            "Authentication", "Docker"
        ),
        "Data Analyst" to listOf(
            "Python", "SQL", "Excel", "Statistics", "Pandas",
            "NumPy", "Data Visualization", "Power BI", "Tableau", "Data Cleaning",
            "Git"
        ),
        "Data Scientist" to listOf(
            "Python", "SQL", "Statistics", "Probability", "Pandas",
            "NumPy", "Scikit-learn", "Machine Learning", "Data Visualization",
            "Feature Engineering", "Model Evaluation", "Git"
        ),
        "Machine Learning Engineer" to listOf(
            "Python", "Machine Learning", "Deep Learning", "NumPy", "Pandas",
            "Scikit-learn", "TensorFlow", "PyTorch", "SQL", "Git",
            "Model Deployment", "REST APIs"
        ),
        "Backend Developer" to listOf(
            "Java", "Python", "Node.js", "REST APIs", "SQL",
            "PostgreSQL", "MongoDB", "Spring Boot", "Authentication", "Git",
            "Docker"
        ),
        "Frontend Developer" to listOf(
            "HTML", "CSS", "JavaScript", "React", "TypeScript",
            "Responsive Design", "REST APIs", "Git", "UI/UX Principles", "Testing"
        ),
        "UI/UX Designer" to listOf(
            "Figma", "Wireframing", "Prototyping", "User Research", "User Flows",
            "Interaction Design", "Visual Design", "Typography", "Color Theory",
            "Usability Testing", "Design Systems"
        )
    )

    fun getRequiredSkills(targetRole: String): List<String> {
        val trimmed = targetRole.trim()
        ROLE_SKILLS[trimmed]?.let { return it }

        val lower = trimmed.lowercase()
        for ((role, skills) in ROLE_SKILLS) {
            val rLower = role.lowercase()
            if (lower == rLower || lower.contains(rLower) || rLower.contains(lower)) {
                return skills
            }
        }
        return when {
            lower.contains("android") -> ROLE_SKILLS["Android Developer"]!!
            lower.contains("java") -> ROLE_SKILLS["Java Developer"]!!
            lower.contains("full") || lower.contains("stack") -> ROLE_SKILLS["Full Stack Developer"]!!
            lower.contains("analyst") -> ROLE_SKILLS["Data Analyst"]!!
            lower.contains("scientist") -> ROLE_SKILLS["Data Scientist"]!!
            lower.contains("machine") || lower.contains("ml") -> ROLE_SKILLS["Machine Learning Engineer"]!!
            lower.contains("backend") -> ROLE_SKILLS["Backend Developer"]!!
            lower.contains("front") || lower.contains("web") -> ROLE_SKILLS["Frontend Developer"]!!
            lower.contains("ui") || lower.contains("ux") || lower.contains("design") -> ROLE_SKILLS["UI/UX Designer"]!!
            else -> ROLE_SKILLS["Android Developer"]!!
        }
    }

    fun isSkillMatched(requiredSkill: String, userSkills: List<String>): Boolean {
        val reqLower = requiredSkill.trim().lowercase()
        val userLowers = userSkills.map { it.trim().lowercase() }
        
        if (userLowers.contains(reqLower)) return true
        
        val aliases = getAliases(requiredSkill)
        if (aliases.any { alias -> userLowers.contains(alias.lowercase()) }) return true

        return userLowers.any { userSkill ->
            userSkill == reqLower ||
            (reqLower.length > 3 && userSkill.contains(reqLower)) ||
            (userSkill.length > 3 && reqLower.contains(userSkill))
        }
    }

    private fun getAliases(skill: String): List<String> {
        val s = skill.lowercase()
        return when {
            s.contains("compose") -> listOf("Compose", "Jetpack Compose", "Jetpack-Compose")
            s.contains("android sdk") -> listOf("Android", "Android Development", "Android SDK", "Android Framework")
            s.contains("android studio") -> listOf("Android Studio", "Android-Studio", "IDE")
            s.contains("room") -> listOf("Room", "Room Database", "Room DB", "Android Room")
            s.contains("material") -> listOf("Material Design", "Material", "Material 3", "M3")
            s.contains("rest") -> listOf("REST", "REST API", "REST APIs", "RESTful", "RESTful APIs")
            s.contains("json") -> listOf("JSON", "Gson", "Jackson", "Moshi", "Kotlin Serialization")
            s.contains("react") -> listOf("React", "React.js", "ReactJS")
            s.contains("node") -> listOf("Node", "Node.js", "NodeJS")
            s.contains("express") -> listOf("Express", "Express.js", "ExpressJS")
            s.contains("spring boot") -> listOf("Spring Boot", "SpringBoot", "Spring-Boot", "Spring")
            s.contains("scikit") -> listOf("Scikit-learn", "Scikit Learn", "sklearn")
            s.contains("tensorflow") -> listOf("TensorFlow", "Tensorflow", "TF")
            s.contains("pytorch") -> listOf("PyTorch", "Pytorch", "Torch")
            s.contains("docker") -> listOf("Docker", "Containerization", "Containers")
            s.contains("postgres") -> listOf("PostgreSQL", "Postgres", "PSQL")
            s.contains("mongo") -> listOf("MongoDB", "Mongo")
            s.contains("power bi") -> listOf("Power BI", "PowerBI", "Power-BI")
            s.contains("ui/ux") || s.contains("design systems") -> listOf("UI/UX", "UI Design", "UX Design", "Design System")
            else -> emptyList()
        }
    }
}

class SkillGapViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _analysisResult = MutableStateFlow<ResumeAnalysisResult?>(sessionManager.getLatestAnalysis())
    val analysisResult: StateFlow<ResumeAnalysisResult?> = _analysisResult

    private val _state = MutableStateFlow(SkillGapState())
    val state: StateFlow<SkillGapState> = _state.asStateFlow()

    private val _selectedSkillForDetail = MutableStateFlow<SkillItem?>(null)
    val selectedSkillForDetail: StateFlow<SkillItem?> = _selectedSkillForDetail.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val analysis = sessionManager.getLatestAnalysis()
        _analysisResult.value = analysis

        val role = sessionManager.getTargetRole() ?: "Android Developer"
        
        if (analysis == null) {
            _state.value = SkillGapState(
                targetRole = role,
                hasAnalysis = false
            )
            return
        }

        val required = RoleSkillsData.getRequiredSkills(role)
        val userExtracted = (analysis.extractedSkills ?: emptyList()).map { it.name }
        
        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()

        required.forEach { req ->
            if (RoleSkillsData.isSkillMatched(req, userExtracted)) {
                matched.add(req)
            } else {
                missing.add(req)
            }
        }

        val allItems = required.map { req ->
            SkillItem(
                name = req,
                isMatched = matched.contains(req),
                targetRole = role
            )
        }

        _state.value = SkillGapState(
            targetRole = role,
            hasAnalysis = true,
            totalRequired = required.size,
            matchedCount = matched.size,
            missingCount = missing.size,
            matchedSkills = matched,
            missingSkills = missing,
            allSkills = allItems,
            filter = _state.value.filter
        )
    }

    fun setFilter(filter: SkillFilter) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun selectSkill(skill: SkillItem) {
        _selectedSkillForDetail.value = skill
    }

    fun dismissSkillDetail() {
        _selectedSkillForDetail.value = null
    }
}

class ResumeViewModel(
    private val sessionManager: SessionManager,
    private val resumeParser: ResumeParser,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _analysisResult = MutableStateFlow<ResumeAnalysisResult?>(null)
    val analysisResult: StateFlow<ResumeAnalysisResult?> = _analysisResult

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loadingStage = MutableStateFlow("")
    val loadingStage: StateFlow<String> = _loadingStage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var selectedUri: Uri? = null

    fun onFileSelected(uri: Uri, name: String) {
        Log.d(TAG, "RESUME_URI_RECEIVED: $uri, FileName: $name")
        selectedUri = uri
        _selectedFileName.value = name
        _errorMessage.value = null
    }

    fun analyzeResume(targetRole: String) {
        Log.d(TAG, "ANALYZE_BUTTON_CLICKED: TargetRole: $targetRole")
        
        val uri = selectedUri ?: run {
            Log.e(TAG, "ANALYSIS_FAILED: Null URI")
            _errorMessage.value = "Please select a resume first."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _loadingStage.value = "Parsing Resume..."
                Log.d(TAG, "RESUME_PARSING_STARTED")
                val text = resumeParser.extractText(uri)
                val candidateInfo = resumeParser.parseCandidateInfo(text)
                
                if (text == ResumeParser.ERROR_SCANNED_PDF) {
                    Log.e(TAG, "ANALYSIS_FAILED: Scanned PDF detected")
                    _errorMessage.value = "This PDF appears to be image-based. Text extraction is not available for this file yet."
                } else if (text.isNotBlank()) {
                    Log.d(TAG, "RESUME_TEXT_EXTRACTED: Length: ${text.length}")
                    
                    _loadingStage.value = "Analyzing with AstraAI..."
                    try {
                        Log.d(TAG, "GEMINI_ANALYSIS_STARTED")
                        val result = geminiService.analyzeResume(text, targetRole)
                        
                        if (result != null) {
                            _loadingStage.value = "Finalizing Results..."
                            Log.d(TAG, "GEMINI_RESPONSE_RECEIVED")
                            val enrichedResult = result.copy(
                                candidateName = candidateInfo.name ?: result.candidateName,
                                candidateEmail = candidateInfo.email ?: result.candidateEmail,
                                candidatePhone = candidateInfo.phone ?: result.candidatePhone,
                                candidateLocation = candidateInfo.location ?: result.candidateLocation,
                                rawResumeText = text
                            )
                            _analysisResult.value = enrichedResult
                            sessionManager.saveLatestAnalysis(enrichedResult)
                            sessionManager.updateTargetRole(targetRole)
                            Log.d(TAG, "ANALYSIS_COMPLETED")
                        } else {
                            Log.e(TAG, "ANALYSIS_FAILED: Gemini returned null")
                            _errorMessage.value = "AI analysis returned no results. Please try again."
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ANALYSIS_FAILED: Gemini error", e)
                        _errorMessage.value = e.message ?: "AI analysis failed unexpectedly."
                    }
                } else {
                    Log.e(TAG, "ANALYSIS_FAILED: Empty extracted text")
                    _errorMessage.value = "Could not extract text from the selected file. Ensure it's not empty or protected."
                }
            } catch (e: Exception) {
                Log.e(TAG, "ANALYSIS_FAILED: General error", e)
                _errorMessage.value = "An error occurred: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _loadingStage.value = ""
            }
        }
    }

    fun reset() {
        selectedUri = null
        _selectedFileName.value = null
        _analysisResult.value = null
        _errorMessage.value = null
        _loadingStage.value = ""
    }
}

class AiAssistantViewModel(
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isInterviewMode = MutableStateFlow(false)
    val isInterviewMode: StateFlow<Boolean> = _isInterviewMode

    private val _isTourMode = MutableStateFlow(false)
    val isTourMode: StateFlow<Boolean> = _isTourMode

    private val _quickPrompts = MutableStateFlow<List<String>>(
        listOf(
            "How does ATS scoring work?",
            "How do I analyze my resume?",
            "What is Skill Gap analysis?",
            "How does JD Matcher work?"
        )
    )
    val quickPrompts: StateFlow<List<String>> = _quickPrompts

    init {
        initDefaultChat()
    }

    private fun initDefaultChat() {
        val userName = sessionManager.getUserName() ?: "there"
        _messages.value = listOf(
            ChatMessage("Hello $userName! I'm Astra, your AI Career Assistant. How can I help you elevate your career today?", false)
        )
    }

    fun startTour() {
        _isTourMode.value = true
        _isInterviewMode.value = false
        val userName = sessionManager.getUserName() ?: "there"
        _messages.value = listOf(
            ChatMessage(
                text = "Welcome to AstraAI, $userName! 🌟 I'm your dedicated Career Intelligence Companion.\n\n" +
                        "Here is what AstraAI does for you:\n" +
                        "1️⃣ **Analyse Resume**: Upload your resume (PDF/DOCX) and choose your target role to build your competency profile.\n" +
                        "2️⃣ **ATS Analysis**: Transparent 4-component scoring (Keyword Coverage 35%, Resume Structure 25%, Formatting Safety 20%, Parsing Accuracy 20%) with deep 'Ask Why?' XAI explanations.\n" +
                        "3️⃣ **Skill Gap Dashboard**: Pinpoint strong competencies, developing capabilities, and critical missing skills.\n" +
                        "4️⃣ **JD Matcher**: Compare your resume against specific job descriptions for targeted alignment.\n\n" +
                        "What would you like to explore first?",
                isFromUser = false
            )
        )
        _quickPrompts.value = listOf(
            "How do I analyze my resume?",
            "Explain ATS 4-component scoring",
            "How does Skill Gap work?",
            "Ready to upload my resume!"
        )
    }

    fun startInterview() {
        _isInterviewMode.value = true
        _isTourMode.value = false
        _messages.value = listOf(ChatMessage("Welcome to your mock interview. Let's start with a basic question: Tell me about yourself and your experience with Kotlin.", false))
        _quickPrompts.value = listOf(
            "I have 3+ years experience with Kotlin & Compose",
            "I have built several production Android applications",
            "Give me a technical architecture question"
        )
    }

    fun sendMessage(text: String, screenContext: String = "") {
        val userMsg = ChatMessage(text, true)
        _messages.value = _messages.value + userMsg
        
        viewModelScope.launch {
            val analysis = sessionManager.getLatestAnalysis()
            val context = "Screen: $screenContext. User name: ${sessionManager.getUserName()}. Analysis: ${if (analysis != null) "Available" else "Not available"}"
            val aiResponse = geminiService.getAiAssistantResponse(text, context)
            _messages.value = _messages.value + ChatMessage(aiResponse, false)
        }
    }
}
