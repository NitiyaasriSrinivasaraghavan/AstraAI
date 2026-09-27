package com.example.aidrivencompetencyplatform.data

import android.util.Log
import com.example.aidrivencompetencyplatform.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InterviewService(
    private val geminiService: GeminiService
) {

    fun generateWelcomeMessage(targetRole: String, candidateName: String? = null): String {
        val namePrefix = if (!candidateName.isNullOrBlank()) ", $candidateName" else ""
        return "Welcome to your mock interview for the $targetRole role$namePrefix! I'll ask you a series of questions based on your profile and target role. Take your time with each answer. Shall we begin?"
    }

    suspend fun generateFirstQuestion(
        targetRole: String,
        candidateProfile: ResumeAnalysisResult?
    ): InterviewQuestion = withContext(Dispatchers.IO) {
        val skills = candidateProfile?.extractedSkills?.map { it.name } ?: emptyList()
        val projects = candidateProfile?.projectAnalysis?.map { it.name } ?: emptyList()
        val experience = candidateProfile?.experience ?: emptyList()
        val education = candidateProfile?.education ?: emptyList()
        val certifications = candidateProfile?.certifications ?: emptyList()

        // Attempt dynamic generation via AI if available
        try {
            if (candidateProfile != null) {
                val questions = geminiService.generateInterviewQuestions(
                    targetRole = targetRole,
                    resumeData = candidateProfile,
                    jobDescription = candidateProfile.jdText
                )
                if (questions.isNotEmpty()) {
                    return@withContext questions.first()
                }
            }
        } catch (e: Exception) {
            Log.w("InterviewService", "AI generation fallback: ${e.localizedMessage}")
        }

        // Graceful dynamic fallback based on available resume data or general role
        createGroundedFallbackQuestion(
            targetRole = targetRole,
            skills = skills,
            projects = projects,
            experience = experience,
            education = education,
            certifications = certifications,
            questionIndex = 1
        )
    }

    suspend fun generateNextQuestion(
        targetRole: String,
        candidateProfile: ResumeAnalysisResult?,
        questionNumber: Int,
        previousQuestion: InterviewQuestion?,
        candidateAnswer: String
    ): InterviewQuestion = withContext(Dispatchers.IO) {
        val skills = candidateProfile?.extractedSkills?.map { it.name } ?: emptyList()
        val projects = candidateProfile?.projectAnalysis?.map { it.name } ?: emptyList()
        val experience = candidateProfile?.experience ?: emptyList()
        val education = candidateProfile?.education ?: emptyList()
        val certifications = candidateProfile?.certifications ?: emptyList()

        try {
            if (candidateProfile != null) {
                val questions = geminiService.generateInterviewQuestions(
                    targetRole = targetRole,
                    resumeData = candidateProfile,
                    jobDescription = candidateProfile.jdText
                )
                val safeIndex = (questionNumber - 1).coerceIn(0, questions.lastIndex)
                if (questions.isNotEmpty()) {
                    return@withContext questions[safeIndex]
                }
            }
        } catch (e: Exception) {
            Log.w("InterviewService", "AI generation next question fallback: ${e.localizedMessage}")
        }

        createGroundedFallbackQuestion(
            targetRole = targetRole,
            skills = skills,
            projects = projects,
            experience = experience,
            education = education,
            certifications = certifications,
            questionIndex = questionNumber
        )
    }

    private fun createGroundedFallbackQuestion(
        targetRole: String,
        skills: List<String>,
        projects: List<String>,
        experience: List<String>,
        education: List<String>,
        certifications: List<String>,
        questionIndex: Int
    ): InterviewQuestion {
        val topSkill = skills.firstOrNull() ?: "core engineering principles"
        val secondSkill = skills.drop(1).firstOrNull() ?: "problem solving"
        val topProject = projects.firstOrNull()

        return when (questionIndex) {
            1 -> {
                if (topProject != null && skills.isNotEmpty()) {
                    InterviewQuestion(
                        text = "I see from your background that you worked on '$topProject' and have experience with $topSkill. Could you walk me through your technical approach and how you applied $topSkill to solve key challenges?",
                        type = InterviewQuestionType.PROJECT_BASED,
                        category = "Technical & Project Experience",
                        modelGuidance = "Highlight the problem statement, architecture decisions, and measurable outcomes using $topSkill."
                    )
                } else if (skills.isNotEmpty()) {
                    InterviewQuestion(
                        text = "To start our discussion, could you explain your experience with $topSkill and $secondSkill, and how you apply them when architecting solutions for a $targetRole position?",
                        type = InterviewQuestionType.TECHNICAL,
                        category = "Core Competency",
                        modelGuidance = "Explain practical real-world usage, design patterns, and debugging strategies."
                    )
                } else {
                    InterviewQuestion(
                        text = "To start our discussion, could you introduce your technical background and what specific strengths you bring to a $targetRole role?",
                        type = InterviewQuestionType.TECHNICAL,
                        category = "Role Overview",
                        modelGuidance = "Provide a structured 2-minute elevator pitch covering your technical background, core stack, and relevant achievements."
                    )
                }
            }
            2 -> {
                if (skills.size >= 2) {
                    InterviewQuestion(
                        text = "How do you ensure performance, scalability, and code quality when working with $topSkill and related tools?",
                        type = InterviewQuestionType.TECHNICAL,
                        category = "Performance & Quality",
                        modelGuidance = "Discuss profiling, optimization techniques, automated testing, and clean architecture."
                    )
                } else {
                    InterviewQuestion(
                        text = "Could you describe a challenging technical bug or architecture problem you encountered in a recent project and how you diagnosed and resolved it?",
                        type = InterviewQuestionType.SITUATIONAL,
                        category = "Problem Solving",
                        modelGuidance = "Use the STAR method (Situation, Task, Action, Result) emphasizing troubleshooting methodology."
                    )
                }
            }
            else -> {
                InterviewQuestion(
                    text = "In a collaborative team environment, how do you handle shifting requirements or tight deadlines while maintaining high engineering standards for $targetRole deliverables?",
                    type = InterviewQuestionType.BEHAVIORAL,
                    category = "Collaboration & Delivery",
                    modelGuidance = "Emphasize clear communication, prioritization, and proactive stakeholder management."
                )
            }
        }
    }
}
