package com.arata.yukarilauncher.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebStorage
import com.arata.yukarilauncher.feature.discord.DiscordAccountManager
import com.arata.yukarilauncher.feature.discord.DiscordPrefs
import com.arata.yukarilauncher.feature.discord.DiscordRpcManager
import com.arata.yukarilauncher.feature.log.Logging

private const val CHROME_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/AP1A.240505.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

private const val JS_SNIPPET =
    "javascript:(function()%7Bvar%20i%3Ddocument.createElement('iframe')%3Bdocument.body.appendChild(i)%3Balert(i.contentWindow.localStorage.token.slice(1,-1))%7D)()"

@SuppressLint("SetJavaScriptEnabled")
fun showDiscordLoginDialog(activity: Activity, onDone: () -> Unit) {
    val webView = WebView(activity)
    webView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.settings.userAgentString = CHROME_UA
    webView.setBackgroundColor(android.graphics.Color.WHITE)

    // 以前のセッションのCookie・localStorageを完全に消去してからログインページを開く
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    WebStorage.getInstance().deleteAllData()

    var tokenReceived = false
    var loginCallbackFired = false

    fun onTokenObtained(token: String) {
        if (tokenReceived) return
        tokenReceived = true
        DiscordAccountManager.loginWithToken(token) { result ->
            if (result.success && result.account != null) {
                if (DiscordPrefs.getSelectedAccountId() == null) {
                    DiscordPrefs.setSelectedAccountId(result.account.id)
                    if (DiscordPrefs.isRpcEnabled()) {
                        DiscordRpcManager.connect()
                    }
                }
            } else {
                Logging.e("DiscordLogin", "Login failed: ${result.error}")
            }
            loginCallbackFired = true
            onDone()
        }
    }

    val dialog = Dialog(activity, android.R.style.Theme_NoTitleBar_Fullscreen)
    dialog.setContentView(webView)
    dialog.setOnDismissListener {
        // ログインコールバックがまだ完了していない場合はここでは呼ばない
        if (loginCallbackFired) onDone()
    }

    webView.webViewClient = object : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Logging.d("DiscordLogin", "url: $url")
            if (url.endsWith("/app") || url.contains("/channels/")) {
                view.stopLoading()
                view.loadUrl(JS_SNIPPET)
                view.visibility = android.view.View.GONE
                return true
            }
            return false
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            Logging.d("DiscordLogin", "url2: $url")
            if (url.endsWith("/app") || url.contains("/channels/")) {
                view.stopLoading()
                view.loadUrl(JS_SNIPPET)
                view.visibility = android.view.View.GONE
                return true
            }
            return false
        }
    }
    webView.webChromeClient = object : WebChromeClient() {
        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            Logging.d("DiscordLogin", "Alert: $message")
            onTokenObtained(message)
            result.confirm()
            dialog.dismiss()
            return true
        }
    }

    dialog.show()
    webView.loadUrl("https://discord.com/login")
}
