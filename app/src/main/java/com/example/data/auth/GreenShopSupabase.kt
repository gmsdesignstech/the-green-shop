package com.example.data.auth

import android.content.Intent
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.createSupabaseClient

object GreenShopSupabase {
    const val callbackScheme = "greenshop"
    const val callbackHost = "login-callback"
    const val callbackUrl = "$callbackScheme://$callbackHost"

    val clientOrNull: SupabaseClient? by lazy {
        val url = BuildConfig.AUTH_SUPABASE_URL.trim()
        val key = BuildConfig.AUTH_SUPABASE_PUBLISHABLE_KEY.trim()
        if (!url.startsWith("https://") || url.contains("YOUR_SUPABASE", true) || key.isBlank()) {
            null
        } else {
            createSupabaseClient(url, key) {
                install(Auth) {
                    scheme = callbackScheme
                    host = callbackHost
                    flowType = FlowType.PKCE
                }
            }
        }
    }

    fun handleDeepLink(intent: Intent, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val client = clientOrNull ?: return
        if (intent.data?.scheme != callbackScheme || intent.data?.host != callbackHost) return
        client.handleDeeplinks(intent, onSessionSuccess = { onSuccess() }, onError = onError)
    }
}
