package com.example.data.auth

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

class SupabaseAuthRepository : AuthRepository {
    private var requestedRole: String? = null
    private var developmentAccessToken: String? = null
    private var developmentRefreshToken: String? = null
    private var developmentUser: AuthenticatedUser? = null
    private val url = BuildConfig.AUTH_SUPABASE_URL.trim()
    private val publishableKey = BuildConfig.AUTH_SUPABASE_PUBLISHABLE_KEY.trim()
    private val refreshMutex = Mutex()
    private val http = OkHttpClient()
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
        developmentRefreshToken?.let { refresh ->
            runCatching { developmentPost("dev-auth/logout",JSONObject().put("refreshToken",refresh)) }
            developmentAccessToken=null;developmentRefreshToken=null;developmentUser=null;requestedRole=null
            return Result.success(Unit)
        }
        return authResult { auth.signOut() }
    }

    private suspend fun loginDevelopmentUser(phone:String,otp:String,role:String):Result<Unit> = runCatching {
        val json=developmentPost("dev-auth/login",JSONObject().put("phone",phone).put("otp",otp).put("role",role))
        storeDevelopmentSession(json)
    }

    private suspend fun refreshDevelopmentSession():Result<String> = runCatching {
        val refresh=developmentRefreshToken?:error("Development refresh token is unavailable")
        val json=developmentPost("dev-auth/refresh",JSONObject().put("refreshToken",refresh))
        storeDevelopmentSession(json)
        developmentAccessToken?:error("Development access token is unavailable")
    }

    private fun storeDevelopmentSession(json:JSONObject){
        developmentAccessToken=json.getString("accessToken");developmentRefreshToken=json.getString("refreshToken")
        val user=json.getJSONObject("user");val role=user.getString("role");developmentUser=AuthenticatedUser(user.getString("id"),user.getString("phone"),when(role){"admin"->"Development administrator";"delivery_partner"->"Development delivery partner";else->"Development customer"})
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
