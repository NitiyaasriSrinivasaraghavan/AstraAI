package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.zip.ZipInputStream
import com.example.aidrivencompetencyplatform.model.ParsedSectionItem

class ResumeParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    companion object {
        private const val TAG = "ResumeParser"
        const val ERROR_SCANNED_PDF = "[ERROR_SCANNED_PDF]"

        val SECTION_HEADER_KEYWORDS = setOf(
            "internship", "internships", "experience", "experiences", "work experience", "professional experience",
            "employment", "employment history", "work history", "career history", "career summary",
            "education", "academic", "academics", "academic background", "qualifications", "educational qualifications",
            "skill", "skills", "technical skills", "core skills", "key skills", "competencies", "core competencies",
            "technologies", "tech stack", "tools", "tools & technologies", "areas of expertise",
            "project", "projects", "key projects", "academic projects", "personal projects", "portfolio",
            "certification", "certifications", "certificates", "courses", "licenses", "licenses & certifications",
            "language", "languages", "language proficiency", "languages known",
            "patent", "patents", "publication", "publications", "research", "research papers",
            "achievement", "achievements", "award", "awards", "honors", "accomplishments",
            "summary", "professional summary", "executive summary", "career objective", "objective", "profile", "about", "about me",
            "contact", "contact info", "contact information", "personal details", "personal info", "personal information",
            "declaration", "references", "reference", "activity", "activities", "extracurricular", "co-curricular",
            "volunteer", "volunteering", "leadership", "positions of responsibility", "interests", "hobbies", "training", "workshops",
            "resume", "curriculum vitae", "cv", "page", "github", "linkedin"
        )

        fun isSectionHeader(text: String): Boolean {
            val clean = text.trim().lowercase().removeSuffix(":").trim()
            if (clean.isBlank()) return false
            if (SECTION_HEADER_KEYWORDS.contains(clean)) return true
            if (SECTION_HEADER_KEYWORDS.any { clean == it || clean.startsWith("$it ") || clean.endsWith(" $it") }) return true
            return false
        }
    }

    fun extractText(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val extension = getExtensionFromUri(uri, mimeType)
        
        Log.d("ResumeParser", "Uri: $uri")
        Log.d("ResumeParser", "Detected MIME: $mimeType, Extension: $extension")

        return try {
            var text = when {
                mimeType == "application/pdf" || extension.equals("pdf", ignoreCase = true) -> {
                    extractFromPdf(uri)
                }
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || 
                extension.equals("docx", ignoreCase = true) -> {
                    extractFromDocx(uri)
                }
                else -> {
                    // Try PDF first, then DOCX, then plain text
                    try {
                        extractFromPdf(uri)
                    } catch (ePdf: Exception) {
                        try {
                            extractFromDocx(uri)
                        } catch (eDocx: Exception) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    stream.bufferedReader().readText()
                                } ?: ""
                            } catch (eStream: Exception) {
                                Log.w("ResumeParser", "Fallback stream read failed", eStream)
                                ""
                            }
                        }
                    }
                }
            }
            
            if (text == ERROR_SCANNED_PDF) return text

            if (text.isBlank()) {
                // If extraction returned empty, try reading as raw text
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        text = stream.bufferedReader().readText()
                    }
                } catch (e: Exception) {
                    Log.w("ResumeParser", "Secondary raw text read failed", e)
                }
            }

            val cleanedText = cleanText(text)
            Log.d("ResumeParser", "Extracted text length: ${cleanedText.length}")
            cleanedText
        } catch (e: Exception) {
            Log.e("ResumeParser", "Text extraction failed", e)
            ""
        }
    }

    fun getSampleResumeText(targetRole: String = "Android Developer"): String {
        return """
            Alex Chen
            Email: alex.chen.dev@example.com | Phone: (415) 555-0199 | Location: San Francisco, CA
            GitHub: github.com/alexchen-dev | LinkedIn: linkedin.com/in/alexchen-android

            PROFESSIONAL SUMMARY
            High-impact Android Software Engineer with 4+ years of experience designing, architecting, and shipping high-performance mobile applications using Kotlin, Jetpack Compose, Coroutines, and clean architecture (MVVM/MVI). Proven track record of optimizing app cold start times by 35% and scaling applications to 500K+ monthly active users.

            WORK EXPERIENCE
            Senior Android Developer | TechFlow Mobile Solutions
            June 2022 – Present | San Francisco, CA
            - Spearheaded the complete migration of legacy Java/XML codebase to 100% Kotlin and Jetpack Compose, reducing UI defect rates by 42%.
            - Implemented reactive state management using Kotlin StateFlow, SharedFlow, and Coroutines, reducing unhandled crash rates by 65%.
            - Integrated RESTful APIs and GraphQL services using Retrofit, OkHttp, and Kotlinx Serialization with robust offline caching in Room Database.
            - Designed modular Gradle multi-module project architecture with Dagger Hilt dependency injection, accelerating team build speeds by 28%.
            - Maintained 88% unit test coverage using JUnit 5, MockK, and Robolectric, integrated with GitHub Actions CI/CD pipelines.

            Android Software Engineer | Nexus Apps Studio
            August 2020 – May 2022 | San Jose, CA
            - Developed and published 3 consumer fintech Android applications on Google Play Store with 4.8-star average ratings.
            - Engineered custom Compose Canvas visualizers and dynamic animations adhering to Material Design 3 guidelines.
            - Integrated Firebase Cloud Messaging (FCM), Crashlytics, and WorkManager background job scheduling for efficient battery optimization.
            - Collaborated with cross-functional product, design, and backend engineering teams in 2-week Agile sprint cycles.

            TECHNICAL SKILLS
            - Programming Languages: Kotlin, Java, SQL, Python
            - Android Frameworks: Jetpack Compose, Jetpack Navigation, Android SDK, ViewModels, LiveData, Flow, Coroutines
            - Architecture & DI: MVVM, MVI, Clean Architecture, Dagger Hilt, Koin
            - Networking & Data: Retrofit, OkHttp, Room Database, SQLite, GraphQL, Protocol Buffers, DataStore
            - Tools & DevOps: Git, GitHub Actions, Android Studio, Gradle, CI/CD, Fastlane, Firebase, Docker, Postman
            - Testing: JUnit, MockK, Espresso, Compose UI Testing, Robolectric

            KEY PROJECTS
            Astra AI Career Platform | Kotlin, Jetpack Compose, Gemini AI, Room DB
            - Architected mobile competency analysis engine featuring deterministic ATS scoring and resume parsing.
            - Integrated Gemini AI generative endpoints for real-time skill gap analysis and automated career trajectory recommendations.

            CryptoTrack Real-Time Portfolio | Kotlin, Compose, WebSocket, Coroutines, Hilt
            - Built offline-first cryptocurrency portfolio tracker streaming live prices via WebSockets to 50,000+ active users.
            - Utilized Room DB for transactions and EncryptedSharedPreferences for API key security.

            EDUCATION
            Bachelor of Science in Computer Science
            University of California, Berkeley | 2016 – 2020
            - Relevant Coursework: Data Structures, Algorithms, Mobile Computing, Operating Systems, Database Management.

            CERTIFICATIONS
            - Google Associate Android Developer Certification (AAD)
            - Meta Android Professional Developer Specialization
        """.trimIndent()
    }

    private fun extractFromPdf(uri: Uri): String {
        Log.d("ResumeParser", "Starting PDF extraction...")
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.w("ResumeParser", "PDF bytes are empty")
                return ""
            }

            PDDocument.load(bytes).use { document ->
                val pageCount = document.numberOfPages
                Log.d("ResumeParser", "PDF Pages: $pageCount")
                
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                val text = stripper.getText(document)
                
                // Basic heuristic to detect scanned PDFs (images instead of text)
                if (text.trim().length < 20 && pageCount > 0) {
                    Log.w("ResumeParser", "PDF has $pageCount pages but almost no text. Likely scanned.")
                    return ERROR_SCANNED_PDF
                }
                
                text
            }
        } catch (e: Exception) {
            Log.e("ResumeParser", "Error extracting text from PDF: ${e.message}", e)
            ""
        }
    }

    private fun extractFromDocx(uri: Uri): String {
        Log.d("ResumeParser", "Starting DOCX extraction...")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                // Word document text is stored in word/document.xml
                if (entry.name == "word/document.xml") {
                    return parseDocxXml(zipInputStream)
                }
                entry = zipInputStream.nextEntry
            }
        }
        return ""
    }

    private fun parseDocxXml(inputStream: InputStream): String {
        val content = inputStream.bufferedReader().readText()
        val sb = StringBuilder()
        
        // Match <w:t> tags which contain text in OOXML
        val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
        regex.findAll(content).forEach { match ->
            sb.append(match.groupValues[1]).append(" ")
        }
        
        return sb.toString().trim()
    }

    private fun cleanText(text: String): String {
        // Preserve meaningful whitespace but collapse excessive formatting artifacts
        return text.replace(Regex("\\r\\n|\\r|\\n"), "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\n\n")
            .trim()
    }

    private fun getExtensionFromUri(uri: Uri, mimeType: String?): String {
        var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        
        if (extension == null) {
            val fileName = getFileName(uri)
            extension = fileName?.substringAfterLast('.', "")
        }
        
        return extension ?: ""
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun parseCandidateInfo(rawText: String): ParsedCandidateInfo {
        if (rawText.isBlank() || rawText == ERROR_SCANNED_PDF) {
            return ParsedCandidateInfo()
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Email extraction
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val emailMatch = emailRegex.find(rawText)?.value?.trim()

        // 2. Phone extraction (supports international, Indian +91 with spaces/dashes, standard 10-digit, and US formats)
        val phoneRegex = Regex("(?:\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}|(?:\\+?91[\\s-]?)?[6-9]\\d{9}|(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}")
        val phoneMatch = phoneRegex.find(rawText)?.value?.trim()

        // 4. Location extraction keywords & patterns
        var candidateLocation: String? = null
        val locationKeywords = listOf(
            "srirangam", "trichy", "tiruchirappalli", "chennai", "coimbatore", "madurai", "salem",
            "bangalore", "bengaluru", "hyderabad", "pune", "mumbai", "delhi", "noida", "gurgaon", "gurugram",
            "kolkata", "kochi", "trivandrum", "san francisco", "new york", "london", "seattle",
            "austin", "tamil nadu", "karnataka", "kerala", "maharashtra", "telangana", "india", "usa", "california", "texas"
        )
        val locationRegex = Regex("([A-Za-z\\s]+),\\s*([A-Za-z\\s]+(?:,\\s*[A-Za-z\\s]+)?)")

        // First pass: extract location from top lines
        for (i in 0 until minOf(15, lines.size)) {
            val line = lines[i]
            val lower = line.lowercase()
            if (lower.contains("university") || lower.contains("college") || lower.contains("school") || 
                lower.contains("institute") || lower.contains("project") || lower.contains("experience") ||
                lower.contains("education") || lower.contains("internship") || lower.contains("description")) continue

            if (locationKeywords.any { lower.contains(it) } || lower.startsWith("location:") || lower.contains("location :")) {
                // Strip "Location:" prefix if present
                val cleanedLine = line.replace(Regex("(?i)^location\\s*:\\s*"), "").trim()
                val pipeParts = cleanedLine.split(Regex("[|•·,]")).map { it.trim() }.filter { it.isNotBlank() }
                val locPart = pipeParts.firstOrNull { p -> locationKeywords.any { p.lowercase().contains(it) } }
                if (locPart != null && locPart.length < 50 && !locPart.contains("@") && !locPart.any { it.isDigit() }) {
                    candidateLocation = locPart.trim()
                    break
                }
                val locMatch = locationRegex.find(cleanedLine)
                if (locMatch != null && locMatch.value.length < 50) {
                    candidateLocation = locMatch.value.trim()
                    break
                }
            }
        }

        // 3. Name extraction heuristic from top header (ensuring NO location is glued to candidate name)
        var candidateName: String? = null
        val blacklistJobTitles = setOf(
            "developer", "engineer", "designer", "architect", "analyst", "consultant",
            "intern", "manager", "lead", "specialist", "student", "fresher", "programmer",
            "software", "hardware", "frontend", "backend", "fullstack", "full stack",
            "b.tech", "b.e", "m.tech", "m.e", "bca", "mca", "bsc", "msc", "phd", "resume", "curriculum", "vitae"
        )

        for (i in 0 until minOf(12, lines.size)) {
            val line = lines[i]
            val lower = line.lowercase().trim()

            // Skip lines that match section headers
            if (isSectionHeader(line)) continue
            if (line.length > 120 || line.length < 2) continue

            // If line contains delimiter like comma, pipe, dash, tab, or bullet, separate name and other info
            val lineParts = line.split(Regex("[|•·\\-\t]")).map { it.trim() }.filter { it.isNotBlank() }
            val potentialNamePart = if (lineParts.size > 1) {
                // Find the part that is clean: no email, no phone, no URL, no digit, not location, not job title
                lineParts.firstOrNull { part ->
                    val partLower = part.lowercase()
                    !part.contains("@") && !part.contains("http") && !part.contains(".com") &&
                    !part.any { it.isDigit() } &&
                    !locationKeywords.any { partLower.contains(it) } &&
                    !blacklistJobTitles.any { partLower.contains(it) } &&
                    part.length in 2..40
                }
            } else {
                // Skip lines with emails, phones, or URLs if line has no delimiters
                if (emailMatch != null && line.contains(emailMatch)) continue
                if (phoneMatch != null && line.contains(phoneMatch)) continue
                if (line.contains("@") || line.contains("http") || line.contains("www.") || line.contains(".com") || line.contains("linkedin") || line.contains("github")) continue
                if (line.any { it.isDigit() }) continue
                if (line.length > 70) continue

                // If it contains a comma e.g. "Nithiyaa S, Trichy" or "Alex Chen, San Francisco"
                val commaParts = line.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (commaParts.size > 1) {
                    val nameCandidate = commaParts[0]
                    val rem = commaParts.drop(1).joinToString(", ")
                    if (locationKeywords.any { rem.lowercase().contains(it) } && candidateLocation == null) {
                        candidateLocation = rem
                    }
                    nameCandidate
                } else {
                    line
                }
            }

            if (potentialNamePart != null) {
                val candidatePartLower = potentialNamePart.lowercase()
                if (blacklistJobTitles.any { candidatePartLower.contains(it) }) continue
                if (locationKeywords.any { candidatePartLower == it }) continue

                // Clean out any trailing location keywords from name
                var cleanedNamePart: String = potentialNamePart
                for (locKw in locationKeywords) {
                    cleanedNamePart = cleanedNamePart.replace(Regex("(?i)\\b$locKw\\b"), "").trim()
                }
                cleanedNamePart = cleanedNamePart.replace(Regex("[,|•·\\-]+$"), "").trim()

                val words = cleanedNamePart.split(Regex("[\\s]+")).filter { it.isNotBlank() }
                if (words.size in 1..4 && words.all { w -> w.all { it.isLetter() || it == '.' } }) {
                    if (words.any { w -> SECTION_HEADER_KEYWORDS.contains(w.lowercase()) || blacklistJobTitles.contains(w.lowercase()) || locationKeywords.contains(w.lowercase()) }) {
                        continue
                    }

                    val isTitleCase = words.all { it.first().isUpperCase() }
                    val isAllUpper = words.all { it.all { c -> c.isUpperCase() || c == '.' } }
                    if (isTitleCase || isAllUpper) {
                        candidateName = cleanedNamePart.trim()
                        break
                    }
                }
            }
        }

        // Fallback: derive name from email if name wasn't cleanly isolated
        if (candidateName.isNullOrBlank() && emailMatch != null) {
            val emailPrefix = emailMatch.substringBefore("@").replace(Regex("[0-9_]+"), " ").trim()
            val candidateWords = emailPrefix.split(Regex("[.\\-_\\s]+")).filter { it.length >= 2 }
            if (candidateWords.size in 1..3 && candidateWords.all { it.all { c -> c.isLetter() } }) {
                candidateName = candidateWords.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

        return ParsedCandidateInfo(
            name = candidateName,
            email = emailMatch,
            phone = phoneMatch,
            location = candidateLocation
        )
    }

    fun parseResumeStructure(rawText: String): DeterministicResumeStructure {
        if (rawText.isBlank() || rawText == ERROR_SCANNED_PDF) {
            return DeterministicResumeStructure(
                candidateInfo = ParsedCandidateInfo(),
                sections = emptyList(),
                extractedSkills = emptyList(),
                extractedProjects = emptyList()
            )
        }

        val candidateInfo = parseCandidateInfo(rawText)
        val lowerText = rawText.lowercase()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Contact Information
        val hasContact = !candidateInfo.email.isNullOrBlank() || !candidateInfo.phone.isNullOrBlank() || !candidateInfo.name.isNullOrBlank()
        val contactDetails = mutableListOf<String>()
        candidateInfo.name?.let { contactDetails.add(it) }
        candidateInfo.email?.let { contactDetails.add(it) }
        candidateInfo.phone?.let { contactDetails.add(it) }
        candidateInfo.location?.let { contactDetails.add(it) }

        // 2. Education
        val hasEducation = lowerText.contains("education") || lowerText.contains("bachelor") || 
                           lowerText.contains("b.tech") || lowerText.contains("b.e") || 
                           lowerText.contains("m.tech") || lowerText.contains("university") || 
                           lowerText.contains("college") || lowerText.contains("degree")
        val educationDetails = mutableListOf<String>()
        lines.filter { l ->
            val lw = l.lowercase()
            lw.contains("bachelor") || lw.contains("b.tech") || lw.contains("master") || 
            lw.contains("university") || lw.contains("institute") || lw.contains("college")
        }.take(2).forEach { educationDetails.add(it) }

        // 3. Work Experience
        val hasExperience = lowerText.contains("experience") || lowerText.contains("employment") || 
                            lowerText.contains("work history") || lowerText.contains("internship") ||
                            lowerText.contains("developer") || lowerText.contains("engineer")
        val experienceDetails = mutableListOf<String>()
        lines.filter { l ->
            val lw = l.lowercase()
            (lw.contains("intern") || lw.contains("developer") || lw.contains("engineer") || lw.contains("analyst")) && l.length < 60
        }.take(2).forEach { experienceDetails.add(it) }

        // 4. Skills
        val hasSkills = lowerText.contains("skills") || lowerText.contains("technical skills") || 
                        lowerText.contains("competencies") || lowerText.contains("technologies") ||
                        lowerText.contains("tech stack")

        // 5. Projects
        val projectKeywords = listOf("project", "projects", "academic projects", "personal projects", "technical projects", "relevant projects", "key projects", "project experience", "portfolio")
        val hasProjects = projectKeywords.any { lowerText.contains(it) }
        
        // Extract project names
        val extractedProjectsList = mutableListOf<String>()
        var inProjectSection = false
        for (line in lines) {
            val lw = line.lowercase().trim().removeSuffix(":")
            if (projectKeywords.contains(lw) || (lw.contains("project") && lw.length < 30)) {
                inProjectSection = true
                continue
            }
            if (inProjectSection) {
                // Section end check
                if (isSectionHeader(line) && !projectKeywords.any { line.lowercase().contains(it) }) {
                    inProjectSection = false
                    continue
                }
                // Check if line looks like a project title (bullet point, bold-ish, short title, or title with tech stack)
                val cleanLine = line.removePrefix("•").removePrefix("-").removePrefix("*").trim()
                if (cleanLine.length in 3..60 && !cleanLine.startsWith("http") && !cleanLine.contains("@")) {
                    val projectCandidate = cleanLine.substringBefore("|").substringBefore("–").substringBefore("-").trim()
                    if (projectCandidate.length in 3..50 && !projectCandidate.lowercase().contains("responsibilities") && !projectCandidate.lowercase().contains("overview")) {
                        extractedProjectsList.add(projectCandidate)
                    }
                }
            }
        }

        // 6. Certifications
        val hasCertifications = lowerText.contains("certification") || lowerText.contains("certified") || 
                                lowerText.contains("certificate") || lowerText.contains("courses") ||
                                lowerText.contains("credential")

        // 7. Languages
        val hasLanguages = lowerText.contains("languages") || lowerText.contains("language proficiency") ||
                           lowerText.contains("english") || lowerText.contains("spanish") ||
                           lowerText.contains("hindi") || lowerText.contains("french") || lowerText.contains("german")

        // 8. Patents / Publications
        val hasPatents = lowerText.contains("patent") || lowerText.contains("publication") || 
                         lowerText.contains("research paper") || lowerText.contains("published")

        val sectionsList = listOf(
            ParsedSectionItem("Contact Information", hasContact, contactDetails),
            ParsedSectionItem("Education", hasEducation, educationDetails),
            ParsedSectionItem("Work Experience", hasExperience, experienceDetails),
            ParsedSectionItem("Skills", hasSkills),
            ParsedSectionItem("Projects", hasProjects, extractedProjectsList.take(3)),
            ParsedSectionItem("Certifications", hasCertifications),
            ParsedSectionItem("Languages", hasLanguages),
            ParsedSectionItem("Patents & Publications", hasPatents)
        )

        // Extract skills deterministically from text
        val knownSkillKeywords = listOf(
            "Kotlin", "Java", "Python", "C++", "C#", "JavaScript", "TypeScript", "Swift", "Go", "Rust",
            "SQL", "MySQL", "PostgreSQL", "MongoDB", "Room Database", "SQLite", "Firebase", "Redis",
            "Jetpack Compose", "Android", "React", "Node.js", "Spring Boot", "Django", "Flask", "Express",
            "Docker", "Kubernetes", "AWS", "GCP", "Azure", "Git", "GitHub", "REST APIs", "GraphQL",
            "CI/CD", "Linux", "Figma", "Tableau", "Power BI", "Postman", "Machine Learning", "TensorFlow",
            "PyTorch", "Coroutines", "MVVM", "Clean Architecture", "HTML", "CSS", "Tailwind CSS", "Pandas", "NumPy"
        )

        val extractedSkillsList = mutableListOf<String>()
        for (skill in knownSkillKeywords) {
            val pattern = Regex("\\b" + Regex.escape(skill) + "\\b", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(rawText)) {
                extractedSkillsList.add(skill)
            }
        }

        val detectedJd = detectJobDescription(rawText)

        return DeterministicResumeStructure(
            candidateInfo = candidateInfo,
            sections = sectionsList,
            extractedSkills = extractedSkillsList.distinct(),
            extractedProjects = extractedProjectsList.distinct().take(4),
            detectedJobDescription = detectedJd
        )
    }

    /**
     * Contextual and semantic Job Description detection.
     * Candidate resumes containing education, student/fresher projects, work experiences, or skills
     * DO NOT count as a Job Description unless an explicit, dedicated Job Description or Job Posting
     * section is present with employer hiring requirements.
     */
    fun detectJobDescription(rawText: String): com.example.aidrivencompetencyplatform.model.JobDescriptionSection? {
        if (rawText.isBlank() || rawText == ERROR_SCANNED_PDF) return null

        val lines = rawText.lines().map { it.trim() }
        
        // Look for explicit dedicated JD section headers
        val jdHeaderRegex = Regex("(?i)^\\s*(?:[-=#*]{2,}\\s*)?(?:target\\s+)?(?:job\\s+description|job\\s+posting|position\\s+description|job\\s+specification|open\\s+position|role\\s+description|about\\s+the\\s+role|position\\s+overview|job\\s+requisition)(?:\\s*[:–-])?\\s*(?:[-=#*]{2,})?$")
        
        val hiringIntroRegex = Regex("(?i)\\b(?:we\\s+are\\s+(?:seeking|looking\\s+for|hiring)|job\\s+summary\\s*:|about\\s+the\\s+job\\s*:|role\\s+summary\\s*:)\\b")
        
        var jdStartIndex = -1
        var detectedRoleTitle: String? = null
        
        for (i in lines.indices) {
            val line = lines[i]
            if (jdHeaderRegex.matches(line)) {
                jdStartIndex = i
                // Check next few lines for role title
                for (j in (i + 1)..minOf(i + 4, lines.size - 1)) {
                    val nextLine = lines[j]
                    if (nextLine.isNotBlank() && !isSectionHeader(nextLine)) {
                        val clean = nextLine.replace(Regex("(?i)^(?:job\\s+title|role|position)\\s*[:–-]\\s*"), "").trim()
                        if (clean.length in 3..60) {
                            detectedRoleTitle = clean
                            break
                        }
                    }
                }
                break
            }
        }
        
        // If candidate resume indicators are present and no explicit JD header exists, this is a candidate resume, NOT a JD!
        val isCandidateResume = lines.take(20).any { l ->
            val lw = l.lowercase()
            lw.contains("education") || lw.contains("skills") || lw.contains("projects") || 
            lw.contains("curriculum vitae") || lw.contains("resume") || lw.contains("cgpa") || lw.contains("gpa")
        }
        
        if (jdStartIndex == -1) {
            if (isCandidateResume) {
                // Candidate's resume (student, fresher, engineer) with summary/projects/experience does NOT have a JD
                return null
            }
            // Standalone Job Posting check
            val hasHiringLanguage = hiringIntroRegex.containsMatchIn(rawText)
            val hasRequirementsHeader = rawText.contains(Regex("(?i)\\b(?:job\\s+requirements|required\\s+qualifications|minimum\\s+qualifications|key\\s+responsibilities|what\\s+you'll\\s+do)\\b"))
            
            if (!hasHiringLanguage && !hasRequirementsHeader) {
                return null
            }
        }
        
        val jdLines = if (jdStartIndex != -1) lines.subList(jdStartIndex, lines.size) else lines
        val jdText = jdLines.joinToString("\n")
        
        if (detectedRoleTitle == null) {
            val roleMatch = Regex("(?i)(?:job\\s+title|role|position)\\s*[:–-]\\s*([A-Za-z0-9\\s/]+)").find(jdText)
            if (roleMatch != null) {
                detectedRoleTitle = roleMatch.groupValues[1].trim()
            } else {
                val hiringMatch = Regex("(?i)we\\s+are\\s+(?:seeking|looking\\s+for|hiring)\\s+(?:a|an)?\\s*([A-Za-z0-9\\s/]+?)(?:\\s+to|\\s+who|\\.|,)").find(jdText)
                detectedRoleTitle = hiringMatch?.groupValues?.get(1)?.trim() ?: "Target Role"
            }
        }
        
        val responsibilities = mutableListOf<String>()
        val requiredSkills = mutableListOf<String>()
        val preferredSkills = mutableListOf<String>()
        val qualifications = mutableListOf<String>()
        var summary = ""
        
        var currentSection = ""
        for (line in jdLines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            val lw = trimmed.lowercase()
            
            if (lw.contains("responsibilities") || lw.contains("what you'll do") || lw.contains("duties")) {
                currentSection = "RESP"
                continue
            } else if (lw.contains("preferred") || lw.contains("nice to have") || lw.contains("bonus")) {
                currentSection = "PREF"
                continue
            } else if (lw.contains("required skill") || lw.contains("must have") || lw.contains("core skills") || lw.contains("requirements")) {
                currentSection = "SKILLS"
                continue
            } else if (lw.contains("qualifications") || lw.contains("eligibility") || lw.contains("education required")) {
                currentSection = "QUAL"
                continue
            } else if (lw.contains("summary") || lw.contains("about the role") || lw.contains("overview")) {
                currentSection = "SUMM"
                continue
            }
            
            val cleanBullet = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            if (cleanBullet.length < 3) continue
            
            when (currentSection) {
                "RESP" -> if (cleanBullet.length in 5..250) responsibilities.add(cleanBullet)
                "SKILLS" -> if (cleanBullet.length in 2..150) requiredSkills.add(cleanBullet)
                "PREF" -> if (cleanBullet.length in 2..150) preferredSkills.add(cleanBullet)
                "QUAL" -> if (cleanBullet.length in 5..200) qualifications.add(cleanBullet)
                "SUMM" -> if (summary.length < 300) summary += (if (summary.isNotBlank()) " " else "") + cleanBullet
            }
        }
        
        if (summary.isBlank()) {
            summary = "Extracted Job Description for $detectedRoleTitle."
        }
        
        return com.example.aidrivencompetencyplatform.model.JobDescriptionSection(
            roleTitle = detectedRoleTitle ?: "Target Role",
            roleSummary = summary,
            responsibilities = responsibilities.ifEmpty { listOf("Execute key development tasks aligned with $detectedRoleTitle responsibilities.") },
            requiredSkills = requiredSkills.ifEmpty { listOf("Core technical competencies") },
            preferredSkills = preferredSkills.ifEmpty { listOf("Industry best practices") },
            qualifications = qualifications.ifEmpty { listOf("Bachelor's degree or equivalent practical experience") }
        )
    }
}

data class DeterministicResumeStructure(
    val candidateInfo: ParsedCandidateInfo,
    val sections: List<com.example.aidrivencompetencyplatform.model.ParsedSectionItem>,
    val extractedSkills: List<String>,
    val extractedProjects: List<String>,
    val detectedJobDescription: com.example.aidrivencompetencyplatform.model.JobDescriptionSection? = null
)

data class ParsedCandidateInfo(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null
)
