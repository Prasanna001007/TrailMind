package com.example.trailmind.ui.hotels

import com.example.trailmind.ui.route.TripSelection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.Hotel
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.route.RouteViewModel
import com.example.trailmind.ui.route.SatelliteSource
import com.example.trailmind.ui.route.StreetSource
import com.example.trailmind.ui.route.TerrainSource
import com.example.trailmind.ui.theme.trailFieldColors
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelsScreen(
    routeViewModel: RouteViewModel = viewModel(),
    hotelsViewModel: HotelsViewModel = viewModel()
) {
    val context = LocalContext.current
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "TrailMind/1.0 (student project, Himalaya College of Engineering)"
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        true
    }

    val destinations by routeViewModel.destinations.collectAsState()
    val state by hotelsViewModel.state.collectAsState()
    var destination by rememberSaveable { mutableStateOf(TripSelection.hotelsDestination) }
    var radius by rememberSaveable { mutableStateOf(5) }
    var mapStyle by rememberSaveable { mutableStateOf(0) }
    var showList by rememberSaveable { mutableStateOf(true) }// 0 Terrain, 1 Street, 2 Satellite
    var tracking by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> tracking = granted }
    var selected by remember { mutableStateOf<Hotel?>(null) }
    var expanded by remember { mutableStateOf(false) }

    fun search(dest: String, r: Int) {
        selected = null
        hotelsViewModel.load(dest, r)
    }

    // Opened from the chat's plan card: search straight away, once
    LaunchedEffect(Unit) {
        if (TripSelection.hotelsDestination.isNotEmpty()) {
            search(destination, radius)
            TripSelection.hotelsDestination = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = destination,
                onValueChange = {},
                readOnly = true,
                label = { Text("Hotels near...") },
                leadingIcon = { Icon(Icons.Default.Flag, null, tint = TrailBlue) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 15.sp),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = trailFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                destinations.map { it.name }.forEach { n ->
                    DropdownMenuItem(
                        text = { Text(n, color = Color.White) },
                        onClick = { destination = n; expanded = false; search(n, radius) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(2, 5, 10).forEach { km ->
                FilterChip(
                    selected = radius == km,
                    onClick = { radius = km; if (destination.isNotEmpty()) search(destination, km) },
                    label = { Text("$km km") }
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        when (val s = state) {
            is HotelsUiState.Idle -> Hint("Pick a destination to see lodging nearby.")
            is HotelsUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TrailBlue)
                Spacer(Modifier.width(10.dp))
                Text("Finding lodging (the first search can take a while)...", fontSize = 13.sp, color = Color(0xFF6B7280))
            }
            is HotelsUiState.Error -> Text(s.message, color = Color(0xFFD32F2F), fontSize = 13.sp)
            is HotelsUiState.Success -> {
                val d = s.data
                // Unnamed lodges are left out completely
                val named = remember(d) { d.hotels.filter { it.name != "Unnamed lodge" } }
                val note = when (d.state) {
                    "cache" -> "Saved results."
                    "cache_old" -> "Server busy, showing older saved results."
                    "unavailable" -> "Map data service is busy. Try again later."
                    else -> "Fresh results."
                }
                Text("${named.size} named lodges within ${d.radius_km} km. $note", fontSize = 12.sp, color = Color(0xFF6B7280))
                Spacer(Modifier.height(6.dp))
                if (named.isEmpty()) {
                    Hint("No named lodges found nearby.")
                } else {
                    // Big map
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (showList) 1.3f else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clipToBounds()
                    ) {
                        // 1. The map (first, so everything else draws on top of it)
                        HotelsMap(named, selected, mapStyle, tracking, Modifier.fillMaxSize())

                        // 2. Terrain / Street / Satellite buttons (top right)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                                .padding(4.dp)
                        ) {
                            listOf("Terrain", "Street", "Satellite").forEachIndexed { i, label ->
                                Text(
                                    label,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (mapStyle == i) TrailBlue else Color.Transparent)
                                        .clickable { mapStyle = i }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    color = if (mapStyle == i) Color.White else Color(0xFF444444)
                                )
                            }
                        }

                        // 3. START BUTTON: paste it here, right after the style Row
                        Button(
                            onClick = {
                                if (tracking) tracking = false
                                else if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) tracking = true
                                else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tracking) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                            )
                        ) { Text(if (tracking) "Stop" else "Start", color = Color.White, fontWeight = FontWeight.SemiBold) }

                        // 4. Hide list / Show list (bottom left)
                        Text(
                            if (showList) "Hide list" else "Show list",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                                .clickable { showList = !showList }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TrailBlue
                        )

                        // 5. Selected hotel name while the list is hidden (top left)
                        if (!showList && selected != null) {
                            Text(
                                selected!!.name,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF1A1A2E)
                            )
                        }
                    }   // end of the map Box
                    Spacer(Modifier.height(8.dp))
                    if (showList) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(named) { h -> HotelRow(h, h == selected) { selected = h } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 14.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(vertical = 16.dp))
}

@Composable
private fun HotelRow(h: Hotel, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEBF3FB) else Color.White)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(h.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E), modifier = Modifier.weight(1f))
                Text("${"%.1f".format(h.distance_km)} km", fontSize = 12.sp, color = TrailBlue)
            }
            Text(
                h.type.replaceFirstChar { it.uppercase() } +
                        if (h.source == "manual") "  •  Verified ${h.checked_on ?: ""}" else "  •  OpenStreetMap (unverified)",
                fontSize = 12.sp,
                color = if (h.source == "manual") Color(0xFF2E7D32) else Color(0xFF6B7280)
            )
            h.price_range?.let { Text("Price: $it", fontSize = 12.sp, color = Color(0xFF1A1A2E)) }
            h.phone?.let { Text("Phone: $it", fontSize = 12.sp, color = Color(0xFF1A1A2E)) }
            h.website?.let { Text(it, fontSize = 12.sp, color = TrailBlue) }
        }
    }
}

// Remembers what is already drawn, so panning is not undone on every redraw
private class MapMemo(var hotels: List<Hotel>? = null, var selected: Hotel? = null, var tracking: Boolean = false)

@Composable
private fun HotelsMap(hotels: List<Hotel>, selected: Hotel?, styleIndex: Int, tracking: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(StreetSource)
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(28.0, 84.0))
            tag = MapMemo()
        }
    }
    val locationOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }
    DisposableEffect(locationOverlay) { onDispose { locationOverlay.disableMyLocation() } }
    // Without pause/resume the map can stay blank
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            val source = when (styleIndex) {
                0 -> TerrainSource
                2 -> SatelliteSource
                else -> StreetSource
            }
            if (map.tileProvider.tileSource.name() != source.name()) map.setTileSource(source)
            map.maxZoomLevel = when (styleIndex) { 0 -> 16.0; 1 -> 17.0; else -> 18.0 }

            val memo = map.tag as MapMemo
            if (memo.hotels !== hotels) {
                memo.hotels = hotels
                memo.selected = null
                map.overlays.clear()
                // Two-finger twist rotates the map (re-added because clear() removes it)
                map.overlays.add(RotationGestureOverlay(map).also { it.setEnabled(true) })
                hotels.forEach { h ->
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(h.lat, h.lon)
                        title = h.name
                        snippet = h.type
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                }
                val pts = hotels.map { GeoPoint(it.lat, it.lon) }
                val fit = {
                    if (pts.size == 1) {
                        map.controller.setZoom(16.0)
                        map.controller.animateTo(pts[0])
                    } else {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(pts), false, 80)
                    }
                }
                if (map.width > 0) map.post(fit) else map.addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
            }
            if (selected != null && memo.selected !== selected) {
                memo.selected = selected
                val z = when (styleIndex) { 0 -> 15.0; 1 -> 16.0; else -> 17.0 }
                map.controller.setZoom(z)
                map.controller.animateTo(GeoPoint(selected.lat, selected.lon))
            }
            if (tracking != memo.tracking) {
                memo.tracking = tracking
                if (tracking) {
                    map.overlays.add(locationOverlay)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                } else {
                    locationOverlay.disableFollowLocation()
                    locationOverlay.disableMyLocation()
                    map.overlays.remove(locationOverlay)
                }
            } else if (tracking && !map.overlays.contains(locationOverlay)) {
                map.overlays.add(locationOverlay)
            }
            map.invalidate()
        }
    )
}