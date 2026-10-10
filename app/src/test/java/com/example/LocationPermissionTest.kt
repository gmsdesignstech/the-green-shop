package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.LocationPermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationPermissionTest {

    @Test
    fun verifyLocationStringResourcesExist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val title = context.getString(R.string.location_rationale_title)
        val subtitle = context.getString(R.string.location_rationale_subtitle)
        val allowBtn = context.getString(R.string.location_btn_allow)
        val settingsBtn = context.getString(R.string.location_btn_open_settings)
        val gpsBtn = context.getString(R.string.location_btn_enable_gps)

        assertNotNull(title)
        assertTrue(title.isNotEmpty())
        assertTrue(subtitle.contains("location"))
        assertEquals("Allow Location Access", allowBtn)
        assertEquals("Open App Settings", settingsBtn)
        assertEquals("Turn On GPS", gpsBtn)
    }

    @Test
    fun verifyLocationPermissionStatusEnum() {
        val statuses = LocationPermissionStatus.values()
        assertTrue(statuses.contains(LocationPermissionStatus.NOT_REQUESTED))
        assertTrue(statuses.contains(LocationPermissionStatus.GRANTED_PRECISE))
        assertTrue(statuses.contains(LocationPermissionStatus.GRANTED_APPROXIMATE))
        assertTrue(statuses.contains(LocationPermissionStatus.RATIONALE_NEEDED))
        assertTrue(statuses.contains(LocationPermissionStatus.PERMANENTLY_DENIED))
        assertTrue(statuses.contains(LocationPermissionStatus.GPS_DISABLED))
    }
}
