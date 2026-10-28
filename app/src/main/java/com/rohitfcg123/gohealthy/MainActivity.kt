package com.rohitfcg123.gohealthy

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var showingOfflineFallback = false

    companion object {
        private const val LIVE_URL = "https://rohitfcg123-arch.github.io/Go-healthy-/"
        private const val LOCAL_URL = "file:///android_asset/index.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(243, 247, 245)
        window.navigationBarColor = Color.rgb(243, 247, 245)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(243, 247, 245))
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            loadsImagesAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url?.startsWith(LIVE_URL) == true) showingOfflineFallback = false
            }
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && !showingOfflineFallback) {
                    showingOfflineFallback = true
                    view?.loadUrl(LOCAL_URL)
                }
            }
        }
        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "(function(){var b=document.querySelector('[data-action=backHome]');if(b){b.click();return 'handled';}var h=document.querySelector('[data-page=home]');if(h&&!h.classList.contains('active')){h.click();return 'handled';}return 'exit';})()"
                ) { result ->
                    if (result == "\"exit\"" || result == "null") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
        if (savedInstanceState != null) webView.restoreState(savedInstanceState) else loadLiveApp()
    }

    private fun loadLiveApp() {
        showingOfflineFallback = false
        val url = LIVE_URL + "?native=1&refresh=" + System.currentTimeMillis()
        webView.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        webView.destroy()
        super.onDestroy()
    }
}
