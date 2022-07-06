package com.example.hirezy.webviewstudy.config
import android.text.TextUtils
import android.os.Build
import android.net.http.SslError
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AlertDialog

/**
 * Created by hirezy on 2016/11/17.
 * 监听网页链接:
 * - 根据标识:打电话、发短信、发邮件
 * - 进度条的显示
 * - 添加javascript监听
 * - 唤起京东，支付宝，微信原生App
 */
class MyWebViewClient(private val mIWebPageView: IWebPageView) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        Log.e("jing", "----url:$url")
        return if (TextUtils.isEmpty(url)) {
            false
        } else mIWebPageView.isOpenThirdApp(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        // html加载完成之后，添加监听图片的点击js函数
        mIWebPageView.onPageFinished(view, url)
        super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        //6.0以下执行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return
        }
        val mErrorUrl = "file:///android_asset/404_error.html"
        view.loadUrl(mErrorUrl)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        //WebTools.handleReceivedHttpError(view, errorResponse);
        // 这个方法在 android 6.0才出现
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val statusCode = errorResponse.statusCode
            if (404 == statusCode || 500 == statusCode) {
                val mErrorUrl = "file:///android_asset/404_error.html"
                view.loadUrl(mErrorUrl)
            }
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (request.isForMainFrame) { //是否是为 main frame创建
                val mErrorUrl = "file:///android_asset/404_error.html"
                view.loadUrl(mErrorUrl)
            }
        }
    }

    /**
     * 解决google play上线 WebViewClient.onReceivedSslError问题
     */
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val builder = AlertDialog.Builder(view.context)
        builder.setMessage("SSL认证失败，是否继续访问？")
        builder.setPositiveButton("继续") { dialog, which -> handler.proceed() }
        builder.setNegativeButton("取消") { dialog, which -> handler.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    // 视频全屏播放按返回页面被放大的问题
    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        super.onScaleChanged(view, oldScale, newScale)
        if (newScale - oldScale > 7) {
            view.setInitialScale((oldScale / newScale * 100).toInt()) //异常放大，缩回去。
        }
    }
}