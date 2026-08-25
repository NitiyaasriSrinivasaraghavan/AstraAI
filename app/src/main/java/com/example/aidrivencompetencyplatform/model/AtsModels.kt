package com.example.aidrivencompetencyplatform.model

data class AtsScoreResult(
    val overallScore: Int,
    val keywordCoverage: AtsParameterScore,
    val resumeStructure: AtsParameterScore,
    val formattingSafety: AtsParameterScore,
    val parsingAccuracy: AtsParameterScore,
    val targetRole: String,
    val priorityFixes: List<String>
)

data class AtsParameterScore(
    val score: Int,
    val status: String,
    val summary: String,
    val evidence: List<AtsDetailItem> = emptyList(),
    val recommendations: List<String> = emptyList()
)

data class AtsDetailItem(
    val label: String,
    val isPositive: Boolean,
    val detail: String? = null
)

data class RoleBenchmark(
    val role: String,
    val coreSkills: List<String>,
    val frameworkSkills: List<String>,
    val toolSkills: List<String>
)
