package com.example

import com.example.data.admin.parseAdminDashboard
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminRepositoryTest {
    @Test
    fun partialDashboardResponseMarksFailedMetricsUnavailable() {
        val dashboard = parseAdminDashboard(
            JSONObject(
                """
                {
                  "metrics": {
                    "today_orders": 4,
                    "pending_orders": null,
                    "completed_orders": 2,
                    "gross_sales": null,
                    "payment_failures": 0,
                    "active_deliveries": 1,
                    "low_stock_products": null,
                    "pending_partner_applications": 3
                  },
                  "recentOrders": [],
                  "lowStock": [],
                  "unavailable": ["metrics", "lowStock"]
                }
                """.trimIndent()
            )
        )

        assertEquals("—", dashboard.metrics.first { it.label == "Today’s orders" }.value)
        assertEquals("—", dashboard.metrics.first { it.label == "Gross sales" }.value)
        assertEquals("—", dashboard.metrics.first { it.label == "Low stock" }.value)
        assertEquals("3", dashboard.metrics.first { it.label == "Partner applications" }.value)
        assertEquals(setOf("metrics", "lowStock"), dashboard.unavailableSections)
    }

    @Test
    fun healthyDashboardResponseKeepsMetricValuesAndRows() {
        val dashboard = parseAdminDashboard(
            JSONObject(
                """
                {
                  "metrics": {
                    "today_orders": 4,
                    "pending_orders": 1,
                    "completed_orders": 2,
                    "gross_sales": "150.00",
                    "payment_failures": 0,
                    "active_deliveries": 1,
                    "low_stock_products": 2,
                    "pending_partner_applications": 3
                  },
                  "recentOrders": [{"id": 12, "total": "75.00", "payment_status": "captured", "status": "placed"}],
                  "lowStock": [{"id": 5, "name": "Spinach", "available": 1, "low_stock_threshold": 5}],
                  "unavailable": []
                }
                """.trimIndent()
            )
        )

        assertEquals("4", dashboard.metrics.first { it.label == "Today’s orders" }.value)
        assertEquals("₹150.00", dashboard.metrics.first { it.label == "Gross sales" }.value)
        assertEquals("Order #12", dashboard.recentOrders.single().title)
        assertEquals("Spinach", dashboard.lowStock.single().title)
        assertTrue(dashboard.unavailableSections.isEmpty())
    }
}
