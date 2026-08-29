package com.example.aidrivencompetencyplatform.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
                        Surface(
                            shape = CircleShape,
                            color = SoftGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AstraAI Guided Tour",
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
                            placeholder = { Text("Ask Astra anything about the app...", fontSize = 14.sp) },
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

    LaunchedEffect(Unit) {
        dashboardViewModel.refreshUser()
    }

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

                    // 6. Assistant
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.AiAssistant.route) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Assistant",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Astra",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Curved Sage/Mint Header with Personalized Greeting
            item {
                val displayName = if (userName.isNotBlank()) userName else "User"
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
                                    onClick = { navController.navigate(Screen.ResumeUpload.route) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Analyze Resume",
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
                            onViewAtsClick = { navController.navigate(Screen.AtsAnalysis.route) },
                            onViewSkillGapClick = { navController.navigate(Screen.SkillGap.route) }
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
                                navController.navigate(Screen.AtsAnalysis.route)
                            },
                            onAnalyzeNewClick = {
                                navController.navigate(Screen.ResumeUpload.route)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
                    Surface(
                        shape = CircleShape,
                        color = SoftGreen,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Astra AI",
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hi, I'm Astra",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your AI Career Copilot",
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
                        Surface(
                            shape = CircleShape,
                            color = SoftGreen,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Astra",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Astra Tour Guide",
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
                            color = Primary
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
                            color = Primary
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
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ASTRAAI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
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
// 3. DEDICATED UPLOAD RESUME DASHBOARD & INTERACTIVE 5-STAGE PROGRESS MODAL
// ==========================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ResumeAnalysisProgressModal(
    isLoading: Boolean = true,
    interactiveState: InteractiveAnalysisState,
    targetRole: String
) {
    if (!isLoading) return

    val stageIndex = interactiveState.stage
    val currentStage = stageIndex.coerceIn(0, 4)

    val stageHeaders = listOf(
        "Parsing Your Resume",
        "Identifying Your Skills",
        "Evaluating ATS Compatibility",
        "Matching Your Profile",
        "Analysis Complete"
    )

    val stageDescriptions = listOf(
        "Detecting sections, document structure, and entity blocks",
        "Extracting programming languages, frameworks, and tools",
        "Calculating deterministic ATS score and formatting metrics",
        "Benchmarking profile against $targetRole standards",
        "Synthesized all 4 intelligence dimensions"
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
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top 5-Stage Step Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..4) {
                        val isDone = i < currentStage
                        val isCurrent = i == currentStage
                        
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isDone -> SoftGreen
                                isCurrent -> Primary
                                else -> SurfaceVariant
                            },
                            border = if (!isDone && !isCurrent) androidx.compose.foundation.BorderStroke(1.dp, BorderColor) else null,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    isDone -> Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    isCurrent -> Text(
                                        text = "${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    else -> Text(
                                        text = "${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        if (i < 4) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                color = if (i < currentStage) Primary else BorderColor,
                                thickness = 2.dp
                            )
                        }
                    }
                }

                // Stage Header with animated title
                AnimatedContent(
                    targetState = currentStage,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "StageHeader"
                ) { stage ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MintLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "Stage ${stage + 1} of 5",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stageHeaders[stage],
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stageDescriptions[stage],
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.8f))

                // Interactive Dynamic Content per Stage
                AnimatedContent(
                    targetState = currentStage,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith (slideOutVertically { height -> -height / 2 } + fadeOut())
                    },
                    label = "StageContent"
                ) { stage ->
                    when (stage) {
                        0 -> {
                            // STAGE 1: PARSING YOUR RESUME
                            val sections = if (interactiveState.sections.isNotEmpty()) {
                                interactiveState.sections
                            } else {
                                listOf(
                                    ParsedSectionItem("Contact Information", true, listOf("Name, Email, Phone"), "Name, Email, Phone"),
                                    ParsedSectionItem("Education", true, listOf("Degrees & Institutions"), "Degrees & Institutions"),
                                    ParsedSectionItem("Work Experience", true, listOf("Employment History"), "Employment History"),
                                    ParsedSectionItem("Skills", true, listOf("Technical Competencies"), "Technical Competencies"),
                                    ParsedSectionItem("Projects", true, listOf("Domain Digest, PALM"), "Domain Digest, PALM"),
                                    ParsedSectionItem("Certifications", true, listOf("Verified Credentials"), "Verified Credentials"),
                                    ParsedSectionItem("Languages", true, listOf("English"), "English, etc."),
                                    ParsedSectionItem("Patents & Publications", true, listOf("Academic/Industry"), "Academic/Industry")
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (interactiveState.candidateName.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MintLight,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Candidate: ${interactiveState.candidateName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDark
                                            )
                                        }
                                    }
                                }

                                sections.take(6).forEach { section ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceVariant, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = section.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                        if (!section.summary.isNullOrBlank()) {
                                            Text(
                                                text = section.summary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // STAGE 2: IDENTIFYING YOUR SKILLS
                            val skills = if (interactiveState.extractedSkills.isNotEmpty()) {
                                interactiveState.extractedSkills
                            } else {
                                listOf("Kotlin", "Jetpack Compose", "Java", "Android SDK", "REST APIs", "Git", "Coroutines", "MVVM", "Room DB", "Dagger Hilt")
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Extracted Competencies",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SoftGreen
                                    ) {
                                        Text(
                                            text = "${skills.size} Found",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(skills.take(12)) { skill ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MintLight,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = skill,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PrimaryDark
                                                )
                                            }
                                        }
                                    }
                                }

                                if (interactiveState.detectedProjects.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceVariant, RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Projects Identified:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = interactiveState.detectedProjects.joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // STAGE 3: EVALUATING ATS COMPATIBILITY
                            val targetAts = if (interactiveState.atsScore > 0) interactiveState.atsScore else 84
                            val targetKeyword = if (interactiveState.keywordScore > 0) interactiveState.keywordScore else 85
                            val targetStructure = if (interactiveState.structureScore > 0) interactiveState.structureScore else 90
                            val targetFormatting = if (interactiveState.formattingScore > 0) interactiveState.formattingScore else 85
                            val targetParsing = if (interactiveState.parsingScore > 0) interactiveState.parsingScore else 88

                            val animatedScore by animateIntAsState(
                                targetValue = targetAts,
                                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                label = "atsScoreAnim"
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Overall ATS Compatibility",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$animatedScore/100",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { animatedScore / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Primary,
                                    trackColor = SurfaceVariant,
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Keyword Coverage" to targetKeyword,
                                        "Resume Structure" to targetStructure,
                                        "Formatting Safety" to targetFormatting,
                                        "Parsing Accuracy" to targetParsing
                                    ).forEach { (label, value) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text(text = "$value%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryDark)
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // STAGE 4: MATCHING YOUR PROFILE
                            val targetMatch = if (interactiveState.skillMatch > 0) interactiveState.skillMatch else 78
                            val animatedMatch by animateIntAsState(
                                targetValue = targetMatch,
                                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                                label = "matchAnim"
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Target Role: $targetRole",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Role Benchmark Alignment",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$animatedMatch%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { animatedMatch / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Primary,
                                    trackColor = SurfaceVariant,
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MintLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Synthesizing skill gap benchmarks for $targetRole...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryDark
                                        )
                                    }
                                }
                            }
                        }

                        4 -> {
                            // STAGE 5: ANALYSIS COMPLETE
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SoftGreen,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Analysis Successfully Synthesized",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "All 4 intelligence dimensions generated. Opening ATS diagnostics dashboard...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                LinearProgressIndicator(
                                    color = Primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
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

    // AUTOMATIC NAVIGATION TO ATS DASHBOARD ON ANALYSIS COMPLETION
    LaunchedEffect(analysisResult) {
        if (analysisResult != null) {
            navController.navigate(Screen.AtsAnalysis.route) {
                popUpTo(Screen.ResumeUpload.route) { inclusive = false }
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                                        text = "Astra Tip",
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
                        targetRole = analyzingRole.ifBlank { targetRole }
                    )
                }
            }
        }
    }

// ==========================================
// 4. ATS DASHBOARD SCREEN (Separate Dashboard)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtsDashboardScreen(navController: NavController, viewModel: AtsViewModel) {
    val analysis by viewModel.analysisResult.collectAsState()
    val scoreResult by viewModel.scoreResult.collectAsState()

    var selectedPriorityForDetail by remember { mutableStateOf<String?>(null) }
    var selectedWhyMetric by remember { mutableStateOf<AtsCardCalculation?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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

                    // 1. Entity Data Extraction Diagnostics (MUST BE FIRST AT THE TOP)
                    item {
                        AtsEntityExtractionCard(
                            score = score,
                            analysis = analysisData
                        )
                    }

                    // 2. Candidate Profile & Target Role Header
                    item {
                        CandidateProfileCard(
                            targetRole = score.targetRole,
                            candidateName = score.candidateName,
                            candidateEmail = score.candidateEmail,
                            candidatePhone = score.candidatePhone,
                            candidateLocation = score.candidateLocation
                        )
                    }

                    // 3. Main ATS Compatibility Score Hero
                    item {
                        AtsOverallScoreHero(
                            score = score,
                            onMetricClick = { metric ->
                                selectedWhyMetric = metric
                            }
                        )
                    }

                    // 4. Keyword Analysis & ATS Performance Areas Header
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

                    // 4a. Dimension 1: Keyword Coverage (35%)
                    item {
                        AtsExpandableCard(
                            calculation = score.keywordCoverage,
                            onAskWhyClick = {
                                selectedWhyMetric = score.keywordCoverage
                            }
                        )
                    }

                    // 4b. Dimension 2: Resume Structure (25%)
                    item {
                        AtsExpandableCard(
                            calculation = score.resumeStructure,
                            onAskWhyClick = {
                                selectedWhyMetric = score.resumeStructure
                            }
                        )
                    }

                    // 4c. Dimension 3: Formatting Safety (20%)
                    item {
                        AtsExpandableCard(
                            calculation = score.formattingSafety,
                            onAskWhyClick = {
                                selectedWhyMetric = score.formattingSafety
                            }
                        )
                    }

                    // 4d. Dimension 4: Parsing Accuracy (20%)
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

                    // 11. NEXT DASHBOARD NAVIGATION: Continue to Skill Gap
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Primary,
                            shadowElevation = 3.dp
                        ) {
                            Button(
                                onClick = { navController.navigate(Screen.SkillGap.route) },
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
                    navController.navigate(Screen.AiAssistant.route)
                }
            )
        }

        // Immersive "Why?" Explanation Modal
        if (selectedWhyMetric != null) {
            AtsMetricWhyModal(
                calculation = selectedWhyMetric!!,
                onDismiss = { selectedWhyMetric = null },
                onAskAi = {
                    selectedWhyMetric = null
                    navController.navigate(Screen.AiAssistant.route)
                }
            )
        }

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
                            "AstraAI evaluates your resume using deterministic parser emulation algorithms weighted across 4 key dimensions:",
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
fun SkillGapDashboardScreen(navController: NavController, viewModel: SkillGapViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedSkillForDetail by viewModel.selectedSkillForDetail.collectAsState()
    var showRolePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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

                    // Section 2: Skill Gap Visualization (Category Distribution)
                    item {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "GAP VISUALIZATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Competency Domain Distribution",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = "Present candidate competencies vs target role requirements by domain:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    state.categoryCoverage.forEach { coverage ->
                                        CategoryGapMeterItem(coverage = coverage)
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

                    // Section 4: Filterable Skill Registry (Present vs Missing)
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

                            // 3 Clean Filters: All, Present, Missing
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
                                        Triple(SkillFilter.ALL, "All", state.totalRequired),
                                        Triple(SkillFilter.PRESENT, "Present", state.matchedCount),
                                        Triple(SkillFilter.MISSING, "Missing", state.missingCount)
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

                    // Render Filtered Skills List
                    val displayedSkills = when (state.filter) {
                        SkillFilter.ALL -> state.allSkillsDetailed
                        SkillFilter.PRESENT -> state.presentSkills
                        SkillFilter.MISSING -> state.missingSkills
                    }

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
                        navController.navigate(Screen.AiAssistant.route)
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

            FloatingAstraBot(navController, "Skill Gap Analysis")
        }
    }
}

@Composable
fun CategoryGapMeterItem(coverage: SkillCategoryCoverage) {
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
                    text = "${coverage.percentage}%",
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
                    .fillMaxWidth(coverage.percentage / 100f)
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
                        Text("Ask Astra AI", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionAnalyzerScreen(navController: NavController, viewModel: JdMatcherViewModel) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val baseJd by viewModel.baseJobDescription.collectAsState()
    val flowMode by viewModel.flowMode.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val currentStageIndex by viewModel.currentStageIndex.collectAsState()
    val poweredResult by viewModel.poweredResult.collectAsState()
    val expandedExplainId by viewModel.expandedExplainId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.extractProfileAndGenerateJd()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("JD Matching", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Automated Job Description Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            when (flowMode) {
                JdFlowMode.NO_JD -> {
                    // ==========================================
                    // NO JOB DESCRIPTION DETECTED STATE
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item {
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
                                        color = SurfaceVariant,
                                        modifier = Modifier.size(76.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FindInPage,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "NO JOB DESCRIPTION FOUND",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "We couldn't find a Job Description in this resume. JD analysis will be available when a Job Description is provided.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp
                                        )
                                    }

                                    HorizontalDivider(color = BorderColor)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MintLight,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "You can proceed directly to Interview Preparation, where technical questions will be generated from your analyzed profile.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PrimaryDark,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Navigation Actions
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
                                        onClick = { navController.navigate(Screen.InterviewPrep.route) },
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
                                                text = "Skip JD Matching → Interview Preparation",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
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
                                    onClick = { navController.navigate(Screen.SkillGap.route) },
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
                                            text = "Back to Skill Gap",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }

                JdFlowMode.JD_DETECTED, JdFlowMode.PROFILE_VIEW -> {
                    // ==========================================
                    // GENUINE JOB DESCRIPTION DETECTED STATE
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AstraCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = SoftGreen
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = PrimaryDark,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "JOB DESCRIPTION DETECTED",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDark
                                            )
                                            Text(
                                                text = "Role Target: ${baseJd.roleTitle.ifBlank { profile.targetRole }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Extracted Job Description Details Card
                        item {
                            AstraCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = baseJd.roleTitle.ifBlank { "Job Description" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    if (baseJd.roleSummary.isNotBlank()) {
                                        Text(
                                            text = baseJd.roleSummary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    if (baseJd.responsibilities.isNotEmpty()) {
                                        HorizontalDivider(color = BorderColor)
                                        Text(
                                            text = "Key Responsibilities",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                        baseJd.responsibilities.forEach { resp ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("• ", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
                                                Text(resp, style = MaterialTheme.typography.bodySmall, color = TextPrimary, lineHeight = 18.sp)
                                            }
                                        }
                                    }

                                    if (baseJd.requiredSkills.isNotEmpty()) {
                                        HorizontalDivider(color = BorderColor)
                                        Text(
                                            text = "Required Competencies & Skills",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(baseJd.requiredSkills) { skill ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MintLight,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = skill,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PrimaryDark,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (baseJd.preferredSkills.isNotEmpty()) {
                                        Text(
                                            text = "Preferred Skills",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(baseJd.preferredSkills) { skill ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = SurfaceVariant,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                                                ) {
                                                    Text(
                                                        text = skill,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Primary Action: Power the JD
                        item {
                            PremiumButton(
                                text = "⚡ Power the JD",
                                onClick = {
                                    viewModel.powerTheJd()
                                }
                            )
                        }

                        // Secondary Navigation: Skip to Interview Prep or Back to Skill Gap
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.InterviewPrep.route) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Skip to Interview Preparation",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.SkillGap.route) },
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
                                            text = "Back to Skill Gap",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                JdFlowMode.POWERING_PROGRESS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Primary,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )

                                Text(
                                    text = "Powering Your Job Description",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Text(
                                    text = "Generating ATS-optimized JD customized to your resume competencies...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                HorizontalDivider(color = BorderColor)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    stages.forEachIndexed { index, stage ->
                                        val isCurrent = index == currentStageIndex
                                        val isDone = stage.status == JdStageStatus.COMPLETED

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isCurrent) MintLight else if (isDone) SurfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isDone) SuccessGreen else if (isCurrent) Primary else BorderColor,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (isDone) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    } else {
                                                        Text(
                                                            text = stage.number,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stage.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isDone) SuccessGreen else if (isCurrent) PrimaryDark else TextSecondary
                                                )
                                                Text(
                                                    text = stage.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (isCurrent) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                JdFlowMode.POWERED_RESULT -> {
                    val result = poweredResult
                    if (result != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top Fit Badge
                            item {
                                AstraCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = SoftGreen
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "${result.alignmentPercentage}% Role Alignment",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryDark
                                                )
                                                Text(
                                                    text = "Calibrated for ${result.targetRole}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.powerAgain() },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                                        ) {
                                            Text("Recalibrate", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Job Description Card
                            item {
                                AstraCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = result.jobDescription.roleTitle,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        Text(
                                            text = result.jobDescription.roleSummary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            lineHeight = 20.sp
                                        )

                                        HorizontalDivider(color = BorderColor)

                                        Text(
                                            text = "Key Responsibilities",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                        result.jobDescription.responsibilities.forEach { resp ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("• ", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
                                                Text(resp, style = MaterialTheme.typography.bodySmall, color = TextPrimary, lineHeight = 18.sp)
                                            }
                                        }

                                        HorizontalDivider(color = BorderColor)

                                        Text(
                                            text = "Required Competencies & Skills",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(result.jobDescription.requiredSkills) { skill ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MintLight,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = skill,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PrimaryDark,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Preferred Skills",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDark
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(result.jobDescription.preferredSkills) { skill ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = SurfaceVariant,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                                                ) {
                                                    Text(
                                                        text = skill,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // What Changed & Why Explainability
                            item {
                                AstraCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "What Changed & Why",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "Transparency breakdown explaining how this JD was personalized to your background:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )

                                        HorizontalDivider(color = BorderColor)

                                        result.whatChangedItems.forEach { explainItem ->
                                            val isExpanded = expandedExplainId == explainItem.id

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isExpanded) MintLight else SurfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isExpanded) Primary.copy(alpha = 0.4f) else BorderColor
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.toggleExplainItem(explainItem.id) }
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = explainItem.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isExpanded) Primary else SoftGreen
                                                        ) {
                                                            Text(
                                                                text = explainItem.tag,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isExpanded) Color.White else PrimaryDark,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    if (isExpanded) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = explainItem.detail,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextPrimary,
                                                            lineHeight = 18.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Forward Navigation to Interview Preparation & Secondary Actions
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Primary,
                                        shadowElevation = 4.dp
                                    ) {
                                        Button(
                                            onClick = { navController.navigate(Screen.InterviewPrep.route) },
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
                                                    text = "Continue to Interview Preparation",
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText(
                                                    "Job Description",
                                                    "${result.jobDescription.roleTitle}\n\n${result.jobDescription.roleSummary}\n\nKey Responsibilities:\n${result.jobDescription.responsibilities.joinToString("\n• ")}"
                                                )
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "JD copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Copy JD", color = TextPrimary)
                                        }

                                        OutlinedButton(
                                            onClick = { navController.navigate(Screen.SkillGap.route) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Back to Skill Gap", color = TextSecondary)
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            FloatingAstraBot(navController, "JD Matcher Dashboard")
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
                        Surface(
                            shape = CircleShape,
                            color = SoftGreen,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AstraAI Assistant", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text("Real-time Career Intelligence", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                },
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
                        placeholder = { Text("Ask Astra anything...", color = TextSecondary) },
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
            modifier = Modifier.widthIn(max = 300.dp)
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
                        text = candidateName ?: "Candidate Resume",
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
                                    text = "Target: $targetRole",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "ATS Engine v2.4",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
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

            if (!candidateEmail.isNullOrBlank() || !candidatePhone.isNullOrBlank() || !candidateLocation.isNullOrBlank()) {
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 2.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!candidateEmail.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(candidateEmail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                    if (!candidatePhone.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(candidatePhone, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                    if (!candidateLocation.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(candidateLocation, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
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

    val scoreColor = when {
        score.overallScore >= 80 -> SuccessGreen
        score.overallScore >= 60 -> Color(0xFF0284C7)
        score.overallScore >= 45 -> WarningAmber
        else -> ErrorRed
    }

    val statusBadge = when {
        score.overallScore >= 85 -> "ATS Optimized (Top 10%)"
        score.overallScore >= 70 -> "Competitive Match"
        score.overallScore >= 50 -> "Moderate ATS Fit"
        else -> "Action Required"
    }

    val passProbability = when {
        score.overallScore >= 80 -> "92% Estimated ATS Pass Rate"
        score.overallScore >= 65 -> "74% Estimated ATS Pass Rate"
        score.overallScore >= 50 -> "51% Estimated ATS Pass Rate"
        else -> "32% Estimated ATS Pass Rate"
    }

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
                    color = SoftGreen,
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { animatedScoreFloat },
                    modifier = Modifier.size(164.dp),
                    strokeWidth = 14.dp,
                    color = scoreColor,
                    trackColor = Color.Transparent
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedScoreInt",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = scoreColor
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
    val color = when {
        calculation.score >= 80 -> SuccessGreen
        calculation.score >= 60 -> Color(0xFF0284C7)
        calculation.score >= 40 -> WarningAmber
        else -> ErrorRed
    }

    val iconVector = when (calculation.iconType) {
        AtsCategoryIcon.KEYWORD -> Icons.Default.Key
        AtsCategoryIcon.STRUCTURE -> Icons.Default.Layers
        AtsCategoryIcon.FORMATTING -> Icons.Default.Description
        AtsCategoryIcon.PARSING -> Icons.Default.Code
        AtsCategoryIcon.OVERALL -> Icons.Default.AutoAwesome
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
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
                        Text("Ask Astra AI", fontWeight = FontWeight.Bold)
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
    val color = when {
        calculation.score >= 80 -> SuccessGreen
        calculation.score >= 60 -> Color(0xFF0284C7)
        calculation.score >= 40 -> WarningAmber
        else -> ErrorRed
    }

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
                    text = "${calculation.score}/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { calculation.score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = SoftGreen
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
                        color = PrimaryDark,
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
    analysis: ResumeAnalysisResult
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
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Entity Data Extraction Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Candidate Profile, Resume Sections & Extraction Summary",
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

                // 1. Candidate Profile Entities Card
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
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CANDIDATE PROFILE ENTITIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                letterSpacing = 0.8.sp
                            )
                        }
                        EntityDiagnosticRow("Name", score.candidateName ?: "Not detected", score.candidateName != null)
                        EntityDiagnosticRow("Email", score.candidateEmail ?: "Not detected", score.candidateEmail != null)
                        EntityDiagnosticRow("Phone", score.candidatePhone ?: "Not detected", score.candidatePhone != null)
                        EntityDiagnosticRow("Location", score.candidateLocation ?: "Not detected", score.candidateLocation != null)
                    }
                }

                // 2. Standard Sections Detected Card
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
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESUME SECTIONS DETECTED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                letterSpacing = 0.8.sp
                            )
                        }
                        val missing = analysis.missingSections ?: emptyList()
                        val sections = listOf("Contact Info", "Work Experience", "Education", "Skills", "Projects", "Summary")
                        sections.forEach { sec ->
                            val isDetected = !missing.any { it.contains(sec, ignoreCase = true) }
                            EntityDiagnosticRow(sec, if (isDetected) "Verified standard header" else "Missing or unrecognized", isDetected)
                        }
                    }
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
private fun EntityDiagnosticRow(label: String, value: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                Surface(
                    shape = CircleShape,
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AstraAI Executive Career Insight",
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            PremiumButton(
                text = "Ask AstraAI to Optimize Bullet Points",
                onClick = onConsultAiClick,
                modifier = Modifier.fillMaxWidth()
            )
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
        color = Primary,
        shadowElevation = 8.dp,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(18.dp)
            .clickable { navController.navigate(Screen.AiAssistant.route) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Ask Astra", tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                text = "✦ AstraAI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
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
