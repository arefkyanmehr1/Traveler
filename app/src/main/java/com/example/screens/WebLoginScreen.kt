package com.example.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val LOGIN_URL = "https://barname.utcms.ir/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginScreen(
    context: Context,
    onSignedIn: () -> Unit,
    onBack: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canContinue by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        CookieManager.getInstance().setAcceptCookie(true)
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()

                            val cookies = CookieManager.getInstance().getCookie(url).orEmpty()
                            // We never solve or bypass CAPTCHA. The user completes the
                            // official login flow, then explicitly confirms here.
                            canContinue = cookies.isNotBlank() && url.startsWith("https://barname.utcms.ir/")
                        }
                    }

                    loadUrl(LOGIN_URL)
                }
            },
            update = { webView = it }
        )

        Button(
            modifier = Modifier,
            enabled = canContinue,
            onClick = {
                CookieManager.getInstance().flush()
                onSignedIn()
            }
        ) {
            Text("ادامه پس از ورود")
        }

        Button(
            onClick = onBack
        ) {
            Text("بازگشت")
        }
    }
}
