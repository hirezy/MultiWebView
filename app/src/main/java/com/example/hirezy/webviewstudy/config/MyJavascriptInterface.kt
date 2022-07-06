package com.example.hirezy.webviewstudy.config
import android.webkit.JavascriptInterface
import com.example.hirezy.webviewstudy.utils.WebTools
import android.text.TextUtils
import android.content.Context
import android.util.Log

/**
 * Created by hirezy on 2019/11/17.
 * js通信接口
 */
class MyJavascriptInterface(private val context: Context) {
    /**
     * 前端代码嵌入js：
     * imageClick 名应和js函数方法名一致
     *
     * @param src 图片的链接
     */
    @JavascriptInterface
    fun imageClick(src: String?) {
        Log.e("imageClick", "----点击了图片")
        Log.e("---src", src)
        WebTools.showToast(src)
    }

    /**
     * 前端代码嵌入js
     * 遍历 * 节点
     *
     * @param type     * 节点下type属性的值
     * @param item_pk item_pk属性的值
     */
    @JavascriptInterface
    fun textClick(type: String, item_pk: String) {
        if (!TextUtils.isEmpty(type) && !TextUtils.isEmpty(item_pk)) {
            Log.e("textClick", "----点击了文字")
            Log.e("type", type)
            Log.e("item_pk", item_pk)
            WebTools.showToast("type: $type, item_pk:$item_pk")
        }
    }

    /**
     * 网页使用的js，方法无参数
     */
    @JavascriptInterface
    fun startFunction() {
        Log.e("startFunction", "----无参")
        WebTools.showToast("无参方法")
    }

    /**
     * 网页使用的js，方法有参数，且参数名为data
     *
     * @param data 网页js里的参数名
     */
    @JavascriptInterface
    fun startFunction(data: String) {
        Log.e("startFunction", "----有参方法: $data")
        WebTools.showToast("----有参方法: $data")
    }

    /**
     * 获取网页源代码
     */
    @JavascriptInterface
    fun showSource(html: String?) {
        Log.e("showSourceCode", html)
    }
}