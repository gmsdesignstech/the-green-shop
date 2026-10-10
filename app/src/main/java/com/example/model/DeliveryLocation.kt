package com.example.model

import com.example.location.LocationData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Clean, production-ready model representing a real-time GPS location update
 * from or to the backend service (e.g. Supabase / WebSocket / REST endpoint).
 *
 * Matching schema fields:
 * - [id]: Unique identifier for the location record (UUID or backend key)
 * - [deliveryPartnerId]: ID of the delivery partner reporting this position
 * - [orderId]: Associated active order ID
 * - [latitude]: Geographic latitude coordinate (-90.0 to 90.0)
 * - [longitude]: Geographic longitude coordinate (-180.0 to 180.0)
 * - [accuracy]: Radial horizontal accuracy in meters
 * - [heading]: Compass bearing / direction of movement in degrees (0.0 to 360.0)
 * - [speed]: Current ground speed in meters per second
 * - [recordedAt]: Epoch millisecond timestamp when the device GPS hardware recorded the fix
 * - [createdAt]: Epoch millisecond timestamp when the record was persisted in the database
 */
@JsonClass(generateAdapter = true)
data class DeliveryLocation(
    @Json(name = "id")
    val id: String = "",

    @Json(name = "delivery_partner_id")
    val deliveryPartnerId: String = "",

    @Json(name = "order_id")
    val orderId: String = "",

    @Json(name = "latitude")
    val latitude: Double,

    @Json(name = "longitude")
    val longitude: Double,

    @Json(name = "accuracy")
    val accuracy: Float = 0f,

    @Json(name = "heading")
    val heading: Float? = null,

    @Json(name = "speed")
    val speed: Float? = null,

    @Json(name = "recorded_at")
    val recordedAt: Long = System.currentTimeMillis(),

    @Json(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Verifies that the coordinates are within valid geographic bounds
     * and do not represent the (0,0) null-island coordinate.
     */
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0) &&
                accuracy >= 0f
    }

    /**
     * Checks if the location was recorded recently within [maxAgeMillis] (default 60 seconds).
     */
    fun isFresh(maxAgeMillis: Long = 60_000L): Boolean {
        return (System.currentTimeMillis() - recordedAt) <= maxAgeMillis
    }

    /**
     * Returns a human-friendly string describing how long ago this location was captured
     * (e.g. "Just now", "45s ago", "2m ago").
     */
    fun getFormattedAge(): String {
        val diffSeconds = (System.currentTimeMillis() - recordedAt) / 1000
        return when {
            diffSeconds < 5 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            else -> "${diffSeconds / 3600}h ago"
        }
    }

    /**
     * Converts to internal [LocationData] used by maps and smooth tracking animations.
     */
    fun toLocationData(): LocationData {
        return LocationData(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            bearing = heading,
            speed = speed,
            timestamp = recordedAt
        )
    }

    companion object {
        /**
         * Convenience constructor converting from an internal [LocationData] instance.
         */
        fun fromLocationData(
            locationData: LocationData,
            orderId: String = "",
            deliveryPartnerId: String = "",
            id: String = ""
        ): DeliveryLocation {
            return DeliveryLocation(
                id = id,
                deliveryPartnerId = deliveryPartnerId,
                orderId = orderId,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                accuracy = locationData.accuracy,
                heading = locationData.bearing,
                speed = locationData.speed,
                recordedAt = locationData.timestamp,
                createdAt = System.currentTimeMillis()
            )
        }
    }
}
