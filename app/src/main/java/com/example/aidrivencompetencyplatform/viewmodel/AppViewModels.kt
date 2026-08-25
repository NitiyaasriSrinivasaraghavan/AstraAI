package com.example.aidrivencompetencyplatform.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidrivencompetencyplatform.data.GeminiService
import com.example.aidrivencompetencyplatform.data.PreferenceManager
import com.example.aidrivencompetencyplatform.data.ResumeParser
import com.example.aidrivencompetencyplatform.data.SessionManager
import com.example.aidrivencompetencyplatform.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "ResumeAnalysisFlow"

class DashboardViewModel(
    private val sessionManager: SessionManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _greetingPrefix = MutableStateFlow("Welcome")
    val greetingPrefix: StateFlow<String> = _greetingPrefix

    private val _hasAnalysis = MutableStateFlow(false)
    val hasAnalysis: StateFlow<Boolean> = _hasAnalysis

    private val _resumeScore = MutableStateFlow<Int?>(null)
    val resumeScore: StateFlow<Int?> = _resumeScore

    private val _atsScore = MutableStateFlow<Int?>(null)
    val atsScore: StateFlow<Int?> = _atsScore

    private val _skillMatch = MutableStateFlow<Int?>(null)
    val skillMatch: StateFlow<Int?> = _skillMatch

    private val _jobRecommendations = MutableStateFlow(emptyList<JobRecommendation>())
    val jobRecommendations: StateFlow<List<JobRecommendation>> = _jobRecommendations

    private val _hasCompletedTour = MutableStateFlow(true)
    val hasCompletedTour: StateFlow<Boolean> = _hasCompletedTour.asStateFlow()

    init {
        refreshUser()
        checkTourStatus()
    }

    private fun checkTourStatus() {
        viewModelScope.launch {
            preferenceManager.hasCompletedAppTour.collectLatest { completed ->
                _hasCompletedTour.value = completed
            }
        }
    }

    fun completeTour() {
        viewModelScope.launch {
            preferenceManager.setHasCompletedAppTour(true)
        }
    }

    fun refreshUser() {
        val name = sessionManager.getUserName()
        _userName.value = name ?: ""
        
        _greetingPrefix.value = if (sessionManager.isNewUser()) "Welcome" else "Welcome back"

        val analysis = sessionManager.getLatestAnalysis()
        if (analysis != null) {
            _hasAnalysis.value = true
            _resumeScore.value = analysis.overallScore
            _atsScore.value = analysis.atsScore
            _skillMatch.value = analysis.skillMatch
            
            // Fix NPE by using safe calls. Gson can set fields to null if missing in JSON.
            val extracted = analysis.extractedSkills ?: emptyList()
            val missing = analysis.missingSections ?: emptyList()
            
            val matchingSkills = extracted.map { it.name }.take(3)
            val missingItems = missing.take(1)

            _jobRecommendations.value = listOf(
                JobRecommendation(
                    sessionManager.getTargetRole() ?: "Target Role",
                    analysis.skillMatch,
                    "Matches based on extracted skills.",
                    matchingSkills,
                    missingItems
                )
            )
        } else {
            _hasAnalysis.value = false
            _resumeScore.value = null
            _atsScore.value = null
            _skillMatch.value = null
            _jobRecommendations.value = emptyList()
        }
    }
}

class AtsViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _analysisResult = MutableStateFlow<ResumeAnalysisResult?>(sessionManager.getLatestAnalysis())
    val analysisResult: StateFlow<ResumeAnalysisResult?> = _analysisResult

    private val _atsBreakdownState = MutableStateFlow(AtsBreakdown())
    val atsBreakdownState: StateFlow<AtsBreakdown> = _atsBreakdownState.asStateFlow()

    private val _overallAtsScore = MutableStateFlow(0)
    val overallAtsScore: StateFlow<Int> = _overallAtsScore.asStateFlow()

    val targetRole: String
        get() = sessionManager.getTargetRole() ?: "Not Specified"

    init {
        calculateAtsScores()
    }

    fun refresh() {
        _analysisResult.value = sessionManager.getLatestAnalysis()
        calculateAtsScores()
    }

    private fun calculateAtsScores() {
        val analysis = _analysisResult.value ?: return
        
        // 1. Parsing Accuracy (20%): Based on ParsingAccuracyDetails
        val parsingDetails = analysis.parsingAccuracyDetails
        val parsingScore = if (parsingDetails != null) {
            val checks = listOf(
                parsingDetails.nameDetected,
                parsingDetails.contactDetected,
                parsingDetails.educationDetected,
                parsingDetails.skillsDetected,
                parsingDetails.experienceDetected,
                parsingDetails.projectsDetected
            )
            (checks.count { it }.toFloat() / checks.size * 100).toInt()
        } else analysis.atsBreakdown?.parsingAccuracy ?: 0

        // 2. Resume Structure (25%): Based on section presence
        val missing = analysis.missingSections ?: emptyList()
        val totalExpected = 6.0f
        val structureScore = ((totalExpected - missing.size.coerceAtMost(totalExpected.toInt())) / totalExpected * 100).toInt()

        // 3. Formatting Safety (20%): Based on risks found
        val risks = analysis.formattingRisks ?: emptyList()
        val formattingScore = (100 - (risks.size * 15)).coerceIn(0, 100)

        // 4. Keyword Coverage (35%): AI derived but used deterministically
        val keywordScore = analysis.atsBreakdown?.keywordCoverage ?: 0

        val breakdown = AtsBreakdown(
            keywordCoverage = keywordScore,
            resumeStructure = structureScore,
            formattingSafety = formattingScore,
            parsingAccuracy = parsingScore
        )
        
        _atsBreakdownState.value = breakdown
        
        // Deterministic Score = Keyword(35%) + Structure(25%) + Formatting(20%) + Parsing(20%)
        val total = (keywordScore * 0.35 + structureScore * 0.25 + formattingScore * 0.20 + parsingScore * 0.20).toInt()
        _overallAtsScore.value = total
    }
}

class SkillGapViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _analysisResult = MutableStateFlow<ResumeAnalysisResult?>(sessionManager.getLatestAnalysis())
    val analysisResult: StateFlow<ResumeAnalysisResult?> = _analysisResult

    fun refresh() {
        _analysisResult.value = sessionManager.getLatestAnalysis()
    }
}

class ResumeViewModel(
    private val sessionManager: SessionManager,
    private val resumeParser: ResumeParser,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _analysisResult = MutableStateFlow<ResumeAnalysisResult?>(null)
    val analysisResult: StateFlow<ResumeAnalysisResult?> = _analysisResult

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loadingStage = MutableStateFlow("")
    val loadingStage: StateFlow<String> = _loadingStage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var selectedUri: Uri? = null

    fun onFileSelected(uri: Uri, name: String) {
        Log.d(TAG, "RESUME_URI_RECEIVED: $uri, FileName: $name")
        selectedUri = uri
        _selectedFileName.value = name
        _errorMessage.value = null
    }

    fun analyzeResume(targetRole: String) {
        Log.d(TAG, "ANALYZE_BUTTON_CLICKED: TargetRole: $targetRole")
        
        val uri = selectedUri ?: run {
            Log.e(TAG, "ANALYSIS_FAILED: Null URI")
            _errorMessage.value = "Please select a resume first."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _loadingStage.value = "Parsing Resume..."
                Log.d(TAG, "RESUME_PARSING_STARTED")
                val text = resumeParser.extractText(uri)
                
                if (text == ResumeParser.ERROR_SCANNED_PDF) {
                    Log.e(TAG, "ANALYSIS_FAILED: Scanned PDF detected")
                    _errorMessage.value = "This PDF appears to be image-based. Text extraction is not available for this file yet."
                } else if (text.isNotBlank()) {
                    Log.d(TAG, "RESUME_TEXT_EXTRACTED: Length: ${text.length}")
                    
                    _loadingStage.value = "Analyzing with AstraAI..."
                    try {
                        Log.d(TAG, "GEMINI_ANALYSIS_STARTED")
                        val result = geminiService.analyzeResume(text, targetRole)
                        
                        if (result != null) {
                            _loadingStage.value = "Finalizing Results..."
                            Log.d(TAG, "GEMINI_RESPONSE_RECEIVED")
                            _analysisResult.value = result
                            sessionManager.saveLatestAnalysis(result)
                            sessionManager.updateTargetRole(targetRole)
                            Log.d(TAG, "ANALYSIS_COMPLETED")
                        } else {
                            Log.e(TAG, "ANALYSIS_FAILED: Gemini returned null")
                            _errorMessage.value = "AI analysis returned no results. Please try again."
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ANALYSIS_FAILED: Gemini error", e)
                        _errorMessage.value = e.message ?: "AI analysis failed unexpectedly."
                    }
                } else {
                    Log.e(TAG, "ANALYSIS_FAILED: Empty extracted text")
                    _errorMessage.value = "Could not extract text from the selected file. Ensure it's not empty or protected."
                }
            } catch (e: Exception) {
                Log.e(TAG, "ANALYSIS_FAILED: General error", e)
                _errorMessage.value = "An error occurred: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _loadingStage.value = ""
            }
        }
    }

    fun reset() {
        selectedUri = null
        _selectedFileName.value = null
        _analysisResult.value = null
        _errorMessage.value = null
        _loadingStage.value = ""
    }
}

class AiAssistantViewModel(
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _messages = MutableStateFlow(listOf(
        ChatMessage("Hello! I'm Astra, your AI Career Assistant. How can I help you today?", false)
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isInterviewMode = MutableStateFlow(false)
    val isInterviewMode: StateFlow<Boolean> = _isInterviewMode

    fun startInterview() {
        _isInterviewMode.value = true
        _messages.value = listOf(ChatMessage("Welcome to your mock interview. Let's start with a basic question: Tell me about yourself and your experience with Kotlin.", false))
    }

    fun sendMessage(text: String, screenContext: String = "") {
        val userMsg = ChatMessage(text, true)
        _messages.value = _messages.value + userMsg
        
        viewModelScope.launch {
            val analysis = sessionManager.getLatestAnalysis()
            val context = "Screen: $screenContext. User name: ${sessionManager.getUserName()}. Analysis: ${if (analysis != null) "Available" else "Not available"}"
            val aiResponse = geminiService.getAiAssistantResponse(text, context)
            _messages.value = _messages.value + ChatMessage(aiResponse, false)
        }
    }
}
