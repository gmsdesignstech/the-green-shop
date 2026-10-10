package com.example.ui.screens

import androidx.compose.runtime.Composable

/** Staff uses the same provider-backed phone OTP flow as customers; authorization remains server-side. */
@Composable
fun StaffAuthScreen(
    role: UserRole,
    loading: Boolean,
    error: String?,
    otpSent: Boolean,
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onChangePhoneNumber: () -> Unit,
    developmentPhone: String?,
    localDevelopmentAuth: Boolean,
    onClearMessage: () -> Unit,
    onBack: () -> Unit,
    onSelectRole: (UserRole) -> Unit,
    isConfigured: Boolean
) {
    LandingScreen(
        otpSent = otpSent,
        isLoading = loading,
        isConfigured = isConfigured,
        errorMessage = error,
        isDevelopmentTestOtpEnabled = localDevelopmentAuth,
        developmentTestPhoneNumber = developmentPhone,
        onSendOtp = onSendOtp,
        onVerifyOtp = onVerifyOtp,
        onChangePhoneNumber = onChangePhoneNumber,
        onClearError = onClearMessage,
        onBack = onBack,
        selectedRole = role,
        onSelectRole = onSelectRole,
        roleTitle = if (role == UserRole.ADMIN) "Administrator" else "Delivery partner"
    )
}
