package com.hirezy.web

import android.widget.FrameLayout
import android.view.MotionEvent
import kotlin.jvm.JvmOverloads
import android.os.Build
import androidx.annotation.RequiresApi
import com.hirezy.web.ByLoadJsHolder
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import android.app.Activity
import com.hirezy.web.ByWebView
import com.hirezy.web.ByFullscreenHolder
import com.hirezy.web.OnTitleProgressCallback
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import com.hirezy.web.R
import android.content.Intent
import com.hirezy.web.ByWebChromeClient
import android.webkit.PermissionRequest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import com.hirezy.web.ByWebTools
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.ViewGroup
import com.hirezy.web.OnByWebClientCallback
import androidx.annotation.LayoutRes
import com.hirezy.web.ByWebViewClient
import android.content.DialogInterface
import android.graphics.Shader
import android.view.View.MeasureSpec
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.view.animation.DecelerateInterpolator
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.AnimatorListenerAdapter
import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import java.lang.ref.WeakReference

/**
 * Created by hirezy on 2019/07/27.
 * - 播放网络视频配置
 * - 上传图片(兼容)
 */
class ByWebChromeClient internal constructor(activity: Activity, byWebView: ByWebView) :
    WebChromeClient() {
    private var mActivityWeakReference: WeakReference<Activity>? = null
    private val mByWebView: ByWebView
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mUploadMessageForAndroid5: ValueCallback<Array<Uri>>? = null
    private var mProgressVideo: View? = null
    private var mCustomView: View? = null
    private var mCustomViewCallback: CustomViewCallback? = null
    var videoFullView: ByFullscreenHolder? = null
        private set
    private var onByWebChromeCallback: OnTitleProgressCallback? = null

    // 修复可能部分h5无故横屏问题
    private var isFixScreenLandscape = false

    // 修复可能部分h5无故竖屏问题
    private var isFixScreenPortrait = false
    fun setOnByWebChromeCallback(onByWebChromeCallback: OnTitleProgressCallback?) {
        this.onByWebChromeCallback = onByWebChromeCallback
    }

    fun setFixScreenLandscape(fixScreenLandscape: Boolean) {
        isFixScreenLandscape = fixScreenLandscape
    }

    fun setFixScreenPortrait(fixScreenPortrait: Boolean) {
        isFixScreenPortrait = fixScreenPortrait
    }

    /**
     * 播放网络视频时全屏会被调用的方法
     */
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val mActivity = mActivityWeakReference!!.get()
        if (mActivity != null && !mActivity.isFinishing) {
            if (!isFixScreenLandscape) {
                mActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            mByWebView.webView?.visibility = View.INVISIBLE

            // 如果一个视图已经存在，那么立刻终止并新建一个
            if (mCustomView != null) {
                callback.onCustomViewHidden()
                return
            }
            val decor = mActivity.window.decorView as FrameLayout
            videoFullView = ByFullscreenHolder(mActivity)
            videoFullView!!.addView(view)
            decor.addView(videoFullView)
            mCustomView = view
            mCustomViewCallback = callback
            videoFullView!!.visibility = View.VISIBLE
        }
    }

    /**
     * 视频播放退出全屏会被调用的
     */
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onHideCustomView() {
        val mActivity = mActivityWeakReference!!.get()
        if (mActivity != null && !mActivity.isFinishing) {
            // 不是全屏播放状态
            if (mCustomView == null) {
                return
            }
            // 还原到之前的屏幕状态
            if (!isFixScreenPortrait) {
                mActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            mCustomView!!.visibility = View.GONE
            if (videoFullView != null) {
                videoFullView!!.removeView(mCustomView)
                videoFullView!!.visibility = View.GONE
            }
            mCustomView = null
            mCustomViewCallback!!.onCustomViewHidden()
            mByWebView.webView?.visibility = View.VISIBLE
        }
    }

    /**
     * 视频加载时loading
     */
    override fun getVideoLoadingProgressView(): View? {
        if (mProgressVideo == null) {
            mProgressVideo = LayoutInflater.from(mByWebView.webView?.context)
                .inflate(R.layout.by_video_loading_progress, null)
        }
        return mProgressVideo
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        // 进度条
        if (mByWebView.progressBar != null) {
            mByWebView.progressBar?.setWebProgress(newProgress)
        }
        // 当显示错误页面时，进度达到100才显示网页
        if (mByWebView.webView != null && mByWebView.webView!!.visibility == View.INVISIBLE && (mByWebView.errorView == null || mByWebView.errorView!!.visibility == View.GONE)
            && newProgress == 100
        ) {
            mByWebView.webView!!.visibility = View.VISIBLE
        }
        if (onByWebChromeCallback != null) {
            onByWebChromeCallback!!.onProgressChanged(newProgress)
        }
    }

    /**
     * 判断是否是全屏
     */
    fun inCustomView(): Boolean {
        return mCustomView != null
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        // 设置title
        if (onByWebChromeCallback != null) {
            if (mByWebView.errorView != null && mByWebView.errorView!!.visibility == View.VISIBLE) {
                onByWebChromeCallback!!.onReceivedTitle(if (TextUtils.isEmpty(mByWebView.errorTitle)) "网页无法打开" else mByWebView.errorTitle)
            } else {
                onByWebChromeCallback!!.onReceivedTitle(title)
            }
        }
    }

    //扩展浏览器上传文件
    //3.0++版本
    fun openFileChooser(uploadMsg: ValueCallback<Uri?>, acceptType: String?) {
        openFileChooserImpl(uploadMsg)
    }

    //3.0--版本
    fun openFileChooser(uploadMsg: ValueCallback<Uri?>) {
        openFileChooserImpl(uploadMsg)
    }

    fun openFileChooser(uploadMsg: ValueCallback<Uri?>, acceptType: String?, capture: String?) {
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

    private fun openFileChooserImpl(uploadMsg: ValueCallback<Uri?>) {
        val mActivity = mActivityWeakReference!!.get()
        if (mActivity != null && !mActivity.isFinishing) {
            mUploadMessage = uploadMsg
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            mActivity.startActivityForResult(
                Intent.createChooser(intent, "文件选择"),
                RESULT_CODE_FILE_CHOOSER
            )
        }
    }

    private fun openFileChooserImplForAndroid5(uploadMsg: ValueCallback<Array<Uri>>) {
        val mActivity = mActivityWeakReference!!.get()
        if (mActivity != null && !mActivity.isFinishing) {
            mUploadMessageForAndroid5 = uploadMsg
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "image/*"
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "图片选择")
            mActivity.startActivityForResult(chooserIntent, RESULT_CODE_FILE_CHOOSER_FOR_ANDROID_5)
        }
    }

    /**
     * 5.0以下 上传图片成功后的回调
     */
    private fun uploadMessage(intent: Intent?, resultCode: Int) {
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
    private fun uploadMessageForAndroid5(intent: Intent?, resultCode: Int) {
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

    /**
     * 用于Activity的回调
     */
    fun handleFileChooser(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == RESULT_CODE_FILE_CHOOSER) {
            uploadMessage(intent, resultCode)
        } else if (requestCode == RESULT_CODE_FILE_CHOOSER_FOR_ANDROID_5) {
            uploadMessageForAndroid5(intent, resultCode)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest) {
        super.onPermissionRequest(request)
        // 部分页面可能崩溃
//        request.grant(request.getResources());
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        return if (super.getDefaultVideoPoster() == null) {
            BitmapFactory.decodeResource(mByWebView.webView?.resources, R.drawable.by_icon_video)
        } else {
            super.getDefaultVideoPoster()
        }
    }

    companion object {
        private const val RESULT_CODE_FILE_CHOOSER = 1
        private const val RESULT_CODE_FILE_CHOOSER_FOR_ANDROID_5 = 2
    }

    init {
        mActivityWeakReference = WeakReference(activity)
        mByWebView = byWebView
    }
}