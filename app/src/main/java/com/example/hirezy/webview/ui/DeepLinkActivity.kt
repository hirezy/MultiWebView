package com.example.hirezy.webview.ui
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import com.example.hirezy.webview.R
import android.util.Log
import android.view.*
import java.lang.Exception

/**
 * 测试DeepLink打开页面
 */
class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_link)
        val textView = findViewById<View>(R.id.tv_deeplink) as TextView
        getDataFromBrowser(textView)
    }

    /**
     * 从deep link中获取数据
     * 'will://share/传过来的数据'
     */
    private fun getDataFromBrowser(textView: TextView) {
        val data = intent.data
        try {
            val scheme = data!!.scheme
            val host = data.host
            val params = data.pathSegments
            // 从网页传过来的数据
            val testId = params[0]
            val text = "Scheme: $scheme\nhost: $host\nparams: $testId"
            Log.e("ScrollingActivity", text)
            textView.text = text
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}