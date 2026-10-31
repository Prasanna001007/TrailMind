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

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val isEmailValid = email.contains("@") && email.contains(".")
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val isFormValid = name.isNotBlank() && isEmailValid && passwordsMatch && password.length >= 8

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val isLoading = uiState is Resource.Loading

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is Resource.Success -> onRegisterSuccess()
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
            IconButton(
                onClick = { onBackToLogin() },
                modifier = Modifier
                    .padding(16.dp)
                    .statusBarsPadding()
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(70.dp))

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

                Spacer(Modifier.height(12.dp))
                Text("TrailMind", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Create account", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                        Spacer(Modifier.height(24.dp))

                        // Full name
                        Column(Modifier.fillMaxWidth()) {
                            Text("Full name", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                singleLine = true, enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrailBlue, unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                                ),
                                placeholder = { Text("Your name", color = Color(0xFFAAAAAA), fontSize = 14.sp) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        // Email
                        Column(Modifier.fillMaxWidth()) {
                            Text("Email address", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = email, onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                singleLine = true, enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                trailingIcon = {
                                    if (isEmailValid) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(TrailBlue),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Default.Check, "Valid", tint = Color.White, modifier = Modifier.size(14.dp)) }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrailBlue, unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                                ),
                                placeholder = { Text("you@example.com", color = Color(0xFFAAAAAA), fontSize = 14.sp) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        // Password
                        Column(Modifier.fillMaxWidth()) {
                            Text("Password", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password, onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                singleLine = true, enabled = !isLoading,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            painter = painterResource(id = if (passwordVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_secure),
                                            contentDescription = "Toggle password", tint = Color(0xFF888888), modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrailBlue, unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                                ),
                                placeholder = { Text("••••••••", color = Color(0xFFAAAAAA), fontSize = 14.sp) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        // Confirm password
                        Column(Modifier.fillMaxWidth()) {
                            Text("Confirm password", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = confirmPassword, onValueChange = { confirmPassword = it },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                singleLine = true, enabled = !isLoading,
                                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    if (passwordsMatch) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(TrailBlue),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Default.Check, "Match", tint = Color.White, modifier = Modifier.size(14.dp)) }
                                    } else {
                                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                            Icon(
                                                painter = painterResource(id = if (confirmVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_secure),
                                                contentDescription = "Toggle password", tint = Color(0xFF888888), modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrailBlue, unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                                    errorBorderColor = Color(0xFFD32F2F)
                                ),
                                placeholder = { Text("••••••••", color = Color(0xFFAAAAAA), fontSize = 14.sp) }
                            )
                            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                Spacer(Modifier.height(4.dp))
                                Text("Passwords do not match", color = Color(0xFFD32F2F), fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(24.dp))

                        // Create account button — NOW CALLS THE BACKEND
                        Button(
                            onClick = {
                                keyboard?.hide()
                                viewModel.register(name, email, password)
                            },
                            enabled = isFormValid && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrailBlue,
                                disabledContainerColor = TrailBlue.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Create account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Already have an account? ", color = Color(0xFF666666), fontSize = 13.sp)
                            TextButton(onClick = { onBackToLogin() }, contentPadding = PaddingValues(0.dp)) {
                                Text("Sign in", color = TrailBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}