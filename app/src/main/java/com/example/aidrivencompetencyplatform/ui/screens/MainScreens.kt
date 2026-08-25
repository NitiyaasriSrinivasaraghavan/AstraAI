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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.model.*
import com.example.aidrivencompetencyplatform.ui.components.GlassCard
import com.example.aidrivencompetencyplatform.ui.components.PremiumButton
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import com.example.aidrivencompetencyplatform.viewmodel.*

@Composable
fun AppTourScreen(navController: NavController, viewModel: DashboardViewModel) {
    var currentStep by remember { mutableStateOf(1) }
    val steps = 5

    val tourData = listOf(
        "Welcome to your AI Career Companion" to "AstraMind helps you navigate your professional journey with advanced AI intelligence.",
        "Analyze Resume" to "Upload your resume and select the role you are targeting to get a deep analysis.",
        "ATS Analysis" to "Understand your resume's ATS compatibility and why you received your score.",
        "Skill Gap Analysis" to "Compare your skills with the skills required for your target role and identify areas to grow.",
        "Astra AI" to "Ask Astra questions about your resume, ATS results, skill gaps, or the application."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
                },
                label = "TourContent"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(step) {
                                1 -> Icons.Default.AutoAwesome
                                2 -> Icons.Default.Description
                                3 -> Icons.Default.Assessment
                                4 -> Icons.Default.Psychology
                                else -> Icons.Default.Chat
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = tourData[step - 1].first,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = tourData[step - 1].second,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(steps) { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i + 1 == currentStep) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }

        if (currentStep < steps) {
            TextButton(
                onClick = {
                    viewModel.completeTour()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.AppTour.route) { inclusive = true }
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text("Skip")
            }
            
            PremiumButton(
                text = "Next",
                onClick = { currentStep++ },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            PremiumButton(
                text = "Get Started",
                onClick = {
                    viewModel.completeTour()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.AppTour.route) { inclusive = true }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(navController: NavController, viewModel: DashboardViewModel) {
    val userName by viewModel.userName.collectAsState()
    val greetingPrefix by viewModel.greetingPrefix.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AstraMind", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "$greetingPrefix, $userName!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Your AI-powered career companion is ready to help you grow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.ResumeUpload.route) },
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("📄 Analyze Resume", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Upload your resume to get deep career insights.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DashboardNavCard(
                            title = "ATS Analysis",
                            subtitle = "Check compatibility",
                            icon = Icons.Default.Assessment,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.AtsAnalysis.route) }
                        )
                        DashboardNavCard(
                            title = "Skill Gap",
                            subtitle = "Identify gaps",
                            icon = Icons.Default.Psychology,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.SkillGap.route) }
                        )
                    }
                }

                item {
                    SectionHeader(title = "Platform Highlights")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HighlightItem("Personalized Insights", "AI analysis tailored to your specific target roles.")
                        HighlightItem("Real-time Feedback", "Astra AI is here to answer your questions instantly.")
                        HighlightItem("Data-Driven Growth", "Understand exactly what skills you need to reach your goals.")
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            FloatingAstraBot(navController, "Main Dashboard")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtsDashboardScreen(navController: NavController, viewModel: AtsViewModel) {
    val analysis by viewModel.analysisResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ATS Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (analysis == null) {
                NoAnalysisState(navController)
            } else {
                val breakdown = analysis!!.atsBreakdown ?: AtsBreakdown()
                // Mathematically calculated score as per formula
                val calculatedScore = (breakdown.keywordCoverage * 0.35 + 
                                       breakdown.resumeStructure * 0.25 + 
                                       breakdown.formattingSafety * 0.20 + 
                                       breakdown.parsingAccuracy * 0.20).toInt()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ATS COMPATIBILITY", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "$calculatedScore / 100",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Calculated compatibility based on key ATS criteria.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Scoring Breakdown")
                        val explanations = breakdown.explanations ?: AtsExplanations()
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AtsScoreCard(
                                title = "Keyword Coverage",
                                score = breakdown.keywordCoverage,
                                weight = "35%",
                                description = "How well your resume matches the target role's keywords.",
                                evidence = explanations.keywords ?: emptyList()
                            )
                            AtsScoreCard(
                                title = "Resume Structure",
                                score = breakdown.resumeStructure,
                                weight = "25%",
                                description = "Standard sections like Experience, Education, and Skills.",
                                evidence = explanations.structure ?: emptyList()
                            )
                            AtsScoreCard(
                                title = "Formatting Safety",
                                score = breakdown.formattingSafety,
                                weight = "20%",
                                description = "Avoiding complex layouts that confuse ATS parsers.",
                                evidence = explanations.formatting ?: emptyList()
                            )
                            AtsScoreCard(
                                title = "Parsing Accuracy",
                                score = breakdown.parsingAccuracy,
                                weight = "20%",
                                description = "How accurately the AI extracted your information.",
                                evidence = explanations.parsing ?: emptyList()
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Resume Strengths")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (analysis!!.strengths ?: emptyList()).forEach { s ->
                                Text("✓ $s", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Areas to Improve")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (analysis!!.weaknesses ?: emptyList()).forEach { w ->
                                Text("• $w", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            FloatingAstraBot(navController, "ATS Dashboard")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillGapDashboardScreen(navController: NavController, viewModel: SkillGapViewModel) {
    val analysis by viewModel.analysisResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill Gap Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (analysis == null) {
                NoAnalysisState(navController)
            } else {
                val skills = analysis!!.extractedSkills ?: emptyList()
                val matchPercentage = analysis!!.skillMatch

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SKILL MATCH", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "$matchPercentage%",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text("Match for: ${(analysis!!.summary ?: "").take(30)}...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "✓ Strong Skills")
                        SkillList(skills.filter { it.level >= 80 })
                    }

                    item {
                        SectionHeader(title = "⚠ Developing Skills")
                        SkillList(skills.filter { it.level in 40..79 })
                    }

                    item {
                        SectionHeader(title = "✗ Skill Gaps")
                        SkillList(skills.filter { it.level < 40 })
                    }

                    val missing = analysis!!.missingSections ?: emptyList()
                    if (missing.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Missing Requirements")
                            missing.forEach { req ->
                                Text("• $req", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            FloatingAstraBot(navController, "Skill Gap Dashboard")
        }
    }
}

@Composable
fun SkillList(skills: List<Skill>) {
    if (skills.isEmpty()) {
        Text("No skills found in this category.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            skills.forEach { skill ->
                var expanded by remember { mutableStateOf(false) }
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    cornerRadius = 12.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(skill.name, fontWeight = FontWeight.Bold)
                            Text("${skill.level}%", color = MaterialTheme.colorScheme.primary)
                        }
                        if (expanded && !skill.evidence.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Evidence: ${skill.evidence}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtsScoreCard(title: String, score: Int, weight: String, description: String, evidence: List<AtsEvidence>) {
    var expanded by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        cornerRadius = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Weight: $weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "$score/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (score >= 70) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
            Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Why this score?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                evidence.forEach { ev ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(if (ev.isDetected) "✓" else "⚠", color = if (ev.isDetected) Color(0xFF4CAF50) else Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(ev.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            if (!ev.detail.isNullOrBlank()) Text(ev.detail, style = MaterialTheme.typography.labelSmall)
                            if (!ev.suggestion.isNullOrBlank()) Text("💡 ${ev.suggestion}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardNavCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(
        modifier = modifier.height(140.dp).clickable { onClick() },
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HighlightItem(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun NoAnalysisState(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Analysis Data Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Complete your first resume analysis to see your career progress and AI insights.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        PremiumButton(text = "Analyze Resume", onClick = { navController.navigate(Screen.ResumeUpload.route) })
    }
}

@Composable
fun BoxScope.FloatingAstraBot(navController: NavController, screenContext: String) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
            .size(56.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .clickable { navController.navigate(Screen.AiAssistant.route) },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = "Ask Astra", tint = Color.White)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

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
    
    var targetRole by remember { mutableStateOf("") }
    var showRoleDialog by remember { mutableStateOf(false) }
    val roles = listOf("Android Developer", "Backend Developer", "Full Stack Developer", "Data Scientist", "DevOps Engineer", "Other")

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it) ?: "Unknown File"
            if (name.endsWith(".pdf", ignoreCase = true) || 
                name.endsWith(".doc", ignoreCase = true) || 
                name.endsWith(".docx", ignoreCase = true)) {
                viewModel.onFileSelected(it, name)
            } else {
                Toast.makeText(context, "Please select a PDF or DOCX file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Analysis") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = loadingStage, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (errorMessage != null) {
                        item {
                            GlassCard(containerColor = Color.Red.copy(alpha = 0.1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(errorMessage!!, color = Color.Red)
                                }
                            }
                        }
                    }

                    item {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = if (selectedFileName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (selectedFileName != null) {
                            Text("File: $selectedFileName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { filePickerLauncher.launch("*/*") }) { Text("Change File") }
                        } else {
                            PremiumButton(text = "Select Resume (PDF/DOCX)", onClick = { filePickerLauncher.launch("*/*") })
                        }
                    }

                    if (selectedFileName != null) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth().clickable { showRoleDialog = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Target Role", style = MaterialTheme.typography.labelSmall)
                                        Text(if (targetRole.isEmpty()) "Select your goal" else targetRole, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            PremiumButton(
                                text = if (isLoading) "Analyzing Resume..." else "Start AI Analysis",
                                onClick = {
                                    if (!isLoading) {
                                        if (targetRole.isBlank()) {
                                            Toast.makeText(context, "Please select a target role first.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.analyzeResume(targetRole)
                                        }
                                    }
                                },
                                containerColor = if (isLoading) Color.Gray else MaterialTheme.colorScheme.secondary,
                                enabled = !isLoading
                            )
                        }
                    }
                }
            }
        }

        if (showRoleDialog) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = false },
                title = { Text("Select Target Role") },
                text = {
                    Column {
                        roles.forEach { role ->
                            TextButton(
                                onClick = {
                                    targetRole = role
                                    showRoleDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(role, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showRoleDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun CircularScore(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = score / 100f,
                modifier = Modifier.size(60.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Text(
                text = "$score%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AppBottomBar(navController: NavController) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Home") },
            selected = true,
            onClick = { navController.navigate(Screen.Dashboard.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Astra AI") },
            selected = false,
            onClick = { navController.navigate(Screen.AiAssistant.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(navController: NavController, viewModel: AiAssistantViewModel) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Astra AI Assistant") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = false
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message ->
                    ChatBubble(message.text, message.isFromUser)
                }
            }
            
            Surface(shadowElevation = 8.dp) {
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
                        placeholder = { Text("Ask Astra anything...") },
                        shape = RoundedCornerShape(24.dp)
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
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewPrepScreen(navController: NavController, viewModel: AiAssistantViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isInterviewMode by viewModel.isInterviewMode.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Interview Prep") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (!isInterviewMode) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ready for a Mock Interview?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Practice technical and behavioral questions tailored to your resume and target role.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            PremiumButton(text = "Start Mock Interview", onClick = { viewModel.startInterview() })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(messages) { message ->
                        ChatBubble(message.text, message.isFromUser)
                    }
                }
                
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your answer...") },
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionAnalyzerScreen(navController: NavController, viewModel: ResumeViewModel) {
    var jobDescription by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    val analysisResult by viewModel.analysisResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JD Match Analyzer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Paste Job Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("Enter the job responsibilities and requirements here...") },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                PremiumButton(
                    text = if (analyzing) "Analyzing..." else "Analyze Match",
                    onClick = {
                        if (jobDescription.isNotBlank()) {
                            analyzing = true
                            viewModel.analyzeResume("Job Description Match")
                        }
                    }
                )
            }

            if (analysisResult != null) {
                item {
                    GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Job Match Score", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularScore(label = "Fit Score", score = 75) // Mocked for JD match
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StubScreen(title: String, navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$title Screen coming soon...", style = MaterialTheme.typography.titleMedium)
            if (title == "Profile" || title == "Settings") {
                Spacer(modifier = Modifier.height(24.dp))
                PremiumButton(
                    text = "Logout",
                    onClick = {
                        sessionManager.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.width(200.dp),
                    containerColor = Color(0xFFEF4444)
                )
            }
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
