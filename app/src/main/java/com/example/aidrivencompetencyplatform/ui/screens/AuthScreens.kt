package com.example.aidrivencompetencyplatform.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.AstraApp
import com.example.aidrivencompetencyplatform.model.User
import com.example.aidrivencompetencyplatform.ui.components.GlassCard
import com.example.aidrivencompetencyplatform.ui.components.PremiumButton
import com.example.aidrivencompetencyplatform.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }
    val preferenceManager = remember { app.preferenceManager }
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )
        delay(1500L)
        
        val hasCompletedTour = try {
            preferenceManager.hasCompletedAppTour.first()
        } catch (e: Exception) {
            true // Default to true on error to avoid being stuck
        }
        
        if (sessionManager.isLoggedIn()) {
            if (!hasCompletedTour) {
                navController.navigate(Screen.AppTour.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AstraMind",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Elevate Your Career with AI",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

class OvershootInterpolator(private val tension: Float = 2f) : android.view.animation.Interpolator {
    override fun getInterpolation(t: Float): Float {
        var time = t
        time -= 1.0f
        return time * time * ((tension + 1) * time + tension) + 1.0f
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }
    val preferenceManager = remember { app.preferenceManager }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Sign in to continue your growth",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            GlassCard {
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    icon = Icons.Default.Email,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    PremiumButton(
                        text = "Sign In",
                        onClick = {
                            if (email.isBlank()) {
                                Toast.makeText(context, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }
                            if (!EMAIL_REGEX.matches(email.trim())) {
                                Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }
                            if (password.isBlank()) {
                                Toast.makeText(context, "Please enter your password.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }

                            isLoading = true
                            if (sessionManager.getUser(email) == null) {
                                Toast.makeText(context, "No account found with this email address. Please register first.", Toast.LENGTH_LONG).show()
                                isLoading = false
                            } else if (sessionManager.authenticate(email, password)) {
                                Toast.makeText(context, "Login successful.", Toast.LENGTH_SHORT).show()
                                sessionManager.login(email, isNewUser = false)
                                
                                scope.launch {
                                    val hasCompletedTour = try {
                                        preferenceManager.hasCompletedAppTour.first()
                                    } catch (e: Exception) {
                                        true
                                    }
                                    if (!hasCompletedTour) {
                                        navController.navigate(Screen.AppTour.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { navController.navigate(Screen.Signup.route) },
                enabled = !isLoading
            ) {
                Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.primary)
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
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val app = context.applicationContext as AstraApp
    val sessionManager = remember { app.sessionManager }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Start your AI-driven career journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            GlassCard {
                AuthTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    icon = Icons.Default.Email,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    PremiumButton(
                        text = "Sign Up",
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Please enter your name.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }
                            if (email.isBlank()) {
                                Toast.makeText(context, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }
                            if (!EMAIL_REGEX.matches(email.trim())) {
                                Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }
                            if (password.length < 6) {
                                Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                                return@PremiumButton
                            }

                            isLoading = true
                            val newUser = User(name = name.trim(), email = email.trim(), password = password)
                            if (sessionManager.register(newUser)) {
                                Toast.makeText(context, "Account created successfully.", Toast.LENGTH_SHORT).show()
                                sessionManager.login(email, isNewUser = true)
                                // Always show tour for signup
                                navController.navigate(Screen.AppTour.route) {
                                    popUpTo(Screen.Signup.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "An account with this email already exists.", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = !isLoading
            ) {
                Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary)
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
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    )
}
