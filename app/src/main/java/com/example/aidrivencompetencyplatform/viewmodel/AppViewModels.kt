package com.example.aidrivencompetencyplatform.viewmodel

import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import java.io.FileOutputStream
import java.io.File
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidrivencompetencyplatform.data.AtsScoringEngine
import com.example.aidrivencompetencyplatform.data.DeterministicResumeStructure
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

        val analysis = sessionManager.getLatestAnalysis()
        _latestAnalysis.value = analysis
        if (analysis != null) {
            _hasAnalysis.value = true
            _resumeScore.value = analysis.overallScore
            _atsScore.value = analysis.atsScore
            _skillMatch.value = analysis.skillMatch
            
            // Prioritize target role from analysis record
            val currentRole = analysis.targetRole ?: sessionManager.getTargetRole() ?: "Android Developer"
            _targetRole.value = currentRole
            
            // Fix NPE by using safe calls. Gson can set fields to null if missing in JSON.
            val extracted = analysis.extractedSkills ?: emptyList()
            val missing = analysis.missingSections ?: emptyList()
            
            val matchingSkills = extracted.map { it.name }.take(3)
            val missingItems = missing.take(1)

            _jobRecommendations.value = listOf(
                JobRecommendation(
                    currentRole,
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
            
            val storedRole = sessionManager.getTargetRole()
            if (!storedRole.isNullOrBlank()) {
                _targetRole.value = storedRole
            }
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
        get() = _analysisResult.value?.targetRole ?: sessionManager.getTargetRole() ?: "Android Developer"

    init {
        val latest = sessionManager.getLatestAnalysis()
        _analysisResult.value = latest
        if (latest != null) {
            loadSavedAtsScores(latest, if (latest.atsScore > 0) latest.atsScore else null)
        } else {
            calculateAtsScores()
        }
    }

    fun refresh() {
        val latest = sessionManager.getLatestAnalysis()
        _analysisResult.value = latest
        if (latest != null) {
            loadSavedAtsScores(latest, if (latest.atsScore > 0) latest.atsScore else null)
        } else {
            calculateAtsScores()
        }
    }

    fun loadAnalysis(id: String?) {
        if (id.isNullOrBlank()) {
            refresh()
            return
        }
        val historyRecord = sessionManager.getAnalysisHistory().find { it.id == id }
        val analysis = sessionManager.getAnalysisById(id) ?: historyRecord?.fullResult
        
        if (analysis != null) {
            val targetScore = historyRecord?.atsScore?.takeIf { it > 0 } ?: (if (analysis.atsScore > 0) analysis.atsScore else null)
            val finalAnalysis = if (targetScore != null && analysis.atsScore != targetScore) {
                analysis.copy(atsScore = targetScore)
            } else {
                analysis
            }
            _analysisResult.value = finalAnalysis
            loadSavedAtsScores(finalAnalysis, targetScore)
        } else if (historyRecord != null) {
            val reconstructed = ResumeAnalysisResult(
                id = historyRecord.id,
                overallScore = historyRecord.atsScore,
                atsScore = historyRecord.atsScore,
                skillMatch = historyRecord.skillMatch,
                targetRole = historyRecord.targetRole,
                candidateName = historyRecord.candidateName,
                extractedSkills = historyRecord.topSkills.map { com.example.aidrivencompetencyplatform.model.Skill(name = it, level = 85) }
            )
            _analysisResult.value = reconstructed
            loadSavedAtsScores(reconstructed, historyRecord.atsScore)
        } else {
            refresh()
        }
    }

    private fun loadSavedAtsScores(analysis: ResumeAnalysisResult, forcedOverallScore: Int? = null) {
        val effectiveScore = forcedOverallScore ?: (if (analysis.atsScore > 0) analysis.atsScore else null)
        val calculated = scoringEngine.calculateScore(analysis, targetRole, forcedOverallScore = effectiveScore)
        _scoreResult.value = calculated
        _overallAtsScore.value = calculated.overallScore
        _atsBreakdownState.value = AtsBreakdown(
            keywordCoverage = calculated.keywordCoverage.score,
            resumeStructure = calculated.resumeStructure.score,
            formattingSafety = calculated.formattingSafety.score,
            parsingAccuracy = calculated.parsingAccuracy.score
        )
    }

    private fun calculateAtsScores() {
        val analysis = _analysisResult.value
        if (analysis == null) {
            _scoreResult.value = null
            _overallAtsScore.value = 0
            _atsBreakdownState.value = AtsBreakdown()
            return
        }
        loadSavedAtsScores(analysis, if (analysis.atsScore > 0) analysis.atsScore else null)
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
    val targetRole: String = "",
    val recommendedCourse: RecommendedCourse? = null
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
    val remainingMissingSkills: List<DetailedSkillItem> = emptyList(),
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

    fun getRecommendedFreeCourse(
        skill: String,
        targetRole: String = "",
        customExplanation: String? = null
    ): RecommendedCourse {
        val s = skill.trim().lowercase()
        val defaultExp = customExplanation ?: "Recommended because $skill is listed in the job description but is not sufficiently demonstrated in your resume."

        return when {
            s == "kotlin" -> RecommendedCourse(
                title = "Android Basics with Compose (Kotlin)",
                provider = "Google Developers & Android Official",
                description = "Master Kotlin syntax, coroutines, and modern Android development with official hands-on codelabs.",
                courseUrl = "https://developer.android.com/courses/android-basics-compose/course",
                originalTitle = "Android Basics with Compose (Kotlin)",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "jetpack compose" -> RecommendedCourse(
                title = "Jetpack Compose Fundamentals",
                provider = "Google Android Developers",
                description = "Learn declarative UI architecture, state management, layouts, and animations in Jetpack Compose.",
                courseUrl = "https://developer.android.com/develop/ui/compose/tutorial",
                originalTitle = "Jetpack Compose Tutorial & Codelabs",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "android sdk" || s == "android studio" || s == "android" -> RecommendedCourse(
                title = "Android App Development with Kotlin",
                provider = "Google Developers Training",
                description = "Learn Activity/Fragment lifecycles, background tasks, WorkManager, and Android system services.",
                courseUrl = "https://developer.android.com/courses",
                originalTitle = "Android App Development with Kotlin",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "room database" || s == "room" -> RecommendedCourse(
                title = "Room Database & Persistence",
                provider = "Android Developers Codelabs",
                description = "Learn SQLite persistence, DAOs, entities, and Flow-based reactive queries in Android.",
                courseUrl = "https://developer.android.com/codelabs/basic-android-kotlin-compose-persisting-data-room",
                originalTitle = "Persisting Data with Room Database",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "java" || s == "oop" || s == "collections" || s == "multithreading" || s == "exception handling" -> RecommendedCourse(
                title = "Java Programming Fundamentals",
                provider = "University of Helsinki (MOOC.fi)",
                description = "Master object-oriented programming, data structures, multithreading, and enterprise Java principles.",
                courseUrl = "https://java-programming.mooc.fi/",
                originalTitle = "Java Programming Comprehensive Course",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "spring" || s == "spring boot" || s == "hibernate" -> RecommendedCourse(
                title = "Spring Boot Microservices",
                provider = "Spring.io Official Guides",
                description = "Build production-ready RESTful web services, dependency injection, and data persistence with Spring Boot.",
                courseUrl = "https://spring.io/guides/gs/spring-boot/",
                originalTitle = "Building Microservices with Spring Boot",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "python" -> RecommendedCourse(
                title = "Python Programming Fundamentals",
                provider = "Harvard University (edX / Free)",
                description = "Learn foundational and advanced Python syntax, data structures, libraries, and automated testing.",
                courseUrl = "https://cs50.harvard.edu/python/",
                originalTitle = "CS50's Introduction to Programming with Python",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "sql" || s == "sqlite" || s == "jdbc" -> RecommendedCourse(
                title = "SQL & Relational Databases",
                provider = "Khan Academy & Mode Analytics",
                description = "Master relational database querying, joins, aggregations, subqueries, and table indexing.",
                courseUrl = "https://www.khanacademy.org/computing/computer-programming/sql",
                originalTitle = "Intro to SQL: Querying and Managing Data",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "postgresql" -> RecommendedCourse(
                title = "PostgreSQL Developer Guide",
                provider = "PostgreSQL Official / Tutorial",
                description = "Learn advanced SQL queries, JSONB indexing, ACID transactions, and performance tuning in Postgres.",
                courseUrl = "https://www.postgresqltutorial.com/",
                originalTitle = "PostgreSQL Tutorial for Developers",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "mongodb" -> RecommendedCourse(
                title = "MongoDB Developer Learning Path",
                provider = "MongoDB University",
                description = "Learn document database modeling, aggregation pipelines, indexing, and CRUD operations.",
                courseUrl = "https://learn.mongodb.com/",
                originalTitle = "MongoDB Developer Learning Path",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "react" || s == "react.js" -> RecommendedCourse(
                title = "React Fundamentals",
                provider = "React.dev (Meta Open Source)",
                description = "Learn modern component architecture, hooks (useState, useEffect), and reactive state rendering.",
                courseUrl = "https://react.dev/learn",
                originalTitle = "React Official Interactive Tutorial",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "node.js" || s == "express.js" || s == "node" || s == "express" -> RecommendedCourse(
                title = "Node.js & Express Backend",
                provider = "freeCodeCamp",
                description = "Learn server-side JavaScript, Express routing, middleware, authentication, and REST APIs.",
                courseUrl = "https://www.freecodecamp.org/learn/back-end-development-and-apis/",
                originalTitle = "Back End Development and APIs with Node.js",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "javascript" -> RecommendedCourse(
                title = "JavaScript Fundamentals",
                provider = "freeCodeCamp",
                description = "Master ES6+ syntax, asynchronous JavaScript (Promises/Async-Await), and DOM manipulation.",
                courseUrl = "https://www.freecodecamp.org/learn/javascript-algorithms-and-data-structures-v8/",
                originalTitle = "JavaScript Algorithms and Data Structures",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "typescript" -> RecommendedCourse(
                title = "TypeScript Fundamentals",
                provider = "Microsoft TypeScript Official",
                description = "Master static typing, generics, utility types, and type inference for robust application design.",
                courseUrl = "https://www.typescriptlang.org/docs/handbook/intro.html",
                originalTitle = "TypeScript Handbook & Practical Guide",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "html" || s == "css" || s == "responsive design" -> RecommendedCourse(
                title = "Responsive Web Design Fundamentals",
                provider = "freeCodeCamp",
                description = "Learn HTML5 semantic structuring, CSS Flexbox, Grid, and mobile-first responsive design.",
                courseUrl = "https://www.freecodecamp.org/learn/2022/responsive-web-design/",
                originalTitle = "Responsive Web Design Certification",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "git" || s == "github" -> RecommendedCourse(
                title = "Git & GitHub Version Control",
                provider = "Git-SCM Documentation & freeCodeCamp",
                description = "Learn branching, merge strategies, interactive rebasing, pull requests, and CI workflows.",
                courseUrl = "https://git-scm.com/doc",
                originalTitle = "Version Control with Git & GitHub",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "docker" -> RecommendedCourse(
                title = "Docker & Containerization",
                provider = "Docker Official Documentation",
                description = "Learn Dockerfile authoring, multi-stage builds, container networking, and Docker Compose.",
                courseUrl = "https://docs.docker.com/get-started/",
                originalTitle = "Docker for Beginners & Containerization",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "machine learning" || s == "scikit-learn" || s == "model evaluation" -> RecommendedCourse(
                title = "Machine Learning Fundamentals",
                provider = "Kaggle Learn / freeCodeCamp",
                description = "Learn supervised learning, regression, classification, cross-validation, and model evaluation.",
                courseUrl = "https://www.kaggle.com/learn/intro-to-machine-learning",
                originalTitle = "Intro to Machine Learning with Python",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "deep learning" || s == "pytorch" -> RecommendedCourse(
                title = "PyTorch Deep Learning",
                provider = "PyTorch.org Official Tutorials",
                description = "Learn neural networks, backpropagation, CNNs, RNNs, and Transformers with PyTorch.",
                courseUrl = "https://pytorch.org/tutorials/",
                originalTitle = "Deep Learning with PyTorch Tutorial",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "tensorflow" -> RecommendedCourse(
                title = "TensorFlow Core Fundamentals",
                provider = "Google TensorFlow",
                description = "Build and train neural networks, Keras sequential models, and model export pipelines.",
                courseUrl = "https://www.tensorflow.org/tutorials",
                originalTitle = "TensorFlow Core Tutorials",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "pandas" || s == "numpy" || s == "data cleaning" -> RecommendedCourse(
                title = "Pandas Data Manipulation",
                provider = "Kaggle Learn",
                description = "Master DataFrame transformations, exploratory data analysis, filtering, and indexing.",
                courseUrl = "https://www.kaggle.com/learn/pandas",
                originalTitle = "Pandas & Data Manipulation Course",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "power bi" -> RecommendedCourse(
                title = "Power BI Guided Learning",
                provider = "Microsoft Learn",
                description = "Learn data modeling, DAX formulas, interactive visual dashboards, and report publication.",
                courseUrl = "https://learn.microsoft.com/en-us/training/powerplatform/power-bi",
                originalTitle = "Microsoft Power BI Guided Learning",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "tableau" -> RecommendedCourse(
                title = "Tableau Analytics & Dashboards",
                provider = "Tableau Official Learning",
                description = "Build interactive business dashboards, geospatial maps, and predictive trend charts.",
                courseUrl = "https://www.tableau.com/learn/training",
                originalTitle = "Tableau Free Training & Data Visuals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "statistics" || s == "probability" -> RecommendedCourse(
                title = "Statistics & Probability",
                provider = "Khan Academy",
                description = "Learn descriptive statistics, probability distributions, hypothesis testing, and regression analysis.",
                courseUrl = "https://www.khanacademy.org/math/statistics-probability",
                originalTitle = "Statistics and Probability for Data Science",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "data visualization" -> RecommendedCourse(
                title = "Data Visualization Fundamentals",
                provider = "Kaggle Learn",
                description = "Create impactful charts using Seaborn, Matplotlib, and plot styling best practices.",
                courseUrl = "https://www.kaggle.com/learn/data-visualization",
                originalTitle = "Data Visualization Micro-Course",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "model deployment" || s == "mlops" -> RecommendedCourse(
                title = "MLOps & Model Deployment",
                provider = "Google Cloud / freeCodeCamp",
                description = "Deploy ML models as scalable REST endpoints using FastAPI, Docker, and cloud runtimes.",
                courseUrl = "https://www.freecodecamp.org/news/how-to-deploy-machine-learning-models/",
                originalTitle = "Machine Learning Model Deployment Guide",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "feature engineering" -> RecommendedCourse(
                title = "Feature Engineering Guide",
                provider = "Kaggle Learn",
                description = "Learn mutual information, target encoding, principal component analysis (PCA), and scaling.",
                courseUrl = "https://www.kaggle.com/learn/feature-engineering",
                originalTitle = "Feature Engineering for Machine Learning",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "firebase" -> RecommendedCourse(
                title = "Firebase Cloud & Auth",
                provider = "Google Firebase Guides",
                description = "Integrate Firestore, Firebase Authentication, Cloud Functions, and push notifications.",
                courseUrl = "https://firebase.google.com/docs/guides",
                originalTitle = "Firebase Fundamentals for Developers",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "figma" || s == "wireframing" || s == "prototyping" || s == "design systems" || s == "user flows" || s == "interaction design" || s == "visual design" || s == "typography" || s == "color theory" || s == "usability testing" || s == "user research" -> RecommendedCourse(
                title = "Figma & UI/UX Design",
                provider = "Figma Official Resource Library",
                description = "Master wireframing, interactive prototyping, auto-layout, design tokens, and usability testing.",
                courseUrl = "https://www.figma.com/resource-library/learn-figma/",
                originalTitle = "Figma for Beginners & UI/UX Design",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "rest apis" || s == "json" -> RecommendedCourse(
                title = "REST API Architecture",
                provider = "MDN Web Docs",
                description = "Learn HTTP methods, status codes, REST conventions, JSON serialization, and API security.",
                courseUrl = "https://developer.mozilla.org/en-US/docs/Learn/Server-side/First_steps/Web_frameworks",
                originalTitle = "RESTful API Design and Web Architecture",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "material design" || s == "xml" -> RecommendedCourse(
                title = "Material Design Guidelines",
                provider = "Google Material Design",
                description = "Learn accessible UI styling, dynamic color, typography scales, and component guidelines.",
                courseUrl = "https://m3.material.io/",
                originalTitle = "Material Design 3 Architecture & Guidelines",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "gradle" || s == "maven" -> RecommendedCourse(
                title = "Gradle & Maven Automation",
                provider = "Gradle Guides / Baeldung",
                description = "Learn build lifecycles, dependency management, plugins, and multi-module configurations.",
                courseUrl = "https://docs.gradle.org/current/userguide/getting_started.html",
                originalTitle = "Build Automation with Gradle & Maven",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "authentication" -> RecommendedCourse(
                title = "Web Security & Auth",
                provider = "MDN Web Docs",
                description = "Learn JWT tokens, OAuth 2.0 flows, session management, and credential security.",
                courseUrl = "https://developer.mozilla.org/en-US/docs/Learn/Server-side/First_steps/Website_security",
                originalTitle = "Web Security & Authentication Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            else -> {
                val cleanTitle = if (skill.length > 28) "$skill Fundamentals" else "$skill Fundamentals & Best Practices"
                RecommendedCourse(
                    title = cleanTitle,
                    provider = "freeCodeCamp / Developer Docs",
                    description = "Learn core concepts, practical syntax, and architectural patterns to master $skill for $targetRole.",
                    courseUrl = "https://www.freecodecamp.org/news/search/?query=" + java.net.URLEncoder.encode(skill, "UTF-8"),
                    originalTitle = "$skill Fundamentals & Best Practices",
                    missingSkill = skill,
                    explanation = defaultExp
                )
            }
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
        loadAnalysisResult(analysis)
    }

    fun loadAnalysis(id: String?) {
        if (id.isNullOrBlank()) {
            refresh()
            return
        }
        val analysis = sessionManager.getAnalysisById(id)
        if (analysis != null) {
            loadAnalysisResult(analysis)
        } else {
            refresh()
        }
    }

    private fun loadAnalysisResult(analysis: ResumeAnalysisResult?) {
        _analysisResult.value = analysis

        val role = analysis?.targetRole ?: sessionManager.getTargetRole() ?: "Android Developer"
        
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
            val course = RoleSkillsData.getRecommendedFreeCourse(reqSkill, role)

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
                    targetRole = role,
                    recommendedCourse = course
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
                    targetRole = role,
                    recommendedCourse = course
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

        val top3MissingNames = priorityItems.take(3).map { it.name }.toSet()
        val remainingMissing = missingList.filterNot { it.name in top3MissingNames }

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
            remainingMissingSkills = remainingMissing,
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
                targetRole = skill.targetRole,
                recommendedCourse = RoleSkillsData.getRecommendedFreeCourse(skill.name, skill.targetRole)
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
    private val atsScoringEngine = AtsScoringEngine()
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
        val localUri = resumeParser.saveUriToLocalCache(uri)
        if (localUri != null) {
            selectedUri = localUri
            sampleResumeText = null
            _selectedFileName.value = name
            _errorMessage.value = null
        } else {
            _errorMessage.value = "Failed to access the selected file. Please try again."
        }
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
        
        if (selectedUri == null && sampleResumeText == null) {
            _errorMessage.value = "Please select a resume file (PDF/DOCX) or click 'Sample Resume' first."
            return
        }
        
        // 1. CLEAR ALL PREVIOUS STATE - No leakage between resumes
        _analysisResult.value = null
        _isLoading.value = true
        _errorMessage.value = null
        _analyzingRole.value = effectiveRole
        _stageIndex.value = 0
        _loadingStage.value = "Starting analysis..."
        
        // Initial real-time state: resetting progress for new analysis
        _interactiveState.value = InteractiveAnalysisState(
            atsComplete = false,
            skillGapComplete = false,
            jdMatchingComplete = false,
            isComplete = false,
            stage = 0,
            targetRole = effectiveRole
        )
        
        viewModelScope.launch {
            try {
                // 2. EXTRACTION STAGE
                _loadingStage.value = "Extracting resume information..."
                _stageIndex.value = 1
                _interactiveState.value = _interactiveState.value.copy(stage = 1)
                
                val text = withContext(Dispatchers.IO) {
                    if (selectedUri != null) {
                        resumeParser.extractText(selectedUri!!)
                    } else {
                        sampleResumeText ?: ""
                    }
                }

                val layout = withContext(Dispatchers.IO) {
                    if (selectedUri != null) {
                        resumeParser.extractLayout(selectedUri!!)
                    } else null
                }

                if (text == ResumeParser.ERROR_SCANNED_PDF) {
                    throw Exception("This PDF appears to be a scanned image. Please upload a text-based PDF or DOCX file for accurate analysis.")
                }
                if (text.isBlank()) {
                    throw Exception("Could not extract any text from the selected file. Please ensure the file is not empty or corrupted.")
                }

                // 3. PARSING STAGE
                _loadingStage.value = "Parsing candidate profile..."
                _stageIndex.value = 2
                _interactiveState.value = _interactiveState.value.copy(stage = 2)
                
                // Deterministic parsing for structure on background dispatcher
                val structure = withContext(Dispatchers.Default) {
                    resumeParser.parseResumeStructure(text)
                }
                
                // 4. AI ANALYSIS STAGE
                _loadingStage.value = "Analyzing competencies with AI..."
                _stageIndex.value = 3
                _interactiveState.value = _interactiveState.value.copy(stage = 3)
                
                // AI Parsing and Analysis
                Log.d("ResumeNetworkDebug", "Starting Gemini analysis for role: $effectiveRole")
                val aiResult = try {
                    val result = geminiService.analyzeResume(text, effectiveRole)
                    if (result != null) {
                        Log.d("ResumeNetworkDebug", "Gemini analysis completed successfully")
                        result
                    } else {
                        Log.w("ResumeNetworkDebug", "Gemini analysis returned null, synthesizing from structure")
                        synthesizeAnalysisResult(text, effectiveRole, structure)
                    }
                } catch (e: Exception) {
                    Log.w("ResumeNetworkDebug", "Gemini analysis encountered error (${e.message}), synthesizing from structure", e)
                    synthesizeAnalysisResult(text, effectiveRole, structure)
                }

                // Merge deterministic structure with AI results, prioritizing valid details
                // If Gemini succeeded (did not return synthesize result), we trust its classification of empty lists.
                val isSynthesized = aiResult.summary == "Synthesized Analysis" // Heuristic for fallback

                fun sanitizeCandidate(value: String?): String? {
                    if (value.isNullOrBlank()) return null
                    val clean = value.trim()
                    val lower = clean.lowercase()
                    val invalid = setOf(
                        "null", "none", "n/a", "na", "not detected", "not specified",
                        "not provided", "not found", "unknown", "nil", "-", "--", "undefined", "empty"
                    )
                    return if (invalid.contains(lower) || lower.startsWith("not detected") || lower.startsWith("not found")) null else clean
                }

                val candidateName = sanitizeCandidate(aiResult.candidateName) ?: sanitizeCandidate(structure.candidateInfo.name)
                val candidateEmail = sanitizeCandidate(aiResult.candidateEmail) ?: sanitizeCandidate(structure.candidateInfo.email)
                val candidatePhone = sanitizeCandidate(aiResult.candidatePhone) ?: sanitizeCandidate(structure.candidateInfo.phone)
                val candidateLocation = sanitizeCandidate(aiResult.candidateLocation) ?: sanitizeCandidate(structure.candidateInfo.location)

                val effectiveSummary = aiResult.summary?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("Synthesized Analysis", ignoreCase = true) }
                    ?: structure.summary?.takeIf { it.isNotBlank() }
                    ?: ""

                // Reconcile Education so 10th, 12th, and College qualifications are never truncated
                val effectiveEducation = if (!aiResult.education.isNullOrEmpty() && !isSynthesized) {
                    val combined = mutableListOf<String>()
                    combined.addAll(aiResult.education)
                    // Check if deterministic parser captured 10th/12th that AI might have missed
                    structure.education.forEach { detEdu ->
                        val detLower = detEdu.lowercase()
                        val isSchoolTier = detLower.contains("10th") || detLower.contains("12th") || 
                                          detLower.contains("sslc") || detLower.contains("hsc") || 
                                          detLower.contains("class x") || detLower.contains("class xii") ||
                                          detLower.contains("matriculation") || detLower.contains("secondary")
                        val alreadyPresent = combined.any { existing ->
                            val exLower = existing.lowercase()
                            (isSchoolTier && (exLower.contains("10th") || exLower.contains("12th") || exLower.contains("sslc") || exLower.contains("hsc"))) ||
                            exLower.contains(detLower.take(15))
                        }
                        if (isSchoolTier && !alreadyPresent) {
                            combined.add(detEdu)
                        }
                    }
                    combined
                } else if (structure.education.isNotEmpty()) {
                    structure.education
                } else {
                    emptyList()
                }

                // Work Experience: strict isolation (only genuine employment/internships)
                val effectiveExperience = if (!aiResult.experience.isNullOrEmpty() && !isSynthesized) {
                    aiResult.experience
                } else if (structure.experience.isNotEmpty()) {
                    structure.experience
                } else {
                    emptyList()
                }

                // Projects: strict isolation (only genuine projects)
                val effectiveProjects = if (!aiResult.projectAnalysis.isNullOrEmpty() && !isSynthesized) {
                    aiResult.projectAnalysis
                } else if (structure.projectAnalyses.isNotEmpty()) {
                    structure.projectAnalyses
                } else if (structure.extractedProjects.isNotEmpty()) {
                    structure.extractedProjects.map { projStr ->
                        ProjectAnalysis(
                            name = projStr,
                            technologies = emptyList(),
                            demonstratedSkills = emptyList(),
                            strengths = listOf("Project verified from resume"),
                            weaknesses = emptyList(),
                            improvementSuggestions = listOf("Add measurable performance metrics")
                        )
                    }
                } else {
                    emptyList()
                }

                val enrichedResult = aiResult.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    candidateName = candidateName,
                    candidateEmail = candidateEmail,
                    candidatePhone = candidatePhone,
                    candidateLocation = candidateLocation,
                    summary = effectiveSummary,
                    education = effectiveEducation,
                    experience = effectiveExperience,
                    extractedSkills = if (!aiResult.extractedSkills.isNullOrEmpty() && !isSynthesized) aiResult.extractedSkills
                        else structure.extractedSkills.map { skillStr -> com.example.aidrivencompetencyplatform.model.Skill(name = skillStr, level = 80, category = RoleSkillsData.getSkillCategory(skillStr)) },
                    projectAnalysis = effectiveProjects,
                    rawResumeText = text,
                    targetRole = effectiveRole,
                    detectedJobDescription = aiResult.detectedJobDescription ?: structure.detectedJobDescription,
                    layoutInfo = layout
                )

                val atsBreakdown = enrichedResult.atsBreakdown ?: com.example.aidrivencompetencyplatform.model.AtsBreakdown()

                // 5. UPDATE PROGRESS MODULES
                
                // Module 1: ATS Analysis
                _interactiveState.value = _interactiveState.value.copy(
                    atsComplete = true,
                    stage = 4,
                    atsScore = enrichedResult.atsScore,
                    keywordScore = atsBreakdown.keywordCoverage,
                    structureScore = atsBreakdown.resumeStructure,
                    formattingScore = atsBreakdown.formattingSafety,
                    parsingScore = atsBreakdown.parsingAccuracy,
                    candidateName = enrichedResult.candidateName ?: "Not found",
                    sections = structure.sections,
                    extractedSkills = enrichedResult.extractedSkills?.map { it.name } ?: structure.extractedSkills,
                    detectedProjects = enrichedResult.projectAnalysis?.map { it.name } ?: structure.extractedProjects
                )
                
                delay(100) // Reduced delay for smoother UI but faster processing
                _loadingStage.value = "Evaluating skill gaps..."
                _stageIndex.value = 4

                // Module 2: Skill Gap Analysis
                val matchedCount = enrichedResult.extractedSkills?.count { it.level >= 70 } ?: 0
                val missingCount = enrichedResult.extractedSkills?.count { it.level < 40 } ?: 0
                _interactiveState.value = _interactiveState.value.copy(
                    skillGapComplete = true,
                    stage = 5,
                    matchedSkillsCount = matchedCount,
                    missingSkillsCount = missingCount
                )
                
                delay(100)
                _loadingStage.value = "Calculating JD alignment..."
                _stageIndex.value = 5

                // Module 3: JD Matching
                _interactiveState.value = _interactiveState.value.copy(
                    jdMatchingComplete = true,
                    stage = 6,
                    skillMatch = enrichedResult.skillMatch
                )

                // Save results
                sessionManager.updateTargetRole(effectiveRole)
                sessionManager.saveLatestAnalysis(enrichedResult, _selectedFileName.value)
                
                _analysisResult.value = enrichedResult
                Log.d(TAG, "ANALYSIS_COMPLETED_SUCCESSFULLY")

                // 6. FINAL COMPLETION - Trigger UI to show "Complete"
                _loadingStage.value = "Analysis complete!"
                _stageIndex.value = 6
                _interactiveState.value = _interactiveState.value.copy(
                    isComplete = true,
                    stage = 7
                )

            } catch (e: Exception) {
                Log.e(TAG, "ANALYSIS_FAILED", e)
                _errorMessage.value = e.localizedMessage ?: "An unexpected error occurred during analysis."
                _interactiveState.value = _interactiveState.value.copy(isComplete = false)
                _isLoading.value = false // Hide loading on error only
            }
        }
    }

    private fun synthesizeAnalysisResult(
        text: String,
        targetRole: String,
        structure: DeterministicResumeStructure
    ): ResumeAnalysisResult {
        val candidateInfo = structure.candidateInfo
        val extractedSkills = structure.extractedSkills
        val extractedProjects = structure.extractedProjects
        val skillsList = extractedSkills.map { s ->
            Skill(
                name = s,
                level = 80,
                category = RoleSkillsData.getSkillCategory(s),
                evidence = "Found in resume profile"
            )
        }

        val projectsList = if (structure.projectAnalyses.isNotEmpty()) {
            structure.projectAnalyses
        } else {
            extractedProjects.map { p ->
                ProjectAnalysis(
                    name = p,
                    technologies = emptyList(),
                    demonstratedSkills = emptyList(),
                    strengths = listOf("Project highlighted in candidate resume"),
                    weaknesses = emptyList(),
                    improvementSuggestions = listOf("Add quantifiable outcome metrics and impact")
                )
            }
        }

        val tempResult = ResumeAnalysisResult(
            id = java.util.UUID.randomUUID().toString(),
            overallScore = 0,
            atsScore = 0,
            skillMatch = 0,
            targetRole = targetRole,
            candidateName = candidateInfo.name,
            candidateEmail = candidateInfo.email,
            candidatePhone = candidateInfo.phone,
            candidateLocation = candidateInfo.location,
            rawResumeText = text,
            summary = structure.summary ?: "",
            extractedSkills = skillsList,
            education = structure.education,
            experience = structure.experience,
            certifications = structure.certifications,
            projectAnalysis = projectsList
        )
        val atsScoreResult = atsScoringEngine.calculateScore(tempResult, targetRole)

        val atsBreakdown = AtsBreakdown(
            keywordCoverage = atsScoreResult.keywordCoverage.score,
            resumeStructure = atsScoreResult.resumeStructure.score,
            formattingSafety = atsScoreResult.formattingSafety.score,
            parsingAccuracy = atsScoreResult.parsingAccuracy.score,
            explanations = AtsExplanations(
                keywords = atsScoreResult.keywordCoverage.evidenceItems.map { AtsEvidence(it.label, it.isPositive, it.detail, it.suggestion) },
                structure = atsScoreResult.resumeStructure.evidenceItems.map { AtsEvidence(it.label, it.isPositive, it.detail, it.suggestion) },
                formatting = atsScoreResult.formattingSafety.evidenceItems.map { AtsEvidence(it.label, it.isPositive, it.detail, it.suggestion) },
                parsing = atsScoreResult.parsingAccuracy.evidenceItems.map { AtsEvidence(it.label, it.isPositive, it.detail, it.suggestion) }
            )
        )

        val highPrioritySuggestions = mutableListOf<String>()
        val mediumPrioritySuggestions = mutableListOf<String>()
        val lowPrioritySuggestions = mutableListOf<String>()

        atsScoreResult.keywordCoverage.evidenceItems.filter { !it.isPositive }.forEach {
            highPrioritySuggestions.add(it.suggestion ?: "Incorporate missing skill keyword: ${it.label}")
        }
        atsScoreResult.resumeStructure.evidenceItems.filter { !it.isPositive }.forEach {
            mediumPrioritySuggestions.add(it.suggestion ?: "Add missing standard section: ${it.label}")
        }
        atsScoreResult.formattingSafety.evidenceItems.filter { !it.isPositive }.forEach {
            lowPrioritySuggestions.add(it.suggestion ?: "Review formatting for: ${it.label}")
        }

        return ResumeAnalysisResult(
            id = java.util.UUID.randomUUID().toString(),
            overallScore = atsScoreResult.overallScore,
            atsScore = atsScoreResult.overallScore,
            skillMatch = atsScoreResult.keywordCoverage.score,
            targetRole = targetRole,
            summary = structure.summary ?: "",
            candidateName = candidateInfo.name,
            candidateEmail = candidateInfo.email,
            candidatePhone = candidateInfo.phone,
            candidateLocation = candidateInfo.location,
            education = structure.education,
            experience = structure.experience,
            extractedSkills = skillsList,
            certifications = structure.certifications,
            projectAnalysis = projectsList,
            atsBreakdown = atsBreakdown,
            missingSections = atsScoreResult.resumeStructure.evidenceItems.filter { !it.isPositive }.map { it.label },
            formattingRisks = atsScoreResult.formattingSafety.evidenceItems.filter { !it.isPositive }.map { it.label },
            prioritizedSuggestions = PrioritizedSuggestions(
                highPriority = highPrioritySuggestions.ifEmpty { listOf("Add measurable impact metrics to work experience bullet points") },
                mediumPriority = mediumPrioritySuggestions.ifEmpty { listOf("Enhance technical skills categorization by framework and tool") },
                lowPriority = lowPrioritySuggestions.ifEmpty { listOf("Keep font styles consistent across all sections") }
            ),
            parsingAccuracyDetails = ParsingAccuracyDetails(
                nameDetected = candidateInfo.name != null,
                contactDetected = candidateInfo.email != null || candidateInfo.phone != null,
                educationDetected = structure.education.isNotEmpty(),
                skillsDetected = extractedSkills.isNotEmpty(),
                experienceDetected = structure.experience.isNotEmpty(),
                projectsDetected = extractedProjects.isNotEmpty()
            ),
            rawResumeText = text
        )
    }

    fun resetAnalysisResult() {
        _analysisResult.value = null
    }

    fun reset() {
        selectedUri = null
        _selectedFileName.value = null
        _analysisResult.value = null
        _errorMessage.value = null
        _loadingStage.value = ""
        _stageIndex.value = 0
        _analyzingRole.value = ""
        _isLoading.value = false
    }

    fun finishLoading() {
        _isLoading.value = false
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

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    init {
        initDefaultChat()
    }

    private fun initDefaultChat() {
        val userName = sessionManager.getUserName() ?: "there"
        _messages.value = listOf(
            ChatMessage("Hello $userName! I'm Nova, your AI Career Assistant. How can I help you elevate your career today?", false)
        )
    }

    fun startTour() {
        _isTourMode.value = true
        _isInterviewMode.value = false
        val userName = sessionManager.getUserName() ?: "there"
        _messages.value = listOf(
            ChatMessage(
                text = "Welcome to NoviQ, $userName! 🌟 I'm Nova, your dedicated Career Intelligence Companion.\n\n" +
                        "Here is what NoviQ does for you:\n" +
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
            _isTyping.value = true
            try {
                val analysis = sessionManager.getLatestAnalysis()
                val context = "Screen: $screenContext. User name: ${sessionManager.getUserName()}. Analysis: ${if (analysis != null) "Available" else "Not available"}"
                val aiResponse = geminiService.getAiAssistantResponse(text, context)
                _messages.value = _messages.value + ChatMessage(aiResponse, false)
            } finally {
                _isTyping.value = false
            }
        }
    }
}

data class ResumeOptimizationState(
    val jdTitle: String = "",
    val companyName: String? = null,
    val suggestions: List<OptimizationSuggestion> = emptyList(),
    val skillGaps: List<String> = emptyList(),
    val keywordOpportunities: List<String> = emptyList(),
    val isAnalysisComplete: Boolean = false,
    val currentImprovementIndex: Int = 0,
    val optimizedResumeData: ResumeAnalysisResult? = null
)

class JdMatcherViewModel(
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService,
    private val resumeParser: ResumeParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(JdMatcherUiState.INPUT)
    val uiState: StateFlow<JdMatcherUiState> = _uiState.asStateFlow()

    private val _jobDescriptionInput = MutableStateFlow("")
    val jobDescriptionInput: StateFlow<String> = _jobDescriptionInput.asStateFlow()

    private val _canonicalResume = MutableStateFlow<ResumeAnalysisResult?>(null)
    val canonicalResume: StateFlow<ResumeAnalysisResult?> = _canonicalResume.asStateFlow()

    private val _hasStoredResume = MutableStateFlow(false)
    val hasStoredResume: StateFlow<Boolean> = _hasStoredResume.asStateFlow()

    val analysisStages: List<String> = listOf(
        "1. Reading Job Description",
        "2. Scanning Your Resume",
        "3. Identifying Relevant Skills",
        "4. Finding Important Keywords",
        "5. Comparing Resume Content",
        "6. Detecting Improvement Opportunities",
        "7. Preparing Targeted Suggestions"
    )

    private val _analysisStageIndex = MutableStateFlow(0)
    val analysisStageIndex: StateFlow<Int> = _analysisStageIndex.asStateFlow()

    private val _optimizationState = MutableStateFlow(ResumeOptimizationState())
    val optimizationState: StateFlow<ResumeOptimizationState> = _optimizationState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCanonicalResume()
    }

    fun loadCanonicalResume() {
        val analysis = sessionManager.getLatestAnalysis()
        _canonicalResume.value = analysis
        _hasStoredResume.value = analysis != null
    }

    fun loadAnalysis(id: String?) {
        if (id.isNullOrBlank()) {
            loadCanonicalResume()
            return
        }
        val analysis = sessionManager.getAnalysisById(id)
        if (analysis != null) {
            _canonicalResume.value = analysis
            _hasStoredResume.value = true
        } else {
            loadCanonicalResume()
        }
    }

    fun onJobDescriptionInputChanged(input: String) {
        _jobDescriptionInput.value = input
        if (_errorMessage.value != null) {
            _errorMessage.value = null
        }
    }

    fun clearInput() {
        _jobDescriptionInput.value = ""
        _errorMessage.value = null
    }

    fun analyzeJobDescription() {
        val jdText = _jobDescriptionInput.value.trim()
        if (jdText.isBlank()) {
            _errorMessage.value = "Please paste a complete job description to analyze."
            return
        }

        val resume = _canonicalResume.value ?: sessionManager.getLatestAnalysis()
        if (resume == null) {
            _errorMessage.value = "No resume data found. Please upload a resume first."
            return
        }
        _canonicalResume.value = resume
        _hasStoredResume.value = true

        _uiState.value = JdMatcherUiState.ANALYZING
        _errorMessage.value = null
        _analysisStageIndex.value = 0

        viewModelScope.launch {
            try {
                val tickerJob = launch {
                    for (i in 0 until analysisStages.size) {
                        _analysisStageIndex.value = i
                        delay(600L)
                    }
                }
                
                val result = geminiService.optimizeResume(jdText, resume)
                tickerJob.cancel()
                _analysisStageIndex.value = analysisStages.size
                delay(300L)
                
                _optimizationState.value = ResumeOptimizationState(
                    jdTitle = result.jdTitle,
                    companyName = result.companyName,
                    suggestions = result.suggestions,
                    skillGaps = result.skillGaps,
                    keywordOpportunities = result.keywordOpportunities,
                    isAnalysisComplete = true,
                    optimizedResumeData = resume
                )
                
                _uiState.value = JdMatcherUiState.OPTIMIZING
            } catch (e: Exception) {
                Log.e("JdMatcherViewModel", "Optimization failed", e)
                _errorMessage.value = e.message ?: "Failed to analyze resume. Please try again."
                _uiState.value = JdMatcherUiState.ERROR
            }
        }
    }

    fun updateSuggestionState(suggestionId: String, newState: SuggestionState, manualText: String? = null) {
        val currentSuggestions = _optimizationState.value.suggestions
        val updatedSuggestions = currentSuggestions.map {
            if (it.changeId == suggestionId) {
                it.copy(state = newState, manualText = manualText)
            } else it
        }
        
        _optimizationState.value = _optimizationState.value.copy(suggestions = updatedSuggestions)
        
        // If accepted or edited, update the preview resume data immediately
        if (newState == SuggestionState.ACCEPTED || newState == SuggestionState.EDITED) {
            applyChangeToPreview(suggestionId, if (newState == SuggestionState.EDITED) manualText else null)
        } else if (newState == SuggestionState.DISMISSED || newState == SuggestionState.ORIGINAL_KEPT) {
            revertChangeInPreview(suggestionId)
        }
    }

    private fun applyChangeToPreview(suggestionId: String, manualText: String?) {
        val suggestion = _optimizationState.value.suggestions.find { it.changeId == suggestionId } ?: return
        val currentResume = _optimizationState.value.optimizedResumeData ?: return
        
        val newText = manualText ?: suggestion.suggestedText
        val oldText = suggestion.originalText
        
        // Text replacement in relevant sections
        val updatedResume = when (suggestion.section.uppercase()) {
            "SUMMARY" -> currentResume.copy(summary = currentResume.summary?.replace(oldText, newText))
            "EXPERIENCE" -> currentResume.copy(experience = currentResume.experience?.map { it.replace(oldText, newText) })
            "PROJECTS" -> currentResume.copy(projectAnalysis = currentResume.projectAnalysis?.map { proj ->
                val newName = if (proj.name.contains(oldText)) proj.name.replace(oldText, newText) else proj.name
                val newStrengths = proj.strengths?.map { s -> if (s.contains(oldText)) s.replace(oldText, newText) else s }
                proj.copy(name = newName, strengths = newStrengths)
            })
            "SKILLS" -> currentResume.copy(extractedSkills = currentResume.extractedSkills?.map { 
                if (it.name.equals(oldText, ignoreCase = true)) it.copy(name = newText)
                else it
            })
            "CERTIFICATION", "CERTIFICATIONS" -> currentResume.copy(certifications = currentResume.certifications?.map {
                if (it.contains(oldText)) it.replace(oldText, newText) else it
            })
            else -> currentResume
        }
        
        _optimizationState.value = _optimizationState.value.copy(optimizedResumeData = updatedResume)
    }

    private fun revertChangeInPreview(suggestionId: String) {
        val suggestion = _optimizationState.value.suggestions.find { it.changeId == suggestionId } ?: return
        val currentResume = _optimizationState.value.optimizedResumeData ?: return
        
        val currentAppliedText = suggestion.manualText ?: suggestion.suggestedText
        val originalText = suggestion.originalText
        
        val updatedResume = when (suggestion.section.uppercase()) {
            "SUMMARY" -> currentResume.copy(summary = currentResume.summary?.replace(currentAppliedText, originalText))
            "EXPERIENCE" -> currentResume.copy(experience = currentResume.experience?.map { it.replace(currentAppliedText, originalText) })
            "PROJECTS" -> currentResume.copy(projectAnalysis = currentResume.projectAnalysis?.map { proj ->
                val newName = if (proj.name.contains(currentAppliedText)) proj.name.replace(currentAppliedText, originalText) else proj.name
                val newStrengths = proj.strengths?.map { s -> if (s.contains(currentAppliedText)) s.replace(currentAppliedText, originalText) else s }
                proj.copy(name = newName, strengths = newStrengths)
            })
            "SKILLS" -> currentResume.copy(extractedSkills = currentResume.extractedSkills?.map { 
                if (it.name.equals(currentAppliedText, ignoreCase = true)) it.copy(name = originalText)
                else it
            })
            "CERTIFICATION", "CERTIFICATIONS" -> currentResume.copy(certifications = currentResume.certifications?.map {
                if (it.contains(currentAppliedText)) it.replace(currentAppliedText, originalText) else it
            })
            else -> currentResume
        }
        
        _optimizationState.value = _optimizationState.value.copy(optimizedResumeData = updatedResume)
    }

    fun setCurrentImprovement(index: Int) {
        if (index in _optimizationState.value.suggestions.indices) {
            _optimizationState.value = _optimizationState.value.copy(currentImprovementIndex = index)
        }
    }

    fun generateOptimizedResume() {
        val currentResume = _optimizationState.value.optimizedResumeData ?: return
        
        _uiState.value = JdMatcherUiState.ANALYZING
        _analysisStageIndex.value = 0
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = com.example.aidrivencompetencyplatform.AstraApp.instance
                val pdfFile = com.example.aidrivencompetencyplatform.data.ResumePdfGenerator.generateCleanResumePdf(
                    context = context,
                    resume = currentResume,
                    targetRole = _optimizationState.value.jdTitle
                ) ?: throw Exception("Failed to generate PDF document.")
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val updatedResume = currentResume.copy(
                        layoutInfo = (currentResume.layoutInfo ?: com.example.aidrivencompetencyplatform.model.ResumeLayout()).copy(
                            pdfUri = Uri.fromFile(pdfFile).toString()
                        )
                    )
                    _optimizationState.value = _optimizationState.value.copy(
                        optimizedResumeData = updatedResume,
                        isAnalysisComplete = true
                    )
                    _uiState.value = JdMatcherUiState.RESULT
                }
            } catch (e: Exception) {
                Log.e("JdMatcherViewModel", "PDF Generation failed", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _errorMessage.value = "Failed to generate optimized PDF: ${e.message}"
                    _uiState.value = JdMatcherUiState.ERROR
                }
            }
        }
    }

    fun resetToInput() {
        _uiState.value = JdMatcherUiState.INPUT
        _optimizationState.value = ResumeOptimizationState()
        _errorMessage.value = null
    }

    fun backToEditor() {
        _uiState.value = JdMatcherUiState.OPTIMIZING
    }

    fun retryAnalysis() {
        analyzeJobDescription()
    }
}


