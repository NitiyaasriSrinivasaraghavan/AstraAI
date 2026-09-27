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
import com.example.aidrivencompetencyplatform.data.InterviewService
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
            s == "android sdk" -> RecommendedCourse(
                title = "Android SDK Fundamentals",
                provider = "Google Developers Training",
                description = "Learn the core components of the Android SDK, Activity lifecycles, and system services.",
                courseUrl = "https://developer.android.com/courses",
                originalTitle = "Android SDK & Platform Tools",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "android studio" -> RecommendedCourse(
                title = "Android Studio User Guide",
                provider = "Google Android Developers",
                description = "Learn to navigate the official Android IDE, use debugging tools, and manage project structures.",
                courseUrl = "https://developer.android.com/studio/intro",
                originalTitle = "Android Studio Guide",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "room database" -> RecommendedCourse(
                title = "Persisting Data with Room Database",
                provider = "Android Developers Codelabs",
                description = "Learn SQLite persistence, DAOs, entities, and Flow-based reactive queries in Android.",
                courseUrl = "https://developer.android.com/codelabs/basic-android-kotlin-compose-persisting-data-room",
                originalTitle = "Room Database & Persistence",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "java" -> RecommendedCourse(
                title = "Java Programming Fundamentals",
                provider = "University of Helsinki (MOOC.fi)",
                description = "Master object-oriented programming, data structures, multithreading, and enterprise Java principles.",
                courseUrl = "https://java-programming.mooc.fi/",
                originalTitle = "Java Programming Comprehensive Course",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "spring" -> RecommendedCourse(
                title = "Spring Framework Essentials",
                provider = "Spring.io Official",
                description = "Learn the core features of the Spring Framework including dependency injection and data access.",
                courseUrl = "https://spring.io/projects/spring-framework",
                originalTitle = "Spring Framework Documentation",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "spring boot" -> RecommendedCourse(
                title = "Spring Boot Fundamentals",
                provider = "Spring.io Official Guides",
                description = "Build production-ready RESTful web services and auto-configuration with Spring Boot.",
                courseUrl = "https://spring.io/guides/gs/spring-boot/",
                originalTitle = "Spring Boot Microservices",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "python" -> RecommendedCourse(
                title = "CS50's Introduction to Programming with Python",
                provider = "Harvard University (edX / Free)",
                description = "Learn foundational and advanced Python syntax, data structures, libraries, and automated testing.",
                courseUrl = "https://cs50.harvard.edu/python/",
                originalTitle = "Python Programming Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "sql" -> RecommendedCourse(
                title = "Intro to SQL: Querying and Managing Data",
                provider = "Khan Academy",
                description = "Master relational database querying, joins, aggregations, subqueries, and table indexing.",
                courseUrl = "https://www.khanacademy.org/computing/computer-programming/sql",
                originalTitle = "SQL & Relational Databases",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "postgresql" -> RecommendedCourse(
                title = "PostgreSQL Tutorial for Developers",
                provider = "PostgreSQL Official / Tutorial",
                description = "Learn advanced SQL queries, JSONB indexing, ACID transactions, and performance tuning in Postgres.",
                courseUrl = "https://www.postgresqltutorial.com/",
                originalTitle = "PostgreSQL Developer Guide",
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
            s == "react" -> RecommendedCourse(
                title = "React Official Interactive Tutorial",
                provider = "React.dev (Meta Open Source)",
                description = "Learn modern component architecture, hooks (useState, useEffect), and reactive state rendering.",
                courseUrl = "https://react.dev/learn",
                originalTitle = "React Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "javascript" -> RecommendedCourse(
                title = "JavaScript Algorithms and Data Structures",
                provider = "freeCodeCamp",
                description = "Master ES6+ syntax, asynchronous JavaScript (Promises/Async-Await), and DOM manipulation.",
                courseUrl = "https://www.freecodecamp.org/learn/javascript-algorithms-and-data-structures-v8/",
                originalTitle = "JavaScript Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "typescript" -> RecommendedCourse(
                title = "TypeScript Handbook & Practical Guide",
                provider = "Microsoft TypeScript Official",
                description = "Master static typing, generics, utility types, and type inference for robust application design.",
                courseUrl = "https://www.typescriptlang.org/docs/handbook/intro.html",
                originalTitle = "TypeScript Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "git" -> RecommendedCourse(
                title = "Version Control with Git & GitHub",
                provider = "Git-SCM Documentation & freeCodeCamp",
                description = "Learn branching, merge strategies, interactive rebasing, pull requests, and CI workflows.",
                courseUrl = "https://git-scm.com/doc",
                originalTitle = "Git & GitHub Version Control",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "docker" -> RecommendedCourse(
                title = "Docker for Beginners & Containerization",
                provider = "Docker Official Documentation",
                description = "Learn Dockerfile authoring, multi-stage builds, container networking, and Docker Compose.",
                courseUrl = "https://docs.docker.com/get-started/",
                originalTitle = "Docker & Containerization",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "machine learning" -> RecommendedCourse(
                title = "Intro to Machine Learning with Python",
                provider = "Kaggle Learn / freeCodeCamp",
                description = "Learn supervised learning, regression, classification, cross-validation, and model evaluation.",
                courseUrl = "https://www.kaggle.com/learn/intro-to-machine-learning",
                originalTitle = "Machine Learning Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "scikit-learn" -> RecommendedCourse(
                title = "Machine Learning with scikit-learn",
                provider = "Scikit-Learn Official / freeCodeCamp",
                description = "Learn to build machine learning models using the scikit-learn library in Python.",
                courseUrl = "https://scikit-learn.org/stable/tutorial/index.html",
                originalTitle = "Scikit-Learn Tutorial",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "tensorflow" -> RecommendedCourse(
                title = "TensorFlow Core Fundamentals",
                provider = "TensorFlow.org / Google",
                description = "Build and train neural networks using the Keras API and core TensorFlow components.",
                courseUrl = "https://www.tensorflow.org/tutorials",
                originalTitle = "TensorFlow Official Tutorials",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "pytorch" -> RecommendedCourse(
                title = "Deep Learning with PyTorch Tutorial",
                provider = "PyTorch.org Official Tutorials",
                description = "Learn neural networks, backpropagation, CNNs, RNNs, and Transformers with PyTorch.",
                courseUrl = "https://pytorch.org/tutorials/",
                originalTitle = "PyTorch Deep Learning Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "pandas" -> RecommendedCourse(
                title = "Pandas & Data Manipulation Course",
                provider = "Kaggle Learn",
                description = "Master DataFrame transformations, exploratory data analysis, filtering, and indexing in Pandas.",
                courseUrl = "https://www.kaggle.com/learn/pandas",
                originalTitle = "Pandas Data Manipulation",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "numpy" -> RecommendedCourse(
                title = "Numerical Computing with NumPy",
                provider = "NumPy.org / freeCodeCamp",
                description = "Learn the fundamentals of NumPy for efficient numerical computing and array processing in Python.",
                courseUrl = "https://numpy.org/doc/stable/user/absolute_beginners.html",
                originalTitle = "NumPy Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "model deployment" -> RecommendedCourse(
                title = "Machine Learning Model Deployment Guide",
                provider = "freeCodeCamp",
                description = "Deploy ML models as scalable REST endpoints using FastAPI, Docker, and cloud runtimes.",
                courseUrl = "https://www.freecodecamp.org/news/how-to-deploy-machine-learning-models/",
                originalTitle = "Model Deployment Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "mlops" -> RecommendedCourse(
                title = "MLOps Fundamentals",
                provider = "Google Cloud / freeCodeCamp",
                description = "Learn the lifecycle of machine learning models from data engineering to production operations.",
                courseUrl = "https://www.freecodecamp.org/news/what-is-mlops/",
                originalTitle = "MLOps & Model Deployment",
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
            s == "figma" -> RecommendedCourse(
                title = "Figma for Beginners & UI/UX Design",
                provider = "Figma Official Resource Library",
                description = "Master wireframing, interactive prototyping, auto-layout, and design tokens in Figma.",
                courseUrl = "https://www.figma.com/resource-library/learn-figma/",
                originalTitle = "Figma & UI/UX Design",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "wireframing" -> RecommendedCourse(
                title = "Wireframing Guide for Beginners",
                provider = "Figma Official",
                description = "Learn the principles of low-fidelity design and how to structure page layouts effectively.",
                courseUrl = "https://www.figma.com/resource-library/wireframing-guide/",
                originalTitle = "Wireframing Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "prototyping" -> RecommendedCourse(
                title = "Interactive Prototyping Essentials",
                provider = "Figma Official",
                description = "Learn to create interactive flows and user tests with high-fidelity prototypes in Figma.",
                courseUrl = "https://www.figma.com/resource-library/prototyping-guide/",
                originalTitle = "Prototyping Fundamentals",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "rest apis" -> RecommendedCourse(
                title = "RESTful API Design and Web Architecture",
                provider = "MDN Web Docs",
                description = "Learn HTTP methods, status codes, REST conventions, and API security principles.",
                courseUrl = "https://developer.mozilla.org/en-US/docs/Learn/Server-side/First_steps/Web_frameworks",
                originalTitle = "REST API Architecture",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "json" -> RecommendedCourse(
                title = "JSON Fundamentals",
                provider = "MDN Web Docs",
                description = "Learn how to parse, manipulate, and structure data in the JSON format for web and mobile apps.",
                courseUrl = "https://developer.mozilla.org/en-US/docs/Learn/JavaScript/Objects/JSON",
                originalTitle = "Working with JSON Data",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "material design" -> RecommendedCourse(
                title = "Material Design 3 Architecture & Guidelines",
                provider = "Google Material Design",
                description = "Learn accessible UI styling, dynamic color, typography scales, and component guidelines.",
                courseUrl = "https://m3.material.io/",
                originalTitle = "Material Design Guidelines",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "xml" -> RecommendedCourse(
                title = "Android XML Layouts",
                provider = "Google Android Developers",
                description = "Learn the fundamentals of structuring Android UI with XML (legacy View-based approach).",
                courseUrl = "https://developer.android.com/develop/ui/views/layout/declaring-layout",
                originalTitle = "XML Layouts in Android",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "gradle" -> RecommendedCourse(
                title = "Build Automation with Gradle",
                provider = "Gradle Guides / Baeldung",
                description = "Learn build lifecycles, dependency management, plugins, and multi-module configurations.",
                courseUrl = "https://docs.gradle.org/current/userguide/getting_started.html",
                originalTitle = "Gradle & Maven Automation",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "maven" -> RecommendedCourse(
                title = "Maven Build Tool Guide",
                provider = "Apache Maven Official",
                description = "Learn project management and dependency resolution using the Maven build tool.",
                courseUrl = "https://maven.apache.org/guides/getting-started/",
                originalTitle = "Gradle & Maven Automation",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "authentication" -> RecommendedCourse(
                title = "Web Security & Authentication Fundamentals",
                provider = "MDN Web Docs",
                description = "Learn JWT tokens, OAuth 2.0 flows, session management, and credential security.",
                courseUrl = "https://developer.mozilla.org/en-US/docs/Learn/Server-side/First_steps/Website_security",
                originalTitle = "Web Security & Auth",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "statistics" -> RecommendedCourse(
                title = "Statistics and Probability for Data Science",
                provider = "Khan Academy",
                description = "Learn descriptive statistics, probability distributions, and hypothesis testing.",
                courseUrl = "https://www.khanacademy.org/math/statistics-probability",
                originalTitle = "Statistics & Probability",
                missingSkill = skill,
                explanation = defaultExp
            )
            s == "probability" -> RecommendedCourse(
                title = "Intro to Probability",
                provider = "Harvard University (edX)",
                description = "Learn the mathematical foundations of probability and its application in data science.",
                courseUrl = "https://www.edx.org/course/introduction-to-probability",
                originalTitle = "Statistics & Probability",
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

                // Requirement #2 & #9: Prevent duplicate resume analyses
                val existing = sessionManager.findExistingResumeAnalysis(text, effectiveRole)
                if (existing != null) {
                    Log.d("ResumeAnalysisDebug", "Found existing matching analysis. Reusing snapshot.")
                    
                    // Restore state from existing snapshot
                    _analysisResult.value = existing
                    
                    // Simulate completion progress for UI smoothness
                    _interactiveState.value = _interactiveState.value.copy(
                        atsComplete = true,
                        skillGapComplete = true,
                        jdMatchingComplete = true,
                        isComplete = true,
                        stage = 7,
                        atsScore = existing.atsScore,
                        skillMatch = existing.skillMatch
                    )
                    
                    _isLoading.value = false
                    _loadingStage.value = "Retrieved existing analysis."
                    return@launch
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

                fun sanitizeCandidate(value: String?, isNameField: Boolean = false): String? {
                    if (value.isNullOrBlank()) return null
                    val clean = value.trim()
                    val lower = clean.lowercase()
                    val invalid = setOf(
                        "null", "none", "n/a", "na", "not detected", "not specified",
                        "not provided", "not found", "unknown", "nil", "-", "--", "undefined", "empty"
                    )
                    if (invalid.contains(lower) || lower.startsWith("not detected") || lower.startsWith("not found")) return null
                    if (isNameField && ResumeParser.isDegreeOrEducationTitle(clean)) return null
                    return clean
                }

                val candidateName = sanitizeCandidate(structure.candidateInfo.name, isNameField = true)
                    ?: sanitizeCandidate(aiResult.candidateName, isNameField = true)
                    ?: "Candidate"
                val candidateEmail = sanitizeCandidate(aiResult.candidateEmail) ?: sanitizeCandidate(structure.candidateInfo.email)
                val candidatePhone = sanitizeCandidate(aiResult.candidatePhone) ?: sanitizeCandidate(structure.candidateInfo.phone)
                val candidateLocation = sanitizeCandidate(aiResult.candidateLocation) ?: sanitizeCandidate(structure.candidateInfo.location)
                
                val effectivePersonalInfo = if (!aiResult.personalInfo.isNullOrEmpty() && !isSynthesized) {
                    aiResult.personalInfo
                } else if (structure.personalInfo.isNotEmpty()) {
                    structure.personalInfo
                } else {
                    emptyList()
                }

                val effectiveSummary = aiResult.summary?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("Synthesized Analysis", ignoreCase = true) }
                    ?: structure.summary?.takeIf { it.isNotBlank() }
                    ?: ""

                fun isFormalEducationEntry(entry: String): Boolean {
                    if (entry.isBlank()) return false
                    val lw = entry.lowercase().trim()
                    
                    // Reject non-academic indicators: online courses, certifications, workshops, training, bootcamps
                    val nonAcademicIndicators = listOf(
                        "nptel", "coursera", "udemy", "edx", "linkedin learning", "udacity",
                        "certification in", "certificate course", "certified in", "course in",
                        "completed course", "workshop on", "workshop in", "bootcamp",
                        "training program", "short-term course", "training in"
                    )
                    if (nonAcademicIndicators.any { lw.contains(it) }) {
                        return false
                    }
                    
                    // Check for legitimate academic qualification keywords
                    val academicKeywords = listOf(
                        "bachelor", "b.tech", "b.e", "b.sc", "bca", "b.com", "bba", "b.s", "b.a",
                        "master", "m.tech", "m.e", "m.sc", "mca", "mba", "m.s", "m.a", "ph.d", "phd", "doctorate",
                        "diploma", "polytechnic",
                        "10th", "12th", "sslc", "hsc", "class x", "class xii", "cbse", "icse",
                        "matriculation", "secondary school", "higher secondary", "intermediate",
                        "puc", "pre-university", "schooling", "university", "college", "institute of technology"
                    )
                    return academicKeywords.any { lw.contains(it) }
                }

                // Reconcile Education: strictly retain ONLY formal academic qualifications (10th, 12th, Diploma, College)
                val validatedAiEducation = (aiResult.education ?: emptyList()).filter { isFormalEducationEntry(it) }
                val validatedDetEducation = structure.education.filter { isFormalEducationEntry(it) }

                val effectiveEducation = if (validatedAiEducation.isNotEmpty() && !isSynthesized) {
                    val combined = mutableListOf<String>()
                    combined.addAll(validatedAiEducation)
                    // Check if deterministic parser captured 10th/12th/qualifications that AI might have missed
                    validatedDetEducation.forEach { detEdu ->
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
                        if (!alreadyPresent) {
                            combined.add(detEdu)
                        }
                    }
                    combined
                } else if (validatedDetEducation.isNotEmpty()) {
                    validatedDetEducation
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
                val hasProjectSection = structure.sections.any { it.sectionName.equals("Projects", ignoreCase = true) && it.isFound } ||
                    structure.extractedProjects.isNotEmpty() ||
                    Regex("(?i)\\b(?:projects?|academic\\s+projects?|personal\\s+projects?|key\\s+projects?|selected\\s+projects?|project\\s+experience)\\b").containsMatchIn(text)

                val effectiveProjects = if (hasProjectSection) {
                    if (!aiResult.projectAnalysis.isNullOrEmpty() && !isSynthesized) {
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
                } else {
                    emptyList()
                }

                // Skills: Merge and preserve ALL explicitly detected skills from AI and deterministic parser
                val allSkillNames = LinkedHashSet<String>()
                val mergedSkills = mutableListOf<com.example.aidrivencompetencyplatform.model.Skill>()
                if (!aiResult.extractedSkills.isNullOrEmpty() && !isSynthesized) {
                    aiResult.extractedSkills.forEach { s ->
                        if (allSkillNames.add(s.name.lowercase().trim())) {
                            mergedSkills.add(s)
                        }
                    }
                }
                structure.extractedSkills.forEach { skillStr ->
                    val clean = skillStr.trim()
                    if (clean.isNotBlank() && allSkillNames.add(clean.lowercase())) {
                        mergedSkills.add(com.example.aidrivencompetencyplatform.model.Skill(name = clean, level = 80, category = RoleSkillsData.getSkillCategory(clean)))
                    }
                }
                val effectiveSkills = if (mergedSkills.isNotEmpty()) mergedSkills
                    else structure.extractedSkills.map { com.example.aidrivencompetencyplatform.model.Skill(name = it, level = 80, category = RoleSkillsData.getSkillCategory(it)) }

                val enrichedResult = aiResult.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    candidateName = candidateName,
                    candidateEmail = candidateEmail,
                    candidatePhone = candidatePhone,
                    candidateLocation = candidateLocation,
                    personalInfo = effectivePersonalInfo,
                    summary = effectiveSummary,
                    education = effectiveEducation,
                    experience = effectiveExperience,
                    extractedSkills = effectiveSkills,
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
    private val geminiService: GeminiService,
    private val interviewService: InterviewService
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatContextMode = MutableStateFlow(ChatContextMode.DEFAULT)
    val chatContextMode: StateFlow<ChatContextMode> = _chatContextMode.asStateFlow()

    private val _interviewSession = MutableStateFlow(InterviewSession())
    val interviewSession: StateFlow<InterviewSession> = _interviewSession.asStateFlow()

    val isInterviewMode: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch {
            _chatContextMode.collect {
                value = (it == ChatContextMode.INTERVIEW_PREPARATION)
            }
        }
    }

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
        _chatContextMode.value = ChatContextMode.DEFAULT
        _messages.value = listOf(
            ChatMessage("Hello $userName! I'm Nova, your AI Career Assistant. How can I help you elevate your career today?", false)
        )
    }

    fun startTour() {
        _chatContextMode.value = ChatContextMode.TOUR
        _isTourMode.value = true
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

    fun startInterview(analysisId: String? = null, preferredRole: String? = null) {
        _chatContextMode.value = ChatContextMode.INTERVIEW_PREPARATION
        _isTourMode.value = false

        val analysis = if (!analysisId.isNullOrBlank()) {
            sessionManager.getAnalysisById(analysisId) ?: sessionManager.getLatestAnalysis()
        } else {
            sessionManager.getLatestAnalysis()
        }

        val role = preferredRole 
            ?: analysis?.targetRole 
            ?: sessionManager.getTargetRole() 
            ?: "Software Engineer"
        val candidateName = analysis?.candidateName ?: sessionManager.getUserName()

        val welcomeText = interviewService.generateWelcomeMessage(role, candidateName)
        val initialMessage = ChatMessage(welcomeText, false)

        val newSession = InterviewSession(
            targetRole = role,
            candidateProfile = analysis,
            currentQuestion = null,
            currentQuestionNumber = 0,
            candidateAnswer = "",
            phase = InterviewPhase.WAITING_FOR_USER,
            status = InterviewStatus.IN_PROGRESS,
            conversationHistory = listOf(initialMessage)
        )

        _interviewSession.value = newSession
        _messages.value = listOf(initialMessage)
        _quickPrompts.value = listOf(
            "Yes, I'm ready!",
            "Let's begin the interview",
            "Ready when you are"
        )
    }

    fun sendMessage(text: String, screenContext: String = "") {
        if (text.isBlank()) return
        val userMsg = ChatMessage(text, true)
        _messages.value = _messages.value + userMsg

        if (_chatContextMode.value == ChatContextMode.INTERVIEW_PREPARATION) {
            handleInterviewConversation(text)
        } else {
            handleGeneralAssistantConversation(text, screenContext)
        }
    }

    private fun handleInterviewConversation(userAnswerText: String) {
        val currentSession = _interviewSession.value
        val updatedHistory = _messages.value

        viewModelScope.launch {
            _isTyping.value = true
            try {
                if (currentSession.currentQuestionNumber == 0) {
                    // Phase: Transitioning from WELCOME / WAITING_FOR_USER to first question
                    _interviewSession.value = currentSession.copy(
                        phase = InterviewPhase.INTERVIEWER_QUESTION,
                        currentQuestionNumber = 1,
                        conversationHistory = updatedHistory
                    )

                    val firstQuestion = interviewService.generateFirstQuestion(
                        targetRole = currentSession.targetRole,
                        candidateProfile = currentSession.candidateProfile
                    )

                    val questionText = "Great, let's begin!\n\nQuestion 1: ${firstQuestion.text}"
                    val botMessage = ChatMessage(questionText, false)
                    _messages.value = _messages.value + botMessage

                    _interviewSession.value = _interviewSession.value.copy(
                        currentQuestion = firstQuestion,
                        currentQuestionNumber = 1,
                        phase = InterviewPhase.WAITING_FOR_USER,
                        questions = listOf(firstQuestion),
                        conversationHistory = _messages.value
                    )
                    _quickPrompts.value = emptyList()
                } else {
                    // Phase: Candidate provided answer to Question N
                    val currentQNum = currentSession.currentQuestionNumber
                    val prevQuestion = currentSession.currentQuestion
                    val updatedAnswers = currentSession.userAnswers + (currentQNum to userAnswerText)

                    _interviewSession.value = currentSession.copy(
                        candidateAnswer = userAnswerText,
                        userAnswers = updatedAnswers,
                        phase = InterviewPhase.USER_ANSWER,
                        conversationHistory = updatedHistory
                    )

                    val nextQNum = currentQNum + 1
                    val nextQuestion = interviewService.generateNextQuestion(
                        targetRole = currentSession.targetRole,
                        candidateProfile = currentSession.candidateProfile,
                        questionNumber = nextQNum,
                        previousQuestion = prevQuestion,
                        candidateAnswer = userAnswerText
                    )

                    val nextText = "Thank you for sharing that answer.\n\nQuestion $nextQNum: ${nextQuestion.text}"
                    val botMessage = ChatMessage(nextText, false)
                    _messages.value = _messages.value + botMessage

                    _interviewSession.value = _interviewSession.value.copy(
                        currentQuestion = nextQuestion,
                        currentQuestionNumber = nextQNum,
                        phase = InterviewPhase.WAITING_FOR_USER,
                        questions = _interviewSession.value.questions + nextQuestion,
                        conversationHistory = _messages.value
                    )
                    _quickPrompts.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AiAssistantViewModel", "Interview question generation error", e)
                val fallbackMsg = ChatMessage(
                    "Let's continue: Could you describe your key strengths and technical experience in ${currentSession.targetRole}?",
                    false
                )
                _messages.value = _messages.value + fallbackMsg
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun handleGeneralAssistantConversation(text: String, screenContext: String) {
        viewModelScope.launch {
            _isTyping.value = true
            try {
                val analysis = sessionManager.getLatestAnalysis()
                val contextSummary = buildUserContext(analysis, screenContext)
                
                // Keep only last 10 messages for conversation context to maintain speed and performance
                val history = _messages.value.takeLast(11).dropLast(1)
                
                val aiResponse = geminiService.getAiAssistantResponse(text, history, contextSummary)
                _messages.value = _messages.value + ChatMessage(aiResponse, false)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("I'm having a bit of trouble connecting to my AI core right now. Could you please try again in a moment?", false)
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun buildUserContext(analysis: ResumeAnalysisResult?, screen: String): String {
        val name = sessionManager.getUserName() ?: "User"
        if (analysis == null) return "User Name: $name. Screen: $screen. Context: No resume has been uploaded yet. Guide the user to upload their resume first."
        
        val sb = StringBuilder()
        sb.append("User Name: $name\n")
        sb.append("Current Screen: $screen\n")
        sb.append("Target Role: ${analysis.targetRole ?: "Not set"}\n")
        sb.append("ATS Score: ${analysis.atsScore}/100\n")
        sb.append("Skill Match Score: ${analysis.skillMatch}%\n")
        
        analysis.strengths?.let { if (it.isNotEmpty()) sb.append("Top Strengths: ${it.take(3).joinToString(", ")}\n") }
        analysis.weaknesses?.let { if (it.isNotEmpty()) sb.append("Top Weaknesses: ${it.take(3).joinToString(", ")}\n") }
        
        val skills = analysis.extractedSkills?.map { it.name } ?: emptyList()
        if (skills.isNotEmpty()) sb.append("Extracted Skills: ${skills.take(15).joinToString(", ")}\n")
        
        if (!analysis.missingSections.isNullOrEmpty()) {
            sb.append("Missing Resume Sections: ${analysis.missingSections.joinToString(", ")}\n")
        }
        
        analysis.modificationReport?.let { report ->
            sb.append("Specific JD Match Score: ${report.overallMatch}% for ${report.targetRole}\n")
            sb.append("JD Match Reasoning: ${report.overallReasoning}\n")
        }
        
        return sb.toString()
    }
}

data class ResumeOptimizationState(
    val jdTitle: String = "",
    val companyName: String? = null,
    val suggestions: List<OptimizationSuggestion> = emptyList(),
    val skillGaps: List<String> = emptyList(),
    val keywordOpportunities: List<String> = emptyList(),
    val report: ResumeModificationReport? = null,
    val reportPdfUri: Uri? = null,
    val isAnalysisComplete: Boolean = false,
    val currentImprovementIndex: Int = 0,
    val optimizedResumeData: ResumeAnalysisResult? = null,
    val isFromExistingHistory: Boolean = false,
    val justCompleted: Boolean = false
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
        "Scanning your resume...",
        "Matching your experience...",
        "Comparing skills with the job description...",
        "Identifying relevant evidence...",
        "Evaluating role alignment...",
        "Preparing your JD suggestions..."
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
            
            // If this is a JD match analysis from history, restore its state
            if (analysis.modificationReport != null) {
                Log.d("JD_ANALYSIS_DEBUG", "Retrieving previous analysis for ID: $id")
                Log.d("JD_ANALYSIS_DEBUG", "Retrieved JD: ${analysis.jdText?.take(100)}...")
                _jobDescriptionInput.value = analysis.jdText ?: ""
                _optimizationState.value = ResumeOptimizationState(
                    jdTitle = analysis.modificationReport.targetRole,
                    report = analysis.modificationReport,
                    isAnalysisComplete = true,
                    optimizedResumeData = analysis
                )
                _uiState.value = JdMatcherUiState.OPTIMIZING
            }
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

        // Requirement #5 & #9: Prevent duplicate analyses
        val existingAnalysis = sessionManager.findExistingJdAnalysis(resume.id, jdText)
        if (existingAnalysis?.modificationReport != null) {
            Log.d("JD_ANALYSIS_DEBUG", "Found existing matching analysis. Skipping AI call.")
            _optimizationState.value = ResumeOptimizationState(
                jdTitle = existingAnalysis.modificationReport.targetRole,
                report = existingAnalysis.modificationReport,
                isAnalysisComplete = true,
                optimizedResumeData = existingAnalysis,
                isFromExistingHistory = true // Flag to show simplified suggestion view in JD Matcher
            )
            _uiState.value = JdMatcherUiState.OPTIMIZING
            return
        }

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
                
                val result = geminiService.generateModificationReport(jdText, resume)
                tickerJob.cancel()
                _analysisStageIndex.value = analysisStages.size
                delay(300L)
                
                Log.d("JD_ANALYSIS_DEBUG", "JD received: ${jdText.take(100)}...")
                Log.d("JD_ANALYSIS_DEBUG", "Resume extracted: ${resume.summary?.take(100)}...")
                Log.d("JD_ANALYSIS_DEBUG", "Report generated: Overall Match ${result.report?.overallMatch}%")
                Log.d("JD_ANALYSIS_DEBUG", "Matches: ${result.report?.strongMatchCount}, Fixes: ${result.report?.recommendedImprovementCount}, Gaps: ${result.report?.potentialGapCount}")
                Log.d("JD_ANALYSIS_DEBUG", "Sections parsed: ${result.report?.sections?.map { it.sectionName }}")

                // Create a new analysis record for this JD match
                val analysisWithReport = resume.copy(
                    id = java.util.UUID.randomUUID().toString(), // New ID for each unique JD analysis
                    modificationReport = result.report,
                    jdText = jdText,
                    targetRole = result.jdTitle
                )
                
                // Save it to history
                sessionManager.saveLatestAnalysis(analysisWithReport, "JD_Analysis_${result.jdTitle}.pdf")
                Log.d("JD_ANALYSIS_DEBUG", "Analysis saved with ID: ${analysisWithReport.id}")
                
                _optimizationState.value = ResumeOptimizationState(
                    jdTitle = result.jdTitle,
                    companyName = result.companyName,
                    report = result.report,
                    isAnalysisComplete = true,
                    optimizedResumeData = analysisWithReport,
                    justCompleted = true
                )
                
                _uiState.value = JdMatcherUiState.OPTIMIZING
            } catch (e: Exception) {
                Log.e("JdMatcherViewModel", "Report generation failed", e)
                _errorMessage.value = e.message ?: "Failed to generate report. Please try again."
                _uiState.value = JdMatcherUiState.ERROR
            }
        }
    }

    fun downloadReport() {
        val report = _optimizationState.value.report ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val file = com.example.aidrivencompetencyplatform.data.ResumePdfGenerator.generateModificationReportPdf(
                com.example.aidrivencompetencyplatform.AstraApp.instance,
                report
            )
            if (file != null) {
                withContext(Dispatchers.Main) {
                    _optimizationState.value = _optimizationState.value.copy(
                        reportPdfUri = Uri.fromFile(file)
                    )
                    _uiState.value = JdMatcherUiState.RESULT
                }
            }
        }
    }

    fun backToReport() {
        _uiState.value = JdMatcherUiState.OPTIMIZING
    }

    fun resetToInput() {
        _uiState.value = JdMatcherUiState.INPUT
        _optimizationState.value = ResumeOptimizationState()
        _errorMessage.value = null
        loadCanonicalResume()
    }

    fun markCompletionHandled() {
        _optimizationState.value = _optimizationState.value.copy(justCompleted = false)
    }

    fun retryAnalysis() {
        analyzeJobDescription()
    }
}

class InterviewViewModel(
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewSessionState())
    val state: StateFlow<InterviewSessionState> = _state.asStateFlow()

    // Map to store user answers locally during the session
    private val _userAnswers = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        refreshContext()
    }

    fun refreshContext() {
        val latest = sessionManager.getLatestAnalysis()
        val role = latest?.targetRole ?: sessionManager.getTargetRole() ?: "Android Developer"
        
        // If the latest analysis already has a completed interview summary, load it as a locked snapshot
        if (latest?.interviewSummary != null) {
            _state.value = _state.value.copy(
                targetRole = role,
                questions = latest.interviewQuestions,
                summary = latest.interviewSummary,
                isComplete = true
            )
            _userAnswers.value = latest.userAnswers
        } else {
            _state.value = _state.value.copy(targetRole = role)
        }
    }

    fun loadHistoryAnalysis(analysisId: String) {
        val analysis = sessionManager.getAnalysisById(analysisId)
        if (analysis != null) {
            _state.value = _state.value.copy(
                targetRole = analysis.targetRole ?: "Android Developer",
                questions = analysis.interviewQuestions,
                summary = analysis.interviewSummary,
                isComplete = analysis.interviewSummary != null
            )
            // Restore user answers if available
            _userAnswers.value = analysis.userAnswers
        } else {
            refreshContext()
        }
    }

    fun startInterview() {
        val latest = sessionManager.getLatestAnalysis()
        if (latest == null) {
            _state.value = _state.value.copy(errorMessage = "Please upload your resume first to generate personalized questions.")
            return
        }

        // Clean start for new interview - Requirement #18
        _userAnswers.value = emptyMap()
        _state.value = InterviewSessionState(targetRole = latest.targetRole ?: "Android Developer", isLoading = true)

        viewModelScope.launch {
            try {
                val jd = latest.jdText 
                val questions = geminiService.generateInterviewQuestions(_state.value.targetRole, latest, jd)
                
                _state.value = _state.value.copy(
                    questions = questions.take(5), // Requirement #8
                    currentQuestionIndex = 0,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Failed to generate questions", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate questions: ${e.localizedMessage}"
                )
            }
        }
    }

    fun submitAnswer(answer: String) {
        if (answer.isBlank()) return
        
        val currentIdx = _state.value.currentQuestionIndex
        val question = _state.value.questions.getOrNull(currentIdx) ?: return
        
        // Store answer locally - Requirement #9
        val updatedAnswers = _userAnswers.value.toMutableMap()
        updatedAnswers[question.id] = answer
        _userAnswers.value = updatedAnswers

        // Advance or Finish
        if (currentIdx < _state.value.questions.size - 1) {
            _state.value = _state.value.copy(currentQuestionIndex = currentIdx + 1)
        } else {
            generateFinalReport()
        }
    }

    private fun generateFinalReport() {
        val latest = sessionManager.getLatestAnalysis() ?: return
        _state.value = _state.value.copy(isLoading = true, isEvaluating = true)
        
        viewModelScope.launch {
            try {
                val summary = geminiService.generateFinalInterviewReport(
                    questions = _state.value.questions,
                    answers = _userAnswers.value,
                    targetRole = _state.value.targetRole,
                    resumeData = latest
                )
                
                // Update persistent analysis result with the immutable snapshot - Requirement #3
                val updatedAnalysis = latest.copy(
                    interviewQuestions = _state.value.questions,
                    userAnswers = _userAnswers.value,
                    interviewSummary = summary
                )
                sessionManager.saveLatestAnalysis(updatedAnalysis)
                
                _state.value = _state.value.copy(
                    isComplete = true,
                    summary = summary,
                    isLoading = false,
                    isEvaluating = false
                )
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Failed to generate summary", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isEvaluating = false,
                    errorMessage = "Failed to generate performance report: ${e.localizedMessage}"
                )
            }
        }
    }

    fun reset() {
        startInterview()
    }
}

class TechnicalMcqViewModel(
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _state = MutableStateFlow(TechnicalMcqSessionState())
    val state: StateFlow<TechnicalMcqSessionState> = _state.asStateFlow()

    private var currentAnalysisId: String? = null

    fun loadAnalysis(analysisId: String?) {
        currentAnalysisId = analysisId
        val targetAnalysis = if (!analysisId.isNullOrBlank()) {
            sessionManager.getAnalysisById(analysisId) ?: sessionManager.getLatestAnalysis()
        } else {
            sessionManager.getLatestAnalysis()
        }

        if (targetAnalysis != null) {
            val effectiveId = analysisId ?: targetAnalysis.id
            if (targetAnalysis.technicalMcqResult != null) {
                // Permanently marked as attempted: show result only, do not show/generate quiz
                _state.value = TechnicalMcqSessionState(
                    currentAnalysisId = effectiveId,
                    result = targetAnalysis.technicalMcqResult,
                    isComplete = true,
                    questions = targetAnalysis.technicalMcqResult.questions,
                    isLoading = false,
                    isSubmitting = false
                )
            } else {
                // Not attempted yet for this analysis
                _state.value = TechnicalMcqSessionState(
                    currentAnalysisId = effectiveId,
                    isComplete = false,
                    result = null,
                    isLoading = false,
                    isSubmitting = false
                )
            }
        } else {
            _state.value = TechnicalMcqSessionState(
                currentAnalysisId = analysisId,
                isComplete = false,
                result = null,
                isLoading = false,
                isSubmitting = false
            )
        }
    }

    fun startTest() {
        val targetId = currentAnalysisId ?: _state.value.currentAnalysisId
        val targetAnalysis = if (!targetId.isNullOrBlank()) {
            sessionManager.getAnalysisById(targetId) ?: sessionManager.getLatestAnalysis()
        } else {
            sessionManager.getLatestAnalysis()
        }

        if (targetAnalysis == null) {
            _state.value = _state.value.copy(errorMessage = "Please upload your resume first.")
            return
        }

        // ONE ATTEMPT CHECK: If this analysis has already been attempted, DO NOT generate or show questions
        if (targetAnalysis.technicalMcqResult != null) {
            _state.value = _state.value.copy(
                currentAnalysisId = targetAnalysis.id,
                result = targetAnalysis.technicalMcqResult,
                questions = targetAnalysis.technicalMcqResult.questions,
                isComplete = true,
                isLoading = false
            )
            return
        }

        if (_state.value.isComplete || _state.value.result != null) {
            return
        }

        if (_state.value.questions.isNotEmpty()) {
            return
        }

        _state.value = _state.value.copy(
            currentAnalysisId = targetAnalysis.id,
            isLoading = true, 
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val questions = geminiService.generateTechnicalMcqs(
                    targetAnalysis.targetRole ?: "Software Engineer",
                    targetAnalysis,
                    targetAnalysis.jdText
                )
                _state.value = _state.value.copy(
                    questions = questions.take(10),
                    currentQuestionIndex = 0,
                    userAnswers = MutableList(questions.size.coerceAtMost(10)) { null },
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate test: ${e.localizedMessage}"
                )
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        if (_state.value.isComplete || _state.value.isSubmitting) return
        val currentIdx = _state.value.currentQuestionIndex
        val updatedAnswers = _state.value.userAnswers.toMutableList()
        if (currentIdx in updatedAnswers.indices) {
            updatedAnswers[currentIdx] = optionIndex
            _state.value = _state.value.copy(userAnswers = updatedAnswers)
        }
    }

    fun nextQuestion() {
        if (_state.value.isComplete || _state.value.isSubmitting) return
        if (_state.value.currentQuestionIndex < _state.value.questions.size - 1) {
            _state.value = _state.value.copy(currentQuestionIndex = _state.value.currentQuestionIndex + 1)
        } else {
            evaluateTest()
        }
    }

    private fun evaluateTest() {
        // Atomic duplicate submission check
        if (_state.value.isComplete || _state.value.isSubmitting) return
        _state.value = _state.value.copy(isSubmitting = true)

        val targetId = currentAnalysisId ?: _state.value.currentAnalysisId
        val targetAnalysis = if (!targetId.isNullOrBlank()) {
            sessionManager.getAnalysisById(targetId) ?: sessionManager.getLatestAnalysis()
        } else {
            sessionManager.getLatestAnalysis()
        } ?: run {
            _state.value = _state.value.copy(isSubmitting = false)
            return
        }

        // If already attempted in storage, do not recalculate as a new attempt
        if (targetAnalysis.technicalMcqResult != null) {
            _state.value = _state.value.copy(
                result = targetAnalysis.technicalMcqResult,
                isComplete = true,
                isSubmitting = false
            )
            return
        }

        val questions = _state.value.questions
        val userAnswers = _state.value.userAnswers

        var correctCount = 0
        val improvementAreas = mutableSetOf<String>()

        questions.forEachIndexed { index, q ->
            if (userAnswers.getOrNull(index) == q.correctAnswerIndex) {
                correctCount++
            } else {
                improvementAreas.add(q.topic)
            }
        }

        val result = TechnicalMcqResult(
            questions = questions,
            userAnswers = userAnswers,
            correctCount = correctCount,
            incorrectCount = questions.size - correctCount,
            improvementAreas = improvementAreas.toList()
        )

        // Save result permanently against this specific analysis
        val updatedAnalysis = targetAnalysis.copy(technicalMcqResult = result)
        sessionManager.saveAnalysis(updatedAnalysis)

        _state.value = _state.value.copy(
            result = result,
            isComplete = true,
            isSubmitting = false
        )
    }
}

data class TechnicalMcqSessionState(
    val currentAnalysisId: String? = null,
    val questions: List<TechnicalMcq> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val userAnswers: List<Int?> = emptyList(),
    val isComplete: Boolean = false,
    val result: TechnicalMcqResult? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)
