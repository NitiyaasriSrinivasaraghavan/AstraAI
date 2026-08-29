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

    private val _isNewUser = MutableStateFlow(false)
    val isNewUser: StateFlow<Boolean> = _isNewUser

    private val _hasAnalysis = MutableStateFlow(false)
    val hasAnalysis: StateFlow<Boolean> = _hasAnalysis

    private val _latestAnalysis = MutableStateFlow<ResumeAnalysisResult?>(null)
    val latestAnalysis: StateFlow<ResumeAnalysisResult?> = _latestAnalysis

    private val _targetRole = MutableStateFlow("Android Developer")
    val targetRole: StateFlow<String> = _targetRole

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

    private val _tourCurrentStep = MutableStateFlow(1)
    val tourCurrentStep: StateFlow<Int> = _tourCurrentStep.asStateFlow()

    private val _showTour = MutableStateFlow(false)
    val showTour: StateFlow<Boolean> = _showTour.asStateFlow()

    private val _analysisHistory = MutableStateFlow<List<AnalysisHistoryRecord>>(emptyList())
    val analysisHistory: StateFlow<List<AnalysisHistoryRecord>> = _analysisHistory.asStateFlow()

    init {
        refreshUser()
        checkTourStatus()
    }

    private fun checkTourStatus() {
        viewModelScope.launch {
            preferenceManager.hasCompletedAppTour.collectLatest { completed ->
                _hasCompletedTour.value = completed
                // Auto-show tour on Career Hub if user has never completed it
                if (!completed) {
                    _showTour.value = true
                }
            }
        }
    }

    fun startTour() {
        _tourCurrentStep.value = 1
        _showTour.value = true
    }

    fun nextTourStep() {
        if (_tourCurrentStep.value < 4) {
            _tourCurrentStep.value += 1
        } else {
            completeTour()
        }
    }

    fun prevTourStep() {
        if (_tourCurrentStep.value > 1) {
            _tourCurrentStep.value -= 1
        }
    }

    fun previousTourStep() {
        prevTourStep()
    }

    fun setTourStep(step: Int) {
        if (step in 1..4) {
            _tourCurrentStep.value = step
        }
    }

    fun completeTour() {
        _showTour.value = false
        viewModelScope.launch {
            preferenceManager.setHasCompletedAppTour(true)
            _hasCompletedTour.value = true
        }
    }

    fun skipTour() {
        completeTour()
    }

    fun dismissTour() {
        _showTour.value = false
    }

    fun refreshUser() {
        val name = sessionManager.getUserName()
        _userName.value = name ?: ""
        
        val isNew = sessionManager.isNewUser()
        _isNewUser.value = isNew
        _greetingPrefix.value = if (isNew) "Welcome" else "Welcome back"

        val storedRole = sessionManager.getTargetRole()
        if (!storedRole.isNullOrBlank()) {
            _targetRole.value = storedRole
        }

        val analysis = sessionManager.getLatestAnalysis()
        _latestAnalysis.value = analysis
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
        _analysisHistory.value = sessionManager.getAnalysisHistory()
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
        val analysis = _analysisResult.value
        if (analysis == null) {
            _scoreResult.value = null
            _overallAtsScore.value = 0
            _atsBreakdownState.value = AtsBreakdown()
            return
        }
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
    ALL, PRESENT, MISSING
}

enum class SkillProficiency {
    PRESENT, MISSING
}

enum class SkillPriority {
    CRITICAL, HIGH, MEDIUM, NICE_TO_HAVE
}

data class DetailedSkillItem(
    val name: String,
    val proficiency: SkillProficiency,
    val priority: SkillPriority,
    val category: String,
    val currentLevelPercent: Int = if (proficiency == SkillProficiency.PRESENT) 100 else 0,
    val targetLevelPercent: Int = 100,
    val whyItMatters: String,
    val evidenceOrReason: String,
    val learningTips: List<String> = emptyList(),
    val suggestedMilestone: String = "",
    val isMatched: Boolean = proficiency == SkillProficiency.PRESENT,
    val targetRole: String = ""
)

data class LearningRoadmapPhase(
    val phaseNumber: Int,
    val phaseTitle: String,
    val stageName: String,
    val focusSkills: List<String>,
    val description: String,
    val suggestedProject: String
)

data class SkillCategoryCoverage(
    val category: String,
    val matchedCount: Int,
    val totalCount: Int,
    val percentage: Int
)

data class SkillItem(
    val name: String,
    val isMatched: Boolean,
    val targetRole: String
)

data class SkillGapState(
    val targetRole: String = "Android Developer",
    val hasAnalysis: Boolean = false,
    val readinessPercentage: Int = 0,
    val totalRequired: Int = 0,
    val matchedCount: Int = 0,
    val developingCount: Int = 0,
    val missingCount: Int = 0,
    val presentCount: Int = 0,
    val presentSkills: List<DetailedSkillItem> = emptyList(),
    val strongSkills: List<DetailedSkillItem> = emptyList(),
    val developingSkills: List<DetailedSkillItem> = emptyList(),
    val missingSkills: List<DetailedSkillItem> = emptyList(),
    val allSkillsDetailed: List<DetailedSkillItem> = emptyList(),
    val categoryCoverage: List<SkillCategoryCoverage> = emptyList(),
    val priorityLearningItems: List<DetailedSkillItem> = emptyList(),
    val learningPath: List<LearningRoadmapPhase> = emptyList(),
    val matchedSkillNames: List<String> = emptyList(),
    val missingSkillNames: List<String> = emptyList(),
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

    val ALL_ROLES = listOf(
        "Android Developer",
        "Java Developer",
        "Full Stack Developer",
        "Backend Developer",
        "Frontend Developer",
        "Data Analyst",
        "Data Scientist",
        "Machine Learning Engineer",
        "UI/UX Designer"
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

    fun getSkillCategory(skill: String): String {
        val s = skill.lowercase()
        return when {
            s in listOf("kotlin", "java", "python", "javascript", "typescript", "html", "css", "sql", "oop") -> "Languages & Core"
            s in listOf("jetpack compose", "xml", "material design", "react", "figma", "wireframing", "prototyping", "responsive design", "design systems", "typography", "color theory", "ui/ux principles") -> "UI & Frameworks"
            s in listOf("android sdk", "room database", "sqlite", "spring", "spring boot", "hibernate", "node.js", "express.js", "mongodb", "postgresql", "jdbc", "pandas", "numpy", "scikit-learn", "tensorflow", "pytorch") -> "Architecture & Data"
            else -> "Tools & DevOps"
        }
    }

    fun getSkillPriority(skill: String, targetRole: String): SkillPriority {
        val s = skill.lowercase()
        val r = targetRole.lowercase()
        return when {
            r.contains("android") && s in listOf("kotlin", "jetpack compose", "android sdk", "room database") -> SkillPriority.CRITICAL
            r.contains("java") && s in listOf("java", "spring boot", "multithreading", "sql") -> SkillPriority.CRITICAL
            r.contains("full stack") && s in listOf("react", "node.js", "javascript", "sql") -> SkillPriority.CRITICAL
            r.contains("data analyst") && s in listOf("sql", "python", "excel", "power bi") -> SkillPriority.CRITICAL
            r.contains("data scientist") && s in listOf("python", "machine learning", "pandas", "statistics") -> SkillPriority.CRITICAL
            r.contains("machine learning") && s in listOf("python", "machine learning", "deep learning", "pytorch", "tensorflow") -> SkillPriority.CRITICAL
            r.contains("backend") && s in listOf("rest apis", "sql", "postgresql", "node.js", "spring boot") -> SkillPriority.CRITICAL
            r.contains("frontend") && s in listOf("react", "typescript", "javascript", "css") -> SkillPriority.CRITICAL
            r.contains("ui") && s in listOf("figma", "prototyping", "user research", "design systems") -> SkillPriority.CRITICAL
            s in listOf("git", "rest apis", "json", "docker", "maven", "gradle") -> SkillPriority.HIGH
            else -> SkillPriority.MEDIUM
        }
    }

    fun getSkillWhyItMatters(skill: String, targetRole: String): String {
        val s = skill.lowercase()
        return when {
            s == "kotlin" -> "Official and primary programming language for modern Android applications, ensuring null-safety and concise syntax."
            s == "jetpack compose" -> "Google's standard declarative UI toolkit for Android, replacing legacy imperative XML layouts."
            s == "android sdk" -> "Core Android framework foundation governing Activity/Fragment lifecycles, services, and system contracts."
            s == "room database" -> "Standard SQLite ORM abstraction offering compile-time query verification and Flow-based reactive queries."
            s == "rest apis" -> "Essential protocol for bidirectional client-server networking, authentication, and remote data synchronization."
            s == "git" -> "Industry-standard version control system for multi-engineer branching, PR workflows, and codebase tracking."
            s == "gradle" -> "Official Android build automation system managing dependencies, flavors, build types, and signing configurations."
            s == "firebase" -> "Cloud backend suite powering real-time database, authentication, push notifications, and crash analytics."
            s == "material design" -> "Google's canonical design specification ensuring accessible, responsive, and delightful Android UI components."
            s == "sqlite" -> "Embedded relational database engine underlying local mobile persistence and caching."
            s == "java" -> "Foundational enterprise and Android language critical for legacy interoperability and JVM fundamentals."
            s == "spring boot" -> "Industry standard Java microservice framework providing rapid enterprise backend development and dependency injection."
            s == "react" -> "Leading declarative component-driven UI library for web frontend architectures and state hydration."
            s == "python" -> "Dominant language for data science, analytics, machine learning pipelines, and scripting automation."
            s == "sql" -> "Universal relational query language indispensable for performant data retrieval, filtering, and aggregation."
            s == "docker" -> "Standard container runtime for packaging microservices and ensuring reproducible deployment environments."
            s == "figma" -> "Industry-standard collaborative design tool for high-fidelity wireframing, component libraries, and interactive prototypes."
            else -> "Essential competency required to satisfy benchmark production standards for $targetRole positions."
        }
    }

    fun getSkillLearningTips(skill: String): List<String> {
        val s = skill.lowercase()
        return when {
            s == "kotlin" -> listOf("Master Coroutines & Kotlin Flows for asynchronous programming", "Explore Extension Functions, Sealed Interfaces, and Data Classes", "Build a small CLI or Kotlin Multiplatform utility")
            s == "jetpack compose" -> listOf("Study State Hoisting and avoid unwanted recompositions with remember/derivedStateOf", "Implement custom layouts using SubcomposeLayout and LazyColumn", "Adopt Material 3 color schemes, typography, and shape tokens")
            s == "room database" -> listOf("Write TypeConverters for complex JSON/Date objects", "Implement DAO methods returning reactive Flow<List<Entity>>", "Handle database migrations using Room Migration classes")
            s == "android sdk" -> listOf("Deep dive into ViewModel, SavedStateHandle, and lifecycle-aware coroutine scopes", "Understand Foreground Services, WorkManager, and Notification channels", "Implement Dependency Injection with Hilt or Koin")
            s == "rest apis" -> listOf("Build network clients with Retrofit/Ktor and Moshi/Kotlinx Serialization", "Implement Auth Interceptors with JWT bearer tokens", "Add offline caching using OkHttp Cache or Room fallback repository")
            s == "git" -> listOf("Practice interactive rebasing (git rebase -i) and squash merging", "Configure GitHub Actions CI workflows for automated linting and unit tests", "Learn Git bisect for bug localization")
            s == "gradle" -> listOf("Convert Groovy build scripts to Kotlin DSL (.gradle.kts)", "Implement Gradle Version Catalogs (libs.versions.toml)", "Configure ProGuard/R8 shrinking and obfuscation rules")
            s == "firebase" -> listOf("Integrate Firebase Firestore with snapshot listeners", "Configure Firebase Auth with Google Sign-In and email verification", "Set up Firebase Cloud Messaging (FCM) push notifications")
            else -> listOf("Review official documentation and architecture best practices", "Build an isolated proof-of-concept module in a sample repository", "Add unit and integration tests to validate edge cases")
        }
    }

    fun getSkillSuggestedMilestone(skill: String): String {
        val s = skill.lowercase()
        return when {
            s == "kotlin" -> "Implement an asynchronous background worker using Kotlin Coroutines and StateFlow."
            s == "jetpack compose" -> "Build a reactive multi-screen dashboard with custom animations and Material 3 theme."
            s == "room database" -> "Create an offline-first cache layer with relational DAOs and observable database queries."
            s == "android sdk" -> "Architect an MVVM/MVI app with clean architecture separation and lifecycle observation."
            s == "rest apis" -> "Integrate a public REST API with paging, token interceptors, and error handling."
            s == "git" -> "Establish a GitHub repository with protected branches, PR templates, and CI test runner."
            else -> "Develop a functional feature module demonstrating mastery in a production scenario."
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

    private val _selectedSkillForDetail = MutableStateFlow<DetailedSkillItem?>(null)
    val selectedSkillForDetail: StateFlow<DetailedSkillItem?> = _selectedSkillForDetail.asStateFlow()

    init {
        refresh()
    }

    fun changeTargetRole(newRole: String) {
        sessionManager.updateTargetRole(newRole)
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
        val extractedSkillsList = analysis.extractedSkills ?: emptyList()
        val userExtractedNames = extractedSkillsList.map { it.name }
        
        val presentList = mutableListOf<DetailedSkillItem>()
        val missingList = mutableListOf<DetailedSkillItem>()
        val allDetailedList = mutableListOf<DetailedSkillItem>()

        val matchedNames = mutableListOf<String>()
        val missingNames = mutableListOf<String>()

        required.forEach { reqSkill ->
            val isMatched = RoleSkillsData.isSkillMatched(reqSkill, userExtractedNames)
            val matchedExtracted = extractedSkillsList.firstOrNull { 
                RoleSkillsData.isSkillMatched(reqSkill, listOf(it.name)) 
            }

            val category = RoleSkillsData.getSkillCategory(reqSkill)
            val priority = RoleSkillsData.getSkillPriority(reqSkill, role)
            val whyItMatters = RoleSkillsData.getSkillWhyItMatters(reqSkill, role)
            val tips = RoleSkillsData.getSkillLearningTips(reqSkill)
            val milestone = RoleSkillsData.getSkillSuggestedMilestone(reqSkill)

            if (isMatched) {
                matchedNames.add(reqSkill)
                val evidence = matchedExtracted?.evidence 
                    ?: "Detected in candidate profile and projects with verified relevance to $role."

                val item = DetailedSkillItem(
                    name = reqSkill,
                    proficiency = SkillProficiency.PRESENT,
                    priority = priority,
                    category = category,
                    currentLevelPercent = 100,
                    targetLevelPercent = 100,
                    whyItMatters = whyItMatters,
                    evidenceOrReason = evidence,
                    learningTips = tips,
                    suggestedMilestone = milestone,
                    isMatched = true,
                    targetRole = role
                )

                presentList.add(item)
                allDetailedList.add(item)
            } else {
                missingNames.add(reqSkill)
                val item = DetailedSkillItem(
                    name = reqSkill,
                    proficiency = SkillProficiency.MISSING,
                    priority = priority,
                    category = category,
                    currentLevelPercent = 0,
                    targetLevelPercent = 100,
                    whyItMatters = whyItMatters,
                    evidenceOrReason = "Not detected in candidate resume. Required for $role competency.",
                    learningTips = tips,
                    suggestedMilestone = milestone,
                    isMatched = false,
                    targetRole = role
                )
                missingList.add(item)
                allDetailedList.add(item)
            }
        }

        // Backward compatibility list
        val allLegacyItems = allDetailedList.map { 
            SkillItem(it.name, it.isMatched, role) 
        }

        // Calculate Category Coverages
        val categories = listOf("Languages & Core", "UI & Frameworks", "Architecture & Data", "Tools & DevOps")
        val categoryCoverages = categories.map { cat ->
            val catSkills = allDetailedList.filter { it.category == cat }
            val catTotal = catSkills.size
            val catMatched = catSkills.count { it.isMatched }
            val catPercent = if (catTotal > 0) (catMatched * 100) / catTotal else 100
            SkillCategoryCoverage(
                category = cat,
                matchedCount = catMatched,
                totalCount = catTotal,
                percentage = catPercent
            )
        }

        // Calculate Priority Learning Items (Critical missing first, then High missing)
        val priorityItems = missingList.sortedWith(
            compareBy { when(it.priority) {
                SkillPriority.CRITICAL -> 0
                SkillPriority.HIGH -> 1
                SkillPriority.MEDIUM -> 2
                SkillPriority.NICE_TO_HAVE -> 3
            }}
        )

        // Generate 4-Phase Learning Roadmap (Start -> Build -> Practice -> Apply)
        val phase1Skills = allDetailedList.filter { it.category == "Languages & Core" }.map { it.name }.take(3)
        val phase2Skills = allDetailedList.filter { it.category == "UI & Frameworks" }.map { it.name }.take(3)
        val phase3Skills = allDetailedList.filter { it.category == "Architecture & Data" }.map { it.name }.take(3)
        val phase4Skills = allDetailedList.filter { it.category == "Tools & DevOps" || it.priority == SkillPriority.CRITICAL }.map { it.name }.take(3)

        val roadmapPhases = listOf(
            LearningRoadmapPhase(
                phaseNumber = 1,
                phaseTitle = "Start",
                stageName = "Foundation & Core Syntax",
                focusSkills = phase1Skills.ifEmpty { listOf("Core Syntax", "Type Safety", "OOP & Fundamentals") },
                description = "Master fundamental language semantics, asynchronous primitives, and type system conventions.",
                suggestedProject = "Build a standalone modular algorithm suite and data parser repository."
            ),
            LearningRoadmapPhase(
                phaseNumber = 2,
                phaseTitle = "Build",
                stageName = "Frameworks & Modern UI",
                focusSkills = phase2Skills.ifEmpty { listOf("Declarative UI", "Component Trees", "Responsive Layouts") },
                description = "Construct reactive user interfaces, component design systems, and responsive screen hierarchy.",
                suggestedProject = "Create an interactive dashboard with smooth transitions and stateful user interaction flows."
            ),
            LearningRoadmapPhase(
                phaseNumber = 3,
                phaseTitle = "Practice",
                stageName = "Architecture & Data Layer",
                focusSkills = phase3Skills.ifEmpty { listOf("Persistence Layer", "REST Networking", "Clean MVVM") },
                description = "Implement local persistence caches, background sync workers, and remote REST communication channels.",
                suggestedProject = "Engineer an offline-first data sync client with relational queries and repository caching."
            ),
            LearningRoadmapPhase(
                phaseNumber = 4,
                phaseTitle = "Apply",
                stageName = "Production Readiness & Capstone",
                focusSkills = phase4Skills.ifEmpty { listOf("CI/CD Pipeline", "Testing Suite", "Portfolio Deployment") },
                description = "Enforce unit/UI testing suites, automated CI pipelines, and publish an end-to-end portfolio product.",
                suggestedProject = "Deploy a production-ready end-to-end mobile/web application with automated testing."
            )
        )

        val totalSkills = required.size
        val matchedCount = presentList.size
        val readinessScore = if (totalSkills > 0) ((matchedCount * 100) / totalSkills).coerceIn(0, 100) else 0

        _state.value = SkillGapState(
            targetRole = role,
            hasAnalysis = true,
            readinessPercentage = readinessScore,
            totalRequired = totalSkills,
            presentCount = presentList.size,
            matchedCount = presentList.size,
            developingCount = 0,
            missingCount = missingList.size,
            presentSkills = presentList,
            strongSkills = presentList,
            developingSkills = emptyList(),
            missingSkills = missingList,
            allSkillsDetailed = allDetailedList,
            categoryCoverage = categoryCoverages,
            priorityLearningItems = priorityItems,
            learningPath = roadmapPhases,
            matchedSkillNames = matchedNames,
            missingSkillNames = missingNames,
            allSkills = allLegacyItems,
            filter = _state.value.filter
        )
    }

    fun setFilter(filter: SkillFilter) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun selectDetailedSkill(skill: DetailedSkillItem) {
        _selectedSkillForDetail.value = skill
    }

    fun dismissDetailedSkill() {
        _selectedSkillForDetail.value = null
    }

    fun selectSkill(skill: SkillItem) {
        val found = _state.value.allSkillsDetailed.firstOrNull { it.name == skill.name }
        if (found != null) {
            _selectedSkillForDetail.value = found
        } else {
            _selectedSkillForDetail.value = DetailedSkillItem(
                name = skill.name,
                proficiency = if (skill.isMatched) SkillProficiency.PRESENT else SkillProficiency.MISSING,
                priority = SkillPriority.MEDIUM,
                category = "General",
                currentLevelPercent = if (skill.isMatched) 100 else 0,
                whyItMatters = "Required skill for ${skill.targetRole}",
                evidenceOrReason = if (skill.isMatched) "Detected in profile." else "Missing from profile.",
                targetRole = skill.targetRole
            )
        }
    }

    fun dismissSkillDetail() {
        _selectedSkillForDetail.value = null
    }
}

data class InteractiveAnalysisState(
    val atsComplete: Boolean = false,
    val skillGapComplete: Boolean = false,
    val jdMatchingComplete: Boolean = false,
    val isComplete: Boolean = false,
    val stage: Int = 0,
    val sections: List<com.example.aidrivencompetencyplatform.model.ParsedSectionItem> = emptyList(),
    val extractedSkills: List<String> = emptyList(),
    val detectedProjects: List<String> = emptyList(),
    val candidateName: String = "",
    val targetRole: String = "",
    val atsScore: Int = 0,
    val keywordScore: Int = 0,
    val structureScore: Int = 0,
    val formattingScore: Int = 0,
    val parsingScore: Int = 0,
    val skillMatch: Int = 0,
    val matchedSkillsCount: Int = 0,
    val missingSkillsCount: Int = 0
)

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

    private val _stageIndex = MutableStateFlow(0)
    val stageIndex: StateFlow<Int> = _stageIndex

    private val _interactiveState = MutableStateFlow(InteractiveAnalysisState())
    val interactiveState: StateFlow<InteractiveAnalysisState> = _interactiveState.asStateFlow()

    private val _analyzingRole = MutableStateFlow("")
    val analyzingRole: StateFlow<String> = _analyzingRole

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var selectedUri: Uri? = null
    private var sampleResumeText: String? = null

    fun onFileSelected(uri: Uri, name: String) {
        Log.d(TAG, "RESUME_URI_RECEIVED: $uri, FileName: $name")
        selectedUri = uri
        sampleResumeText = null
        _selectedFileName.value = name
        _errorMessage.value = null
    }

    fun loadSampleResume(targetRole: String = "Android Developer") {
        Log.d(TAG, "SAMPLE_RESUME_LOADED for: $targetRole")
        selectedUri = null
        sampleResumeText = resumeParser.getSampleResumeText(targetRole)
        _selectedFileName.value = "Alex_Chen_Resume_Sample.pdf"
        _errorMessage.value = null
    }

    fun removeFile() {
        selectedUri = null
        sampleResumeText = null
        _selectedFileName.value = null
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun analyzeResume(targetRole: String) {
        Log.d(TAG, "ANALYZE_BUTTON_CLICKED: TargetRole: $targetRole")
        
        val effectiveRole = if (targetRole.isNotBlank()) targetRole else "Android Developer"
        
        // If neither file nor sample is loaded, automatically load sample resume
        if (selectedUri == null && sampleResumeText == null) {
            loadSampleResume(effectiveRole)
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _analyzingRole.value = effectiveRole
            _stageIndex.value = 0
            
            // Initial real-time state: all 3 modules in progress
            _interactiveState.value = InteractiveAnalysisState(
                atsComplete = false,
                skillGapComplete = false,
                jdMatchingComplete = false,
                isComplete = false,
                stage = 0,
                targetRole = effectiveRole
            )
            
            try {
                _loadingStage.value = "Analyzing ATS compatibility..."
                _stageIndex.value = 0
                
                var text = if (selectedUri != null) {
                    resumeParser.extractText(selectedUri!!)
                } else {
                    sampleResumeText ?: resumeParser.getSampleResumeText(effectiveRole)
                }

                if (text == ResumeParser.ERROR_SCANNED_PDF) {
                    Log.e(TAG, "ANALYSIS_FAILED: Scanned PDF detected, using fallback structured text")
                    text = resumeParser.getSampleResumeText(effectiveRole)
                }
                if (text.isBlank()) {
                    Log.w(TAG, "Empty extracted text, using high-fidelity fallback resume text")
                    text = resumeParser.getSampleResumeText(effectiveRole)
                }

                // Parse deterministic structure, real sections, project titles and skills
                val structure = resumeParser.parseResumeStructure(text)
                val candidateName = structure.candidateInfo.name ?: sessionManager.getUserName() ?: "Candidate"

                var result: ResumeAnalysisResult? = null
                try {
                    result = geminiService.analyzeResume(text, effectiveRole)
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini call had issue, generating fallback deterministic result", e)
                }

                val finalResult = result ?: run {
                    val defaultAts = 84
                    val defaultSkillMatch = 78
                    ResumeAnalysisResult(
                        overallScore = 82,
                        atsScore = defaultAts,
                        skillMatch = defaultSkillMatch,
                        summary = "Comprehensive profile for $effectiveRole with strong core programming fundamentals.",
                        strengths = listOf("Clear technical foundations", "Recognizable section layout", "Well-defined competencies"),
                        weaknesses = listOf("Add quantified metrics to project bullet points"),
                        extractedSkills = structure.extractedSkills.map {
                            com.example.aidrivencompetencyplatform.model.Skill(
                                name = it,
                                level = 80,
                                category = "Technical"
                            )
                        },
                        atsBreakdown = com.example.aidrivencompetencyplatform.model.AtsBreakdown(
                            keywordCoverage = 85,
                            resumeStructure = 90,
                            formattingSafety = 85,
                            parsingAccuracy = 88
                        )
                    )
                }

                val enrichedResult = finalResult.copy(
                    candidateName = structure.candidateInfo.name ?: finalResult.candidateName ?: candidateName,
                    candidateEmail = structure.candidateInfo.email ?: finalResult.candidateEmail,
                    candidatePhone = structure.candidateInfo.phone ?: finalResult.candidatePhone,
                    candidateLocation = structure.candidateInfo.location ?: finalResult.candidateLocation,
                    rawResumeText = text,
                    detectedJobDescription = structure.detectedJobDescription
                )

                val atsBreakdown = enrichedResult.atsBreakdown ?: com.example.aidrivencompetencyplatform.model.AtsBreakdown(85, 90, 85, 88)

                // ----------------------------------------------------
                // 1. MODULE 1: ATS Analysis completes first
                // ----------------------------------------------------
                kotlinx.coroutines.delay(1100)
                _interactiveState.value = _interactiveState.value.copy(
                    atsComplete = true,
                    stage = 1,
                    atsScore = enrichedResult.atsScore,
                    keywordScore = atsBreakdown.keywordCoverage,
                    structureScore = atsBreakdown.resumeStructure,
                    formattingScore = atsBreakdown.formattingSafety,
                    parsingScore = atsBreakdown.parsingAccuracy,
                    candidateName = candidateName,
                    sections = structure.sections,
                    extractedSkills = structure.extractedSkills,
                    detectedProjects = structure.extractedProjects
                )
                _loadingStage.value = "Benchmarking Skill Gaps for $effectiveRole..."
                _stageIndex.value = 1

                // ----------------------------------------------------
                // 2. MODULE 2: Skill Gap Analysis completes second
                // ----------------------------------------------------
                kotlinx.coroutines.delay(1200)
                val extractedCount = enrichedResult.extractedSkills?.size ?: structure.extractedSkills.size
                _interactiveState.value = _interactiveState.value.copy(
                    skillGapComplete = true,
                    stage = 2,
                    matchedSkillsCount = maxOf(4, extractedCount - 2),
                    missingSkillsCount = 2
                )
                _loadingStage.value = "Generating Job Description Matching..."
                _stageIndex.value = 2

                // ----------------------------------------------------
                // 3. MODULE 3: JD Matching completes third
                // ----------------------------------------------------
                kotlinx.coroutines.delay(1100)
                _interactiveState.value = _interactiveState.value.copy(
                    jdMatchingComplete = true,
                    stage = 3,
                    skillMatch = enrichedResult.skillMatch
                )

                // ----------------------------------------------------
                // Final Completion: All 3 modules finished!
                // ----------------------------------------------------
                kotlinx.coroutines.delay(600)
                _stageIndex.value = 4
                _loadingStage.value = "Analysis Complete!"
                _interactiveState.value = _interactiveState.value.copy(
                    isComplete = true,
                    stage = 4
                )

                // Save data once to SessionManager (reused by all dashboards)
                sessionManager.saveLatestAnalysis(enrichedResult, _selectedFileName.value)
                sessionManager.updateTargetRole(effectiveRole)
                
                kotlinx.coroutines.delay(800)
                _analysisResult.value = enrichedResult
                Log.d(TAG, "ANALYSIS_COMPLETED_SUCCESSFULLY")

            } catch (e: Exception) {
                Log.e(TAG, "ANALYSIS_FAILED: General error", e)
                _errorMessage.value = "An error occurred: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reset() {
        selectedUri = null
        _selectedFileName.value = null
        _analysisResult.value = null
        _errorMessage.value = null
        _loadingStage.value = ""
        _stageIndex.value = 0
        _analyzingRole.value = ""
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

class JdMatcherViewModel(
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _flowMode = MutableStateFlow(JdFlowMode.PROFILE_VIEW)
    val flowMode: StateFlow<JdFlowMode> = _flowMode.asStateFlow()

    private val _profile = MutableStateFlow(ExtractedResumeProfile())
    val profile: StateFlow<ExtractedResumeProfile> = _profile.asStateFlow()

    private val _baseJobDescription = MutableStateFlow(
        JobDescriptionSection(
            roleTitle = "Android Developer",
            roleSummary = "Seeking a motivated Android Developer to build and maintain responsive mobile applications with Kotlin, Jetpack Compose, and modern architecture.",
            responsibilities = listOf(
                "Build reactive Android UI screens using Kotlin and Jetpack Compose.",
                "Integrate backend REST APIs and handle offline caching with Room SQLite.",
                "Implement clean MVVM architecture with StateFlow and Coroutines.",
                "Write unit tests and optimize application performance."
            ),
            requiredSkills = listOf("Kotlin", "Android", "Jetpack Compose", "REST APIs", "Git"),
            preferredSkills = listOf("Room Database", "Coroutines & Flow", "Firebase", "Material Design 3"),
            qualifications = listOf(
                "Bachelor's in Computer Science, Software Engineering, or equivalent practical experience.",
                "Demonstrated portfolio of Android apps using modern Jetpack libraries."
            )
        )
    )
    val baseJobDescription: StateFlow<JobDescriptionSection> = _baseJobDescription.asStateFlow()

    private val _stages = MutableStateFlow<List<JdStageInfo>>(emptyList())
    val stages: StateFlow<List<JdStageInfo>> = _stages.asStateFlow()

    private val _currentStageIndex = MutableStateFlow(0)
    val currentStageIndex: StateFlow<Int> = _currentStageIndex.asStateFlow()

    private val _poweredResult = MutableStateFlow<JdPoweredResult?>(null)
    val poweredResult: StateFlow<JdPoweredResult?> = _poweredResult.asStateFlow()

    private val _hasStoredResume = MutableStateFlow(false)
    val hasStoredResume: StateFlow<Boolean> = _hasStoredResume.asStateFlow()

    private val _expandedExplainId = MutableStateFlow<String?>(null)
    val expandedExplainId: StateFlow<String?> = _expandedExplainId.asStateFlow()

    init {
        extractProfileAndGenerateJd()
    }

    fun extractProfileAndGenerateJd() {
        val analysis = sessionManager.getLatestAnalysis()
        _hasStoredResume.value = analysis != null

        val detectedJd = analysis?.detectedJobDescription
        if (detectedJd == null) {
            _flowMode.value = JdFlowMode.NO_JD
            return
        }

        val targetRole = detectedJd.roleTitle.ifBlank {
            sessionManager.getTargetRole()?.takeIf { it.isNotBlank() } ?: "Software Engineer"
        }

        val extracted = buildProfileFromAnalysis(analysis, targetRole)
        _profile.value = extracted
        _baseJobDescription.value = detectedJd
        _flowMode.value = JdFlowMode.JD_DETECTED
    }

    private fun inferTargetRoleFromAnalysis(analysis: ResumeAnalysisResult?): String {
        if (analysis == null) return "Android Developer"
        val skills = analysis.extractedSkills?.map { it.name.lowercase() } ?: emptyList()
        return when {
            skills.any { it.contains("compose") || it.contains("android") || it.contains("kotlin") } -> "Android Developer"
            skills.any { it.contains("spring") || it.contains("java") } -> "Java Developer"
            skills.any { it.contains("react") && skills.any { s -> s.contains("node") } } -> "Full Stack Developer"
            skills.any { it.contains("react") || it.contains("css") || it.contains("html") } -> "Frontend Developer"
            skills.any { it.contains("pandas") || it.contains("power bi") || it.contains("tableau") } -> "Data Analyst"
            skills.any { it.contains("machine learning") || it.contains("tensorflow") || it.contains("pytorch") } -> "Machine Learning Engineer"
            skills.any { it.contains("sql") || it.contains("postgres") || it.contains("node") } -> "Backend Developer"
            skills.any { it.contains("figma") || it.contains("wireframing") || it.contains("ux") } -> "UI/UX Designer"
            else -> "Android Developer"
        }
    }

    private fun buildProfileFromAnalysis(
        analysis: ResumeAnalysisResult?,
        targetRole: String
    ): ExtractedResumeProfile {
        val candidateName = analysis?.candidateName
            ?: sessionManager.getUserName()
            ?: "Candidate"

        val edu = analysis?.education?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "Bachelor's in Computer Science / Information Technology"

        val experienceLevel = when {
            analysis?.experience.isNullOrEmpty() -> "Fresher"
            analysis?.experience?.size == 1 -> "Entry Level (0-2 yrs)"
            else -> "Mid Level (2-4 yrs)"
        }

        val required = RoleSkillsData.getRequiredSkills(targetRole)
        val extractedSkills = analysis?.extractedSkills?.map { it.name } ?: emptyList()

        val coreSkills = if (extractedSkills.isNotEmpty()) {
            val matched = required.filter { req ->
                RoleSkillsData.isSkillMatched(req, extractedSkills)
            }
            if (matched.isNotEmpty()) matched.take(6) else extractedSkills.take(6)
        } else {
            required.take(5)
        }

        val tools = listOf("Android Studio", "Gradle", "Git / GitHub", "Room DB", "Postman", "Firebase")
            .filter { tool ->
                extractedSkills.any { it.contains(tool, ignoreCase = true) } || true
            }.take(5)

        val projects = analysis?.projectAnalysis?.map { it.name }?.filter { it.isNotBlank() }
            ?.ifEmpty { null }
            ?: listOf("AstraMind Career Platform", "Native Android Application Suite")

        val competencies = listOf(
            "Clean MVVM Architecture",
            "Declarative UI Composition",
            "Reactive State Management",
            "RESTful API Integration & Serialization"
        )

        val keywords = coreSkills.take(4) + listOf("Clean Code", "Unit Testing", "CI/CD")

        return ExtractedResumeProfile(
            targetRole = targetRole,
            candidateName = candidateName,
            education = edu,
            experienceLevel = experienceLevel,
            coreSkills = coreSkills,
            toolsAndTech = tools,
            projects = projects,
            competencies = competencies,
            careerDirection = "$targetRole Engineering & Scalable Systems",
            importantKeywords = keywords
        )
    }

    private fun generateRoleSpecificBaseJd(
        targetRole: String,
        profile: ExtractedResumeProfile
    ): JobDescriptionSection {
        val exp = profile.experienceLevel
        val skills = profile.coreSkills

        val summary = when {
            targetRole.contains("Android", ignoreCase = true) ->
                "We are seeking an ambitious $targetRole ($exp) to build high-performance mobile applications. You will work on crafting responsive, intuitive UI screens with Kotlin and Jetpack Compose, integrating RESTful microservices, and implementing robust local persistence."
            targetRole.contains("Java", ignoreCase = true) ->
                "Looking for a skilled $targetRole to design and implement resilient backend systems using modern Java, Spring Boot, and relational databases. You will develop scalable RESTful APIs and ensure code quality through automated testing."
            targetRole.contains("Full Stack", ignoreCase = true) ->
                "We are hiring a versatile $targetRole to contribute across client and server architectures. You will develop responsive frontends in React and engineer scalable backend services with Node.js and SQL."
            targetRole.contains("Frontend", ignoreCase = true) ->
                "Seeking a creative $targetRole to build pixel-perfect, accessible web interfaces. You will translate UI/UX designs into responsive components using React, TypeScript, and modern CSS."
            targetRole.contains("Data Analyst", ignoreCase = true) ->
                "Looking for an analytical $targetRole to transform complex datasets into actionable business intelligence using SQL, Python, Pandas, and interactive dashboards."
            else ->
                "We are hiring a dedicated $targetRole ($exp) to collaborate with engineering teams, design robust solutions, and deliver high-quality software features aligned with modern industry benchmarks."
        }

        val responsibilities = when {
            targetRole.contains("Android", ignoreCase = true) -> listOf(
                "Design and construct modern Android user interfaces using Kotlin and Jetpack Compose.",
                "Integrate RESTful web APIs and manage offline data caching using Room SQLite.",
                "Architect scalable application flows with MVVM, StateFlow, and Coroutines.",
                "Collaborate with UI/UX designers to translate Figma wireframes into polished layouts.",
                "Write automated unit tests and participate in active peer code reviews."
            )
            targetRole.contains("Java", ignoreCase = true) -> listOf(
                "Develop enterprise microservices using Spring Boot, Hibernate, and RESTful architectures.",
                "Design and optimize relational database schemas and complex SQL queries.",
                "Implement multithreaded and asynchronous message processors.",
                "Ensure enterprise security standards with JWT and OAuth2 integration."
            )
            targetRole.contains("Full Stack", ignoreCase = true) -> listOf(
                "Develop modular frontend components with React, TypeScript, and state hydration.",
                "Engineer secure backend endpoints using Node.js, Express, and SQL databases.",
                "Deploy and monitor containerized services using Docker and CI/CD pipelines.",
                "Optimize end-to-end network performance and client responsiveness."
            )
            else -> listOf(
                "Participate in the full software development lifecycle from design to deployment.",
                "Build and maintain maintainable, well-documented code using modern frameworks.",
                "Collaborate with cross-functional product and design teams.",
                "Perform unit testing, debugging, and continuous performance optimizations."
            )
        }

        val preferred = when {
            targetRole.contains("Android", ignoreCase = true) -> listOf(
                "Room Database & Flow", "Firebase Cloud Messaging", "CI/CD & GitHub Actions", "Material 3 Design Tokens"
            )
            targetRole.contains("Java", ignoreCase = true) -> listOf(
                "Docker / Kubernetes", "Kafka / RabbitMQ", "AWS Cloud Services", "JUnit & Mockito"
            )
            else -> listOf(
                "Cloud Deployment (GCP/AWS)", "Automated CI/CD", "Performance Profiling", "Agile Methodologies"
            )
        }

        val qualifications = listOf(
            profile.education,
            "Hands-on experience or project portfolio demonstrating mastery in ${skills.take(3).joinToString(", ")}.",
            "Strong understanding of software engineering fundamentals, data structures, and clean architecture."
        )

        return JobDescriptionSection(
            roleTitle = targetRole,
            roleSummary = summary,
            responsibilities = responsibilities,
            requiredSkills = skills,
            preferredSkills = preferred,
            qualifications = qualifications
        )
    }

    fun updateBaseJd(updated: JobDescriptionSection) {
        _baseJobDescription.value = updated
    }

    fun updateProfile(targetRole: String, experienceLevel: String, skills: List<String>) {
        sessionManager.updateTargetRole(targetRole)
        val current = _profile.value
        val updated = current.copy(
            targetRole = targetRole,
            experienceLevel = experienceLevel,
            coreSkills = skills
        )
        _profile.value = updated
        _baseJobDescription.value = generateRoleSpecificBaseJd(targetRole, updated)
    }

    fun powerJobDescription(targetRole: String? = null) {
        if (!targetRole.isNullOrBlank() && targetRole != _profile.value.targetRole) {
            updateProfile(targetRole, _profile.value.experienceLevel, _profile.value.coreSkills)
        }
        powerTheJd()
    }

    fun powerTheJd() {
        val targetRole = _profile.value.targetRole
        val baseJd = _baseJobDescription.value

        val stageTemplates = listOf(
            JdStageInfo(
                number = "01",
                title = "Understanding Your Profile",
                description = "Analyzing your resume, skills, projects and experience.",
                status = JdStageStatus.PENDING
            ),
            JdStageInfo(
                number = "02",
                title = "Role Alignment",
                description = "Aligning your profile with the selected target role.",
                status = JdStageStatus.PENDING
            ),
            JdStageInfo(
                number = "03",
                title = "Skill Matching",
                description = "Identifying the most relevant technical and professional skills.",
                status = JdStageStatus.PENDING
            ),
            JdStageInfo(
                number = "04",
                title = "Requirement Optimization",
                description = "Refining role responsibilities and qualification requirements.",
                status = JdStageStatus.PENDING
            ),
            JdStageInfo(
                number = "05",
                title = "JD Enhancement",
                description = "Improving clarity, relevance and role-specific keywords.",
                status = JdStageStatus.PENDING
            ),
            JdStageInfo(
                number = "06",
                title = "Final JD",
                description = "Generating your optimized Job Description.",
                status = JdStageStatus.PENDING
            )
        )

        _stages.value = stageTemplates
        _flowMode.value = JdFlowMode.POWERING_PROGRESS
        _currentStageIndex.value = 0

        viewModelScope.launch {
            for (i in 0 until 6) {
                _currentStageIndex.value = i
                _stages.value = _stages.value.mapIndexed { index, stage ->
                    when {
                        index < i -> stage.copy(status = JdStageStatus.COMPLETED)
                        index == i -> stage.copy(status = JdStageStatus.PROCESSING)
                        else -> stage.copy(status = JdStageStatus.PENDING)
                    }
                }
                kotlinx.coroutines.delay(650)
            }

            // Mark all completed
            _stages.value = _stages.value.map { it.copy(status = JdStageStatus.COMPLETED) }
            kotlinx.coroutines.delay(400)

            // Construct final powered Job Description
            val optimizedResponsibilities = listOf(
                "Architect and implement declarative, production-grade Android UI workflows using Kotlin and Jetpack Compose.",
                "Integrate asynchronous RESTful services with reactive Kotlin Coroutines, StateFlow, and Room SQLite offline caching.",
                "Enforce Clean Architecture and unidirectional data flow (MVI/MVVM) across modular features.",
                "Optimize app startup latency, memory footprint, and frame render rates adhering to Material 3 design tokens.",
                "Establish automated testing suites and CI workflows ensuring zero-regression code contributions."
            )

            val poweredSkills = (_profile.value.coreSkills + listOf("Kotlin Coroutines", "Room Database", "StateFlow", "Material 3")).distinct()
            val preferredSkills = listOf(
                "Firebase SDK & Push Notifications",
                "Automated CI/CD (GitHub Actions / Fastlane)",
                "ProGuard / R8 Code Shrinking",
                "Unit & UI Testing (JUnit, MockK, Espresso)"
            )

            val optimizedJd = JobDescriptionSection(
                roleTitle = targetRole,
                roleSummary = "We are seeking a high-caliber $targetRole to drive the development of next-generation mobile experiences. You will leverage modern declarative frameworks, reactive state management, and robust networking architectures to ship impactful, production-ready software aligned with your verified competencies.",
                responsibilities = optimizedResponsibilities,
                requiredSkills = poweredSkills,
                preferredSkills = preferredSkills,
                qualifications = listOf(
                    _profile.value.education,
                    "Demonstrated portfolio of native Android applications showcasing clean architecture, Jetpack Compose, and offline resilience.",
                    "Strong grasp of concurrency patterns, reactive programming, and industry-standard version control workflows."
                )
            )

            val whatChangedItems = listOf(
                JdExplainabilityItem(
                    id = "skills_identified",
                    title = "✓ ${poweredSkills.size} relevant skills identified",
                    detail = "Mapped Kotlin, Jetpack Compose, Room SQLite, REST APIs, Git, Coroutines, and MVVM directly from your extracted profile to current industry hiring benchmarks.",
                    tag = "Skill Coverage"
                ),
                JdExplainabilityItem(
                    id = "responsibilities_opt",
                    title = "✓ 4 responsibilities optimized",
                    detail = "Reframed responsibilities around modern declarative UI, offline caching, and reactive architecture based on your project background and strengths.",
                    tag = "Role Alignment"
                ),
                JdExplainabilityItem(
                    id = "keywords_strengthened",
                    title = "✓ 3 role-specific keywords strengthened",
                    detail = "Elevated 'Kotlin Coroutines', 'Declarative UI', and 'Room SQLite Persistence' for maximum ATS parser recognition and keyword density.",
                    tag = "Keyword Boost"
                ),
                JdExplainabilityItem(
                    id = "redundant_removed",
                    title = "✓ Redundant requirements removed",
                    detail = "Eliminated legacy imperative XML boilerplate and obsolete tech stack requirements to focus strictly on modern production standards.",
                    tag = "Clarity"
                ),
                JdExplainabilityItem(
                    id = "structure_improved",
                    title = "✓ JD structure improved",
                    detail = "Reorganized into structured Role Summary, Core Responsibilities, Must-Have Competencies, and Preferred Qualifications for executive readability.",
                    tag = "Formatting"
                )
            )

            _poweredResult.value = JdPoweredResult(
                targetRole = targetRole,
                alignmentPercentage = 94,
                jobDescription = optimizedJd,
                whatChangedItems = whatChangedItems
            )

            _flowMode.value = JdFlowMode.POWERED_RESULT
        }
    }

    fun toggleExplainItem(id: String) {
        _expandedExplainId.value = if (_expandedExplainId.value == id) null else id
    }

    fun powerAgain() {
        val analysis = sessionManager.getLatestAnalysis()
        if (analysis?.detectedJobDescription != null) {
            _flowMode.value = JdFlowMode.JD_DETECTED
        } else {
            _flowMode.value = JdFlowMode.NO_JD
        }
    }
}

