package com.example.hirezy.webviewstudy.tencentx5
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import android.widget.TextView
import android.os.Bundle
import com.example.hirezy.webviewstudy.R
import android.os.Build
import com.example.hirezy.webviewstudy.utils.WebTools
import com.example.hirezy.webviewstudy.utils.StatusBarUtil
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.annotation.SuppressLint
import com.example.hirezy.webviewstudy.utils.CheckNetwork
import android.content.pm.ActivityInfo
import android.content.Intent
import com.example.hirezy.webviewstudy.MainActivity
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.example.hirezy.webviewstudy.config.*
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import java.lang.Exception

/**
 * 使用 tencent x5 内核处理网页
 * 1、放入对应jar
 * 2、application 初始化
 * 3、gradle ndk配置
 * 4、jniLibs 配置
 * 5、添加权限 READ_PHONE_STATE
 * 6、getWindow().setFormat(PixelFormat.TRANSLUCENT);
 * @author hirezy
 */
class X5WebViewActivity : AppCompatActivity(), IX5WebPageView {
    // 进度条
    private var mProgressBar: WebProgress? = null
    private var webView: WebView? = null

    // 全屏时视频加载view
    override var videoFullView: FrameLayout? = null
        private set

    // 加载视频相关
    private var mWebChromeClient: MyX5WebChromeClient? = null

    // 网页链接
    private var mUrl: String? = null
    private var mTitleToolBar: Toolbar? = null

    // 可滚动的title 使用简单 没有渐变效果，文字两旁有阴影
    private var tvGunTitle: TextView? = null
    private var mTitle: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_x5)
        window.setFormat(PixelFormat.TRANSLUCENT)
        intentData
        initTitle()
        initWebView()
        webView!!.loadUrl(mUrl)
        getDataFromBrowser(intent)
    }

    private val intentData: Unit
        private get() {
            mUrl = intent.getStringExtra("mUrl")
            mTitle = intent.getStringExtra("mTitle")
        }

    private fun initTitle() {
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimary), 0)
        mProgressBar = findViewById(R.id.pb_progress)
        mProgressBar?.setColor(
            ContextCompat.getColor(this, R.color.colorPink),
            ContextCompat.getColor(this, R.color.color_FF4081)
        )
        mProgressBar?.show()
        webView = findViewById(R.id.webview_detail)
        mTitleToolBar = findViewById(R.id.title_tool_bar)
        tvGunTitle = findViewById(R.id.tv_gun_title)
        initToolBar()
    }

    private fun initToolBar() {
        setSupportActionBar(mTitleToolBar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
        mTitleToolBar!!.overflowIcon = ContextCompat.getDrawable(this, R.drawable.actionbar_more)
        tvGunTitle!!.postDelayed({ tvGunTitle!!.isSelected = true }, 1900)
        setTitle(mTitle)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_webview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> handleFinish()
            R.id.actionbar_share -> {
                val shareText = webView!!.title + webView!!.url
                WebTools.share(this, shareText)
            }
            R.id.actionbar_cope -> {
                WebTools.copy(webView!!.url)
                Toast.makeText(this, "复制成功", Toast.LENGTH_LONG).show()
            }
            R.id.actionbar_open -> WebTools.openLink(this, webView!!.url)
            R.id.actionbar_webview_refresh -> webView!!.reload()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun initWebView() {
        val ws = webView!!.settings
        // 网页内容的宽度自适应屏幕
        ws.loadWithOverviewMode = true
        ws.useWideViewPort = true
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
        // 设置此属性，可任意比例缩放。
        ws.useWideViewPort = true
        // 告诉WebView启用JavaScript执行。默认的是false。
        ws.javaScriptEnabled = true
        //  页面加载好以后，再放开图片
        ws.blockNetworkImage = false
        // 使用localStorage则必须打开
        ws.domStorageEnabled = true
        // 排版适应屏幕
        ws.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        // WebView是否新窗口打开(加了后可能打不开网页)
        ws.setSupportMultipleWindows(true)

        // webview从5.0开始默认不允许混合模式,https中不能加载http资源,需要设置开启。MIXED_CONTENT_ALWAYS_ALLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.mixedContentMode = WebSettings.LOAD_NORMAL
        }
        /** 设置字体默认缩放大小(改变网页字体大小,setTextSize  api14被弃用) */
        ws.textZoom = 100
        mWebChromeClient = MyX5WebChromeClient(this)
        webView!!.setWebChromeClient(mWebChromeClient)
        // 与js交互
        webView!!.addJavascriptInterface(MyJavascriptInterface(this), "injectedObject")
        webView!!.setWebViewClient(MyX5WebViewClient(this))
        webView!!.setOnLongClickListener { handleLongImage() }
    }

    override fun showWebView() {
        webView!!.visibility = View.VISIBLE
    }

    override fun hindWebView() {
        webView!!.visibility = View.INVISIBLE
    }

    override fun fullViewAddView(view: View?) {
        val decor = window.decorView as FrameLayout
        videoFullView = FullscreenHolder(this)
        videoFullView?.addView(view)
        decor.addView(videoFullView)
    }

    override fun showVideoFullView() {
        videoFullView!!.visibility = View.VISIBLE
    }

    override fun hindVideoFullView() {
        videoFullView!!.visibility = View.GONE
    }

    override fun startProgress(newProgress: Int) {
        mProgressBar!!.setWebProgress(newProgress)
    }

    fun setTitle(mTitle: String?) {
        tvGunTitle!!.text = mTitle
    }

    /**
     * android与js交互：
     * 前端注入js代码：不能加重复的节点，不然会覆盖
     * 前端调用js代码
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        if (!CheckNetwork.isNetworkConnected(this)) {
            mProgressBar!!.hide()
        }
        loadImageClickJS()
        loadTextClickJS()
        loadCallJS()
        loadWebsiteSourceCodeJS()
    }

    /**
     * 处理是否唤起三方app
     */
    override fun isOpenThirdApp(url: String?): Boolean {
        return WebTools.handleThirdApp(this, url)
    }

    /**
     * 前端注入JS：
     * 这段js函数的功能就是，遍历所有的img节点，并添加onclick函数，函数的功能是在图片点击的时候调用本地java接口并传递url过去
     */
    private fun loadImageClickJS() {
        loadJs(
            "javascript:(function(){" +
                    "var objs = document.getElementsByTagName(\"img\");" +
                    "for(var i=0;i<objs.length;i++)" +
                    "{" +
                    "objs[i].onclick=function(){window.injectedObject.imageClick(this.getAttribute(\"src\"));}" +
                    "}" +
                    "})()"
        )
    }

    /**
     * 前端注入JS：
     * 遍历所有的 * 节点,将节点里的属性传递过去(属性自定义,用于页面跳转)
     */
    private fun loadTextClickJS() {
        loadJs(
            "javascript:(function(){" +
                    "var objs =document.getElementsByTagName(\"li\");" +
                    "for(var i=0;i<objs.length;i++)" +
                    "{" +
                    "objs[i].onclick=function(){" +
                    "window.injectedObject.textClick(this.getAttribute(\"type\"),this.getAttribute(\"item_pk\"));}" +
                    "}" +
                    "})()"
        )
    }

    /**
     * 传应用内的数据给html，方便html处理
     */
    private fun loadCallJS() {
        // 无参数调用
        loadJs("javascript:javacalljs()")
        // 传递参数调用
        loadJs("javascript:javacalljswithargs('" + "android传入到网页里的数据，有参" + "')")
    }

    /**
     * get website source code
     * 获取网页源码
     */
    private fun loadWebsiteSourceCodeJS() {
        loadJs("javascript:window.injectedObject.showSource(document.getElementsByTagName('html')[0].innerHTML);")
    }

    /**
     * 4.4以上可用 evaluateJavascript 效率高
     */
    private fun loadJs(jsString: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView!!.evaluateJavascript(jsString, null)
        } else {
            webView!!.loadUrl(jsString)
        }
    }

    /**
     * 全屏时按返加键执行退出全屏方法
     */
    fun hideCustomView() {
        mWebChromeClient!!.onHideCustomView()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override val videoLoadingProgressView: View?
        get() = LayoutInflater.from(this).inflate(R.layout.video_loading_progress, null)

    override fun onReceivedTitle(view: WebView?, title: String?) {
        setTitle(title)
    }

    override fun startFileChooserForResult(intent: Intent?, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    /**
     * 上传图片之后的回调
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == MyWebChromeClient.FILECHOOSER_RESULTCODE) {
            mWebChromeClient!!.mUploadMessage(intent, resultCode)
        } else if (requestCode == MyWebChromeClient.FILECHOOSER_RESULTCODE_FOR_ANDROID_5) {
            mWebChromeClient!!.mUploadMessageForAndroid5(intent, resultCode)
        }
    }

    /**
     * 使用singleTask启动模式的Activity在系统中只会存在一个实例。
     * 如果这个实例已经存在，intent就会通过onNewIntent传递到这个Activity。
     * 否则新的Activity实例被创建。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        getDataFromBrowser(intent)
    }

    /**
     * 作为三方浏览器打开传过来的值
     * Scheme: https
     * host: www.jianshu.com
     * path: /p/1cbaf784c29c
     * url = scheme + "://" + host + path;
     */
    private fun getDataFromBrowser(intent: Intent) {
        val data = intent.data
        if (data != null) {
            try {
                val scheme = data.scheme
                val host = data.host
                val path = data.path
                val text = "Scheme: $scheme\nhost: $host\npath: $path"
                Log.e("data", text)
                val url = "$scheme://$host$path"
                webView!!.loadUrl(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 直接通过三方浏览器打开时，回退到首页
     */
    fun handleFinish() {
        supportFinishAfterTransition()
        if (!MainActivity.Companion.isLaunch) {
            MainActivity.Companion.start(this)
        }
    }

    /**
     * 长按图片事件处理
     */
    private fun handleLongImage(): Boolean {
        val hitTestResult = webView!!.hitTestResult
        // 如果是图片类型或者是带有图片链接的类型
        if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
            hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        ) {
            // 弹出保存图片的对话框
            AlertDialog.Builder(this)
                .setItems(arrayOf("查看大图", "保存图片到相册")) { dialog, which ->
                    val picUrl = hitTestResult.extra
                    //获取图片
                    Log.e("picUrl", picUrl)
                    when (which) {
                        0 -> {
                        }
                        1 -> {
                        }
                        else -> {
                        }
                    }
                }
                .show()
            return true
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //全屏播放退出全屏
            if (mWebChromeClient!!.inCustomView()) {
                hideCustomView()
                return true

                //返回网页上一页
            } else if (webView!!.canGoBack()) {
                webView!!.goBack()
                return true

                //退出网页
            } else {
                handleFinish()
            }
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        webView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView!!.onResume()
        // 支付宝网页版在打开文章详情之后,无法点击按钮下一步
        webView!!.resumeTimers()
        // 设置为横屏
        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onDestroy() {
        if (videoFullView != null) {
            videoFullView!!.removeAllViews()
        }
        if (webView != null) {
            val parent = webView!!.parent as ViewGroup
            parent?.removeView(webView)
            webView!!.removeAllViews()
            webView!!.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            webView!!.stopLoading()
            webView!!.setWebChromeClient(null)
            webView!!.setWebViewClient(null)
            webView!!.destroy()
            webView = null
        }
        super.onDestroy()
    }

    companion object {
        /**
         * 打开网页:
         *
         * @param mContext 上下文
         * @param mUrl     要加载的网页url
         * @param mTitle   标题
         */
        fun loadUrl(mContext: Context, mUrl: String?, mTitle: String?) {
            val intent = Intent(mContext, X5WebViewActivity::class.java)
            intent.putExtra("mUrl", mUrl)
            intent.putExtra("mTitle", mTitle ?: "加载中...")
            mContext.startActivity(intent)
        }
    }
}