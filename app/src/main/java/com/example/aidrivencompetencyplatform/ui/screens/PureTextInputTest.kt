package com.example.aidrivencompetencyplatform.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@Composable
fun PureTextInputTest() {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                Log.d("AUTH_INPUT_DEBUG", "onValueChange='$it'")
                text = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    Log.d(
                        "AUTH_INPUT_DEBUG",
                        "focused=${it.isFocused}, hasFocus=${it.hasFocus}"
                    )
                },
            singleLine = true
        )
    }
}
