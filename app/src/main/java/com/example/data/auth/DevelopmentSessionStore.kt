package com.example.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class DevelopmentSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val phone: String,
    val role: String
)

internal interface DevelopmentSessionStore {
    fun read(): DevelopmentSession?
    fun write(session: DevelopmentSession)
    fun clear()
}

internal class AndroidKeystoreDevelopmentSessionStore(context: Context) : DevelopmentSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun read(): DevelopmentSession? {
        val stored = preferences.getString(SESSION_KEY, null) ?: return null
        val encrypted = Base64.decode(stored, Base64.NO_WRAP)
        require(encrypted.size > GCM_IV_LENGTH) { "Stored development session is incomplete" }

        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val session = JSONObject(String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8))
        return DevelopmentSession(
            accessToken = session.getString("accessToken"),
            refreshToken = session.getString("refreshToken"),
            userId = session.getString("userId"),
            phone = session.getString("phone"),
            role = session.getString("role")
        )
    }

    @Synchronized
    override fun write(session: DevelopmentSession) {
        val plaintext = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("userId", session.userId)
            .put("phone", session.phone)
            .put("role", session.role)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(plaintext)
        check(preferences.edit().putString(SESSION_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) {
            "Could not securely save the development session"
        }
    }

    @Synchronized
    override fun clear() {
        check(preferences.edit().remove(SESSION_KEY).commit()) {
            "Could not clear the development session"
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "green_shop_development_session"
        const val SESSION_KEY = "encrypted_session"
        const val KEY_ALIAS = "green_shop_development_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128
    }
}
