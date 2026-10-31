package com.example.trailmind.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// One drawer entry
private data class DrawerItem(val label: String, val icon: ImageVector)

private val drawerItems = listOf(
    DrawerItem("Home", Icons.Default.Home),
    DrawerItem("My Trips", Icons.Default.Map),
    DrawerItem("Saved Places", Icons.Default.Favorite),
    DrawerItem("Budget", Icons.Default.AccountBalanceWallet),
    DrawerItem("Profile", Icons.Default.Person),
    DrawerItem("Settings", Icons.Default.Settings)
)

@Composable
fun HomeScreenWithDrawer(
    userName: String = "Traveller",
    userEmail: String = "",
    onLogout: () -> Unit,
    onBudgetClick: () -> Unit = {},
    // ✅ REPLACED onPlanTripClick with these two
    onStartAiChat: () -> Unit = {},
    onStartWizard: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onPlaceClick: (PopularPlace) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("Home") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.fillMaxWidth(0.78f)
            ) {
                // ── Header: avatar + name + email ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TrailBlue)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Navigation items ──
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        selected = selectedItem == item.label,
                        onClick = {
                            selectedItem = item.label
                            scope.launch { drawerState.close() }
                            if (item.label == "Budget") onBudgetClick()
                            // hook up real navigation per item later
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = TrailBlueFaint,
                            selectedIconColor = TrailBlue,
                            selectedTextColor = TrailBlue
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // ── Pushes logout to the bottom ──
                Spacer(Modifier.weight(1f))

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // ── Logout button, pinned at bottom ──
                NavigationDrawerItem(
                    label = { Text("Logout", color = Color(0xFFD32F2F)) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFD32F2F)
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        // ── Main content: your existing HomeScreen ──
        HomeScreen(
            onMenuClick = { scope.launch { drawerState.open() } },
            // ✅ CHANGED: Pass the new callbacks instead of onPlanTripClick
            onStartAiChat = onStartAiChat,
            onStartWizard = onStartWizard,
            onCategoryClick = onCategoryClick,
            onPlaceClick = onPlaceClick
        )
    }
}