package com.hirezy.web
import android.os.Build
import androidx.annotation.RequiresApi
import android.text.TextUtils
import android.app.Activity
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.*
import androidx.appcompat.app.AlertDialog
import java.lang.ref.WeakReference

/**
 * Created by hirezy on 2020/06/30
 * 监听网页链接:
 * - 根据标识:打电话、发短信、发邮件
 * - 进度条的显示
 * - 添加javascript监听
 * - 唤起京东，支付宝，微信原生App
 */
class MulWebViewClient internal constructor(activity: Activity, mulWebView: MulWebView) :
    WebViewClient() {
    private var mActivityWeakReference: WeakReference<Activity>? = null
    private val mMulWebView: MulWebView
    private var onByWebClientCallback: OnByWebClientCallback? = null
    fun setOnByWebClientCallback(onByWebClientCallback: OnByWebClientCallback?) {
        this.onByWebClientCallback = onByWebClientCallback
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (TextUtils.isEmpty(url)) {
            return false
        }
        return if (onByWebClientCallback != null) {
            onByWebClientCallback!!.isOpenThirdApp(url)
        } else {
            val mActivity = mActivityWeakReference!!.get()
            if (mActivity != null && !mActivity.isFinishing) {
                MulWebTools.handleThirdApp(mActivity, url)
            } else {
                !url.startsWith("http:") && !url.startsWith("https:")
            }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (TextUtils.isEmpty(url)) {
            return false
        }
        return if (onByWebClientCallback != null) {
            onByWebClientCallback!!.isOpenThirdApp(url)
        } else {
            val mActivity = mActivityWeakReference!!.get()
            if (mActivity != null && !mActivity.isFinishing) {
                MulWebTools.handleThirdApp(mActivity, url)
            } else {
                !url.startsWith("http:") && !url.startsWith("https:")
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (onByWebClientCallback != null) {
            onByWebClientCallback!!.onPageStarted(view, url, favicon)
        }
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        // html加载完成之后，添加监听图片的点击js函数
        val mActivity = mActivityWeakReference!!.get()
        if (mActivity != null && !mActivity.isFinishing
            && !MulWebTools.isNetworkConnected(mActivity) && mMulWebView.progressBar != null
        ) {
            mMulWebView.progressBar!!.hide()
        }
        if (onByWebClientCallback != null) {
            onByWebClientCallback!!.onPageFinished(view, url)
        }
        super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        // 6.0以下执行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return
        }
        mMulWebView.showErrorView()
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        // 这个方法在 android 6.0才出现。加了正常的页面可能会出现错误页面
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            int statusCode = errorResponse.getStatusCode();
//            if (404 == statusCode || 500 == statusCode) {
//                mMulWebView.showErrorView();
//            }
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            // 是否是为 main frame创建
            mMulWebView.showErrorView()
        }
    }

    /**
     * 解决google play上线 WebViewClient.onReceivedSslError问题
     */
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        if (onByWebClientCallback == null || !onByWebClientCallback!!.onReceivedSslError(
                view,
                handler,
                error
            )
        ) {
            val builder = AlertDialog.Builder(view.context)
            builder.setMessage("SSL认证失败，是否继续访问？")
            builder.setPositiveButton("继续") { dialog, which -> handler.proceed() }
            builder.setNegativeButton("取消") { dialog, which -> handler.cancel() }
            val dialog = builder.create()
            dialog.show()
        } else {
            onByWebClientCallback!!.onReceivedSslError(view, handler, error)
        }
    }

    /**
     * 视频全屏播放按返回页面被放大的问题
     */
    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        super.onScaleChanged(view, oldScale, newScale)
        if (newScale - oldScale > 7) {
            //异常放大，缩回去。
            view.setInitialScale((oldScale / newScale * 100).toInt())
        }
    }

    init {
        mActivityWeakReference = WeakReference(activity)
        mMulWebView = mulWebView
    }
}