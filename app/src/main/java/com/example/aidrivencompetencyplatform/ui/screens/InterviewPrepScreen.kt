package com.example.aidrivencompetencyplatform.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.model.InterviewQuestion
import com.example.aidrivencompetencyplatform.model.NovaState
import com.example.aidrivencompetencyplatform.ui.components.NovaCharacter
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import com.example.aidrivencompetencyplatform.ui.theme.*
import com.example.aidrivencompetencyplatform.viewmodel.InterviewViewModel
import com.example.aidrivencompetencyplatform.voice.VoiceSpeechManager

enum class InterviewVoicePhase {
    INTRO,
    NOVA_SPEAKING,
    READY_TO_ANSWER,
    USER_LISTENING,
    THINKING,
    COMPLETED
}

/**
 * Character-led Interview Preparation Experience
 * Nova acts as the personal AI interviewer inspired by Duolingo-style character interaction.
 *
 * Core loop:
 * Nova greets & asks -> Spoken aloud -> User replies with mic -> Nova thinks & evaluates
 * -> Nova gives feedback & asks next question -> Final performance summary.
 */
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
    val audioRms by voiceManager.audioRms.collectAsState()

    var userSpokenText by remember { mutableStateOf("") }
    var interviewPhase by remember { mutableStateOf(InterviewVoicePhase.INTRO) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showKeyboardFallbackDialog by remember { mutableStateOf(false) }
    var typedAnswerText by remember { mutableStateOf("") }

    // Derive active character expression: priority to listening/speaking/thinking states, else ViewModel expression
    val activeNovaState = remember(isListening, isSpeaking, state.isEvaluating, state.currentNovaExpression) {
        when {
            state.isEvaluating -> NovaState.THINKING
            isListening -> NovaState.LISTENING
            isSpeaking -> NovaState.SPEAKING
            else -> state.currentNovaExpression
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.release()
        }
    }

    // Spoken introduction and question flow
    LaunchedEffect(state.currentQuestionIndex, state.questions, state.isLoading) {
        if (!state.isLoading && state.questions.isNotEmpty() && !state.isComplete) {
            val q = state.questions.getOrNull(state.currentQuestionIndex)
            if (q != null) {
                interviewPhase = InterviewVoicePhase.NOVA_SPEAKING
                val isFirstQuestion = state.currentQuestionIndex == 0 && !state.isIntroFinished

                val speechText = if (isFirstQuestion) {
                    val namePart = if (!state.candidateName.isNullOrBlank()) " ${state.candidateName}" else ""
                    val intro = "Hi$namePart! I'm Nova, your AI interviewer today. I'm excited to practice with you for the ${state.targetRole} role. Take your time, speak naturally, and let's begin with your first question:"
                    viewModel.markIntroFinished()
                    "$intro ${q.text}"
                } else if (!state.lastNovaFeedback.isNullOrBlank()) {
                    "${state.lastNovaFeedback}. Question ${state.currentQuestionIndex + 1}: ${q.text}"
                } else {
                    "Question ${state.currentQuestionIndex + 1}: ${q.text}"
                }

                voiceManager.speak(
                    text = speechText,
                    utteranceId = "q_${state.currentQuestionIndex}"
                ) {
                    interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
                }
            }
        } else if (state.isComplete) {
            interviewPhase = InterviewVoicePhase.COMPLETED
            voiceManager.speak(
                text = "Fantastic job! Your mock interview is complete. Let's review your personalized performance summary.",
                utteranceId = "completion"
            )
        }
    }

    // Fallback sync when TTS stops speaking
    LaunchedEffect(isSpeaking) {
        if (!isSpeaking && interviewPhase == InterviewVoicePhase.NOVA_SPEAKING) {
            interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
        }
    }

    // Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.stopSpeaking()
            interviewPhase = InterviewVoicePhase.USER_LISTENING
            userSpokenText = ""
            voiceManager.startListening(
                onPartialResult = { partial ->
                    userSpokenText = partial
                },
                onFinalResult = { finalSpoken ->
                    userSpokenText = finalSpoken
                    if (finalSpoken.isNotBlank()) {
                        interviewPhase = InterviewVoicePhase.THINKING
                        viewModel.submitAnswer(finalSpoken)
                    } else {
                        interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
                    }
                },
                onErrorCallback = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for the voice interview.", Toast.LENGTH_LONG).show()
        }
    }

    // Function to trigger replaying question
    val replayCurrentQuestion = {
        val currentQ = state.questions.getOrNull(state.currentQuestionIndex)
        if (currentQ != null && !state.isComplete && !state.isEvaluating) {
            voiceManager.stopListening()
            interviewPhase = InterviewVoicePhase.NOVA_SPEAKING
            voiceManager.speak(
                text = "Here is the question again: ${currentQ.text}",
                utteranceId = "replay_${state.currentQuestionIndex}"
            ) {
                interviewPhase = InterviewVoicePhase.READY_TO_ANSWER
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Nova • Interviewer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SoftGreen
                            ) {
                                Text(
                                    text = "AI INTERVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Target Role: ${state.targetRole}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!state.isComplete && state.questions.isNotEmpty()) {
                            showExitDialog = true
                        } else {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        voiceManager.stopSpeaking()
                        voiceManager.stopListening()
                        viewModel.startInterview()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart Interview", tint = TextSecondary)
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
                        colors = listOf(
                            Color(0xFFE8F7F0),
                            Surface,
                            Background
                        )
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
                    }
                )
            } else {
                val currentQ = state.questions.getOrNull(state.currentQuestionIndex)
                val totalQuestions = state.questions.size.coerceAtLeast(1)
                val progressFraction = ((state.currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "ProgressAnimation"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. TOP PROGRESS BAR (Duolingo-style smooth progress)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${state.currentQuestionIndex + 1} of $totalQuestions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )

                            // Fallback keyboard button
                            TextButton(
                                onClick = {
                                    typedAnswerText = userSpokenText
                                    showKeyboardFallbackDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Type answer", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Indicator
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Primary,
                            trackColor = BorderColorGreen
                        )
                    }

                    // 2. MAIN CHARACTER STAGE: Nova & Speech Bubble
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Nova Character (Prominent, Duolingo-style illustrated character)
                        NovaCharacter(
                            size = 195.dp,
                            state = activeNovaState,
                            modifier = Modifier.clickable {
                                // Tapping Nova replays the question or greeting
                                replayCurrentQuestion()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Nova Speech Bubble (Pointing toward Nova)
                        NovaSpeechBubble(
                            question = currentQ,
                            feedback = state.lastNovaFeedback,
                            isSpeaking = isSpeaking,
                            onReplayClick = replayCurrentQuestion
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dynamic Live Transcription / Thinking Card
                        AnimatedVisibility(
                            visible = isListening || state.isEvaluating || userSpokenText.isNotBlank(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LiveTranscriptCard(
                                isListening = isListening,
                                isEvaluating = state.isEvaluating,
                                spokenText = userSpokenText,
                                audioRms = audioRms
                            )
                        }
                    }

                    // 3. BOTTOM ACTION AREA: Large Microphone Button with Ripples
                    MicInteractionSection(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isEvaluating = state.isEvaluating,
                        onMicClick = {
                            if (isListening) {
                                voiceManager.stopListeningAndProcess()
                            } else {
                                voiceManager.stopSpeaking()
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave Mock Interview?", fontWeight = FontWeight.Bold) },
            text = { Text("Your current interview progress will be saved. You can restart or practice again anytime.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        voiceManager.release()
                        navController.popBackStack()
                    }
                ) {
                    Text("Exit", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Interview", color = Primary)
                }
            }
        )
    }

    // Fallback Keyboard Input Dialog
    if (showKeyboardFallbackDialog) {
        Dialog(onDismissRequest = { showKeyboardFallbackDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Type Your Answer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Voice is recommended for realistic practice, but you can type your answer below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = typedAnswerText,
                        onValueChange = { typedAnswerText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Type your detailed interview answer here...") },
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showKeyboardFallbackDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (typedAnswerText.isNotBlank()) {
                                    showKeyboardFallbackDialog = false
                                    userSpokenText = typedAnswerText
                                    interviewPhase = InterviewVoicePhase.THINKING
                                    viewModel.submitAnswer(typedAnswerText)
                                }
                            },
                            enabled = typedAnswerText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Submit Answer", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Duolingo-style Speech Bubble with speech arrow, question text, and Replay Audio button
 */
@Composable
private fun NovaSpeechBubble(
    question: InterviewQuestion?,
    feedback: String?,
    isSpeaking: Boolean,
    onReplayClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, BorderColorGreen),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Optional previous feedback banner
            if (!feedback.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryDark
                        )
                    }
                }
            }

            // Question Category and Replay Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = question?.category?.uppercase() ?: "INTERVIEW QUESTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Replay Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSpeaking) SoftGreen else SurfaceVariant,
                    modifier = Modifier.clickable { onReplayClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Replay Question",
                            tint = if (isSpeaking) PrimaryDark else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSpeaking) "Speaking..." else "Replay",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSpeaking) PrimaryDark else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The Interview Question Text
            Text(
                text = question?.text ?: "Loading interview question...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 24.sp
            )

            if (!question?.modelGuidance.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Tip: ${question.modelGuidance}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Live Transcription & Thinking Card
 */
@Composable
private fun LiveTranscriptCard(
    isListening: Boolean,
    isEvaluating: Boolean,
    spokenText: String,
    audioRms: Float
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isEvaluating) WarningAmber.copy(alpha = 0.10f) else Color(0xFFF9FDFB),
        border = BorderStroke(1.dp, if (isEvaluating) WarningAmber.copy(alpha = 0.5f) else Primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEvaluating) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = WarningAmber
                        )
                    } else if (isListening) {
                        // Pulsing red recording dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isEvaluating -> "Nova is evaluating your answer..."
                            isListening -> "Listening to your answer..."
                            else -> "Your Answer Transcript:"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isEvaluating -> WarningAmber
                            isListening -> ErrorRed
                            else -> PrimaryDark
                        }
                    )
                }

                // Audio level indicator when listening
                if (isListening) {
                    val normalizedRms = (audioRms.coerceIn(0f, 10f) / 10f)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(4) { i ->
                            val height = (6 + (normalizedRms * 14 * (i + 1) / 4)).dp
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Primary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (spokenText.isNotBlank()) spokenText else "Speak clearly into your microphone...",
                style = MaterialTheme.typography.bodyMedium,
                color = if (spokenText.isNotBlank()) TextPrimary else TextMuted,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Concentric Glowing Ripple Microphone Section matching reference image
 */
@Composable
private fun MicInteractionSection(
    isListening: Boolean,
    isSpeaking: Boolean,
    isEvaluating: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicRipples")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RippleScale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RippleAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Outer concentric glow container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(118.dp)
        ) {
            // Ripple 1
            if (isListening || !isSpeaking && !isEvaluating) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = if (isListening) rippleScale else 1.0f
                            scaleY = if (isListening) rippleScale else 1.0f
                            alpha = if (isListening) rippleAlpha else 0.25f
                        }
                        .clip(CircleShape)
                        .background(if (isListening) ErrorRed else Color(0xFFC8F0DF))
                )
            }

            // Ripple 2
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(if (isListening) ErrorRed.copy(alpha = 0.35f) else Color(0xFFE2F7ED))
            )

            // Central Mic Button (84.dp)
            Button(
                onClick = onMicClick,
                enabled = !isEvaluating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) ErrorRed else Primary,
                    disabledContainerColor = SurfaceVariant
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(80.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Default.Stop
                        isEvaluating -> Icons.Default.HourglassTop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = if (isListening) "Stop Recording" else "Tap to Answer",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State label below mic
        Text(
            text = when {
                isListening -> "Listening... Tap to finish answer"
                isEvaluating -> "Nova is thinking..."
                isSpeaking -> "Nova is speaking..."
                else -> "Tap to answer"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                isListening -> ErrorRed
                isEvaluating -> WarningAmber
                isSpeaking -> PrimaryDark
                else -> TextPrimary
            }
        )
    }
}
