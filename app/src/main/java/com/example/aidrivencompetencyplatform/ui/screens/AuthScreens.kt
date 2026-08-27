package com.example.aidrivencompetencyplatform.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.model.User
import com.example.aidrivencompetencyplatform.ui.components.AstraCard
import com.example.aidrivencompetencyplatform.ui.components.PremiumButton
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import com.example.aidrivencompetencyplatform.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun isValidEmail(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.isNotEmpty() && trimmed.contains("@") && trimmed.contains(".") && !trimmed.startsWith("@") && !trimmed.endsWith("@") && trimmed.length >= 5
}

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300)
        )
        delay(200L)
        
        if (sessionManager.isLoggedIn()) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Primary,
                                PrimaryLight
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AstraAI",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryDark,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Career Intelligence & Competency Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SoftGreen,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to AstraAI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sign in to elevate your career intelligence",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(28.dp))
            
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                AuthTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Email Address",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                PremiumButton(
                    text = "Sign In",
                    onClick = {
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()

                        if (trimmedEmail.isBlank()) {
                            errorMessage = "Please enter your email address."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }
                        if (!isValidEmail(trimmedEmail)) {
                            errorMessage = "Please enter a valid email address."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }
                        if (trimmedPassword.isBlank()) {
                            errorMessage = "Please enter your password."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }

                        val user = sessionManager.getUser(trimmedEmail)
                        if (user == null) {
                            errorMessage = "No account found with this email. Please sign up."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        } else if (sessionManager.authenticate(trimmedEmail, trimmedPassword)) {
                            errorMessage = null
                            sessionManager.login(trimmedEmail)
                            Toast.makeText(context, "Login successful.", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            errorMessage = "Incorrect password. Please try again."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = { navController.navigate(Screen.Signup.route) }
            ) {
                Text("Don't have an account? Sign Up", color = PrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SignupScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SoftGreen,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Create AstraAI Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Join your intelligent career companion",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(28.dp))
            
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                AuthTextField(
                    value = name,
                    onValueChange = { 
                        name = it 
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthTextField(
                    value = email,
                    onValueChange = { 
                        email = it 
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Email Address",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { 
                        password = it 
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it 
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Confirm Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                PremiumButton(
                    text = "Sign Up",
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()
                        val trimmedConfirmPassword = confirmPassword.trim()
                        
                        if (trimmedName.isBlank()) {
                            errorMessage = "Please enter your full name."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }
                        if (trimmedEmail.isBlank()) {
                            errorMessage = "Please enter your email address."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }
                        if (!isValidEmail(trimmedEmail)) {
                            errorMessage = "Please enter a valid email address."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }
                        if (trimmedPassword.length < 4) {
                            errorMessage = "Password must be at least 4 characters."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }
                        if (trimmedPassword != trimmedConfirmPassword) {
                            errorMessage = "Passwords do not match."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            return@PremiumButton
                        }

                        val existingUser = sessionManager.getUser(trimmedEmail)
                        if (existingUser != null) {
                            errorMessage = "An account with this email already exists. Please sign in."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            return@PremiumButton
                        }

                        // Create account
                        val newUser = User(name = trimmedName, email = trimmedEmail, password = trimmedPassword)
                        val success = sessionManager.register(newUser)
                        if (success) {
                            errorMessage = null
                            Toast.makeText(context, "Account created successfully! Please sign in.", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Signup.route) { inclusive = true }
                            }
                        } else {
                            errorMessage = "Failed to create account. Please try again."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text("Already have an account? Login", color = PrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Primary) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Primary,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant
        )
    )
}

