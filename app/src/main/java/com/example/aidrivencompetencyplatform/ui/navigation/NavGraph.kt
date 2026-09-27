package com.example.aidrivencompetencyplatform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.ui.screens.*
import com.example.aidrivencompetencyplatform.viewmodel.*

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val factory = AppViewModelFactory(app)

    // Shared ResumeViewModel to ensure data consistency across Hub and Upload screen
    val sharedResumeViewModel: ResumeViewModel = viewModel(factory = factory)
    
    // Shared AtsViewModel for detailed extraction views
    val sharedAtsViewModel: AtsViewModel = viewModel(factory = factory)

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.AppTour.route) {
            val assistantViewModel: AiAssistantViewModel = viewModel(factory = factory)
            val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
            AppTourScreen(navController, assistantViewModel, dashboardViewModel)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Signup.route) {
            SignupScreen(navController)
        }
        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
            MainDashboardScreen(navController, dashboardViewModel, sharedResumeViewModel)
        }
        composable(Screen.ResumeUpload.route) {
            ResumeUploadScreen(navController, sharedResumeViewModel)
        }
        composable(
            route = Screen.AtsAnalysis.route + "?analysisId={analysisId}",
            arguments = listOf(
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            LaunchedEffect(analysisId) {
                sharedAtsViewModel.loadAnalysis(analysisId)
            }
            AtsDashboardScreen(navController, sharedAtsViewModel, analysisId)
        }
        composable(
            route = Screen.SkillGap.route + "?analysisId={analysisId}",
            arguments = listOf(
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            val viewModel: SkillGapViewModel = viewModel(factory = factory)
            LaunchedEffect(analysisId) {
                viewModel.loadAnalysis(analysisId)
            }
            SkillGapDashboardScreen(navController, viewModel, analysisId)
        }
        composable(
            route = Screen.AiAssistant.route + "?mode={mode}&analysisId={analysisId}",
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode")
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            val viewModel: AiAssistantViewModel = viewModel(factory = factory)
            LaunchedEffect(mode, analysisId) {
                if (mode == "INTERVIEW_PREPARATION" || mode == "interview") {
                    val targetRoute = Screen.InterviewPrep.route + (if (!analysisId.isNullOrBlank()) "?analysisId=$analysisId" else "")
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.AiAssistant.route) { inclusive = true }
                    }
                }
            }
            AiAssistantScreen(navController, viewModel)
        }
        composable(Screen.Settings.route) {
            StubScreen("Settings", navController)
        }
        composable(Screen.Profile.route) {
            StubScreen("Profile", navController)
        }
        composable(
            route = Screen.InterviewPrep.route + "?analysisId={analysisId}",
            arguments = listOf(
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            val viewModel: InterviewViewModel = viewModel(factory = factory)
            LaunchedEffect(analysisId) {
                if (!analysisId.isNullOrBlank()) {
                    viewModel.loadHistoryAnalysis(analysisId)
                } else {
                    viewModel.startInterview()
                }
            }
            InterviewPrepScreen(navController, viewModel)
        }
        composable(
            route = Screen.TechnicalMcq.route + "?analysisId={analysisId}",
            arguments = listOf(
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            val viewModel: TechnicalMcqViewModel = viewModel(factory = factory)
            TechnicalMcqScreen(navController, viewModel, analysisId)
        }
        composable(
            route = Screen.TechnicalMcqResult.route + "?analysisId={analysisId}",
            arguments = listOf(
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            val viewModel: TechnicalMcqViewModel = viewModel(factory = factory)
            TechnicalMcqResultScreen(navController, viewModel, analysisId)
        }
        composable(
            route = Screen.JobDescriptionAnalyzer.route + "?analysisId={analysisId}",
            arguments = listOf(
                navArgument("analysisId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId")
            val viewModel: JdMatcherViewModel = viewModel(factory = factory)
            LaunchedEffect(analysisId) {
                viewModel.loadAnalysis(analysisId)
            }
            JobDescriptionAnalyzerScreen(navController, viewModel)
        }
        composable(Screen.SummaryDetail.route) {
            ResumeSectionDetailScreen("Summary", navController, sharedAtsViewModel)
        }
        composable(Screen.ExperienceDetail.route) {
            ResumeSectionDetailScreen("Work Experience", navController, sharedAtsViewModel)
        }
        composable(Screen.EducationDetail.route) {
            ResumeSectionDetailScreen("Education", navController, sharedAtsViewModel)
        }
        composable(Screen.SkillsDetail.route) {
            ResumeSectionDetailScreen("Skills", navController, sharedAtsViewModel)
        }
        composable(Screen.ProjectsDetail.route) {
            ResumeSectionDetailScreen("Projects", navController, sharedAtsViewModel)
        }
        composable(Screen.KeyboardTest.route) {
            KeyboardTestScreen(navController)
        }
    }
}
