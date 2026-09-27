package com.example.aidrivencompetencyplatform.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.aidrivencompetencyplatform.model.ChatContextMode
import com.example.aidrivencompetencyplatform.model.NovaState
import com.example.aidrivencompetencyplatform.ui.screens.ModernChatBubble
import com.example.aidrivencompetencyplatform.ui.theme.*
import com.example.aidrivencompetencyplatform.viewmodel.AiAssistantViewModel
import com.example.aidrivencompetencyplatform.voice.VoiceSpeechManager
import kotlinx.coroutines.launch

/**
 * Floating Nova Chatbot Button that remains accessible across all dashboards.
 * Uses official Nova avatar asset with clean theme styling.
 */
@Composable
fun NovaFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 80.dp,
    endPadding: Dp = 16.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "novaPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding, end = endPadding),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(26.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary.copy(alpha = 0.35f)),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .height(52.dp)
                .wrapContentWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Nova Avatar with online indicator
                Box(contentAlignment = Alignment.BottomEnd) {
                    NovaAvatar(size = 38.dp, elevation = 2.dp)
                    Surface(
                        shape = CircleShape,
                        color = SuccessGreen,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Surface),
                        modifier = Modifier.size(10.dp)
                    ) {}
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ask Nova",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "AI Career Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

/**
 * Nova Character Interviewer Header widget reflecting Nova's live interaction states:
 * IDLE → SPEAKING → LISTENING → THINKING → ENCOURAGING
 */
@Composable
fun NovaInterviewerHeader(
    novaState: NovaState,
    targetRole: String,
    isInterviewMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "novaCharAnim")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Surface(
        color = SurfaceVariant.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColorGreen.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Avatar Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer(scaleX = breathingScale, scaleY = breathingScale)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(NovaSageBg)
            ) {
                NovaAvatar(size = 50.dp, elevation = 0.dp, showBorder = false)

                // State indicator ring / badge overlay
                when (novaState) {
                    NovaState.SPEAKING -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(2.5.dp, Primary, CircleShape)
                        )
                    }
                    NovaState.LISTENING -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(2.5.dp, ErrorRed, CircleShape)
                        )
                    }
                    NovaState.THINKING -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(2.5.dp, WarningAmber, CircleShape)
                        )
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isInterviewMode) "Nova • Interviewer" else "Nova AI",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (novaState) {
                            NovaState.SPEAKING -> SoftGreen
                            NovaState.LISTENING -> ErrorRed.copy(alpha = 0.15f)
                            NovaState.THINKING -> WarningAmber.copy(alpha = 0.2f)
                            NovaState.ENCOURAGING, NovaState.EXCITED -> SuccessGreen.copy(alpha = 0.2f)
                            else -> SurfaceVariant
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val statusText = when (novaState) {
                                NovaState.SPEAKING -> "🔊 Speaking"
                                NovaState.LISTENING -> "🎤 Listening..."
                                NovaState.THINKING -> "✨ Thinking..."
                                NovaState.ENCOURAGING -> "🌟 Encouraging"
                                NovaState.EXCITED -> "⭐ Excited"
                                NovaState.SURPRISED -> "😮 Surprised"
                                NovaState.WINKING -> "😉 Winking"
                                NovaState.HAPPY -> "😊 Happy"
                                else -> "🟢 Ready"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (novaState) {
                                    NovaState.SPEAKING -> PrimaryDark
                                    NovaState.LISTENING -> ErrorRed
                                    NovaState.THINKING -> PrimaryDark
                                    NovaState.ENCOURAGING, NovaState.EXCITED -> SuccessGreen
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val subtitle = when (novaState) {
                    NovaState.SPEAKING -> "Asking question / speaking..."
                    NovaState.LISTENING -> "Listening to your answer..."
                    NovaState.THINKING -> "Processing response & generating next step..."
                    NovaState.ENCOURAGING, NovaState.EXCITED -> "Great progress so far!"
                    else -> if (isInterviewMode) "Target Role: $targetRole" else "Persistent Career & Competency Guide"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Nova Chatbot Overlay Panel
 * Opens on top of the current screen without navigating away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaOverlayChatPanel(
    viewModel: AiAssistantViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val quickPrompts by viewModel.quickPrompts.collectAsState()
    val chatMode by viewModel.chatContextMode.collectAsState()
    val interviewSession by viewModel.interviewSession.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val voiceManager = remember { VoiceSpeechManager(context) }
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val currentUtteranceId by voiceManager.currentUtteranceId.collectAsState()
    val novaState by voiceManager.novaState.collectAsState()

    // Sync typing state with thinking state
    LaunchedEffect(isTyping) {
        if (isTyping) {
            voiceManager.setNovaState(NovaState.THINKING)
        } else if (novaState == NovaState.THINKING) {
            voiceManager.setNovaState(NovaState.IDLE)
        }
    }

    // Auto-speak the latest assistant message if in interview mode
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (!lastMsg.isFromUser && chatMode == ChatContextMode.INTERVIEW_PREPARATION) {
                val utteranceId = "utterance_${messages.size}"
                voiceManager.speak(lastMsg.text, utteranceId)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.release()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Barge-in: stop TTS immediately when user activates mic
            voiceManager.stopSpeaking()
            voiceManager.startListening(
                onPartialResult = { inputText = it },
                onFinalResult = { inputText = it },
                onErrorCallback = { errorMsg ->
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val isInterviewMode = chatMode == ChatContextMode.INTERVIEW_PREPARATION

    val defaultPrompts = listOf(
        "📊 ATS Score analysis",
        "🎯 Top skill gaps",
        "💡 Resume improvements",
        "💼 Job role advice",
        "🎤 Mock interview"
    )
    val displayPrompts = if (quickPrompts.isNotEmpty()) quickPrompts else defaultPrompts

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Semi-transparent backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Floating Chat Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent clicks inside card from closing
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Background,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Nova Interviewer Character Header
                    NovaInterviewerHeader(
                        novaState = novaState,
                        targetRole = interviewSession.targetRole,
                        isInterviewMode = isInterviewMode
                    )

                    // Close Button bar top right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isInterviewMode) "Mock Interview Session" else "AI Assistant Chat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Assistant",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. Quick Prompt Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayPrompts) { prompt ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColorGreen),
                                modifier = Modifier.clickable {
                                    if (prompt.contains("Mock interview", ignoreCase = true)) {
                                        viewModel.startInterview()
                                    } else {
                                        viewModel.sendMessage(prompt)
                                    }
                                }
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                    // 3. Message List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(messages) { message ->
                            val messageId = "overlay_msg_${messages.indexOf(message)}"
                            val isThisMsgSpeaking = isSpeaking && currentUtteranceId == messageId
                            ModernChatBubble(
                                text = message.text,
                                isUser = message.isFromUser,
                                isSpeaking = isThisMsgSpeaking,
                                onSpeakClick = if (!message.isFromUser) {
                                    {
                                        voiceManager.speak(message.text, messageId)
                                    }
                                } else null
                            )
                        }

                        if (isTyping) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    NovaAvatar(size = 28.dp, elevation = 1.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isInterviewMode) "Nova is preparing next question..." else "Nova is thinking...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = Primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // 4. Input Field Bar (with Barge-in & Live Speech Transcription)
                    Surface(
                        color = Surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = if (isListening) "Listening to you speak..." else if (isInterviewMode) "Speak or type your answer..." else "Ask Nova anything...",
                                        color = if (isListening) ErrorRed else TextMuted,
                                        fontSize = 15.sp
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isListening) ErrorRed else Primary,
                                    unfocusedBorderColor = if (isListening) ErrorRed.copy(alpha = 0.5f) else BorderColor,
                                    focusedContainerColor = SurfaceVariant,
                                    unfocusedContainerColor = SurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Mic Button with Barge-in / Interruption
                            IconButton(
                                onClick = {
                                    if (isListening) {
                                        voiceManager.stopListening()
                                    } else {
                                        // Barge-in: immediately stop TTS when user starts recording
                                        voiceManager.stopSpeaking()
                                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isListening) ErrorRed else SurfaceVariant)
                                    .size(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop Listening" else "Voice Input",
                                    tint = if (isListening) Color.White else Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Send Button
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        val text = inputText
                                        inputText = ""
                                        // Ensure TTS stops when sending answer
                                        voiceManager.stopSpeaking()
                                        voiceManager.stopListening()
                                        viewModel.sendMessage(text)
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (inputText.isNotBlank()) Primary else Primary.copy(alpha = 0.4f))
                                    .size(42.dp)
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
