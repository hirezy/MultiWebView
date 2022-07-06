package com.hirezy.web

import android.widget.FrameLayout
import android.os.Build
import android.text.TextUtils
import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import androidx.annotation.LayoutRes
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import java.lang.Exception
import java.lang.NullPointerException

/**
 * 网页可以处理:
 * 点击相应控件：
 * - 进度条显示
 * - 上传图片(版本兼容)
 * - 全屏播放网络视频
 * - 唤起微信支付宝
 * - 拨打电话、发送短信、发送邮件
 * - 返回网页上一层、显示网页标题
 * JS交互部分：
 * - 前端代码嵌入js(缺乏灵活性)
 * - 网页自带js跳转
 */
class MulWebView private constructor(builder: Builder) {
    private var mWebView: WebView? = null
    var progressBar: WebProgress? = null
        private set
    var errorView: View? = null
        private set
    private val mErrorLayoutId: Int
    val errorTitle: String? = builder.mErrorTitle
    private val activity: Activity = builder.mActivity
    private val mWebChromeClient: MulWebChromeClient?
    private var mulLoadJsHolder: MulLoadJsHolder? = null
    @SuppressLint("JavascriptInterface", "AddJavascriptInterface")
    private fun handleJsInterface(builder: Builder) {
        if (!TextUtils.isEmpty(builder.mInterfaceName) && builder.mInterfaceObj != null) {
            mWebView!!.addJavascriptInterface(builder.mInterfaceObj, builder.mInterfaceName)
        }
    }

    val loadJsHolder: MulLoadJsHolder
        get() {
            if (mulLoadJsHolder == null) {
                mulLoadJsHolder = MulLoadJsHolder(mWebView)
            }
            return mulLoadJsHolder!!
        }

    @SuppressLint("SetJavaScriptEnabled")
    private fun handleSetting() {
        val ws = mWebView!!.settings
        // 保存表单数据
        ws.saveFormData = true
        // 是否应该支持使用其屏幕缩放控件和手势缩放
        ws.setSupportZoom(true)
        ws.builtInZoomControls = true
        ws.displayZoomControls = false
        // 启动应用缓存
        ws.setAppCacheEnabled(true)
        // 设置缓存模式
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        // setDefaultZoom  api19被弃用
        // 网页内容的宽度自适应屏幕
        ws.loadWithOverviewMode = true
        ws.useWideViewPort = true
        // 告诉WebView启用JavaScript执行。默认的是false。
        ws.javaScriptEnabled = true
        //  页面加载好以后，再放开图片
        ws.blockNetworkImage = false
        // 使用localStorage则必须打开
        ws.domStorageEnabled = true
        ws.allowFileAccess = true
        ws.allowFileAccessFromFileURLs=true
        ws.allowUniversalAccessFromFileURLs=true
        ws.loadsImagesAutomatically=true
        ws.layoutAlgorithm=WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        // 排版适应屏幕
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ws.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        } else {
            ws.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        }
        // WebView是否新窗口打开(加了后可能打不开网页)
//        ws.setSupportMultipleWindows(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // WebView从5.0开始默认不允许混合模式,https中不能加载http资源,需要设置开启。
            ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    /**
     * 设置字体默认缩放大小(改变网页字体大小,setTextSize  api14被弃用)
     *
     * @param textZoom 默认100
     */
    fun setTextZoom(textZoom: Int) {
        mWebView!!.settings.textZoom = textZoom
    }

    private fun handleWebProgress(builder: Builder, parentLayout: FrameLayout) {
        if (builder.mUseWebProgress) {
            progressBar = WebProgress(activity)
            if (builder.mProgressStartColor != 0 && builder.mProgressEndColor != 0) {
                progressBar!!.setColor(builder.mProgressStartColor, builder.mProgressEndColor)
            } else if (builder.mProgressStartColor != 0) {
                progressBar!!.setColor(builder.mProgressStartColor, builder.mProgressStartColor)
            } else if (!TextUtils.isEmpty(builder.mProgressStartColorString)
                && !TextUtils.isEmpty(builder.mProgressEndColorString)
            ) {
                progressBar!!.setColor(
                    builder.mProgressStartColorString,
                    builder.mProgressEndColorString
                )
            } else if (!TextUtils.isEmpty(builder.mProgressStartColorString)
                && TextUtils.isEmpty(builder.mProgressEndColorString)
            ) {
                progressBar!!.setColor(
                    builder.mProgressStartColorString,
                    builder.mProgressStartColorString
                )
            }
            var progressHeight = MulWebTools.dip2px(
                parentLayout.context,
                WebProgress.Companion.WEB_PROGRESS_DEFAULT_HEIGHT.toFloat()
            )
            if (builder.mProgressHeightDp != 0) {
                progressBar!!.height = builder.mProgressHeightDp
                progressHeight =
                    MulWebTools.dip2px(parentLayout.context, builder.mProgressHeightDp.toFloat())
            }
            progressBar!!.visibility = View.GONE
            parentLayout.addView(
                progressBar,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, progressHeight)
            )
        }
    }

    fun loadUrl(url: String) {
        if (!TextUtils.isEmpty(url) && url.endsWith("mp4") && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mWebView!!.loadData(MulWebTools.getVideoHtmlBody(url), "text/html", "UTF-8")
        } else {
            mWebView!!.loadUrl(url)
        }
        if (progressBar != null) {
            progressBar!!.show()
        }
        hideErrorView()
    }

    fun reload() {
        hideErrorView()
        mWebView!!.reload()
    }

    fun onResume() {
        mWebView!!.onResume()
        // 支付宝网页版在打开文章详情之后,无法点击按钮下一步
        mWebView!!.resumeTimers()
    }

    fun onPause() {
        mWebView!!.onPause()
        mWebView!!.resumeTimers()
    }

    fun onDestroy() {
        if (mWebChromeClient?.videoFullView != null) {
            mWebChromeClient.videoFullView!!.removeAllViews()
        }
        if (mWebView != null) {
            val parent = mWebView!!.parent as ViewGroup
            parent?.removeView(mWebView)
            mWebView!!.removeAllViews()
            mWebView!!.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            mWebView!!.stopLoading()
            mWebView!!.webChromeClient = null
            mWebView!!.webViewClient = null
            mWebView!!.destroy()
            mWebView = null
        }
    }

    /**
     * 选择图片之后的回调，在Activity里onActivityResult调用
     */
    fun handleFileChooser(requestCode: Int, resultCode: Int, intent: Intent?) {
        mWebChromeClient?.handleFileChooser(requestCode, resultCode, intent)
    }

    fun handleKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            isBack
        } else false
    }// 返回网页上一页

    // 全屏播放退出全屏
    @get:SuppressLint("SourceLockedOrientationActivity")
    val isBack: Boolean
        get() {
            // 全屏播放退出全屏
            if (mWebChromeClient!!.inCustomView()) {
                mWebChromeClient.onHideCustomView()
                return true

                // 返回网页上一页
            } else if (mWebView!!.canGoBack()) {
                hideErrorView()
                mWebView!!.goBack()
                return true
            }
            return false
        }

    /**
     * 配置自定义的WebView
     */
    var webView: WebView?
        get() = mWebView
        private set(mCustomWebView) {
            mWebView = mCustomWebView ?: WebView(activity)
        }

    /**
     * 显示错误布局
     */
    fun showErrorView() {
        try {
            if (errorView == null) {
                val parent = mWebView!!.parent as FrameLayout
                errorView = LayoutInflater.from(parent.context).inflate(
                    if (mErrorLayoutId == 0) R.layout.by_load_url_error else mErrorLayoutId,
                    null
                )
                errorView!!.setOnClickListener(View.OnClickListener { reload() })
                parent.addView(
                    errorView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            } else {
                errorView!!.visibility = View.VISIBLE
            }
            mWebView!!.visibility = View.INVISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 隐藏错误布局
     */
    fun hideErrorView() {
        if (errorView != null) {
            errorView!!.visibility = View.GONE
        }
    }

    /**
     * 修复可能部分h5无故竖屏问题，如果h5里有视频全屏播放请禁用
     */
    fun setFixScreenPortrait(fixScreenPortrait: Boolean) {
        mWebChromeClient?.setFixScreenPortrait(fixScreenPortrait)
    }

    /**
     * 修复可能部分h5无故横屏问题，如果h5里有视频全屏播放请禁用
     */
    fun setFixScreenLandscape(fixScreenLandscape: Boolean) {
        mWebChromeClient?.setFixScreenLandscape(fixScreenLandscape)
    }

    class Builder(val mActivity: Activity) {
        // 默认使用进度条
        var mUseWebProgress = true

        // 进度条 开始颜色
        var mProgressStartColor = 0
        var mProgressStartColorString: String? = null

        // 进度条 结束颜色
        var mProgressEndColor = 0
        var mProgressEndColorString: String? = null

        // 进度条 高度
        var mProgressHeightDp = 0
        var mErrorLayoutId = 0
        var mIndex = -1
        var mErrorTitle: String? = null
        var mCustomWebView: WebView? = null
        var mInterfaceName: String? = null
        var mInterfaceObj: Any? = null
        var mWebContainer: ViewGroup? = null
        var mLayoutParams: ViewGroup.LayoutParams? = null
        var mOnTitleProgressCallback: OnTitleProgressCallback? = null
        var mOnByWebClientCallback: OnByWebClientCallback? = null

        /**
         * WebView容器
         */
        fun setWebParent(webContainer: ViewGroup, layoutParams: ViewGroup.LayoutParams?): Builder {
            mWebContainer = webContainer
            mLayoutParams = layoutParams
            return this
        }

        /**
         * WebView容器
         *
         * @param webContainer 外部WebView容器
         * @param index        加入的位置
         * @param layoutParams 对应的LayoutParams
         */
        fun setWebParent(
            webContainer: ViewGroup,
            index: Int,
            layoutParams: ViewGroup.LayoutParams?
        ): Builder {
            mWebContainer = webContainer
            mIndex = index
            mLayoutParams = layoutParams
            return this
        }

        /**
         * @param isUse 是否使用进度条，默认true
         */
        fun useWebProgress(isUse: Boolean): Builder {
            mUseWebProgress = isUse
            return this
        }

        /**
         * 设置进度条颜色
         *
         * @param color 示例：ContextCompat.getColor(this, R.color.red)
         */
        fun useWebProgress(color: Int): Builder {
            return useWebProgress(color, color, 3)
        }

        /**
         * 设置进度条颜色
         *
         * @param color 示例："#FF0000"
         */
        fun useWebProgress(color: String?): Builder {
            return useWebProgress(color, color, 3)
        }

        /**
         * 设置进度条渐变色颜色
         *
         * @param startColor 开始颜色
         * @param endColor   结束颜色
         * @param heightDp   进度条高度，单位dp
         */
        fun useWebProgress(startColor: Int, endColor: Int, heightDp: Int): Builder {
            mProgressStartColor = startColor
            mProgressEndColor = endColor
            mProgressHeightDp = heightDp
            return this
        }

        fun useWebProgress(startColor: String?, endColor: String?, heightDp: Int): Builder {
            mProgressStartColorString = startColor
            mProgressEndColorString = endColor
            mProgressHeightDp = heightDp
            return this
        }

        /**
         * @param customWebView 自定义的WebView
         */
        fun setCustomWebView(customWebView: WebView?): Builder {
            mCustomWebView = customWebView
            return this
        }

        /**
         * @param errorLayoutId 错误页面布局，标题默认“网页打开失败”
         */
        fun setErrorLayout(@LayoutRes errorLayoutId: Int): Builder {
            mErrorLayoutId = errorLayoutId
            return this
        }

        /**
         * @param errorLayoutId 错误页面布局
         * @param errorTitle    错误页面标题
         */
        fun setErrorLayout(@LayoutRes errorLayoutId: Int, errorTitle: String?): Builder {
            mErrorLayoutId = errorLayoutId
            mErrorTitle = errorTitle
            return this
        }

        /**
         * 添加Js监听
         */
        fun addJavascriptInterface(interfaceName: String?, interfaceObj: Any?): Builder {
            mInterfaceName = interfaceName
            mInterfaceObj = interfaceObj
            return this
        }

        /**
         * @param onTitleProgressCallback 返回Title 和 Progress
         */
        fun setOnTitleProgressCallback(onTitleProgressCallback: OnTitleProgressCallback?): Builder {
            mOnTitleProgressCallback = onTitleProgressCallback
            return this
        }

        /**
         * 页面加载结束监听 和 处理三方跳转链接
         */
        fun setOnByWebClientCallback(onByWebClientCallback: OnByWebClientCallback?): Builder {
            mOnByWebClientCallback = onByWebClientCallback
            return this
        }

        /**
         * 直接获取ByWebView，避免一定要调用loadUrl()才能获取ByWebView的情况
         */
        fun get(): MulWebView {
            return MulWebView(this)
        }

        /**
         * loadUrl()并获取ByWebView
         */
        fun loadUrl(url: String): MulWebView {
            val byWebView = get()
            byWebView.loadUrl(url)
            return byWebView
        }
    }

    companion object {
        @JvmStatic
        fun with(activity: Activity): Builder {
            if (activity == null) {
                throw NullPointerException("activity can not be null .")
            }
            return Builder(activity)
        }
    }

    init {
        mErrorLayoutId = builder.mErrorLayoutId
        val parentLayout = FrameLayout(activity)
        // 设置WebView
        webView = builder.mCustomWebView
        parentLayout.addView(
            mWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        // 进度条布局
        handleWebProgress(builder, parentLayout)
        if (builder.mIndex != -1) {
            builder.mWebContainer!!.addView(parentLayout, builder.mIndex, builder.mLayoutParams)
        } else {
            builder.mWebContainer!!.addView(parentLayout, builder.mLayoutParams)
        }
        // 配置
        handleSetting()
        // 视频、照片、进度条
        mWebChromeClient = MulWebChromeClient(activity, this)
        mWebChromeClient.setOnByWebChromeCallback(builder.mOnTitleProgressCallback)
        mWebView!!.webChromeClient = mWebChromeClient

        // 错误页面、页面结束、处理DeepLink
        val mByWebViewClient = MulWebViewClient(activity, this)
        mByWebViewClient.setOnByWebClientCallback(builder.mOnByWebClientCallback)
        mWebView!!.webViewClient = mByWebViewClient
        handleJsInterface(builder)
    }
}