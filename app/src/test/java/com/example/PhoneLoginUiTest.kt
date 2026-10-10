package com.example

import com.example.ui.screens.isValidIndianMobile
import com.example.ui.screens.maskIndianPhone
import org.junit.Assert.*
import org.junit.Test

class PhoneLoginUiTest {
    @Test fun acceptsValidIndianMobileNumbers() {
        assertTrue(isValidIndianMobile("9876543210"))
        assertTrue(isValidIndianMobile("6123456789"))
    }

    @Test fun rejectsIncompleteAndInvalidIndianMobileNumbers() {
        assertFalse(isValidIndianMobile("12345"))
        assertFalse(isValidIndianMobile("1234567890"))
        assertFalse(isValidIndianMobile("98765abc10"))
    }

    @Test fun otpPromptMasksTheNumberActuallyEntered() {
        assertEquals("+91 ••••••3210", maskIndianPhone("9876543210"))
        assertEquals("+91 ••••••0001", maskIndianPhone("9000000001"))
    }
}
