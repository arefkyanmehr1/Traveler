package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.api.GeminiKeyStore
import com.example.api.ShipmentItem
import com.example.screens.*
import com.example.ui.theme.BaarbargTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaarbargTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }
                var session by remember {
                    mutableStateOf<Map<String, Any>?>(
                        when {
                            GeminiKeyStore.isWebSession(applicationContext) -> {
                                mapOf(
                                    "mode" to "web",
                                    "nationalCode" to (GeminiKeyStore.getNationalCode(applicationContext) ?: "")
                                )
                            }
                            !GeminiKeyStore.getAccessToken(applicationContext).isNullOrBlank() -> {
                                mapOf(
                                    "mode" to "live",
                                    "nationalCode" to (GeminiKeyStore.getNationalCode(applicationContext) ?: "")
                                )
                            }
                            else -> null
                        }
                    )
                }
                var selectedShipment by remember { mutableStateOf<ShipmentItem?>(null) }
                var placeholderTitle by remember { mutableStateOf("") }
                var returnToShipments by remember { mutableStateOf("dashboard") }

                if (session == null && currentScreen != "placeholder") {
                    WebLoginScreen(
                        context = applicationContext,
                        onSignedIn = {
                            GeminiKeyStore.saveWebSession(applicationContext, true)
                            session = mapOf("mode" to "web", "nationalCode" to "")
                            currentScreen = "dashboard"
                        },
                        onBack = {
                            currentScreen = "placeholder"
                            placeholderTitle = "ورود لغو شد"
                        }
                    )
                } else if (session == null) {
                    PlaceholderScreen(
                        title = placeholderTitle,
                        onBack = { currentScreen = "login" }
                    )
                } else {
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(
                            context = applicationContext,
                            session = session,
                            onOpen = { key ->
                                when (key) {
                                    "newdoc" -> { placeholderTitle = "بارنامه حقیقی"; currentScreen = "placeholder" }
                                    "daily" -> { placeholderTitle = "بارنامه روزانه"; currentScreen = "placeholder" }
                                    "carrying" -> currentScreen = "shipments_carrying"
                                    "history" -> currentScreen = "shipments_issued"
                                    "wallet" -> { placeholderTitle = "کیف پول اعتباری"; currentScreen = "placeholder" }
                                    "fuel" -> { placeholderTitle = "سهمیه سوخت"; currentScreen = "placeholder" }
                                    "inbox" -> { placeholderTitle = "پیام‌ها"; currentScreen = "placeholder" }
                                    "automation" -> currentScreen = "automation"
                                    "settings" -> currentScreen = "settings"
                                }
                            },
                            onOpenAutomation = { currentScreen = "automation" }
                        )
                        "shipments_issued" -> ShipmentsScreen(
                            context = applicationContext,
                            initialStatus = "issued",
                            mode = session?.get("mode") as? String,
                            onSelect = { item ->
                                selectedShipment = item
                                returnToShipments = "shipments_issued"
                                currentScreen = "details"
                            },
                            onBack = { currentScreen = "dashboard" }
                        )
                        "shipments_carrying" -> ShipmentsScreen(
                            context = applicationContext,
                            initialStatus = "carrying",
                            mode = session?.get("mode") as? String,
                            onSelect = { item ->
                                selectedShipment = item
                                returnToShipments = "shipments_carrying"
                                currentScreen = "details"
                            },
                            onBack = { currentScreen = "dashboard" }
                        )
                        "details" -> {
                            if (selectedShipment != null) {
                                ShipmentDetailsScreen(
                                    context = applicationContext,
                                    shipment = selectedShipment!!,
                                    demoMode = session?.get("mode") == "demo",
                                    onBack = { currentScreen = returnToShipments },
                                    onTripUpdated = { currentScreen = "dashboard" }
                                )
                            } else {
                                currentScreen = "dashboard"
                            }
                        }
                        "automation" -> GeminiAutomationScreen(
                            context = applicationContext,
                            onOpenSettings = { currentScreen = "settings" },
                            onNavigate = { section ->
                                currentScreen = when (section) {
                                    "issued" -> "shipments_issued"
                                    "carrying" -> "shipments_carrying"
                                    "settings" -> "settings"
                                    "newdoc" -> { placeholderTitle = "بارنامه حقیقی"; "placeholder" }
                                    "daily" -> { placeholderTitle = "بارنامه روزانه"; "placeholder" }
                                    "wallet" -> { placeholderTitle = "کیف پول اعتباری"; "placeholder" }
                                    "fuel" -> { placeholderTitle = "سهمیه سوخت"; "placeholder" }
                                    "inbox" -> { placeholderTitle = "پیام‌ها"; "placeholder" }
                                    else -> "dashboard"
                                }
                            },
                            onBack = { currentScreen = "dashboard" }
                        )
                        "settings" -> SettingsScreen(
                            context = applicationContext,
                            onBack = { currentScreen = "dashboard" },
                            onLogout = {
                                GeminiKeyStore.clearSession(applicationContext)
                                session = null
                                currentScreen = "login"
                            }
                        )
                        "placeholder" -> PlaceholderScreen(
                            title = placeholderTitle,
                            onBack = { currentScreen = "login" }
                        )
                        else -> DashboardScreen(
                            context = applicationContext,
                            session = session,
                            onOpen = { currentScreen = "dashboard" },
                            onOpenAutomation = { currentScreen = "automation" }
                        )
                    }
                }
            }
        }
    }
}
