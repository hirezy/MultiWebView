package com.example.hirezy.webviewstudy.ui
import com.hirezy.web.ByWebView.Companion.with
import com.hirezy.web.ByWebTools.handleThirdApp
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import com.example.hirezy.webviewstudy.R
import com.example.hirezy.webviewstudy.utils.WebTools
import com.example.hirezy.webviewstudy.utils.StatusBarUtil
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.example.hirezy.webviewstudy.config.MyJavascriptInterface
import android.content.Intent
import com.example.hirezy.webviewstudy.MainActivity
import com.hirezy.web.ByWebView
import android.widget.LinearLayout
import com.hirezy.web.OnTitleProgressCallback
import com.hirezy.web.OnByWebClientCallback
import android.graphics.Bitmap
import android.content.Context
import android.net.http.SslError
import android.util.Log
import android.view.*
import android.webkit.SslErrorHandler
import android.webkit.WebView
import androidx.appcompat.widget.Toolbar
import java.lang.Exception

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
 * 被作为第三方浏览器打开
 *
 * @author hirezy
 */
class ByWebViewActivity : AppCompatActivity() {
    // 网页链接
    private var mState = 0
    private var mUrl: String? = null
    private var mTitle: String? = null
    private var webView: WebView? = null
    private var byWebView: ByWebView? = null
    private var tvGunTitle: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_by_webview)
        intentData
        initTitle()
        getDataFromBrowser(intent)
    }

    private val intentData: Unit
        private get() {
            mUrl = intent.getStringExtra("url")
            mTitle = intent.getStringExtra("title")
            mState = intent.getIntExtra("state", 0)
        }

    private fun initTitle() {
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimary), 0)
        initToolBar()
        val container = findViewById<LinearLayout>(R.id.ll_container)

        //file:///android_asset/pdf/web/viewer.html?file=$it
        if (mUrl!!.endsWith(".pdf",true)){
            mUrl="file:///android_asset/pdf/web/viewer.html?file=$mUrl"
        }
        byWebView = with(this)
            .setWebParent(container, LinearLayout.LayoutParams(-1, -1))
            .useWebProgress(ContextCompat.getColor(this, R.color.coloRed))
            .setOnTitleProgressCallback(onTitleProgressCallback)
            .setOnByWebClientCallback(onByWebClientCallback)
            .addJavascriptInterface("injectedObject", MyJavascriptInterface(this))
            .loadUrl(mUrl!!)
        webView = byWebView!!.webView
    }

    private fun initToolBar() {
        // 可滚动的title 使用简单 没有渐变效果，文字两旁有阴影
        val mTitleToolBar = findViewById<Toolbar>(R.id.title_tool_bar)
        tvGunTitle = findViewById(R.id.tv_gun_title)
        setSupportActionBar(mTitleToolBar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
        mTitleToolBar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.actionbar_more)
        tvGunTitle?.postDelayed(Runnable { tvGunTitle?.isSelected = true }, 1900)
        tvGunTitle?.text = mTitle
    }

    private val onTitleProgressCallback: OnTitleProgressCallback =
        object : OnTitleProgressCallback() {
            override fun onReceivedTitle(title: String?) {
                Log.e("---title", title)
                tvGunTitle?.text = title
            }
        }
    private val onByWebClientCallback: OnByWebClientCallback = object : OnByWebClientCallback() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Log.e("---onPageStarted", url)
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ): Boolean {
            // 如果自己处理，需要返回true
            return super.onReceivedSslError(view, handler, error)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            // 网页加载完成后的回调
            if (mState == 1) {
                loadImageClickJs()
                loadTextClickJs()
                loadWebsiteSourceCodeJs()
            } else if (mState == 2) {
                loadCallJs()
            }
        }

        override fun isOpenThirdApp(url: String): Boolean {
            // 处理三方链接
            Log.e("---url", url)
            return handleThirdApp(this@ByWebViewActivity, url)
        }
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
                WebTools.share(this@ByWebViewActivity, shareText)
            }
            R.id.actionbar_cope -> {
                WebTools.copy(webView!!.url)
                Toast.makeText(this, "复制成功", Toast.LENGTH_LONG).show()
            }
            R.id.actionbar_open -> WebTools.openLink(this@ByWebViewActivity, webView!!.url)
            R.id.actionbar_webview_refresh -> byWebView!!.reload()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 前端注入JS：
     * 这段js函数的功能就是，遍历所有的img节点，并添加onclick函数，函数的功能是在图片点击的时候调用本地java接口并传递url过去
     */
    private fun loadImageClickJs() {
        byWebView!!.loadJsHolder.loadJs(
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
    private fun loadTextClickJs() {
        byWebView!!.loadJsHolder.loadJs(
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
    private fun loadCallJs() {
        // 无参数调用
        byWebView!!.loadJsHolder.quickCallJs("javacalljs")
        // 传递参数调用
        byWebView!!.loadJsHolder.quickCallJs("javacalljswithargs", "android传入到网页里的数据，有参")
    }

    /**
     * get website source code
     * 获取网页源码
     */
    private fun loadWebsiteSourceCodeJs() {
        byWebView!!.loadJsHolder.loadJs("javascript:window.injectedObject.showSource(document.getElementsByTagName('html')[0].innerHTML);")
    }

    /**
     * 上传图片之后的回调
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        byWebView!!.handleFileChooser(requestCode, resultCode, intent)
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
                byWebView!!.loadUrl(url)
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (byWebView!!.handleKeyEvent(keyCode, event)) {
            true
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                handleFinish()
            }
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        byWebView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        byWebView!!.onResume()
    }

    override fun onDestroy() {
        byWebView!!.onDestroy()
        super.onDestroy()
    }

    companion object {
        /**
         * 打开网页:
         *
         * @param mContext 上下文
         * @param url      要加载的网页url
         * @param title    标题
         * @param state    类型
         */
        fun loadUrl(mContext: Context, url: String?, title: String?, state: Int) {
            val intent = Intent(mContext, ByWebViewActivity::class.java)
            intent.putExtra("url", url)
            intent.putExtra("state", state)
            intent.putExtra("title", title ?: "加载中...")
            mContext.startActivity(intent)
        }
    }
}