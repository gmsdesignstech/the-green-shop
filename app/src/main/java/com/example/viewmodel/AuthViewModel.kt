package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isConfigured: Boolean = false,
    val isRestoringSession: Boolean = true,
    val sessionRestored: Boolean = false,
    val isAuthenticated: Boolean = false,
    val otpSent: Boolean = false,
    val isLoading: Boolean = false,
    val phone: String = "",
    val errorMessage: String? = null,
    val isDevelopmentTestOtpEnabled: Boolean = false,
    val developmentTestPhoneNumber: String? = null,
    val infoMessage: String? = null,
    val isLocalDevelopmentAuth: Boolean = false,
    val developmentRolePhones: Map<String, String> = emptyMap(),
    val currentUser: AuthenticatedUser? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(
            isConfigured = repository.isConfigured,
            isAuthenticated = false,
            isDevelopmentTestOtpEnabled = repository.isDevelopmentTestOtpEnabled,
            developmentTestPhoneNumber = repository.developmentTestPhoneNumber,
            isLocalDevelopmentAuth = repository.isLocalDevelopmentAuth,
            developmentRolePhones = repository.developmentRolePhones
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        loadAuthenticatedUser(restoring = true)
    }

    fun accessToken(): String? = repository.accessToken()

    fun selectLoginRole(role: String?) = repository.selectLoginRole(role)

    suspend fun validAccessToken(forceRefresh: Boolean = false): Result<String> =
        repository.validAccessToken(forceRefresh)

    private fun loadAuthenticatedUser(restoring: Boolean) {
        viewModelScope.launch {
            repository.currentUser().fold(
                onSuccess = { user -> _uiState.update { state ->
                    if (restoring && !state.isRestoringSession) state else state.copy(
                        isRestoringSession = false,
                        sessionRestored = restoring && user != null,
                        isLoading = false,
                        isAuthenticated = user != null && repository.hasSession(),
                        currentUser = user
                    )
                } },
                onFailure = { error -> _uiState.update { state ->
                    if (restoring && !state.isRestoringSession) state else state.copy(
                        isRestoringSession = false, isLoading = false, isAuthenticated = false,
                        currentUser = null, errorMessage = AuthErrorMapper.toUserMessage(error)
                    )
                } }
            )
        }
    }

    private fun refreshAuthenticatedUser() = loadAuthenticatedUser(restoring = false)

    fun refreshIdentity() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, currentUser = null) }
        refreshAuthenticatedUser()
    }

    fun sendOtp(localPhoneNumber: String) {
        if (_uiState.value.isLoading) return
        if (!localPhoneNumber.matches(Regex("^[6-9]\\d{9}$"))) {
            _uiState.update { it.copy(errorMessage = "Enter a valid 10-digit mobile number") }
            return
        }
        if (!repository.isConfigured) {
            _uiState.update {
                it.copy(errorMessage = "Authentication is not configured for this build. Add the Supabase project URL and publishable key.")
            }
            return
        }

        val phone = "+91$localPhoneNumber"
        _uiState.update { it.copy(isRestoringSession = false, sessionRestored = false, isLoading = true, errorMessage = null, currentUser = null, isAuthenticated = false) }
        viewModelScope.launch {
            repository.sendPhoneOtp(phone).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(isLoading = false, otpSent = true, phone = phone)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = AuthErrorMapper.toUserMessage(error)) }
                }
            )
        }
    }

    fun verifyOtp(token: String) {
        if (_uiState.value.isLoading) return
        val phone = _uiState.value.phone
        if (token.length != 6 || token.any { !it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit verification code") }
            return
        }
        if (phone.isBlank()) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.verifyPhoneOtp(phone, token).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = true, isAuthenticated = false, currentUser = null, errorMessage = null)
                    }
                    refreshAuthenticatedUser()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = AuthErrorMapper.toUserMessage(error)) }
                }
            )
        }
    }

    fun changePhoneNumber() {
        _uiState.update { it.copy(otpSent = false, phone = "", errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.signOut().fold(
                onSuccess = {
                    _uiState.value = AuthUiState(
                        isConfigured = repository.isConfigured,
                        isRestoringSession = false,
                        isDevelopmentTestOtpEnabled = repository.isDevelopmentTestOtpEnabled,
                        developmentTestPhoneNumber = repository.developmentTestPhoneNumber,
                        isLocalDevelopmentAuth = repository.isLocalDevelopmentAuth,
                        developmentRolePhones = repository.developmentRolePhones
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = AuthErrorMapper.toUserMessage(error)) }
                }
            )
        }
    }
}

internal object AuthErrorMapper {
    fun toUserMessage(error: Throwable): String {
        val message = sequenceOf(error.message, error.cause?.message, error::class.simpleName)
            .filterNotNull()
            .joinToString(" ")
            .lowercase()
    return when {
        "rate limit" in message || "too many requests" in message || "429" in message ->
            "Too many verification attempts. Please wait before trying again."
        "authorized development test number" in message || "not authorized for development" in message ->
            error.message ?: "Use the configured development test phone number."
        "no development test phone" in message ->
            "No development test phone is configured for this login role."
        "authentication server unreachable" in message ->
            "Couldn't reach the development API. Check this build's API_BASE_URL, make sure the phone and computer are on the same Wi-Fi, and allow port 3000 through the computer's firewall."
        "development authentication is disabled" in message ->
            "Development OTP is disabled on the authentication server."
        "internal server error" in message || "http 500" in message || "http 502" in message ||
            "http 503" in message ->
            "The development API returned a server error. Retry in a moment; if it continues, check the NestJS logs."
        "sms provider" in message || "unsupported phone provider" in message ||
            "error sending confirmation otp" in message ->
            "Phone verification is temporarily unavailable. Please contact support."
        "expired" in message -> "This verification code has expired. Request a new code."
        "invalid" in message || "token" in message || ("otp" in message && "wrong" in message) ->
            "The verification code is invalid. Try again."
        "network" in message || "unable to resolve" in message || "unknownhost" in message ||
            "timeout" in message || "connectexception" in message || "socket" in message ->
            "Unable to reach the authentication service. Check your connection and retry."
        "without a valid session" in message ->
            "Verification did not create a secure session. Please request a new code."
        "api key" in message || "invalid key" in message || "project not specified" in message ->
            "Authentication is not configured correctly for this build."
        else -> "Authentication failed. Check your details and try again."
    }
    }
}
