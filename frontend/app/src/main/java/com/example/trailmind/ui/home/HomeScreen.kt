package com.example.trailmind.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Brand Colors — reused throughout the app
// ─────────────────────────────────────────────
val TrailBlue = Color(0xFF1F5C8B)
val TrailBlueDark = Color(0xFF163D5C)
val TrailBlueMid = Color(0xFF2A72A8)
val TrailBlueFaint = Color(0xFFEBF3FB)

// ─────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────
data class CategoryItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

data class PopularPlace(
    val name: String,
    val location: String,
    val price: String,
    val rating: Float
)

// ─────────────────────────────────────────────
// STATIC DATA
// ─────────────────────────────────────────────

// ✅ FIXED: "Beaches" is now "Treks" with Terrain icon
val categories = listOf(
    CategoryItem("Hotels",   Icons.Default.Hotel,          "hotels"),
    CategoryItem("Flights",  Icons.Default.Flight,         "flights"),
    CategoryItem("Buses",    Icons.Default.DirectionsBus,  "buses"),
    CategoryItem("Places",   Icons.Default.Place,          "places"),
    CategoryItem("Foods",    Icons.Default.Restaurant,     "foods"),
    CategoryItem("Treks",    Icons.Default.Terrain,        "treks")   // CHANGED
)

val popularPlaces = listOf(
    PopularPlace("Pokhara",           "Gandaki Province, Nepal",       "NPR 8,000",  4.8f),
    PopularPlace("Everest Base Camp", "Khumbu Region, Nepal",          "NPR 45,000", 4.9f),
    PopularPlace("Chitwan",           "Bagmati Province, Nepal",       "NPR 6,500",  4.5f),
    PopularPlace("Annapurna Circuit", "Gandaki Province, Nepal",       "NPR 25,000", 4.7f),
    PopularPlace("Lumbini",           "Lumbini Province, Nepal",       "NPR 5,000",  4.4f),
    PopularPlace("Mustang",           "Gandaki Province, Nepal",       "NPR 60,000", 4.6f)
)

// ─────────────────────────────────────────────
// MAIN HOME SCREEN (UPDATED)
// ─────────────────────────────────────────────
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onStartAiChat: () -> Unit = {},      // NEW: replaces onPlanTripClick
    onStartWizard: () -> Unit = {},      // NEW: replaces onPlanTripClick
    onCategoryClick: (String) -> Unit = {},
    onPlaceClick: (PopularPlace) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showPlanSheet by remember { mutableStateOf(false) }   // NEW

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(scrollState)
    ) {
        HomeTopHeader(onMenuClick = onMenuClick)
        HeroBanner()
        Spacer(modifier = Modifier.height(20.dp))

        // CHANGED: opens the bottom sheet instead of navigating directly
        PlanMyTripButton(onClick = { showPlanSheet = true })

        Spacer(modifier = Modifier.height(24.dp))
        CategoryGrid(categories = categories, onCategoryClick = onCategoryClick)
        Spacer(modifier = Modifier.height(24.dp))
        PopularPlacesSection(places = popularPlaces, onPlaceClick = onPlaceClick)
        Spacer(modifier = Modifier.height(32.dp))
    }

    // NEW: Bottom Sheet
    if (showPlanSheet) {
        PlanTripChoiceSheet(
            onDismiss = { showPlanSheet = false },
            onChooseAiChat = onStartAiChat,
            onChooseWizard = onStartWizard
        )
    }
}

// ─────────────────────────────────────────────
// COMPONENT: TOP HEADER
// ─────────────────────────────────────────────
@Composable
fun HomeTopHeader(onMenuClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(TrailBlueFaint)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = TrailBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Good Morning 👋", fontSize = 13.sp, color = Color(0xFF888888))
                Text("Where to next?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(TrailBlueFaint)
                .clickable { /* notifications later */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = TrailBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: HERO BANNER
// ─────────────────────────────────────────────
@Composable
fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A72A8),
                        Color(0xFF1F5C8B),
                        Color(0xFF163D5C)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Explore Nepal",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Mountains · Culture · Adventure",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            SearchBarPlaceholder()
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: SEARCH BAR PLACEHOLDER
// ─────────────────────────────────────────────
@Composable
fun SearchBarPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .clickable { /* navigate to search screen later */ }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Where want to go?",
            color = Color(0xFF999999),
            fontSize = 14.sp
        )
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = TrailBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────
// COMPONENT: PLAN MY TRIP BUTTON
// ─────────────────────────────────────────────
@Composable
fun PlanMyTripButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrailBlue
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Plan My Trip",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: CATEGORY GRID
// ─────────────────────────────────────────────
@Composable
fun CategoryGrid(
    categories: List<CategoryItem>,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Explore",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(14.dp))

        categories.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryTile(
                        item = category,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(category.route) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: CATEGORY TILE
// ─────────────────────────────────────────────
@Composable
fun CategoryTile(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TrailBlueFaint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = TrailBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: POPULAR PLACES SECTION
// ─────────────────────────────────────────────
@Composable
fun PopularPlacesSection(
    places: List<PopularPlace>,
    onPlaceClick: (PopularPlace) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Popular Places",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            TextButton(onClick = { /* navigate to all places later */ }) {
                Text(
                    text = "See all",
                    color = TrailBlue,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(places) { place ->
                PlaceCard(
                    place = place,
                    onClick = { onPlaceClick(place) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: PLACE CARD
// ─────────────────────────────────────────────
@Composable
fun PlaceCard(
    place: PopularPlace,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(TrailBlueMid, TrailBlueDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Landscape,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = place.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = place.location,
                        fontSize = 11.sp,
                        color = Color(0xFF888888),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = place.price,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrailBlue
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = place.rating.toString(),
                            fontSize = 11.sp,
                            color = Color(0xFF555555),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// NEW COMPONENTS: PLAN TRIP CHOICE SHEET
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTripChoiceSheet(
    onDismiss: () -> Unit,
    onChooseAiChat: () -> Unit,
    onChooseWizard: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "How do you want to plan?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pick whichever feels easier",
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )
            Spacer(Modifier.height(20.dp))

            PlanOptionCard(
                icon = Icons.Default.Chat,
                title = "Chat with AI Guide",
                subtitle = "New to Nepal travel? Just talk it through — no forms.",
                onClick = { onDismiss(); onChooseAiChat() }
            )
            Spacer(Modifier.height(12.dp))
            PlanOptionCard(
                icon = Icons.Default.Checklist,
                title = "Guided Questions",
                subtitle = "Answer a few quick questions and get a plan instantly.",
                onClick = { onDismiss(); onChooseWizard() }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlanOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TrailBlueFaint),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TrailBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF666666))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF999999))
        }
    }
}