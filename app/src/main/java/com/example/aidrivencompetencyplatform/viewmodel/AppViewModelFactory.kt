package com.example.aidrivencompetencyplatform.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aidrivencompetencyplatform.AstraApp

class AppViewModelFactory(private val app: AstraApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> 
                DashboardViewModel(app.sessionManager, app.preferenceManager) as T
            modelClass.isAssignableFrom(AtsViewModel::class.java) -> 
                AtsViewModel(app.sessionManager) as T
            modelClass.isAssignableFrom(SkillGapViewModel::class.java) -> 
                SkillGapViewModel(app.sessionManager) as T
            modelClass.isAssignableFrom(ResumeViewModel::class.java) -> 
                ResumeViewModel(app.sessionManager, app.resumeParser, app.geminiService) as T
            modelClass.isAssignableFrom(AiAssistantViewModel::class.java) -> 
                AiAssistantViewModel(app.sessionManager, app.geminiService) as T
            modelClass.isAssignableFrom(JdMatcherViewModel::class.java) -> 
                JdMatcherViewModel(app.sessionManager, app.geminiService, app.resumeParser) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
