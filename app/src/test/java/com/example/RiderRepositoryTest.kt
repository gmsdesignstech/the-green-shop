package com.example

import com.example.data.rider.parseRiderDashboard
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RiderRepositoryTest {
    @Test
    fun partialDashboardKeepsProfileAndMarksUnavailableMetrics() {
        val dashboard = parseRiderDashboard(
            JSONObject(
                """
                {
                  "profile": {
                    "display_name": "Delivery Partner",
                    "service_area": "South",
                    "vehicle_type": "Bicycle",
                    "is_online": false,
                    "availability_version": 1
                  },
                  "summary": {
                    "today_completed": null,
                    "today_earnings": null,
                    "pending_earnings": null,
                    "pending_payout": null
                  },
                  "activeAssignment": null,
                  "recentDeliveries": [],
                  "notifications": [],
                  "unavailable": ["summary", "notifications"]
                }
                """.trimIndent()
            )
        )

        assertEquals("Delivery Partner", dashboard.profile.name)
        assertEquals("—", dashboard.summary.todayCompleted)
        assertEquals("—", dashboard.summary.todayEarnings)
        assertNull(dashboard.active)
        assertEquals(setOf("summary", "notifications"), dashboard.unavailableSections)
    }
}
