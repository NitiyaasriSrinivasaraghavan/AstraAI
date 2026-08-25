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
    val overallScore: Int,
    val atsScore: Int,
    val skillMatch: Int,
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
    val parsingAccuracyDetails: ParsingAccuracyDetails? = ParsingAccuracyDetails()
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
