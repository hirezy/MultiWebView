package com.example.hirezy.webviewstudy.tencentx5
import com.example.hirezy.webviewstudy.R
import android.content.pm.ActivityInfo
import android.content.Intent
import android.app.Activity
import android.graphics.Bitmap
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.*
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView

/**
 * Created by hirezy on 2019/1/15.
 * - 播放网络视频配置
 * - 上传图片(兼容)
 */
class MyX5WebChromeClient(private val mIWebPageView: IX5WebPageView) : WebChromeClient() {
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mUploadMessageForAndroid5: ValueCallback<Array<Uri>>? = null
    private var mXProgressVideo: View? = null
    private val mActivity: X5WebViewActivity = mIWebPageView as X5WebViewActivity
    private var mXCustomView: View? = null
    private var mXCustomViewCallback: IX5WebChromeClient.CustomViewCallback? = null

    /**
     * 播放网络视频时全屏会被调用的方法
     */
    override fun onShowCustomView(view: View, callback: IX5WebChromeClient.CustomViewCallback) {
        mIWebPageView.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        mIWebPageView.hindWebView()
        // 如果一个视图已经存在，那么立刻终止并新建一个
        if (mXCustomView != null) {
            callback.onCustomViewHidden()
            return
        }
        mIWebPageView.fullViewAddView(view)
        mXCustomView = view
        mXCustomViewCallback = callback
        mIWebPageView.showVideoFullView()
    }

    /**
     * 视频播放退出全屏会被调用的
     */
    override fun onHideCustomView() {
        // 不是全屏播放状态
        if (mXCustomView == null) {
            return
        }
        mIWebPageView.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        mXCustomView!!.visibility = View.GONE
        if (mIWebPageView.videoFullView != null) {
            mIWebPageView.videoFullView!!.removeView(mXCustomView)
        }
        mXCustomView = null
        mIWebPageView.hindVideoFullView()
        mXCustomViewCallback!!.onCustomViewHidden()
        mIWebPageView.showWebView()
    }

    /**
     * 视频加载时loading
     */
    override fun getVideoLoadingProgressView(): View {
        if (mXProgressVideo == null) {
            mXProgressVideo = mIWebPageView.videoLoadingProgressView
        }
        return mXProgressVideo!!
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        mIWebPageView.startProgress(newProgress)
    }

    /**
     * 判断是否是全屏
     */
    fun inCustomView(): Boolean {
        return mXCustomView != null
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        // 设置title
        mIWebPageView.onReceivedTitle(view, title)
    }

    //扩展浏览器上传文件
    //3.0++版本
    fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String?) {
        openFileChooserImpl(uploadMsg)
    }

    //3.0--版本
    fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
        openFileChooserImpl(uploadMsg)
    }

    override fun openFileChooser(
        uploadMsg: ValueCallback<Uri?>?,
        acceptType: String,
        capture: String
    ) {
        openFileChooserImpl(uploadMsg)
    }

    // For Android > 5.0
    override fun onShowFileChooser(
        webView: WebView,
        uploadMsg: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        openFileChooserImplForAndroid5(uploadMsg)
        return true
    }

    private fun openFileChooserImpl(uploadMsg: ValueCallback<Uri?>?) {
        mUploadMessage = uploadMsg
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        mIWebPageView.startFileChooserForResult(
            Intent.createChooser(intent, "文件选择"),
            FILECHOOSER_RESULTCODE
        )
    }

    private fun openFileChooserImplForAndroid5(uploadMsg: ValueCallback<Array<Uri>>) {
        mUploadMessageForAndroid5 = uploadMsg
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "image/*"
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "图片选择")
        mIWebPageView.startFileChooserForResult(chooserIntent, FILECHOOSER_RESULTCODE_FOR_ANDROID_5)
    }

    /**
     * 5.0以下 上传图片成功后的回调
     */
    fun mUploadMessage(intent: Intent?, resultCode: Int) {
        if (null == mUploadMessage) {
            return
        }
        val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        mUploadMessage!!.onReceiveValue(result)
        mUploadMessage = null
    }

    /**
     * 5.0以上 上传图片成功后的回调
     */
    fun mUploadMessageForAndroid5(intent: Intent?, resultCode: Int) {
        if (null == mUploadMessageForAndroid5) {
            return
        }
        val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        if (result != null) {
            mUploadMessageForAndroid5!!.onReceiveValue(arrayOf(result))
        } else {
            mUploadMessageForAndroid5!!.onReceiveValue(arrayOf())
        }
        mUploadMessageForAndroid5 = null
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        return if (super.getDefaultVideoPoster() == null) {
            BitmapFactory.decodeResource(mActivity.resources, R.drawable.by_icon_video)
        } else {
            super.getDefaultVideoPoster()
        }
    }

    companion object {
        var FILECHOOSER_RESULTCODE = 1
        var FILECHOOSER_RESULTCODE_FOR_ANDROID_5 = 2
    }

}