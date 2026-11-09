package com.example.trailmind.ui.trip

import com.example.trailmind.ui.route.TripSelection
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.ui.budget.BudgetViewModel
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueDark
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.ui.home.TrailBlueMid
import com.example.trailmind.util.Resource

// ─────────────────────────────────────────────
// ANSWER OPTIONS
// These mirror the lists in BudgetScreen.kt, so the strings sent to the
// ML service are exactly the ones the model already understands.
// If BudgetScreen's lists change, update these too.
// ─────────────────────────────────────────────
private val cities = listOf(
    "Kathmandu", "Chitwan", "Pokhara", "Lumbini", "Itahari", "Surkhet", "Nepalgunj"
)

private val destinations = listOf(
    "Everest Base Camp", "Annapurna Circuit", "Langtang Valley", "Mardi Himal",
    "Manaslu Circuit", "Gokyo Lakes", "Upper Mustang", "Muktinath",
    "Poon Hill (Ghorepani)", "Nagarkot", "Shivapuri National Park", "Bandipur Trails",
    "Dhampus / Australian Camp", "Champadevi / Chandragiri Trail", "Pashupatinath",
    "Boudhanath", "Swayambhunath", "Bhaktapur Durbar Square", "Lumbini",
    "Janakpur (Janaki Mandir)", "Sirubari", "Ghalegaun", "Bhujung Village",
    "Amaltari Community Homestay", "Chitwan National Park", "Bardia National Park",
    "Sagarmatha National Park"
)

// Only these destinations allow hiring a porter (same rule as BudgetScreen's destMeta)
private val porterDestinations = setOf(
    "Everest Base Camp", "Annapurna Circuit", "Langtang Valley", "Mardi Himal",
    "Manaslu Circuit", "Gokyo Lakes", "Upper Mustang", "Poon Hill (Ghorepani)",
    "Dhampus / Australian Camp", "Champadevi / Chandragiri Trail", "Sagarmatha National Park"
)

private val months = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val nationalities = listOf("Domestic", "SAARC", "Foreign")
private val tiers = listOf("Budget", "Standard", "Luxury")
private val transports = listOf("Public Bus", "Private Jeep", "Flight")

// ─────────────────────────────────────────────
// THE QUESTIONS, in the order they are asked.
// One question per screen. Together they fill every field of BudgetRequest.
// ─────────────────────────────────────────────
private enum class Question(
    val title: String,
    val hint: String,
    val icon: ImageVector
) {
    CITY("Where are you starting from?", "Your starting city in Nepal", Icons.Default.MyLocation),
    DESTINATION("Where do you want to go?", "Pick your destination", Icons.Default.Flag),
    DAYS("How many days is your trip?", "Use + and - to change", Icons.Default.Schedule),
    GROUP("How many people are travelling?", "1 means you are travelling solo", Icons.Default.Groups),
    MONTH("Which month are you travelling?", "The season affects prices", Icons.Default.CalendarMonth),
    NATIONALITY("What is your nationality?", "Permit fees depend on it", Icons.Default.Public),
    TIER("What accommodation do you prefer?", "This changes the lodging cost", Icons.Default.Hotel),
    TRANSPORT("How will you travel there?", "Main transport from your starting city", Icons.Default.DirectionsBus),
    EXTRAS("Do you want extra help?", "A guide and a porter change the total", Icons.Default.Hiking)
}

// ─────────────────────────────────────────────
// MAIN SCREEN
// Shows the questions one by one. After the last one it sends the answers
// to BudgetViewModel.predict() and shows the result on the same screen.
// ─────────────────────────────────────────────
@Composable
fun TripWizardScreen(
    onBack: () -> Unit,
    onGetDirections: () -> Unit,
    viewModel: BudgetViewModel = viewModel()
) {
    val questions = Question.values()

    // The user's answers. rememberSaveable keeps them when the phone rotates.
    val pre = TripSelection.prefill
    var stepIndex by rememberSaveable { mutableStateOf(if (pre != null) Question.TIER.ordinal else 0) }
    var startingCity by rememberSaveable { mutableStateOf(pre?.start_city ?: "") }
    var destination by rememberSaveable { mutableStateOf(pre?.destination ?: "") }
    var days by rememberSaveable { mutableStateOf(pre?.days ?: 7) }
    var groupSize by rememberSaveable { mutableStateOf(pre?.group_size ?: 2) }
    var month by rememberSaveable { mutableStateOf(pre?.month ?: "") }
    var nationality by rememberSaveable { mutableStateOf(pre?.nationality ?: "") }
    var tier by rememberSaveable { mutableStateOf(pre?.tier ?: "") }
    var transport by rememberSaveable { mutableStateOf(pre?.transport ?: "") }
    var useGuide by rememberSaveable { mutableStateOf(true) }
    var usePorter by rememberSaveable { mutableStateOf(true) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { TripSelection.prefill = null }   // use it once
    val hasPorter = destination in porterDestinations
    val question = questions[stepIndex]
    val isLast = stepIndex == questions.size - 1

    // Back goes: result -> last question -> earlier questions -> out of the wizard
    val goBack: () -> Unit = {
        when {
            submitted -> submitted = false
            stepIndex > 0 -> stepIndex--
            else -> onBack()
        }
    }
    BackHandler(onBack = goBack)

    // Builds the request from the answers and asks the ML service for the estimate
    val submit: () -> Unit = {
        submitted = true
        viewModel.predict(
            BudgetRequest(
                starting_city = startingCity,
                destination = destination,
                duration_days = days,
                group_size = groupSize,
                travel_month = month,
                nationality_category = nationality,
                // No separate question: 1 person = Solo, more = Group
                traveler_type = if (groupSize == 1) "Solo" else "Group",
                accommodation_tier = tier,
                transport_mode = transport,
                use_guide = useGuide,
                use_porter = usePorter && hasPorter
            )
        )
    }

    // Next is disabled until the current question has an answer
    val canContinue = when (question) {
        Question.CITY -> startingCity.isNotEmpty()
        Question.DESTINATION -> destination.isNotEmpty()
        Question.MONTH -> month.isNotEmpty()
        Question.NATIONALITY -> nationality.isNotEmpty()
        Question.TIER -> tier.isNotEmpty()
        Question.TRANSPORT -> transport.isNotEmpty()
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        WizardHeader(
            title = if (submitted) "Your estimate" else "Guided Questions",
            subtitle = if (submitted) "$startingCity to $destination"
            else "Question ${stepIndex + 1} of ${questions.size}",
            progress = if (submitted) null else (stepIndex + 1f) / questions.size,
            onBack = goBack
        )

        if (submitted) {
            ResultContent(
                state = uiState,
                summary = "$startingCity to $destination  |  $days days  |  $groupSize " +
                        if (groupSize == 1) "person" else "people",
                onEdit = { submitted = false },
                onRetry = submit,
                onDone = onBack,
                onGetDirections = {
                    TripSelection.set(startingCity, destination)
                    onGetDirections()
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            // ── The current question ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(TrailBlueFaint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(question.icon, contentDescription = null, tint = TrailBlue, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    question.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    lineHeight = 30.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(question.hint, fontSize = 13.sp, color = Color(0xFF888888))
                Spacer(Modifier.height(24.dp))

                when (question) {
                    Question.CITY -> OptionList(cities, startingCity) { startingCity = it }
                    Question.DESTINATION -> OptionList(destinations, destination) { destination = it }
                    Question.DAYS -> Stepper(
                        value = days, unit = if (days == 1) "day" else "days",
                        min = 1, max = 30
                    ) { days = it }
                    Question.GROUP -> Stepper(
                        value = groupSize, unit = if (groupSize == 1) "person (solo)" else "people",
                        min = 1, max = 50
                    ) { groupSize = it }
                    Question.MONTH -> OptionList(months, month, columns = 2) { month = it }
                    Question.NATIONALITY -> OptionList(nationalities, nationality) { nationality = it }
                    Question.TIER -> OptionList(tiers, tier) { tier = it }
                    Question.TRANSPORT -> OptionList(transports, transport) { transport = it }
                    Question.EXTRAS -> {
                        ToggleCard(
                            icon = Icons.Default.Hiking,
                            title = "Hire a guide",
                            subtitle = null,
                            checked = useGuide,
                            enabled = true,
                            onCheckedChange = { useGuide = it }
                        )
                        Spacer(Modifier.height(10.dp))
                        ToggleCard(
                            icon = Icons.Default.Backpack,
                            title = "Hire a porter",
                            subtitle = if (!hasPorter) "Not available for $destination" else null,
                            checked = usePorter && hasPorter,
                            enabled = hasPorter,
                            onCheckedChange = { usePorter = it }
                        )
                    }
                }
            }

            // ── Next / Estimate button, always visible at the bottom ──
            Button(
                onClick = { if (isLast) submit() else stepIndex++ },
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrailBlue,
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                Text(
                    if (isLast) "Estimate Budget" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (!isLast) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// HEADER: gradient bar with back button and (optional) progress bar
// ─────────────────────────────────────────────
@Composable
private fun WizardHeader(
    title: String,
    subtitle: String,
    progress: Float?,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(TrailBlueMid, TrailBlue, TrailBlueDark)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// ANSWER WIDGETS
// ─────────────────────────────────────────────

// A list of tappable answer cards. columns = 2 puts two cards per row.
@Composable
private fun OptionList(
    options: List<String>,
    selected: String,
    columns: Int = 1,
    onSelect: (String) -> Unit
) {
    options.chunked(columns).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEach { option ->
                OptionCard(
                    text = option,
                    isSelected = option == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(option) }
                )
            }
            // Keeps the cards in a short last row the same width as the others
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun OptionCard(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) TrailBlue else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Big number with - and + buttons, used for days and group size
@Composable
private fun Stepper(
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = { if (value > min) onChange(value - 1) },
            enabled = value > min,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = TrailBlue)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
        }
        Column(
            modifier = Modifier.padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$value", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
            Text(unit, fontSize = 14.sp, color = Color(0xFF888888))
        }
        FilledIconButton(
            onClick = { if (value < max) onChange(value + 1) },
            enabled = value < max,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = TrailBlue)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
        }
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    icon, contentDescription = null,
                    tint = if (enabled) TrailBlue else Color(0xFFBBBBBB),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontSize = 15.sp, color = if (enabled) Color(0xFF1A1A2E) else Color(0xFFAAAAAA))
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
}

// ─────────────────────────────────────────────
// RESULT: shown after the last question.
// state is null or Loading while waiting, then Success or Error.
// ─────────────────────────────────────────────
@Composable
private fun ResultContent(
    state: Resource<BudgetResponse>?,
    summary: String,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onGetDirections: () -> Unit,
    modifier: Modifier = Modifier
)
 {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(summary, fontSize = 13.sp, color = Color(0xFF6B7280))
        Spacer(Modifier.height(14.dp))

        when (state) {
            is Resource.Success -> {
                val r = state.data
                val b = r.breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TrailBlueFaint)
                                .padding(16.dp)
                        ) {
                            Text("Estimated total", fontSize = 12.sp, color = Color(0xFF6B7280))
                            Text(
                                "NPR ${"%,.0f".format(r.itemized_total_npr)}",
                                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TrailBlue
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        CostLine(Icons.Default.Badge, "Permit", b.permit_npr)
                        if (b.guide_npr > 0) CostLine(Icons.Default.Hiking, "Guide", b.guide_npr)
                        if (b.porter_npr > 0) CostLine(Icons.Default.Backpack, "Porter", b.porter_npr)
                        CostLine(Icons.Default.Hotel, "Lodging", b.lodging_npr)
                        CostLine(Icons.Default.Restaurant, "Meals", b.meals_npr)
                        CostLine(
                            Icons.Default.DirectionsBus,
                            "Transport (${"%.0f".format(r.start_to_dest_km)} km)",
                            b.transport_npr
                        )

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE5E9F0))
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Per person", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                            Text(
                                "NPR ${"%,.0f".format(r.per_person_npr)}",
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TrailBlue
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "ML market estimate: NPR ${"%,.0f".format(r.ml_estimate_npr)}",
                            fontSize = 12.sp, color = Color(0xFF6B7280)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGetDirections,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Get Directions", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Change my answers", color = TrailBlue) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                ) { Text("Done", color = Color.White) }
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
                    Text(state.message, color = Color(0xFFD32F2F), fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                ) { Text("Try again", color = Color.White) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Change my answers", color = TrailBlue) }
            }

            // null or Loading: still waiting for the ML service
            else -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TrailBlue)
                }
            }
        }
    }
}

@Composable
private fun CostLine(icon: ImageVector, label: String, value: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TrailBlue, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF444444), modifier = Modifier.weight(1f))
        Text("NPR ${"%,.0f".format(value)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A2E))
    }
}