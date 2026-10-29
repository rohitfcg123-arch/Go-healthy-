package com.rohitfcg123.gohealthy

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var showingOfflineFallback = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // The request is intentionally made only once. If denied, the app will not ask again.
            webView.post {
                webView.evaluateJavascript(
                    "window.onNativeNotificationPermission && window.onNativeNotificationPermission($granted)",
                    null
                )
            }
        }

    companion object {
        private const val LIVE_URL = "https://rohitfcg123-arch.github.io/Go-healthy-/"
        private const val LOCAL_URL = "file:///android_asset/index.html"
        private const val PREFS = "go_healthy_permissions"
        private const val ASKED_NOTIFICATIONS = "asked_notifications_once"
        private const val CHANNEL_ID = "go_healthy_motivation"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(243, 247, 245)
        window.navigationBarColor = Color.rgb(243, 247, 245)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        createNotificationChannel()
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
        webView.addJavascriptInterface(NativeNotificationsBridge(), "AndroidNotifications")
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

        // Ask on the first app launch, not only when the user opens Profile.
        // Persist the attempt before showing the system prompt so a denial never causes repeat prompts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            if (!prefs.getBoolean(ASKED_NOTIFICATIONS, false)) {
                prefs.edit().putBoolean(ASKED_NOTIFICATIONS, true).apply()
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    webView.postDelayed({
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }, 450)
                }
            }
        }

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hourly motivation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Encouraging reminders for your smoke-free journey"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private inner class NativeNotificationsBridge {
        @JavascriptInterface
        fun hasPermission(): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        @JavascriptInterface
        fun notify(title: String, body: String, tag: String) {
            runOnUiThread {
                if (!hasPermission()) return@runOnUiThread
                val id = tag.hashCode()
                val notification = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(applicationInfo.icon)
                    .setContentTitle(title.take(100))
                    .setContentText(body.take(240))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(500)))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(this@MainActivity).notify(id, notification)
            }
        }
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
        webView.removeJavascriptInterface("AndroidNotifications")
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        webView.destroy()
        super.onDestroy()
    }
}
