package com.example.aidrivencompetencyplatform.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.model.*
import com.example.aidrivencompetencyplatform.ui.components.*
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import com.example.aidrivencompetencyplatform.ui.theme.*
import com.example.aidrivencompetencyplatform.viewmodel.*

// ==========================================
// 1. APP TOUR / ONBOARDING SCREEN
// ==========================================

@Composable
fun AppTourScreen(navController: NavController, viewModel: DashboardViewModel) {
    var currentStep by remember { mutableStateOf(1) }
    val steps = 4

    val tourData = listOf(
        TourStepItem(
            step = 1,
            title = "1. Resume Upload & Role Selection",
            subtitle = "Upload your resume and choose the role you're targeting.",
            icon = Icons.Default.Description,
            badge = "STEP 1 OF 4"
        ),
        TourStepItem(
            step = 2,
            title = "2. ATS Analysis",
            subtitle = "See how well your resume performs against ATS systems with deterministic scoring.",
            icon = Icons.Default.Assessment,
            badge = "STEP 2 OF 4"
        ),
        TourStepItem(
            step = 3,
            title = "3. Skill Gap Analysis",
            subtitle = "Discover the skills you are missing for your target role and areas to grow.",
            icon = Icons.Default.Psychology,
            badge = "STEP 3 OF 4"
        ),
        TourStepItem(
            step = 4,
            title = "4. JD Matcher",
            subtitle = "Compare your resume directly against specific job descriptions.",
            icon = Icons.Default.Work,
            badge = "STEP 4 OF 4"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ASTRAAI TOUR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.completeTour()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.AppTour.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Skip", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally { it } togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally { -it }
                },
                label = "TourTransition"
            ) { step ->
                val current = tourData[step - 1]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MintLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.size(130.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = current.icon,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftGreen
                    ) {
                        Text(
                            text = current.badge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = current.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom Navigation Indicators & Next Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Step indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(steps) { i ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (i + 1 == currentStep) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i + 1 == currentStep) Primary
                                    else BorderColor
                                )
                        )
                    }
                }

                if (currentStep < steps) {
                    PremiumButton(
                        text = "Continue",
                        onClick = { currentStep++ }
                    )
                } else {
                    PremiumButton(
                        text = "Finish & Open Dashboard",
                        onClick = {
                            viewModel.completeTour()
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.AppTour.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

data class TourStepItem(
    val step: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
)

// ==========================================
// 2. MAIN DASHBOARD SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(navController: NavController, viewModel: DashboardViewModel) {
    val userName by viewModel.userName.collectAsState()
    val greetingPrefix by viewModel.greetingPrefix.collectAsState()
    val hasAnalysis by viewModel.hasAnalysis.collectAsState()
    val atsScore by viewModel.atsScore.collectAsState()
    val skillMatch by viewModel.skillMatch.collectAsState()
    val hasCompletedTour by viewModel.hasCompletedTour.collectAsState()

    var showTourDialog by remember { mutableStateOf(false) }
    var activeTourStep by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
    }

    // Auto-launch interactive tour for brand new users if not completed
    LaunchedEffect(hasCompletedTour) {
        if (!hasCompletedTour) {
            showTourDialog = true
        }
    }

    Scaffold(
        containerColor = Background
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
                // Curved Emerald Green Header Wave (matching visual reference)
                item {
                    AstraDashboardHeader(
                        greeting = if (greetingPrefix.contains("back", ignoreCase = true)) {
                            "Welcome back, $userName 👋"
                        } else {
                            "Welcome to AstraAI 👋"
                        },
                        subGreeting = if (greetingPrefix.contains("back", ignoreCase = true)) {
                            "Track and elevate your career intelligence"
                        } else {
                            "Your AI-powered career companion is ready"
                        },
                        onProfileClick = { navController.navigate(Screen.Profile.route) }
                    )
                }

                // Overview Status & Real State Card (Clean white card overlapping slightly)
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AstraRealStatusCard(
                            hasAnalysis = hasAnalysis,
                            atsScore = atsScore,
                            skillMatch = skillMatch,
                            onTakeTourClick = {
                                activeTourStep = 1
                                showTourDialog = true
                            }
                        )
                    }
                }

                // Section Header: 4 Core Features
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Core Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "4 Modules",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                // FEATURE 1: Resume Upload + Role Selection
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MainFeatureCard(
                            number = "01",
                            title = "Resume Upload + Role Selection",
                            subtitle = "Upload your resume (PDF/DOCX) and choose your target role to build the competency baseline.",
                            icon = Icons.Default.CloudUpload,
                            actionText = if (hasAnalysis) "Update Resume / Role →" else "Upload Resume & Role →",
                            statusTag = if (hasAnalysis) "Active Resume Configured" else "Step 1: Action Required",
                            isHighlighted = showTourDialog && activeTourStep == 1,
                            onClick = { navController.navigate(Screen.ResumeUpload.route) }
                        )
                    }
                }

                // FEATURE 2: ATS Analysis
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MainFeatureCard(
                            number = "02",
                            title = "ATS Analysis",
                            subtitle = "Evaluate keyword coverage, structure, formatting safety, and parsing accuracy with deterministic scoring.",
                            icon = Icons.Default.Assessment,
                            actionText = "Open ATS Dashboard →",
                            statusTag = if (atsScore != null) "ATS Score: $atsScore/100" else "Ready to Analyze",
                            isHighlighted = showTourDialog && activeTourStep == 2,
                            onClick = { navController.navigate(Screen.AtsAnalysis.route) }
                        )
                    }
                }

                // FEATURE 3: Skill Gap
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MainFeatureCard(
                            number = "03",
                            title = "Skill Gap Analysis",
                            subtitle = "Identify strong competencies, developing capabilities, and critical missing skill gaps for your role.",
                            icon = Icons.Default.Psychology,
                            actionText = "Open Skill Gap Dashboard →",
                            statusTag = if (skillMatch != null) "Match: $skillMatch%" else "Explore Gaps",
                            isHighlighted = showTourDialog && activeTourStep == 3,
                            onClick = { navController.navigate(Screen.SkillGap.route) }
                        )
                    }
                }

                // FEATURE 4: JD Matcher
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MainFeatureCard(
                            number = "04",
                            title = "JD Matcher",
                            subtitle = "Compare your resume directly against specific job description requirements for tailored fit insights.",
                            icon = Icons.Default.Work,
                            actionText = "Open JD Matcher Dashboard →",
                            statusTag = "Targeted Comparison",
                            isHighlighted = showTourDialog && activeTourStep == 4,
                            onClick = { navController.navigate(Screen.JobDescriptionAnalyzer.route) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }

            // Floating AstraAI Assistant Button (Bottom Right)
            FloatingAstraBot(
                navController = navController,
                screenContext = "Main Dashboard"
            )

            // Interactive Product Tour Modal (For new users and "Take a Tour")
            if (showTourDialog) {
                InteractiveTourBottomSheet(
                    currentStep = activeTourStep,
                    onNextStep = {
                        if (activeTourStep < 4) {
                            activeTourStep++
                        } else {
                            viewModel.completeTour()
                            showTourDialog = false
                        }
                    },
                    onSkip = {
                        viewModel.completeTour()
                        showTourDialog = false
                    },
                    onNavigateToStep = { step ->
                        viewModel.completeTour()
                        showTourDialog = false
                        when (step) {
                            1 -> navController.navigate(Screen.ResumeUpload.route)
                            2 -> navController.navigate(Screen.AtsAnalysis.route)
                            3 -> navController.navigate(Screen.SkillGap.route)
                            4 -> navController.navigate(Screen.JobDescriptionAnalyzer.route)
                        }
                    }
                )
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
// COMPONENT: REAL STATUS OVERVIEW CARD
// ==========================================

@Composable
fun AstraRealStatusCard(
    hasAnalysis: Boolean,
    atsScore: Int?,
    skillMatch: Int?,
    onTakeTourClick: () -> Unit
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
                Text(
                    text = "Career Intelligence Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.clickable { onTakeTourClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Take a Tour",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Resume Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resume",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasAnalysis) "Uploaded" else "Not Uploaded",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAnalysis) SuccessGreen else WarningAmber
                    )
                }

                // ATS Score Status
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ATS Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (atsScore != null) "$atsScore/100" else "Pending",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (atsScore != null) Primary else TextSecondary
                    )
                }

                // Skill Match Status
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Skill Match",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (skillMatch != null) "$skillMatch%" else "Pending",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (skillMatch != null) Primary else TextSecondary
                    )
                }
            }
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
// COMPONENT: INTERACTIVE PRODUCT TOUR MODAL
// ==========================================

@Composable
fun InteractiveTourBottomSheet(
    currentStep: Int,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    onNavigateToStep: (Int) -> Unit
) {
    val stepsInfo = listOf(
        Triple(
            "Step 1: Resume & Role Setup",
            "Upload your resume and choose the role you're targeting to establish your competency baseline.",
            Icons.Default.CloudUpload
        ),
        Triple(
            "Step 2: ATS Analysis",
            "See how well your resume performs against ATS systems with transparent 4-dimension scoring.",
            Icons.Default.Assessment
        ),
        Triple(
            "Step 3: Skill Gap Analysis",
            "Discover the skills you are missing for your target role and actionable learning paths.",
            Icons.Default.Psychology
        ),
        Triple(
            "Step 4: JD Matcher",
            "Compare your resume with a specific job description to evaluate role-specific match.",
            Icons.Default.Work
        )
    )

    val current = stepsInfo[currentStep - 1]

    Dialog(onDismissRequest = onSkip) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AstraAI Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Step $currentStep of 4",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MintLight,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = current.third,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = current.first,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = current.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i + 1 == currentStep) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (i + 1 == currentStep) Primary else BorderColor)
                        )
                    }
                }

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
                        Text("Skip", color = TextSecondary)
                    }

                    Button(
                        onClick = onNextStep,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(if (currentStep < 4) "Next Step" else "Finish")
                    }
                }

                TextButton(
                    onClick = { onNavigateToStep(currentStep) }
                ) {
                    Text(
                        text = "Open this module now →",
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
// 3. RESUME UPLOAD + ROLE SELECTION DASHBOARD
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeUploadScreen(navController: NavController, viewModel: ResumeViewModel) {
    val analysisResult by viewModel.analysisResult.collectAsState()
    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingStage by viewModel.loadingStage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    LaunchedEffect(analysisResult) {
        if (analysisResult != null) {
            navController.navigate(Screen.AtsAnalysis.route)
        }
    }

    var targetRole by remember { mutableStateOf("Android Developer") }
    val roles = listOf(
        "Android Developer",
        "Backend Developer",
        "Full Stack Developer",
        "Data Scientist",
        "DevOps Engineer",
        "Cloud Solutions Architect",
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

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Resume & Role Setup",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 5.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = loadingStage,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Processing through AstraAI career intelligence engine...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (errorMessage != null) {
                        item {
                            AstraCard(
                                containerColor = ErrorRed.copy(alpha = 0.08f),
                                borderColor = ErrorRed.copy(alpha = 0.3f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(errorMessage!!, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Step 1: Upload Resume Card
                    item {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedFileName != null) SoftGreen else MintLight,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = if (selectedFileName != null) Primary else TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (selectedFileName != null) "Resume Uploaded" else "Upload Your Resume",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (selectedFileName != null) selectedFileName!! else "Supported formats: PDF, DOC, DOCX",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedFileName != null) PrimaryDark else TextSecondary,
                                    fontWeight = if (selectedFileName != null) FontWeight.SemiBold else FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (selectedFileName != null) {
                                    OutlinedButton(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Replace Resume", color = Primary)
                                    }
                                } else {
                                    PremiumButton(
                                        text = "Select Resume File",
                                        onClick = { filePickerLauncher.launch("*/*") }
                                    )
                                }
                            }
                        }
                    }

                    // Step 2: Target Role Selection Card
                    item {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Work, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Target Career Role",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Text(
                                    text = "Select the role you're targeting for tailored ATS matching and competency benchmarks:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

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

                    // Action Button: Start AI Analysis
                    item {
                        PremiumButton(
                            text = "Start AI Analysis",
                            enabled = selectedFileName != null && !isLoading,
                            onClick = {
                                if (selectedFileName == null) {
                                    Toast.makeText(context, "Please select a resume file first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.analyzeResume(targetRole)
                                }
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
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

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ATS Analytics Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Deterministic 4-Component Scoring",
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
            if (analysis == null || scoreResult == null) {
                NoAnalysisState(navController)
            } else {
                val score = scoreResult!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // Candidate Profile Header Card
                    item {
                        CandidateProfileCard(
                            targetRole = score.targetRole,
                            candidateName = score.candidateName,
                            candidateEmail = score.candidateEmail,
                            candidatePhone = score.candidatePhone,
                            candidateLocation = score.candidateLocation
                        )
                    }

                    // Hero Circular Gauge Overall ATS Score
                    item {
                        AtsOverallScoreHero(score = score)
                    }

                    item {
                        Text(
                            text = "4-Component ATS Breakdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }

                    // Dimension 1: Keyword Coverage (35%)
                    item {
                        AtsExpandableCard(
                            calculation = score.keywordCoverage,
                            defaultExpanded = true
                        )
                    }

                    // Dimension 2: Resume Structure (25%)
                    item {
                        AtsExpandableCard(
                            calculation = score.resumeStructure,
                            defaultExpanded = false
                        )
                    }

                    // Dimension 3: Formatting Safety (20%)
                    item {
                        AtsExpandableCard(
                            calculation = score.formattingSafety,
                            defaultExpanded = false
                        )
                    }

                    // Dimension 4: Parsing Accuracy (20%)
                    item {
                        AtsExpandableCard(
                            calculation = score.parsingAccuracy,
                            defaultExpanded = false
                        )
                    }

                    // Priority Fixes Booster Plan
                    if (score.priorityFixes.isNotEmpty()) {
                        item {
                            AtsPriorityFixesCard(fixes = score.priorityFixes)
                        }
                    }

                    // AI Strengths
                    if (!analysis!!.strengths.isNullOrEmpty()) {
                        item {
                            AstraCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AI Identified Strengths", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    }
                                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                                    analysis!!.strengths!!.forEach { s ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                            Text("✓", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(s, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }

            FloatingAstraBot(navController, "ATS Dashboard")
        }
    }
}

// ==========================================
// 5. SKILL GAP DASHBOARD SCREEN (Separate Dashboard)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillGapDashboardScreen(navController: NavController, viewModel: SkillGapViewModel) {
    val analysis by viewModel.analysisResult.collectAsState()
    var selectedFilterIndex by remember { mutableStateOf(0) }
    val filterTabs = listOf("All Skills", "Strong (80%+)", "Developing (40-79%)", "Gaps (<40%)")

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Skill Gap Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (analysis == null) {
                NoAnalysisState(navController)
            } else {
                val skills = analysis!!.extractedSkills ?: emptyList()
                val matchPercentage = analysis!!.skillMatch

                val filteredSkills = when (selectedFilterIndex) {
                    1 -> skills.filter { it.level >= 80 }
                    2 -> skills.filter { it.level in 40..79 }
                    3 -> skills.filter { it.level < 40 }
                    else -> skills
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // Hero Skill Match Card
                    item {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "TARGET ROLE COMPETENCY MATCH",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(130.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = 1f,
                                        modifier = Modifier.size(130.dp),
                                        strokeWidth = 12.dp,
                                        color = SoftGreen,
                                        trackColor = Color.Transparent
                                    )
                                    CircularProgressIndicator(
                                        progress = matchPercentage / 100f,
                                        modifier = Modifier.size(130.dp),
                                        strokeWidth = 12.dp,
                                        color = Primary,
                                        trackColor = Color.Transparent
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$matchPercentage%",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Primary
                                        )
                                        Text(
                                            text = "Fit Score",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MintLight
                                ) {
                                    Text(
                                        text = "Target: ${(analysis!!.summary ?: "Target Role").take(40)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Filter Pills
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filterTabs.size) { idx ->
                                val isSelected = idx == selectedFilterIndex
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Primary else Surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Primary else BorderColor
                                    ),
                                    modifier = Modifier.clickable { selectedFilterIndex = idx }
                                ) {
                                    Text(
                                        text = filterTabs[idx],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Skills List
                    item {
                        if (filteredSkills.isEmpty()) {
                            AstraCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No skills found in this filter category.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filteredSkills.forEach { skill ->
                                    ModernSkillCard(skill = skill)
                                }
                            }
                        }
                    }

                    // Missing Requirements Section
                    val missing = analysis!!.missingSections ?: emptyList()
                    if (missing.isNotEmpty()) {
                        item {
                            AstraCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = WarningAmber.copy(alpha = 0.05f),
                                borderColor = WarningAmber.copy(alpha = 0.3f)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Missing Section Requirements",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    HorizontalDivider(color = BorderColor)
                                    missing.forEach { req ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                            Text("•", color = WarningAmber, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(req, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }

            FloatingAstraBot(navController, "Skill Gap Dashboard")
        }
    }
}

@Composable
fun ModernSkillCard(skill: Skill) {
    var expanded by remember { mutableStateOf(false) }

    val levelColor = when {
        skill.level >= 80 -> SuccessGreen
        skill.level >= 40 -> WarningAmber
        else -> ErrorRed
    }

    AstraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        cornerRadius = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = levelColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${skill.level}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = skill.level / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = levelColor,
                trackColor = SoftGreen
            )

            if (expanded && !skill.evidence.isNullOrBlank()) {
                HorizontalDivider(color = BorderColor)
                Text(
                    text = "Resume Evidence: ${skill.evidence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ==========================================
// 6. JD MATCHER DASHBOARD SCREEN (Separate Dashboard)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionAnalyzerScreen(navController: NavController, viewModel: ResumeViewModel) {
    var jobDescription by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    val analysisResult by viewModel.analysisResult.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("JD Matcher Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Resume ↔ Job Description Alignment",
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AstraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Target Job Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Text(
                                text = "Paste the complete requirements and responsibilities for the target job posting:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            OutlinedTextField(
                                value = jobDescription,
                                onValueChange = { jobDescription = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                placeholder = {
                                    Text(
                                        "e.g. Seeking Senior Android Developer with 4+ years Kotlin, Jetpack Compose, Coroutines, MVVM, and CI/CD...",
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface
                                )
                            )
                        }
                    }
                }

                item {
                    PremiumButton(
                        text = if (analyzing) "Comparing Resume against JD..." else "Analyze Job Match",
                        enabled = jobDescription.isNotBlank() && !analyzing,
                        onClick = {
                            analyzing = true
                            viewModel.analyzeResume("Target Job Description")
                        }
                    )
                }

                if (analysisResult != null) {
                    val result = analysisResult!!
                    val jdMatchScore = result.skillMatch

                    item {
                        AstraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "JOB DESCRIPTION FIT",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(120.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = 1f,
                                        modifier = Modifier.size(120.dp),
                                        strokeWidth = 10.dp,
                                        color = SoftGreen,
                                        trackColor = Color.Transparent
                                    )
                                    CircularProgressIndicator(
                                        progress = jdMatchScore / 100f,
                                        modifier = Modifier.size(120.dp),
                                        strokeWidth = 10.dp,
                                        color = Primary,
                                        trackColor = Color.Transparent
                                    )
                                    Text(
                                        text = "$jdMatchScore%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (jdMatchScore >= 75) "Strong Alignment with JD" else "Moderate Fit — Keyword Enhancements Recommended",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (jdMatchScore >= 75) SuccessGreen else WarningAmber
                                )
                            }
                        }
                    }

                    // Matching vs Missing Skills
                    item {
                        val skills = result.extractedSkills ?: emptyList()
                        val strong = skills.filter { it.level >= 60 }
                        val gaps = skills.filter { it.level < 60 }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AstraCard(
                                modifier = Modifier.weight(1f),
                                containerColor = MintLight
                            ) {
                                Text(
                                    text = "Matched Requirements",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                strong.take(4).forEach { s ->
                                    Text("✓ ${s.name}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                            }

                            AstraCard(
                                modifier = Modifier.weight(1f),
                                containerColor = ErrorRed.copy(alpha = 0.05f),
                                borderColor = ErrorRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Missing Keywords",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (gaps.isEmpty()) {
                                    Text("No critical gaps", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                } else {
                                    gaps.take(4).forEach { s ->
                                        Text("✗ ${s.name}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
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
// 9. ATS HELPER COMPOSABLES
// ==========================================

@Composable
fun CandidateProfileCard(
    targetRole: String,
    candidateName: String?,
    candidateEmail: String?,
    candidatePhone: String?,
    candidateLocation: String?
) {
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftGreen
                        ) {
                            Text(
                                text = "Target: $targetRole",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (!candidateEmail.isNullOrBlank() || !candidatePhone.isNullOrBlank() || !candidateLocation.isNullOrBlank()) {
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!candidateEmail.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(candidateEmail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                    if (!candidatePhone.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(candidatePhone, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtsOverallScoreHero(score: AtsScoreResult) {
    val animatedScoreFloat by animateFloatAsState(
        targetValue = score.overallScore / 100f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "scoreFloat"
    )
    val animatedScoreInt by animateIntAsState(
        targetValue = score.overallScore,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "scoreInt"
    )

    val scoreColor = when {
        score.overallScore >= 80 -> SuccessGreen
        score.overallScore >= 60 -> Color(0xFF0284C7)
        score.overallScore >= 45 -> WarningAmber
        else -> ErrorRed
    }

    val statusBadge = when {
        score.overallScore >= 85 -> "ATS Optimized"
        score.overallScore >= 70 -> "Competitive Match"
        score.overallScore >= 50 -> "Moderate Fit"
        else -> "Needs Revision"
    }

    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "OVERALL ATS SCORE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large Circular Score Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 14.dp,
                    color = SoftGreen,
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = animatedScoreFloat,
                    modifier = Modifier.size(160.dp),
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
                        text = "/ 100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scoreColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusBadge,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Formula: (Keyword × 0.35) + (Structure × 0.25) + (Formatting × 0.20) + (Parsing × 0.20)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "= (${score.keywordCoverage.score} × 0.35) + (${score.resumeStructure.score} × 0.25) + (${score.formattingSafety.score} × 0.20) + (${score.parsingAccuracy.score} × 0.20) = ${score.overallScore}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AtsExpandableCard(
    calculation: AtsCardCalculation,
    defaultExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    val color = when {
        calculation.score >= 80 -> SuccessGreen
        calculation.score >= 60 -> Color(0xFF0284C7)
        calculation.score >= 40 -> WarningAmber
        else -> ErrorRed
    }

    AstraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = calculation.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftGreen
                        ) {
                            Text(
                                text = "${calculation.weightPercentage}% Weight",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Contributes ${String.format("%.1f", calculation.weightedContribution)} pts to overall score",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${calculation.score}/100",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = calculation.score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = SoftGreen
            )

            if (expanded) {
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                // Formula & Mathematical step
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Calculation Formula:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = calculation.formulaText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Math: ${calculation.calculationText}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Evidence Checklist
                Text(
                    text = "Evaluation Evidence & Checklist",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    calculation.evidenceItems.forEach { ev ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = if (ev.isPositive) "✓" else "⚠",
                                color = if (ev.isPositive) SuccessGreen else ErrorRed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (!ev.detail.isNullOrBlank()) {
                                    Text(
                                        text = ev.detail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                if (!ev.suggestion.isNullOrBlank()) {
                                    Text(
                                        text = "💡 Fix: ${ev.suggestion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }

                // Matched & Missing Keywords Tags if present
                if (calculation.matchedList.isNotEmpty() || calculation.missingList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (calculation.matchedList.isNotEmpty()) {
                        Text(
                            text = "Matched Role Competencies (${calculation.matchedList.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(calculation.matchedList) { kw ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SoftGreen
                                ) {
                                    Text(
                                        text = "✓ $kw",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryDark,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (calculation.missingList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Missing High-Impact Keywords (${calculation.missingList.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(calculation.missingList) { kw ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ErrorRed.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = "+ $kw",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
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
fun AtsPriorityFixesCard(fixes: List<String>) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarningAmber.copy(alpha = 0.06f),
        borderColor = WarningAmber.copy(alpha = 0.3f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "High-Impact ATS Booster Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "Apply these prioritized changes to maximize resume passing rate:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            HorizontalDivider(color = BorderColor)

            fixes.forEachIndexed { index, fix ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = WarningAmber,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fix,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
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
