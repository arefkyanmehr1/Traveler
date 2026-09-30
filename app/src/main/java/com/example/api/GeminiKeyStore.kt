package com.example.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig

object GeminiKeyStore {
    private const val PREF_NAME = "baarbarg_encrypted_prefs"
    private const val LEGACY_PREF_NAME = "baarbarg_secure_prefs"
    private const val API_PREF_NAME = "baarbarg_api_prefs"
    private const val KEY_GEMINI = "gemini.apiKey"
    private const val KEY_ACCESS_TOKEN = "auth.accessToken"
    private const val KEY_NATIONAL_CODE = "auth.nationalCode"
    private const val KEY_WEB_SESSION = "auth.webSession"
    private const val KEY_POLICY = "automation.policy.enabled"
    private const val KEY_API_BASE_URL = "api.baseUrl"
    private const val KEY_SERVICE_PASSWORD = "api.servicePassword"
    private const val KEY_SECURITY_KEY = "api.securityKey"

    private fun prefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encrypted = EncryptedSharedPreferences.create(
            appContext,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        migrateLegacyPreferences(appContext, encrypted)
        return encrypted
    }

    private fun migrateLegacyPreferences(context: Context, target: SharedPreferences) {
        val legacy = context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return

        val editor = target.edit()
        legacy.getString(KEY_GEMINI, null)?.let { editor.putString(KEY_GEMINI, it) }
        legacy.getString(KEY_ACCESS_TOKEN, null)
            ?.takeUnless { it.startsWith("token_") }
            ?.let { editor.putString(KEY_ACCESS_TOKEN, it) }
        if (legacy.contains(KEY_POLICY)) editor.putBoolean(KEY_POLICY, legacy.getBoolean(KEY_POLICY, false))
        if (editor.commit()) legacy.edit().clear().apply()
    }

    fun getGeminiApiKey(context: Context): String? = prefs(context).getString(KEY_GEMINI, null)

    fun hasGeminiApiKey(context: Context): Boolean = !getGeminiApiKey(context).isNullOrBlank()

    fun saveGeminiApiKey(context: Context, key: String) {
        val trimmed = key.trim()
        if (trimmed.length < 20) throw IllegalArgumentException("کلید Gemini معتبر به نظر نمی‌رسد.")
        prefs(context).edit().putString(KEY_GEMINI, trimmed).apply()
    }

    fun removeGeminiApiKey(context: Context) {
        prefs(context).edit().remove(KEY_GEMINI).apply()
    }

    fun getAccessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun saveAccessToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getNationalCode(context: Context): String? = prefs(context).getString(KEY_NATIONAL_CODE, null)

    fun saveNationalCode(context: Context, nationalCode: String) {
        prefs(context).edit().putString(KEY_NATIONAL_CODE, nationalCode).apply()
    }

    fun isWebSession(context: Context): Boolean = prefs(context).getBoolean(KEY_WEB_SESSION, false)

    fun saveWebSession(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WEB_SESSION, enabled).apply()
    }

    fun clearSession(context: Context) {
        prefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_NATIONAL_CODE)
            .remove(KEY_WEB_SESSION)
            .apply()
    }

    fun getServicePassword(context: Context): String? = prefs(context).getString(KEY_SERVICE_PASSWORD, null)

    fun getSecurityKey(context: Context): String? = prefs(context).getString(KEY_SECURITY_KEY, null)

    fun saveServiceCredentials(context: Context, servicePassword: String, securityKey: String) {
        prefs(context).edit().apply {
            if (servicePassword.isBlank()) remove(KEY_SERVICE_PASSWORD)
            else putString(KEY_SERVICE_PASSWORD, servicePassword.trim())
            if (securityKey.isBlank()) remove(KEY_SECURITY_KEY)
            else putString(KEY_SECURITY_KEY, securityKey.trim())
        }.apply()
    }

    fun getApiBaseUrl(context: Context): String {
        return context.getSharedPreferences(API_PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_BASE_URL, null)
            ?: BuildConfig.BAARBARG_API_BASE_URL
    }

    fun saveApiBaseUrl(context: Context, baseUrl: String) {
        val value = baseUrl.trim().trimEnd('/')
        val preferences = context.getSharedPreferences(API_PREF_NAME, Context.MODE_PRIVATE)
        if (value.isBlank()) {
            preferences.edit().remove(KEY_API_BASE_URL).apply()
            return
        }
        val parsed = android.net.Uri.parse(value)
        require(parsed.scheme == "https" && !parsed.host.isNullOrBlank()) {
            "نشانی سرویس باید یک آدرس HTTPS معتبر باشد."
        }
        preferences.edit().putString(KEY_API_BASE_URL, value).apply()
    }

    fun isAutomationEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_POLICY, false)

    fun setAutomationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_POLICY, enabled).apply()
    }
}
