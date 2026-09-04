package com.example.aidrivencompetencyplatform.ui.screens

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aidrivencompetencyplatform.ui.theme.*

/**
 * Isolated diagnostic screen to test Android IME / soft keyboard and hardware keyboard input.
 * Stripped of all AstraCard, custom wrappers, or complex nesting to isolate input pipeline:
 * Focus -> InputMethodManager -> IME / Hardware Keyboard -> Compose State -> UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardTestScreen(navController: NavController) {
    var bareText by remember { mutableStateOf("") }
    var outlinedText by remember { mutableStateOf("") }
    var bareFocused by remember { mutableStateOf(false) }
    var outlinedFocused by remember { mutableStateOf(false) }
    var lastKeyEventInfo by remember { mutableStateOf("None") }

    val bareFocusRequester = remember { FocusRequester() }
    val outlinedFocusRequester = remember { FocusRequester() }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    // IME insets detection to observe if IME is visible
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Diagnostic Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IME & Focus Telemetry",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Soft Keyboard Visible: ${if (isImeVisible) "YES (${imeBottomPx}px)" else "NO"}",
                        color = if (isImeVisible) SuccessGreen else TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Field 1 (Outlined) Focus: ${if (outlinedFocused) "FOCUSED" else "Unfocused"}",
                        color = if (outlinedFocused) Primary else TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Field 2 (Basic) Focus: ${if (bareFocused) "FOCUSED" else "Unfocused"}",
                        color = if (bareFocused) Primary else TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Last Key Event: $lastKeyEventInfo",
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { keyboardController?.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Show Keyboard", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { keyboardController?.hide() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hide Keyboard", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { focusManager.clearFocus() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Focus", fontSize = 12.sp)
                }
            }

            // Test 1: Bare OutlinedTextField
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Test 1: Standard OutlinedTextField",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Direct Material3 field with no wrappers",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = outlinedText,
                        onValueChange = { outlinedText = it },
                        label = { Text("Standard Outlined Input") },
                        placeholder = { Text("Type here...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { bareFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(outlinedFocusRequester)
                            .onFocusChanged { outlinedFocused = it.isFocused }
                            .onKeyEvent { keyEvent ->
                                val native = keyEvent.nativeKeyEvent
                                lastKeyEventInfo = "Code=${native.keyCode}, Action=${native.action}, Char='${native.unicodeChar.toChar()}'"
                                false
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Value: \"$outlinedText\" (Length: ${outlinedText.length})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            // Test 2: Bare BasicTextField
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Test 2: Minimal BasicTextField",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Raw Compose primitive without Material3 decoration",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, if (bareFocused) Primary else BorderColor, RoundedCornerShape(8.dp))
                            .background(SurfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        if (bareText.isEmpty() && !bareFocused) {
                            Text("Raw BasicTextField placeholder...", color = TextMuted, fontSize = 15.sp)
                        }
                        BasicTextField(
                            value = bareText,
                            onValueChange = { bareText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(bareFocusRequester)
                                .onFocusChanged { bareFocused = it.isFocused }
                                .onKeyEvent { keyEvent ->
                                    val native = keyEvent.nativeKeyEvent
                                    lastKeyEventInfo = "Basic: Code=${native.keyCode}, Action=${native.action}"
                                    false
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Value: \"$bareText\" (Length: ${bareText.length})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            // Return Button
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Return to Login Screen")
            }
        }
    }
}
