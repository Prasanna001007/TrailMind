package com.example.trailmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.trailmind.ui.auth.LoginScreen
import com.example.trailmind.ui.home.HomeScreen
import com.example.trailmind.ui.splash.SplashScreen
import com.example.trailmind.ui.theme.TrailMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrailMindTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    // currentScreen tracks which screen is visible
    // starts at splash → login → home
    var currentScreen by remember { mutableStateOf("splash") }

    when (currentScreen) {
        "splash" -> SplashScreen(
            onSplashFinished = { currentScreen = "login" }
        )
        "login" -> LoginScreen(
            onLoginSuccess = { currentScreen = "home" },
            onSignUpClick = { /* Sign up screen — build later */ }
        )
        "home" -> HomeScreen(
            onPlanTripClick = { /* Plan My Trip wizard — build next */ },
            onCategoryClick = { route ->
                // route will be "hotels", "flights", "buses" etc.
                // navigate to respective screen — build later
            },
            onPlaceClick = { place ->
                // navigate to place detail screen — build later
            }
        )
    }
}