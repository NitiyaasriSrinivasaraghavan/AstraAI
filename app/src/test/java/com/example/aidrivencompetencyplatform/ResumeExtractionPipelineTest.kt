package com.example.aidrivencompetencyplatform

import com.example.aidrivencompetencyplatform.data.AtsScoringEngine
import com.example.aidrivencompetencyplatform.data.GeminiService
import com.example.aidrivencompetencyplatform.data.ResumeParser
import com.example.aidrivencompetencyplatform.model.*
import org.junit.Assert.*
import org.junit.Test

class ResumeExtractionPipelineTest {

    private val resumeParser = ResumeParser()
    private val atsScoringEngine = AtsScoringEngine()
    private val geminiService = GeminiService()

    @Test
    fun testSemanticSummaryExtraction_WithUnconventionalHeader() {
        val resumeText = """
            Nitiyaa Sri
            Email: nitiyaa@example.com | Phone: 9876543210 | Location: Chennai, Tamil Nadu
            
            ABOUT ME
            Passionate and driven software engineering student specializing in native Android and modern cloud architectures. Experienced in building responsive UI and scalable mobile applications.
            
            EDUCATION
            B.Tech in Information Technology | XYZ Institute of Technology | 2022 - 2026 | CGPA: 8.9
            Class XII (HSC) | ABC Matriculation Higher Secondary School | 2022 | 94.2%
            Class X (SSLC) | ABC High School | 2020 | 96.0%
            
            TECHNICAL SKILLS
            Kotlin, Java, Jetpack Compose, Room Database, Git, SQL
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)
        assertNotNull("Summary should be extracted", structure.summary)
        assertTrue("Summary should contain profile text", structure.summary!!.contains("software engineering student"))
        assertFalse("Summary should not be empty", structure.summary!!.isBlank())
    }

    @Test
    fun testSemanticSummaryExtraction_WithoutHeader() {
        val resumeText = """
            John Doe
            Email: john.doe@example.com | Phone: +1 415 555 0199 | Location: San Francisco, CA
            
            High-impact Android Engineer with experience in Kotlin, Jetpack Compose, and building reactive mobile apps.
            
            EDUCATION
            Bachelor of Science in Computer Science | UC Berkeley | 2020 - 2024
            
            TECHNICAL SKILLS
            Kotlin, Jetpack Compose, Coroutines, Room
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)
        assertNotNull("Introductory paragraph should be identified as summary", structure.summary)
        assertTrue("Summary should contain intro text", structure.summary!!.contains("High-impact Android Engineer"))
    }

    @Test
    fun testWorkExperience_InternshipsOnly_NoContamination() {
        val resumeText = """
            Jane Developer
            Email: jane@example.com | Phone: 9876543210 | Location: Bengaluru, Karnataka
            
            PROFILE
            Dedicated Android Developer with hands-on internship experience building mobile apps.
            
            EDUCATION
            B.Tech Computer Science | National Institute of Technology | 2021 - 2025 | CGPA: 8.5
            Class XII | KV School | 2021 | 91%
            Class X | KV School | 2019 | 95%
            
            INTERNSHIPS
            Android Developer Intern | Innovate Tech Labs
            June 2024 – August 2024 | Bengaluru
            - Built 4 Jetpack Compose screens for e-commerce client app.
            - Integrated REST APIs with Retrofit and Kotlinx Serialization.
            
            PROJECTS
            Smart Task Manager | Kotlin, Room, Compose
            - Task management app with offline SQLite sync and notifications.
            
            CERTIFICATIONS
            Google Associate Android Developer Certification
            
            TECHNICAL SKILLS
            Kotlin, Compose, Android SDK, Git, SQLite
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify Work Experience has ONLY the internship
        assertEquals("Should contain 1 work experience/internship entry", 1, structure.experience.size)
        assertTrue("Work experience should contain internship role", structure.experience[0].contains("Android Developer Intern"))
        assertFalse("Work experience must NOT contain summary", structure.experience[0].contains("Dedicated Android Developer"))
        assertFalse("Work experience must NOT contain education", structure.experience[0].contains("National Institute of Technology"))
        assertFalse("Work experience must NOT contain certification", structure.experience[0].contains("Google Associate Android Developer Certification"))
        assertFalse("Work experience must NOT contain project description", structure.experience[0].contains("Smart Task Manager"))
    }

    @Test
    fun testWorkExperience_FresherWithNoExperience_ReturnsEmpty() {
        val resumeText = """
            Student Developer
            Email: student@example.com | Phone: 9876543210 | Location: Chennai, Tamil Nadu
            
            CAREER OBJECTIVE
            Aspiring Software Engineer seeking entry-level developer opportunities.
            
            EDUCATION
            B.E. Computer Science and Engineering | Anna University | 2022 - 2026 | CGPA: 8.8
            Class XII | St. Joseph Higher Secondary School | 2022 | 93%
            Class X | St. Joseph High School | 2020 | 95%
            
            PROJECTS
            Campus Portal Application | Kotlin, Firebase
            - Real-time event notifications and student bulletin board.
            
            TECHNICAL SKILLS
            Kotlin, Java, SQL, Git
            
            CERTIFICATIONS
            Oracle Certified Java Associate
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)
        assertTrue("Fresher with no experience should have empty experience list", structure.experience.isEmpty())
    }

    @Test
    fun testEducation_PreservesAllTiers_10th_12th_College() {
        val resumeText = """
            Alex Smith
            Email: alex@example.com | Phone: 9876543210 | Location: Hyderabad, Telangana
            
            SUMMARY
            Computer Science undergraduate with strong foundation in algorithms and development.
            
            EDUCATION
            B.Tech in Computer Science and Engineering | IIT Hyderabad | 2022 - 2026 | CGPA: 9.1
            Higher Secondary Certificate (HSC - 12th) | Delhi Public School | 2022 | 95.6%
            Secondary School Leaving Certificate (SSLC - 10th) | Delhi Public School | 2020 | 97.2%
            
            TECHNICAL SKILLS
            Python, Java, Kotlin, SQL, Data Structures
            
            PROJECTS
            Algorithmic Trading Bot | Python, Pandas
            - Developed automated backtesting platform for equity trading strategies.
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify ALL 3 education qualifications are extracted
        assertEquals("Should extract all 3 education qualifications", 3, structure.education.size)
        assertTrue("Should contain college degree", structure.education.any { it.contains("B.Tech") || it.contains("IIT") })
        assertTrue("Should contain 12th/HSC", structure.education.any { it.contains("12th") || it.contains("HSC") || it.contains("Higher Secondary") })
        assertTrue("Should contain 10th/SSLC", structure.education.any { it.contains("10th") || it.contains("SSLC") || it.contains("Secondary School") })
    }

    @Test
    fun testProjects_StrictProjectValidation_NoLoneSkillsOrCertifications() {
        val resumeText = """
            Priya Raman
            Email: priya@example.com | Phone: 9876543210 | Location: Chennai, Tamil Nadu
            
            PROFESSIONAL SUMMARY
            Mobile application developer with expertise in Kotlin and Compose.
            
            EDUCATION
            B.Tech Information Technology | MIT Chennai | 2022 - 2026 | CGPA: 8.7
            Class 12th | DAV Girls Senior Secondary School | 2022 | 94%
            Class 10th | DAV Girls Senior Secondary School | 2020 | 96%
            
            PROJECTS
            Smart Medical Prescription Reader | Kotlin, Gemini AI, Jetpack Compose
            - Built Android application extracting structured medical dosage schedules from doctor prescriptions.
            - Implemented offline database caching with Room SQLite.
            
            EcoTrack Carbon Footprint Estimator | Kotlin, Clean Architecture, Coroutines
            - Designed mobile app calculating household emissions and personalized reduction milestones.
            
            TECHNICAL SKILLS
            Database Management, Machine Learning, Kotlin, Java, SQLite, Git
            
            CERTIFICATIONS
            AWS Certified Cloud Practitioner
            Google Associate Android Developer
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify exactly 2 actual projects are extracted
        assertEquals("Should extract exactly 2 actual projects", 2, structure.extractedProjects.size)
        assertTrue("Project 1 title should match", structure.extractedProjects.any { it.contains("Smart Medical Prescription Reader") })
        assertTrue("Project 2 title should match", structure.extractedProjects.any { it.contains("EcoTrack Carbon Footprint Estimator") })

        // Verify technical terms and certifications are NOT in projects
        assertFalse("Database Management must not become a project", structure.extractedProjects.any { it.equals("Database Management", ignoreCase = true) })
        assertFalse("Machine Learning must not become a project", structure.extractedProjects.any { it.equals("Machine Learning", ignoreCase = true) })
        assertFalse("Certifications must not be in projects", structure.extractedProjects.any { it.contains("AWS Certified") || it.contains("Google Associate") })

        // Verify certifications are kept in certifications
        assertEquals("Should extract 2 certifications", 2, structure.certifications.size)
    }

    @Test
    fun testDownstreamPipeline_AtsScoringConsumesCorrectStructure() {
        val result = ResumeAnalysisResult(
            id = "test-analysis-1",
            overallScore = 85,
            atsScore = 85,
            skillMatch = 80,
            targetRole = "Android Developer",
            summary = "Experienced Android developer with Kotlin and Compose expertise.",
            candidateName = "Alex Chen",
            candidateEmail = "alex@example.com",
            candidatePhone = "4155550199",
            candidateLocation = "San Francisco, CA",
            education = listOf("B.S. Computer Science | UC Berkeley", "High School Diploma | Lincoln High"),
            experience = listOf("Android Developer Intern | Nexus Studio\nBuilt 3 Compose features"),
            extractedSkills = listOf(
                Skill(name = "Kotlin", level = 90),
                Skill(name = "Android SDK", level = 85),
                Skill(name = "Jetpack Compose", level = 85),
                Skill(name = "Room Database", level = 80),
                Skill(name = "Git", level = 85)
            ),
            projectAnalysis = listOf(
                ProjectAnalysis(name = "Astra AI Platform", technologies = listOf("Kotlin", "Compose"))
            )
        )

        val score = atsScoringEngine.calculateScore(result, "Android Developer")
        assertEquals(85, score.overallScore)
        assertEquals("Alex Chen", score.candidateName)
        assertEquals("alex@example.com", score.candidateEmail)
        assertTrue("Parsing accuracy should be high", score.parsingAccuracy.score >= 80)
        assertTrue("Resume structure should detect all components", score.resumeStructure.score >= 80)
    }

    @Test
    fun testKeywordCoverage_ZeroMatch_MustBeZeroOutOf100_NoFallbacks() {
        // Java/Python resume tested against Cloud Architect target role
        val zeroMatchResume = ResumeAnalysisResult(
            id = "zero-match-test",
            overallScore = 0,
            atsScore = 0,
            skillMatch = 0,
            targetRole = "Cloud Architect",
            candidateName = "Backend Dev",
            candidateEmail = "dev@example.com",
            extractedSkills = listOf(
                Skill(name = "Java", level = 90),
                Skill(name = "Python", level = 85)
            ),
            education = listOf("B.S. in Computer Science"),
            experience = listOf("Software Developer | Corp")
        )

        val atsResult = atsScoringEngine.calculateScore(zeroMatchResume, "Cloud Architect")

        // Strictly 0/100 Keyword Coverage score and 0% match
        assertEquals("Keyword Coverage score must be strictly 0/100 when keyword match is 0%", 0, atsResult.keywordCoverage.score)
        assertEquals("Weighted contribution must be 0.0 pts", 0.0, atsResult.keywordCoverage.weightedContribution, 0.01)
        assertEquals("Matched list must be empty", 0, atsResult.keywordCoverage.matchedList.size)
        assertTrue("Status must indicate 0% / No Match", atsResult.keywordCoverage.status.contains("0%") || atsResult.keywordCoverage.status.contains("No Match"))
        assertTrue("Calculation text must show 0%", atsResult.keywordCoverage.calculationText.contains("= 0%"))
    }

    @Test
    fun testKeywordCoverage_ProportionalAndFullMatch() {
        val fullMatchResume = ResumeAnalysisResult(
            id = "full-match-test",
            overallScore = 0,
            atsScore = 0,
            skillMatch = 0,
            targetRole = "Cloud Architect",
            extractedSkills = listOf(
                Skill(name = "AWS", level = 90),
                Skill(name = "Azure", level = 90),
                Skill(name = "Cloud Architecture", level = 90),
                Skill(name = "GCP", level = 90),
                Skill(name = "Kubernetes", level = 90),
                Skill(name = "Docker", level = 90),
                Skill(name = "Terraform", level = 90),
                Skill(name = "CI/CD", level = 90),
                Skill(name = "Linux", level = 90),
                Skill(name = "Git", level = 90),
                Skill(name = "Monitoring", level = 90),
                Skill(name = "Security", level = 90),
                Skill(name = "Networking", level = 90)
            )
        )

        val atsFullResult = atsScoringEngine.calculateScore(fullMatchResume, "Cloud Architect")
        assertEquals("Full match must yield 100/100", 100, atsFullResult.keywordCoverage.score)
        assertEquals("Full match contribution must be 35.0", 35.0, atsFullResult.keywordCoverage.weightedContribution, 0.01)
    }

    @Test
    fun testJdMatcher_StrictComparison_And_FreeResources() {
        val candidateResume = ResumeAnalysisResult(
            id = "test-resume-1",
            overallScore = 88,
            atsScore = 88,
            skillMatch = 85,
            targetRole = "Android Developer",
            candidateName = "Dev Candidate",
            summary = "Experienced Android developer.",
            extractedSkills = listOf(
                Skill(name = "Kotlin", level = 90),
                Skill(name = "Jetpack Compose", level = 85),
                Skill(name = "Room Database", level = 80),
                Skill(name = "Git", level = 85)
            ),
            education = listOf("B.Tech in Computer Science | 2024"),
            experience = listOf("Android Developer | TechCorp (1.5 years)")
        )

        val jdMatchJson = """
            {
              "jobTitle": "Senior Android Engineer",
              "companyName": "Acme Innovations",
              "roleSummary": "Looking for an engineer to architect Kotlin applications and deploy cloud container pipelines.",
              "matchScore": 72,
              "matchedSkills": ["Kotlin", "Jetpack Compose", "Room Database", "Git"],
              "missingSkills": ["Docker", "AWS", "Kubernetes"],
              "preferredSkillsMatched": [],
              "preferredSkillsMissing": ["CI/CD"],
              "responsibilities": [
                "Build modern Compose UI",
                "Deploy microservices in Docker",
                "Maintain AWS cloud pipelines"
              ],
              "experienceRequirement": "2+ years of professional experience",
              "candidateExperience": "1.5 years at TechCorp",
              "experienceMatchStatus": "Partial Match",
              "experienceMatchExplanation": "Candidate has 1.5 years against 2+ years required.",
              "educationRequirement": "Bachelor's in Computer Science",
              "candidateEducation": "B.Tech in Computer Science",
              "educationMatchStatus": "Match",
              "educationMatchExplanation": "B.Tech satisfies degree requirement.",
              "certificationRequirement": "None",
              "candidateCertifications": "None",
              "certificationMatchStatus": "Not Required",
              "certificationMatchExplanation": "No certifications requested.",
              "strengths": [
                "Strong Kotlin and declarative Compose proficiency",
                "Solid local persistence experience with Room"
              ],
              "priorityGaps": ["Docker", "AWS", "Kubernetes"],
              "recommendedActions": [
                "Complete Docker containerization tutorial",
                "Learn AWS cloud deployment fundamentals"
              ]
            }
        """.trimIndent()

        val matchResult = geminiService.parseJdMatchJson(jdMatchJson, "Sample JD", candidateResume)

        assertEquals("Senior Android Engineer", matchResult.jobTitle)
        assertEquals("Acme Innovations", matchResult.companyName)
        assertEquals(72, matchResult.matchScore)
        assertEquals(4, matchResult.matchedSkills.size)
        assertTrue(matchResult.matchedSkills.contains("Kotlin"))
        assertTrue(matchResult.matchedSkills.contains("Jetpack Compose"))
        assertEquals(3, matchResult.missingSkills.size)
        assertTrue(matchResult.missingSkills.contains("Docker"))
        assertTrue(matchResult.missingSkills.contains("AWS"))
        assertTrue(matchResult.missingSkills.contains("Kubernetes"))

        // Verify free learning resources are populated for missing skills
        assertTrue("Must provide free learning resources", matchResult.freeLearningResources.isNotEmpty())
        assertTrue("All learning resources must be free", matchResult.freeLearningResources.all { it.isFree })
        assertTrue(
            "Should include Docker course resource",
            matchResult.freeLearningResources.any { it.title.contains("Docker", ignoreCase = true) }
        )
    }

    @Test
    fun testJdMatcher_DynamicScoreFallbackCalculation() {
        val candidateResume = ResumeAnalysisResult(
            id = "test-resume-2",
            overallScore = 80,
            atsScore = 80,
            skillMatch = 75,
            targetRole = "Machine Learning Engineer",
            extractedSkills = listOf(
                Skill(name = "Python", level = 90),
                Skill(name = "SQL", level = 85)
            ),
            experience = emptyList(), // Fresher
            education = listOf("B.S. in Data Science")
        )

        val jdMatchJsonWithoutScore = """
            {
              "jobTitle": "Machine Learning Engineer",
              "matchedSkills": ["Python", "SQL"],
              "missingSkills": ["TensorFlow", "PyTorch", "Docker"],
              "preferredSkillsMatched": [],
              "preferredSkillsMissing": ["MLOps"],
              "experienceMatchStatus": "Gap",
              "educationMatchStatus": "Match"
            }
        """.trimIndent()

        val matchResult = geminiService.parseJdMatchJson(jdMatchJsonWithoutScore, "Sample ML JD", candidateResume)

        assertTrue("Dynamic score should be calculated", matchResult.matchScore in 10..95)
        assertEquals("Machine Learning Engineer", matchResult.jobTitle)
        assertEquals(2, matchResult.matchedSkills.size)
        assertEquals(3, matchResult.missingSkills.size)
    }

    @Test
    fun testJdMatcher_UnabridgedEducationRequirementPreserved() {
        val candidateResume = ResumeAnalysisResult(
            id = "test-resume-edu",
            overallScore = 85,
            atsScore = 88,
            skillMatch = 82,
            targetRole = "Full Stack Engineer",
            extractedSkills = listOf(Skill(name = "TypeScript", level = 90)),
            education = listOf("Bachelor of Technology in Computer Science and Engineering, XYZ University")
        )

        val completeEducationText = "Bachelor's degree in Computer Science, Information Technology, Software Engineering, or a related field. Master's degree preferred."
        val jdMatchJson = """
            {
              "jobTitle": "Full Stack Engineer",
              "matchedSkills": ["TypeScript"],
              "missingSkills": ["Next.js", "Docker"],
              "educationRequirement": "$completeEducationText",
              "candidateEducation": "Bachelor of Technology in Computer Science and Engineering, XYZ University",
              "educationMatchStatus": "Match",
              "educationMatchExplanation": "Candidate degree in Computer Science meets the Bachelor's degree in related technical field requirement."
            }
        """.trimIndent()

        val matchResult = geminiService.parseJdMatchJson(jdMatchJson, "Sample Full Stack JD", candidateResume)

        assertEquals("Education requirement must be preserved unabridged without truncation", completeEducationText, matchResult.educationRequirement)
        assertTrue(matchResult.educationRequirement.contains("Bachelor's degree in Computer Science, Information Technology, Software Engineering, or a related field"))
        assertTrue(matchResult.educationRequirement.contains("Master's degree preferred"))
        assertEquals("Match", matchResult.educationMatchStatus)
    }

    @Test
    fun testJdMatcher_CandidateWithoutCertifications_DisplaysNoneListed() {
        val resumeWithoutCerts = ResumeAnalysisResult(
            id = "resume-no-certs",
            overallScore = 80,
            atsScore = 80,
            skillMatch = 75,
            targetRole = "Cloud Architect",
            extractedSkills = listOf(Skill(name = "Java", level = 90)),
            certifications = emptyList()
        )

        val jdMatchJson = """
            {
              "jobTitle": "Cloud Architect",
              "matchedSkills": [],
              "missingSkills": ["AWS"],
              "certificationRequirement": "AWS Certified Solutions Architect, Azure Solutions Architect, Google Cloud Professional Cloud Architect, or equivalent certification.",
              "candidateCertifications": "",
              "certificationMatchStatus": "Gap"
            }
        """.trimIndent()

        val matchResult = geminiService.parseJdMatchJson(jdMatchJson, "Cloud JD", resumeWithoutCerts)
        assertEquals("Candidate certifications must be 'None listed' when resume has no certifications", "None listed", matchResult.candidateCertifications)
    }

    @Test
    fun testJdMatcher_CandidateWithCertifications_DisplaysExactCertifications() {
        val resumeWithCerts = ResumeAnalysisResult(
            id = "resume-with-certs",
            overallScore = 90,
            atsScore = 90,
            skillMatch = 85,
            targetRole = "Cloud Architect",
            extractedSkills = listOf(Skill(name = "AWS", level = 90)),
            certifications = listOf("AWS Certified Solutions Architect – Associate", "Microsoft Azure Fundamentals")
        )

        val jdMatchJson = """
            {
              "jobTitle": "Cloud Architect",
              "matchedSkills": ["AWS"],
              "missingSkills": [],
              "certificationRequirement": "AWS Certified Solutions Architect, Azure Solutions Architect, Google Cloud Professional Cloud Architect, or equivalent certification.",
              "candidateCertifications": "AWS Certified Solutions Architect – Associate, Microsoft Azure Fundamentals",
              "certificationMatchStatus": "Match"
            }
        """.trimIndent()

        val matchResult = geminiService.parseJdMatchJson(jdMatchJson, "Cloud JD", resumeWithCerts)
        assertEquals(
            "Candidate certifications must display exact extracted certifications",
            "AWS Certified Solutions Architect – Associate, Microsoft Azure Fundamentals",
            matchResult.candidateCertifications
        )
    }

    @Test
    fun testJdMatcher_IndependentCertifications_NoRequirementWithCandidateCerts() {
        val resumeWithCerts = ResumeAnalysisResult(
            id = "resume-with-certs-2",
            overallScore = 85,
            atsScore = 85,
            skillMatch = 80,
            targetRole = "Java Developer",
            extractedSkills = listOf(Skill(name = "Java", level = 90)),
            certifications = listOf("Oracle Certified Professional: Java SE 11 Developer", "Spring Certified Professional")
        )

        // Case: JD has NO requirement, but candidate HAS certs
        val jdMatchJson = """
            {
              "jobTitle": "Java Developer",
              "matchedSkills": ["Java"],
              "missingSkills": [],
              "certificationRequirement": "No certification requirement specified",
              "candidateCertifications": "Oracle Certified Professional: Java SE 11 Developer, Spring Certified Professional",
              "certificationMatchStatus": "Not Required"
            }
        """.trimIndent()

        val matchResult = geminiService.parseJdMatchJson(jdMatchJson, "Java JD", resumeWithCerts)
        
        assertEquals("Requirement should be clear", "No certification requirement specified", matchResult.certificationRequirement)
        assertTrue("Candidate certifications must be preserved independently of JD requirement", 
            matchResult.candidateCertifications.contains("Oracle Certified Professional") && 
            matchResult.candidateCertifications.contains("Spring Certified Professional"))
    }
}

