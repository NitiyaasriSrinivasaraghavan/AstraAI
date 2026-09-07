package com.example.aidrivencompetencyplatform.model

data class User(
    val name: String,
    val email: String,
    val password: String,
    val targetRole: String? = null
)

data class Skill(
    val name: String,
    val level: Int,
    val category: String? = "Other",
    val isGap: Boolean = false,
    val importance: String? = null,
    val reason: String? = null,
    val evidence: String? = null
)

data class ResumeAnalysisResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val overallScore: Int,
    val atsScore: Int,
    val skillMatch: Int,
    val targetRole: String? = null,
    val summary: String? = "",
    val education: List<String>? = emptyList(),
    val experience: List<String>? = emptyList(),
    val strengths: List<String>? = emptyList(),
    val weaknesses: List<String>? = emptyList(),
    val atsBreakdown: AtsBreakdown? = AtsBreakdown(),
    val extractedSkills: List<Skill>? = emptyList(),
    val missingSections: List<String>? = emptyList(),
    val formattingRisks: List<String>? = emptyList(),
    val projectAnalysis: List<ProjectAnalysis>? = emptyList(),
    val prioritizedSuggestions: PrioritizedSuggestions? = PrioritizedSuggestions(),
    val parsingAccuracyDetails: ParsingAccuracyDetails? = ParsingAccuracyDetails(),
    val candidateName: String? = null,
    val candidateEmail: String? = null,
    val candidatePhone: String? = null,
    val candidateLocation: String? = null,
    val rawResumeText: String? = null,
    val detectedJobDescription: JobDescriptionSection? = null,
    val certifications: List<String>? = emptyList(),
    val layoutInfo: ResumeLayout? = null
)

data class TextPosition(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val pageIndex: Int
)

data class ResumeLayout(
    val textPositions: List<TextPosition> = emptyList(),
    val pdfUri: String? = null
)

data class AtsBreakdown(
    val keywordCoverage: Int = 0,
    val resumeStructure: Int = 0,
    val formattingSafety: Int = 0,
    val parsingAccuracy: Int = 0,
    val explanations: AtsExplanations? = AtsExplanations()
)

data class AtsExplanations(
    val keywords: List<AtsEvidence>? = emptyList(),
    val structure: List<AtsEvidence>? = emptyList(),
    val formatting: List<AtsEvidence>? = emptyList(),
    val parsing: List<AtsEvidence>? = emptyList()
)

data class AtsEvidence(
    val label: String,
    val isDetected: Boolean,
    val detail: String? = null,
    val suggestion: String? = null
)

data class ParsingAccuracyDetails(
    val nameDetected: Boolean = false,
    val contactDetected: Boolean = false,
    val educationDetected: Boolean = false,
    val skillsDetected: Boolean = false,
    val experienceDetected: Boolean = false,
    val projectsDetected: Boolean = false
)

data class ProjectAnalysis(
    val name: String,
    val technologies: List<String>? = emptyList(),
    val demonstratedSkills: List<String>? = emptyList(),
    val strengths: List<String>? = emptyList(),
    val weaknesses: List<String>? = emptyList(),
    val improvementSuggestions: List<String>? = emptyList()
)

data class PrioritizedSuggestions(
    val highPriority: List<String>? = emptyList(),
    val mediumPriority: List<String>? = emptyList(),
    val lowPriority: List<String>? = emptyList()
)

data class RoadmapItem(
    val title: String,
    val description: String,
    val status: RoadmapStatus,
    val importance: String? = "High",
    val difficulty: String? = "Medium",
    val suggestedProject: String? = null
)

enum class RoadmapStatus {
    COMPLETED, IN_PROGRESS, LOCKED
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

data class JobRecommendation(
    val role: String,
    val matchPercentage: Int,
    val whyMatch: String,
    val matchingSkills: List<String>,
    val missingSkills: List<String>
)

data class SkillGapData(
    val targetRole: String,
    val matchPercentage: Int,
    val strongSkills: List<Skill>? = emptyList(),
    val developingSkills: List<Skill>? = emptyList(),
    val skillGaps: List<Skill>? = emptyList(),
    val requiredSkills: List<String>? = emptyList()
)

data class ExtractedResumeProfile(
    val targetRole: String? = null,
    val candidateName: String? = null,
    val education: String? = null,
    val experienceLevel: String? = null,
    val coreSkills: List<String> = emptyList(),
    val toolsAndTech: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val competencies: List<String> = emptyList(),
    val careerDirection: String? = null,
    val importantKeywords: List<String> = emptyList()
)

data class JobDescriptionSection(
    val roleTitle: String,
    val roleSummary: String,
    val responsibilities: List<String>,
    val requiredSkills: List<String>,
    val preferredSkills: List<String>,
    val qualifications: List<String>
)

data class JdStageInfo(
    val number: String,
    val title: String,
    val description: String,
    val status: JdStageStatus
)

enum class JdStageStatus {
    COMPLETED,
    PROCESSING,
    PENDING
}

data class JdExplainabilityItem(
    val id: String,
    val title: String,
    val detail: String,
    val tag: String = "Optimized"
)

data class JdPoweredResult(
    val targetRole: String,
    val alignmentPercentage: Int,
    val jobDescription: JobDescriptionSection,
    val whatChangedItems: List<JdExplainabilityItem>
)

enum class JdFlowMode {
    NO_JD,
    JD_DETECTED,
    PROFILE_VIEW,
    POWERING_PROGRESS,
    POWERED_RESULT
}

data class ParsedSectionItem(
    val sectionName: String,
    val isFound: Boolean = true,
    val details: List<String> = emptyList(),
    val summary: String? = null
) {
    val name: String get() = sectionName
}

data class AnalysisHistoryRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val targetRole: String,
    val atsScore: Int,
    val skillMatch: Int,
    val candidateName: String? = null,
    val fileName: String? = "Resume.pdf",
    val topSkills: List<String> = emptyList(),
    val fullResult: ResumeAnalysisResult? = null
)

data class RecommendedCourse(
    val title: String,
    val provider: String,
    val description: String = "Learn this skill through this free resource.",
    val courseUrl: String,
    val isFree: Boolean = true,
    val originalTitle: String = title,
    val missingSkill: String = "",
    val explanation: String = ""
)

data class JdMatchResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "Target Role",
    val companyName: String? = null,
    val roleSummary: String = "",
    val matchScore: Int = 0,
    val matchedSkills: List<String> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val preferredSkillsMatched: List<String> = emptyList(),
    val preferredSkillsMissing: List<String> = emptyList(),
    val responsibilities: List<String> = emptyList(),
    val experienceRequirement: String = "",
    val candidateExperience: String = "",
    val experienceMatchStatus: String = "Match", // Match, Partial Match, Gap, Not Specified
    val experienceMatchExplanation: String = "",
    val educationRequirement: String = "",
    val candidateEducation: String = "",
    val educationMatchStatus: String = "Match", // Match, Partial Match, Not Required
    val educationMatchExplanation: String = "",
    val certificationRequirement: String = "",
    val candidateCertifications: String = "",
    val certificationMatchStatus: String = "Not Required", // Match, Missing, Not Required
    val certificationMatchExplanation: String = "",
    val strengths: List<String> = emptyList(),
    val priorityGaps: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList(),
    val freeLearningResources: List<RecommendedCourse> = emptyList()
)

enum class JdMatcherUiState {
    INPUT,
    ANALYZING,
    OPTIMIZING, // NEW: Optimization mode with resume preview
    RESULT,
    ERROR
}

data class OptimizationSuggestion(
    val changeId: String = java.util.UUID.randomUUID().toString(),
    val section: String,
    val originalText: String,
    val suggestedText: String,
    val changeType: ChangeType,
    val reason: String,
    val relatedKeyword: String? = null,
    val relatedRequirement: String? = null,
    val priority: SuggestionPriority = SuggestionPriority.MEDIUM,
    val state: SuggestionState = SuggestionState.UNREVIEWED,
    val manualText: String? = null,
    val confidence: Int = 100,
    val resumeEvidence: String? = null,
    val supportedByResume: Boolean = true
)

enum class ChangeType {
    WORD, PHRASE, SENTENCE, BULLET, SKILL, PROJECT, EXPERIENCE, SUMMARY, CERTIFICATION, OTHER
}

enum class SuggestionPriority {
    CRITICAL, HIGH, MEDIUM, LOW
}

enum class SuggestionState {
    UNREVIEWED, ACCEPTED, EDITED, DISMISSED, ORIGINAL_KEPT
}

data class ResumeOptimizationResult(
    val jdTitle: String,
    val companyName: String?,
    val suggestions: List<OptimizationSuggestion>,
    val skillGaps: List<String>,
    val keywordOpportunities: List<String>
)


