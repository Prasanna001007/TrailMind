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
// These represent the shape of data used in the UI.
// Later, these will come from the backend via Retrofit.
// ─────────────────────────────────────────────

// Represents one category tile in the grid (Hotels, Flights, etc.)
data class CategoryItem(
    val label: String,       // Display name
    val icon: ImageVector,   // Icon from Material Icons
    val route: String        // Navigation route identifier (used later)
)

// Represents one popular destination card
data class PopularPlace(
    val name: String,        // Destination name
    val location: String,    // Region/area
    val price: String,       // Starting price per person
    val rating: Float        // Star rating out of 5
)

// ─────────────────────────────────────────────
// STATIC DATA
// Hardcoded for now. Will be replaced with
// API calls once the backend is connected.
// ─────────────────────────────────────────────

// Category grid items shown on the home screen
val categories = listOf(
    CategoryItem("Hotels",   Icons.Default.Hotel,          "hotels"),
    CategoryItem("Flights",  Icons.Default.Flight,         "flights"),
    CategoryItem("Buses",    Icons.Default.DirectionsBus,  "buses"),
    CategoryItem("Places",   Icons.Default.Place,          "places"),
    CategoryItem("Foods",    Icons.Default.Restaurant,     "foods"),
    CategoryItem("Beaches",  Icons.Default.BeachAccess,    "beaches")
)

// Popular Nepal destinations shown in the horizontal scroll list
val popularPlaces = listOf(
    PopularPlace("Pokhara",           "Gandaki Province, Nepal",       "NPR 8,000",  4.8f),
    PopularPlace("Everest Base Camp", "Khumbu Region, Nepal",          "NPR 45,000", 4.9f),
    PopularPlace("Chitwan",           "Bagmati Province, Nepal",       "NPR 6,500",  4.5f),
    PopularPlace("Annapurna Circuit", "Gandaki Province, Nepal",       "NPR 25,000", 4.7f),
    PopularPlace("Lumbini",           "Lumbini Province, Nepal",       "NPR 5,000",  4.4f),
    PopularPlace("Mustang",           "Gandaki Province, Nepal",       "NPR 60,000", 4.6f)
)

// ─────────────────────────────────────────────
// MAIN HOME SCREEN COMPOSABLE
// This is the root screen shown after login.
// It contains:
//   1. Top header with greeting + notification bell
//   2. Hero banner with destination image + search bar
//   3. "Plan My Trip" CTA button
//   4. Category grid (Hotels, Flights, Buses etc.)
//   5. Popular Places horizontal scroll list
// ─────────────────────────────────────────────
@Composable
fun HomeScreen(
    onPlanTripClick: () -> Unit = {},       // Called when "Plan My Trip" is tapped
    onCategoryClick: (String) -> Unit = {}, // Called with route string when category tapped
    onPlaceClick: (PopularPlace) -> Unit = {} // Called when a place card is tapped
) {
    // Scroll state allows the entire screen to scroll vertically
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)) // Light grey background for the page
            .verticalScroll(scrollState)
    ) {

        // ── SECTION 1: TOP HEADER ──────────────────────────
        // Shows greeting on the left and notification bell on the right
        HomeTopHeader()

        // ── SECTION 2: HERO BANNER ────────────────────────
        // Full-width banner with gradient background simulating
        // a destination image, plus a search bar overlay
        HeroBanner()

        Spacer(modifier = Modifier.height(20.dp))

        // ── SECTION 3: PLAN MY TRIP BUTTON ───────────────
        // This is the entry point for the AI-powered trip
        // planning wizard (onboarding questions flow).
        // Tapping it will later navigate to PlanTripScreen.
        PlanMyTripButton(onClick = onPlanTripClick)

        Spacer(modifier = Modifier.height(24.dp))

        // ── SECTION 4: CATEGORY GRID ─────────────────────
        // 2-row grid showing quick navigation options.
        // Each tile navigates to a dedicated screen.
        CategoryGrid(
            categories = categories,
            onCategoryClick = onCategoryClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── SECTION 5: POPULAR PLACES ────────────────────
        // Horizontally scrollable list of Nepal destinations.
        // Data is hardcoded now; will come from backend later.
        PopularPlacesSection(
            places = popularPlaces,
            onPlaceClick = onPlaceClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────
// COMPONENT: TOP HEADER
// Greeting + user avatar + notification bell
// ─────────────────────────────────────────────
@Composable
fun HomeTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: greeting text
        Column {
            Text(
                text = "Good Morning 👋",
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )
            Text(
                text = "Where to next?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
        }

        // Right side: notification bell inside a circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(TrailBlueFaint)
                .clickable { /* navigate to notifications later */ },
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
// Large banner at the top with a gradient that
// simulates a scenic Nepal destination photo.
// Contains an overlaid search bar.
// Note: Replace gradient with real Image() composable
// once we have actual destination photos.
// ─────────────────────────────────────────────
@Composable
fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            // Gradient simulates a mountain/sky photo for now
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
        // Decorative circle overlays to make the banner feel alive
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
            // Banner headline text
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

            // Search bar overlaid on the banner
            // Will navigate to a search screen when tapped
            SearchBarPlaceholder()
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: SEARCH BAR PLACEHOLDER
// A tappable search bar shown on the hero banner.
// Not a real text field — tapping it will later
// open a dedicated search screen.
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
// This is the primary CTA (Call To Action) button.
// Tapping this opens the step-by-step trip planning
// wizard where the user answers questions about their
// trip (duration, budget, preferences etc.) and
// receives an AI-generated itinerary.
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
            // Map icon on the left
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            // Button label
            Text(
                text = "Plan My Trip",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            // Arrow icon on the right to hint navigation
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
// A 2-row grid of category tiles.
// Each tile shows an icon + label.
// Tapping a tile calls onCategoryClick with the
// route string (e.g. "hotels", "flights") which
// will be used by the NavGraph to navigate later.
// ─────────────────────────────────────────────
@Composable
fun CategoryGrid(
    categories: List<CategoryItem>,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        // Section title
        Text(
            text = "Explore",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Split the list into rows of 3 tiles each
        // chunked(3) groups [A,B,C,D,E,F] into [[A,B,C],[D,E,F]]
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
// Individual tile inside the category grid.
// White card with icon + label centered.
// ─────────────────────────────────────────────
@Composable
fun CategoryTile(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // Makes each tile a perfect square
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
            // Icon inside a colored circle background
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
// Section title + horizontally scrollable row
// of destination cards showing Nepal places.
// ─────────────────────────────────────────────
@Composable
fun PopularPlacesSection(
    places: List<PopularPlace>,
    onPlaceClick: (PopularPlace) -> Unit
) {
    Column {
        // Section header with "See all" button
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

        // Horizontal scrolling list of place cards
        // LazyRow only renders cards that are visible on screen
        // which is more efficient than rendering all at once
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
// Individual destination card shown in the
// Popular Places horizontal list.
// Shows: image placeholder, name, location,
// price per person, and star rating.
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
            // Image placeholder (gradient box)
            // Replace this Box with AsyncImage (Coil library)
            // once we have real destination photos from the backend
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

                // Wishlist / heart button on the top right of image
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

            // Card content below the image
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Destination name
                Text(
                    text = place.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Location with pin icon
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

                // Price + Star rating on the same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Starting price
                    Text(
                        text = place.price,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrailBlue
                    )

                    // Star rating badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107), // amber/yellow star
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