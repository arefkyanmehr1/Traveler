package com.example.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object BaarbargApi {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun post(
        context: Context,
        path: String,
        body: Any,
        authenticated: Boolean = true
    ): JsonObject = withContext(Dispatchers.IO) {
        val baseUrl = GeminiKeyStore.getApiBaseUrl(context).trimEnd('/')
        require(baseUrl.startsWith("https://")) { "نشانی HTTPS سرویس در تنظیمات معتبر نیست." }

        val request = Request.Builder()
            .url("$baseUrl$path")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .apply {
                if (authenticated) {
                    GeminiKeyStore.getAccessToken(context)?.takeIf(String::isNotBlank)?.let {
                        header("Authorization", "Bearer $it")
                    }
                }
                GeminiKeyStore.getServicePassword(context)?.takeIf(String::isNotBlank)?.let {
                    header("ServicePassword", it)
                }
                GeminiKeyStore.getSecurityKey(context)?.takeIf(String::isNotBlank)?.let {
                    header("SecurityKey", it)
                }
            }
            .post(gson.toJson(body).toRequestBody(jsonType))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val payload = try {
                JsonParser.parseString(text).asJsonObject
            } catch (_: Exception) {
                throw IllegalStateException(
                    if (response.isSuccessful) "پاسخ سرویس قابل خواندن نیست."
                    else httpErrorMessage(response.code)
                )
            }

            if (!response.isSuccessful) {
                throw IllegalStateException(errorMessage(payload, response.code))
            }
            val resultCode = payload.get("resultCode")?.asString?.toIntOrNull()
            if (resultCode != null && resultCode != 200) {
                throw IllegalStateException(errorMessage(payload, resultCode))
            }
            payload
        }
    }

    fun errorMessage(payload: JsonObject, code: Int): String {
        return sequenceOf("resultMessage", "message", "Message")
            .mapNotNull { key -> payload.get(key)?.takeIf { it.isJsonPrimitive }?.asString }
            .firstOrNull { it.isNotBlank() }
            ?: httpErrorMessage(code)
    }

    private fun httpErrorMessage(code: Int): String {
        return if (code == 401) {
            "سرویس ورود یا دسترسی را نپذیرفت (401). حساب، نشانی API و در صورت نیاز کلیدهای مجاز سرویس را بررسی کنید."
        } else {
            "خطای ارتباط با سرویس ($code)."
        }
    }
}
