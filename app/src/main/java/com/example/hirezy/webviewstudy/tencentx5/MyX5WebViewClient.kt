package com.example.hirezy.webviewstudy.tencentx5
import android.text.TextUtils
import android.util.Log
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient

/**
 * Created by hirezy on 2019/01/15.
 * 监听网页链接:
 * - 根据标识:打电话、发短信、发邮件
 * - 进度条的显示
 * - 添加javascript监听
 * - 唤起京东，支付宝，微信原生App
 */
class MyX5WebViewClient internal constructor(private val mIWebPageView: IX5WebPageView) :
    WebViewClient() {
    private val mActivity: X5WebViewActivity = mIWebPageView as X5WebViewActivity
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
        if (errorCode == 404) {
            //用javascript隐藏系统定义的404页面信息
            val data = "Page NO FOUND！"
            view.loadUrl("javascript:document.body.innerHTML=\"$data\"")
        }
    }

    // SSL Error. Failed to validate the certificate chain,error: java.security.cert.CertPathValidatorExcept
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed() //解决方案, 不要调用super.xxxx
    }

    // 视频全屏播放按返回页面被放大的问题
    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        super.onScaleChanged(view, oldScale, newScale)
        if (newScale - oldScale > 7) {
            view.setInitialScale((oldScale / newScale * 100).toInt()) //异常放大，缩回去。
        }
    }

}