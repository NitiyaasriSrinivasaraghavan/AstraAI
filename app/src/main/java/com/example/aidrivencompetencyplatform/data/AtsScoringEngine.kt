package com.example.aidrivencompetencyplatform.data

import android.util.Log
import com.example.aidrivencompetencyplatform.model.*
import kotlin.math.roundToInt

class AtsScoringEngine {

    companion object {
        private const val TAG = "AtsScoringEngine"
        
        // Deterministic weights
        private const val WEIGHT_KEYWORD = 0.35
        private const val WEIGHT_STRUCTURE = 0.25
        private const val WEIGHT_FORMATTING = 0.20
        private const val WEIGHT_PARSING = 0.20
    }

    fun calculateScore(result: ResumeAnalysisResult, targetRole: String): AtsScoreResult {
        Log.d(TAG, "Calculating deterministic ATS scores for: $targetRole")

        val keywordScore = calculateKeywordCoverage(result, targetRole)
        val structureScore = calculateResumeStructure(result)
        val formattingScore = calculateFormattingSafety(result)
        val parsingScore = calculateParsingAccuracy(result)

        val finalScore = (
            (keywordScore.score * WEIGHT_KEYWORD) +
            (structureScore.score * WEIGHT_STRUCTURE) +
            (formattingScore.score * WEIGHT_FORMATTING) +
            (parsingScore.score * WEIGHT_PARSING)
        ).roundToInt().coerceIn(0, 100)

        val priorityFixes = mutableListOf<String>()
        if (keywordScore.score < 60) priorityFixes.add("Increase role-specific keyword density")
        if (structureScore.score < 70) priorityFixes.add("Add missing standard resume sections")
        if (formattingScore.score < 80) priorityFixes.add("Simplify layout for better ATS parseability")
        if (parsingScore.score < 80) priorityFixes.add("Ensure resume text is cleanly selectable")

        return AtsScoreResult(
            overallScore = finalScore,
            keywordCoverage = keywordScore,
            resumeStructure = structureScore,
            formattingSafety = formattingScore,
            parsingAccuracy = parsingScore,
            targetRole = targetRole,
            priorityFixes = priorityFixes
        )
    }

    private fun calculateKeywordCoverage(result: ResumeAnalysisResult, role: String): AtsParameterScore {
        val userSkills = (result.extractedSkills ?: emptyList()).map { it.name.lowercase() }
        val benchmark = getBenchmarkForRole(role)
        
        val allRequired = benchmark.coreSkills + benchmark.frameworkSkills + benchmark.toolSkills
        if (allRequired.isEmpty()) return AtsParameterScore(100, "Excellent", "Universal professional match", emptyList())

        var matchCount = 0.0
        val evidence = mutableListOf<AtsDetailItem>()
        val missing = mutableListOf<String>()

        allRequired.forEach { req ->
            val reqLower = req.lowercase()
            if (userSkills.contains(reqLower)) {
                matchCount += 1.0
                evidence.add(AtsDetailItem(req, true, "Exact match found"))
            } else if (userSkills.any { it.contains(reqLower) || reqLower.contains(it) }) {
                matchCount += 0.5
                evidence.add(AtsDetailItem(req, true, "Partial semantic match"))
            } else {
                missing.add(req)
                evidence.add(AtsDetailItem(req, false, "Not detected"))
            }
        }

        val score = (matchCount / allRequired.size * 100).roundToInt()
        
        return AtsParameterScore(
            score = score,
            status = if (score >= 70) "Strong Match" else if (score >= 40) "Moderate" else "Weak Match",
            summary = "Found ${matchCount.toInt()} out of ${allRequired.size} key requirements.",
            evidence = evidence,
            recommendations = if (missing.isNotEmpty()) listOf("Consider adding: ${missing.take(3).joinToString()}") else emptyList()
        )
    }

    private fun calculateResumeStructure(result: ResumeAnalysisResult): AtsParameterScore {
        val details = result.parsingAccuracyDetails
        val missing = result.missingSections ?: emptyList()
        
        val checklist = listOf(
            Triple("Contact Info", details?.contactDetected ?: false, 10),
            Triple("Summary", !missing.contains("Summary"), 10),
            Triple("Experience", details?.experienceDetected ?: false, 25),
            Triple("Skills", details?.skillsDetected ?: false, 20),
            Triple("Education", details?.educationDetected ?: false, 15),
            Triple("Projects", details?.projectsDetected ?: false, 15),
            Triple("Certifications", !missing.contains("Certifications"), 5)
        )

        var score = 0
        val items = mutableListOf<AtsDetailItem>()
        checklist.forEach { (label, detected, weight) ->
            if (detected) score += weight
            items.add(AtsDetailItem(label, detected, if (detected) "Section identified" else "Section missing or empty"))
        }

        return AtsParameterScore(
            score = score,
            status = if (score >= 80) "Excellent" else if (score >= 60) "Good" else "Needs Work",
            summary = "Organization of key professional components.",
            evidence = items
        )
    }

    private fun calculateFormattingSafety(result: ResumeAnalysisResult): AtsParameterScore {
        val risks = result.formattingRisks ?: emptyList()
        val score = (100 - (risks.size * 20)).coerceIn(0, 100)
        
        val items = risks.map { AtsDetailItem(it, false, "ATS risk detected") }
        
        return AtsParameterScore(
            score = score,
            status = if (score >= 80) "Safe" else "High Risk",
            summary = "Evaluation of layout complexity and parseability.",
            evidence = if (risks.isEmpty()) listOf(AtsDetailItem("Clean Layout", true, "No formatting risks found")) else items
        )
    }

    private fun calculateParsingAccuracy(result: ResumeAnalysisResult): AtsParameterScore {
        val details = result.parsingAccuracyDetails
        val checks = listOf(
            details?.nameDetected ?: false,
            details?.contactDetected ?: false,
            details?.educationDetected ?: false,
            details?.skillsDetected ?: false,
            details?.experienceDetected ?: false,
            details?.projectsDetected ?: false
        )
        
        val completeness = (checks.count { it }.toFloat() / checks.size * 100).roundToInt()
        // Integrity heuristic based on formatting risks
        val integrity = (100 - (result.formattingRisks?.size ?: 0) * 10).coerceIn(0, 100)
        
        val score = (completeness * 0.6 + integrity * 0.4).roundToInt()

        return AtsParameterScore(
            score = score,
            status = if (score >= 85) "Excellent" else "Variable",
            summary = "Reliability of AI data extraction from your file.",
            evidence = listOf(
                AtsDetailItem("Completeness", completeness >= 80, "$completeness% data coverage"),
                AtsDetailItem("Text Integrity", integrity >= 80, "$integrity% reading confidence")
            )
        )
    }

    private fun getBenchmarkForRole(role: String): RoleBenchmark {
        val roleLower = role.lowercase()
        return when {
            roleLower.contains("android") -> RoleBenchmark(
                "Android Developer",
                listOf("Kotlin", "Java", "Android SDK"),
                listOf("Jetpack Compose", "MVVM", "Retrofit", "Hilt"),
                listOf("Git", "Android Studio", "JUnit")
            )
            roleLower.contains("frontend") || roleLower.contains("react") -> RoleBenchmark(
                "Frontend Developer",
                listOf("JavaScript", "TypeScript", "HTML", "CSS"),
                listOf("React", "Next.js", "Redux", "Tailwind"),
                listOf("Webpack", "Git", "Jest")
            )
            roleLower.contains("backend") || roleLower.contains("node") -> RoleBenchmark(
                "Backend Developer",
                listOf("Java", "Python", "Node.js", "SQL"),
                listOf("Spring Boot", "Express", "PostgreSQL", "Microservices"),
                listOf("Docker", "AWS", "CI/CD")
            )
            else -> RoleBenchmark(
                "Professional",
                listOf("Communication", "Problem Solving"),
                listOf("Project Management", "Teamwork"),
                listOf("Office", "Git")
            )
        }
    }
}
