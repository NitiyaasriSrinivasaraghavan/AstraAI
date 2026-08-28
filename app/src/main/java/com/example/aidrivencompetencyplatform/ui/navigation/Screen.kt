package com.example.aidrivencompetencyplatform.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object AppTour : Screen("app_tour")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Dashboard : Screen("dashboard")
    object ResumeUpload : Screen("resume_upload")
    object ResumeAnalysis : Screen("resume_analysis")
    object AtsAnalysis : Screen("ats_analysis")
    object SkillGap : Screen("skill_gap")
    object InterviewPrep : Screen("interview_prep")
    object AiAssistant : Screen("ai_assistant")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object JobDescriptionAnalyzer : Screen("job_description_analyzer")
}
