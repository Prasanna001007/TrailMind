package com.example.trailmind.ui.chat

import com.example.trailmind.data.remote.dto.TurnResponse
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.example.trailmind.data.remote.dto.PlanInputs
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.ChatMsg
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueDark
import com.example.trailmind.ui.home.TrailBlueMid
import com.example.trailmind.ui.theme.trailFieldColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip

@Composable
fun ChatScreen(
    onViewRoute: (String, String) -> Unit,
    onSeeHotels: (String) -> Unit,
    onEditPlan: (PlanInputs) -> Unit,
    viewModel: ChatViewModel = viewModel()
)  {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to the newest message
    LaunchedEffect(messages.size, loading) {
        val last = messages.size + (if (loading) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .imePadding()
    ) {
        // Title bar (Back works with the phone's back gesture)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(TrailBlueMid, TrailBlue, TrailBlueDark)))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text("TrailMind Assistant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Trip ideas for Nepal", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { m ->
                val card = m.plan
                when {
                    card == null -> Bubble(m)
                    card.type == "plan" -> PlanCard(card, onViewRoute, onSeeHotels, onEditPlan)
                    else -> ActionCard(card, onViewRoute, onSeeHotels)
                }
            }
            // Tappable examples, only while the chat is new
            if (messages.size == 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Plan 5 days in Poon Hill for 2 people",
                            "Show me hotels near Langtang Valley",
                            "Is March cold in Pokhara?"
                        ).forEach { example ->
                            Text(
                                example,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEBF3FB))
                                    .clickable { viewModel.send(example) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = 13.sp,
                                color = TrailBlue
                            )
                        }
                    }
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your trip...", color = Color(0xFF777777)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = trailFieldColors()
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { viewModel.send(input); input = "" },
                enabled = input.isNotBlank() && !loading,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = TrailBlue)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    val isError = msg.role == "error"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            msg.text,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    when {
                        isUser -> TrailBlue
                        isError -> Color(0xFFFDECEA)
                        else -> Color.White
                    },
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = when {
                isUser -> Color.White
                isError -> Color(0xFFD32F2F)
                else -> Color(0xFF1A1A2E)
            }
        )
    }
}

@Composable
private fun PlanCard(
    p: TurnResponse,
    onViewRoute: (String, String) -> Unit,
    onSeeHotels: (String) -> Unit,
    onEditPlan: (PlanInputs) -> Unit
) {
    val i = p.inputs
    val r = p.budget
    if (i == null || r == null) return
    val b = r.breakdown
    val dest = i.destination ?: ""
    val city = i.start_city ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Trip plan: $dest", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
            Text(
                "${i.days} days  •  ${i.group_size} people  •  ${i.month}  •  from $city",
                fontSize = 12.sp, color = Color(0xFF6B7280)
            )
            Spacer(Modifier.height(10.dp))

            Text("Estimated total", fontSize = 12.sp, color = Color(0xFF6B7280))
            Text(
                "NPR ${"%,.0f".format(r.itemized_total_npr)}",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TrailBlue
            )
            Text(
                "NPR ${"%,.0f".format(r.per_person_npr)} per person",
                fontSize = 12.sp, color = Color(0xFF6B7280)
            )
            Spacer(Modifier.height(8.dp))

            PlanCostRow("Permit", b.permit_npr)
            if (b.guide_npr > 0) PlanCostRow("Guide", b.guide_npr)
            if (b.porter_npr > 0) PlanCostRow("Porter", b.porter_npr)
            PlanCostRow("Lodging", b.lodging_npr)
            PlanCostRow("Meals", b.meals_npr)
            PlanCostRow("Transport (${"%.0f".format(r.start_to_dest_km)} km)", b.transport_npr)

            p.assumed?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Assumed: ${it.joinToString(", ")}. For other choices, use Estimate my budget.",
                    fontSize = 11.sp, color = Color(0xFF6B7280)
                )
            }
            if (!p.altitude_warning.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Highest point ${"%,d".format(p.altitude_m ?: 0)} m. ${p.altitude_warning}",
                    fontSize = 12.sp, color = Color(0xFFD32F2F)
                )
            }
            if (!p.itinerary.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("Day by day", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(4.dp))
                Text(p.itinerary, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(4.dp))
                Text(
                    "The day-by-day text is a general guide. Costs come from TrailMind's budget model.",
                    fontSize = 11.sp, color = Color(0xFF6B7280)
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onViewRoute(city, dest) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                ) { Text("View route", color = Color.White, fontSize = 13.sp) }
                OutlinedButton(
                    onClick = { onSeeHotels(dest) },
                    modifier = Modifier.weight(1f)
                ) { Text("See all hotels", color = TrailBlue, fontSize = 13.sp) }
            }
            TextButton(
                onClick = { onEditPlan(i) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Change details (guided questions)", color = TrailBlue, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun PlanCostRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF444444))
        Text("NPR ${"%,.0f".format(value)}", fontSize = 13.sp, color = Color(0xFF1A1A2E))
    }
}
// Small card for "route" and "hotels" requests
@Composable
private fun ActionCard(
    p: TurnResponse,
    onViewRoute: (String, String) -> Unit,
    onSeeHotels: (String) -> Unit
) {
    val i = p.inputs ?: return
    val dest = i.destination ?: return
    val city = i.start_city ?: ""
    val isRoute = p.type == "route"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (isRoute) "Route: $city to $dest" else "Hotels near $dest",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TrailBlue
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { if (isRoute) onViewRoute(city, dest) else onSeeHotels(dest) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
            ) { Text(if (isRoute) "View route" else "See hotels", color = Color.White) }
        }
    }
}