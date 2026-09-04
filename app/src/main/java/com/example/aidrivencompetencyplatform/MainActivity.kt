package com.example.aidrivencompetencyplatform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.aidrivencompetencyplatform.ui.navigation.AppNavGraph
import com.example.aidrivencompetencyplatform.ui.theme.AstraMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AstraMindTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}