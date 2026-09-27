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

    @Test
    fun testSemanticSummaryEquivalents_AllFiveVariationsMapToSummaryField() {
        val variations = listOf(
            "Professional Summary" to "Results-oriented Android developer with 4 years of Kotlin and Jetpack Compose expertise.",
            "Career Objective" to "Dedicated software engineer seeking to leverage mobile engineering skills to build scalable apps.",
            "Profile" to "Passionate mobile engineer with proven track record in architecting modern reactive applications.",
            "Career Summary" to "Experienced mobile technologist specializing in performance optimization and clean architecture.",
            "About Me" to "Curious and creative developer enthusiastic about crafting accessible and performant mobile experiences.",
            "Executive Summary" to "Seasoned software development leader with deep technical expertise in mobile and cloud infrastructure.",
            "Professional Profile" to "High-impact engineer with strong background in distributed systems and Android development.",
            "Objective" to "Aspiring mobile applications developer focused on delivering high quality Kotlin and Compose applications.",
            "PROFILE SUMMARY:" to "Proactive developer with strong problem-solving skills and expertise in Android SDK.",
            "1. CAREER OBJECTIVE" to "To obtain a challenging software engineering position in mobile app development.",
            "CAREER OBJECTIVES" to "Seeking to contribute to high scale mobile solutions using modern Android tools.",
            "### Summary / Objective" to "Experienced software engineer with extensive background in native Android engineering."
        )

        for ((heading, expectedContent) in variations) {
            val resumeText = """
                Candidate Name
                Email: candidate@example.com | Phone: 9876543210 | Location: Bengaluru, India
                
                $heading
                $expectedContent
                
                EDUCATION
                B.Tech in Computer Science | National University | 2020 - 2024
                
                TECHNICAL SKILLS
                Kotlin, Java, Compose, SQLite, Git
            """.trimIndent()

            val structure = resumeParser.parseResumeStructure(resumeText)
            assertNotNull("Summary should be extracted for heading: '$heading'", structure.summary)
            assertTrue(
                "Summary content for '$heading' must contain expected text",
                structure.summary!!.contains(expectedContent.take(30))
            )
            val summarySectionItem = structure.sections.find { it.sectionName == "Summary" }
            assertNotNull("Summary section item must exist for heading '$heading'", summarySectionItem)
            assertTrue("Summary section item should be detected as present", summarySectionItem!!.isFound)
            assertEquals("Summary section item content must match extracted summary", structure.summary, summarySectionItem.summary)
        }
    }

    @Test
    fun testSemanticWorkExperienceEquivalents_AllVariationsMapToExperienceField() {
        val variations = listOf(
            "Work Experience",
            "Professional Experience",
            "Employment History",
            "Work History",
            "Career History",
            "Internships",
            "Relevant Experience"
        )

        for (heading in variations) {
            val resumeText = """
                Candidate Dev
                Email: dev@example.com | Phone: 9876543210 | Location: San Francisco, CA
                
                CAREER OBJECTIVE
                Software engineer focused on mobile technologies.
                
                $heading
                Android Software Engineer | Acme Corp
                June 2022 – Present | San Francisco, CA
                - Built core user onboarding flow in Jetpack Compose.
                - Refactored networking layer to Kotlin Coroutines and Flow.
                
                EDUCATION
                B.S. in Computer Science | UC Berkeley | 2018 - 2022
                
                SKILLS
                Kotlin, Compose, Coroutines, Flow
            """.trimIndent()

            val structure = resumeParser.parseResumeStructure(resumeText)
            assertEquals("Work experience must be extracted for heading '$heading'", 1, structure.experience.size)
            assertTrue("Must contain role from experience section", structure.experience[0].contains("Android Software Engineer"))
            assertTrue("Must contain company name", structure.experience[0].contains("Acme Corp"))
            val expSection = structure.sections.find { it.sectionName == "Work Experience" }
            assertNotNull("Work Experience section item must exist", expSection)
            assertTrue("Work Experience section must be detected", expSection!!.isFound)
        }
    }

    @Test
    fun testSemanticProjectsEquivalents_AllVariationsMapToProjectsField() {
        val variations = listOf(
            "Projects",
            "Academic Projects",
            "Selected Projects",
            "Personal Projects",
            "Key Projects",
            "Selected Work",
            "Project Experience"
        )

        for (heading in variations) {
            val resumeText = """
                Jane Doe
                Email: jane.doe@example.com | Phone: 9876543210 | Location: Austin, TX
                
                PROFESSIONAL SUMMARY
                Mobile engineer passionate about offline-first architectures.
                
                $heading
                Astra Competency Engine | Kotlin, Jetpack Compose, Room DB
                - Developed deterministic career trajectory and skill gap scoring algorithm.
                - Integrated local persistence layer with Room SQLite.
                
                CryptoTracker Mobile | Kotlin, WebSocket, Coroutines
                - Built real-time price monitoring dashboard with WebSocket streaming.
                
                TECHNICAL SKILLS
                Kotlin, Compose, Room, SQLite, WebSocket, Coroutines
                
                EDUCATION
                B.S. Computer Science | UT Austin | 2020 - 2024
            """.trimIndent()

            val structure = resumeParser.parseResumeStructure(resumeText)
            assertEquals("Must extract 2 projects for heading '$heading'", 2, structure.extractedProjects.size)
            assertTrue("Must contain Astra Competency Engine", structure.extractedProjects.contains("Astra Competency Engine"))
            assertTrue("Must contain CryptoTracker Mobile", structure.extractedProjects.contains("CryptoTracker Mobile"))
            val projSection = structure.sections.find { it.sectionName == "Projects" }
            assertNotNull("Projects section must exist", projSection)
            assertTrue("Projects section must be detected", projSection!!.isFound)
        }
    }

    @Test
    fun testSemanticSkillsEquivalents_AllVariationsMapToSkillsField() {
        val variations = listOf(
            "Technical Skills",
            "Skills & Technologies",
            "Core Competencies",
            "Technologies",
            "Tech Stack",
            "Tools & Technologies",
            "Areas of Expertise"
        )

        for (heading in variations) {
            val resumeText = """
                Skill Candidate
                Email: skill@example.com | Phone: 9876543210 | Location: Seattle, WA
                
                PROFESSIONAL SUMMARY
                Full stack and mobile developer.
                
                $heading
                Programming Languages: Kotlin, Java, Python, SQL
                Frameworks & Tools: Jetpack Compose, Docker, Git, Room Database
                
                EDUCATION
                B.S. Computer Science | University of Washington | 2021 - 2025
            """.trimIndent()

            val structure = resumeParser.parseResumeStructure(resumeText)
            assertTrue("Must extract skills for heading '$heading'", structure.extractedSkills.isNotEmpty())
            assertTrue("Must extract Kotlin", structure.extractedSkills.any { it.equals("Kotlin", ignoreCase = true) })
            assertTrue("Must extract Compose", structure.extractedSkills.any { it.contains("Compose", ignoreCase = true) })
            val skillsSection = structure.sections.find { it.sectionName == "Skills" }
            assertNotNull("Skills section item must exist", skillsSection)
            assertTrue("Skills section item must be detected", skillsSection!!.isFound)
        }
    }

    @Test
    fun testContentBasedFallback_UnconventionalHeadings() {
        // Test "MY CAREER JOURNEY" classified as summary by content
        val resumeWithJourney = """
            Alex Journey
            Email: alex@example.com | Phone: 9876543210 | Location: New York, NY
            
            MY CAREER JOURNEY
            Software engineer with 5 years of experience in distributed backend systems and mobile application development. Passionate about building resilient microservices and intuitive user interfaces.
            
            EDUCATION
            B.S. Computer Science | NYU | 2019
            
            TECHNICAL SKILLS
            Kotlin, Java, Python, Docker, Git
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeWithJourney)
        assertNotNull("Unconventional heading 'MY CAREER JOURNEY' should be classified as summary", structure.summary)
        assertTrue("Summary should contain career text", structure.summary!!.contains("Software engineer with 5 years"))

        // Test "SELECTED WORK" with project descriptions classified as projects
        val resumeWithSelectedWork = """
            Sam Selected
            Email: sam@example.com | Phone: 9876543210 | Location: Chicago, IL
            
            PROFESSIONAL SUMMARY
            Mobile application architect.
            
            SELECTED WORK
            TaskMaster Pro | Kotlin, Jetpack Compose, Coroutines
            - Architected modular task management tool with offline syncing.
            
            EDUCATION
            B.S. Software Engineering | UIUC | 2022
            
            SKILLS
            Kotlin, Compose, Coroutines
        """.trimIndent()

        val structureWork = resumeParser.parseResumeStructure(resumeWithSelectedWork)
        assertTrue("Unconventional heading 'SELECTED WORK' should extract projects", structureWork.extractedProjects.isNotEmpty())
        assertTrue("Must extract TaskMaster Pro", structureWork.extractedProjects.contains("TaskMaster Pro"))
    }

    @Test
    fun testStrictSectionBoundaries_NoCrossContamination() {
        val resumeText = """
            Candidate Strict
            Email: strict@example.com | Phone: 9876543210 | Location: Boston, MA
            
            CAREER OBJECTIVE
            Dedicated Android engineer striving to develop clean, scalable mobile apps.
            
            TECHNICAL SKILLS
            Kotlin, Jetpack Compose, Room Database, Git, SQL
            
            WORK EXPERIENCE
            Android Developer Intern | Mobile Innovations LLC
            May 2023 – August 2023 | Boston, MA
            - Built 5 Compose screens for enterprise client.
            - Implemented Room database offline caching.
            
            EDUCATION
            B.S. in Computer Science | Boston University | 2020 - 2024 | CGPA: 3.8
            High School Diploma | Boston Latin School | 2020
            
            PROJECTS
            SmartFinance Mobile | Kotlin, Jetpack Compose, Room
            - Built financial budgeting app with chart visualizers.
            
            CERTIFICATIONS
            Google Associate Android Developer Certification
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify summary contains ONLY objective text
        assertEquals(
            "Dedicated Android engineer striving to develop clean, scalable mobile apps.",
            structure.summary
        )
        assertFalse("Summary must NOT contain skills", structure.summary!!.contains("Kotlin, Jetpack Compose"))
        assertFalse("Summary must NOT contain experience", structure.summary!!.contains("Mobile Innovations LLC"))
        assertFalse("Summary must NOT contain education", structure.summary!!.contains("Boston University"))

        // Verify work experience contains ONLY work experience
        assertEquals(1, structure.experience.size)
        assertTrue(structure.experience[0].contains("Android Developer Intern"))
        assertFalse(structure.experience[0].contains("Dedicated Android engineer"))
        assertFalse(structure.experience[0].contains("Boston University"))
        assertFalse(structure.experience[0].contains("SmartFinance Mobile"))

        // Verify education contains ONLY education
        assertEquals(2, structure.education.size)
        assertTrue(structure.education.any { it.contains("Boston University") })
        assertTrue(structure.education.any { it.contains("Boston Latin School") })

        // Verify projects contains ONLY projects
        assertEquals(1, structure.extractedProjects.size)
        assertEquals("SmartFinance Mobile", structure.extractedProjects[0])

        // Verify certifications contains ONLY certifications
        assertEquals(1, structure.certifications.size)
        assertTrue(structure.certifications[0].contains("Google Associate Android Developer"))

        // Verify contact info is not contaminated
        assertEquals("Candidate Strict", structure.candidateInfo.name)
        assertEquals("strict@example.com", structure.candidateInfo.email)
        assertEquals("9876543210", structure.candidateInfo.phone)
        assertEquals("Boston, MA", structure.candidateInfo.location)
    }

    @Test
    fun testAcceptance3_AllExplicitSkillsExtractedWithoutLimit() {
        val resumeText = """
            Dev Skillmaster
            Email: dev@example.com | Phone: 9876543210 | Location: Coimbatore, Tamil Nadu
            
            CAREER OBJECTIVE
            Software developer passionate about system design and cloud technologies.
            
            TECHNICAL SKILLS
            Java, Python, C, SQL, HTML, CSS, JavaScript, Git, Android, Firebase, Machine Learning, Kotlin, Docker, AWS
            
            EDUCATION
            B.E. Computer Science | PSG College of Technology | 2021 - 2025 | CGPA: 8.9
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)
        val extractedSkills = structure.extractedSkills

        // Verify there is NO artificial limit (e.g. > 10 skills extracted)
        assertTrue("Must extract more than 3 skills without limitation", extractedSkills.size >= 10)
        assertTrue("Must extract Java", extractedSkills.any { it.equals("Java", ignoreCase = true) })
        assertTrue("Must extract Python", extractedSkills.any { it.equals("Python", ignoreCase = true) })
        assertTrue("Must extract single-character skill C", extractedSkills.any { it.equals("C", ignoreCase = true) })
        assertTrue("Must extract SQL", extractedSkills.any { it.equals("SQL", ignoreCase = true) })
        assertTrue("Must extract HTML", extractedSkills.any { it.equals("HTML", ignoreCase = true) })
        assertTrue("Must extract JavaScript", extractedSkills.any { it.contains("JavaScript", ignoreCase = true) })
        assertTrue("Must extract Git", extractedSkills.any { it.equals("Git", ignoreCase = true) })
        assertTrue("Must extract Android", extractedSkills.any { it.equals("Android", ignoreCase = true) })
        assertTrue("Must extract Firebase", extractedSkills.any { it.equals("Firebase", ignoreCase = true) })
        assertTrue("Must extract Machine Learning", extractedSkills.any { it.contains("Machine Learning", ignoreCase = true) })

        val skillsSection = structure.sections.find { it.sectionName == "Skills" }
        assertNotNull(skillsSection)
        assertTrue("Skills section item should preserve all skills without take(6) limit", skillsSection!!.details.size >= 10)
    }

    @Test
    fun testAcceptance4_LocationDetectedFromHeaderInVariousFormats() {
        // Format A: In header line mixed with email and phone
        val resumeA = """
            Kumar V
            kumar.v@example.com, +91 9876543210, Coimbatore, Tamil Nadu
            
            OBJECTIVE
            Passionate software engineer.
            
            EDUCATION
            B.Tech IT | Anna University | 2020 - 2024
        """.trimIndent()
        val structureA = resumeParser.parseResumeStructure(resumeA)
        assertNotNull("Location should be detected from mixed header line", structureA.candidateInfo.location)
        assertTrue("Location should contain Coimbatore", structureA.candidateInfo.location!!.contains("Coimbatore"))

        // Format B: Dedicated location line in header
        val resumeB = """
            Sarah Jenkins
            sarah@example.com | (512) 555-0144
            Austin, TX
            
            SUMMARY
            Cloud Infrastructure Engineer.
            
            EDUCATION
            B.S. Computer Science | UT Austin | 2023
        """.trimIndent()
        val structureB = resumeParser.parseResumeStructure(resumeB)
        assertNotNull("Location should be detected from dedicated line", structureB.candidateInfo.location)
        assertEquals("Austin, TX", structureB.candidateInfo.location)

        // Format C: No location present in resume
        val resumeC = """
            Ghost Candidate
            Email: ghost@example.com | Phone: 9876543210
            
            SUMMARY
            Software Developer.
            
            EDUCATION
            B.S. Computer Science | MIT | 2024
        """.trimIndent()
        val structureC = resumeParser.parseResumeStructure(resumeC)
        assertNull("Missing location must return null (handled as 'Not specified' in UI)", structureC.candidateInfo.location)
    }

    @Test
    fun testAcceptance5_MissingProjects_YieldsEmptyList_NeverFabricates() {
        val resumeWithoutProjects = """
            Ravi Kumar
            Email: ravi@example.com | Phone: 9876543210 | Location: Chennai, India
            
            CAREER OBJECTIVE
            Hardworking computer science graduate eager to apply algorithms, system design, and database knowledge.
            
            TECHNICAL EXPERTISE
            Java, Python, C++, SQL, Git, Linux
            
            ACADEMIC BACKGROUND
            Bachelor of Engineering in Computer Science and Engineering
            College of Engineering Guindy, Anna University
            2021 - 2025 | CGPA: 8.75
            
            Higher Secondary Certificate
            DAV Boys Senior Secondary School
            2021 | Percentage: 93.4%
            
            CERTIFICATIONS
            Oracle Certified Professional: Java SE 11 Developer
            AWS Certified Solutions Architect – Associate
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeWithoutProjects)

        // Verify Projects section is completely empty and NOT fabricated from skills/education/summary/certifications
        assertTrue("Resume without projects must yield empty project list", structure.extractedProjects.isEmpty())
        assertTrue("Project analyses must be empty", structure.projectAnalyses.isEmpty())

        val projectSection = structure.sections.find { it.sectionName == "Projects" }
        assertNotNull(projectSection)
        assertFalse("Projects section must NOT be detected as found", projectSection!!.isFound)
        assertTrue("Projects section items must be empty", projectSection.details.isEmpty())

        // Ensure Certifications are kept strictly in Certifications
        assertEquals(2, structure.certifications.size)
        assertTrue(structure.certifications.any { it.contains("Oracle Certified") })
        assertTrue(structure.certifications.any { it.contains("AWS Certified") })
    }

    @Test
    fun testAcceptance7_MultipleEducationEntries_PreservesAllCompletely() {
        val resumeText = """
            Deepak S
            Email: deepak@example.com | Phone: 9876543210 | Location: Bengaluru, Karnataka
            
            SUMMARY
            Backend Engineer with strong distributed systems foundations.
            
            EDUCATION
            Master of Technology in Computer Science
            Indian Institute of Science, Bangalore
            2023 - 2025
            CGPA: 9.2 / 10.0
            Specialization: Cloud Computing and Distributed Systems
            
            Bachelor of Technology in Information Technology
            National Institute of Technology Karnataka, Surathkal
            2019 - 2023
            CGPA: 8.8 / 10.0
            
            Higher Secondary School Certificate (12th Standard)
            Kendriya Vidyalaya IISc Campus, Bengaluru
            2019
            Percentage: 95.8%
            
            Secondary School Leaving Certificate (10th Standard)
            Kendriya Vidyalaya IISc Campus, Bengaluru
            2017
            Percentage: 97.4%
            
            TECHNICAL SKILLS
            Java, Go, Kubernetes, Docker, PostgreSQL
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify ALL 4 education tiers are preserved completely
        assertEquals("Must extract all 4 education qualifications", 4, structure.education.size)
        assertTrue("Must contain M.Tech details", structure.education.any { it.contains("Master") && it.contains("Indian Institute of Science") })
        assertTrue("Must contain B.Tech details", structure.education.any { it.contains("Bachelor") && it.contains("Surathkal") })
        assertTrue("Must contain 12th details", structure.education.any { it.contains("Higher Secondary") || it.contains("12th") })
        assertTrue("Must contain 10th details", structure.education.any { it.contains("Secondary School") || it.contains("10th") })
    }

    @Test
    fun testAcceptance9_DifferentSectionOrdering_CorrectlyMapped() {
        // Resume where Education comes first, then Projects, then Skills, then Objective at the bottom
        val resumeText = """
            Ananya Sharma
            Email: ananya@example.com | Phone: 9876543210 | Location: Delhi, India
            
            EDUCATION
            B.Tech in Computer Engineering | DTU Delhi | 2020 - 2024 | CGPA: 8.6
            
            KEY PROJECTS
            VoiceAI Smart Assistant | Python, FastSpeech, PyTorch
            - Developed edge-based real-time voice translation pipeline with sub-100ms latency.
            
            SKILLS & TOOLS
            Python, C++, PyTorch, Docker, Git, Linux
            
            CAREER OBJECTIVE
            Driven Machine Learning engineer seeking to build intelligent edge computing systems.
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        assertNotNull("Objective at bottom must be identified as summary", structure.summary)
        assertTrue("Summary contains ML goal", structure.summary!!.contains("Machine Learning engineer"))

        assertEquals(1, structure.education.size)
        assertTrue(structure.education[0].contains("DTU Delhi"))

        assertEquals(1, structure.extractedProjects.size)
        assertEquals("VoiceAI Smart Assistant", structure.extractedProjects[0])

        assertTrue(structure.extractedSkills.size >= 5)
        assertTrue(structure.extractedSkills.contains("Python"))
        assertTrue(structure.extractedSkills.contains("PyTorch"))
    }

    @Test
    fun testCandidateNameExtraction_WithInitials() {
        // Case 1: Tharshika A
        val resume1 = """
            Tharshika A
            tharshika.a@example.com | 9876543210 | Chennai, Tamil Nadu
            
            SUMMARY
            Android developer.
            
            EDUCATION
            B.E. Computer Science | 2025
        """.trimIndent()
        val s1 = resumeParser.parseResumeStructure(resume1)
        assertEquals("Tharshika A", s1.candidateInfo.name)

        // Case 2: THARSHIKA A (all caps in header)
        val resume2 = """
            THARSHIKA A
            tharshika.a@example.com | 9876543210 | Chennai, Tamil Nadu
            
            SUMMARY
            Android developer.
            
            EDUCATION
            B.E. Computer Science | 2025
        """.trimIndent()
        val s2 = resumeParser.parseResumeStructure(resume2)
        assertEquals("Tharshika A", s2.candidateInfo.name)

        // Case 3: A Tharshika
        val resume3 = """
            A Tharshika
            atharshika@example.com | 9876543210 | Coimbatore, India
            
            SUMMARY
            Android developer.
            
            EDUCATION
            B.Tech IT | 2024
        """.trimIndent()
        val s3 = resumeParser.parseResumeStructure(resume3)
        assertEquals("A Tharshika", s3.candidateInfo.name)

        // Case 4: Tharshika A. (with dot)
        val resume4 = """
            Tharshika A.
            tharshika@example.com | 9876543210 | Chennai, India
            
            SUMMARY
            Software engineer.
            
            EDUCATION
            B.E. CSE | 2025
        """.trimIndent()
        val s4 = resumeParser.parseResumeStructure(resume4)
        assertEquals("Tharshika A.", s4.candidateInfo.name)

        // Case 5: S. Priya
        val resume5 = """
            S. Priya
            priya.s@example.com | 9876543210 | Bengaluru, India
            
            SUMMARY
            Java developer.
            
            EDUCATION
            B.Sc CS | 2023
        """.trimIndent()
        val s5 = resumeParser.parseResumeStructure(resume5)
        assertEquals("S. Priya", s5.candidateInfo.name)

        // Case 6: Karthik R
        val resume6 = """
            Karthik R
            karthik.r@example.com | 9876543210 | Hyderabad, India
            
            SUMMARY
            Cloud engineer.
            
            EDUCATION
            B.Tech | 2024
        """.trimIndent()
        val s6 = resumeParser.parseResumeStructure(resume6)
        assertEquals("Karthik R", s6.candidateInfo.name)
    }

    @Test
    fun testCandidateNameResolution_DeterministicAuthoritativeOverAi() {
        fun sanitizeCandidate(value: String?): String? {
            if (value.isNullOrBlank()) return null
            val clean = value.trim()
            val lower = clean.lowercase()
            val invalid = setOf(
                "null", "none", "n/a", "na", "not detected", "not specified",
                "not provided", "not found", "unknown", "nil", "-", "--", "undefined", "empty"
            )
            return if (invalid.contains(lower) || lower.startsWith("not detected") || lower.startsWith("not found")) null else clean
        }

        fun resolveCandidateName(detName: String?, aiName: String?): String {
            return sanitizeCandidate(detName)
                ?: sanitizeCandidate(aiName)
                ?: "Candidate"
        }

        // 1. When deterministic has valid name and AI has merged/corrupted name
        val detName1 = "Tharshika A"
        val aiName1 = "Tharshikaa"
        assertEquals("Tharshika A", resolveCandidateName(detName1, aiName1))

        // 2. R Karthik / RKarthik
        val detName2 = "R Karthik"
        val aiName2 = "RKarthik"
        assertEquals("R Karthik", resolveCandidateName(detName2, aiName2))

        // 3. When deterministic is null or invalid, fallback to AI name
        assertEquals("Alice Bob", resolveCandidateName(null, "Alice Bob"))
        assertEquals("Alice Bob", resolveCandidateName("Not Detected", "Alice Bob"))

        // 4. When both are missing, fallback to "Candidate"
        assertEquals("Candidate", resolveCandidateName(null, null))
        assertEquals("Candidate", resolveCandidateName("N/A", "Unknown"))
    }

    @Test
    fun testCandidateNameExtraction_HeaderDelimitersAndStrictHeaderOnly() {
        // Test comma delimiter
        val resume1 = """
            THARSHIKA A, tharshika@example.com, +91 9876543210, Chennai
            
            EDUCATION
            B.E in Computer Science | Anna University | 2024
        """.trimIndent()
        val s1 = resumeParser.parseResumeStructure(resume1)
        assertEquals("Tharshika A", s1.candidateInfo.name)

        // Test space delimiter
        val resume2 = """
            THARSHIKA A    tharshika2004@gmail.com    9876543210
            
            EDUCATION
            B.Tech in Information Technology | 2024
        """.trimIndent()
        val s2 = resumeParser.parseResumeStructure(resume2)
        assertEquals("Tharshika A", s2.candidateInfo.name)
    }

    @Test
    fun testEducationExtraction_StrictSectionBoundaries_ExcludesNptelAndCourses() {
        val resumeText = """
            THARSHIKA A
            tharshika@example.com | 9876543210 | Chennai
            
            EDUCATION
            B.E. Computer Science and Engineering | XYZ College of Engineering | 2020 - 2024 | CGPA: 8.7
            Class XII (HSC) | ABC Higher Secondary School | 2020 | 92%
            Class X (SSLC) | ABC High School | 2018 | 95%
            
            CERTIFICATIONS
            NPTEL Course in Programming in Java and IoT
            AWS Certified Cloud Practitioner
            
            TECHNICAL SKILLS
            Java, Python, Android, SQL
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify Education contains ONLY formal degrees/schooling
        assertEquals("Education should contain 3 formal academic entries", 3, structure.education.size)
        assertTrue("Education contains B.E.", structure.education.any { it.contains("B.E.") })
        assertTrue("Education contains XII", structure.education.any { it.contains("Class XII") || it.contains("HSC") })
        assertTrue("Education contains X", structure.education.any { it.contains("Class X") || it.contains("SSLC") })
        assertFalse("Education must NOT contain NPTEL course", structure.education.any { it.contains("NPTEL") })

        // Verify Certifications contains NPTEL
        assertEquals("Certifications should contain 2 entries", 2, structure.certifications.size)
        assertTrue("Certifications contains NPTEL", structure.certifications.any { it.contains("NPTEL Course in Programming in Java and IoT") })
        assertTrue("Certifications contains AWS", structure.certifications.any { it.contains("AWS Certified Cloud Practitioner") })
    }

    @Test
    fun testCandidateNameExtraction_RejectsAddressAndLandmarks() {
        val resumeWithAddress = """
            THARSHIKA A
            Address: 14, E.B Office Backside, Tiruchirappalli, Tamil Nadu - 620001
            Email: tharshika2004@gmail.com | Phone: 9876543210
            
            SUMMARY
            Motivated software engineer.
            
            EDUCATION
            B.E. Computer Science | XYZ Engineering College | 2024
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeWithAddress)
        assertEquals("Candidate name must be Tharshika A and not address landmark", "Tharshika A", structure.candidateInfo.name)
    }

    @Test
    fun testProjectExtraction_DoesNotSplitSubheadingsIntoMultipleProjects() {
        val resumeText = """
            THARSHIKA A
            tharshika@example.com | 9876543210
            
            PROJECTS
            Hospital Management System
            Frontend - HTML, CSS, JavaScript
            Backend - Java, Spring Boot
            Database - MySQL
            Tools - Git, Eclipse
            Role - Full Stack Developer
            Description - Developed an online patient record and appointment scheduling system.
            Outcome - Successfully deployed and tested with 50+ concurrent users.

            Smart Agriculture Monitoring System | IoT, Arduino, Python
            - Built automated soil moisture and temperature tracking system using sensors.
            - Integrated alert notification system.
            
            WORK EXPERIENCE
            Software Engineer Intern | ABC Tech Labs
            June 2024 - August 2024
            - Developed backend REST APIs using Kotlin and Spring Boot.
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)

        // Verify exactly 2 projects are extracted, NOT 7+
        assertEquals("Should extract exactly 2 genuine projects", 2, structure.extractedProjects.size)
        assertTrue("Contains Hospital Management System", structure.extractedProjects.contains("Hospital Management System"))
        assertTrue("Contains Smart Agriculture", structure.extractedProjects.any { it.contains("Smart Agriculture") })
        
        // Sub-headings must NOT be standalone projects
        assertFalse("Frontend must not be a project", structure.extractedProjects.contains("Frontend"))
        assertFalse("Backend must not be a project", structure.extractedProjects.contains("Backend"))
        assertFalse("Database must not be a project", structure.extractedProjects.contains("Database"))
        assertFalse("Role must not be a project", structure.extractedProjects.contains("Role"))
        assertFalse("Description must not be a project", structure.extractedProjects.contains("Description"))
        assertFalse("Outcome must not be a project", structure.extractedProjects.contains("Outcome"))

        // Confirm Work Experience is intact and unchanged
        assertEquals("Work experience must contain 1 entry", 1, structure.experience.size)
        assertTrue("Work experience contains Software Engineer Intern", structure.experience[0].contains("Software Engineer Intern"))
    }

    @Test
    fun testCandidateNameExtraction_RejectsSivagangaiAndLocationWords() {
        val resumeWithSivagangai = """
            Sivagangai, Tamil Nadu
            THARSHIKA A
            tharshikaa2005@gmail.com | 7603985003
            
            SUMMARY
            Motivated software engineer.
            
            EDUCATION
            B.E. Computer Science | XYZ Engineering College | 2024
            
            PROJECTS
            Smart Attendance System
            - Built automated attendance tracker using face recognition.
            
            WORK EXPERIENCE
            Software Engineer Intern | ABC Tech Labs
            June 2024 - August 2024
            - Developed backend REST APIs.
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeWithSivagangai)
        assertEquals("Candidate name must be Tharshika A and NOT Sivagangai", "Tharshika A", structure.candidateInfo.name)
        assertEquals("Location should be Sivagangai, Tamil Nadu", "Sivagangai, Tamil Nadu", structure.candidateInfo.location)
        assertEquals("Email should be tharshikaa2005@gmail.com", "tharshikaa2005@gmail.com", structure.candidateInfo.email)
        assertEquals("Phone should be 7603985003", "7603985003", structure.candidateInfo.phone)
    }

    @Test
    fun testCandidateNameExtraction_NeverSelectsBachelorOfEngineeringOrDegrees() {
        // Resume containing Bachelor of Engineering at top / near header
        val resumeText = """
            THARSHIKA A
            tharshikaa2005@gmail.com | +91 7603985003 | Sivagangai, Tamil Nadu
            
            EDUCATION
            Bachelor of Engineering in Computer Science and Engineering
            XYZ College of Engineering, Anna University | 2021 - 2025 | CGPA: 8.8
            Class XII (HSC) | St. Mary Higher Secondary School | 2021 | 94%
            Class X (SSLC) | St. Mary High School | 2019 | 96%
            
            SUMMARY
            Enthusiastic software engineer specializing in Android and mobile architecture.
            
            SKILLS
            Kotlin, Java, Jetpack Compose, Room Database, Git, SQL
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeText)
        
        // 1. Verify candidate's real personal name is extracted
        assertEquals("Candidate name must be Tharshika A", "Tharshika A", structure.candidateInfo.name)
        assertNotEquals("Candidate name must NEVER be Bachelor of Engineering", "Bachelor of Engineering", structure.candidateInfo.name)
        assertFalse("Candidate name must not contain degree text", structure.candidateInfo.name!!.contains("Bachelor", ignoreCase = true))
        assertFalse("Candidate name must not contain engineering", structure.candidateInfo.name!!.contains("Engineering", ignoreCase = true))

        // 2. Verify Bachelor of Engineering remains correctly classified under education
        assertEquals(3, structure.education.size)
        assertTrue("Education must contain Bachelor of Engineering", structure.education.any { it.contains("Bachelor of Engineering") })
    }

    @Test
    fun testCandidateNameExtraction_EducationBeforeNameInHeader_ExtractsRealName() {
        val resumeWithEducationFirst = """
            Bachelor of Technology in Information Technology
            XYZ Institute of Engineering
            Tharshika A
            tharshika@example.com | 9876543210
            
            EDUCATION
            Bachelor of Technology in Information Technology | XYZ Institute | 2025
            
            TECHNICAL SKILLS
            Kotlin, Compose, Coroutines
        """.trimIndent()

        val structure = resumeParser.parseResumeStructure(resumeWithEducationFirst)
        assertEquals("Candidate name must be Tharshika A even if education appears first in text", "Tharshika A", structure.candidateInfo.name)
    }

    @Test
    fun testIsDegreeOrEducationTitle_RejectsAllDegreeVariations() {
        val degreesToReject = listOf(
            "Bachelor of Engineering",
            "Bachelor of Technology",
            "Bachelor of Science",
            "Bachelor of Arts",
            "Bachelor of Computer Applications",
            "Master of Engineering",
            "Master of Technology",
            "Master of Science",
            "Master of Computer Applications",
            "Master of Business Administration",
            "B.E.",
            "B.E",
            "B.Tech",
            "BTech",
            "M.E.",
            "M.Tech",
            "B.Sc",
            "M.Sc",
            "MCA",
            "BCA",
            "MBA",
            "Ph.D.",
            "PhD",
            "Diploma in Computer Engineering",
            "Anna University",
            "XYZ College of Engineering",
            "Software Engineer",
            "Android Developer",
            "Curriculum Vitae",
            "Resume"
        )

        for (deg in degreesToReject) {
            assertTrue("'$deg' must be identified as degree/education/invalid title", ResumeParser.isDegreeOrEducationTitle(deg))
        }

        val validNames = listOf(
            "Tharshika A",
            "THARSHIKA A",
            "A Tharshika",
            "Tharshika A.",
            "S. Priya",
            "Karthik R",
            "K. R. Karthik",
            "Alex Chen",
            "John Doe",
            "Mary Jane Watson"
        )

        for (name in validNames) {
            assertFalse("'$name' must NOT be classified as degree title", ResumeParser.isDegreeOrEducationTitle(name))
        }
    }

    @Test
    fun testTechnicalMcq_OneAttemptPerAnalysis_PersistenceAndIsolation() {
        val sampleQuestions = listOf(
            TechnicalMcq(
                id = "q1",
                question = "What is the primary purpose of Jetpack Compose?",
                options = listOf("Declarative UI", "Database management", "Network calls", "Dependency injection"),
                correctAnswerIndex = 0,
                explanation = "Jetpack Compose is Android's modern toolkit for building declarative native UI.",
                topic = "Android UI",
                difficulty = "Easy"
            ),
            TechnicalMcq(
                id = "q2",
                question = "Which Kotlin keyword is used for coroutine builders?",
                options = listOf("launch", "execute", "runLater", "defer"),
                correctAnswerIndex = 0,
                explanation = "launch is a coroutine builder in Kotlin.",
                topic = "Kotlin Coroutines",
                difficulty = "Medium"
            )
        )

        // Analysis A - Initially unattempted
        val analysisA = ResumeAnalysisResult(
            id = "analysis-A",
            overallScore = 85,
            atsScore = 85,
            skillMatch = 80,
            targetRole = "Android Developer",
            jdText = "Looking for an Android Developer with Compose and Coroutines skills.",
            technicalMcqResult = null
        )
        assertNull("Analysis A quiz must initially be unattempted", analysisA.technicalMcqResult)

        // Attempt Quiz for Analysis A
        val userAnswersA = listOf(0, 0) // Both correct
        var correctCountA = 0
        val improvementAreasA = mutableSetOf<String>()
        sampleQuestions.forEachIndexed { index, q ->
            if (userAnswersA[index] == q.correctAnswerIndex) {
                correctCountA++
            } else {
                improvementAreasA.add(q.topic)
            }
        }
        val resultA = TechnicalMcqResult(
            questions = sampleQuestions,
            userAnswers = userAnswersA,
            correctCount = correctCountA,
            incorrectCount = sampleQuestions.size - correctCountA,
            improvementAreas = improvementAreasA.toList()
        )

        val completedAnalysisA = analysisA.copy(technicalMcqResult = resultA)
        assertNotNull("Analysis A must have technicalMcqResult after submission", completedAnalysisA.technicalMcqResult)
        assertEquals(2, completedAnalysisA.technicalMcqResult!!.correctCount)
        assertEquals(0, completedAnalysisA.technicalMcqResult!!.incorrectCount)

        // Reopening Analysis A: Result must be preserved permanently without reattempting
        val reopenedAnalysisA = completedAnalysisA
        assertNotNull("Reopening Analysis A must show existing result", reopenedAnalysisA.technicalMcqResult)
        assertEquals(2, reopenedAnalysisA.technicalMcqResult!!.correctCount)

        // Analysis B - New Independent JD Matcher Analysis
        val analysisB = ResumeAnalysisResult(
            id = "analysis-B",
            overallScore = 75,
            atsScore = 75,
            skillMatch = 70,
            targetRole = "Cloud Architect",
            jdText = "Looking for a Cloud Architect with AWS and Docker skills.",
            technicalMcqResult = null
        )
        assertNull("Analysis B must have its own fresh unattempted quiz independently of Analysis A", analysisB.technicalMcqResult)

        // Verify Analysis A still has its original result unchanged
        assertEquals(2, completedAnalysisA.technicalMcqResult!!.correctCount)
    }

    @Test
    fun testTechnicalMcq_DuplicateSubmissionProtection() {
        val sampleQuestions = listOf(
            TechnicalMcq(
                id = "q1",
                question = "What is Kotlin?",
                options = listOf("Programming language", "Database", "Browser", "Operating system"),
                correctAnswerIndex = 0,
                explanation = "Kotlin is a modern statically typed programming language.",
                topic = "Kotlin",
                difficulty = "Easy"
            )
        )

        val analysis = ResumeAnalysisResult(
            id = "test-analysis-dup",
            overallScore = 90,
            atsScore = 90,
            skillMatch = 85,
            targetRole = "Android Developer",
            technicalMcqResult = null
        )

        val firstSubmissionResult = TechnicalMcqResult(
            questions = sampleQuestions,
            userAnswers = listOf(0),
            correctCount = 1,
            incorrectCount = 0,
            improvementAreas = emptyList()
        )

        val submittedAnalysis = analysis.copy(technicalMcqResult = firstSubmissionResult)
        assertNotNull(submittedAnalysis.technicalMcqResult)

        // Duplicate submission attempt: if technicalMcqResult is already present, it should not be overwritten
        val isAlreadyAttempted = submittedAnalysis.technicalMcqResult != null
        assertTrue("Duplicate submission must detect already attempted state", isAlreadyAttempted)
        assertEquals("Existing result must be retained", 1, submittedAnalysis.technicalMcqResult!!.correctCount)
    }
}






