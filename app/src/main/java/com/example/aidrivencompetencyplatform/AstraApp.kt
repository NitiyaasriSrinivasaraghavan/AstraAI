package com.example.aidrivencompetencyplatform

import android.app.Application
import com.example.aidrivencompetencyplatform.data.GeminiService
import com.example.aidrivencompetencyplatform.data.PreferenceManager
import com.example.aidrivencompetencyplatform.data.ResumeParser
import com.example.aidrivencompetencyplatform.data.SessionManager

class AstraApp : Application() {
    lateinit var sessionManager: SessionManager
    lateinit var preferenceManager: PreferenceManager
    lateinit var resumeParser: ResumeParser
    lateinit var geminiService: GeminiService

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)
        preferenceManager = PreferenceManager(this)
        resumeParser = ResumeParser(this)
        geminiService = GeminiService()
    }

    companion object {
        lateinit var instance: AstraApp
            private set
    }
}
