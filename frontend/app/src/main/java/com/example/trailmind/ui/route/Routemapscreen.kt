package com.example.trailmind.ui.route


import androidx.compose.ui.draw.clipToBounds
import android.graphics.DashPathEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.RoutePoint
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueDark
import com.example.trailmind.ui.home.TrailBlueMid
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.activity.compose.BackHandler
import com.example.trailmind.ui.theme.trailFieldColors
import java.io.File


// ─────────────────────────────────────────────
// ROUTE SCREEN (opened from the "Treks" tile)
// 1. The user picks a start and a destination.
// 2. The app asks the ml-service (POST /route) for the A* route.
// 3. The route is drawn on an OpenStreetMap map with OSMDroid.
//
// Note: the line connects the stops with straight segments. The ml-service
// returns the ordered stops, not road geometry.
// ─────────────────────────────────────────────

// Draws a round badge with a letter or number, used as the map marker

// A small orange label drawn on the map (used for legs with no mapped path)
private fun labelIcon(context: Context, text: String): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 12f * d
        isFakeBoldText = true
    }
    val w = (textPaint.measureText(text) + 20f * d).toInt()
    val h = (26f * d).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val bg = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E67E22")
    }
    canvas.drawRoundRect(android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), 13f * d, 13f * d, bg)
    canvas.drawText(text, 10f * d, h / 2f - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}

// A small white name tag drawn BELOW its point
private fun nameTag(context: Context, text: String): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1A1A2E")
        textSize = 11f * d
        isFakeBoldText = true
    }
    val top = 12f * d                       // empty space where the dot sits
    val w = (textPaint.measureText(text) + 14f * d).toInt()
    val h = (top + 22f * d).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val bg = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(230, 255, 255, 255)
    }
    canvas.drawRoundRect(android.graphics.RectF(0f, top, w.toFloat(), h.toFloat()), 8f * d, 8f * d, bg)
    canvas.drawText(text, 7f * d, top + 15f * d, textPaint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}

// Known places (destinations, cities) that lie within 2 km of the drawn road or trail
fun nearbyLandmarks(legs: List<Leg>, places: List<RoutePoint>, ends: List<RoutePoint>): List<RoutePoint> {
    val line = legs.filter { it.type != LegType.FLIGHT }.flatMap { it.points }
    if (line.isEmpty()) return emptyList()
    val skip = ends.map { it.name }.toSet()
    return (places + CityPoints.all)
        .filter { it.name !in skip }
        .map { p ->
            val g = GeoPoint(p.lat, p.lon)
            p to line.minOf { it.distanceToAsDouble(g) }
        }
        .filter { it.second <= 2000 }
        .sortedBy { it.second }
        .take(8)
        .map { it.first }
}
private fun pinIcon(context: Context, label: String, colorHex: String, sizeDp: Int): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(colorHex)
    }
    val ring = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = px * 0.45f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    val r = px / 2f - 2f * density
    canvas.drawCircle(px / 2f, px / 2f, r, fill)
    canvas.drawCircle(px / 2f, px / 2f, r, ring)
    canvas.drawText(label, px / 2f, px / 2f - (text.descent() + text.ascent()) / 2, text)
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}

// Cuts out any piece where the line returns to a point it already passed (out-and-back spurs)
fun eraseLoops(points: List<GeoPoint>): List<GeoPoint> {
    fun key(p: GeoPoint) = Math.round(p.latitude * 100000) * 100000000L + Math.round(p.longitude * 100000)
    val out = ArrayList<GeoPoint>()
    val seen = HashMap<Long, Int>()
    for (p in points) {
        val k = key(p)
        val idx = seen[k]
        if (idx != null) {
            while (out.size > idx + 1) seen.remove(key(out.removeAt(out.lastIndex)))
        } else {
            seen[k] = out.size
            out.add(p)
        }
    }
    return out
}

// Joins the real path pieces into one line per stretch, and keeps long orange gaps separate
private fun cleanRoute(lines: List<List<GeoPoint>>): List<List<GeoPoint>> {
    val result = ArrayList<List<GeoPoint>>()
    val run = ArrayList<GeoPoint>()
    fun flush() {
        if (run.size >= 2) {
            val cleaned = eraseLoops(run)
            if (cleaned.size >= 2) result.add(cleaned)
        }
        run.clear()
    }
    for (line in lines) {
        val longGap = line.size == 2 && line[0].distanceToAsDouble(line[1]) > 5000
        if (longGap) { flush(); result.add(line) } else run.addAll(line)
    }
    flush()
    return result
}

@Composable
fun RouteMapScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    // Configure OSMDroid once, before the first MapView is created
    val osmReady = remember { initOsmdroid(context); true }

    var start by rememberSaveable { mutableStateOf(TripSelection.start) }
    var end by rememberSaveable { mutableStateOf(TripSelection.end) }
    var autoRun by rememberSaveable { mutableStateOf(TripSelection.start.isNotEmpty() && TripSelection.end.isNotEmpty()) }
    var mapStyle by rememberSaveable { mutableStateOf(0) }   // 0 Terrain, 1 Street, 2 Satellite
    var showDetails by rememberSaveable { mutableStateOf(true) }
    var tracking by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> tracking = granted }


    val destinations by viewModel.destinations.collectAsState()
    val destinationsError by viewModel.destinationsError.collectAsState()
    val state by viewModel.state.collectAsState()

    val names = destinations.map { it.name }
    val startNames = CityPoints.all.map { it.name } + names

    // One place that decides: city start (local) or server route
    val runRoute: () -> Unit = {
        val city = CityPoints.byName(start)
        if (city != null) {
            destinations.firstOrNull { it.name == end }?.let { viewModel.planFrom(city, it) }
        } else {
            viewModel.findRoute(start, end)
        }
    }
    val isLoading = state is RouteUiState.Loading
    val success = state as? RouteUiState.Success
    val canFind = start.isNotEmpty() && end.isNotEmpty() && start != end && !isLoading
    // Only the two places the user picked: no detour stops
    // Runs once the destination list has loaded, when the wizard filled both pickers
    // Start with no leftover route, unless the wizard filled both pickers
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!opened) {
            opened = true
            if (!autoRun) viewModel.reset()
        }
    }

    // Runs once the destination list has loaded, when the wizard filled both pickers
    LaunchedEffect(names.isNotEmpty()) {
        if (names.isNotEmpty()) {
            if (autoRun && start in startNames && end in names && start != end) {
                runRoute()
            }
            autoRun = false
            TripSelection.clear()   // so the Treks tile later opens empty
        }
    }
    val stops = success?.data?.path ?: emptyList()
    val endpoints = remember(stops) {
        if (stops.size >= 2) listOf(stops.first(), stops.last()) else emptyList()
    }
    var legs by remember(stops) { mutableStateOf<List<Leg>>(emptyList()) }
    var roadStatus by remember(stops) { mutableStateOf("") }
    LaunchedEffect(stops) {
        if (endpoints.size == 2) {
            roadStatus = "Finding route..."
            val result = withContext(Dispatchers.IO) { LegPlanner.plan(endpoints[0], endpoints[1]) }
            legs = result.legs
            roadStatus = result.note
        }
    }
    val landmarks = remember(legs, destinations) { nearbyLandmarks(legs, destinations, endpoints) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
    ) {

        // ── Pickers and button ──
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                destinationsError != null -> {
                    Text(
                        "Could not load destinations: $destinationsError",
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.loadDestinations() }) { Text("Retry") }
                }
                names.isEmpty() -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TrailBlue)
                        Spacer(Modifier.width(10.dp))
                        Text("Loading destinations...", fontSize = 13.sp, color = Color(0xFF6B7280))
                    }
                }
                else -> {
                    PointDropdown("Start", start, startNames, Icons.Default.MyLocation) { start = it; viewModel.reset() }
                    Spacer(Modifier.height(10.dp))
                    PointDropdown("Destination", end, names, Icons.Default.Flag) { end = it; viewModel.reset() }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { runRoute() },
                        enabled = canFind,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrailBlue,
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Get Directions", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                    if (start.isNotEmpty() && start == end) {
                        Spacer(Modifier.height(6.dp))
                        Text("Pick two different places.", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
            }

            (state as? RouteUiState.Error)?.let {
                Spacer(Modifier.height(8.dp))
                Text(it.message, color = Color(0xFFD32F2F), fontSize = 13.sp)
            }
        }

        // ── Map with the result card on top ──
        // The map only appears after "Get Directions" is tapped
        if (success != null || isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                OsmMap(
                    points = endpoints,
                    legs = legs,
                    landmarks = landmarks,
                    styleIndex = mapStyle,
                    tracking = tracking,
                    modifier = Modifier.fillMaxSize()
                )
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
                if (success != null) {
                    Button(
                        onClick = {
                            if (tracking) {
                                tracking = false
                            } else {
                                val has = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (has) tracking = true
                                else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 52.dp, end = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tracking) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (tracking) "Stop" else "Start", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (roadStatus == "Finding route...") {
                    Text(
                        roadStatus,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .widthIn(max = 140.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                success?.let { s ->
                    val data = s.data
                    val highest = data.path.maxOfOrNull { it.altitude_m } ?: 0
                    if (showDetails) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .navigationBarsPadding()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${"%.1f".format(legKm(legs))} km total",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TrailBlue
                                    )
                                    IconButton(onClick = { showDetails = false }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide details", tint = TrailBlue)
                                    }
                                }
                                Text(
                                    "Road ${"%.0f".format(legKm(legs, LegType.ROAD))} km  •  Trail ${"%.0f".format(legKm(legs, LegType.TRAIL))} km  •  Flight ${"%.0f".format(legKm(legs, LegType.FLIGHT))} km",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${data.path.first().name}  →  ${data.path.last().name}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1A1A2E)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Highest point: ${"%,d".format(highest)} m",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Blue: road.  Green: trail.  Orange dashed: flight.  Purple: trail stops.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                                if (roadStatus.isNotEmpty() && roadStatus != "Finding route...") {
                                    Spacer(Modifier.height(4.dp))
                                    Text(roadStatus, fontSize = 11.sp, color = Color(0xFFE67E22))
                                }
                                if (legs.any { it.type == LegType.FLIGHT }) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Why a flight? No road reaches this trailhead, so trekkers fly in and then walk.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Route is a guide only. Treks need a licensed guide and permits.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                                if (highest >= 3000) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Above 3,000 m altitude sickness is possible. Climb gradually.",
                                            fontSize = 12.sp,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Small pill that brings the details back
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .navigationBarsPadding()
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .clickable { showDetails = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${"%.1f".format(legKm(legs))} km",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrailBlue
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Show details", tint = TrailBlue)
                        }
                    }
                }
            }
        } else {
            // Shown before the user has asked for directions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = TrailBlue.copy(alpha = 0.35f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Pick a start and a destination, then tap Get Directions to see the route on the map.",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Esri "World Topo Map" tiles (roads, terrain and place names, no API key).
// Esri orders the URL as zoom/y/x, so getTileURLString is overridden.
 fun esriSource(name: String, service: String, maxZoom: Int) = object : OnlineTileSourceBase(
    name, 0, maxZoom, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/$service/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex)
}

 val TerrainSource = esriSource("EsriTopo", "World_Topo_Map", 16)
 val StreetSource = esriSource("EsriStreet", "World_Street_Map", 17)
 val SatelliteSource = esriSource("EsriImagery", "World_Imagery", 17)

// ─────────────────────────────────────────────
// OSMDROID SETUP
// OSM asks every app to identify itself, so the user agent is the package name.
// The tile cache goes into the app's cache folder, which needs no storage permission.
// ─────────────────────────────────────────────
private fun initOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        userAgentValue = "TrailMind/1.0 (student project, Himalaya College of Engineering; your-email@example.com)"
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
}

// ─────────────────────────────────────────────
// THE MAP
// Wraps OSMDroid's MapView (an old-style Android view) so Compose can show it.
// Whenever `points` changes, the old route is removed and the new one is drawn.
// ─────────────────────────────────────────────
@Composable
private fun OsmMap(
    points: List<RoutePoint>,
    legs: List<Leg>,
    landmarks: List<RoutePoint>,
    styleIndex: Int,
    tracking : Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TerrainSource) // the standard OpenStreetMap tiles
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0// pinch to zoom
            controller.setZoom(7.0)
            controller.setCenter(GeoPoint(28.3949, 84.1240))   // centre of Nepal
        }
    }

    val locationOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }
    DisposableEffect(locationOverlay) { onDispose { locationOverlay.disableMyLocation() } }

    // OSMDroid needs to know when the screen pauses and resumes
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            // Switch the map pictures when the user taps Terrain / Street / Satellite
            val source = when (styleIndex) {
                1 -> StreetSource
                2 -> SatelliteSource
                else -> TerrainSource
            }
            if (map.tileProvider.tileSource.name() != source.name()) {
                map.setTileSource(source)
            }

            // Redraw when the stops or the fetched paths change
            val old = map.tag as? Triple<*, *, *>
            if (old?.first !== points || old?.second !== legs || old?.third !== landmarks) {
                map.tag = Triple(points, legs, landmarks)
                drawRoute(map, points, legs, landmarks)
            }
            if (tracking) {
                if (!map.overlays.contains(locationOverlay)) {
                    map.overlays.add(locationOverlay)
                    map.controller.setZoom(17.0)   // close-up when the trip starts
                }
                locationOverlay.enableMyLocation()
                locationOverlay.enableFollowLocation()
            } else {
                locationOverlay.disableFollowLocation()
                locationOverlay.disableMyLocation()
                map.overlays.remove(locationOverlay)
            }
        }
    )
}

private fun drawRoute(map: MapView, points: List<RoutePoint>, legs: List<Leg>, landmarks: List<RoutePoint>) {
    map.overlays.clear()
    // Two-finger twist rotates the map (re-added because clear() removes it)
    map.overlays.add(RotationGestureOverlay(map).apply { isEnabled = true })

    if (points.size >= 2) {
        // 1. Lines: blue road, green trail, orange dashed flight
        legs.forEach { leg ->
            map.overlays.add(Polyline().apply {
                setPoints(leg.points)
                outlinePaint.strokeWidth = 10f
                when (leg.type) {
                    LegType.ROAD -> outlinePaint.color = android.graphics.Color.parseColor("#1F5C8B")
                    LegType.TRAIL -> outlinePaint.color = android.graphics.Color.parseColor("#2E7D32")
                    LegType.FLIGHT -> {
                        outlinePaint.color = android.graphics.Color.parseColor("#E67E22")
                        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(25f, 20f), 0f)
                    }
                }
            })
            // 2. Flights get airport labels at both ends
            if (leg.type == LegType.FLIGHT) {
                listOf(leg.points.first() to leg.from, leg.points.last() to leg.to).forEach { (p, name) ->
                    map.overlays.add(Marker(map).apply {
                        position = p
                        icon = labelIcon(map.context, name ?: "Airport")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setInfoWindow(null)
                    })
                }
            }
        }

        // 3. Purple dots on the trail stops, with their names
        val trailNames = TrekWaypoints.names(points.last().name)
        TrekWaypoints.before(points.last().name).forEachIndexed { i, g ->
            map.overlays.add(Marker(map).apply {
                position = g
                icon = pinIcon(map.context, "${i + 1}", "#7B1FA2", 22)
                title = trailNames.getOrNull(i) ?: "Trail stop ${i + 1}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            })
            trailNames.getOrNull(i)?.let { name ->
                map.overlays.add(Marker(map).apply {
                    position = g
                    icon = nameTag(map.context, name)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                    setInfoWindow(null)
                })
            }
        }

        // 3b. Known places near the road or trail: a small gray dot and a name
        landmarks.forEach { p ->
            val g = GeoPoint(p.lat, p.lon)
            map.overlays.add(Marker(map).apply {
                position = g
                icon = pinIcon(map.context, "", "#6B7280", 14)
                title = p.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            })
            map.overlays.add(Marker(map).apply {
                position = g
                icon = nameTag(map.context, p.name)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                setInfoWindow(null)
            })
        }
        // 4. A (start) and B (destination)
        points.forEachIndexed { index, p ->
            val isStart = index == 0
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(p.lat, p.lon)
                icon = pinIcon(map.context, if (isStart) "A" else "B", if (isStart) "#2E7D32" else "#D32F2F", 40)
                title = (if (isStart) "Start: " else "Destination: ") + p.name
                snippet = "${p.type}, ${p.altitude_m} m"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            })
        }

        // 5. Zoom so everything fits
        val all = legs.flatMap { it.points }.ifEmpty { points.map { GeoPoint(it.lat, it.lon) } }
        map.post { map.zoomToBoundingBox(BoundingBox.fromGeoPoints(all), true, 120) }
    }

    map.invalidate()
}

// ─────────────────────────────────────────────
// DROPDOWN used for the start and destination pickers
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointDropdown(
    label: String,
    selected: String,
    options: List<String>,
    icon: ImageVector,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = TrailBlue) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = trailFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}