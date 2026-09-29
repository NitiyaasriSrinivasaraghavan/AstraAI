package com.example.aidrivencompetencyplatform

import android.app.Application
import com.example.aidrivencompetencyplatform.data.BackendRepository
import com.example.aidrivencompetencyplatform.data.GeminiService
import com.example.aidrivencompetencyplatform.data.InterviewService
import com.example.aidrivencompetencyplatform.data.PreferenceManager
import com.example.aidrivencompetencyplatform.data.ResumeParser
import com.example.aidrivencompetencyplatform.data.SessionManager

class AstraApp : Application() {
    lateinit var backendRepository: BackendRepository
    lateinit var sessionManager: SessionManager
    lateinit var preferenceManager: PreferenceManager
    lateinit var resumeParser: ResumeParser
    lateinit var geminiService: GeminiService
    lateinit var interviewService: InterviewService

    override fun onCreate() {
        super.onCreate()
        instance = this
        backendRepository = BackendRepository()
        sessionManager = SessionManager(this, backendRepository)
        preferenceManager = PreferenceManager(this)
        resumeParser = ResumeParser(this)
        geminiService = GeminiService()
        interviewService = InterviewService(geminiService)
    }

    companion object {
        lateinit var instance: AstraApp
            private set
    }
}
