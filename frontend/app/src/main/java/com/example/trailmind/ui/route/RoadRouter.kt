package com.example.trailmind.ui.route

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit
import android.util.Log

object RoadRouter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Returns the real path, or null if the service fails or the path is
    // unreasonably long compared with the straight-line distance.
    fun leg(from: GeoPoint, to: GeoPoint, profile: String): List<GeoPoint>? {
        return try {
            val url = "https://routing.openstreetmap.de/routed-$profile/route/v1/driving/" +
                    "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
                    "?overview=full&geometries=geojson"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TrailMind/1.0 (student project, Himalaya College of Engineering)")
                .build()
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) {
                    Log.d("RoadRouter", "$profile HTTP ${res.code}")
                    return null
                }
                val route = JSONObject(res.body?.string() ?: return null)
                    .getJSONArray("routes").getJSONObject(0)

                // Reject detours (e.g. a path through India for a trek inside Nepal)
                val straight = from.distanceToAsDouble(to)
                val limit = if (profile == "car") straight * 2.5 + 5000 else straight * 2.2 + 3000
                if (route.getDouble("distance") > limit) {
                    Log.d("RoadRouter", "$profile rejected: route ${route.getDouble("distance")} m vs straight $straight m")
                    return null
                }

                val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                List(coords.length()) { i ->
                    val c = coords.getJSONArray(i)
                    GeoPoint(c.getDouble(1), c.getDouble(0))
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}