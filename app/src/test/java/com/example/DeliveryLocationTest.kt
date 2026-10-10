package com.example

import com.example.location.LocationData
import com.example.model.DeliveryLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryLocationTest {

    @Test
    fun testValidCoordinates() {
        val location = DeliveryLocation(
            id = "loc_123",
            deliveryPartnerId = "rider_456",
            orderId = "order_789",
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 4.5f,
            heading = 90.0f,
            speed = 7.2f
        )

        assertTrue(location.isValid())
        assertTrue(location.isFresh())
        assertEquals("loc_123", location.id)
        assertEquals("rider_456", location.deliveryPartnerId)
        assertEquals("order_789", location.orderId)
    }

    @Test
    fun testInvalidCoordinates() {
        // Null Island (0,0)
        val nullIsland = DeliveryLocation(
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 10f
        )
        assertFalse(nullIsland.isValid())

        // Out of latitude range
        val outOfBoundsLat = DeliveryLocation(
            latitude = 95.0,
            longitude = 12.0,
            accuracy = 10f
        )
        assertFalse(outOfBoundsLat.isValid())

        // Out of longitude range
        val outOfBoundsLng = DeliveryLocation(
            latitude = 12.0,
            longitude = 185.0,
            accuracy = 10f
        )
        assertFalse(outOfBoundsLng.isValid())

        // Negative accuracy
        val negativeAccuracy = DeliveryLocation(
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = -1f
        )
        assertFalse(negativeAccuracy.isValid())
    }

    @Test
    fun testAgeFormatting() {
        val now = System.currentTimeMillis()
        val justNow = DeliveryLocation(latitude = 12.0, longitude = 77.0, recordedAt = now - 2000)
        assertEquals("Just now", justNow.getFormattedAge())

        val thirtySecondsAgo = DeliveryLocation(latitude = 12.0, longitude = 77.0, recordedAt = now - 30_000)
        assertEquals("30s ago", thirtySecondsAgo.getFormattedAge())

        val fiveMinutesAgo = DeliveryLocation(latitude = 12.0, longitude = 77.0, recordedAt = now - 300_000)
        assertEquals("5m ago", fiveMinutesAgo.getFormattedAge())
    }

    @Test
    fun testConversionToAndFromLocationData() {
        val original = LocationData(
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 5.0f,
            bearing = 120.0f,
            speed = 8.5f,
            timestamp = 1690000000000L
        )

        val deliveryLoc = DeliveryLocation.fromLocationData(
            locationData = original,
            orderId = "ORD_01",
            deliveryPartnerId = "PARTNER_01",
            id = "REC_01"
        )

        assertEquals("REC_01", deliveryLoc.id)
        assertEquals("ORD_01", deliveryLoc.orderId)
        assertEquals("PARTNER_01", deliveryLoc.deliveryPartnerId)
        assertEquals(12.9716, deliveryLoc.latitude, 0.0001)
        assertEquals(77.5946, deliveryLoc.longitude, 0.0001)
        assertEquals(5.0f, deliveryLoc.accuracy, 0.01f)
        assertEquals(120.0f, deliveryLoc.heading ?: 0f, 0.01f)
        assertEquals(8.5f, deliveryLoc.speed ?: 0f, 0.01f)
        assertEquals(1690000000000L, deliveryLoc.recordedAt)

        val convertedBack = deliveryLoc.toLocationData()
        assertEquals(original.latitude, convertedBack.latitude, 0.0001)
        assertEquals(original.longitude, convertedBack.longitude, 0.0001)
        assertEquals(original.accuracy, convertedBack.accuracy, 0.01f)
        assertEquals(original.bearing, convertedBack.bearing)
        assertEquals(original.speed, convertedBack.speed)
        assertEquals(original.timestamp, convertedBack.timestamp)
    }
}
