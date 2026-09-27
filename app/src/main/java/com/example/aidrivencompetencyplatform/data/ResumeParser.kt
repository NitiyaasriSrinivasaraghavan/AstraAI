package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.InputStream
import java.util.zip.ZipInputStream
import com.example.aidrivencompetencyplatform.model.ParsedSectionItem
import com.example.aidrivencompetencyplatform.model.ProjectAnalysis

class ResumeParser(private val context: Context? = null) {

    init {
        context?.let {
            try {
                PDFBoxResourceLoader.init(it)
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        private const val TAG = "ResumeParser"
        const val ERROR_SCANNED_PDF = "[ERROR_SCANNED_PDF]"

        enum class SectionType {
            SUMMARY,
            WORK_EXPERIENCE,
            EDUCATION,
            SKILLS,
            PROJECTS,
            CERTIFICATIONS,
            LANGUAGES,
            ACHIEVEMENTS,
            PUBLICATIONS,
            PERSONAL_INFO,
            OTHER
        }

        val SECTION_HEADER_KEYWORDS = setOf(
            "internship", "internships", "experience", "experiences", "work experience", "professional experience",
            "employment", "employment history", "work history", "career history", "career summary", "work background",
            "education", "academic", "academics", "academic background", "qualifications", "educational qualifications",
            "educational background", "scholastic details", "academic details", "degrees",
            "skill", "skills", "technical skills", "core skills", "key skills", "competencies", "core competencies",
            "technologies", "tech stack", "tools", "tools & technologies", "areas of expertise", "technical proficiencies",
            "project", "projects", "key projects", "academic projects", "personal projects", "portfolio", "selected projects", "selected work",
            "certification", "certifications", "certificates", "courses", "licenses", "licenses & certifications", "credentials",
            "language", "languages", "language proficiency", "languages known",
            "patent", "patents", "publication", "publications", "research", "research papers",
            "achievement", "achievements", "award", "awards", "honors", "accomplishments", "extracurricular",
            "summary", "professional summary", "executive summary", "career objective", "objective", "profile", "about", "about me",
            "career profile", "professional profile", "profile summary", "career overview", "statement of purpose", "career goal",
            "contact", "contact info", "contact information", "personal details", "personal info", "personal information", "personal profile",
            "family", "family details", "father's name", "permanent address",
            "declaration", "references", "reference", "activity", "activities", "co-curricular",
            "volunteer", "volunteering", "leadership", "positions of responsibility", "interests", "hobbies", "training", "workshops",
            "resume", "curriculum vitae", "cv", "page", "github", "linkedin"
        )

        fun normalizeHeadingText(raw: String): String {
            var s = raw.trim()
            // Remove markdown headers (#, ##, etc.)
            s = s.replace(Regex("^#{1,6}\\s*"), "")
            // Remove leading numbering/bullets like "1.", "1.0", "1)", "(1)", "[1]", "I.", "A.", "•", "-", "*", "~"
            s = s.replace(Regex("^(?:\\d+(?:\\.\\d+)*[.)]?|\\([0-9a-zA-Z]+\\)|\\[[0-9a-zA-Z]+\\]|[IVXLCDMivxlcdm]+[.)]|[A-Za-z][.)]|[-*•·~–—=+>]+)\\s*"), "")
            // Remove enclosing markdown styling like **bold**, *italic*, __underline__
            s = s.replace(Regex("^[*_~`]+|[*_~`]+$"), "")
            // Remove trailing colons, semicolons, dashes, pipes, dots, etc.
            s = s.replace(Regex("[:;\\-–—|/\\\\.]+$"), "")
            // Normalize internal whitespace
            s = s.replace(Regex("\\s+"), " ")
            return s.trim().lowercase()
        }

        fun isSectionHeader(text: String): Boolean {
            return classifySectionHeader(text) != null
        }

        fun classifySectionHeader(text: String, nextLines: List<String> = emptyList()): SectionType? {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return null
            
            // Filter out lines that are clearly candidate emails or full URLs
            val lowerTrimmed = trimmed.lowercase()
            if (lowerTrimmed.contains("@") || lowerTrimmed.startsWith("http://") || lowerTrimmed.startsWith("https://") || lowerTrimmed.startsWith("www.")) {
                return null
            }
            if (trimmed.length > 70) return null

            val clean = normalizeHeadingText(trimmed)
            if (clean.isBlank()) return null

            // 1. SUMMARY / PROFILE / OBJECTIVE (Semantic taxonomy & variations)
            val summaryExactHeaders = setOf(
                "summary", "professional summary", "executive summary", "career summary",
                "career objective", "objective", "objectives", "profile", "professional profile", "career profile",
                "profile summary", "executive profile", "personal profile", "personal summary",
                "about me", "about", "about myself", "who i am", "overview", "career overview",
                "professional overview", "background", "professional background", "statement of purpose",
                "career goal", "career goals", "career statement", "career aim", "professional objective",
                "professional objectives", "synopsis", "professional synopsis", "summary of qualifications",
                "qualification summary", "qualifications summary", "candidate profile", "core summary",
                "brief summary", "introductory summary", "my profile", "my journey", "my career journey"
            )
            if (summaryExactHeaders.contains(clean)) {
                return SectionType.SUMMARY
            }

            // Compound / regex patterns for summary/objective
            val summaryRegex = Regex("^(?:my\\s+)?(?:career\\s+|professional\\s+|personal\\s+|executive\\s+)?(?:summary|objective|objectives|profile|synopsis|statement|overview|background)s?$|" +
                                     "^(?:about\\s+me|about\\s+myself|who\\s+i\\s+am|career\\s+goal|career\\s+goals|statement\\s+of\\s+purpose|professional\\s+bio)$|" +
                                     "^(?:summary|profile|objective)\\s*[/&|+-]\\s*(?:summary|profile|objective|overview)$")
            if (summaryRegex.matches(clean)) {
                return SectionType.SUMMARY
            }

            // 2. WORK EXPERIENCE / INTERNSHIPS / EMPLOYMENT
            val experienceExactHeaders = setOf(
                "work experience", "professional experience", "experience", "experiences",
                "employment", "employment history", "work history", "career history",
                "internship", "internships", "internship experience", "industrial experience",
                "industrial training", "relevant experience", "professional background", "industry experience",
                "work background", "job history", "career experience", "practical experience",
                "corporate experience", "hands-on experience", "previous employment", "employment record",
                "relevant work experience", "technical experience", "professional engagements"
            )
            if (experienceExactHeaders.contains(clean)) {
                return SectionType.WORK_EXPERIENCE
            }

            val experienceRegex = Regex("^(?:my\\s+)?(?:work|professional|career|employment|job|industry|internship|relevant|corporate|hands-on|technical|practical)?\\s*(?:experience|history|employment|engagements|background|career)s?$|" +
                                        "^(?:internship|internships|internship\\s+experience|industrial\\s+training|industrial\\s+experience)$")
            if (experienceRegex.matches(clean)) {
                return SectionType.WORK_EXPERIENCE
            }

            // 3. EDUCATION / ACADEMICS / QUALIFICATIONS
            val educationExactHeaders = setOf(
                "education", "academic background", "academics", "academic details", "academic history",
                "qualifications", "educational qualifications", "academic qualifications",
                "educational background", "scholastic details", "scholastic record", "education & qualifications",
                "education and qualifications", "educational details", "academic credentials", "academic record",
                "degrees", "degrees & qualifications", "formal education", "schooling", "university education",
                "college education", "educational credentials"
            )
            if (educationExactHeaders.contains(clean)) {
                return SectionType.EDUCATION
            }

            val educationRegex = Regex("^(?:my\\s+)?(?:academic|educational|scholastic|formal|university|college|school)?\\s*(?:education|qualifications?|academics?|background|details|history|record|credentials?|degrees?|preparation)s?$|" +
                                       "^(?:education\\s*(?:&|and|/)\\s*(?:qualifications?|certifications?|academics?))$")
            if (educationRegex.matches(clean)) {
                return SectionType.EDUCATION
            }

            // 4. PROJECTS
            val projectExactHeaders = setOf(
                "projects", "key projects", "academic projects", "personal projects", "technical projects",
                "demonstrated projects", "portfolio", "notable projects", "major projects", "mini projects",
                "software projects", "system projects", "recent projects", "project work", "selected projects",
                "project experience", "capstone projects", "practical projects", "selected work",
                "featured projects", "engineering projects", "development projects", "client projects"
            )
            if (projectExactHeaders.contains(clean)) {
                return SectionType.PROJECTS
            }

            val projectRegex = Regex("^(?:my\\s+)?(?:key|selected|academic|personal|technical|major|mini|capstone|software|engineering|featured|recent|demonstrated|hands-on|client)?\\s*(?:projects?|portfolio|project\\s+work|project\\s+experience|selected\\s+work)s?$")
            if (projectRegex.matches(clean)) {
                return SectionType.PROJECTS
            }

            // 5. SKILLS
            val skillExactHeaders = setOf(
                "skills", "technical skills", "core skills", "key skills", "technologies", "tech stack",
                "tools", "tools & technologies", "tools and technologies", "competencies", "core competencies",
                "technical competencies", "areas of expertise", "programming skills", "technical proficiencies",
                "skills & abilities", "skills and abilities", "skills & tools", "skills and tools",
                "skills & technologies", "skills and technologies", "it skills", "computer skills",
                "frameworks & tools", "software skills", "technical strengths", "domain expertise",
                "specialties", "proficiencies", "technical toolkit", "skill set", "skillsets"
            )
            if (skillExactHeaders.contains(clean)) {
                return SectionType.SKILLS
            }

            val skillRegex = Regex("^(?:my\\s+)?(?:technical|core|key|programming|it|software|domain|professional|areas\\s+of)?\\s*(?:skills?|competencies|technologies|tech\\s+stack|tools|proficiencies|expertise|skill\\s*sets?|capabilities|strengths)s?$|" +
                                   "^(?:skills?\\s*(?:&|and|/|\\+)\\s*(?:technologies|tools|abilities|frameworks|competencies))$|" +
                                   "^(?:tools?\\s*(?:&|and|/|\\+)\\s*(?:technologies|frameworks|platforms))$")
            if (skillRegex.matches(clean)) {
                return SectionType.SKILLS
            }

            // 6. CERTIFICATIONS / COURSES / TRAININGS
            val certExactHeaders = setOf(
                "certification", "certifications", "certificates", "licenses & certifications",
                "licenses and certifications", "certifications & training", "certifications and training",
                "courses & certifications", "courses and certifications", "courses", "credentials",
                "professional certifications", "trainings", "workshops", "courses completed", "licenses",
                "professional development", "accreditations", "training & certifications",
                "certifications & licenses", "online certifications", "certificate courses",
                "online courses", "nptel certifications", "nptel courses", "moocs", "training & courses",
                "certifications / courses", "courses / certifications"
            )
            if (certExactHeaders.contains(clean)) {
                return SectionType.CERTIFICATIONS
            }

            val certRegex = Regex("^(?:my\\s+)?(?:professional\\s+|online\\s+|technical\\s+)?(?:certifications?|certificates?|credentials?|licenses?|trainings?|courses?|workshops?|accreditations?|coursework)s?$|" +
                                  "^(?:licenses?\\s*(?:&|and|/|\\+)\\s*certifications?|certifications?\\s*(?:&|and|/|\\+)\\s*(?:licenses?|trainings?|courses?)|courses?\\s*(?:&|and|/|\\+)\\s*(?:certifications?|trainings?))$")
            if (certRegex.matches(clean)) {
                return SectionType.CERTIFICATIONS
            }

            // 7. LANGUAGES
            val langExactHeaders = setOf(
                "language", "languages", "language proficiency", "languages known", "foreign languages",
                "linguistic skills", "spoken languages"
            )
            if (langExactHeaders.contains(clean) || Regex("^(?:spoken\\s+|known\\s+|foreign\\s+)?languages?(?:\\s+known|\\s+proficiency)?$").matches(clean)) {
                return SectionType.LANGUAGES
            }

            // 8. ACHIEVEMENTS / AWARDS
            val achieveExactHeaders = setOf(
                "achievement", "achievements", "award", "awards", "honors", "accomplishments",
                "honors & awards", "honors and awards", "achievements & awards", "achievements and awards",
                "key achievements", "extracurricular", "extracurricular activities", "co-curricular",
                "co-curricular activities", "positions of responsibility", "leadership",
                "leadership & activities", "volunteer", "volunteering", "volunteer work", "activities",
                "awards & recognition", "honours", "recognitions"
            )
            if (achieveExactHeaders.contains(clean) || Regex("^(?:my\\s+)?(?:key\\s+|notable\\s+|major\\s+)?(?:achievements?|awards?|honors?|accomplishments?|recognitions?|honours?)s?$").matches(clean)) {
                return SectionType.ACHIEVEMENTS
            }

            // 9. PUBLICATIONS
            val pubExactHeaders = setOf(
                "patent", "patents", "publication", "publications", "research", "research papers",
                "conference papers", "journals", "published works", "scientific papers", "white papers"
            )
            if (pubExactHeaders.contains(clean) || Regex("^(?:my\\s+)?(?:research\\s+|scientific\\s+|conference\\s+)?(?:publications?|patents?|research\\s+papers?|journals?|published\\s+works?|white\\s+papers?)s?$").matches(clean)) {
                return SectionType.PUBLICATIONS
            }

            // 10. PERSONAL INFO
            val personalInfoExactHeaders = setOf(
                "personal details", "personal info", "personal information", "contact", "contact info",
                "contact information", "family details", "family background", "father's name", "permanent address",
                "residential address"
            )
            if (personalInfoExactHeaders.contains(clean) || Regex("^(?:personal\\s+details|personal\\s+info|personal\\s+information|contact\\s+details|contact\\s+info|contact\\s+information|family\\s+details|family\\s+background|permanent\\s+address|residential\\s+address)$").matches(clean)) {
                return SectionType.PERSONAL_INFO
            }

            // 11. OTHER
            val otherExactHeaders = setOf(
                "declaration", "references", "reference", "interests", "hobbies"
            )
            if (otherExactHeaders.contains(clean)) {
                return SectionType.OTHER
            }

            // 12. CONTENT-BASED FALLBACK: For unconventional headings (e.g. "MY CAREER JOURNEY", "SELECTED WORK")
            // Must strictly look like a section heading: short line, no role/job delimiters, no email/phone/periods,
            // and either ALL CAPS or containing heading-like keywords.
            val hasDelimiter = trimmed.contains("|") || trimmed.contains(" – ") || trimmed.contains(" - ") || trimmed.contains(":") || trimmed.contains(",") || trimmed.contains("@")
            val isJobTitle = Regex("(?i)\\b(?:developer|engineer|intern|manager|lead|architect|analyst|consultant|programmer)\\b").containsMatchIn(trimmed)
            val isHeadingStyle = trimmed.all { it.isUpperCase() || !it.isLetter() } || 
                                 listOf("journey", "selected work", "highlights", "milestones", "snapshot", "portfolio", "what i do", "what i bring").any { clean.contains(it) }

            if (nextLines.isNotEmpty() && clean.length in 3..35 && !hasDelimiter && !isJobTitle && isHeadingStyle) {
                val fallbackType = classifySectionByContent(clean, nextLines)
                if (fallbackType != null) {
                    return fallbackType
                }
            }

            return null
        }

        /**
         * Semantic content-based classification for unconventional or ambiguous section headings.
         */
        private fun classifySectionByContent(heading: String, bodyLines: List<String>): SectionType? {
            val sample = bodyLines.take(6).filter { it.isNotBlank() }
            if (sample.isEmpty()) return null
            val fullSample = sample.joinToString(" ")
            val fullSampleLower = fullSample.lowercase()

            // 1. Check for Summary / Objective content characteristics:
            // Single narrative paragraph describing career identity, goals, strengths, or experience
            val summaryIndicators = listOf(
                "software engineer", "developer", "experience in", "passionate", "seeking", "driven",
                "skilled in", "proven track record", "proficient in", "specialized in", "dedicated",
                "career goal", "objective", "results-driven", "aiming to", "looking for an opportunity"
            )
            val summaryCount = summaryIndicators.count { fullSampleLower.contains(it) }
            val hasDelimiters = sample.any { it.contains(" | ") || it.contains(" – ") }
            val hasDates = Regex("\\b(?:19|20)\\d{2}\\b").containsMatchIn(fullSample)
            val isShortParagraph = sample.size in 1..4 && fullSample.length in 40..500

            if (summaryCount >= 2 || (summaryCount >= 1 && isShortParagraph && !hasDelimiters && !hasDates)) {
                return SectionType.SUMMARY
            }

            // 2. Check for Project content characteristics (titles with tech stack, bullets describing built systems)
            val projectIndicators = listOf("built", "developed", "architected", "implemented", "designed", "created", "technologies:", "tech stack:")
            val hasProjectBullet = projectIndicators.any { fullSampleLower.contains(it) }
            val hasProjectTech = listOf("kotlin", "java", "python", "react", "compose", "sql", "firebase", "node", "aws", "flutter").count { fullSampleLower.contains(it) }
            if (hasProjectBullet && (hasProjectTech >= 1 || hasDelimiters)) {
                return SectionType.PROJECTS
            }

            // 3. Check for Experience content characteristics (job roles + company/dates)
            val roleIndicators = listOf("engineer", "developer", "intern", "manager", "lead", "architect", "analyst", "consultant")
            val hasRole = roleIndicators.any { fullSampleLower.contains(it) }
            val hasDateRange = Regex("\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)?\\s*\\d{4}\\s*(?:–|-|to)\\s*(?:present|current|\\d{4})\\b", RegexOption.IGNORE_CASE).containsMatchIn(fullSample)
            if (hasRole && (hasDateRange || (hasDates && hasDelimiters))) {
                return SectionType.WORK_EXPERIENCE
            }

            // 4. Check for Education content characteristics
            val eduIndicators = listOf("bachelor", "master", "b.tech", "b.e.", "b.sc", "m.tech", "mca", "10th", "12th", "sslc", "hsc", "cgpa", "gpa", "university", "college", "institute")
            if (eduIndicators.count { fullSampleLower.contains(it) } >= 2) {
                return SectionType.EDUCATION
            }

            // 5. Check for Skills (dense list of technical terms)
            val skillMatchCount = listOf("kotlin", "java", "python", "c++", "sql", "git", "docker", "aws", "react", "html", "css", "linux").count { fullSampleLower.contains(it) }
            if (skillMatchCount >= 3 && sample.size in 1..4) {
                return SectionType.SKILLS
            }

            return null
        }
        fun isDegreeOrEducationTitle(text: String): Boolean {
            val clean = text.trim().lowercase()
            if (clean.isBlank()) return true
            
            // Regex patterns for degree and qualification text
            val degreeRegex = Regex("(?i)\\b(?:bachelor|bachelors|bachelor's|master|masters|master's|doctor|doctorate|diploma|associate|degree|b\\.?e\\.?|b\\.?tech\\.?|btech|m\\.?e\\.?|m\\.?tech\\.?|mtech|b\\.?sc\\.?|bsc|m\\.?sc\\.?|msc|bca|mca|bba|mba|b\\.?com\\.?|bcom|m\\.?com\\.?|mcom|b\\.?a\\.?|m\\.?a\\.?|ph\\.?d\\.?|d\\.?phil\\.?|postgraduate|post\\s+graduate|undergraduate|under\\s+graduate|10th|12th|sslc|hsc|cbse|icse|matriculation|intermediate|higher\\s+secondary|secondary\\s+school|pre-university|puc)\\b")
            if (degreeRegex.containsMatchIn(clean)) return true

            // Regex patterns for educational institutions
            val institutionRegex = Regex("(?i)\\b(?:university|college|institute|institution|school|academy|polytechnic|campus|department|faculty|vidyalaya|vidyamandir)\\b")
            if (institutionRegex.containsMatchIn(clean)) return true

            // Regex patterns for job titles
            val jobTitleRegex = Regex("(?i)\\b(?:developer|engineer|designer|architect|analyst|consultant|intern|manager|lead|specialist|programmer|administrator|officer|executive|associate|trainee|fullstack|frontend|backend|software|hardware|student|fresher)\\b")
            if (jobTitleRegex.containsMatchIn(clean)) return true

            // Section headings
            val sectionHeaderRegex = Regex("(?i)^(?:curriculum\\s+vitae|resume|cv|bio\\s*data|biodata|profile|summary|objective|career\\s+objective|professional\\s+summary|about\\s+me|education|academics|academic\\s+background|work\\s+experience|experience|projects|skills|technical\\s+skills|certifications|courses|languages|achievements|declaration|references|contact|personal\\s+details|personal\\s+info|hobbies)$")
            if (sectionHeaderRegex.matches(clean)) return true

            val eduKeywords = setOf(
                "engineering", "technology", "science", "arts", "commerce"
            )
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.any { eduKeywords.contains(it.trim('.', ',', '-', '\'')) }) return true

            return false
        }
    }

    fun saveUriToLocalCache(uri: Uri): Uri? {
        val resolver = context?.contentResolver ?: return null
        val fileName = getFileName(uri) ?: "uploaded_resume.pdf"
        val localFile = java.io.File(context.cacheDir, "resumes").apply { if (!exists()) mkdirs() }
        val targetFile = java.io.File(localFile, "source_${System.currentTimeMillis()}_$fileName")
        
        return try {
            resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy URI to local cache: ${e.message}", e)
            null
        }
    }

    fun extractText(uri: Uri): String {
        val resolver = context?.contentResolver
        val mimeType = resolver?.getType(uri)
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
                    try {
                        extractFromPdf(uri)
                    } catch (ePdf: Exception) {
                        try {
                            extractFromDocx(uri)
                        } catch (eDocx: Exception) {
                            try {
                                resolver?.openInputStream(uri)?.use { stream ->
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
                try {
                    resolver?.openInputStream(uri)?.use { stream ->
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

    fun extractLayout(uri: Uri): com.example.aidrivencompetencyplatform.model.ResumeLayout {
        val resolver = context?.contentResolver
        val mimeType = resolver?.getType(uri)
        val extension = getExtensionFromUri(uri, mimeType)

        if (mimeType == "application/pdf" || extension.equals("pdf", ignoreCase = true)) {
            try {
                val bytes = resolver?.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    PDDocument.load(bytes).use { document ->
                        val stripper = PositionStripper()
                        stripper.sortByPosition = true
                        stripper.getText(document)
                        return com.example.aidrivencompetencyplatform.model.ResumeLayout(
                            textPositions = stripper.capturedPositions,
                            pdfUri = uri.toString()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ResumeParser", "Layout extraction failed", e)
            }
        }
        return com.example.aidrivencompetencyplatform.model.ResumeLayout(pdfUri = uri.toString())
    }

    fun findTextRects(query: String, layout: com.example.aidrivencompetencyplatform.model.ResumeLayout, pageIndex: Int): List<com.example.aidrivencompetencyplatform.model.TextPosition> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return emptyList()
        
        val pagePositions = layout.textPositions.filter { it.pageIndex == pageIndex }
        if (pagePositions.isEmpty()) return emptyList()

        val fullText = pagePositions.joinToString("") { it.text }
        val startIndex = fullText.lowercase().indexOf(q)
        
        if (startIndex == -1) return emptyList()
        
        val matchedPositions = mutableListOf<com.example.aidrivencompetencyplatform.model.TextPosition>()
        var charCount = 0
        for (pos in pagePositions) {
            val nextCount = charCount + pos.text.length
            if (nextCount > startIndex && charCount < startIndex + q.length) {
                matchedPositions.add(pos)
            }
            charCount = nextCount
            if (charCount >= startIndex + q.length) break
        }
        return matchedPositions
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
            val bytes = context?.contentResolver?.openInputStream(uri)?.use { it.readBytes() }
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
        context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
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
        
        val pRegex = Regex("<w:p[ >](.*?)</w:p>")
        val tRegex = Regex("<w:t[^>]*>(.*?)</w:t>")
        val pMatches = pRegex.findAll(content).toList()
        
        if (pMatches.isNotEmpty()) {
            for (pMatch in pMatches) {
                val pContent = pMatch.groupValues[1]
                val lineSb = StringBuilder()
                tRegex.findAll(pContent).forEach { tMatch ->
                    lineSb.append(tMatch.groupValues[1])
                }
                val lineStr = lineSb.toString().trim()
                if (lineStr.isNotEmpty()) {
                    sb.append(lineStr).append("\n")
                }
            }
        } else {
            tRegex.findAll(content).forEach { match ->
                sb.append(match.groupValues[1]).append(" ")
            }
        }
        
        return sb.toString().trim()
    }

    private fun cleanText(text: String): String {
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
            val cursor = context?.contentResolver?.query(uri, null, null, null, null)
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

        // 2. Phone extraction
        val phoneMatch = extractPhoneNumber(rawText, lines)

        // 3. Location extraction
        val locationMatch = extractCandidateLocation(rawText, lines)

        // 4. Name extraction
        val nameMatch = extractCandidateName(lines, emailMatch, locationMatch)

        return ParsedCandidateInfo(
            name = nameMatch,
            email = emailMatch,
            phone = phoneMatch,
            location = locationMatch
        )
    }

    private fun extractPhoneNumber(rawText: String, lines: List<String>): String? {
        val phoneLabelRegex = Regex("(?i)\\b(?:phone|mobile|mob|tel|telephone|cell|contact|ph|whatsapp)\\s*(?:no|number|#)?\\s*[:–-]?\\s*(\\+?[\\d\\s().-]{7,25}\\d)")
        for (i in 0 until minOf(30, lines.size)) {
            val line = lines[i]
            val m = phoneLabelRegex.find(line)
            if (m != null) {
                val candidate = m.groupValues[1].trim()
                val digits = candidate.filter { it.isDigit() }
                if (digits.length in 7..15) {
                    if (!(digits.length == 8 && (digits.startsWith("19") || digits.startsWith("20")))) {
                        return cleanPhone(candidate)
                    }
                }
            }
        }

        val patterns = listOf(
            Regex("\\+\\d{1,4}(?:[-.\\s]?(?:\\(\\d+\\)|\\d+)){2,5}"),
            Regex("(?:\\+?1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"),
            Regex("(?:\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}\\b"),
            Regex("\\(?0\\d{2,4}\\)?[-.\\s]?\\d{6,8}\\b"),
            Regex("\\b[6-9]\\d{9}\\b")
        )

        for (i in 0 until minOf(20, lines.size)) {
            val line = lines[i]
            val parts = line.split(Regex("[|•·\\t/]")).map { it.trim() }
            for (part in parts) {
                for (pat in patterns) {
                    val m = pat.find(part)
                    if (m != null) {
                        val digits = m.value.filter { it.isDigit() }
                        if (digits.length in 10..15) {
                            if (!(digits.length == 8 && (digits.startsWith("19") || digits.startsWith("20")))) {
                                return cleanPhone(m.value.trim())
                            }
                        }
                    }
                }
            }
        }

        for (i in 0 until minOf(25, lines.size)) {
            val line = lines[i]
            for (pat in patterns) {
                val m = pat.find(line)
                if (m != null) {
                    val digits = m.value.filter { it.isDigit() }
                    if (digits.length in 10..15) {
                        if (!(digits.length == 8 && (digits.startsWith("19") || digits.startsWith("20")))) {
                            return cleanPhone(m.value.trim())
                        }
                    }
                }
            }
        }

        for (pat in patterns) {
            val m = pat.find(rawText)
            if (m != null) {
                val digits = m.value.filter { it.isDigit() }
                if (digits.length in 10..15) {
                    if (!(digits.length == 8 && (digits.startsWith("19") || digits.startsWith("20")))) {
                        return cleanPhone(m.value.trim())
                    }
                }
            }
        }

        return null
    }

    private fun cleanPhone(phone: String): String {
        return phone.replace(Regex("^[^+\\d(]+"), "").replace(Regex("[^\\d)]+$"), "").trim()
    }

    private fun extractCandidateLocation(rawText: String, lines: List<String>): String? {
        val usStates = setOf(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY",
            "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND",
            "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY", "DC"
        )

        val stateAndCountryNames = setOf(
            "alabama", "alaska", "arizona", "arkansas", "california", "colorado", "connecticut", "delaware", "florida",
            "georgia", "hawaii", "idaho", "illinois", "indiana", "iowa", "kansas", "kentucky", "louisiana", "maine",
            "maryland", "massachusetts", "michigan", "minnesota", "mississippi", "missouri", "montana", "nebraska",
            "nevada", "new hampshire", "new jersey", "new mexico", "new york", "north carolina", "north dakota", "ohio",
            "oklahoma", "oregon", "pennsylvania", "rhode island", "south carolina", "south dakota", "tennessee", "texas",
            "utah", "vermont", "virginia", "washington", "west virginia", "wisconsin", "wyoming",
            "tamil nadu", "karnataka", "kerala", "maharashtra", "telangana", "andhra pradesh", "uttar pradesh", "gujarat",
            "rajasthan", "west bengal", "punjab", "haryana", "madhya pradesh", "bihar", "odisha", "assam", "delhi",
            "india", "usa", "united states", "uk", "united kingdom", "canada", "australia", "germany", "france",
            "singapore", "japan", "netherlands", "ireland", "sweden", "switzerland", "uae", "united arab emirates",
            "ontario", "british columbia", "quebec", "alberta"
        )

        val knownCities = setOf(
            "san francisco", "san jose", "los angeles", "san diego", "seattle", "austin", "new york", "nyc",
            "boston", "chicago", "denver", "atlanta", "dallas", "houston", "miami", "portland", "phoenix",
            "philadelphia", "washington", "toronto", "vancouver", "montreal", "london", "manchester", "berlin",
            "munich", "paris", "amsterdam", "dublin", "sydney", "melbourne", "singapore", "tokyo",
            "chennai", "bangalore", "bengaluru", "hyderabad", "pune", "mumbai", "delhi", "noida", "gurgaon",
            "gurugram", "kolkata", "kochi", "trivandrum", "trichy", "tiruchirappalli", "srirangam", "coimbatore",
            "madurai", "salem", "vellore", "erode", "tirunelveli", "thanjavur", "ahmedabad", "jaipur", "surat",
            "indore", "nagpur", "bhopal", "patna", "vadodara", "ghaziabad", "ludhiana", "agra", "nashik",
            "faridabad", "meerut", "rajkot", "varanasi", "srinagar", "aurangabad", "amritsar", "navi mumbai",
            "mysore", "mysuru", "mangalore", "mangaluru", "visakhapatnam", "vijayawada", "chandigarh", "calicut", "kozhikode"
        )

        val labelRegex = Regex("(?i)\\b(?:location|current\\s+location|address|residence|place|domicile|based\\s+in|living\\s+in|city)\\s*[:–-]?\\s*([^\\n|•·]+)")
        for (i in 0 until minOf(35, lines.size)) {
            val line = lines[i]
            val m = labelRegex.find(line)
            if (m != null) {
                var candidate = m.groupValues[1].trim()
                candidate = candidate.split(Regex("[|•·]"))[0].trim()
                candidate = cleanLocationString(candidate)
                if (candidate.length in 2..70 && !candidate.contains("@") && !candidate.contains("http")) {
                    return candidate
                }
            }
        }

        for (i in 0 until minOf(20, lines.size)) {
            val line = lines[i]
            val lw = line.lowercase()
            if (lw.contains("university") || lw.contains("college") || lw.contains("school") || 
                lw.contains("institute") || lw.contains("experience") || lw.contains("education") || 
                lw.contains("project") || lw.contains("summary") || lw.contains("skills")) {
                continue
            }

            // Strip emails, phone numbers, and URLs from line before checking location chunks
            val strippedLine = line
                .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "")
                .replace(Regex("https?://\\S+|www\\.\\S+|linkedin\\.com/\\S+|github\\.com/\\S+"), "")
                .replace(Regex("\\+?\\d{1,4}[\\s-]?(?:\\(?\\d{3}\\)?[\\s-]?)?\\d{3}[\\s-]?\\d{4}|\\b[6-9]\\d{9}\\b"), "")
                .trim()

            if (strippedLine.isBlank()) continue

            val chunks = strippedLine.split(Regex("[|•·\\t]")).map { it.trim() }.filter { it.isNotBlank() }
            for (chunk in chunks) {
                val clw = chunk.lowercase()
                if (clw.contains("linkedin") || clw.contains("github")) continue
                if (chunk.length !in 2..70) continue

                val usStateMatch = Regex("\\b([A-Za-z\\s]+),\\s*([A-Z]{2})\\b(?:\\s+\\d{5}(?:-\\d{4})?)?").find(chunk)
                if (usStateMatch != null) {
                    val stateCode = usStateMatch.groupValues[2]
                    if (usStates.contains(stateCode)) {
                        return cleanLocationString(chunk)
                    }
                }

                val pincodeMatch = Regex("\\b([A-Za-z\\s]+)\\s*[-–]\\s*\\d{6}\\b").find(chunk)
                if (pincodeMatch != null) {
                    return cleanLocationString(chunk)
                }

                val hasCity = knownCities.any { clw.contains(it) }
                val hasStateOrCountry = stateAndCountryNames.any { clw.contains(it) }
                if (hasCity || hasStateOrCountry) {
                    // Check if chunk is separated by commas (e.g. "Coimbatore, Tamil Nadu")
                    val commaSubparts = chunk.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (commaSubparts.size in 1..4 && commaSubparts.all { it.length in 2..35 && it.all { c -> c.isLetter() || c.isWhitespace() || c == '.' || c == '-' } }) {
                        return cleanLocationString(chunk)
                    } else if (hasCity && chunk.length in 2..40) {
                        return cleanLocationString(chunk)
                    }
                }
            }
        }

        return null
    }

    private fun cleanLocationString(loc: String): String {
        return loc
            .replace(Regex("(?i)^(?:location|current\\s+location|address|based\\s+in|living\\s+in|city)\\s*[:–-]?\\s*"), "")
            .replace(Regex("^[•·|,-]+\\s*"), "")
            .replace(Regex("\\s*[•·|,-]+$"), "")
            .trim()
    }

    private fun extractCandidateName(lines: List<String>, emailMatch: String?, locationMatch: String?): String? {
        val blacklistJobTitles = setOf(
            "developer", "engineer", "designer", "architect", "analyst", "consultant",
            "intern", "manager", "lead", "specialist", "student", "fresher", "programmer",
            "software", "hardware", "frontend", "backend", "fullstack", "full stack",
            "b.tech", "b.e", "m.tech", "m.e", "bca", "mca", "bsc", "msc", "phd", "ph.d", "resume", "curriculum", "vitae", "cv",
            "university", "college", "institute", "school", "academy", "polytechnic", "campus", "department", "institution", "faculty",
            "bachelor", "master", "doctor", "degree", "diploma", "engineering", "technology", "science", "arts", "commerce",
            "objective", "summary", "profile", "contact", "information", "phone", "email", "address", "location", "skills", "experience", "education", "projects", "certifications", "languages", "hobbies", "interests", "declaration", "references", "achievement", "award", "honor", "scholarship",
            "internship", "volunteer", "leadership", "position", "responsibility", "club", "society", "organization", "training", "workshop"
        )

        val addressAndLandmarkKeywords = setOf(
            "office", "backside", "street", "nagar", "road", "colony", "door", "flat", "floor", "lane", "avenue",
            "cross", "layout", "opp", "opposite", "near", "behind", "dist", "district", "taluk", "post", "p.o",
            "building", "apartment", "complex", "tower", "house", "plot", "sector", "block", "pin", "pincode",
            "zip", "zipcode", "address", "residence", "domicile", "city", "state", "country", "tamil", "nadu", "india",
            "chennai", "trichy", "tiruchirappalli", "salem", "coimbatore", "madurai", "bengaluru", "hyderabad", "bangalore",
            "pune", "mumbai", "delhi", "noida", "gurgaon", "kolkata", "karnataka", "kerala", "telangana",
            "andhra", "maharashtra", "sivagangai", "sivaganga", "karaikudi", "dindigul", "thanjavur", "tirunelveli",
            "erode", "vellore", "thoothukudi", "tuticorin", "kanchipuram", "cuddalore", "nagapattinam", "karur",
            "theni", "namakkal", "dharmapuri", "krishnagiri", "villupuram", "pudukkottai", "ramanathapuram",
            "virudhunagar", "perambalur", "ariyalur", "nilgiris", "ooty", "kanyakumari", "nagercoil", "tirupur",
            "tiruvallur", "tiruvannamalai", "tiruvarur", "ranipet", "tenkasi", "chengalpattu",
            "contact", "details", "info", "information", "phone", "mobile", "email",
            "github", "linkedin", "profile", "summary", "objective", "education", "experience", "projects",
            "skills", "certifications", "languages", "declaration", "hobbies", "interests"
        )

        fun isValidNameToken(name: String, minWords: Int = 1): Boolean {
            val clean = name.trim().replace(Regex("[,|•·\\-]+$"), "").trim()
            if (clean.length !in 2..60) return false
            if (clean.contains("@") || clean.contains("http") || clean.contains("www.") || clean.contains(".com")) return false
            if (clean.any { it.isDigit() }) return false
            if (isContactOrAddressLine(clean)) return false
            if (isDegreeOrEducationTitle(clean)) return false

            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size < minWords || words.size > 5) return false
            if (!words.all { w -> w.all { it.isLetter() || it == '.' || it == '-' || it == '\'' } }) return false

            val lowerWords = words.map { it.lowercase().trim('.', ',', '-', '\'') }
            if (lowerWords.any { blacklistJobTitles.contains(it) || addressAndLandmarkKeywords.contains(it) || SECTION_HEADER_KEYWORDS.contains(it) }) {
                return false
            }
            if (locationMatch != null) {
                val locLower = locationMatch.lowercase().trim()
                val cleanLower = clean.lowercase()
                if (cleanLower == locLower || cleanLower.contains(locLower)) {
                    return false
                }
            }

            return words.any { it.first().isUpperCase() }
        }

        val headerLimit = minOf(25, lines.size)
        val nameLabelRegex = Regex("(?i)^(?:candidate\\s+name|full\\s+name|applicant\\s+name|name\\s+of\\s+(?:the\\s+)?candidate|name)\\s*[:–-]?\\s*([A-Za-z\\s.\\-']+)")
        for (i in 0 until headerLimit) {
            val line = lines[i]
            val m = nameLabelRegex.find(line)
            if (m != null) {
                val candidate = m.groupValues[1].trim().trim('"', '\'', '\t', ' ')
                if (isValidNameToken(candidate)) {
                    return normalizeCandidateName(candidate)
                }
            }
        }

        // Pass 1: Prioritize multi-word candidate name (2..5 words, e.g. "THARSHIKA A", "R Karthik", "S. Priya", "Mary Jane Thomas")
        for (i in 0 until headerLimit) {
            val line = lines[i]
            val lw = line.lowercase().trim()

            if (isSectionHeader(line)) break
            if (lw in setOf("resume", "curriculum vitae", "cv", "personal details", "profile", "contact", "contact information")) continue
            if (line.length !in 2..150) continue

            val lineParts = line.split(Regex("[|•·\\t,]|\\s{2,}|\\s+[-–]\\s+|\\s+/\\s+")).map { it.trim() }.filter { it.isNotBlank() }
            val potentialPart = if (lineParts.size > 1) {
                lineParts.firstOrNull { part -> isValidNameToken(part, minWords = 2) }
            } else {
                if (isValidNameToken(line, minWords = 2)) line else null
            }

            if (potentialPart != null) {
                val cleanedPart = potentialPart.replace(Regex("[,|•·\\-]+$"), "").trim()
                if (isValidNameToken(cleanedPart, minWords = 2)) {
                    return normalizeCandidateName(cleanedPart)
                }
            }
        }

        // Pass 2: Fallback to single-word valid name only if no multi-word candidate name exists
        for (i in 0 until minOf(12, lines.size)) {
            val line = lines[i]
            val lw = line.lowercase().trim()

            if (isSectionHeader(line)) break
            if (lw in setOf("resume", "curriculum vitae", "cv", "personal details", "profile", "contact", "contact information")) continue
            if (line.length !in 2..150) continue

            val lineParts = line.split(Regex("[|•·\\t,]|\\s{2,}|\\s+[-–]\\s+|\\s+/\\s+")).map { it.trim() }.filter { it.isNotBlank() }
            val potentialPart = if (lineParts.size > 1) {
                lineParts.firstOrNull { part -> isValidNameToken(part, minWords = 1) }
            } else {
                if (isValidNameToken(line, minWords = 1)) line else null
            }

            if (potentialPart != null) {
                val cleanedPart = potentialPart.replace(Regex("[,|•·\\-]+$"), "").trim()
                if (isValidNameToken(cleanedPart, minWords = 1)) {
                    return normalizeCandidateName(cleanedPart)
                }
            }
        }

        return null
    }

    private fun normalizeCandidateName(rawName: String): String {
        val trimmed = rawName.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) return trimmed
        val isAllCaps = trimmed.all { !it.isLetter() || it.isUpperCase() }
        if (isAllCaps) {
            return trimmed.split(" ").joinToString(" ") { word ->
                val cleanWord = word.trim('.', ',', '-', '\'')
                if (cleanWord.length <= 2 && cleanWord.all { it.isLetter() }) {
                    word.uppercase()
                } else {
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
        }
        return trimmed
    }

    /**
     * High-precision semantic resume structure segmentation and extraction.
     * Enforces strict boundaries to eliminate cross-contamination between sections.
     */
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
        val allLines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Partition the resume into semantic section blocks
        val sectionSpans = mutableListOf<SectionSpan>()
        for (i in allLines.indices) {
            val line = allLines[i]
            val nextLines = if (i + 1 < allLines.size) allLines.subList(i + 1, minOf(allLines.size, i + 10)) else emptyList()
            val sectionType = classifySectionHeader(line, nextLines)
            if (sectionType != null) {
                sectionSpans.add(SectionSpan(type = sectionType, headerIndex = i, headerText = line))
            }
        }

        // Map section contents
        val sectionContents = mutableMapOf<SectionType, MutableList<String>>()
        for (st in SectionType.values()) {
            sectionContents[st] = mutableListOf()
        }

        for (idx in sectionSpans.indices) {
            val span = sectionSpans[idx]
            val startLine = span.headerIndex + 1
            val endLine = if (idx + 1 < sectionSpans.size) sectionSpans[idx + 1].headerIndex else allLines.size
            if (startLine < endLine) {
                val lines = allLines.subList(startLine, endLine)
                sectionContents[span.type]?.addAll(lines)
            }
        }

        // 1. EXTRACT SUMMARY (Semantic extraction across all equivalent headings)
        var extractedSummary: String? = null
        val summaryLines = sectionContents[SectionType.SUMMARY] ?: emptyList()
        if (summaryLines.isNotEmpty()) {
            val cleaned = summaryLines.filter { 
                !isSectionHeader(it) && !it.contains("@") && !isContactOrAddressLine(it) && it.length > 10 
            }.joinToString(" ")
            if (cleaned.isNotBlank()) {
                extractedSummary = cleaned
            }
        }

        // Semantic fallback for Summary: check introductory paragraph before the first section
        if (extractedSummary.isNullOrBlank() && sectionSpans.isNotEmpty()) {
            val firstSectionIndex = sectionSpans.first().headerIndex
            if (firstSectionIndex > 1) {
                val headerLines = allLines.subList(0, firstSectionIndex)
                // Filter out contact information lines
                val introLines = headerLines.filter { line ->
                    !line.contains("@") && !line.startsWith("http") && !line.contains(".com") &&
                    !line.contains("linkedin") && !line.contains("github") &&
                    !Regex("\\b\\d{10}\\b").containsMatchIn(line) &&
                    (candidateInfo.name == null || !line.equals(candidateInfo.name, ignoreCase = true)) &&
                    (candidateInfo.location == null || !line.equals(candidateInfo.location, ignoreCase = true)) &&
                    line.length > 25
                }
                if (introLines.isNotEmpty()) {
                    val candidateIntro = introLines.joinToString(" ")
                    if (candidateIntro.length in 35..600) {
                        extractedSummary = candidateIntro
                    }
                }
            }
        }

        // 2. EXTRACT EDUCATION (All tiers: 10th, 12th, Diploma, UG, PG, etc.)
        val educationLines = sectionContents[SectionType.EDUCATION] ?: emptyList()
        val extractedEducation = parseEducationEntries(educationLines)

        // 3. EXTRACT WORK EXPERIENCE (ONLY actual employment / internships)
        val experienceLines = sectionContents[SectionType.WORK_EXPERIENCE] ?: emptyList()
        val extractedExperience = parseWorkExperienceEntries(experienceLines)

        // 4. EXTRACT PROJECTS (ONLY actual projects, NEVER lone skills/subjects/certifications)
        val projectLines = sectionContents[SectionType.PROJECTS] ?: emptyList()
        val (extractedProjectNames, extractedProjectAnalyses) = parseProjectEntries(projectLines)

        // 5. EXTRACT CERTIFICATIONS
        val certificationLines = sectionContents[SectionType.CERTIFICATIONS] ?: emptyList()
        val extractedCertifications = parseCertificationEntries(certificationLines)

        // 6. EXTRACT SKILLS
        val skillLines = sectionContents[SectionType.SKILLS] ?: emptyList()
        val extractedSkillsList = parseSkillsList(skillLines, rawText)

        // Contact details list
        val contactDetails = mutableListOf<String>()
        candidateInfo.name?.let { contactDetails.add(it) }
        candidateInfo.email?.let { contactDetails.add(it) }
        candidateInfo.phone?.let { contactDetails.add(it) }
        candidateInfo.location?.let { contactDetails.add(it) }

        val hasContact = contactDetails.isNotEmpty()
        val hasEducation = extractedEducation.isNotEmpty()
        val hasExperience = extractedExperience.isNotEmpty()
        val hasSkills = extractedSkillsList.isNotEmpty()
        val hasProjects = extractedProjectNames.isNotEmpty()
        val hasCertifications = extractedCertifications.isNotEmpty()
        val hasLanguages = sectionContents[SectionType.LANGUAGES]?.isNotEmpty() == true
        val hasPatents = sectionContents[SectionType.PUBLICATIONS]?.isNotEmpty() == true || sectionContents[SectionType.ACHIEVEMENTS]?.isNotEmpty() == true
        val hasPersonalInfo = sectionContents[SectionType.PERSONAL_INFO]?.isNotEmpty() == true

        val personalInfoLines = (sectionContents[SectionType.PERSONAL_INFO] ?: emptyList()).filter { !isSectionHeader(it) }

        val sectionsList = listOf(
            ParsedSectionItem("Contact Information", hasContact || hasPersonalInfo, contactDetails + personalInfoLines),
            ParsedSectionItem("Summary", !extractedSummary.isNullOrBlank(), emptyList(), extractedSummary),
            ParsedSectionItem("Education", hasEducation, extractedEducation),
            ParsedSectionItem("Work Experience", hasExperience, extractedExperience),
            ParsedSectionItem("Skills", hasSkills, extractedSkillsList),
            ParsedSectionItem("Projects", hasProjects, extractedProjectNames),
            ParsedSectionItem("Certifications", hasCertifications, extractedCertifications),
            ParsedSectionItem("Languages", hasLanguages),
            ParsedSectionItem("Achievements & Publications", hasPatents)
        )

        val detectedJd = detectJobDescription(rawText)

        return DeterministicResumeStructure(
            candidateInfo = candidateInfo,
            sections = sectionsList,
            extractedSkills = extractedSkillsList,
            extractedProjects = extractedProjectNames,
            detectedJobDescription = detectedJd,
            summary = extractedSummary,
            education = extractedEducation,
            experience = extractedExperience,
            personalInfo = personalInfoLines,
            certifications = extractedCertifications,
            projectAnalyses = extractedProjectAnalyses
        )
    }

    /**
     * Parses education qualifications preserving ALL tiers:
     * 10th / Secondary, 12th / Higher Secondary, Diploma, UG, PG, Doctorate.
     * Enforces strict academic validation; non-academic courses/certifications are excluded.
     */
    private fun parseEducationEntries(educationLines: List<String>): List<String> {
        if (educationLines.isEmpty()) return emptyList()

        val qualifications = mutableListOf<String>()
        val currentBlock = mutableListOf<String>()

        fun flushBlock() {
            if (currentBlock.isNotEmpty()) {
                val combined = currentBlock.joinToString(" | ")
                if (combined.length in 5..1000) {
                    qualifications.add(combined)
                }
                currentBlock.clear()
            }
        }

        val nonAcademicIndicators = listOf(
            "nptel", "coursera", "udemy", "edx", "linkedin learning", "udacity",
            "certification in", "certificate course", "certified in", "course in",
            "completed course", "workshop on", "workshop in", "bootcamp",
            "training program", "short-term course", "training in"
        )

        val tierIndicators = listOf(
            // 10th / Secondary
            Regex("(?i)\\b(?:10th|class\\s*x|class\\s*10|sslc|secondary\\s+school|matriculation|high\\s+school\\s+leaving)\\b"),
            // 12th / Higher Secondary / Intermediate
            Regex("(?i)\\b(?:12th|class\\s*xii|class\\s*12|hsc|intermediate|higher\\s+secondary|senior\\s+secondary|puc|pre-university)\\b"),
            // Diploma
            Regex("(?i)\\b(?:diploma|polytechnic)\\b"),
            // Undergraduate
            Regex("(?i)\\b(?:bachelor|b\\.tech|b\\.e\\b|b\\.sc|bca|b\\.com|bba|undergraduate|b\\.s\\b|b\\.a\\b)\\b"),
            // Postgraduate
            Regex("(?i)\\b(?:master|m\\.tech|m\\.e\\b|m\\.sc|mca|mba|postgraduate|m\\.s\\b|m\\.a\\b|ph\\.?d)\\b")
        )

        for (rawLine in educationLines) {
            val line = rawLine.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            if (line.isBlank() || isSectionHeader(line) || isContactOrAddressLine(line)) continue

            val lw = line.lowercase()
            if (nonAcademicIndicators.any { lw.contains(it) }) continue

            val isNewTier = tierIndicators.any { it.containsMatchIn(line) }
            if (isNewTier && currentBlock.isNotEmpty()) {
                flushBlock()
            }

            currentBlock.add(line)
        }
        flushBlock()

        return if (qualifications.isNotEmpty()) {
            qualifications
        } else {
            educationLines
                .map { it.removePrefix("•").removePrefix("-").removePrefix("*").trim() }
                .filter { line ->
                    val lw = line.lowercase()
                    line.length in 5..300 && !isSectionHeader(line) && !isContactOrAddressLine(line) &&
                    !nonAcademicIndicators.any { lw.contains(it) } &&
                    (tierIndicators.any { it.containsMatchIn(line) } || lw.contains("university") || lw.contains("college") || lw.contains("institute") || lw.contains("school") || lw.contains("degree") || lw.contains("cgpa") || lw.contains("gpa"))
                }
        }
    }

    private fun isContactOrAddressLine(line: String): Boolean {
        val lw = line.lowercase().trim()
        if (lw.isBlank()) return false
        
        // Explicit contact/address labels at start of line
        val contactLabelRegex = Regex("^(?:phone|mobile|mob|tel|telephone|cell|contact|ph|whatsapp|email|e-mail|mail|gmail|github|linkedin|portfolio|website|address|current\\s+address|permanent\\s+address|residence|domicile|location)\\s*[:–-]\\s*", RegexOption.IGNORE_CASE)
        if (contactLabelRegex.containsMatchIn(line)) return true
        
        // Line that is exclusively or predominantly an email or URL
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        if (emailRegex.matches(lw)) return true
        val urlRegex = Regex("^(?:https?://|www\\.|linkedin\\.com|github\\.com)[^\\s]+$")
        if (urlRegex.matches(lw)) return true

        // Standalone phone number line
        val phoneOnlyRegex = Regex("^(?:\\+?\\d{1,4}[\\s-]?)?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{4}$|^\\+?91[\\s-]?[6-9]\\d{9}$|^[6-9]\\d{9}$")
        if (phoneOnlyRegex.matches(lw.replace(" ", ""))) return true

        // Address specific patterns (e.g. "Flat No. 102, ...", "Plot 45, Gandhi Road", "123 Main St, Austin, TX 78701")
        val addressPattern = Regex("(?i)\\b(?:h(?:ouse)?\\.?\\s*no\\.?|door\\.?\\s*no\\.?|flat\\.?\\s*no\\.?|plot\\.?\\s*no\\.?|apt\\.?\\s*no\\.?|ward\\.?\\s*no\\.?)\\s*[:–-]?\\s*\\d+")
        if (addressPattern.containsMatchIn(line)) return true
        
        // Pincode/Zipcode standalone patterns with city/state
        val pincodeWithContext = Regex("(?i)\\b(?:pin\\s*code|pincode|zip\\s*code|postal\\s*code)\\s*[:–-]?\\s*\\d{5,6}\\b")
        if (pincodeWithContext.containsMatchIn(line)) return true

        // Personal / Family metadata labels
        val personalMetadataRegex = Regex("(?i)\\b(?:father'?s\\s+name|mother'?s\\s+name|date\\s+of\\s+birth|d\\.o\\.b|marital\\s+status|nationality\\s*:|gender\\s*:|blood\\s+group\\s*:)\\b")
        if (personalMetadataRegex.containsMatchIn(line)) return true
        
        return false
    }

    /**
     * Parses work experience entries strictly from the experience section.
     * Rejects summary sentences, degree qualifications, standalone certifications, address lines, or skills.
     */
    private fun parseWorkExperienceEntries(experienceLines: List<String>): List<String> {
        if (experienceLines.isEmpty()) return emptyList()

        val validEntries = mutableListOf<String>()
        val currentEntry = mutableListOf<String>()

        fun flushEntry() {
            if (currentEntry.isNotEmpty()) {
                val formatted = currentEntry.joinToString("\n")
                if (formatted.length in 5..800) { // Increased limit for detailed entries
                    validEntries.add(formatted)
                }
                currentEntry.clear()
            }
        }

        val roleTitleRegex = Regex("(?i)\\b(?:intern|internship|developer|engineer|analyst|associate|manager|lead|architect|consultant|trainee|specialist|officer|programmer|specialization)\\b")
        val educationBlockKeywords = listOf("bachelor", "b.tech", "degree", "university", "college", "school", "10th", "12th", "sslc", "hsc", "cgpa", "percentage", "coursework", "secondary", "matriculation")

        for (rawLine in experienceLines) {
            val line = rawLine.trim()
            if (line.isBlank() || isSectionHeader(line) || isContactOrAddressLine(line)) continue

            val lw = line.lowercase()
            // Reject obvious education contamination
            if (educationBlockKeywords.any { lw.contains(it) } && !lw.contains("intern") && !lw.contains("worked") && !lw.contains("experience")) {
                continue
            }

            // Heuristic for role line: contains title + organization or date
            val isRoleLine = roleTitleRegex.containsMatchIn(line) && (line.contains("|") || line.contains("–") || line.contains("-") || line.contains("at") || line.contains(",") || line.contains(":") || Regex("\\b(20\\d{2}|19\\d{2})\\b").containsMatchIn(line))
            
            if (isRoleLine && currentEntry.isNotEmpty()) {
                flushEntry()
            }

            currentEntry.add(line)
        }
        flushEntry()

        return if (validEntries.isNotEmpty()) {
            validEntries
        } else {
            experienceLines.filter { 
                val lw = it.lowercase()
                !isContactOrAddressLine(it) && roleTitleRegex.containsMatchIn(it) && !educationBlockKeywords.any { ed -> lw.contains(ed) }
            }
        }
    }

    /**
     * Parses projects strictly from the project section.
     * Rejects lone technical terms, metadata subheadings (Frontend, Backend, Database, Tools, Description, etc.), certifications, or courses.
     */
    private fun parseProjectEntries(projectLines: List<String>): Pair<List<String>, List<ProjectAnalysis>> {
        if (projectLines.isEmpty()) return Pair(emptyList(), emptyList())

        val metadataLabelsAndSkills = setOf(
            "sql", "mysql", "java", "python", "management", "palm", "ml", "database", "database management",
            "cloud computing", "machine learning", "artificial intelligence", "data structures", "algorithms",
            "operating systems", "computer networks", "software engineering", "web technologies", "deep learning",
            "natural language processing", "system design", "object oriented programming", "core java", "advanced java",
            "frontend", "backend", "fullstack", "tools", "technologies", "tech stack", "role", "roles", "team size",
            "duration", "description", "responsibilities", "key features", "features", "overview", "outcome",
            "outcomes", "objective", "objectives", "environment", "platform", "modules", "summary", "architecture",
            "client", "scope", "impact", "methodology", "contributions", "challenges", "learnings", "technology",
            "tools used", "tech stack used", "technologies used", "programming language", "languages", "libraries", "frameworks"
        )

        val projectNames = mutableListOf<String>()
        val projectAnalyses = mutableListOf<ProjectAnalysis>()

        var currentProjectName: String? = null
        val currentTech = mutableListOf<String>()
        val currentBullets = mutableListOf<String>()

        fun flushProject() {
            val name = currentProjectName
            if (!name.isNullOrBlank()) {
                projectNames.add(name)
                projectAnalyses.add(
                    ProjectAnalysis(
                        name = name,
                        technologies = currentTech.distinct(),
                        demonstratedSkills = currentTech.distinct(),
                        strengths = if (currentBullets.isNotEmpty()) currentBullets.take(3) else listOf("Technical project highlighted in resume"),
                        weaknesses = emptyList(),
                        improvementSuggestions = listOf("Add quantifiable impact metrics to outcome description")
                    )
                )
            }
            currentProjectName = null
            currentTech.clear()
            currentBullets.clear()
        }

        for (rawLine in projectLines) {
            val line = rawLine.trim()
            if (line.isBlank() || isSectionHeader(line) || isContactOrAddressLine(line)) continue

            val cleanLine = line.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            val isBullet = line.startsWith("•") || line.startsWith("-") || line.startsWith("*")

            val lw = cleanLine.lowercase()
            val isMetadataLabel = metadataLabelsAndSkills.any { 
                lw.startsWith(it + ":") || lw.startsWith(it + " :") || lw.startsWith(it + " -") || lw.startsWith(it + " –") || lw.startsWith(it + " |") || lw == it
            }

            if (isMetadataLabel) {
                if (currentProjectName != null) {
                    currentBullets.add(cleanLine)
                    val techPart = if (cleanLine.contains(":")) cleanLine.substringAfter(":") 
                                   else if (cleanLine.contains(" - ")) cleanLine.substringAfter(" - ")
                                   else if (cleanLine.contains(" – ")) cleanLine.substringAfter(" – ")
                                   else ""
                    if (techPart.isNotBlank()) {
                        currentTech.addAll(techPart.split(Regex("[,|/]")).map { it.trim() }.filter { it.isNotBlank() && it.length in 2..30 })
                    }
                }
                continue
            }

            val hasDelimiter = cleanLine.contains("|") || cleanLine.contains("–") || cleanLine.contains(" - ") || cleanLine.contains(" : ")
            
            if (!isBullet && (hasDelimiter || (cleanLine.length in 3..60 && cleanLine.all { it.isLetterOrDigit() || it.isWhitespace() || it == '-' || it == '&' }))) {
                val titlePart = if (hasDelimiter) {
                    cleanLine.split(Regex("[|–]| - | : ")).first().trim()
                } else {
                    cleanLine
                }

                val isBlacklisted = metadataLabelsAndSkills.any { titlePart.equals(it, ignoreCase = true) }
                val isCertification = titlePart.lowercase().contains("certified") || titlePart.lowercase().contains("certification") || titlePart.lowercase().contains("certificate")

                if (!isBlacklisted && !isCertification && titlePart.length in 3..75) {
                    if (currentProjectName != null) {
                        flushProject()
                    }
                    currentProjectName = titlePart
                    
                    if (hasDelimiter) {
                        val techPart = cleanLine.substringAfter(titlePart).trim().removePrefix("|").removePrefix("–").removePrefix("-").removePrefix(":").trim()
                        currentTech.addAll(techPart.split(Regex("[,|/]")).map { it.trim() }.filter { it.isNotBlank() && it.length in 2..30 })
                    }
                } else if (currentProjectName != null) {
                    currentBullets.add(cleanLine)
                }
            } else {
                if (currentProjectName == null && !isBullet && cleanLine.length in 3..60) {
                    currentProjectName = cleanLine
                } else if (currentProjectName != null) {
                    currentBullets.add(cleanLine)
                    if (cleanLine.lowercase().contains("technologies:") || cleanLine.lowercase().contains("tech stack:") || cleanLine.lowercase().contains("tools:")) {
                        val techPart = cleanLine.substringAfter(":").trim()
                        currentTech.addAll(techPart.split(Regex("[,|/]")).map { it.trim() }.filter { it.isNotBlank() && it.length in 2..30 })
                    }
                }
            }
        }
        flushProject()

        return Pair(projectNames, projectAnalyses)
    }

    /**
     * Parses certifications cleanly into a dedicated list strictly from the certifications section.
     */
    private fun parseCertificationEntries(certificationLines: List<String>): List<String> {
        if (certificationLines.isEmpty()) return emptyList()

        return certificationLines
            .map { it.removePrefix("•").removePrefix("-").removePrefix("*").trim() }
            .filter { it.length in 5..150 && !isSectionHeader(it) && !isContactOrAddressLine(it) }
            .distinct()
    }

    /**
     * Extracts and deduplicates skills from skills section and document text.
     */
    private fun parseSkillsList(skillLines: List<String>, rawText: String): List<String> {
        val knownSkillKeywords = listOf(
            "Kotlin", "Java", "Python", "C++", "C#", "C", "R", "JavaScript", "TypeScript", "Swift", "Go", "Rust",
            "SQL", "MySQL", "PostgreSQL", "MongoDB", "Room Database", "SQLite", "Firebase", "Redis",
            "Jetpack Compose", "Android", "Android SDK", "React", "Node.js", "Spring Boot", "Django", "Flask", "Express",
            "Docker", "Kubernetes", "AWS", "GCP", "Azure", "Git", "GitHub", "REST APIs", "GraphQL",
            "CI/CD", "Linux", "Figma", "Tableau", "Power BI", "Postman", "Machine Learning", "TensorFlow",
            "PyTorch", "Coroutines", "MVVM", "Clean Architecture", "HTML", "CSS", "Tailwind CSS", "Pandas", "NumPy",
            "XML", "Material Design", "Robolectric", "JUnit", "MockK", "Espresso"
        )

        val extracted = mutableListOf<String>()

        // 1. Check explicit skills section lines
        for (line in skillLines) {
            val clean = line.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            if (isSectionHeader(clean) || isContactOrAddressLine(clean)) continue
            val contentToParse = if (clean.contains(":")) clean.substringAfter(":") else clean
            val parts = contentToParse.split(Regex("[,|•·\\t/;]")).map { it.trim() }.filter { it.isNotBlank() && it.length in 1..40 }
            for (p in parts) {
                if (!isSectionHeader(p) && !isContactOrAddressLine(p)) {
                    val cleanedSkill = p.replace(Regex("^[•·*\\-]+\\s*"), "").trim()
                    if (cleanedSkill.isNotBlank()) {
                        extracted.add(cleanedSkill)
                    }
                }
            }
        }

        // 2. Cross-match known skills across document
        for (skill in knownSkillKeywords) {
            val pattern = if (skill.length == 1) {
                Regex("(?<=[\\s,;(|]|^)" + Regex.escape(skill) + "(?=[\\s,;)|]|$)")
            } else {
                Regex("\\b" + Regex.escape(skill) + "\\b", RegexOption.IGNORE_CASE)
            }
            if (pattern.containsMatchIn(rawText)) {
                extracted.add(skill)
            }
        }

        return extracted.distinct()
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
        
        val jdHeaderRegex = Regex("(?i)^\\s*(?:[-=#*]{2,}\\s*)?(?:target\\s+)?(?:job\\s+description|job\\s+posting|position\\s+description|job\\s+specification|open\\s+position|role\\s+description|about\\s+the\\s+role|position\\s+overview|job\\s+requisition)(?:\\s*[:–-])?\\s*(?:[-=#*]{2,})?$")
        val hiringIntroRegex = Regex("(?i)\\b(?:we\\s+are\\s+(?:seeking|looking\\s+for|hiring)|job\\s+summary\\s*:|about\\s+the\\s+job\\s*:|role\\s+summary\\s*:)\\b")
        
        var jdStartIndex = -1
        var detectedRoleTitle: String? = null
        
        for (i in lines.indices) {
            val line = lines[i]
            if (jdHeaderRegex.matches(line)) {
                jdStartIndex = i
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
        
        val isCandidateResume = lines.take(20).any { l ->
            val lw = l.lowercase()
            lw.contains("education") || lw.contains("skills") || lw.contains("projects") || 
            lw.contains("curriculum vitae") || lw.contains("resume") || lw.contains("cgpa") || lw.contains("gpa")
        }
        
        if (jdStartIndex == -1) {
            if (isCandidateResume) {
                return null
            }
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

    private data class SectionSpan(
        val type: SectionType,
        val headerIndex: Int,
        val headerText: String
    )

    private class PositionStripper : PDFTextStripper() {
        val capturedPositions = mutableListOf<com.example.aidrivencompetencyplatform.model.TextPosition>()

        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            textPositions?.forEach { pos ->
                capturedPositions.add(
                    com.example.aidrivencompetencyplatform.model.TextPosition(
                        text = pos.unicode,
                        x = pos.xDirAdj,
                        y = pos.yDirAdj,
                        width = pos.widthDirAdj,
                        height = pos.heightDir,
                        pageIndex = currentPageNo - 1
                    )
                )
            }
            super.writeString(text, textPositions)
        }
    }
}

data class DeterministicResumeStructure(
    val candidateInfo: ParsedCandidateInfo,
    val sections: List<com.example.aidrivencompetencyplatform.model.ParsedSectionItem>,
    val extractedSkills: List<String>,
    val extractedProjects: List<String>,
    val detectedJobDescription: com.example.aidrivencompetencyplatform.model.JobDescriptionSection? = null,
    val summary: String? = null,
    val education: List<String> = emptyList(),
    val experience: List<String> = emptyList(),
    val personalInfo: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val projectAnalyses: List<ProjectAnalysis> = emptyList()
)

data class ParsedCandidateInfo(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null
)
