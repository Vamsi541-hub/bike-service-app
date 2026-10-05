package com.example.data.location

import android.location.Location
import com.example.data.model.FuelType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class NearbyPlace(
    val id: String,
    val title: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val phone: String? = null,
    val isOpen: Boolean = true
)

class NearbyPlacesService {
    private val client = OkHttpClient()

    suspend fun findNearby(location: UserLocation, fuelType: FuelType, radiusMeters: Int = 5000): List<NearbyPlace> {
        val fuelClause = if (fuelType == FuelType.EV) {
            """nwr["amenity"="charging_station"](around:${radiusMeters},${location.latitude},${location.longitude});"""
        } else {
            """nwr["amenity"="fuel"](around:${radiusMeters},${location.latitude},${location.longitude});"""
        }

        val query = """
            [out:json][timeout:15];
            (
              nwr["shop"="motorcycle"](around:${radiusMeters},${location.latitude},${location.longitude});
              nwr["shop"="car_repair"](around:${radiusMeters},${location.latitude},${location.longitude});
              nwr["craft"="car_repair"](around:${radiusMeters},${location.latitude},${location.longitude});
              $fuelClause
              nwr["amenity"="charging_station"](around:${radiusMeters},${location.latitude},${location.longitude});
            );
            out center tags;
        """.trimIndent()

        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://overpass-api.de/api/interpreter?data=$encoded")
            .header("User-Agent", "BikeCare/1.0 Android")
            .get()
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                parse(body, location, fuelType)
            }
        }.getOrDefault(emptyList())
    }

    private fun parse(body: String, origin: UserLocation, fuelType: FuelType): List<NearbyPlace> {
        val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val result = mutableListOf<NearbyPlace>()

        for (i in 0 until elements.length()) {
            val item = elements.optJSONObject(i) ?: continue
            val tags = item.optJSONObject("tags") ?: JSONObject()
            val lat = if (item.has("lat")) item.optDouble("lat") else item.optJSONObject("center")?.optDouble("lat") ?: Double.NaN
            val lon = if (item.has("lon")) item.optDouble("lon") else item.optJSONObject("center")?.optDouble("lon") ?: Double.NaN
            if (!lat.isFinite() || !lon.isFinite()) continue

            val distance = FloatArray(1)
            Location.distanceBetween(origin.latitude, origin.longitude, lat, lon, distance)
            val amenity = tags.optString("amenity")
            val shop = tags.optString("shop")
            val craft = tags.optString("craft")

            val category = when {
                amenity == "fuel" && fuelType != FuelType.EV -> "PETROL"
                amenity == "charging_station" && fuelType == FuelType.EV -> "EV"
                shop == "motorcycle" || shop == "car_repair" || craft == "car_repair" -> "MECHANIC"
                else -> continue
            }

            val title = tags.optString("name").ifBlank {
                when (category) {
                    "PETROL" -> "Nearby Fuel Station"
                    "EV" -> "Nearby EV Charging Station"
                    else -> "Nearby Bike Repair"
                }
            }

            result += NearbyPlace(
                id = "osm_${item.optLong("id", i.toLong())}",
                title = title,
                category = category,
                latitude = lat,
                longitude = lon,
                distanceKm = distance[0] / 1000.0,
                phone = tags.optString("phone").ifBlank { null }
            )
        }

        return result.distinctBy { it.id }.sortedBy { it.distanceKm }.take(30)
    }
}
