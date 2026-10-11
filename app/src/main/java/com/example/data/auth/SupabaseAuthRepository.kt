package com.example.data.auth

import android.content.Context
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseAuthRepository internal constructor(
    context: Context,
    private val developmentSessionStore: DevelopmentSessionStore =
        AndroidKeystoreDevelopmentSessionStore(context.applicationContext)
) : AuthRepository {
    private var requestedRole: String? = null
    private var developmentAccessToken: String? = null
    private var developmentRefreshToken: String? = null
    private var developmentUser: AuthenticatedUser? = null
    private var developmentSessionRole: String? = null
    private val developmentSessionRestoreMutex = Mutex()
    private var developmentSessionRestoreAttempted = false
    private val url = BuildConfig.AUTH_SUPABASE_URL.trim()
    private val publishableKey = BuildConfig.AUTH_SUPABASE_PUBLISHABLE_KEY.trim()
    private val refreshMutex = Mutex()
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
    private val apiBaseUrl = BuildConfig.API_BASE_URL.trim().trimEnd('/')

    private val isFixedDebugOtpEnabled = BuildConfig.DEBUG && BuildConfig.DEV_FIXED_OTP_ENABLED
    override val isLocalDevelopmentAuth: Boolean = isFixedDebugOtpEnabled
    override val isDevelopmentTestOtpEnabled: Boolean = BuildConfig.DEV_TEST_OTP_ENABLED || isFixedDebugOtpEnabled
    override val developmentTestPhoneNumber: String? =
        BuildConfig.DEV_TEST_PHONE_NUMBER.takeIf { isDevelopmentTestOtpEnabled && it.isNotBlank() }
    override val developmentRolePhones: Map<String,String> = if (isFixedDebugOtpEnabled) buildMap {
        BuildConfig.DEV_TEST_PHONE_NUMBER.takeIf(String::isNotBlank)?.let { put("customer",it) }
        BuildConfig.DEV_ADMIN_TEST_PHONE_NUMBER.takeIf(String::isNotBlank)?.let { put("admin",it) }
        BuildConfig.DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER.takeIf(String::isNotBlank)?.let { put("delivery_partner",it) }
    } else emptyMap()

    override val isConfigured: Boolean =
        url.startsWith("https://") &&
            !url.contains("YOUR_SUPABASE", ignoreCase = true) &&
            publishableKey.isNotBlank() &&
            !publishableKey.contains("YOUR_SUPABASE", ignoreCase = true)

    private val client: SupabaseClient? by lazy { if (isConfigured) GreenShopSupabase.clientOrNull else null }

    override fun selectLoginRole(role: String?) { requestedRole = role }

    override fun hasSession(): Boolean = developmentAccessToken != null || client?.auth?.currentSessionOrNull() != null

    override fun accessToken(): String? = developmentAccessToken ?: client?.auth?.currentSessionOrNull()?.accessToken

    override suspend fun validAccessToken(forceRefresh: Boolean): Result<String> {
        if (developmentAccessToken != null) return refreshMutex.withLock {
            if (forceRefresh) refreshDevelopmentSession() else Result.success(developmentAccessToken!!)
        }
        val configuredClient = client ?: return Result.failure(
            IllegalStateException("Supabase authentication is not configured for this build.")
        )
        return runCatching { refreshMutex.withLock {
            configuredClient.auth.awaitInitialization()
            if (forceRefresh) configuredClient.auth.refreshCurrentSession()
            configuredClient.auth.currentSessionOrNull()?.accessToken
                ?: error("No authenticated session is available.")
        } }
    }

    override suspend fun currentUser(): Result<AuthenticatedUser?> {
        if (isFixedDebugOtpEnabled) {
            restoreDevelopmentSession()?.let { return Result.success(it) }
        }
        developmentUser?.let { return Result.success(it) }
        val configuredClient = client ?: return Result.success(null)
        return runCatching {
            configuredClient.auth.awaitInitialization()
            configuredClient.auth.currentUserOrNull()?.let { user ->
                val metadataName = sequenceOf("full_name", "name")
                    .mapNotNull { key -> runCatching { user.userMetadata?.get(key)?.jsonPrimitive?.content }.getOrNull() }
                    .firstOrNull { it.isNotBlank() }
                AuthenticatedUser(user.id, user.phone?.takeIf(String::isNotBlank), metadataName)
            }
        }
    }

    override suspend fun sendPhoneOtp(phone: String): Result<Unit> {
        if (isFixedDebugOtpEnabled) {
            val role=requestedRole?:"customer"
            val allowed=developmentRolePhones[role]
            if (allowed.isNullOrBlank()) return Result.failure(IllegalStateException("No development test phone is configured for $role login"))
            if (phone!=allowed) return Result.failure(IllegalArgumentException("Use the authorized development test number $allowed"))
            return Result.success(Unit)
        }
        return authResult { auth.signInWith(OTP) { this.phone = phone } }
    }

    override suspend fun verifyPhoneOtp(phone: String, token: String): Result<Unit> {
        if (isFixedDebugOtpEnabled) {
            return loginDevelopmentUser(phone,token,requestedRole ?: "customer")
        }
        return authResult { auth.verifyPhoneOtp(
            type = OtpType.Phone.SMS,
            phone = phone,
            token = token
        )
        check(auth.currentSessionOrNull() != null) {
            "Authentication completed without a valid session"
        }
        }
    }

    override suspend fun signOut(): Result<Unit> {
        if (isFixedDebugOtpEnabled) {
            val refreshToken = developmentRefreshToken
            if (refreshToken != null) {
                runCatching {
                    developmentPost("dev-auth/logout", JSONObject().put("refreshToken", refreshToken))
                }.onFailure {
                    Log.w("GreenShopAuth", "Could not revoke the development session on the API")
                }
            }
            withContext(Dispatchers.IO) { developmentSessionStore.clear() }
            clearDevelopmentSession()
            requestedRole = null
            developmentSessionRestoreAttempted = true
            return Result.success(Unit)
        }
        return authResult { auth.signOut() }
    }

    private suspend fun loginDevelopmentUser(phone:String,otp:String,role:String):Result<Unit> = runCatching {
        val json=developmentPost("dev-auth/login",JSONObject().put("phone",phone).put("otp",otp).put("role",role))
        storeDevelopmentSession(json, expectedPhone = phone, expectedRole = role)
        developmentSessionRestoreAttempted = true
    }

    private suspend fun refreshDevelopmentSession():Result<String> = runCatching {
        val refresh=developmentRefreshToken?:error("Development refresh token is unavailable")
        val json=developmentPost("dev-auth/refresh",JSONObject().put("refreshToken",refresh))
        storeDevelopmentSession(
            json,
            expectedPhone = developmentUser?.verifiedPhone,
            expectedRole = developmentSessionRole,
            expectedUserId = developmentUser?.id
        )
        developmentAccessToken?:error("Development access token is unavailable")
    }

    private suspend fun restoreDevelopmentSession(): AuthenticatedUser? =
        developmentSessionRestoreMutex.withLock {
            if (developmentSessionRestoreAttempted) return@withLock developmentUser
            val saved = withContext(Dispatchers.IO) { developmentSessionStore.read() }
            developmentSessionRestoreAttempted = true
            if (saved == null) return@withLock null

            developmentAccessToken = saved.accessToken
            developmentRefreshToken = saved.refreshToken
            developmentSessionRole = saved.role
            developmentUser = AuthenticatedUser(saved.userId, saved.phone, developmentDisplayName(saved.role))

            refreshMutex.withLock {
                refreshDevelopmentSession().fold(
                    onSuccess = { developmentUser },
                    onFailure = { error ->
                        if (error !is IOException && "unreachable" !in error.message.orEmpty().lowercase()) {
                            withContext(Dispatchers.IO) { developmentSessionStore.clear() }
                            clearDevelopmentSession()
                            null
                        } else {
                            throw error
                        }
                    }
                )
            }
        }

    private suspend fun storeDevelopmentSession(
        json: JSONObject,
        expectedPhone: String? = null,
        expectedRole: String? = null,
        expectedUserId: String? = null
    ) {
        val accessToken = json.getString("accessToken")
        val refreshToken = json.getString("refreshToken")
        val user = json.getJSONObject("user")
        val userId = user.getString("id")
        val phone = user.getString("phone")
        val role = user.getString("role")
        require(accessToken.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank()) {
            "Development authentication returned an incomplete session"
        }
        require(role in setOf("customer", "admin", "delivery_partner")) {
            "Development authentication returned an invalid role"
        }
        require(expectedPhone == null || expectedPhone == phone) {
            "Development authentication returned a different phone number"
        }
        require(expectedRole == null || expectedRole == role) {
            "Development authentication returned a different role"
        }
        require(expectedUserId == null || expectedUserId == userId) {
            "Development authentication returned a different user"
        }

        withContext(Dispatchers.IO) {
            developmentSessionStore.write(
                DevelopmentSession(accessToken, refreshToken, userId, phone, role)
            )
        }
        developmentAccessToken = accessToken
        developmentRefreshToken = refreshToken
        developmentSessionRole = role
        developmentUser = AuthenticatedUser(userId, phone, developmentDisplayName(role))
    }

    private fun clearDevelopmentSession() {
        developmentAccessToken = null
        developmentRefreshToken = null
        developmentSessionRole = null
        developmentUser = null
    }

    private fun developmentDisplayName(role: String) = when (role) {
        "admin" -> "Development administrator"
        "delivery_partner" -> "Development delivery partner"
        else -> "Development customer"
    }

    private suspend fun developmentPost(path:String,body:JSONObject):JSONObject=withContext(Dispatchers.IO){
        check(apiBaseUrl.startsWith("https://") || (BuildConfig.DEBUG && apiBaseUrl.startsWith("http://"))){"Development API URL is not configured"}
        val endpoint="$apiBaseUrl/$path"
        val request=Request.Builder().url(endpoint).post(body.toString().toRequestBody("application/json".toMediaType())).build()
        try { http.newCall(request).execute().use{response->val raw=response.body?.string().orEmpty();if(!response.isSuccessful){Log.w("GreenShopAuth","Development authentication request failed: endpoint=$endpoint status=${response.code}");val message=runCatching{JSONObject(raw).optString("message")}.getOrNull().orEmpty();error(message.ifBlank{"Development authentication failed with HTTP ${response.code}"})};JSONObject(raw)} }
        catch(error:IOException){Log.w("GreenShopAuth","Authentication server unreachable: endpoint=$endpoint cause=${error.javaClass.simpleName}");throw IOException("Authentication server unreachable at ${request.url.host}:${request.url.port}",error)}
    }

    private suspend fun authResult(block: suspend SupabaseClient.() -> Unit): Result<Unit> {
        if (isFixedDebugOtpEnabled) {
            return runCatching { client?.block() ?: block(GreenShopSupabase.clientOrNull ?: error("Supabase authentication is not configured")) }
        }
        val configuredClient = client ?: return Result.failure(
            IllegalStateException("Supabase authentication is not configured for this build.")
        )
        return runCatching { configuredClient.block() }
    }
}
