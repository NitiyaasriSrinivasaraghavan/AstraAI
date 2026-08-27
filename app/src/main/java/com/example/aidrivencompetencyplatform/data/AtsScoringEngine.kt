package com.example.aidrivencompetencyplatform.data

import android.util.Log
import com.example.aidrivencompetencyplatform.model.*
import kotlin.math.roundToInt

class AtsScoringEngine {

    companion object {
        private const val TAG = "AtsScoringEngine"
        
        // Exact 4-component deterministic weights
        const val WEIGHT_KEYWORD = 0.35
        const val WEIGHT_STRUCTURE = 0.25
        const val WEIGHT_FORMATTING = 0.20
        const val WEIGHT_PARSING = 0.20
    }

    fun calculateScore(result: ResumeAnalysisResult, targetRole: String): AtsScoreResult {
        Log.d(TAG, "Calculating deterministic ATS scores for: $targetRole")

        // Candidate personal details from resume
        val candidateName = result.candidateName?.takeIf { it.isNotBlank() }
        val candidateEmail = result.candidateEmail?.takeIf { it.isNotBlank() }
        val candidatePhone = result.candidatePhone?.takeIf { it.isNotBlank() }
        val candidateLocation = result.candidateLocation?.takeIf { it.isNotBlank() }

        // 1. Card 1: Keyword Coverage (35%)
        val keywordCalc = calculateKeywordCoverage(result, targetRole)

        // 2. Card 2: Resume Structure (25%)
        val structureCalc = calculateResumeStructure(result, candidateName, candidateEmail)

        // 3. Card 3: Formatting Safety (20%)
        val formattingCalc = calculateFormattingSafety(result)

        // 4. Card 4: Parsing Accuracy (20%)
        val parsingCalc = calculateParsingAccuracy(result, candidateName, candidateEmail, candidatePhone, candidateLocation)

        // Base weighted sum: (Keyword × 0.35) + (Structure × 0.25) + (Formatting × 0.20) + (Parsing × 0.20)
        val weightedSum = (
            (keywordCalc.score * WEIGHT_KEYWORD) +
            (structureCalc.score * WEIGHT_STRUCTURE) +
            (formattingCalc.score * WEIGHT_FORMATTING) +
            (parsingCalc.score * WEIGHT_PARSING)
        )

        // In real-world ATS screening, a zero score in any fundamental pillar (0% keywords, 0% parsing accuracy, etc.)
        // represents a fatal disqualification. A resume with 0 keywords or 0 parsing cannot pass ATS screening.
        val zeroCategoryCount = listOf(keywordCalc.score, structureCalc.score, formattingCalc.score, parsingCalc.score).count { it == 0 }

        val finalScore = when {
            zeroCategoryCount >= 2 -> 0
            zeroCategoryCount == 1 -> {
                // If any single core feature has a score of 0, apply strict disqualification penalty (capped at 20)
                (weightedSum * 0.30).roundToInt().coerceIn(0, 20)
            }
            keywordCalc.score < 25 || parsingCalc.score < 25 -> {
                (weightedSum * 0.60).roundToInt().coerceIn(0, 45)
            }
            else -> weightedSum.roundToInt().coerceIn(0, 100)
        }

        val priorityFixes = mutableListOf<String>()
        if (keywordCalc.score < 70) {
            val missing = keywordCalc.missingList.take(3).joinToString(", ")
            if (missing.isNotBlank()) {
                priorityFixes.add("Add missing target keywords: $missing")
            } else {
                priorityFixes.add("Increase role-specific keyword density for $targetRole")
            }
        }
        if (structureCalc.score < 80) {
            val missing = structureCalc.missingList.joinToString(", ")
            if (missing.isNotBlank()) {
                priorityFixes.add("Include missing standard sections: $missing")
            } else {
                priorityFixes.add("Enhance standard resume section structure")
            }
        }
        if (formattingCalc.score < 80) {
            priorityFixes.add("Simplify layout to single-column format to eliminate ATS parsing risks")
        }
        if (parsingCalc.score < 80) {
            priorityFixes.add("Ensure contact info and key headers are in standard plaintext at the top")
        }

        return AtsScoreResult(
            overallScore = finalScore,
            keywordCoverage = keywordCalc,
            resumeStructure = structureCalc,
            formattingSafety = formattingCalc,
            parsingAccuracy = parsingCalc,
            targetRole = targetRole,
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            candidatePhone = candidatePhone,
            candidateLocation = candidateLocation,
            priorityFixes = priorityFixes
        )
    }

    private fun calculateKeywordCoverage(result: ResumeAnalysisResult, role: String): AtsCardCalculation {
        val userSkills = (result.extractedSkills ?: emptyList()).map { it.name.trim() }
        val userSkillsLower = userSkills.map { it.lowercase() }
        val benchmark = getBenchmarkForRole(role)
        
        val allRequired = (benchmark.coreSkills + benchmark.frameworkSkills + benchmark.toolSkills).distinct()
        val totalKeywords = if (allRequired.isNotEmpty()) allRequired.size else maxOf(1, userSkills.size)

        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()
        val evidence = mutableListOf<AtsDetailItem>()

        allRequired.forEach { req ->
            val reqLower = req.lowercase()
            val exact = userSkillsLower.any { it == reqLower }
            val partial = !exact && userSkillsLower.any { it.contains(reqLower) || reqLower.contains(it) }
            
            if (exact) {
                matched.add(req)
                evidence.add(AtsDetailItem(req, true, "Exact match detected in resume skills"))
            } else if (partial) {
                matched.add(req)
                evidence.add(AtsDetailItem(req, true, "Partial/semantic match detected"))
            } else {
                missing.add(req)
                evidence.add(AtsDetailItem(req, false, "Not detected in extracted skills", "Add $req to your skills or project descriptions"))
            }
        }

        val matchedCount = matched.size
        val score = if (totalKeywords > 0) {
            ((matchedCount.toDouble() / totalKeywords.toDouble()) * 100).roundToInt().coerceIn(0, 100)
        } else {
            100
        }

        val weightedContribution = Math.round(score * WEIGHT_KEYWORD * 10.0) / 10.0
        val formulaText = "(Matched Keywords / Required Role Benchmarks) × 100"
        val calculationText = "$matchedCount / $totalKeywords × 100 = $score%"
        val resultText = "$score% keyword match ($matchedCount of $totalKeywords required keywords detected for $role)"

        val status = when {
            score >= 80 -> "Strong Match"
            score >= 60 -> "Moderate Match"
            score > 0 -> "Low Match"
            else -> "No Match (0%)"
        }

        val summary = when {
            score == 0 -> "0% keyword match: None of the required $totalKeywords benchmark keywords for $role were found."
            score < 50 -> "Critical keyword deficit: Only $matchedCount of $totalKeywords target keywords identified."
            score < 80 -> "Moderate keyword coverage: $matchedCount of $totalKeywords required skills present."
            else -> "Strong keyword alignment with $matchedCount of $totalKeywords target skills matched."
        }

        val recommendations = mutableListOf<String>()
        if (missing.isNotEmpty()) {
            recommendations.add("Incorporate missing high-demand keywords: ${missing.take(4).joinToString(", ")}")
            recommendations.add("Highlight context and tools used alongside core technical skills.")
        } else {
            recommendations.add("Excellent keyword coverage for $role! Maintain current skill terminology.")
        }

        return AtsCardCalculation(
            categoryName = "Keyword Coverage",
            iconType = AtsCategoryIcon.KEYWORD,
            score = score,
            weightPercentage = (WEIGHT_KEYWORD * 100).toInt(),
            weightedContribution = weightedContribution,
            formulaText = formulaText,
            calculationText = calculationText,
            resultText = resultText,
            oneLineSummary = summary,
            status = status,
            strengths = matched.map { "Matched keyword: $it" },
            issues = missing.map { "Missing keyword: $it" },
            recommendations = recommendations,
            evidenceItems = evidence,
            matchedList = matched,
            missingList = missing
        )
    }

    private fun calculateResumeStructure(
        result: ResumeAnalysisResult,
        candidateName: String?,
        candidateEmail: String?
    ): AtsCardCalculation {
        val details = result.parsingAccuracyDetails
        val missingFromAi = (result.missingSections ?: emptyList()).map { it.lowercase() }
        
        val contactDetected = (details?.contactDetected == true) || !candidateEmail.isNullOrBlank() || !candidateName.isNullOrBlank()
        val summaryDetected = !missingFromAi.any { it.contains("summary") || it.contains("objective") } && !result.summary.isNullOrBlank()
        val experienceDetected = (details?.experienceDetected == true) || !(result.experience.isNullOrEmpty())
        val skillsDetected = (details?.skillsDetected == true) || !(result.extractedSkills.isNullOrEmpty())
        val educationDetected = (details?.educationDetected == true) || !(result.education.isNullOrEmpty())
        val projectsDetected = (details?.projectsDetected == true) || !(result.projectAnalysis.isNullOrEmpty())
        val certsDetected = !missingFromAi.any { it.contains("certif") }

        val evaluatedSections = listOf(
            Triple("Contact Information", contactDetected, "Standard contact details header"),
            Triple("Professional Summary", summaryDetected, "Career summary or objective statement"),
            Triple("Work Experience", experienceDetected, "Chronological professional experience"),
            Triple("Skills", skillsDetected, "Categorized technical and soft competencies"),
            Triple("Education", educationDetected, "Degrees, institutions, and graduation dates"),
            Triple("Projects", projectsDetected, "Demonstrated technical projects"),
            Triple("Certifications", certsDetected, "Relevant credentials and certifications")
        )

        val totalExpected = evaluatedSections.size
        val detected = mutableListOf<String>()
        val missing = mutableListOf<String>()
        val evidence = mutableListOf<AtsDetailItem>()

        evaluatedSections.forEach { (name, isPresent, desc) ->
            if (isPresent) {
                detected.add(name)
                evidence.add(AtsDetailItem(name, true, "$desc detected and recognized"))
            } else {
                missing.add(name)
                evidence.add(AtsDetailItem(name, false, "$desc missing or unlabeled", "Add a standard '$name' section heading"))
            }
        }

        val score = ((detected.size.toDouble() / totalExpected.toDouble()) * 100).roundToInt().coerceIn(0, 100)
        val weightedContribution = Math.round(score * WEIGHT_STRUCTURE * 10.0) / 10.0
        val formulaText = "(Recognized Standard Sections / Total Standard Sections) × 100"
        val calculationText = "${detected.size} / $totalExpected × 100 = $score%"
        val resultText = "$score% structural completeness (${detected.size} of $totalExpected standard sections detected)"

        val status = when {
            score >= 85 -> "Excellent"
            score >= 70 -> "Good"
            score > 0 -> "Incomplete"
            else -> "Missing Structure (0%)"
        }

        val summary = when {
            score == 0 -> "0% structure score: No standard ATS section headers were detected in the document."
            score < 60 -> "Incomplete section layout: Missing ${missing.joinToString(", ")}."
            score < 85 -> "Standard structure: ${detected.size} of $totalExpected sections identified, with minor gaps."
            else -> "Well-organized structure: ${detected.size} of $totalExpected standard ATS sections clearly delineated."
        }

        val recommendations = mutableListOf<String>()
        if (missing.isNotEmpty()) {
            recommendations.add("Add distinct standard section headers for: ${missing.joinToString(", ")}")
            recommendations.add("Use standard conventional headings (e.g., 'Work Experience', 'Education', 'Projects').")
        } else {
            recommendations.add("Great resume structure. All standard ATS sections are present and clearly delineated.")
        }

        return AtsCardCalculation(
            categoryName = "Resume Structure",
            iconType = AtsCategoryIcon.STRUCTURE,
            score = score,
            weightPercentage = (WEIGHT_STRUCTURE * 100).toInt(),
            weightedContribution = weightedContribution,
            formulaText = formulaText,
            calculationText = calculationText,
            resultText = resultText,
            oneLineSummary = summary,
            status = status,
            strengths = detected.map { "Detected section: $it" },
            issues = missing.map { "Missing section: $it" },
            recommendations = recommendations,
            evidenceItems = evidence,
            matchedList = detected,
            missingList = missing
        )
    }

    private fun calculateFormattingSafety(result: ResumeAnalysisResult): AtsCardCalculation {
        val risks = (result.formattingRisks ?: emptyList()).distinct()
        
        // Formatting penalty calculation (20 pts per detected risk)
        val penalty = (risks.size * 20).coerceAtMost(100)
        val score = (100 - penalty).coerceIn(0, 100)
        val weightedContribution = Math.round(score * WEIGHT_FORMATTING * 10.0) / 10.0

        val formulaText = "100 − (Formatting Risks × 20)"
        val calculationText = if (risks.isEmpty()) "100 − 0 = 100%" else "100 − $penalty = $score%"
        val resultText = if (risks.isEmpty()) {
            "100% formatting safety (0 parsing risks detected)"
        } else {
            "$score% formatting safety (${risks.size} potential parsing risks detected)"
        }

        val evidence = mutableListOf<AtsDetailItem>()
        if (risks.isEmpty()) {
            evidence.add(AtsDetailItem("Clean Text Layer", true, "No complex multi-column, table, or scan issues found"))
            evidence.add(AtsDetailItem("Standard Hierarchy", true, "Headings and bullet points are cleanly extractable"))
            evidence.add(AtsDetailItem("Font & Symbol Safety", true, "Standard ASCII/Unicode text with no disruptive symbols"))
        } else {
            risks.forEach { risk ->
                evidence.add(AtsDetailItem(risk, false, "ATS risk detected", "Simplify to standard single-column text layout"))
            }
        }

        val status = when {
            score >= 85 -> "Safe"
            score >= 70 -> "Moderate Risk"
            score > 0 -> "High Risk"
            else -> "Critical Formatting Risk (0%)"
        }

        val summary = when {
            risks.isEmpty() -> "100% ATS-safe layout: Single-column text without tables, columns, or graphic blockers."
            risks.size == 1 -> "1 formatting risk detected: ${risks.first()}."
            else -> "${risks.size} formatting risks detected: ${risks.take(2).joinToString(", ")}."
        }

        val recommendations = mutableListOf<String>()
        if (risks.isNotEmpty()) {
            recommendations.add("Use a single-column layout without tables or text boxes.")
            recommendations.add("Avoid decorative symbols, icons, or complex graphical dividers.")
            recommendations.add("Stick to standard ATS-safe fonts (Roboto, Arial, Calibri, Helvetica).")
        } else {
            recommendations.add("Formatting is ATS-compliant. Avoid adding graphics, tables, or columns in future edits.")
        }

        return AtsCardCalculation(
            categoryName = "Formatting Safety",
            iconType = AtsCategoryIcon.FORMATTING,
            score = score,
            weightPercentage = (WEIGHT_FORMATTING * 100).toInt(),
            weightedContribution = weightedContribution,
            formulaText = formulaText,
            calculationText = calculationText,
            resultText = resultText,
            oneLineSummary = summary,
            status = status,
            strengths = if (risks.isEmpty()) listOf("Clean text extractability", "Standard layout structure", "ATS-compatible typography") else emptyList(),
            issues = risks.map { "Formatting risk: $it" },
            recommendations = recommendations,
            evidenceItems = evidence,
            matchedList = if (risks.isEmpty()) listOf("Clean Layout", "Standard Fonts") else emptyList(),
            missingList = risks
        )
    }

    private fun calculateParsingAccuracy(
        result: ResumeAnalysisResult,
        candidateName: String?,
        candidateEmail: String?,
        candidatePhone: String?,
        candidateLocation: String?
    ): AtsCardCalculation {
        val details = result.parsingAccuracyDetails

        val parsedFields = mutableListOf<String>()
        val unparsedFields = mutableListOf<String>()
        val unavailableFields = mutableListOf<String>()
        val evidence = mutableListOf<AtsDetailItem>()

        // 1. Name
        if (!candidateName.isNullOrBlank() || details?.nameDetected == true) {
            val nameDisplay = candidateName ?: "Detected in resume header"
            parsedFields.add("Candidate Name")
            evidence.add(AtsDetailItem("Candidate Name", true, "Extracted: $nameDisplay"))
        } else {
            unparsedFields.add("Candidate Name")
            evidence.add(AtsDetailItem("Candidate Name", false, "Name could not be reliably extracted from header", "Place full name at the very top line"))
        }

        // 2. Email
        if (!candidateEmail.isNullOrBlank() || details?.contactDetected == true) {
            val emailDisplay = candidateEmail ?: "Detected in contact section"
            parsedFields.add("Email Address")
            evidence.add(AtsDetailItem("Email Address", true, "Extracted: $emailDisplay"))
        } else {
            unparsedFields.add("Email Address")
            evidence.add(AtsDetailItem("Email Address", false, "Email address not detected", "Include standard email format (e.g., name@domain.com)"))
        }

        // 3. Phone
        if (!candidatePhone.isNullOrBlank()) {
            parsedFields.add("Phone Number")
            evidence.add(AtsDetailItem("Phone Number", true, "Extracted: $candidatePhone"))
        } else {
            unavailableFields.add("Phone Number")
            evidence.add(AtsDetailItem("Phone Number", false, "No phone number detected in resume"))
        }

        // 4. Location
        if (!candidateLocation.isNullOrBlank()) {
            parsedFields.add("Location")
            evidence.add(AtsDetailItem("Location", true, "Extracted: $candidateLocation"))
        } else {
            unavailableFields.add("Location")
            evidence.add(AtsDetailItem("Location", false, "Location details not specified in header"))
        }

        // 5. Skills
        if ((details?.skillsDetected == true) || !(result.extractedSkills.isNullOrEmpty())) {
            val count = result.extractedSkills?.size ?: 0
            parsedFields.add("Skills")
            evidence.add(AtsDetailItem("Skills", true, "Successfully extracted $count categorized skills"))
        } else {
            unparsedFields.add("Skills")
            evidence.add(AtsDetailItem("Skills", false, "Skills could not be parsed cleanly"))
        }

        // 6. Education
        if ((details?.educationDetected == true) || !(result.education.isNullOrEmpty())) {
            parsedFields.add("Education")
            evidence.add(AtsDetailItem("Education", true, "Extracted education and academic history"))
        } else {
            unparsedFields.add("Education")
            evidence.add(AtsDetailItem("Education", false, "Education section not parsed"))
        }

        // 7. Experience
        if ((details?.experienceDetected == true) || !(result.experience.isNullOrEmpty())) {
            parsedFields.add("Work Experience")
            evidence.add(AtsDetailItem("Work Experience", true, "Extracted work history and roles"))
        } else {
            unparsedFields.add("Work Experience")
            evidence.add(AtsDetailItem("Work Experience", false, "Experience section not parsed"))
        }

        // 8. Projects
        if ((details?.projectsDetected == true) || !(result.projectAnalysis.isNullOrEmpty())) {
            parsedFields.add("Projects")
            evidence.add(AtsDetailItem("Projects", true, "Extracted project names and technical descriptions"))
        } else {
            unavailableFields.add("Projects")
            evidence.add(AtsDetailItem("Projects", false, "No projects extracted"))
        }

        val availableFieldsCount = (parsedFields.size + unparsedFields.size).coerceAtLeast(1)
        val score = ((parsedFields.size.toDouble() / availableFieldsCount.toDouble()) * 100).roundToInt().coerceIn(0, 100)
        val weightedContribution = Math.round(score * WEIGHT_PARSING * 10.0) / 10.0

        val formulaText = "(Extracted Fields / Total Available Target Fields) × 100"
        val calculationText = "${parsedFields.size} / $availableFieldsCount × 100 = $score%"
        val resultText = "$score% parsing accuracy (${parsedFields.size} of $availableFieldsCount target fields extracted)"

        val status = when {
            score >= 85 -> "High Accuracy"
            score >= 70 -> "Moderate Accuracy"
            score > 0 -> "Low Accuracy"
            else -> "Extraction Failed (0%)"
        }

        val summary = when {
            score == 0 -> "0% parsing accuracy: Crucial fields could not be extracted from the resume text."
            score < 60 -> "Partial data extraction: Extracted ${parsedFields.size} of $availableFieldsCount fields (${unparsedFields.joinToString(", ")} missing)."
            score < 85 -> "Good extraction fidelity: ${parsedFields.size} of $availableFieldsCount standard fields recognized."
            else -> "High-fidelity AI parsing: ${parsedFields.size} of $availableFieldsCount candidate fields extracted with full accuracy."
        }

        val recommendations = mutableListOf<String>()
        if (unparsedFields.isNotEmpty()) {
            recommendations.add("Ensure ${unparsedFields.joinToString(", ")} are in plain text without tables or columns.")
        }
        recommendations.add("Place candidate full name and contact information at the very top in standard text format.")

        return AtsCardCalculation(
            categoryName = "Parsing Accuracy",
            iconType = AtsCategoryIcon.PARSING,
            score = score,
            weightPercentage = (WEIGHT_PARSING * 100).toInt(),
            weightedContribution = weightedContribution,
            formulaText = formulaText,
            calculationText = calculationText,
            resultText = resultText,
            oneLineSummary = summary,
            status = status,
            strengths = parsedFields.map { "Successfully parsed: $it" },
            issues = unparsedFields.map { "Failed to parse: $it" },
            recommendations = recommendations,
            evidenceItems = evidence,
            matchedList = parsedFields,
            missingList = unparsedFields
        )
    }

    private fun getBenchmarkForRole(role: String): RoleBenchmark {
        val roleLower = role.lowercase()
        return when {
            roleLower.contains("android") -> RoleBenchmark(
                "Android Developer",
                listOf("Kotlin", "Java", "Android SDK", "Jetpack Compose"),
                listOf("Room Database", "Material Design", "REST APIs", "JSON"),
                listOf("Git", "Gradle", "Android Studio", "Firebase", "SQLite")
            )
            roleLower.contains("full stack") || roleLower.contains("fullstack") || roleLower.contains("mern") -> RoleBenchmark(
                "Full Stack Developer",
                listOf("HTML", "CSS", "JavaScript", "React", "Node.js"),
                listOf("Express.js", "REST APIs", "SQL", "MongoDB"),
                listOf("Git", "Docker", "Authentication")
            )
            roleLower.contains("java") -> RoleBenchmark(
                "Java Developer",
                listOf("Java", "OOP", "Collections", "Multithreading"),
                listOf("Spring", "Spring Boot", "Hibernate", "REST APIs", "JDBC"),
                listOf("SQL", "Maven", "Git")
            )
            roleLower.contains("data analyst") -> RoleBenchmark(
                "Data Analyst",
                listOf("Python", "SQL", "Excel", "Statistics"),
                listOf("Pandas", "NumPy", "Data Visualization", "Data Cleaning"),
                listOf("Power BI", "Tableau", "Git")
            )
            roleLower.contains("data scientist") -> RoleBenchmark(
                "Data Scientist",
                listOf("Python", "SQL", "Statistics", "Probability"),
                listOf("Pandas", "NumPy", "Scikit-learn", "Machine Learning", "Data Visualization"),
                listOf("Feature Engineering", "Model Evaluation", "Git")
            )
            roleLower.contains("machine learning") || roleLower.contains("ml") -> RoleBenchmark(
                "Machine Learning Engineer",
                listOf("Python", "Machine Learning", "Deep Learning"),
                listOf("NumPy", "Pandas", "Scikit-learn", "TensorFlow", "PyTorch"),
                listOf("SQL", "Git", "Model Deployment", "REST APIs")
            )
            roleLower.contains("frontend") || roleLower.contains("web") -> RoleBenchmark(
                "Frontend Developer",
                listOf("HTML", "CSS", "JavaScript", "React", "TypeScript"),
                listOf("Responsive Design", "REST APIs", "UI/UX Principles"),
                listOf("Git", "Testing")
            )
            roleLower.contains("backend") -> RoleBenchmark(
                "Backend Developer",
                listOf("Java", "Python", "Node.js", "SQL"),
                listOf("REST APIs", "PostgreSQL", "MongoDB", "Spring Boot"),
                listOf("Authentication", "Git", "Docker")
            )
            roleLower.contains("ui") || roleLower.contains("ux") || roleLower.contains("design") -> RoleBenchmark(
                "UI/UX Designer",
                listOf("Figma", "Wireframing", "Prototyping", "User Research"),
                listOf("User Flows", "Interaction Design", "Visual Design", "Design Systems"),
                listOf("Typography", "Color Theory", "Usability Testing")
            )
            else -> RoleBenchmark(
                "Software Engineer",
                listOf("Problem Solving", "Data Structures", "Algorithms", "Software Engineering"),
                listOf("System Design", "REST APIs", "Database", "Testing"),
                listOf("Git", "CI/CD", "Docker")
            )
        }
    }
}
