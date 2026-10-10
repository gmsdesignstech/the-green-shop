package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service contract for interacting with Google Play Services Location APIs.
 * Provides high-accuracy, battery-efficient location tracking, status queries,
 * and offline queue buffering for Green Shop's real-time delivery tracking system.
 */
interface LocationService {
    /**
     * Checks if the app has coarse or fine location permission.
     */
    fun hasLocationPermission(): Boolean

    /**
     * Checks if the app has ACCESS_FINE_LOCATION permission.
     */
    fun hasPreciseLocationPermission(): Boolean

    /**
     * Checks if device GPS/location provider is enabled.
     */
    fun isLocationEnabled(): Boolean

    /**
     * Evaluates whether device location settings satisfy the given tracking requirement.
     */
    suspend fun checkLocationSettings(config: LocationTrackingConfig = LocationTrackingConfig.ACTIVE_DELIVERY): LocationSettingsStatus

    /**
     * Returns the cached last known location if available and fresh.
     */
    suspend fun getLastKnownLocation(): Result<LocationData?>

    /**
     * Requests a fresh, immediate one-shot GPS location fix.
     */
    suspend fun getCurrentLocation(
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
        timeoutMs: Long = 10_000L
    ): Result<LocationData>

    /**
     * Streams real-time location updates as a Kotlin Flow using FusedLocationProviderClient.
     */
    fun getLocationUpdates(config: LocationTrackingConfig = LocationTrackingConfig.ACTIVE_DELIVERY): Flow<LocationResultState>

    /**
     * Calculates distance in meters between two coordinates.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float

    /**
     * Calculates bearing in degrees (0..360) from start to destination coordinates.
     */
    fun calculateBearing(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float

    /**
     * Queue manager storing offline GPS points when network is disconnected.
     */
    val queueManager: LocationQueueManager

    companion object {
        /**
         * Creates a production-ready [LocationService] instance.
         */
        fun create(context: Context): LocationService {
            val appContext = context.applicationContext
            return DefaultLocationService(
                context = appContext,
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext),
                settingsClient = LocationServices.getSettingsClient(appContext)
            )
        }
    }
}

/**
 * Default production implementation of [LocationService] powered by
 * Google Play Services FusedLocationProviderClient and SettingsClient.
 */
class DefaultLocationService(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val settingsClient: SettingsClient,
    override val queueManager: LocationQueueManager = LocationQueueManager()
) : LocationService {

    override fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    override fun hasPreciseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override suspend fun checkLocationSettings(config: LocationTrackingConfig): LocationSettingsStatus {
        val locationRequest = buildLocationRequest(config)
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        return try {
            settingsClient.checkLocationSettings(settingsRequest).awaitTask()
            LocationSettingsStatus.Satisfied
        } catch (resolvable: ResolvableApiException) {
            LocationSettingsStatus.ResolutionRequired(resolvable)
        } catch (e: Exception) {
            LocationSettingsStatus.Unavailable(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Result<LocationData?> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return try {
            val location: Location? = fusedLocationClient.lastLocation.awaitTask()
            val locationData = location?.toLocationData()
            if (locationData != null && locationData.isValid()) {
                queueManager.enqueue(locationData)
                Result.success(locationData)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(priority: Int, timeoutMs: Long): Result<LocationData> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        if (!isLocationEnabled()) {
            return Result.failure(IllegalStateException("Device location services are disabled"))
        }

        val cancellationTokenSource = CancellationTokenSource()

        val locationResult = withTimeoutOrNull(timeoutMs) {
            try {
                val currentRequest = CurrentLocationRequest.Builder()
                    .setPriority(priority)
                    .setMaxUpdateAgeMillis(10_000L)
                    .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                    .build()

                val loc: Location? = fusedLocationClient.getCurrentLocation(
                    currentRequest,
                    cancellationTokenSource.token
                ).awaitTask()

                loc?.toLocationData()
            } catch (e: Exception) {
                null
            }
        }

        return if (locationResult != null && locationResult.isValid()) {
            queueManager.enqueue(locationResult)
            Result.success(locationResult)
        } else {
            // Fallback to last known location if one-shot timed out or failed
            val lastKnown = getLastKnownLocation().getOrNull()
            if (lastKnown != null && lastKnown.isValid() && lastKnown.isFresh(120_000L)) {
                Result.success(lastKnown)
            } else {
                Result.failure(NoSuchElementException("Unable to acquire reliable location fix within timeout"))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(config: LocationTrackingConfig): Flow<LocationResultState> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(LocationResultState.PermissionDenied)
            close()
            return@callbackFlow
        }

        if (!isLocationEnabled()) {
            trySend(LocationResultState.GpsDisabled(resolvable = true))
        }

        val locationRequest = buildLocationRequest(config)

        val callback = object : LocationCallback() {
            private var lastEmittedLocation: LocationData? = null

            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val data = location.toLocationData()
                    if (!data.isValid()) continue

                    // Discard readings with unacceptable accuracy for the tracking mode
                    if (data.accuracy > config.maxAccuracyThresholdMeters) {
                        continue
                    }

                    // Deduplicate identical readings
                    val prev = lastEmittedLocation
                    if (prev != null) {
                        val distance = calculateDistance(prev.latitude, prev.longitude, data.latitude, data.longitude)
                        val timeDelta = data.timestamp - prev.timestamp
                        if (distance < 1.0f && timeDelta < 1_500L) {
                            continue
                        }
                    }

                    lastEmittedLocation = data
                    queueManager.enqueue(data)
                    trySend(LocationResultState.Success(data, isRecent = true))
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    if (!isLocationEnabled()) {
                        trySend(LocationResultState.GpsDisabled(resolvable = true))
                    } else {
                        trySend(LocationResultState.LocationUnavailable)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { exception ->
                trySend(
                    LocationResultState.Error(
                        errorType = LocationErrorType.PLAY_SERVICES_UNAVAILABLE,
                        message = exception.localizedMessage ?: "Failed to attach location updates",
                        cause = exception
                    )
                )
            }
        } catch (se: SecurityException) {
            trySend(LocationResultState.PermissionDenied)
            close(se)
            return@callbackFlow
        } catch (e: Exception) {
            trySend(
                LocationResultState.Error(
                    errorType = LocationErrorType.UNKNOWN,
                    message = e.localizedMessage ?: "Failed to initiate location tracking",
                    cause = e
                )
            )
            close(e)
            return@callbackFlow
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    override fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    override fun calculateBearing(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
        val startLoc = Location("").apply {
            latitude = startLat
            longitude = startLon
        }
        val endLoc = Location("").apply {
            latitude = endLat
            longitude = endLon
        }
        val rawBearing = startLoc.bearingTo(endLoc)
        // Normalize bearing to [0, 360)
        return (rawBearing + 360f) % 360f
    }

    private fun buildLocationRequest(config: LocationTrackingConfig): LocationRequest {
        val builder = LocationRequest.Builder(config.priority, config.intervalMillis)
            .setMinUpdateIntervalMillis(config.minUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(config.minUpdateDistanceMeters)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setWaitForAccurateLocation(false)

        if (config.maxWaitTimeMillis > 0L) {
            builder.setMaxUpdateDelayMillis(config.maxWaitTimeMillis)
        }

        return builder.build()
    }
}

/**
 * Extension function to safely convert an Android framework [Location]
 * into a typed [LocationData] representation.
 */
fun Location.toLocationData(): LocationData {
    val isMockLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.isMock
    } else {
        @Suppress("DEPRECATION")
        this.isFromMockProvider
    }

    return LocationData(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        bearing = if (hasBearing()) bearing else null,
        speed = if (hasSpeed()) speed else null,
        altitude = if (hasAltitude()) altitude else null,
        timestamp = time,
        isMock = isMockLocation,
        provider = provider
    )
}

/**
 * Awaits completion of a Google Play Services [Task] in a coroutine.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
