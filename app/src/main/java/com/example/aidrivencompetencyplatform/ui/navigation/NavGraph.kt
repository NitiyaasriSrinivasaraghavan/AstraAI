package com.example.aidrivencompetencyplatform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.ui.screens.*
import com.example.aidrivencompetencyplatform.viewmodel.*

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val factory = AppViewModelFactory(app)

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
            val resumeViewModel: ResumeViewModel = viewModel(factory = factory)
            MainDashboardScreen(navController, dashboardViewModel, resumeViewModel)
        }
        composable(Screen.ResumeUpload.route) {
            val viewModel: ResumeViewModel = viewModel(factory = factory)
            ResumeUploadScreen(navController, viewModel)
        }
        composable(Screen.AtsAnalysis.route) {
            val viewModel: AtsViewModel = viewModel(factory = factory)
            AtsDashboardScreen(navController, viewModel)
        }
        composable(Screen.SkillGap.route) {
            val viewModel: SkillGapViewModel = viewModel(factory = factory)
            SkillGapDashboardScreen(navController, viewModel)
        }
        composable(Screen.AiAssistant.route) {
            val viewModel: AiAssistantViewModel = viewModel(factory = factory)
            AiAssistantScreen(navController, viewModel)
        }
        composable(Screen.Settings.route) {
            StubScreen("Settings", navController)
        }
        composable(Screen.Profile.route) {
            StubScreen("Profile", navController)
        }
        composable(Screen.InterviewPrep.route) {
            val viewModel: AiAssistantViewModel = viewModel(factory = factory)
            InterviewPrepScreen(navController, viewModel)
        }
        composable(Screen.JobDescriptionAnalyzer.route) {
            val viewModel: ResumeViewModel = viewModel(factory = factory)
            JobDescriptionAnalyzerScreen(navController, viewModel)
        }
    }
}
