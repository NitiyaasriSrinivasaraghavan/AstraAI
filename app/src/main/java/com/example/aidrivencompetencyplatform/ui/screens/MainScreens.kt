package com.example.aidrivencompetencyplatform.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.model.*
import com.example.aidrivencompetencyplatform.ui.components.*
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import com.example.aidrivencompetencyplatform.ui.theme.*
import com.example.aidrivencompetencyplatform.viewmodel.*
import kotlinx.coroutines.delay

// ==========================================
// 1. APP TOUR / ONBOARDING SCREEN (CHATBOT-LED)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTourScreen(
    navController: NavController,
    assistantViewModel: AiAssistantViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val messages by assistantViewModel.messages.collectAsState()
    val quickPrompts by assistantViewModel.quickPrompts.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        assistantViewModel.startTour()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NovaAvatar(size = 38.dp, elevation = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Nova Guided Tour",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Interactive AI Career Companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            dashboardViewModel.completeTour()
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.AppTour.route) { inclusive = true }
                            }
                        }
                    ) {
                        Text("Skip to Home", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            Surface(
                color = Surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Suggested Prompts Chips
                    if (quickPrompts.isNotEmpty()) {
                        Text(
                            text = "Suggested Questions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickPrompts) { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MintLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.clickable {
                                        if (prompt.contains("Ready to upload", ignoreCase = true) || prompt.contains("Analyse", ignoreCase = true)) {
                                            dashboardViewModel.completeTour()
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(Screen.AppTour.route) { inclusive = true }
                                            }
                                        } else {
                                            assistantViewModel.sendMessage(prompt, screenContext = "Product Tour")
                                        }
                                    }
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryDark,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Direct CTA Button & Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Nova anything about the app...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceVariant,
                                unfocusedContainerColor = SurfaceVariant
                            ),
                            maxLines = 2
                        )

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText.trim()
                                    inputText = ""
                                    assistantViewModel.sendMessage(text, screenContext = "Product Tour")
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Primary)
                                .size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumButton(
                        text = "Start Analyzing Resume →",
                        onClick = {
                            dashboardViewModel.completeTour()
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.AppTour.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { msg ->
                ModernChatBubble(msg.text, msg.isFromUser)
            }
        }
    }
}// ==========================================
// 2. CAREER HUB (MAIN DASHBOARD)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel,
    resumeViewModel: ResumeViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val assistantViewModel: AiAssistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AppViewModelFactory(app))
    var isNovaOpen by remember { mutableStateOf(false) }

    val userName by dashboardViewModel.userName.collectAsState()
    val greetingPrefix by dashboardViewModel.greetingPrefix.collectAsState()
    val isNewUser by dashboardViewModel.isNewUser.collectAsState()
    val hasAnalysis by dashboardViewModel.hasAnalysis.collectAsState()
    val atsScore by dashboardViewModel.atsScore.collectAsState()
    val skillMatch by dashboardViewModel.skillMatch.collectAsState()
    val targetRole by dashboardViewModel.targetRole.collectAsState()
    val latestAnalysis by dashboardViewModel.latestAnalysis.collectAsState()
    val showTour by dashboardViewModel.showTour.collectAsState()
    val tourCurrentStep by dashboardViewModel.tourCurrentStep.collectAsState()
    val analysisHistory by dashboardViewModel.analysisHistory.collectAsState()

    val isLoading by resumeViewModel.isLoading.collectAsState()
    val interactiveState by resumeViewModel.interactiveState.collectAsState()
    val analyzingRole by resumeViewModel.analyzingRole.collectAsState()
    val analysisResult by resumeViewModel.analysisResult.collectAsState()

    LaunchedEffect(Unit) {
        dashboardViewModel.refreshUser()
    }

    // Removed the automatic navigation from Hub to avoid conflicts with Upload screen

    // Interactive Astra Onboarding Tour Dialog
    if (showTour) {
        AstraInteractiveTourDialog(
            currentStep = tourCurrentStep,
            onNextStep = { dashboardViewModel.nextTourStep() },
            onPreviousStep = { dashboardViewModel.previousTourStep() },
            onSkip = { dashboardViewModel.dismissTour() },
            onNavigateToStep = { step ->
                dashboardViewModel.dismissTour()
                when (step) {
                    1 -> navController.navigate(Screen.ResumeUpload.route)
                    2 -> navController.navigate(Screen.AtsAnalysis.route)
                    3 -> navController.navigate(Screen.SkillGap.route)
                    4 -> navController.navigate(Screen.JobDescriptionAnalyzer.route)
                }
            }
        )
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            // Clean Bottom Navigation Hub
            Surface(
                color = Surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Career Hub (Active)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SoftGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Career Hub",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hub",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }

                    // 2. Upload Resume
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.ResumeUpload.route) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // 3. ATS Score
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.AtsAnalysis.route) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = "ATS Score",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ATS Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // 4. Skill Gap
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.SkillGap.route) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "Skill Gap",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Skill Gap",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // 5. JD Matcher
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.JobDescriptionAnalyzer.route) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = "JD Matcher",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "JD Matcher",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Curved Sage/Mint Header with Personalized Greeting
            item {
                val displayName = userName.ifBlank { "User" }
                val greeting = if (isNewUser || greetingPrefix.equals("Welcome", ignoreCase = true)) {
                    "Welcome, $displayName 👋"
                } else {
                    "Welcome back, $displayName 👋"
                }
                val subGreeting = if (isNewUser) {
                    "Your AI career companion is ready to guide you"
                } else {
                    "Central career intelligence & competency hub"
                }

                AstraDashboardHeader(
                    greeting = greeting,
                    subGreeting = subGreeting,
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )
            }

            if (!hasAnalysis) {
                // ==========================================
                // NEW USER EMPTY STATE
                // ==========================================
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AstraCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            containerColor = Surface,
                            borderColor = BorderColor
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SoftGreen,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Analyze your resume to get started",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Upload your resume in PDF or DOCX format to calculate deterministic ATS compatibility scoring, identify skill gaps, and optimize your job readiness.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        navController.navigate(Screen.ResumeUpload.route)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upload Resume to Analyze",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // EXISTING USER DASHBOARD:
                // 1. RECENT ANALYSIS OVERVIEW
                // 2. HERO "ANALYZE RESUME" ACTION
                // 3. ANALYSIS HISTORY
                // ==========================================
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AstraProgressSummaryCard(
                            atsScore = atsScore,
                            skillMatch = skillMatch,
                            targetRole = targetRole,
                            latestAnalysis = latestAnalysis,
                            onViewAtsClick = { 
                                val route = Screen.AtsAnalysis.route + (latestAnalysis?.id?.let { "?analysisId=$it" } ?: "")
                                navController.navigate(route) 
                            },
                            onViewSkillGapClick = { 
                                val route = Screen.SkillGap.route + (latestAnalysis?.id?.let { "?analysisId=$it" } ?: "")
                                navController.navigate(route) 
                            }
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AnalyzeResumeHeroCard(
                            hasAnalysis = true,
                            onAnalyzeClick = {
                                navController.navigate(Screen.ResumeUpload.route)
                            }
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AnalysisHistorySection(
                            historyList = analysisHistory,
                            onRecordClick = { record ->
                                navController.navigate(Screen.AtsAnalysis.route + "?analysisId=${record.id}")
                            },
                            onAnalyzeNewClick = {
                                navController.navigate(Screen.ResumeUpload.route)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Nova Assistant Button
        NovaFloatingButton(
            onClick = { isNovaOpen = true },
            bottomPadding = 16.dp
        )
    }

    // Real-time Resume Analysis Progress Modal (ATS, Skill Gap, JD Matching)
    if (isLoading) {
        ResumeAnalysisProgressModal(
            isLoading = true,
            interactiveState = interactiveState,
            targetRole = analyzingRole.ifBlank { targetRole.ifBlank { "Android Developer" } },
            onFinishClick = {
                resumeViewModel.finishLoading()
                navController.navigate(Screen.AtsAnalysis.route)
            }
        )
    }

    // Nova Overlay Chat Panel
    NovaOverlayChatPanel(
        viewModel = assistantViewModel,
        isOpen = isNovaOpen,
        onDismiss = { isNovaOpen = false }
    )
}
}

// ==========================================
// COMPONENT: ASTRA WELCOME INTRO CARD
// ==========================================

@Composable
fun AstraWelcomeIntroCard(
    isNewUser: Boolean,
    onStartTourClick: () -> Unit
) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        containerColor = Surface,
        borderColor = Primary.copy(alpha = 0.25f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NovaAvatar(size = 44.dp, elevation = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hi, I'm Nova",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your AI Career Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.clickable { onStartTourClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Tour,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isNewUser) "App Tour" else "Take Tour",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }
            }

            Text(
                text = "I'll help you understand your resume, identify skill gaps, and find better job matches.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

// ==========================================
// COMPONENT: HERO ACTION - ANALYZE RESUME
// ==========================================

@Composable
fun AnalyzeResumeHeroCard(
    hasAnalysis: Boolean,
    onAnalyzeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MintLight)
            .border(2.dp, Primary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Primary,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = "Analyze Resume",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Analyze Resume",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasAnalysis) "Re-upload or switch target role" else "Primary step to get started",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary
                ) {
                    Text(
                        text = "START HERE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Upload your resume and select your target role to begin your career analysis.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Button(
                onClick = onAnalyzeClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (hasAnalysis) "Upload New Resume / Change Role" else "Upload Resume & Start Analysis",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: ASTRA INTERACTIVE TOUR DIALOG
// ==========================================

@Composable
fun AstraInteractiveTourDialog(
    currentStep: Int,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSkip: () -> Unit,
    onNavigateToStep: (Int) -> Unit
) {
    val steps = listOf(
        Triple(
            "Analyze Resume",
            "This is where you upload your resume and select your target role before starting analysis.",
            Icons.Default.CloudUpload
        ),
        Triple(
            "ATS Analysis",
            "Evaluates how well your resume is optimized for applicant tracking systems with transparent scoring.",
            Icons.Default.Assessment
        ),
        Triple(
            "Skill Gap",
            "Identifies missing or improvable skills for your selected target role with guided learning paths.",
            Icons.Default.Psychology
        ),
        Triple(
            "JD Matcher",
            "Compares your resume against a specific job description to benchmark role-specific alignment.",
            Icons.Default.Work
        )
    )

    val stepIndex = (currentStep - 1).coerceIn(0, steps.size - 1)
    val step = steps[stepIndex]

    Dialog(onDismissRequest = onSkip) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary.copy(alpha = 0.3f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Astra Avatar and Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NovaAvatar(size = 36.dp, elevation = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nova Tour Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoftGreen
                    ) {
                        Text(
                            text = "Step $currentStep of 4",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Feature Icon Display
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.third,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Step Title & 2-line Description
                Text(
                    text = step.first,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = step.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                // Step Progress Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        val isCurrent = i + 1 == currentStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isCurrent) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) Primary else BorderColor)
                        )
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.7f))

                // Navigation Controls (Skip / Previous / Next)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Skip", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }

                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = onPreviousStep,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Text("Back", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = onNextStep,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(
                            text = if (currentStep < 4) "Next" else "Finish",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Quick jump link
                TextButton(
                    onClick = { onNavigateToStep(currentStep) },
                    modifier = Modifier.padding(top = 0.dp)
                ) {
                    Text(
                        text = "Open ${step.first} now →",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: PROGRESS SUMMARY (RETURNING USERS)
// ==========================================

@Composable
fun AstraProgressSummaryCard(
    atsScore: Int?,
    skillMatch: Int?,
    targetRole: String,
    latestAnalysis: com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult?,
    onViewAtsClick: () -> Unit,
    onViewSkillGapClick: () -> Unit
) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        containerColor = Surface,
        borderColor = BorderColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SoftGreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Recent Analysis Overview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target Role: $targetRole",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SoftGreen
                ) {
                    Text(
                        text = "Active Profile",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ATS Metric Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewAtsClick() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (atsScore != null) "$atsScore/100" else "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = AtsDarkGreen
                        )
                        Text(
                            text = "ATS Compatibility",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Skill Match Metric Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewSkillGapClick() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (skillMatch != null) "$skillMatch%" else "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = AtsDarkGreen
                        )
                        Text(
                            text = "Skill Coverage",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: NEW USER GETTING STARTED CARD
// ==========================================

@Composable
fun AstraNewUserGuideCard(
    onStartClick: () -> Unit
) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        containerColor = Surface,
        borderColor = BorderColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Getting Started in 2 Easy Steps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SoftGreen,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Upload your resume & select target role",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SoftGreen,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Unlock deep ATS diagnostics & tailored skill gaps",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }

            TextButton(
                onClick = onStartClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Get Started →", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// COMPONENT: ANALYSIS HISTORY SECTION
// ==========================================

@Composable
fun AnalysisHistorySection(
    historyList: List<AnalysisHistoryRecord>,
    onRecordClick: (AnalysisHistoryRecord) -> Unit,
    onAnalyzeNewClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SoftGreen,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analysis History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (historyList.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SoftGreen
                ) {
                    Text(
                        text = "${historyList.size} Record${if (historyList.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No previous analysis records yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Your completed resume scans and ATS evaluations will appear here for quick reference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            historyList.forEach { record ->
                AnalysisHistoryItemCard(
                    record = record,
                    onClick = { onRecordClick(record) }
                )
            }
        }
    }
}

@Composable
fun AnalysisHistoryItemCard(
    record: AnalysisHistoryRecord,
    onClick: () -> Unit
) {
    val dateFormatted = remember(record.timestamp) {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(record.timestamp))
    }

    AstraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp,
        containerColor = Surface,
        borderColor = BorderColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SoftGreen,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = record.targetRole,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${record.fileName ?: "Resume.pdf"} • $dateFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${record.atsScore}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = PrimaryDark
                        )
                        Text(
                            text = "ATS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                }
            }

            if (record.topSkills.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    record.topSkills.take(3).forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceVariant
                        ) {
                            Text(
                                text = skill,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (record.topSkills.size > 3) {
                        Text(
                            text = "+${record.topSkills.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${record.skillMatch}% Match",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: ASTRA DASHBOARD HEADER
// ==========================================

@Composable
fun AstraDashboardHeader(
    greeting: String,
    subGreeting: String,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Primary,
                        PrimaryLight
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NoviQLogoTile(size = 38.dp, elevation = 3.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NoviQ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Career Intelligence",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subGreeting,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// ==========================================
// COMPONENT: MAIN FEATURE CARD
// ==========================================

@Composable
fun MainFeatureCard(
    number: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionText: String,
    statusTag: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (isHighlighted) Primary else BorderColor
    val backgroundColor = if (isHighlighted) MintLight else Surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isHighlighted) 4.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(if (isHighlighted) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isHighlighted) Primary else SoftGreen,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isHighlighted) Color.White else PrimaryDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = statusTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            HorizontalDivider(color = BorderColor.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. DEDICATED UPLOAD RESUME REAL-TIME 3-MODULE PROGRESS CARD
// ==========================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ResumeAnalysisProgressModal(
    isLoading: Boolean = true,
    interactiveState: InteractiveAnalysisState,
    targetRole: String,
    onFinishClick: () -> Unit
) {
    if (!isLoading) return

    val currentStage = interactiveState.stage
    val allComplete = interactiveState.isComplete
    
    val stages = listOf(
        "Extraction" to "Extracting raw text from document...",
        "Parsing" to "Identifying candidate profile details...",
        "AI Analysis" to "Deep semantic evaluation with Gemini...",
        "ATS Analysis" to "Benchmarking ATS compatibility...",
        "Skill Gaps" to "Mapping competencies to $targetRole...",
        "JD Alignment" to "Calculating role-specific match score..."
    )

    Dialog(
        onDismissRequest = { /* Modal remains active during processing */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Background,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (allComplete) "Analysis Complete" else "Analyzing Your Resume",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Nova AI Intelligence Hub",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                // Miniature Resume Preview with Scanning Animation
                MiniResumeCard(isScanning = !allComplete)

                // Progress Stages Status
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        stages.forEachIndexed { index, (title, desc) ->
                            val stageNum = index + 1
                            val isDone = currentStage > stageNum || allComplete
                            val isProcessing = currentStage == stageNum && !allComplete
                            
                            AnalysisStepItem(
                                title = title,
                                description = desc,
                                isCompleted = isDone,
                                isProcessing = isProcessing
                            )
                        }
                    }
                }

                if (allComplete) {
                    Button(
                        onClick = onFinishClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Open ATS Dashboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Primary,
                            strokeWidth = 2.0.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Please wait, AI engines running...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniResumeCard(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scannerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scannerOffset"
    )

    Box(
        modifier = Modifier
            .size(width = 170.dp, height = 220.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Primary.copy(alpha = 0.03f))
    ) {
        // Resume Layout Mockup (Floating miniature document)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header lines
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(8.dp).background(TextSecondary.copy(alpha = 0.15f)))
            Box(modifier = Modifier.fillMaxWidth(0.3f).height(4.dp).background(TextSecondary.copy(alpha = 0.1f)))
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Section 1
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(6.dp).background(Primary.copy(alpha = 0.15f)))
            repeat(2) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(TextMuted.copy(alpha = 0.12f)))
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Section 2
            Box(modifier = Modifier.fillMaxWidth(0.45f).height(6.dp).background(Primary.copy(alpha = 0.15f)))
            repeat(3) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(TextMuted.copy(alpha = 0.12f)))
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Section 3 (Skills chips)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(modifier = Modifier.width(26.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(SoftGreen.copy(alpha = 0.4f)))
                }
            }
        }

        // Scanning Line
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 220.dp * scannerOffset)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Primary.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )
            
            // Subtle Overlay Glow following the scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(scannerOffset)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Primary.copy(alpha = 0.04f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun AnalysisStepItem(
    title: String,
    description: String,
    isCompleted: Boolean,
    isProcessing: Boolean
) {
    val alpha = if (isCompleted || isProcessing) 1f else 0.4f
    Row(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(20.dp).padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.5.dp, TextSecondary.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isProcessing) Primary else TextPrimary
            )
            if (isProcessing) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeUploadScreen(navController: NavController, viewModel: ResumeViewModel) {
    val analysisResult by viewModel.analysisResult.collectAsState()
    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingStage by viewModel.loadingStage.collectAsState()
    val stageIndex by viewModel.stageIndex.collectAsState()
    val interactiveState by viewModel.interactiveState.collectAsState()
    val analyzingRole by viewModel.analyzingRole.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val assistantViewModel: AiAssistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AppViewModelFactory(app))
    var isNovaOpen by remember { mutableStateOf(false) }

    // Target Role selection
    var targetRole by remember { mutableStateOf("Android Developer") }
    val roles = listOf(
        "Android Developer",
        "Java Developer",
        "Full Stack Developer",
        "Backend Developer",
        "Frontend Developer",
        "Data Analyst",
        "Data Scientist",
        "Machine Learning Engineer",
        "UI/UX Designer",
        "DevOps Engineer",
        "Cloud Architect",
        "Other"
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it) ?: "Resume.pdf"
            if (name.endsWith(".pdf", ignoreCase = true) ||
                name.endsWith(".doc", ignoreCase = true) ||
                name.endsWith(".docx", ignoreCase = true)
            ) {
                viewModel.onFileSelected(it, name)
            } else {
                Toast.makeText(context, "Please select a PDF or DOCX file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Reset any previous completion state on entering screen
    LaunchedEffect(Unit) {
        viewModel.resetAnalysisResult()
    }

    // AUTOMATIC NAVIGATION TO ATS DASHBOARD ON ANALYSIS COMPLETION
    LaunchedEffect(interactiveState.isComplete) {
        if (interactiveState.isComplete && analysisResult != null) {
            // SUCCESS: Actual analysis result received and UI marked as complete
            Log.d("ResumeNetworkDebug", "Analysis complete state reached, preparing navigation")
            delay(600L) // Reduced delay for faster transition
            viewModel.finishLoading()
            val route = Screen.AtsAnalysis.route + (analysisResult?.id?.let { "?analysisId=$it" } ?: "")
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Upload Resume",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Step 1 of your Career Intelligence analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Screen Subtitle & Description Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Upload your latest resume and select the role you're targeting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                    // 2. Astra Contextual Tip Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MintLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SoftGreen,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Nova Tip",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDark
                                    )
                                    Text(
                                        text = "Select your exact target role so our parser can benchmark your skill matches and ATS compatibility accurately.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // 3. Error Banner (if any)
                    if (errorMessage != null) {
                        item {
                            AstraCard(
                                containerColor = ErrorRed.copy(alpha = 0.08f),
                                borderColor = ErrorRed.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = errorMessage!!,
                                            color = ErrorRed,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = { viewModel.clearError() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4. STEP 1: RESUME UPLOAD CARD
                    item {
                        AstraCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (selectedFileName != null) SoftGreen else MintLight,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.CloudUpload,
                                                    contentDescription = null,
                                                    tint = if (selectedFileName != null) Primary else TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "1. Resume Document",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (selectedFileName != null) "File attached" else "PDF, DOC, or DOCX",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    if (selectedFileName != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SoftGreen
                                        ) {
                                            Text(
                                                text = "Ready",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = BorderColor.copy(alpha = 0.7f))

                                if (selectedFileName != null) {
                                    // File uploaded preview & action controls
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MintLight,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = Primary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = selectedFileName ?: "Resume",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = if (selectedFileName?.endsWith(".pdf", ignoreCase = true) == true) "PDF Document" else "Word Document",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = SoftGreen
                                                ) {
                                                    Text(
                                                        text = "✓ Ready for AI",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { viewModel.removeFile() }) {
                                                    Text("Remove", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                OutlinedButton(
                                                    onClick = { filePickerLauncher.launch("*/*") },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                                                ) {
                                                    Text("Replace Resume", color = Primary, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Empty Dropzone with Browse and 1-Click Sample options
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SurfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = SoftGreen,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clickable { filePickerLauncher.launch("*/*") }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.CloudUpload,
                                                        contentDescription = null,
                                                        tint = Primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Upload Your Resume",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Supports PDF & Word (.docx) formats",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { filePickerLauncher.launch("*/*") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                                                ) {
                                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Browse File", color = Primary, style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.loadSampleResume(targetRole)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MintLight, contentColor = PrimaryDark),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Sample Resume", color = PrimaryDark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. STEP 2: TARGET ROLE SELECTION CARD
                    item {
                        AstraCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SoftGreen,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Work,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "2. Target Career Role",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Select your target role to benchmark competencies",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                HorizontalDivider(color = BorderColor.copy(alpha = 0.7f))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    roles.forEach { role ->
                                        val isSelected = targetRole == role
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MintLight else Surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Primary else BorderColor
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { targetRole = role }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = role,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PrimaryDark else TextPrimary
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Primary,
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

                    // 6. ANALYZE BUTTON (PROMINENT ACTION)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PremiumButton(
                                text = if (selectedFileName == null) "Analyze Resume (or Sample)" else "Analyze Resume for $targetRole",
                                enabled = !isLoading,
                                onClick = {
                                    if (selectedFileName == null) {
                                        viewModel.loadSampleResume(targetRole)
                                    }
                                    viewModel.analyzeResume(targetRole)
                                }
                            )

                            Text(
                                text = if (selectedFileName == null) 
                                    "Tip: Tap button to analyze with sample resume, or attach your own above"
                                else
                                    "Ready to analyze $selectedFileName for $targetRole",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Resume Analysis Loading Modal
                if (isLoading) {
                    ResumeAnalysisProgressModal(
                        isLoading = true,
                        interactiveState = interactiveState,
                        targetRole = analyzingRole.ifBlank { targetRole },
                        onFinishClick = {
                            viewModel.finishLoading()
                            val route = Screen.AtsAnalysis.route + (analysisResult?.id?.let { "?analysisId=$it" } ?: "")
                            navController.navigate(route)
                        }
                    )
                }

                // Floating Nova Assistant Button
                NovaFloatingButton(
                    onClick = { isNovaOpen = true },
                    bottomPadding = 16.dp
                )
            }

            // Nova Overlay Chat Panel
            NovaOverlayChatPanel(
                viewModel = assistantViewModel,
                isOpen = isNovaOpen,
                onDismiss = { isNovaOpen = false }
            )
        }
    }

// ==========================================
// 4. ATS DASHBOARD SCREEN (Separate Dashboard)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtsDashboardScreen(
    navController: NavController, 
    viewModel: AtsViewModel,
    analysisId: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val assistantViewModel: AiAssistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AppViewModelFactory(app))
    var isNovaOpen by remember { mutableStateOf(false) }

    LaunchedEffect(analysisId) {
        if (!analysisId.isNullOrBlank()) {
            viewModel.loadAnalysis(analysisId)
        } else {
            viewModel.refresh()
        }
    }

    val analysis by viewModel.analysisResult.collectAsState()
    val scoreResult by viewModel.scoreResult.collectAsState()

    var selectedPriorityForDetail by remember { mutableStateOf<String?>(null) }
    var selectedWhyMetric by remember { mutableStateOf<AtsCardCalculation?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ATS Compatibility Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Deterministic Multi-Engine Parser Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "ATS Scoring Info", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Dashboard Content Container - Gets blurred when Why Modal is active (without any color tint or dark overlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (selectedWhyMetric != null) Modifier.blur(16.dp) else Modifier
                    )
            ) {
                if (analysis == null || scoreResult == null) {
                    NoAnalysisState(navController)
                } else {
                    val score = scoreResult!!
                    val analysisData = analysis!!

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // 1. USER NAME / CANDIDATE HEADER (MUST BE FIRST)
                        item {
                            CandidateProfileCard(
                                targetRole = score.targetRole,
                                candidateName = score.candidateName,
                                candidateEmail = score.candidateEmail,
                                candidatePhone = score.candidatePhone,
                                candidateLocation = score.candidateLocation
                            )
                        }

                        // 2. Main ATS Compatibility Score Hero
                        item {
                            AtsOverallScoreHero(
                                score = score,
                                onMetricClick = { metric ->
                                    selectedWhyMetric = metric
                                }
                            )
                        }

                        // 3. ATS SCORE FEATURES / BREAKDOWN Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ATS Performance Areas",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Tap 'Why?' on any area for formulas & evidence",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SoftGreen
                                ) {
                                    Text(
                                        text = "4 Dimensions",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 3a. Dimension 1: Keyword Coverage (35%)
                        item {
                            AtsExpandableCard(
                                calculation = score.keywordCoverage,
                                onAskWhyClick = {
                                    selectedWhyMetric = score.keywordCoverage
                                }
                            )
                        }

                        // 3b. Dimension 2: Resume Structure (25%)
                        item {
                            AtsExpandableCard(
                                calculation = score.resumeStructure,
                                onAskWhyClick = {
                                    selectedWhyMetric = score.resumeStructure
                                }
                            )
                        }

                        // 3c. Dimension 3: Formatting Safety (20%)
                        item {
                            AtsExpandableCard(
                                calculation = score.formattingSafety,
                                onAskWhyClick = {
                                    selectedWhyMetric = score.formattingSafety
                                }
                            )
                        }

                        // 3d. Dimension 4: Parsing Accuracy (20%)
                        item {
                            AtsExpandableCard(
                                calculation = score.parsingAccuracy,
                                onAskWhyClick = {
                                    selectedWhyMetric = score.parsingAccuracy
                                }
                            )
                        }

                        // 5. Canonical Score Reconciliation (Expandable Breakdown)
                        item {
                            AtsCanonicalScoreReconciliationCard(score = score)
                        }

                        // 6. What's Helping Your Resume
                        item {
                            AtsHelpingFactorsCard(
                                score = score,
                                analysis = analysisData
                            )
                        }

                        // 7. What's Holding Your Resume Back
                        item {
                            AtsHoldingBackCard(
                                score = score,
                                analysis = analysisData
                            )
                        }

                        // 8. Top Priorities / Actionable Booster Plan
                        item {
                            val fixes = score.priorityFixes.ifEmpty {
                                analysisData.prioritizedSuggestions?.highPriority ?: emptyList()
                            }
                            if (fixes.isNotEmpty()) {
                                AtsPriorityFixesCard(
                                    fixes = fixes,
                                    onFixClick = { fix ->
                                        selectedPriorityForDetail = fix
                                    }
                                )
                            }
                        }

                        // 9. Role Alignment & Evidence Snapshot
                        item {
                            AtsRoleAlignmentSnapshotCard(
                                score = score,
                                analysis = analysisData
                            )
                        }

                        // 10. Key ATS AI Insight
                        item {
                            AtsKeyAiInsightCard(
                                score = score,
                                analysis = analysisData,
                                onConsultAiClick = {
                                    navController.navigate(Screen.AiAssistant.route)
                                }
                            )
                        }

                        // 11. ENTITY DATA EXTRACTION (At the absolute bottom as requested)
                        item {
                            AtsEntityExtractionCard(
                                score = score,
                                analysis = analysisData,
                                navController = navController
                            )
                        }

                        // 12. NEXT DASHBOARD NAVIGATION: Continue to Skill Gap & Return to Main Dashboard
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        val route = Screen.SkillGap.route + (analysis?.id?.let { "?analysisId=$it" } ?: "")
                                        navController.navigate(route) 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Continue to Skill Gap",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!navController.popBackStack()) {
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderColor),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Back to Main Dashboard",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                FloatingAstraBot(navController, "ATS Dashboard")
            }

            // Priority Detail Dialog
            if (selectedPriorityForDetail != null) {
                AtsPriorityDetailDialog(
                    priority = selectedPriorityForDetail!!,
                    onDismiss = { selectedPriorityForDetail = null },
                    onAskAi = {
                        selectedPriorityForDetail = null
                        isNovaOpen = true
                    }
                )
            }

            // Immersive Enlarged "Why?" Explanation Modal with Pure Background Blur (No color tint, No dark overlay)
            if (selectedWhyMetric != null) {
                AtsMetricWhyModal(
                    calculation = selectedWhyMetric!!,
                    onDismiss = { selectedWhyMetric = null },
                    onAskAi = {
                        selectedWhyMetric = null
                        isNovaOpen = true
                    }
                )
            }

            // Floating Nova Assistant Button
            NovaFloatingButton(
                onClick = { isNovaOpen = true },
                bottomPadding = 16.dp
            )
        }

        // Nova Overlay Chat Panel
        NovaOverlayChatPanel(
            viewModel = assistantViewModel,
            isOpen = isNovaOpen,
            onDismiss = { isNovaOpen = false }
        )

        // ATS Info Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About ATS Scoring", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "NoviQ evaluates your resume using deterministic parser emulation algorithms weighted across 4 key dimensions:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text("• Keyword Coverage (35%): Role-specific technical terms & competencies", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("• Resume Structure (25%): Standard headings and logical hierarchy", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("• Formatting Safety (20%): Single-column, clean typography, parser safety", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("• Parsing Accuracy (20%): Clean extraction of contact & profile entities", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Got it", fontWeight = FontWeight.Bold, color = Primary)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Surface
            )
        }
    }
}

// ==========================================
// 5. SKILL GAP DASHBOARD SCREEN (Dedicated Career Intelligence Dashboard)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillGapDashboardScreen(
    navController: NavController, 
    viewModel: SkillGapViewModel,
    analysisId: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val assistantViewModel: AiAssistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AppViewModelFactory(app))
    var isNovaOpen by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val selectedSkillForDetail by viewModel.selectedSkillForDetail.collectAsState()
    var showRolePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(analysisId) {
        if (!analysisId.isNullOrBlank()) {
            viewModel.loadAnalysis(analysisId)
        } else {
            viewModel.refresh()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Skill Gap Analysis",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target Competency vs Current Profile",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { showRolePickerDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Change Role",
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Switch Role",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.hasAnalysis) {
                SkillGapEmptyState(navController)
            } else {
                val top3MissingNames = remember(state.priorityLearningItems) {
                    state.priorityLearningItems.take(3).map { it.name }.toSet()
                }
                val remainingMissingCount = state.remainingMissingSkills.size
                val displayedSkills = when (state.filter) {
                    SkillFilter.ALL -> state.allSkillsDetailed.filterNot { it.name in top3MissingNames }
                    SkillFilter.PRESENT -> state.presentSkills
                    SkillFilter.MISSING -> state.remainingMissingSkills
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(2.dp)) }

                    // Header & Target Role Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Skill Gap Analysis",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compares your verified competencies with industry benchmarks for ${state.targetRole} to prioritize your upskilling.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Target Role Banner Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SoftGreen,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.WorkOutline,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "TARGET BENCHMARK ROLE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Text(
                                                text = state.targetRole,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showRolePickerDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MintLight
                                        )
                                    ) {
                                        Text(
                                            text = "Change Role ▾",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Overall Skill Readiness (Development-Oriented, NOT ATS Score)
                    item {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ROLE READINESS INDEX",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Competency Alignment",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    val readinessLevelTag = when {
                                        state.readinessPercentage >= 80 -> "High Readiness"
                                        state.readinessPercentage >= 50 -> "Moderate Alignment"
                                        else -> "Foundational Focus"
                                    }
                                    val tagBg = when {
                                        state.readinessPercentage >= 80 -> SoftGreen
                                        state.readinessPercentage >= 50 -> MintLight
                                        else -> WarningAmber.copy(alpha = 0.12f)
                                    }
                                    val tagColor = when {
                                        state.readinessPercentage >= 80 -> PrimaryDark
                                        state.readinessPercentage >= 50 -> Primary
                                        else -> WarningAmber
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = tagBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = readinessLevelTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = tagColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                val animatedReadiness by animateFloatAsState(
                                    targetValue = state.readinessPercentage / 100f,
                                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                                    label = "ReadinessAnimation"
                                )

                                // Linear Readiness Progression Bar
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "${state.readinessPercentage}%",
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.Black,
                                                color = Primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "benchmark coverage",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }

                                        Text(
                                            text = "${state.matchedCount} of ${state.totalRequired} Skills Present",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MintLight)
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(animatedReadiness)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(PrimaryLight, Primary)
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 2 Direct Breakdown Cards: Present vs Missing
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Present Skills
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SoftGreen.copy(alpha = 0.7f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setFilter(SkillFilter.PRESENT) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("✓", color = PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Present Skills",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryDark
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${state.matchedCount}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = PrimaryDark
                                            )
                                            Text(
                                                text = "Verified in Profile",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = PrimaryDark.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    // 2. Missing Skills
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = WarningAmber.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setFilter(SkillFilter.MISSING) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("○", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Missing Skills",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WarningAmber
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${state.missingCount}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = WarningAmber
                                            )
                                            Text(
                                                text = "Required for Role",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = WarningAmber.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Background,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (state.missingCount == 0) {
                                                "Complete alignment! All benchmark skills for ${state.targetRole} are present."
                                            } else {
                                                "${state.missingCount} benchmark skills are currently missing for ${state.targetRole}."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Priority Missing Competencies
                    if (state.priorityLearningItems.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Top Missing Role Competencies",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "High-priority missing skills for ${state.targetRole}.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MintLight
                                    ) {
                                        Text(
                                            text = "${state.priorityLearningItems.size} Gaps",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.priorityLearningItems.take(3).forEach { skillItem ->
                                        PriorityLearningCard(
                                            skill = skillItem,
                                            onClick = { viewModel.selectDetailedSkill(skillItem) },
                                            onAskAi = {
                                                navController.navigate(Screen.AiAssistant.route)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Filterable Skill Registry (Present vs Remaining Missing)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Benchmark Skill Comparison",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Comparison of required benchmark skills for ${state.targetRole}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3 Clean Filters: All, Present, Missing (Non-duplicated)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MintLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val filterList = listOf(
                                        Triple(SkillFilter.ALL, "All", state.presentSkills.size + remainingMissingCount),
                                        Triple(SkillFilter.PRESENT, "Present", state.matchedCount),
                                        Triple(SkillFilter.MISSING, "Missing", remainingMissingCount)
                                    )

                                    filterList.forEach { (fOption, label, count) ->
                                        val isSelected = state.filter == fOption
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) Primary else Color.Transparent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { viewModel.setFilter(fOption) }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "$label ($count)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Render Filtered Skills List (Top 3 Priority Skills excluded to prevent duplicate presentation)
                    if (displayedSkills.isEmpty()) {
                        item {
                            AstraCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No skills found under the selected filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        items(displayedSkills) { skillItem ->
                            DetailedSkillCard(
                                skill = skillItem,
                                onClick = { viewModel.selectDetailedSkill(skillItem) }
                            )
                        }
                    }

                    // Forward Navigation to JD Matching & Back Navigation
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Primary,
                                shadowElevation = 4.dp
                            ) {
                                Button(
                                    onClick = { navController.navigate(Screen.JobDescriptionAnalyzer.route) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Continue to JD Matching",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { navController.navigate(Screen.AtsAnalysis.route) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Back to ATS Analysis",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }

            // Interactive Skill Detail Dialog
            selectedSkillForDetail?.let { skill ->
                SkillDetailDialog(
                    skill = skill,
                    onDismiss = { viewModel.dismissDetailedSkill() },
                    onAskAi = {
                        viewModel.dismissDetailedSkill()
                        isNovaOpen = true
                    }
                )
            }

            // Target Role Picker Dialog
            if (showRolePickerDialog) {
                TargetRolePickerDialog(
                    currentRole = state.targetRole,
                    availableRoles = RoleSkillsData.ALL_ROLES,
                    onSelectRole = { newRole ->
                        viewModel.changeTargetRole(newRole)
                        showRolePickerDialog = false
                    },
                    onDismiss = { showRolePickerDialog = false }
                )
            }

            // Floating Nova Assistant Button
            NovaFloatingButton(
                onClick = { isNovaOpen = true },
                bottomPadding = 16.dp
            )
        }

        // Nova Overlay Chat Panel
        NovaOverlayChatPanel(
            viewModel = assistantViewModel,
            isOpen = isNovaOpen,
            onDismiss = { isNovaOpen = false }
        )
    }
}

@Composable
fun CategoryGapMeterItem(coverage: SkillCategoryCoverage) {
    val animatedProgress by animateFloatAsState(
        targetValue = coverage.percentage / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "CategoryProgress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = coverage.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${coverage.matchedCount}/${coverage.totalCount} Skills",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (coverage.percentage >= 80) PrimaryDark else if (coverage.percentage >= 50) WarningAmber else ErrorRed
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MintLight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (coverage.percentage >= 80) Primary
                        else if (coverage.percentage >= 50) WarningAmber
                        else Primary.copy(alpha = 0.5f)
                    )
            )
        }
    }
}

@Composable
fun PriorityLearningCard(
    skill: DetailedSkillItem,
    onClick: () -> Unit,
    onAskAi: () -> Unit
) {
    val priorityBadgeColor = when (skill.priority) {
        SkillPriority.CRITICAL -> ErrorRed
        SkillPriority.HIGH -> WarningAmber
        else -> Primary
    }
    val priorityBadgeBg = when (skill.priority) {
        SkillPriority.CRITICAL -> ErrorRed.copy(alpha = 0.1f)
        SkillPriority.HIGH -> WarningAmber.copy(alpha = 0.12f)
        else -> MintLight
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = priorityBadgeBg,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = priorityBadgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = skill.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = skill.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityBadgeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, priorityBadgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${skill.priority} PRIORITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = priorityBadgeColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = skill.whyItMatters,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 17.sp,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Status: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (skill.proficiency == SkillProficiency.MISSING) "Missing Gap" else "Present",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (skill.proficiency == SkillProficiency.MISSING) WarningAmber else PrimaryDark
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Tips & Guide ▾", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedSkillCard(
    skill: DetailedSkillItem,
    onClick: () -> Unit
) {
    val isPresent = skill.proficiency != SkillProficiency.MISSING
    val statusText = if (isPresent) "Present" else "Missing"
    val statusBg = if (isPresent) SoftGreen else WarningAmber.copy(alpha = 0.1f)
    val statusColor = if (isPresent) PrimaryDark else WarningAmber
    val iconSymbol = if (isPresent) "✓" else "○"

    val cardBorderColor = if (isPresent) {
        Primary.copy(alpha = 0.25f)
    } else {
        BorderColor
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusBg,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = iconSymbol,
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = skill.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun SkillDetailDialog(
    skill: DetailedSkillItem,
    onDismiss: () -> Unit,
    onAskAi: () -> Unit
) {
    val isPresent = skill.proficiency != SkillProficiency.MISSING
    val icon = if (isPresent) "✓" else "○"
    val color = if (isPresent) PrimaryDark else WarningAmber
    val bg = if (isPresent) SoftGreen else WarningAmber.copy(alpha = 0.15f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = bg,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = icon,
                                    color = color,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = skill.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = skill.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Status & Priority Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("STATUS", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(
                                text = if (isPresent) "Present in Profile" else "Missing Gap",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("ROLE PRIORITY", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(
                                text = "${skill.priority}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Why it Matters
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "WHY THIS MATTERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = skill.whyItMatters,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }

                // Evidence or Gap Note
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (skill.isMatched) "PROFILE EVIDENCE" else "GAP ANALYSIS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = skill.evidenceOrReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Action Learning Tips
                if (skill.learningTips.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "HOW TO ACQUIRE / MASTER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        skill.learningTips.forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Recommended Free Course Card
                val course = skill.recommendedCourse
                if (course != null) {
                    val dialogContext = LocalContext.current
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "RECOMMENDED FREE COURSE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftGreen.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = course.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "FREE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = PrimaryDark,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Provider: ${course.provider}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )

                                if (course.description.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "WHAT YOU'LL LEARN:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = course.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                if (course.explanation.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "WHY THIS IS RECOMMENDED:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = course.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PrimaryLight,
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(course.courseUrl))
                                            dialogContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle case if no web browser is installed
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "View Course",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onAskAi,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Nova AI", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TargetRolePickerDialog(
    currentRole: String,
    availableRoles: List<String>,
    onSelectRole: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Target Role",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Recalculates your competency readiness & gaps",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = BorderColor)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableRoles.forEach { role ->
                        val isSelected = role.equals(currentRole, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MintLight else Background,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Primary else BorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRole(role) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryDark else TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SkillStatusCard(
    skillName: String,
    isMatched: Boolean,
    targetRole: String,
    onClick: () -> Unit
) {
    val statusBg = if (isMatched) SoftGreen else Surface
    val borderColor = if (isMatched) Primary.copy(alpha = 0.35f) else BorderColor
    val iconColor = if (isMatched) Primary else TextSecondary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = statusBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isMatched) "✓" else "○",
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = skillName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMatched) MintLight else Background,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMatched) Primary.copy(alpha = 0.3f) else BorderColor
                )
            ) {
                Text(
                    text = if (isMatched) "Matched" else "Missing",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMatched) PrimaryDark else TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SkillGapEmptyState(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = SoftGreen,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Primary
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "No Skills Analyzed Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload your resume or enter your background to calculate your role readiness score, identify critical skill gaps, and generate your custom learning path.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        PremiumButton(
            text = "Upload Resume to Analyze Skills",
            onClick = { navController.navigate(Screen.ResumeUpload.route) }
        )
    }
}

// ==========================================
// 6. JD MATCHER DASHBOARD SCREEN (Separate Dashboard)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun JobDescriptionAnalyzerScreen(navController: NavController, viewModel: JdMatcherViewModel) {
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState(initial = JdMatcherUiState.INPUT)
    val jdInput by viewModel.jobDescriptionInput.collectAsState(initial = "")
    val canonicalResume by viewModel.canonicalResume.collectAsState(initial = null)
    val optimizationState by viewModel.optimizationState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
    val analysisStageIndex by viewModel.analysisStageIndex.collectAsState(initial = 0)
    val analysisStages = viewModel.analysisStages

    var selectedSuggestion by remember { mutableStateOf<OptimizationSuggestion?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCanonicalResume()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Resume Optimizer", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Tailor your resume to a specific job",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                JdMatcherUiState.INPUT -> {
                    JdOptimizerInputView(
                        jdInput = jdInput,
                        onJdChanged = { viewModel.onJobDescriptionInputChanged(it) },
                        onAnalyzeClick = { viewModel.analyzeJobDescription() },
                        onPasteClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clipText.isNullOrBlank()) {
                                    viewModel.onJobDescriptionInputChanged(clipText)
                                }
                            } catch (_: Exception) {}
                        },
                        onClearClick = { viewModel.clearInput() },
                        errorMessage = errorMessage,
                        hasStoredResume = canonicalResume != null,
                        onUploadClick = { navController.navigate(Screen.ResumeUpload.route) }
                    )
                }

                JdMatcherUiState.ANALYZING -> {
                    JdAnalysisAnimationView(
                        stageIndex = analysisStageIndex,
                        stages = analysisStages
                    )
                }

                JdMatcherUiState.OPTIMIZING -> {
                    val resume = optimizationState.optimizedResumeData
                    if (resume != null) {
                        ResumeOptimizationDashboard(
                            resume = resume,
                            optimizationState = optimizationState,
                            onSuggestionClick = { selectedSuggestion = it },
                            onGeneratePdf = { viewModel.generateOptimizedResume() },
                            onPrevImprovement = { viewModel.setCurrentImprovement(optimizationState.currentImprovementIndex - 1) },
                            onNextImprovement = { viewModel.setCurrentImprovement(optimizationState.currentImprovementIndex + 1) }
                        )
                    }
                }

                JdMatcherUiState.RESULT -> {
                    OptimizedResumePreview(
                        resume = optimizationState.optimizedResumeData!!,
                        jdTitle = optimizationState.jdTitle,
                        onBack = { viewModel.resetToInput() },
                        onShare = { /* Share logic */ },
                        onSave = { /* Save logic */ }
                    )
                }

                JdMatcherUiState.ERROR -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analysis Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(errorMessage ?: "Unknown error", textAlign = TextAlign.Center, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.retryAnalysis() }) {
                            Text("Retry Analysis")
                        }
                    }
                }
            }

            // Interactive Suggestion Panel
            if (selectedSuggestion != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedSuggestion = null },
                    containerColor = Surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    SuggestionPanel(
                        suggestion = selectedSuggestion!!,
                        onKeepChange = { editedText ->
                            viewModel.updateSuggestionState(selectedSuggestion!!.changeId, SuggestionState.ACCEPTED, editedText)
                            selectedSuggestion = null
                        },
                        onDiscard = {
                            viewModel.updateSuggestionState(selectedSuggestion!!.changeId, SuggestionState.DISMISSED)
                            selectedSuggestion = null
                        }
                    )
                }
            }
        }
    }
}


// ==========================================
// 7. CHATBOT / ASTRAAI ASSISTANT SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(navController: NavController, viewModel: AiAssistantViewModel) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NovaAvatar(size = 38.dp, elevation = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Nova", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text("Your AI Career Assistant", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message ->
                    ModernChatBubble(message.text, message.isFromUser)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            Surface(
                color = Surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Nova anything...", color = TextSecondary) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Primary)
                            .size(46.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ModernChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                NovaAvatar(size = 30.dp, elevation = 1.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Surface(
                color = if (isUser) Primary else Surface,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (isUser) Color.White else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ==========================================
// 8. INTERVIEW PREP & COMMON HELPERS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewPrepScreen(navController: NavController, viewModel: AiAssistantViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isInterviewMode by viewModel.isInterviewMode.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("AI Mock Interview", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isInterviewMode) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AstraCard(modifier = Modifier.padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = SoftGreen,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(40.dp), tint = Primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ready for a Mock Interview?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "Practice technical and behavioral questions tailored to your resume and target role.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            PremiumButton(text = "Start Mock Interview", onClick = { viewModel.startInterview() })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(messages) { message ->
                        ModernChatBubble(message.text, message.isFromUser)
                    }
                }

                Surface(
                    color = Surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your response...", color = TextSecondary) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceVariant,
                                unfocusedContainerColor = SurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Primary)
                                .size(46.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. ATS HELPER COMPOSABLES (REDESIGNED)
// ==========================================

@Composable
fun CandidateProfileCard(
    targetRole: String,
    candidateName: String?,
    candidateEmail: String?,
    candidatePhone: String?,
    candidateLocation: String?
) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!candidateName.isNullOrBlank()) candidateName else "Candidate Resume",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftGreen
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = null,
                                    tint = PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Target: ${targetRole.ifBlank { "Not specified" }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                            }
                        }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 2.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Email
                item {
                    ProfileBadge(
                        icon = Icons.Default.Email,
                        value = candidateEmail ?: "Not found"
                    )
                }
                // Phone
                item {
                    ProfileBadge(
                        icon = Icons.Default.Phone,
                        value = candidatePhone ?: "Not found"
                    )
                }
                // Location
                item {
                    ProfileBadge(
                        icon = Icons.Default.LocationOn,
                        value = candidateLocation ?: "Not found"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
            Spacer(modifier = Modifier.width(5.dp))
            Text(value, style = MaterialTheme.typography.labelSmall, color = if (value == "Not found") ErrorRed else TextSecondary)
        }
    }
}

@Composable
fun AtsOverallScoreHero(
    score: AtsScoreResult,
    onMetricClick: ((AtsCardCalculation) -> Unit)? = null
) {
    val animatedScoreFloat by animateFloatAsState(
        targetValue = score.overallScore / 100f,
        animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        label = "scoreFloat"
    )
    val animatedScoreInt by animateIntAsState(
        targetValue = score.overallScore,
        animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        label = "scoreInt"
    )

    // Using the new Green-based ATS Palette
    val scoreColor = if (score.overallScore >= 70) AtsPrimaryGreen else AtsDarkGreen

    val statusBadge = when {
        score.overallScore >= 85 -> "ATS Optimized (Top 10%)"
        score.overallScore >= 70 -> "Competitive Match"
        score.overallScore >= 50 -> "Moderate ATS Fit"
        else -> "Action Required"
    }

    val estimatedPassRate = ((score.overallScore * 0.95).toInt().coerceIn(10, 99))
    val passProbability = "$estimatedPassRate% Estimated ATS Pass Rate"

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OVERALL ATS COMPATIBILITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.1.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scoreColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Circular Score Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(164.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(164.dp),
                    strokeWidth = 14.dp,
                    color = AtsLightMint,
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { animatedScoreFloat },
                    modifier = Modifier.size(164.dp),
                    strokeWidth = 14.dp,
                    color = AtsPrimaryGreen,
                    trackColor = Color.Transparent
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedScoreInt",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = AtsDarkGreen
                    )
                    Text(
                        text = "out of 100",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pass probability pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = passProbability,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 KPI Quick Badges Grid (Tappable with Why explanation)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KPI 1: Keywords
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = onMetricClick != null) {
                                onMetricClick?.invoke(score.keywordCoverage)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Keywords (35%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${score.keywordCoverage.score}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )
                            Text(
                                text = "+${String.format("%.1f", score.keywordCoverage.weightedContribution)} pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // KPI 2: Structure
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = onMetricClick != null) {
                                onMetricClick?.invoke(score.resumeStructure)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Structure (25%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${score.resumeStructure.score}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )
                            Text(
                                text = "+${String.format("%.1f", score.resumeStructure.weightedContribution)} pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KPI 3: Formatting Safety
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = onMetricClick != null) {
                                onMetricClick?.invoke(score.formattingSafety)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Formatting (20%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${score.formattingSafety.score}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )
                            Text(
                                text = "+${String.format("%.1f", score.formattingSafety.weightedContribution)} pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // KPI 4: Parsing Accuracy
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = onMetricClick != null) {
                                onMetricClick?.invoke(score.parsingAccuracy)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Parsing (20%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${score.parsingAccuracy.score}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )
                            Text(
                                text = "+${String.format("%.1f", score.parsingAccuracy.weightedContribution)} pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prominent "Why this Score?" Button
            Button(
                onClick = { onMetricClick?.invoke(score.toOverallCalculation()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MintLight, contentColor = PrimaryDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Why this Score? (Formulas & Arithmetic Breakdown)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AtsMetricWhyModal(
    calculation: AtsCardCalculation,
    onDismiss: () -> Unit,
    onAskAi: () -> Unit
) {
    val color = AtsDarkGreen

    val iconVector = when (calculation.iconType) {
        AtsCategoryIcon.KEYWORD -> Icons.Default.Key
        AtsCategoryIcon.STRUCTURE -> Icons.Default.Layers
        AtsCategoryIcon.FORMATTING -> Icons.Default.Description
        AtsCategoryIcon.PARSING -> Icons.Default.Code
        AtsCategoryIcon.OVERALL -> Icons.Default.AutoAwesome
    }

    // Pure transparent overlay container - No dark overlay, No blue background, No color tint
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Intercept clicks to prevent dismiss when clicking inside card */ }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = color.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = calculation.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "ATS Evaluation & Deterministic Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Metric Score & Weight Pill Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CATEGORY EVALUATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = PrimaryDark,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = calculation.oneLineSummary.ifBlank {
                                        "${calculation.categoryName} analysis: Evaluated resume against target parser specifications."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${calculation.score}/100",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = color
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SoftGreen
                                ) {
                                    Text(
                                        text = "${calculation.weightPercentage}% Weight (${String.format("%.1f", calculation.weightedContribution)} pts)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Deterministic Formula
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. ATS EVALUATION FORMULA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = calculation.formulaText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // 3. Arithmetic Computation & Math
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "2. ARITHMETIC BREAKDOWN & MATH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MintLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = calculation.calculationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                                if (calculation.resultText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = calculation.resultText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 4. Evidence & Recognized Factors
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "3. DETECTED PROFILE EVIDENCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            letterSpacing = 1.sp
                        )

                        if (calculation.evidenceItems.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                calculation.evidenceItems.forEach { ev ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (ev.isPositive) SoftGreen else ErrorRed.copy(alpha = 0.1f),
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (ev.isPositive) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = if (ev.isPositive) PrimaryDark else ErrorRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ev.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (!ev.detail.isNullOrBlank()) {
                                                Text(
                                                    text = ev.detail,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            if (!ev.suggestion.isNullOrBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lightbulb,
                                                        contentDescription = null,
                                                        tint = WarningAmber,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = ev.suggestion,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = WarningAmber
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (calculation.matchedList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Found Match Keywords (${calculation.matchedList.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(calculation.matchedList) { kw ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SoftGreen
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = PrimaryDark,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = kw,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryDark,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (calculation.missingList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Missing High-Impact Keywords (${calculation.missingList.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(calculation.missingList) { kw ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ErrorRed.copy(alpha = 0.08f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = ErrorRed,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = kw,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ErrorRed,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Actionable Boosters
                    if (calculation.recommendations.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "4. RECOMMENDED BOOSTERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarningAmber.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    calculation.recommendations.forEach { rec ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = WarningAmber,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = rec,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onAskAi,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Nova AI", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AtsExpandableCard(
    calculation: AtsCardCalculation,
    onAskWhyClick: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = calculation.score / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "AtsCategoryProgress"
    )
    val animatedScoreInt by animateIntAsState(
        targetValue = calculation.score,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "AtsCategoryScoreInt"
    )

    val color = AtsDarkGreen

    val iconVector = when (calculation.iconType) {
        AtsCategoryIcon.KEYWORD -> Icons.Default.Key
        AtsCategoryIcon.STRUCTURE -> Icons.Default.Layers
        AtsCategoryIcon.FORMATTING -> Icons.Default.Description
        AtsCategoryIcon.PARSING -> Icons.Default.Code
        AtsCategoryIcon.OVERALL -> Icons.Default.AutoAwesome
    }

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. RESULT HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = color.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = calculation.categoryName,
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
                                    text = "${calculation.weightPercentage}% Weight",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Contributes ${String.format("%.1f", calculation.weightedContribution)} / ${calculation.weightPercentage}.0 pts to total score",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "$animatedScoreInt/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { (animatedProgress).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AtsMediumSage,
                trackColor = AtsLightMint
            )

            // FEATURE ANALYSIS SUMMARY
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Background,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ANALYSIS SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = PrimaryDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = calculation.oneLineSummary.ifBlank {
                                "${calculation.categoryName} analysis: Evaluated resume against target parser specifications."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 2. ASK WHY ACTION BUTTON
            Button(
                onClick = { onAskWhyClick?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintLight,
                    contentColor = PrimaryDark
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Why? (Deep Breakdown)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AtsCanonicalScoreReconciliationCard(score: AtsScoreResult) {
    var expanded by remember { mutableStateOf(false) }

    AstraCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Canonical Score Reconciliation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mathematical sum of all 4 weighted dimensions",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryDark
                )
            }

            // Summary equation banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Formula: (Keyword × 0.35) + (Structure × 0.25) + (Formatting × 0.20) + (Parsing × 0.20)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "= (${score.keywordCoverage.score} × 0.35) + (${score.resumeStructure.score} × 0.25) + (${score.formattingSafety.score} × 0.20) + (${score.parsingAccuracy.score} × 0.20) = ${score.overallScore}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AtsDarkGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(color = BorderColor)

                // 4 Horizontal Contribution Meters
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Keyword
                    ReconciliationDimensionRow(
                        title = "Keyword Coverage (35% Max)",
                        score = score.keywordCoverage.score,
                        pointsEarned = score.keywordCoverage.weightedContribution,
                        maxPoints = 35.0,
                        barColor = Primary
                    )
                    // Structure
                    ReconciliationDimensionRow(
                        title = "Resume Structure (25% Max)",
                        score = score.resumeStructure.score,
                        pointsEarned = score.resumeStructure.weightedContribution,
                        maxPoints = 25.0,
                        barColor = Color(0xFF0284C7)
                    )
                    // Formatting
                    ReconciliationDimensionRow(
                        title = "Formatting Safety (20% Max)",
                        score = score.formattingSafety.score,
                        pointsEarned = score.formattingSafety.weightedContribution,
                        maxPoints = 20.0,
                        barColor = SuccessGreen
                    )
                    // Parsing
                    ReconciliationDimensionRow(
                        title = "Parsing Accuracy (20% Max)",
                        score = score.parsingAccuracy.score,
                        pointsEarned = score.parsingAccuracy.weightedContribution,
                        maxPoints = 20.0,
                        barColor = WarningAmber
                    )

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Final ATS Score",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${score.overallScore} / 100",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = PrimaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationDimensionRow(
    title: String,
    score: Int,
    pointsEarned: Double,
    maxPoints: Double,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(
                "${String.format("%.1f", pointsEarned)} / ${maxPoints.toInt()} pts ($score%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { (pointsEarned / maxPoints).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = SoftGreen
        )
    }
}

@Composable
fun AtsHelpingFactorsCard(
    score: AtsScoreResult,
    analysis: ResumeAnalysisResult
) {
    val strengthsList = mutableListOf<String>()
    analysis.strengths?.filter { it.isNotBlank() }?.let { strengthsList.addAll(it) }

    // Add deterministic positive evidence items
    if (score.keywordCoverage.score >= 70) {
        strengthsList.add("Strong target keyword density: ${score.keywordCoverage.matchedList.size} role competencies detected")
    }
    if (score.resumeStructure.score >= 80) {
        strengthsList.add("Standard resume structure with clear recognizable section headings")
    }
    if (score.formattingSafety.score >= 80) {
        strengthsList.add("Clean single-column formatting passing automated layout parsers")
    }
    if (score.parsingAccuracy.score >= 80) {
        strengthsList.add("Header and contact entities correctly parsed into candidate profile")
    }

    val displayList = strengthsList.distinct().take(6)

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = SoftGreen.copy(alpha = 0.5f),
        borderColor = SuccessGreen.copy(alpha = 0.3f),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What's Helping Your Resume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Parser Advantaged",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "These verified elements protect your application from automated rejection:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 2.dp))

            if (displayList.isEmpty()) {
                Text(
                    text = "No strong ATS advantage factors detected yet. Follow the priority plan below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                displayList.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtsHoldingBackCard(
    score: AtsScoreResult,
    analysis: ResumeAnalysisResult
) {
    val bottlenecks = mutableListOf<String>()

    // Missing keywords
    if (score.keywordCoverage.missingList.isNotEmpty()) {
        val missingSample = score.keywordCoverage.missingList.take(3).joinToString(", ")
        bottlenecks.add("Missing high-frequency target keywords: $missingSample")
    }

    // Weaknesses from AI analysis
    analysis.weaknesses?.filter { it.isNotBlank() }?.let { bottlenecks.addAll(it) }

    // Formatting risks
    analysis.formattingRisks?.filter { it.isNotBlank() }?.forEach { risk ->
        bottlenecks.add("Formatting obstacle: $risk")
    }

    // Missing standard sections
    analysis.missingSections?.filter { it.isNotBlank() }?.forEach { sec ->
        bottlenecks.add("Missing standard section header: $sec")
    }

    val displayBottlenecks = bottlenecks.distinct().take(6)

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarningAmber.copy(alpha = 0.05f),
        borderColor = WarningAmber.copy(alpha = 0.35f),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What's Holding Your Resume Back",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmber.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fix Needed",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Key obstacles causing point deductions in automated ATS parsers:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 2.dp))

            if (displayBottlenecks.isEmpty()) {
                Text(
                    text = "No critical ATS bottlenecks found! Your resume passes standard parsing checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
            } else {
                displayBottlenecks.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtsPriorityFixesCard(
    fixes: List<String>,
    onFixClick: (String) -> Unit = {}
) {
    val checkedStates = remember { mutableStateMapOf<Int, Boolean>() }

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarningAmber.copy(alpha = 0.06f),
        borderColor = WarningAmber.copy(alpha = 0.35f),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "High-Impact ATS Booster Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+15-25 pts Potential",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Apply these ranked steps to maximize resume passing rate (tap any step for details):",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 2.dp))

            fixes.forEachIndexed { index, fix ->
                val isChecked = checkedStates[index] ?: false

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isChecked) SuccessGreen.copy(alpha = 0.08f) else Surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isChecked) SuccessGreen.copy(alpha = 0.3f) else BorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFixClick(fix) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Checkbox / Number badge
                        Surface(
                            shape = CircleShape,
                            color = if (isChecked) SuccessGreen else WarningAmber,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { checkedStates[index] = !isChecked }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isChecked) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = fix,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                            color = if (isChecked) TextSecondary else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "View Details",
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtsRoleAlignmentSnapshotCard(
    score: AtsScoreResult,
    analysis: ResumeAnalysisResult
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val matchedKeywords = score.keywordCoverage.matchedList
    val missingKeywords = score.keywordCoverage.missingList
    val allKeywords = (matchedKeywords + missingKeywords).distinct()

    val filteredList = when (selectedFilter) {
        "MATCHED" -> matchedKeywords
        "MISSING" -> missingKeywords
        else -> allKeywords
    }

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Role Alignment & Evidence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SoftGreen
                ) {
                    Text(
                        text = "${matchedKeywords.size}/${allKeywords.size.coerceAtLeast(1)} Matched",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Competency benchmark for ${score.targetRole}:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterTabPill(
                    text = "All (${allKeywords.size})",
                    isSelected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterTabPill(
                    text = "Matched (${matchedKeywords.size})",
                    isSelected = selectedFilter == "MATCHED",
                    onClick = { selectedFilter = "MATCHED" }
                )
                FilterTabPill(
                    text = "Missing (${missingKeywords.size})",
                    isSelected = selectedFilter == "MISSING",
                    onClick = { selectedFilter = "MISSING" }
                )
            }

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 2.dp))

            // Skills Flow / Grid
            if (filteredList.isEmpty()) {
                Text(
                    text = "No skills in this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(filteredList) { skill ->
                        val isMatched = matchedKeywords.contains(skill)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMatched) SoftGreen else ErrorRed.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isMatched) Primary.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMatched) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isMatched) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isMatched) PrimaryDark else ErrorRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Primary else SurfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun AtsEntityExtractionCard(
    score: AtsScoreResult,
    analysis: ResumeAnalysisResult,
    navController: NavController
) {
    var expanded by remember { mutableStateOf(true) }

    AstraCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = AtsPrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Resume Data Extracted",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Interactive profile entities & extracted sections",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryDark
                )
            }

            if (expanded) {
                HorizontalDivider(color = BorderColor)

                // 1. Candidate Profile Entities Card (Visually unchanged)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = AtsPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CANDIDATE PROFILE ENTITIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AtsDarkGreen,
                                letterSpacing = 0.8.sp
                            )
                        }
                        
                        val name = score.candidateName ?: "Not detected"
                        val email = score.candidateEmail ?: "Not detected"
                        val phone = score.candidatePhone ?: "Not detected"
                        val location = score.candidateLocation ?: "Not detected"
                        
                        EntityDiagnosticRow("Name", name, score.candidateName != null)
                        EntityDiagnosticRow("Email", email, score.candidateEmail != null)
                        EntityDiagnosticRow("Phone", phone, score.candidatePhone != null)
                        EntityDiagnosticRow("Location", location, score.candidateLocation != null)
                    }
                }

                // 2. Standard Sections Detected (Redesigned as independent clickable cards)
                Text(
                    text = "RESUME SECTIONS DETECTED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AtsDarkGreen,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val sections = listOf(
                    Triple("Summary", Icons.Default.Description, Screen.SummaryDetail.route),
                    Triple("Work Experience", Icons.Default.Work, Screen.ExperienceDetail.route),
                    Triple("Education", Icons.Default.School, Screen.EducationDetail.route),
                    Triple("Skills", Icons.Default.Psychology, Screen.SkillsDetail.route),
                    Triple("Projects", Icons.Default.Architecture, Screen.ProjectsDetail.route)
                )
                
                sections.forEach { (name, icon, route) ->
                    val isDetected = when(name) {
                        "Summary" -> !analysis.summary.isNullOrBlank()
                        "Work Experience" -> !analysis.experience.isNullOrEmpty()
                        "Education" -> !analysis.education.isNullOrEmpty()
                        "Skills" -> !analysis.extractedSkills.isNullOrEmpty()
                        "Projects" -> !analysis.projectAnalysis.isNullOrEmpty()
                        else -> true
                    }
                    
                    val countText = when(name) {
                        "Summary" -> if (isDetected) "1 section extracted" else "Not detected"
                        "Work Experience" -> if (isDetected) "${analysis.experience?.size} entries extracted" else "No experience detected"
                        "Education" -> if (isDetected) "${analysis.education?.size} entries extracted" else "Not detected"
                        "Skills" -> if (isDetected) "${analysis.extractedSkills?.size} skills extracted" else "Not detected"
                        "Projects" -> if (isDetected) "${analysis.projectAnalysis?.size} projects extracted" else "No projects detected"
                        else -> "Not detected"
                    }

                    ResumeSectionNavigationCard(
                        name = name,
                        countText = countText,
                        icon = icon,
                        isDetected = isDetected,
                        onClick = { navController.navigate(route) }
                    )
                }

                // 3. Extraction Summary & Parser Text Stats Card
                val rawTextLength = analysis.rawResumeText?.length ?: 0
                val wordCount = if (rawTextLength > 0) analysis.rawResumeText!!.split("\\s+".toRegex()).size else 0

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EXTRACTION SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Characters", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$rawTextLength", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PrimaryDark)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Estimated Words", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("$wordCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PrimaryDark)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Extracted Skills", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${analysis.extractedSkills?.size ?: 0}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PrimaryDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeSectionNavigationCard(
    name: String,
    countText: String,
    icon: ImageVector,
    isDetected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDetected) BorderColorGreen else BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDetected) MintLight else SurfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDetected) AtsPrimaryGreen else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDetected) AtsPrimaryGreen else TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EntityDiagnosticRow(
    label: String,
    value: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSuccess) SuccessGreen else ErrorRed,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSuccess) TextSecondary else ErrorRed
            )
        }
    }
}

@Composable
fun AtsKeyAiInsightCard(
    score: AtsScoreResult,
    analysis: ResumeAnalysisResult,
    onConsultAiClick: () -> Unit
) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Surface,
        borderColor = Primary.copy(alpha = 0.35f),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NovaAvatar(size = 36.dp, elevation = 1.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nova AI Executive Career Insight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Deterministic parser strategy summary",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val highestLeverageTweak = when {
                        score.keywordCoverage.score < 70 -> "Your single highest leverage improvement is adding the ${score.keywordCoverage.missingList.size} missing target role keywords into your Experience and Skills sections."
                        score.resumeStructure.score < 80 -> "Adding standard header sections will immediately raise your ATS score above 80%."
                        score.formattingSafety.score < 80 -> "Simplifying your resume into a standard single-column text format eliminates parser drops."
                        else -> "Your resume is highly optimized for ATS parsers. Focus on quantifying bullet points with impact metrics."
                    }

                    Text(
                        text = highestLeverageTweak,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            TextButton(
                onClick = onConsultAiClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Consult Nova for Strategy →",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}

@Composable
fun AtsPriorityDetailDialog(
    priority: String,
    onDismiss: () -> Unit,
    onAskAi: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Actionable Step",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = priority,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Why this matters for ATS parsers:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Text(
                        text = "Automated applicant tracking systems evaluate keyword placement, semantic structure, and plain-text entity extraction. Completing this fix increases your pass score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = TextPrimary)
                    }
                    Button(
                        onClick = onAskAi,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ask AI to Fix", color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. FLOATING BOT & STUB / UTILITY COMPOSABLES
// ==========================================

@Composable
fun BoxScope.FloatingAstraBot(navController: NavController, screenContext: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary.copy(alpha = 0.35f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(18.dp)
            .clickable { navController.navigate(Screen.AiAssistant.route) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NovaAvatar(size = 28.dp, showBorder = false)
            Column {
                Text(
                    text = "Nova AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Ask Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun NoAnalysisState(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = SoftGreen,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Primary
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "No Analysis Data Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Upload your resume in the setup screen to compute your ATS score and skill gaps.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        PremiumButton(
            text = "Upload Resume & Role",
            onClick = { navController.navigate(Screen.ResumeUpload.route) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StubScreen(title: String, navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = SoftGreen,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$title Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Logged in as: ${sessionManager.getCurrentEmail() ?: "User"}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            PremiumButton(
                text = "Sign Out",
                onClick = {
                    sessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                modifier = Modifier.width(220.dp),
                containerColor = ErrorRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResumeSectionDetailScreen(
    title: String,
    navController: NavController,
    viewModel: AtsViewModel
) {
    val analysis by viewModel.analysisResult.collectAsState()
    
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Resume Data Extracted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            val currentAnalysis = analysis
            if (currentAnalysis == null) {
                item { Text("No data available.", color = TextSecondary) }
            } else {
                when (title) {
                    "Summary" -> {
                        item {
                            DetailContentCard(
                                countText = "1 section extracted",
                                content = {
                                    Text(
                                        text = currentAnalysis.summary ?: "Not found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        lineHeight = 22.sp
                                    )
                                }
                            )
                        }
                    }
                    "Work Experience" -> {
                        val items = currentAnalysis.experience ?: emptyList()
                        if (items.isEmpty()) {
                            item { Text("No experience detected", color = TextSecondary) }
                        } else {
                            items(items) { exp ->
                                DetailContentCard(
                                    title = "Work Experience Entry",
                                    content = {
                                        Text(text = exp, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    }
                                )
                            }
                        }
                    }
                    "Education" -> {
                        val items = currentAnalysis.education ?: emptyList()
                        if (items.isEmpty()) {
                            item { Text("No education detected", color = TextSecondary) }
                        } else {
                            items(items) { edu ->
                                DetailContentCard(
                                    title = "Education Entry",
                                    content = {
                                        Text(text = edu, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    }
                                )
                            }
                        }
                    }
                    "Skills" -> {
                        item {
                            DetailContentCard(
                                countText = "${currentAnalysis.extractedSkills?.size ?: 0} skills extracted",
                                content = {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        currentAnalysis.extractedSkills?.forEach { skill ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = AtsLightMint,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AtsMediumSage.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = skill.name,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AtsDarkGreen,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    "Projects" -> {
                        val items = currentAnalysis.projectAnalysis ?: emptyList()
                        if (items.isEmpty()) {
                            item { Text("No projects detected", color = TextSecondary) }
                        } else {
                            items(items) { proj ->
                                DetailContentCard(
                                    title = proj.name,
                                    content = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (!proj.technologies.isNullOrEmpty()) {
                                                Text(
                                                    text = "Technologies: ${proj.technologies.joinToString(", ")}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (!proj.demonstratedSkills.isNullOrEmpty()) {
                                                Text(
                                                    text = "Demonstrated Skills: ${proj.demonstratedSkills.joinToString(", ")}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            proj.strengths?.forEach { str ->
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = str, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SourceIndicator()
            }
        }
    }
}

@Composable
private fun DetailContentCard(
    title: String? = null,
    countText: String? = null,
    content: @Composable () -> Unit
) {
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null || countText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AtsDarkGreen)
                    }
                    if (countText != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MintLight) {
                            Text(
                                text = countText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AtsPrimaryGreen
                            )
                        }
                    }
                }
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            }
            content()
        }
    }
}

@Composable
private fun SourceIndicator() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = AtsPrimaryGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Source", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(text = "Uploaded Resume", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AtsDarkGreen)
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun JdOptimizerInputView(
    jdInput: String,
    onJdChanged: (String) -> Unit,
    onAnalyzeClick: () -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    errorMessage: String?,
    hasStoredResume: Boolean,
    onUploadClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tailor your resume to a job",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Paste a job description and NoviQ will identify specific parts of your resume that can be improved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (!hasStoredResume) {
            item {
                AstraCard(containerColor = Color(0xFFFEF3C7), borderColor = WarningAmber.copy(alpha = 0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("No Resume Found", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                        Text("Analyze your resume first so NoviQ can optimize it.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                        Button(onClick = onUploadClick, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("Upload Resume", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = jdInput,
                        onValueChange = onJdChanged,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
                        placeholder = {
                            Text(
                                "Paste job description here...\n\nExample:\nWe are looking for an Android Developer with 3+ years of experience in Kotlin and Jetpack Compose...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onPasteClick, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste from Clipboard", style = MaterialTheme.typography.labelSmall)
                        }

                        if (jdInput.isNotBlank()) {
                            TextButton(onClick = onClearClick) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            item {
                Text(errorMessage, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            PremiumButton(
                text = "Analyze Resume",
                onClick = onAnalyzeClick,
                enabled = hasStoredResume && jdInput.isNotBlank()
            )
        }
        
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun JdAnalysisAnimationView(
    stageIndex: Int,
    stages: List<String>
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AstraCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Resume Scanner Animation
                Box(
                    modifier = Modifier.size(width = 180.dp, height = 240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ResumeScannerMockup(isScanning = true)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AI Analysis in Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Tailoring your resume for the best alignment", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stages.forEachIndexed { index, stage ->
                        val isDone = index < stageIndex
                        val isCurrent = index == stageIndex
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().alpha(if (isDone || isCurrent) 1f else 0.4f)
                        ) {
                            if (isDone) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            } else if (isCurrent) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                            } else {
                                Box(modifier = Modifier.size(18.dp).background(BorderColor, CircleShape))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) Primary else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResumeScannerMockup(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(10.dp).background(SurfaceVariant))
            Box(modifier = Modifier.fillMaxWidth(0.3f).height(6.dp).background(SurfaceVariant.copy(alpha = 0.6f)))
            Spacer(modifier = Modifier.height(16.dp))
            repeat(8) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(SurfaceVariant.copy(alpha = 0.4f)))
            }
        }

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .offset(y = 240.dp * scanProgress)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Primary, Color.Transparent)))
                    .shadow(4.dp, spotColor = Primary)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(scanProgress)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Primary.copy(alpha = 0.05f))))
            )
        }
    }
}

@Composable
fun ResumeOptimizationDashboard(
    resume: ResumeAnalysisResult,
    optimizationState: ResumeOptimizationState,
    onSuggestionClick: (OptimizationSuggestion) -> Unit,
    onGeneratePdf: () -> Unit,
    onPrevImprovement: () -> Unit,
    onNextImprovement: () -> Unit
) {
    val suggestions = optimizationState.suggestions
    val totalImprovements = suggestions.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary bar
        Surface(color = Surface, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("$totalImprovements targeted improvements found", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Tap highlighted areas to review suggestions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }

        // Resume View Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (resume.layoutInfo?.pdfUri != null) {
                InteractiveResumeViewer(
                    resume = resume,
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick,
                    currentSuggestionIndex = optimizationState.currentImprovementIndex
                )
            } else {
                // Fallback to text-based if no PDF
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryChip("${optimizationState.skillGaps.size} Gaps", WarningAmber)
                            SummaryChip("${optimizationState.keywordOpportunities.size} Keywords", SuccessGreen)
                            SummaryChip("${suggestions.count { it.state != SuggestionState.UNREVIEWED }} Reviewed", Primary)
                        }
                    }
                    item {
                        OptimizableResumeDocument(
                            resume = resume,
                            suggestions = suggestions,
                            onSuggestionClick = onSuggestionClick
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }

        // Bottom Action
        Surface(color = Surface, shadowElevation = 8.dp) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onGeneratePdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Generate Optimized Resume", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InteractiveResumeViewer(
    resume: ResumeAnalysisResult,
    suggestions: List<OptimizationSuggestion>,
    onSuggestionClick: (OptimizationSuggestion) -> Unit,
    currentSuggestionIndex: Int
) {
    val layout = resume.layoutInfo ?: return
    val pdfUriStr = layout.pdfUri ?: return
    val pdfUri = Uri.parse(pdfUriStr)
    val context = LocalContext.current
    
    val scrollState = rememberScrollState()
    var pageCount by remember { mutableIntStateOf(0) }
    val pageSizes = remember { mutableStateMapOf<Int, Size>() }
    var renderError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = if (pdfUri.scheme == "file") {
                    android.os.ParcelFileDescriptor.open(java.io.File(pdfUri.path!!), android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    context.contentResolver.openFileDescriptor(pdfUri, "r")
                }
                
                pfd?.use {
                    val renderer = PdfRenderer(it)
                    pageCount = renderer.pageCount
                    renderer.close()
                } ?: run { renderError = "Could not open resume file" }
            } catch (e: Exception) {
                Log.e("InteractiveResumeViewer", "Failed to count pages", e)
                renderError = "Error loading resume: ${e.message}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE5E5E5))) {
        if (renderError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(renderError!!, textAlign = TextAlign.Center, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { renderError = null }) { Text("Retry") }
            }
        } else if (pageCount == 0) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
        } else {
            Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (i in 0 until pageCount) {
                        Box(
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(2.dp))
                                .background(Color.White)
                                .fillMaxWidth()
                                .aspectRatio(if (pageSizes[i] != null) pageSizes[i]!!.width / pageSizes[i]!!.height else 0.707f)
                        ) {
                            PdfPageWithOverlays(
                                pdfUri = pdfUri,
                                pageIndex = i,
                                suggestions = suggestions,
                                layout = layout,
                                onSuggestionClick = onSuggestionClick,
                                onPageMeasured = { size -> pageSizes[i] = size },
                                currentSuggestionIndex = currentSuggestionIndex
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.01f)
                .align(Alignment.TopCenter)
                .offset(y = 1000.dp * scanY)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Primary, Color.Transparent)))
        )
    }
}

@Composable
fun PdfPageWithOverlays(
    pdfUri: Uri,
    pageIndex: Int,
    suggestions: List<OptimizationSuggestion>,
    layout: ResumeLayout,
    onSuggestionClick: (OptimizationSuggestion) -> Unit,
    onPageMeasured: (Size) -> Unit,
    currentSuggestionIndex: Int
) {
    val context = LocalContext.current
    val bitmapState = remember(pdfUri, pageIndex) { mutableStateOf<Bitmap?>(null) }
    
    var pageWidth by remember { mutableFloatStateOf(612f) }
    var pageHeight by remember { mutableFloatStateOf(792f) }
    
    // Animation states
    var startHighlightAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300) // Small delay after page appearing
        startHighlightAnimation = true
    }

    LaunchedEffect(pdfUri, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = if (pdfUri.scheme == "file") {
                    android.os.ParcelFileDescriptor.open(java.io.File(pdfUri.path!!), android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    context.contentResolver.openFileDescriptor(pdfUri, "r")
                }

                pfd?.use {
                    val renderer = PdfRenderer(it)
                    val page = renderer.openPage(pageIndex)
                    
                    pageWidth = page.width.toFloat()
                    pageHeight = page.height.toFloat()
                    onPageMeasured(Size(pageWidth, pageHeight))

                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmapState.value = bitmap
                    
                    page.close()
                    renderer.close()
                } ?: run {
                    Log.e("PdfPageWithOverlays", "Failed to open ParcelFileDescriptor for $pdfUri")
                }
            } catch (e: Exception) {
                Log.e("PdfPageWithOverlays", "Failed to render page $pageIndex: ${e.message}", e)
            }
        }
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        val density = LocalContext.current.resources.displayMetrics.density
        
        bitmapState.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Red Sweep Animation Progress
        val sweepProgress = animateFloatAsState(
            targetValue = if (startHighlightAnimation) 1.2f else -0.2f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        )
        
        // Pulsing animation for highlights
        val infiniteTransition = rememberInfiniteTransition()
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = canvasWidth / pageWidth
            val scaleY = canvasHeight / pageHeight
            
            suggestions.forEachIndexed { index, suggestion ->
                val rects = findTextRectsForSuggestion(suggestion, layout, pageIndex)
                
                rects.forEach { rect ->
                    val isUnreviewed = suggestion.state == SuggestionState.UNREVIEWED
                    
                    if (isUnreviewed) {
                        val drawRect = androidx.compose.ui.geometry.Rect(
                            rect.left * scaleX, rect.top * scaleY,
                            rect.right * scaleX, rect.bottom * scaleY
                        )
                        
                        // Red Highlight Sweep Effect
                        // We highlight only if sweepProgress has reached this vertical position
                        // Or we can just use sweepProgress as a global X sweep.
                        // User said: "Red highlight sweeps across affected text"
                        
                        val relativeTop = drawRect.top / canvasHeight
                        val highlightAlpha = if (sweepProgress.value > relativeTop - 0.1f) pulseAlpha else 0f
                        
                        drawRoundRect(
                            color = ErrorRed.copy(alpha = highlightAlpha),
                            topLeft = Offset(drawRect.left - 2.dp.toPx(), drawRect.top - 1.dp.toPx()),
                            size = Size(drawRect.width + 4.dp.toPx(), drawRect.height + 2.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                        
                        // Subtle red border sweep
                        if (sweepProgress.value in relativeTop - 0.05f..relativeTop + 0.05f) {
                            drawRoundRect(
                                color = ErrorRed.copy(alpha = 0.6f),
                                topLeft = Offset(drawRect.left - 2.dp.toPx(), drawRect.top - 1.dp.toPx()),
                                size = Size(drawRect.width + 4.dp.toPx(), drawRect.height + 2.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
        
        // Replacement Text Overlays (Seamless merging)
        suggestions.filter { it.state == SuggestionState.ACCEPTED || it.state == SuggestionState.EDITED }.forEach { suggestion ->
            val rects = findTextRectsForSuggestion(suggestion, layout, pageIndex)
            if (rects.isNotEmpty()) {
                val scaleX = canvasWidth / pageWidth
                val scaleY = canvasHeight / pageHeight
                
                rects.forEach { lineRect ->
                    // Cover original text with white background (matching resume usually)
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (lineRect.left * scaleX / density).dp,
                                y = (lineRect.top * scaleY / density).dp
                            )
                            .size(
                                width = (lineRect.width * scaleX / density).dp,
                                height = (lineRect.height * scaleY / density).dp
                            )
                            .background(Color.White) // Assuming white background for seamless merging
                    )
                }
                
                // Draw new text at the start position of the first rect
                val firstRect = rects.first()
                val totalWidth = rects.maxOf { it.right } - rects.minOf { it.left }
                val totalHeight = rects.maxOf { it.bottom } - rects.minOf { it.top }
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = (rects.minOf { it.left } * scaleX / density).dp,
                            y = (rects.minOf { it.top } * scaleY / density).dp
                        )
                        .size(
                            width = (totalWidth * scaleX / density).dp,
                            height = (totalHeight * scaleY / density).dp
                        )
                        .padding(horizontal = 1.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = suggestion.manualText ?: suggestion.suggestedText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (firstRect.height * scaleY / density * 0.75f).sp, // Slightly smaller than box to fit
                            lineHeight = (firstRect.height * scaleY / density).sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF222222) // Professional dark gray/black
                        ),
                        maxLines = rects.size,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Interactive hit areas
        suggestions.forEach { suggestion ->
            if (suggestion.state == SuggestionState.UNREVIEWED) {
                val rects = findTextRectsForSuggestion(suggestion, layout, pageIndex)
                rects.forEach { rect ->
                    val scaleX = canvasWidth / pageWidth
                    val scaleY = canvasHeight / pageHeight
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (rect.left * scaleX / density).dp,
                                y = (rect.top * scaleY / density).dp
                            )
                            .size(
                                width = (rect.width * scaleX / density).dp,
                                height = (rect.height * scaleY / density).dp
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSuggestionClick(suggestion)
                            }
                    )
                }
            }
        }
    }
}

fun findTextRectsForSuggestion(suggestion: OptimizationSuggestion, layout: ResumeLayout, pageIndex: Int): List<Rect> {
    val query = suggestion.originalText.lowercase().trim()
    if (query.isEmpty()) return emptyList()
    
    val pagePositions = layout.textPositions.filter { it.pageIndex == pageIndex }
    if (pagePositions.isEmpty()) return emptyList()

    val fullText = pagePositions.joinToString("") { it.text }
    val startIndex = fullText.lowercase().indexOf(query)
    
    if (startIndex == -1) return emptyList()
    
    val matchedPositions = mutableListOf<com.example.aidrivencompetencyplatform.model.TextPosition>()
    var charCount = 0
    for (pos in pagePositions) {
        val nextCount = charCount + pos.text.length
        if (nextCount > startIndex && charCount < startIndex + query.length) {
            matchedPositions.add(pos)
        }
        charCount = nextCount
        if (charCount >= startIndex + query.length) break
    }
    
    if (matchedPositions.isEmpty()) return emptyList()

    // Group by Y to handle multi-line highlights
    return matchedPositions.groupBy { it.y }.values.map { linePos ->
        Rect(
            left = linePos.minOf { it.x },
            top = linePos.minOf { it.y },
            right = linePos.maxOf { it.x + it.width },
            bottom = linePos.maxOf { it.y + it.height }
        )
    }
}

@Composable
fun OptimizableResumeDocument(
    resume: ResumeAnalysisResult,
    suggestions: List<OptimizationSuggestion>,
    onSuggestionClick: (OptimizationSuggestion) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Text(resume.candidateName ?: "Candidate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("${resume.candidateEmail ?: ""} | ${resume.candidatePhone ?: ""} | ${resume.candidateLocation ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Summary
            ResumeSectionHeader("PROFESSIONAL SUMMARY")
            HighlightableResumeText(
                text = resume.summary ?: "",
                section = "SUMMARY",
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Experience
            ResumeSectionHeader("WORK EXPERIENCE")
            resume.experience?.forEach { exp ->
                HighlightableResumeText(
                    text = exp,
                    section = "EXPERIENCE",
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Projects
            ResumeSectionHeader("PROJECTS")
            resume.projectAnalysis?.forEach { proj ->
                Text(proj.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                HighlightableResumeText(
                    text = proj.strengths?.joinToString("; ") ?: "",
                    section = "PROJECTS",
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Skills
            ResumeSectionHeader("TECHNICAL SKILLS")
            val skillsText = resume.extractedSkills?.joinToString(", ") { it.name } ?: ""
            HighlightableResumeText(
                text = skillsText,
                section = "SKILLS",
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick
            )
        }
    }
}

@Composable
fun ResumeSectionHeader(title: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Primary, letterSpacing = 1.sp)
        HorizontalDivider(color = Primary.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun HighlightableResumeText(
    text: String,
    section: String,
    suggestions: List<OptimizationSuggestion>,
    onSuggestionClick: (OptimizationSuggestion) -> Unit
) {
    val relevantSuggestions = suggestions.filter { 
        it.section.uppercase() == section.uppercase() && text.contains(it.originalText) 
    }
    
    if (relevantSuggestions.isEmpty()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.Black, lineHeight = 20.sp)
    } else {
        val annotatedString = buildAnnotatedString {
            var currentIndex = 0
            val sortedSuggestions = relevantSuggestions.sortedBy { text.indexOf(it.originalText) }
            
            for (suggestion in sortedSuggestions) {
                val startIndex = text.indexOf(suggestion.originalText, currentIndex)
                if (startIndex >= currentIndex) {
                    append(text.substring(currentIndex, startIndex))
                    
                    val highlightColor = when (suggestion.state) {
                        SuggestionState.ACCEPTED, SuggestionState.EDITED -> SuccessGreen.copy(alpha = 0.2f)
                        SuggestionState.DISMISSED, SuggestionState.ORIGINAL_KEPT -> Color.Transparent
                        else -> Primary.copy(alpha = 0.15f)
                    }
                    
                    pushStringAnnotation(tag = "SUGGESTION", annotation = suggestion.changeId)
                    withStyle(style = SpanStyle(
                        background = highlightColor,
                        fontWeight = if (suggestion.state == SuggestionState.UNREVIEWED) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (suggestion.state == SuggestionState.UNREVIEWED) androidx.compose.ui.text.style.TextDecoration.Underline else null
                    )) {
                        append(suggestion.originalText)
                    }
                    pop()
                    currentIndex = startIndex + suggestion.originalText.length
                }
            }
            append(text.substring(currentIndex))
        }
        
        androidx.compose.foundation.text.ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, lineHeight = 20.sp),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "SUGGESTION", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        suggestions.find { it.changeId == annotation.item }?.let { onSuggestionClick(it) }
                    }
            }
        )
    }
}

@Composable
fun SuggestionPanel(
    suggestion: OptimizationSuggestion,
    onKeepChange: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var editedText by remember { mutableStateOf(suggestion.manualText ?: suggestion.suggestedText) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Suggested Improvement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("ORIGINAL", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Text(
                suggestion.originalText,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("SUGGESTED (EDITABLE)", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = editedText,
            onValueChange = { editedText = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Primary.copy(alpha = 0.02f),
                focusedContainerColor = Primary.copy(alpha = 0.02f),
                unfocusedBorderColor = Primary.copy(alpha = 0.2f),
                focusedBorderColor = Primary
            )
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text("WHY THIS CHANGE?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
        Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
            ) {
                Text("Discard", color = TextSecondary)
            }
            
            Button(
                onClick = { onKeepChange(editedText) },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Keep Change", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SuggestionBox(label: String, content: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = color.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Text(
                content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun OptimizedResumePreview(
    resume: ResumeAnalysisResult,
    jdTitle: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Optimized Resume Ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Tailored for $jdTitle", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            if (resume.layoutInfo?.pdfUri != null) {
                InteractiveResumeViewer(
                    resume = resume,
                    suggestions = emptyList(), // Final view
                    onSuggestionClick = {},
                    currentSuggestionIndex = -1
                )
            } else {
                OptimizableResumeDocument(resume, emptyList(), {})
            }
        }
        
        Surface(color = Surface, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back to Editor")
                }
                Button(onClick = onSave, modifier = Modifier.weight(1.5f), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save PDF")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, null, tint = Primary)
                }
            }
        }
    }
}

