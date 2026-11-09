package com.example.trailmind.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.trailmind.ui.home.TrailBlue

// Shared colors for every text field: black text, white background, blue focus
@Composable
fun trailFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Black,
    cursorColor = TrailBlue,
    focusedBorderColor = TrailBlue,
    unfocusedBorderColor = Color(0xFFBBBBBB),
    errorBorderColor = Color(0xFFD32F2F),
    focusedLabelColor = TrailBlue,
    unfocusedLabelColor = Color(0xFF444444),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White
)