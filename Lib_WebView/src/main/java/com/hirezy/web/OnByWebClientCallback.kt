package com.hirezy.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView

/**
 * Created by hirezy on 2020/6/30.
 */
abstract class OnByWebClientCallback {
    open fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {}
    open fun onPageFinished(view: WebView?, url: String?) {}
    open fun isOpenThirdApp(url: String): Boolean {
        return !url.startsWith("http:") && !url.startsWith("https:")
    }

    /**
     * @return true 表示是自己处理的
     */
    open fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ): Boolean {
        return false
    }
}