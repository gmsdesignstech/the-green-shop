package com.example.data.auth

data class AuthenticatedUser(
    val id: String,
    val verifiedPhone: String?,
    val displayName: String?
)

interface AuthRepository {
    val isConfigured: Boolean

    val isDevelopmentTestOtpEnabled: Boolean
        get() = false

    val developmentTestPhoneNumber: String?
        get() = null

    val isLocalDevelopmentAuth: Boolean
        get() = false

    val developmentRolePhones: Map<String, String>
        get() = emptyMap()

    fun selectLoginRole(role: String?) = Unit

    fun hasSession(): Boolean

    fun accessToken(): String? = null

    suspend fun validAccessToken(forceRefresh: Boolean = false): Result<String> =
        accessToken()?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No authenticated session is available."))

    suspend fun currentUser(): Result<AuthenticatedUser?> = Result.success(null)

    suspend fun sendPhoneOtp(phone: String): Result<Unit>

    suspend fun verifyPhoneOtp(phone: String, token: String): Result<Unit>

    suspend fun signOut(): Result<Unit>
}
