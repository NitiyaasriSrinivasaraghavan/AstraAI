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
    KEYWORD, STRUCTURE, FORMATTING, PARSING, OVERALL
}

data class AtsDetailItem(
    val label: String,
    val isPositive: Boolean,
    val detail: String? = null,
    val suggestion: String? = null
)

fun AtsScoreResult.toOverallCalculation(): AtsCardCalculation {
    val status = when {
        overallScore >= 80 -> "ATS Optimized (Pass)"
        overallScore >= 60 -> "Competitive"
        overallScore >= 45 -> "Moderate Risk"
        else -> "Action Required"
    }

    val strengths = mutableListOf<String>()
    strengths.add("Keywords: ${keywordCoverage.score}% (+${String.format("%.1f", keywordCoverage.weightedContribution)} pts)")
    strengths.add("Structure: ${resumeStructure.score}% (+${String.format("%.1f", resumeStructure.weightedContribution)} pts)")
    strengths.add("Formatting: ${formattingSafety.score}% (+${String.format("%.1f", formattingSafety.weightedContribution)} pts)")
    strengths.add("Parsing: ${parsingAccuracy.score}% (+${String.format("%.1f", parsingAccuracy.weightedContribution)} pts)")

    val evidence = listOf(
        AtsDetailItem("Keywords (35%)", keywordCoverage.score >= 70, "${keywordCoverage.score}% match - +${String.format("%.1f", keywordCoverage.weightedContribution)} pts", if (keywordCoverage.score < 70) "Add missing target keywords" else null),
        AtsDetailItem("Structure (25%)", resumeStructure.score >= 75, "${resumeStructure.score}% layout - +${String.format("%.1f", resumeStructure.weightedContribution)} pts", if (resumeStructure.score < 75) "Add standard section headers" else null),
        AtsDetailItem("Formatting (20%)", formattingSafety.score >= 75, "${formattingSafety.score}% clean - +${String.format("%.1f", formattingSafety.weightedContribution)} pts", if (formattingSafety.score < 75) "Simplify multi-column layout" else null),
        AtsDetailItem("Parsing (20%)", parsingAccuracy.score >= 75, "${parsingAccuracy.score}% extracted - +${String.format("%.1f", parsingAccuracy.weightedContribution)} pts", if (parsingAccuracy.score < 75) "Include plain text contact header" else null)
    )

    return AtsCardCalculation(
        categoryName = "Overall ATS Compatibility",
        iconType = AtsCategoryIcon.OVERALL,
        score = overallScore,
        weightPercentage = 100,
        weightedContribution = overallScore.toDouble(),
        formulaText = "(Keywords × 35%) + (Structure × 25%) + (Formatting × 20%) + (Parsing × 20%)",
        calculationText = "(${keywordCoverage.score} × 0.35) + (${resumeStructure.score} × 0.25) + (${formattingSafety.score} × 0.20) + (${parsingAccuracy.score} × 0.20) = $overallScore/100",
        resultText = "$overallScore/100 weighted aggregate compatibility score for $targetRole",
        oneLineSummary = "Evaluated against automated Enterprise ATS parsers (Workday, Taleo, Greenhouse, iCIMS).",
        status = status,
        strengths = strengths,
        issues = priorityFixes,
        recommendations = if (priorityFixes.isNotEmpty()) priorityFixes else listOf("Your resume is well optimized for automated ATS parsers."),
        evidenceItems = evidence,
        matchedList = keywordCoverage.matchedList,
        missingList = keywordCoverage.missingList
    )
}

data class RoleBenchmark(
    val role: String,
    val coreSkills: List<String>,
    val frameworkSkills: List<String>,
    val toolSkills: List<String>
)
