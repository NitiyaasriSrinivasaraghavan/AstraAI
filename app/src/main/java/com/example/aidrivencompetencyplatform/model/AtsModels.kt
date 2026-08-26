package com.example.aidrivencompetencyplatform.model

data class AtsScoreResult(
    val overallScore: Int,
    val keywordCoverage: AtsCardCalculation,
    val resumeStructure: AtsCardCalculation,
    val formattingSafety: AtsCardCalculation,
    val parsingAccuracy: AtsCardCalculation,
    val targetRole: String,
    val candidateName: String? = null,
    val candidateEmail: String? = null,
    val candidatePhone: String? = null,
    val candidateLocation: String? = null,
    val priorityFixes: List<String> = emptyList()
)

data class AtsCardCalculation(
    val categoryName: String,
    val iconType: AtsCategoryIcon,
    val score: Int,
    val weightPercentage: Int,
    val weightedContribution: Double,
    val formulaText: String,
    val calculationText: String,
    val resultText: String,
    val oneLineSummary: String,
    val status: String,
    val strengths: List<String> = emptyList(),
    val issues: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val evidenceItems: List<AtsDetailItem> = emptyList(),
    val matchedList: List<String> = emptyList(),
    val missingList: List<String> = emptyList()
)

enum class AtsCategoryIcon {
    KEYWORD, STRUCTURE, FORMATTING, PARSING
}

data class AtsDetailItem(
    val label: String,
    val isPositive: Boolean,
    val detail: String? = null,
    val suggestion: String? = null
)

data class RoleBenchmark(
    val role: String,
    val coreSkills: List<String>,
    val frameworkSkills: List<String>,
    val toolSkills: List<String>
)
