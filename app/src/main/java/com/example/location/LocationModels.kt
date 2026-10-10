package com.example.location

import com.google.android.gms.location.Priority

/**
 * Clean, immutable model representing a validated geographic location.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val bearing: Float? = null,
    val speed: Float? = null,
    val altitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isMock: Boolean = false,
    val provider: String? = null
) {
    /**
     * Checks if coordinates fall within valid geographic bounds
     * and are not the (0,0) null-island coordinate.
     */
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0) &&
                accuracy >= 0f
    }

    /**
     * Checks if location was captured within the last [maxAgeMillis] milliseconds.
     */
    fun isFresh(maxAgeMillis: Long = 60_000L): Boolean {
        return (System.currentTimeMillis() - timestamp) <= maxAgeMillis
    }

    /**
     * Helper to compute formatted time representation.
     */
    fun getFormattedAge(): String {
        val seconds = (System.currentTimeMillis() - timestamp) / 1000
        return when {
            seconds < 5 -> "Just now"
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            else -> "${seconds / 3600}h ago"
        }
    }
}

/**
 * Operational mode defining power and accuracy trade-offs.
 */
enum class LocationTrackingMode {
    /**
     * High accuracy with frequent updates (3-5s) and strict distance filtering.
     * Ideal for delivery partners actively navigating to the customer.
     */
    ACTIVE_DELIVERY,

    /**
     * Balanced power accuracy with 8-12s intervals.
     * Ideal for customer tracking views and general map positioning.
     */
    CUSTOMER_TRACKING,

    /**
     * Low power mode with 25-30s intervals.
     * Conserves battery when device is low on battery or waiting for pickup.
     */
    BATTERY_SAVER,

    /**
     * Passive updates listening only when other apps request location.
     */
    PASSIVE
}

/**
 * Configuration parameters for FusedLocationProviderClient requests.
 */
data class LocationTrackingConfig(
    val intervalMillis: Long,
    val minUpdateIntervalMillis: Long,
    val minUpdateDistanceMeters: Float,
    val priority: Int,
    val maxWaitTimeMillis: Long = 0L,
    val maxAccuracyThresholdMeters: Float = 100f
) {
    companion object {
        /**
         * Real-time navigation for delivery partners on the road.
         * Updates every 4 seconds or 3 meters.
         */
        val ACTIVE_DELIVERY = LocationTrackingConfig(
            intervalMillis = 4_000L,
            minUpdateIntervalMillis = 2_000L,
            minUpdateDistanceMeters = 3.0f,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            maxAccuracyThresholdMeters = 80f
        )

        /**
         * Live tracking screen for customers viewing delivery progress.
         * Updates every 8 seconds or 5 meters, balanced power.
         */
        val CUSTOMER_TRACKING = LocationTrackingConfig(
            intervalMillis = 8_000L,
            minUpdateIntervalMillis = 4_000L,
            minUpdateDistanceMeters = 5.0f,
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            maxAccuracyThresholdMeters = 100f
        )

        /**
         * Low battery or order preparation mode.
         */
        val BATTERY_SAVER = LocationTrackingConfig(
            intervalMillis = 25_000L,
            minUpdateIntervalMillis = 15_000L,
            minUpdateDistanceMeters = 20.0f,
            priority = Priority.PRIORITY_LOW_POWER,
            maxAccuracyThresholdMeters = 150f
        )

        /**
         * Passive tracking mode.
         */
        val PASSIVE = LocationTrackingConfig(
            intervalMillis = 60_000L,
            minUpdateIntervalMillis = 30_000L,
            minUpdateDistanceMeters = 50.0f,
            priority = Priority.PRIORITY_PASSIVE,
            maxAccuracyThresholdMeters = 200f
        )

        /**
         * Creates a config from [LocationTrackingMode].
         */
        fun fromMode(mode: LocationTrackingMode): LocationTrackingConfig {
            return when (mode) {
                LocationTrackingMode.ACTIVE_DELIVERY -> ACTIVE_DELIVERY
                LocationTrackingMode.CUSTOMER_TRACKING -> CUSTOMER_TRACKING
                LocationTrackingMode.BATTERY_SAVER -> BATTERY_SAVER
                LocationTrackingMode.PASSIVE -> PASSIVE
            }
        }
    }
}

/**
 * Result state emitted by real-time location update flows.
 */
sealed class LocationResultState {
    /**
     * Successfully received a validated location.
     */
    data class Success(
        val location: LocationData,
        val isRecent: Boolean = true
    ) : LocationResultState()

    /**
     * An error occurred while retrieving or listening for locations.
     */
    data class Error(
        val errorType: LocationErrorType,
        val message: String,
        val cause: Throwable? = null
    ) : LocationResultState()

    /**
     * Permission has not been granted by user.
     */
    data object PermissionDenied : LocationResultState()

    /**
     * Location services (GPS / Network location) are disabled on the device.
     */
    data class GpsDisabled(
        val resolvable: Boolean = false
    ) : LocationResultState()

    /**
     * Location provider has temporarily lost location fix.
     */
    data object LocationUnavailable : LocationResultState()
}

/**
 * Error categorizations for handling failures gracefully.
 */
enum class LocationErrorType {
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    TIMEOUT,
    ACCURACY_TOO_LOW,
    PLAY_SERVICES_UNAVAILABLE,
    NETWORK_DISCONNECTED,
    UNKNOWN
}

/**
 * Status of device location settings resolution.
 */
sealed class LocationSettingsStatus {
    data object Satisfied : LocationSettingsStatus()
    data class ResolutionRequired(val exception: Exception) : LocationSettingsStatus()
    data class Unavailable(val exception: Exception) : LocationSettingsStatus()
}
