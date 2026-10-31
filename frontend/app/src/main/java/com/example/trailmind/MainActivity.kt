package com.example.trailmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.trailmind.ui.auth.LoginScreen
import com.example.trailmind.ui.auth.RegisterScreen
import com.example.trailmind.ui.budget.BudgetScreen
import com.example.trailmind.ui.home.HomeScreenWithDrawer
import com.example.trailmind.ui.splash.SplashScreen
import com.example.trailmind.ui.theme.TrailMindTheme
import com.example.trailmind.data.local.TokenStore
import androidx.compose.ui.platform.LocalContext

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
    var currentScreen by remember { mutableStateOf("splash") }
    val context = LocalContext.current

    when (currentScreen) {
        "splash" -> SplashScreen(
            onSplashFinished = { currentScreen = "login" }
        )

        "login" -> LoginScreen(
            onLoginSuccess = { currentScreen = "home" },
            onSignUpClick = { currentScreen = "register" }
        )

        "register" -> RegisterScreen(
            onRegisterSuccess = { currentScreen = "home" },
            onBackToLogin = { currentScreen = "login" }
        )

        "home" -> HomeScreenWithDrawer(
            onLogout = {
                TokenStore(context).clear()
                currentScreen = "login"
            },
            onBudgetClick = { currentScreen = "budget" },
            // ✅ UPDATED: Removed onPlanTripClick, added these two
            onStartAiChat = { currentScreen = "trip_chat" },
            onStartWizard = { currentScreen = "trip_wizard" },
            onCategoryClick = { _: String -> },
            onPlaceClick = { _: com.example.trailmind.ui.home.PopularPlace -> }
        )

        "budget" -> BudgetScreen(
            onBack = { currentScreen = "home" }
        )

        // ✅ NEW PLACEHOLDER SCREENS (so clicking the sheet options doesn't crash)
        "trip_chat" -> PlaceholderScreen(
            title = "AI Chat Screen (Coming Soon)",
            onBack = { currentScreen = "home" }
        )

        "trip_wizard" -> PlaceholderScreen(
            title = "Guided Questions (Coming Soon)",
            onBack = { currentScreen = "home" }
        )
    }
}

// ✅ NEW: Simple placeholder screen for upcoming features
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
        // You can add a back button here later if needed.
        // For now, tapping "Back" in the top bar will come later.
        // We'll rely on the onBack lambda passed in.
    }
}