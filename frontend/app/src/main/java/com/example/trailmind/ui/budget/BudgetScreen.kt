package com.example.trailmind.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.util.Resource

private val TrailBlue = Color(0xFF1F5C8B)
private val TrailBlueFaint = Color(0xFFEBF3FB)
private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)
private val CardBorder = Color(0xFFE5E9F0)

private val cities = listOf(
    "Kathmandu", "Chitwan", "Pokhara", "Lumbini", "Itahari", "Surkhet", "Nepalgunj"
)

// destination -> (altitude, isRestricted, hasPorter) : mirrors backend, display only
private val destMeta = linkedMapOf(
    "Everest Base Camp" to Triple(5364, false, true),
    "Annapurna Circuit" to Triple(5416, false, true),
    "Langtang Valley" to Triple(3870, false, true),
    "Mardi Himal" to Triple(4500, false, true),
    "Manaslu Circuit" to Triple(5106, true, true),
    "Gokyo Lakes" to Triple(4790, false, true),
    "Upper Mustang" to Triple(3840, true, true),
    "Muktinath" to Triple(3710, false, false),
    "Poon Hill (Ghorepani)" to Triple(3210, false, true),
    "Nagarkot" to Triple(2175, false, false),
    "Shivapuri National Park" to Triple(2732, false, false),
    "Bandipur Trails" to Triple(1030, false, false),
    "Dhampus / Australian Camp" to Triple(2060, false, true),
    "Champadevi / Chandragiri Trail" to Triple(2552, false, true),
    "Pashupatinath" to Triple(1400, false, false),
    "Boudhanath" to Triple(1400, false, false),
    "Swayambhunath" to Triple(1400, false, false),
    "Bhaktapur Durbar Square" to Triple(1401, false, false),
    "Lumbini" to Triple(150, false, false),
    "Janakpur (Janaki Mandir)" to Triple(70, false, false),
    "Sirubari" to Triple(1700, false, false),
    "Ghalegaun" to Triple(2100, false, false),
    "Bhujung Village" to Triple(1600, false, false),
    "Amaltari Community Homestay" to Triple(180, false, false),
    "Chitwan National Park" to Triple(415, false, false),
    "Bardia National Park" to Triple(415, false, false),
    "Sagarmatha National Park" to Triple(3440, false, true),
)
private val destinations = destMeta.keys.toList()

private val months = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val nationalities = listOf("Domestic", "SAARC", "Foreign")
private val travelerTypes = listOf("Solo", "Group")
private val tiers = listOf("Budget", "Standard", "Luxury")
private val transports = listOf("Public Bus", "Private Jeep", "Flight")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel()
) {
    var startingCity by remember { mutableStateOf(cities.first()) }
    var destination by remember { mutableStateOf(destinations.first()) }
    var durationDays by remember { mutableStateOf("7") }
    var groupSize by remember { mutableStateOf("2") }
    var month by remember { mutableStateOf(months.first()) }
    var nationality by remember { mutableStateOf(nationalities.first()) }
    var travelerType by remember { mutableStateOf(travelerTypes.last()) }
    var tier by remember { mutableStateOf(tiers[1]) }
    var transport by remember { mutableStateOf(transports.first()) }
    var useGuide by remember { mutableStateOf(true) }
    var usePorter by remember { mutableStateOf(true) }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is Resource.Loading

    val (altitude, restricted, hasPorter) = destMeta[destination] ?: Triple(0, false, false)

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Budget Estimator", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrailBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            // ── SECTION: ROUTE ──────────────────────────
            SectionCard(title = "Route", icon = Icons.Default.Map) {
                Dropdown("Starting city", startingCity, cities, Icons.Default.MyLocation) { startingCity = it }
                Spacer(Modifier.height(12.dp))
                Dropdown("Destination", destination, destinations, Icons.Default.Flag) { destination = it }
                Spacer(Modifier.height(12.dp))

                // altitude / restricted insight strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrailBlueFaint)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightItem(
                        icon = Icons.Default.Terrain,
                        label = "Max altitude",
                        value = "$altitude m",
                        valueColor = TrailBlue
                    )
                    InsightItem(
                        icon = if (restricted) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        label = "Restricted area",
                        value = if (restricted) "Yes — permit" else "No",
                        valueColor = if (restricted) Color(0xFFD32F2F) else TrailBlue,
                        alignEnd = true
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── SECTION: TRIP DETAILS ───────────────────
            SectionCard(title = "Trip Details", icon = Icons.Default.CalendarMonth) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Days", durationDays, Icons.Default.Schedule, Modifier.weight(1f)) { durationDays = it }
                    NumberField("Group size", groupSize, Icons.Default.Groups, Modifier.weight(1f)) { groupSize = it }
                }
                Spacer(Modifier.height(12.dp))
                Dropdown("Travel month", month, months, Icons.Default.CalendarMonth) { month = it }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        Dropdown("Nationality", nationality, nationalities, Icons.Default.Public) { nationality = it }
                    }
                    Box(Modifier.weight(1f)) {
                        Dropdown("Traveler type", travelerType, travelerTypes, Icons.Default.Person) { travelerType = it }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── SECTION: PREFERENCES ────────────────────
            SectionCard(title = "Preferences", icon = Icons.Default.Tune) {
                Dropdown("Accommodation", tier, tiers, Icons.Default.Hotel) { tier = it }
                Spacer(Modifier.height(12.dp))
                Dropdown("Transport", transport, transports, Icons.Default.DirectionsBus) { transport = it }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(Modifier.height(12.dp))

                ToggleRow(
                    icon = Icons.Default.Hiking,
                    title = "Hire a guide",
                    subtitle = null,
                    checked = useGuide,
                    enabled = true,
                    onCheckedChange = { useGuide = it }
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    icon = Icons.Default.Backpack,
                    title = "Hire a porter",
                    subtitle = if (!hasPorter) "Not available here" else null,
                    checked = usePorter && hasPorter,
                    enabled = hasPorter,
                    onCheckedChange = { usePorter = it }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── CTA BUTTON (matches Home's PlanMyTripButton style) ──
            Button(
                onClick = {
                    viewModel.predict(
                        BudgetRequest(
                            starting_city = startingCity,
                            destination = destination,
                            duration_days = durationDays.toIntOrNull() ?: 1,
                            group_size = groupSize.toIntOrNull() ?: 1,
                            travel_month = month,
                            nationality_category = nationality,
                            traveler_type = travelerType,
                            accommodation_tier = tier,
                            transport_mode = transport,
                            use_guide = useGuide,
                            use_porter = usePorter && hasPorter
                        )
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrailBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Estimate Budget", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val s = uiState) {
                is Resource.Success -> {
                    val r = s.data
                    val b = r.breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            // header strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TrailBlueFaint)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text("Estimated Total", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                    Text(
                                        "NPR ${"%,.0f".format(r.itemized_total_npr)}",
                                        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TrailBlue
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            CostLine(Icons.Default.Badge, "Permit", b.permit_npr)
                            if (b.guide_npr > 0) CostLine(Icons.Default.Hiking, "Guide", b.guide_npr)
                            if (b.porter_npr > 0) CostLine(Icons.Default.Backpack, "Porter", b.porter_npr)
                            CostLine(Icons.Default.Hotel, "Lodging", b.lodging_npr)
                            CostLine(Icons.Default.Restaurant, "Meals", b.meals_npr)
                            CostLine(
                                Icons.Default.DirectionsBus,
                                "Transport from ${r.starting_city} (${"%.0f".format(r.start_to_dest_km)} km)",
                                b.transport_npr
                            )

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = CardBorder)
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Per person", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Text(
                                    "NPR ${"%,.0f".format(r.per_person_npr)}",
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TrailBlue
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Insights, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "ML market estimate: NPR ${"%,.0f".format(r.ml_estimate_npr)}",
                                    fontSize = 12.sp, color = TextMuted
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFDECEA))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.message, color = Color(0xFFD32F2F), fontSize = 13.sp)
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENT: SECTION CARD
// Groups related fields under a titled white card,
// giving the form visual hierarchy instead of a
// flat stack of identical boxes.
// ─────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(TrailBlueFaint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TrailBlue, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun InsightItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    alignEnd: Boolean = false
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) {
                Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(label, fontSize = 12.sp, color = TextMuted)
            if (alignEnd) {
                Spacer(Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(12.dp))
            }
        }
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) TrailBlue else Color(0xFFBBBBBB),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title, fontSize = 14.sp,
                    color = if (enabled) TextDark else Color(0xFFAAAAAA)
                )
                if (subtitle != null) {
                    Text(subtitle, fontSize = 11.sp, color = Color(0xFFAAAAAA))
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = TrailBlue)
        )
    }
}

@Composable
private fun CostLine(icon: ImageVector, label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TrailBlue, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF444444), modifier = Modifier.weight(1f))
        Text("NPR ${"%,.0f".format(value)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(
    label: String, selected: String, options: List<String>, icon: ImageVector? = null, onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = TrailBlue) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrailBlue,
                focusedLabelColor = TrailBlue
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String, value: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = TrailBlue) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TrailBlue,
            focusedLabelColor = TrailBlue
        )
    )
}