package com.example.hirezy.webviewstudy
import com.hirezy.web.MulWebTools.getUrl
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import android.text.TextUtils
import com.example.hirezy.webviewstudy.utils.StatusBarUtil
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.content.Intent
import com.example.hirezy.webviewstudy.ui.MulWebViewActivity
import com.example.hirezy.webviewstudy.ui.CoordinatorWebActivity
import android.content.Context
import com.example.hirezy.webviewstudy.tencentx5.X5WebViewActivity
import android.view.*
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.TextView.OnEditorActionListener
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var etSearch: AutoCompleteTextView? = null
    private var rbSystem: RadioButton? = null
    private var state = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimary), 0)
        initView()
        isLaunch = true
    }

    private fun initView() {
        findViewById<View>(R.id.bt_deeplink).setOnClickListener(this)
        findViewById<View>(R.id.bt_openUrl).setOnClickListener(this)
        findViewById<View>(R.id.bt_baidu).setOnClickListener(this)
        findViewById<View>(R.id.bt_movie).setOnClickListener(this)
        findViewById<View>(R.id.bt_upload_photo).setOnClickListener(this)
        findViewById<View>(R.id.bt_call).setOnClickListener(this)
        findViewById<View>(R.id.bt_java_js).setOnClickListener(this)
        findViewById<View>(R.id.bt_toolbar).setOnClickListener(this)
        findViewById<View>(R.id.bt_open_pdf).setOnClickListener(this)
        rbSystem = findViewById(R.id.rb_system)
        etSearch = findViewById(R.id.et_search)
        rbSystem?.isChecked = true
        val tvVersion = findViewById<TextView>(R.id.tv_version)
        tvVersion.text = String.format("❤版本：v%s", BuildConfig.VERSION_NAME)
        tvVersion.setOnClickListener(this)
        /** 处理键盘搜索键  */
        etSearch?.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                openUrl()
            }
            false
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.bt_open_pdf -> {
                val baiDuUrl = "https://view.xdocin.com/demo/view/view.pdf"
                loadUrl(baiDuUrl, getString(R.string.text_open_pdf))
            }
            R.id.bt_openUrl -> openUrl()
            R.id.bt_baidu -> {
                state = 0
                val baiDuUrl = "http://www.baidu.com"
                loadUrl(baiDuUrl, getString(R.string.text_baidu))
            }
            R.id.bt_movie -> {
                state = 0
                val movieUrl =
                    "https://sv.baidu.com/videoui/page/videoland?context=%7B%22nid%22%3A%22sv_5861863042579737844%22%7D&pd=feedtab_h5"
                loadUrl(movieUrl, getString(R.string.text_movie))
            }
            R.id.bt_upload_photo -> {
                state = 0
                val uploadUrl = "file:///android_asset/upload_photo.html"
                loadUrl(uploadUrl, getString(R.string.text_upload_photo))
            }
            R.id.bt_call -> {
                state = 1
                val callUrl = "file:///android_asset/callsms.html"
                loadUrl(callUrl, getString(R.string.text_js))
            }
            R.id.bt_java_js -> {
                state = 2
                val javaJs = "file:///android_asset/java_js.html"
                loadUrl(javaJs, getString(R.string.js_android))
            }
            R.id.bt_deeplink -> {
                state = 0
                val deepLinkUrl = "file:///android_asset/deeplink.html"
                loadUrl(deepLinkUrl, getString(R.string.deeplink))
            }
            R.id.bt_toolbar -> CoordinatorWebActivity.Companion.loadUrl(
                this,
                "http://www.baidu.com",
                "百度一下",
                0
            )
            R.id.tv_version -> {
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle("感谢")
                builder.setMessage("开源不易，给作者一个star好吗？😊")
                builder.setNegativeButton("已给") { dialog, which ->
                    Toast.makeText(
                        this@MainActivity,
                        "感谢老铁~",
                        Toast.LENGTH_LONG
                    ).show()
                }
                builder.setPositiveButton("去star") { dialog, which ->
                    state = 0
                    loadUrl("https://github.com/hirezy/MultiWebView", "MultiWebView")
                }
                builder.show()
            }
            else -> {
            }
        }
    }

    /**
     * 打开网页
     */
    private fun openUrl() {
        state = 0
        val url = getUrl(etSearch!!.text.toString().trim { it <= ' ' })
        loadUrl(
            if (!TextUtils.isEmpty(url)) url else "https://github.com/hirezy/MultiWebView",
            "MulWebView"
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionbar_update -> {
                state = 0
                loadUrl(
                    "https://github.com/hirezy/MultiWebView/blob/master/art/app.apk",
                    "MulWebView.apk"
                )
            }
            R.id.actionbar_about -> {
                state = 0
                loadUrl("https://github.com/hirezy/MultiWebView", "MulWebView")
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadUrl(mUrl: String, mTitle: String) {
        if (rbSystem!!.isChecked) {
//            WebViewActivity.loadUrl(this, mUrl, mTitle);
            MulWebViewActivity.loadUrl(this, mUrl, mTitle, state)
        } else {
            X5WebViewActivity.loadUrl(this, mUrl, mTitle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isLaunch = false
    }

    companion object {
        // 是否开启了主页，没有开启则会返回主页
        var isLaunch = false
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}