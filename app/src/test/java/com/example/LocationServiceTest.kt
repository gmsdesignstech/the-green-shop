package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.location.LocationData
import com.example.location.LocationErrorType
import com.example.location.LocationQueueManager
import com.example.location.LocationResultState
import com.example.location.LocationService
import com.example.location.LocationTrackingConfig
import com.example.location.LocationTrackingMode
import com.google.android.gms.location.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationServiceTest {

    @Test
    fun testLocationDataValidation() {
        // Valid location
        val valid = LocationData(
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 10f
        )
        assertTrue(valid.isValid())
        assertTrue(valid.isFresh(60_000L))

        // Invalid (Null Island 0,0)
        val nullIsland = LocationData(latitude = 0.0, longitude = 0.0)
        assertFalse(nullIsland.isValid())

        // Out of bounds latitude
        val badLat = LocationData(latitude = 95.0, longitude = 77.0)
        assertFalse(badLat.isValid())

        // Out of bounds longitude
        val badLon = LocationData(latitude = 12.0, longitude = 200.0)
        assertFalse(badLon.isValid())
    }

    @Test
    fun testLocationQueueManagerCapacityAndRetention() {
        val queueManager = LocationQueueManager(maxCapacity = 5, maxRetentionAgeMillis = 50_000L)
        assertEquals(0, queueManager.size)
        assertTrue(queueManager.isEmpty)

        // Enqueue valid items
        for (i in 1..10) {
            val loc = LocationData(
                latitude = 12.9 + (i * 0.001),
                longitude = 77.5 + (i * 0.001),
                accuracy = 5f,
                timestamp = System.currentTimeMillis() + i
            )
            queueManager.enqueue(loc)
        }

        // Must cap at maxCapacity (5)
        assertEquals(5, queueManager.size)
        assertFalse(queueManager.isEmpty)

        // Drain batch
        val drained = queueManager.drainBatch(batchSize = 3)
        assertEquals(3, drained.size)
        assertEquals(2, queueManager.size)

        // Clear
        queueManager.clear()
        assertEquals(0, queueManager.size)
    }

    @Test
    fun testLocationQueueManagerDeduplication() {
        val queueManager = LocationQueueManager(maxCapacity = 10)
        val now = System.currentTimeMillis()

        val loc1 = LocationData(
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 4f,
            timestamp = now
        )
        val enqueuedFirst = queueManager.enqueue(loc1)
        assertTrue(enqueuedFirst)
        assertEquals(1, queueManager.size)

        // Second point identical coordinates within 1 second -> should be skipped as duplicate
        val loc2 = LocationData(
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 4f,
            timestamp = now + 500
        )
        val enqueuedSecond = queueManager.enqueue(loc2)
        assertFalse(enqueuedSecond)
        assertEquals(1, queueManager.size)
    }

    @Test
    fun testLocationTrackingConfigPresets() {
        val activeDelivery = LocationTrackingConfig.ACTIVE_DELIVERY
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, activeDelivery.priority)
        assertEquals(4_000L, activeDelivery.intervalMillis)
        assertEquals(2_000L, activeDelivery.minUpdateIntervalMillis)
        assertTrue(activeDelivery.minUpdateDistanceMeters <= 5f)

        val customer = LocationTrackingConfig.CUSTOMER_TRACKING
        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, customer.priority)

        val modeConfig = LocationTrackingConfig.fromMode(LocationTrackingMode.BATTERY_SAVER)
        assertEquals(Priority.PRIORITY_LOW_POWER, modeConfig.priority)
    }

    @Test
    fun testBearingAndDistanceCalculations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = LocationService.create(context)
        assertNotNull(service)

        // Distance between Bangalore Center (12.9716, 77.5946) and Indiranagar (12.9784, 77.6408)
        val distance = service.calculateDistance(12.9716, 77.5946, 12.9784, 77.6408)
        assertTrue("Distance should be around 5000m", distance in 4000f..6000f)

        // Bearing calculation towards East/Northeast
        val bearing = service.calculateBearing(12.9716, 77.5946, 12.9784, 77.6408)
        assertTrue("Bearing should be valid degrees in 0..360", bearing in 0f..360f)
    }

    @Test
    fun testLocationResultStateAndErrors() {
        val sampleLoc = LocationData(12.9716, 77.5946, 5f)
        val successState = LocationResultState.Success(sampleLoc)
        assertEquals(12.9716, successState.location.latitude, 0.0001)

        val errorState = LocationResultState.Error(
            errorType = LocationErrorType.PLAY_SERVICES_UNAVAILABLE,
            message = "Play Services disconnected"
        )
        assertEquals(LocationErrorType.PLAY_SERVICES_UNAVAILABLE, errorState.errorType)

        assertTrue(LocationResultState.PermissionDenied is LocationResultState)
        assertTrue(LocationResultState.LocationUnavailable is LocationResultState)
    }
}
