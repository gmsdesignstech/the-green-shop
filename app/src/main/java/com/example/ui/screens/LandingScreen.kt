package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.SampleData
import com.example.ui.components.GreenShopLogoBadge
import com.example.ui.components.ProductPhoto
import com.example.ui.components.productPhotoResource
import kotlinx.coroutines.delay

@Composable
fun LandingScreen(
    otpSent: Boolean,
    isLoading: Boolean,
    isConfigured: Boolean,
    errorMessage: String?,
    isDevelopmentTestOtpEnabled: Boolean,
    developmentTestPhoneNumber: String?,
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onChangePhoneNumber: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    onBrowseAsGuest: () -> Unit = {},
    selectedRole: UserRole = UserRole.CUSTOMER,
    onSelectRole: (UserRole) -> Unit = {},
    roleTitle: String = "Customer",
    modifier: Modifier = Modifier
) {
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var resendSeconds by rememberSaveable { mutableIntStateOf(60) }
    val focusManager = LocalFocusManager.current
    val otpFocusRequester = remember { FocusRequester() }
    val validPhone = isValidIndianMobile(phoneNumber)

    LaunchedEffect(otpSent, resendSeconds, isDevelopmentTestOtpEnabled) {
        if (otpSent && !isDevelopmentTestOtpEnabled && resendSeconds > 0) { delay(1_000); resendSeconds-- }
    }
    LaunchedEffect(otpSent) {
        if (otpSent) { delay(200); otpFocusRequester.requestFocus() }
    }

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()
            .verticalScroll(rememberScrollState()).testTag("landing_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            ProductShowcase()
            IconButton(
                onClick = onBack,
                modifier = Modifier.statusBarsPadding().padding(8.dp).testTag("customer_auth_back")
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Choose another role") }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 5.dp,
                modifier = Modifier.offset(y = (-12).dp)
            ) {
                GreenShopLogoBadge(Modifier.padding(7.dp), size = 54.dp)
            }
            Spacer(Modifier.height(2.dp))
            Text(if (otpSent) "Verify your number" else "Welcome to The Green Shop", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                if (otpSent && isDevelopmentTestOtpEnabled) "Enter the development code for ${maskIndianPhone(phoneNumber)}"
                else if (otpSent) "Enter the code sent to ${maskIndianPhone(phoneNumber)}"
                else "Freshness at your doorstep • $roleTitle access",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            if (!otpSent) {
                Row(
                    Modifier.fillMaxWidth().testTag("login_role_selector"),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        UserRole.CUSTOMER to "Customer",
                        UserRole.ADMIN to "Admin",
                        UserRole.DELIVERY_PARTNER to "Delivery"
                    ).forEach { (role, label) ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { onSelectRole(role) },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f).testTag("role_${role.name.lowercase()}")
                        )
                    }
                }
                Text(
                    when (selectedRole) {
                        UserRole.CUSTOMER -> "Shop fresh produce, order and track deliveries"
                        UserRole.ADMIN -> "Authorized store operations and management"
                        UserRole.DELIVERY_PARTNER -> "Approved assignments, navigation and delivery updates"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            if (!otpSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter(Char::isDigit).take(10); onClearError() },
                    modifier = Modifier.fillMaxWidth().testTag("phone_number_field"),
                    label = { Text("Mobile number") },
                    prefix = { Text("🇮🇳  +91  ", fontWeight = FontWeight.Bold) },
                    supportingText = { Text(
                        if (phoneNumber.isNotEmpty() && !validPhone) "Enter a valid 10-digit Indian mobile number"
                        else if (isDevelopmentTestOtpEnabled) "Development mode uses a fixed code; no SMS is sent"
                        else "A secure OTP will be sent to this number"
                    ) },
                    isError = phoneNumber.isNotEmpty() && !validPhone,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (validPhone && !isLoading) onSendOtp(phoneNumber) }),
                    shape = RoundedCornerShape(16.dp)
                )
            } else {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6); onClearError() },
                    modifier = Modifier.fillMaxWidth().focusRequester(otpFocusRequester).testTag("otp_field"),
                    label = { Text("6-digit OTP") }, placeholder = { Text("• • • • • •") },
                    singleLine = true, isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (otp.length == 6 && !isLoading) { focusManager.clearFocus(); onVerifyOtp(otp) } }),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Change number", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(enabled = !isLoading) { otp = ""; onChangePhoneNumber() })
                    if (isDevelopmentTestOtpEnabled) {
                        Text("No SMS sent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(
                            if (resendSeconds > 0) "Resend in ${resendSeconds}s" else "Resend OTP",
                            color = if (resendSeconds > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(enabled = resendSeconds == 0 && !isLoading) { otp = ""; resendSeconds = 60; onSendOtp(phoneNumber) }
                        )
                    }
                }
            }

            val visibleError = errorMessage ?: if (!isConfigured) "Phone authentication is not configured for this build." else null
            visibleError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
            if (isDevelopmentTestOtpEnabled) Text(
                developmentTestPhoneNumber?.let { "Development test mode • $it • OTP 123456" }
                    ?: "Development test mode • OTP 123456",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (otpSent) { focusManager.clearFocus(); onVerifyOtp(otp) } else onSendOtp(phoneNumber) },
                enabled = !isLoading && isConfigured && if (otpSent) otp.length == 6 else validPhone,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag(if (otpSent) "verify_otp_button" else "continue_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text(if (otpSent) "Verify & continue" else "Continue", fontWeight = FontWeight.Bold)
            }
            if (!otpSent && selectedRole == UserRole.CUSTOMER) {
                TextButton(
                    onClick = onBrowseAsGuest,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().testTag("browse_as_guest_button")
                ) {
                    Text("Browse fresh products without signing in", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("By continuing, you agree to our", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Terms of Service",
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("terms_of_service_link")
                )
                Text("  and  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("privacy_policy_link")
                )
            }
        }
    }
}

@Composable
private fun ProductShowcase() {
    val products = remember { SampleData.products.filter { productPhotoResource(it.id) != null }.take(10) }
    Box(
        Modifier.fillMaxWidth().height(326.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f))
            .testTag("product_showcase")
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            val columns = if (maxWidth < 350.dp) 3 else 4
            val rows = products.chunked(columns)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { index, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = if (index % 2 == 1) 12.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { product ->
                            Surface(
                                modifier = Modifier.weight(1f, fill = false).widthIn(max = 82.dp).aspectRatio(1f)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f), RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                ProductPhoto(product, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(112.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.colorScheme.background)))
        )
    }
}

internal fun isValidIndianMobile(value: String): Boolean = value.matches(Regex("^[6-9]\\d{9}$"))

internal fun maskIndianPhone(value: String): String = if (value.length == 10) "+91 ••••••${value.takeLast(4)}" else "+91 $value"
