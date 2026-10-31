package com.example.trailmind.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.util.Resource

// Brand colors
val TrailBlue = Color(0xFF1F5C8B)
val TrailBlueDark = Color(0xFF163D5C)
val TrailBlueMid = Color(0xFF2A72A8)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isEmailValid = email.contains("@") && email.contains(".")
    val isFormValid = isEmailValid && password.isNotBlank()

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val isLoading = uiState is Resource.Loading

    // navigate on success, snackbar on error
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is Resource.Success -> {
                viewModel.reset()      // clear so it won't re-fire on next visit
                onLoginSuccess()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(TrailBlueMid, TrailBlue, TrailBlueDark)
                    )
                )
        ) {
            // Back button
            IconButton(
                onClick = { /* handle back */ },
                modifier = Modifier
                    .padding(16.dp)
                    .statusBarsPadding()
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                // Logo + App name
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_compass),
                        contentDescription = "TrailMind Logo",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TrailMind",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // White card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sign in",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Email address",
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                trailingIcon = {
                                    if (isEmailValid) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(TrailBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Valid",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrailBlue,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                placeholder = {
                                    Text(
                                        "you@example.com",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Password",
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !isLoading,
                                visualTransformation = if (passwordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (passwordVisible)
                                                    android.R.drawable.ic_menu_view
                                                else
                                                    android.R.drawable.ic_secure
                                            ),
                                            contentDescription = if (passwordVisible)
                                                "Hide password"
                                            else
                                                "Show password",
                                            tint = Color(0xFF888888),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrailBlue,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                placeholder = {
                                    Text(
                                        "••••••••",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }

                        // Forgot password
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { /* handle forgot password */ },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    color = TrailBlue,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sign in button
                        Button(
                            onClick = {
                                keyboard?.hide()
                                viewModel.login(email, password)
                            },
                            enabled = isFormValid && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrailBlue,
                                disabledContainerColor = TrailBlue.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Sign in",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider with text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFCCCCCC))
                            Text(
                                text = "  Or Login with  ",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFCCCCCC))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Social login buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Facebook
                            OutlinedButton(
                                onClick = { /* Facebook login */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFDDDDDD)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "f",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1877F2)
                                )
                            }

                            // Google
                            OutlinedButton(
                                onClick = { /* Google login */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFDDDDDD)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDB4437)
                                )
                            }

                            // Apple
                            OutlinedButton(
                                onClick = { /* Apple login */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFDDDDDD)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF000000)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sign up link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = Color(0xFF666666),
                                fontSize = 13.sp
                            )
                            TextButton(
                                onClick = { onSignUpClick() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Sign up",
                                    color = TrailBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}