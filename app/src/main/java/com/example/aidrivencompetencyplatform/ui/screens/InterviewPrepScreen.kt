package com.example.aidrivencompetencyplatform.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.model.NovaState
import com.example.aidrivencompetencyplatform.ui.components.NovaAvatar
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import com.example.aidrivencompetencyplatform.ui.theme.*
import com.example.aidrivencompetencyplatform.viewmodel.InterviewViewModel
import com.example.aidrivencompetencyplatform.voice.VoiceSpeechManager

enum class InterviewVoicePhase {
    IDLE, NOVA_SPEAKING, READY_TO_ANSWER, USER_LISTENING, THINKING, COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewPrepScreen(
    navController: NavController,
    viewModel: InterviewViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    val voiceManager = remember { VoiceSpeechManager(context) }
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val novaState by voiceManager.novaState.collectAsState()

    var userSpokenText by remember { mutableStateOf("") }
    var interviewPhase by remember { mutableStateOf(InterviewVoicePhase.IDLE) }

    DisposableEffect(Unit) {
        viewModel.startInterview()
        onDispose {
            voiceManager.release()
        }
    }

    // Handle question changes and TTS speaking
    LaunchedEffect(state.currentQuestionIndex, state.questions) {
        if (state.questions.isNotEmpty() && !state.isComplete) {
            val q = state.questions.getOrNull(state.currentQuestionIndex)
            if (q != null) {
                interviewPhase = InterviewVoicePhase.NOVA_SPEAKING
                voiceManager.setNovaState(NovaState.SPEAKING)
                val introText = if (state.currentQuestionIndex == 0) {
                    "Welcome to your mock interview for the ${state.targetRole} role. Let's begin. Question 1: ${q.text}"
                } else {
                    "Question ${state.currentQuestionIndex + 1}: ${q.text}"
                }
                voiceManager.speak(introText, "question_${state.currentQuestionIndex}")
            }
        } else if (state.isComplete) {
            interviewPhase = InterviewVoicePhase.COMPLETED
            voiceManager.setNovaState(NovaState.ENCOURAGING)
            voiceManager.speak("Fantastic job! Your mock interview is complete. Let's review your performance summary.", "completion")
        }
    }

    // When TTS finishes speaking, transition to READY_TO_ANSWER
    LaunchedEffect(isSpeaking) {
        if (!isSpeaking && interviewPhase == InterviewVoicePhase.NOVA_SPEAKING) {
            interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
            voiceManager.setNovaState(NovaState.IDLE)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.stopSpeaking()
            interviewPhase = InterviewVoicePhase.USER_LISTENING
            voiceManager.setNovaState(NovaState.LISTENING)
            userSpokenText = ""
            voiceManager.startListening(
                onPartialResult = { spoken ->
                    userSpokenText = spoken
                },
                onFinalResult = { finalSpoken ->
                    userSpokenText = finalSpoken
                    interviewPhase = InterviewVoicePhase.THINKING
                    voiceManager.setNovaState(NovaState.THINKING)
                    viewModel.submitAnswer(finalSpoken)
                },
                onErrorCallback = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
                    voiceManager.setNovaState(NovaState.IDLE)
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for voice interview.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nova Voice Interview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Role: ${state.targetRole}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route)
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceVariant.copy(alpha = 0.4f), Background)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading && state.questions.isEmpty()) {
                LoadingInterviewState()
            } else if (state.isComplete) {
                InterviewFinalResultView(
                    summary = state.summary,
                    onGoToCourses = {
                        navController.navigate(Screen.SkillGap.route)
                    },
                    onRestart = {
                        viewModel.reset()
                        viewModel.startInterview()
                    }
                )
            } else {
                val currentQ = state.questions.getOrNull(state.currentQuestionIndex)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftGreen
                        ) {
                            Text(
                                text = "Question ${state.currentQuestionIndex + 1} of ${state.questions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = SurfaceVariant
                        ) {
                            IconButton(onClick = { viewModel.startInterview() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Central Character Focus: Nova
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(130.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(SurfaceVariant)
                                .border(
                                    width = 3.dp,
                                    color = when (novaState) {
                                        NovaState.SPEAKING -> Primary
                                        NovaState.LISTENING -> ErrorRed
                                        NovaState.THINKING -> WarningAmber
                                        else -> BorderColorGreen
                                    },
                                    shape = CircleShape
                                )
                        ) {
                            NovaAvatar(size = 120.dp, elevation = 0.dp, showBorder = false)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // State Status Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when (novaState) {
                                NovaState.SPEAKING -> SoftGreen
                                NovaState.LISTENING -> ErrorRed.copy(alpha = 0.15f)
                                NovaState.THINKING -> WarningAmber.copy(alpha = 0.2f)
                                else -> SurfaceVariant
                            }
                        ) {
                            Text(
                                text = when (novaState) {
                                    NovaState.SPEAKING -> "🔊 Nova is speaking..."
                                    NovaState.LISTENING -> "🎤 Listening to your answer..."
                                    NovaState.THINKING -> "✨ Processing response..."
                                    else -> "Ready for your answer"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (novaState) {
                                    NovaState.SPEAKING -> PrimaryDark
                                    NovaState.LISTENING -> ErrorRed
                                    NovaState.THINKING -> PrimaryDark
                                    else -> TextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Question / Transcription Card
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Surface,
                        border = BorderStroke(1.dp, BorderColor),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (interviewPhase == InterviewVoicePhase.USER_LISTENING) {
                                Text(
                                    text = "Your Answer (Live Transcription):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (userSpokenText.isNotBlank()) userSpokenText else "Listening... Speak clearly into your microphone.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (userSpokenText.isNotBlank()) TextPrimary else TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = currentQ?.category ?: "Interview Question",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentQ?.text ?: "Loading next question...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Bottom Voice Interaction Control
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isListening) {
                                    voiceManager.stopListening()
                                } else {
                                    voiceManager.stopSpeaking()
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = interviewPhase != InterviewVoicePhase.NOVA_SPEAKING && interviewPhase != InterviewVoicePhase.THINKING,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) ErrorRed else Primary
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(76.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Tap to Answer",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when {
                                isListening -> "Tap to finish answering"
                                interviewPhase == InterviewVoicePhase.NOVA_SPEAKING -> "Nova is speaking..."
                                interviewPhase == InterviewVoicePhase.THINKING -> "Processing response..."
                                else -> "Tap to answer"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) ErrorRed else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
