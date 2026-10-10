package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.applications.ApplicationRepository
import kotlinx.coroutines.launch

@Composable
fun RoleApplicationScreen(
    role: UserRole,
    accessToken: String?,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onApproved: () -> Unit = {},
    repository: ApplicationRepository = remember { ApplicationRepository() }
) {
    val partner = role == UserRole.DELIVERY_PARTNER
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var serviceArea by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("scooter") }
    var licence by remember { mutableStateOf("") }
    var registration by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var partnerStep by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun valid(): Boolean = fullName.trim().length >= 2 && phone.filter(Char::isDigit).length >= 10 &&
        if (partner) serviceArea.trim().length >= 2 else email.contains('@') && businessName.trim().length >= 2 && reason.trim().length >= 10

    fun submit() {
        val token = accessToken ?: run { error = "Your session expired. Sign in again."; return }
        if (!valid()) { error = "Complete all required fields with valid information."; return }
        scope.launch {
            loading = true; error = null
            val result = if (partner) repository.submitPartner(token, mapOf(
                "fullName" to fullName.trim(), "phone" to phone.trim(), "email" to email.trim(),
                "serviceArea" to serviceArea.trim(), "vehicleType" to vehicleType,
                "drivingLicenceNumber" to licence.trim(), "vehicleRegistrationNumber" to registration.trim(), "consent" to true
            )) else repository.submitAdmin(token, mapOf(
                "fullName" to fullName.trim(), "email" to email.trim(), "phone" to phone.trim(),
                "businessName" to businessName.trim(), "reason" to reason.trim()
            ))
            result.fold(onSuccess = { status = it.status }, onFailure = { error = it.message })
            loading = false
        }
    }

    fun refresh() {
        val token = accessToken ?: return
        scope.launch {
            loading = true; error = null
            repository.getStatus(token, partner).fold(onSuccess = {
                status = it.status
                if (it.status == "approved") onApproved()
            }, onFailure = { error = it.message })
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(if (partner) "Partner application" else "Admin Access", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(18.dp))
        Icon(if (partner) Icons.Rounded.DeliveryDining else Icons.Rounded.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Text(if (partner) "Become a Delivery Partner" else "Request secure admin access", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (partner) "Join our delivery team and bring freshness to every doorstep." else "Every request is reviewed by an existing authorized administrator.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        if (status != null) {
            Text("Application status", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(status!!.replace('_', ' ').uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(if (partner) when (status) {
                "approved" -> "Your account is approved. Continue to open delivery operations."
                "rejected" -> "Your application was not approved. Contact support if you need clarification."
                "suspended" -> "Delivery access is suspended. You cannot go online or access assignments."
                "under_review" -> "Your information is being reviewed by an authorized administrator."
                else -> "Application submitted successfully. Your application is awaiting verification."
            } else "Your access request is recorded and awaiting authorized review.")
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = ::refresh, enabled = !loading) { Text("Check status") }
        } else {
            if (partner) {
                val titles = listOf("Personal information", "Delivery details", "Payout setup", "Review & submit")
                LinearProgressIndicator(progress = { (partnerStep + 1) / 4f }, modifier = Modifier.fillMaxWidth())
                Text("Step ${partnerStep + 1} of 4 • ${titles[partnerStep]}", modifier = Modifier.padding(top = 10.dp), fontWeight = FontWeight.SemiBold)
                when (partnerStep) {
                    0 -> {
                        Text("Your mobile number was verified securely before this form.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Field("Full name", fullName) { fullName = it }
                        Field("Mobile number", phone, KeyboardType.Phone) { phone = it }
                        Field("Email (optional)", email, KeyboardType.Email) { email = it }
                        Field("Service area", serviceArea) { serviceArea = it }
                    }
                    1 -> {
                        Field("Vehicle type", vehicleType) { vehicleType = it }
                        Field("Driving licence number (if required)", licence) { licence = it }
                        Field("Vehicle registration (if applicable)", registration) { registration = it }
                        Text("Only information required for your selected vehicle should be provided.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    2 -> {
                        Text("Secure payout onboarding is not configured yet.", style = MaterialTheme.typography.titleMedium)
                        Text("No bank credentials will be collected in this app. Payout setup will become available after a verified provider is connected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        Text(fullName.ifBlank { "Name not provided" }, fontWeight = FontWeight.Bold)
                        Text("$phone • ${email.ifBlank { "No email" }}")
                        Text("$vehicleType • $serviceArea")
                        Text("By submitting, you confirm these details are accurate and consent to verification.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (partnerStep > 0) OutlinedButton(onClick = { partnerStep-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = {
                            val stepValid = when (partnerStep) {
                                0 -> fullName.trim().length >= 2 && phone.filter(Char::isDigit).length >= 10 && serviceArea.trim().length >= 2
                                1 -> vehicleType.isNotBlank()
                                else -> true
                            }
                            if (!stepValid) error = "Complete the required information before continuing."
                            else if (partnerStep < 3) { error = null; partnerStep++ } else submit()
                        },
                        enabled = !loading && repository.isConfigured,
                        modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)
                    ) { if (loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text(if (partnerStep == 3) "Submit application" else "Continue") }
                }
            } else {
                Field("Full name", fullName) { fullName = it }
                Field("Mobile number", phone, KeyboardType.Phone) { phone = it }
                Field("Email", email, KeyboardType.Email) { email = it }
                Field("Store or business name", businessName) { businessName = it }
                Field("Reason for access", reason) { reason = it }
                Spacer(Modifier.height(18.dp))
                Button(onClick = ::submit, enabled = !loading && repository.isConfigured, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    if (loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Submit application")
                }
            }
            OutlinedButton(onClick = ::refresh, enabled = !loading && repository.isConfigured) { Text("I already applied") }
        }
        if (!repository.isConfigured) error = "Set API_BASE_URL to the deployed HTTPS backend before submitting."
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
    }
}

@Composable
private fun Field(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = false,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(14.dp)
    )
}
