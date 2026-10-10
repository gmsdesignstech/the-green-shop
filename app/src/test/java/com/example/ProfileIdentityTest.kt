package com.example

import com.example.ui.screens.formatPhoneForDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileIdentityTest {
    @Test fun formatsVerifiedIndianPhoneWithoutChangingIdentity() {
        assertEquals("+91 98765 43210", formatPhoneForDisplay("+919876543210"))
    }

    @Test fun preservesUnknownServerValueRatherThanSubstitutingAnotherNumber() {
        assertEquals("unavailable", formatPhoneForDisplay("unavailable"))
    }
}
