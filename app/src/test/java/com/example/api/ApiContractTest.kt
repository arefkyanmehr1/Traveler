package com.example.api

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiContractTest {
    @Test
    fun loginRequestUsesOriginalServerFieldNames() {
        val body = Gson().toJsonTree(LoginRequest("0012345678", "password", "captcha")).asJsonObject

        assertEquals("0012345678", body.get("nationalCode").asString)
        assertEquals("password", body.get("password").asString)
        assertEquals("captcha", body.get("capToken").asString)
        assertEquals(3, body.size())
    }

    @Test
    fun gpsPointUsesOriginalRouteFieldNames() {
        val body = Gson().toJsonTree(GpsPoint(3, 51.4, 35.7, 0.0, "2025-01-02T03:04:05.000Z")).asJsonObject

        assertEquals(3, body.get("type").asInt)
        assertEquals(51.4, body.get("longitude").asDouble, 0.0)
        assertEquals(35.7, body.get("latitude").asDouble, 0.0)
        assertTrue(body.has("speed"))
        assertTrue(body.has("date"))
    }

    @Test
    fun apiErrorsPreferServerMessageAndHaveStatusSpecificFallbacks() {
        val payload = JsonParser.parseString("""{"resultMessage":"دسترسی رد شد"}""").asJsonObject
        assertEquals("دسترسی رد شد", BaarbargApi.errorMessage(payload, 403))

        assertEquals(
            "سرویس ورود یا دسترسی را نپذیرفت (401). حساب، نشانی API و در صورت نیاز کلیدهای مجاز سرویس را بررسی کنید.",
            BaarbargApi.errorMessage(JsonParser.parseString("{}").asJsonObject, 401)
        )
        assertEquals("خطای ارتباط با سرویس (503).", BaarbargApi.errorMessage(JsonParser.parseString("{}").asJsonObject, 503))
    }
}
