package com.example.data.applications

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ApplicationStatus(val status: String, val decisionReason: String? = null)
data class RoleAccess(
    val admin: Boolean,
    val deliveryPartner: Boolean,
    val adminStatus: String?,
    val partnerStatus: String?
)
class ApplicationApiException(val statusCode: Int, message: String) : IllegalStateException(message)

class ApplicationRepository(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()) {
    private val baseUrl = BuildConfig.API_BASE_URL.trim().trimEnd('/')
    val isConfigured = (baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://"))) &&
        !baseUrl.contains("YOUR_API_HOST")

    suspend fun getRoleAccess(token: String): Result<RoleAccess> = withContext(Dispatchers.IO) {
        apiResult {
            val request = Request.Builder().url("$baseUrl/access/me")
                .header("Authorization", "Bearer $token").get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.code !in 200..299) parse(response.code, body)
                val json = body?.let(::JSONObject) ?: JSONObject()
                RoleAccess(
                    admin = json.optBoolean("admin"),
                    deliveryPartner = json.optBoolean("delivery_partner"),
                    adminStatus = json.optString("admin_status").ifBlank { null },
                    partnerStatus = json.optString("partner_status").ifBlank { null }
                )
            }
        }
    }

    suspend fun submitAdmin(token: String, values: Map<String, String>) = post(
        "admin/access-requests", token, JSONObject(values).toString()
    )

    suspend fun submitPartner(token: String, values: Map<String, Any>) = post(
        "delivery-partners/applications", token, JSONObject(values).toString()
    )

    suspend fun getStatus(token: String, partner: Boolean): Result<ApplicationStatus> = withContext(Dispatchers.IO) {
        apiResult {
            val path = if (partner) "delivery-partners/applications/me" else "admin/access-requests/me"
            val request = Request.Builder().url("$baseUrl/$path").header("Authorization", "Bearer $token").get().build()
            client.newCall(request).execute().use { response -> parse(response.code, response.body?.string()) }
        }
    }

    private suspend fun post(path: String, token: String, json: String): Result<ApplicationStatus> = withContext(Dispatchers.IO) {
        apiResult {
            val request = Request.Builder().url("$baseUrl/$path").header("Authorization", "Bearer $token")
                .post(json.toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().use { response -> parse(response.code, response.body?.string()) }
        }
    }

    private fun parse(code: Int, body: String?): ApplicationStatus {
        val json = body?.let(::JSONObject) ?: JSONObject()
        if (code !in 200..299) {
            val message = json.optString("message").ifBlank { when (code) {
                401 -> "Your session expired. Sign in again."
                409 -> "An active application already exists."
                else -> "The service could not complete this request."
            } }
            throw ApplicationApiException(code, message)
        }
        return ApplicationStatus(json.optString("status", "pending"), json.optString("decision_reason").ifBlank { null })
    }

    private inline fun <T> apiResult(block: () -> T): Result<T> {
        if (!isConfigured) return Result.failure(IllegalStateException("The application API URL is not configured."))
        return runCatching(block)
    }
}
