package com.hirezy.web
import kotlin.jvm.JvmOverloads
import android.os.Build
import androidx.annotation.RequiresApi
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import android.webkit.ValueCallback
import android.webkit.WebView
import java.lang.StringBuilder

/**
 * Created by hirezy on 2020/7/4.
 */
class MulLoadJsHolder internal constructor(private val mWebView: WebView?) {
    @JvmOverloads
    fun loadJs(js: String, callback: ValueCallback<String>? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJs(js, callback)
        } else {
            mWebView!!.loadUrl(js)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun evaluateJs(js: String, callback: ValueCallback<String>?) {
        mWebView!!.evaluateJavascript(js) { value -> callback?.onReceiveValue(value) }
    }

    private fun quickCallJs(method: String?, callback: ValueCallback<String>, vararg params: String?) {
        val sb = StringBuilder()
        sb.append("javascript:").append(method)
        if (params.isEmpty()) {
            sb.append("()")
        } else {
            sb.append("(").append(concat(*params as Array<out String>)).append(")")
        }
        loadJs(sb.toString(), callback)
    }

    private fun concat(vararg params: String): String {
        val mStringBuilder = StringBuilder()
        for (i in params.indices) {
            val param = params[i]
            if (!isJson(param)) {
                mStringBuilder.append("\"").append(param).append("\"")
            } else {
                mStringBuilder.append(param)
            }
            if (i != params.size - 1) {
                mStringBuilder.append(" , ")
            }
        }
        return mStringBuilder.toString()
    }

    fun quickCallJs(method: String?, vararg params: String?) {
        this.quickCallJs(method, null, *params)
    }

    fun quickCallJs(method: String?) {
        this.quickCallJs(method, *(null as Array<String?>))
    }

    companion object {
        fun isJson(target: String): Boolean {
            if (TextUtils.isEmpty(target)) {
                return false
            }
            var tag = false
            tag = try {
                if (target.startsWith("[")) {
                    JSONArray(target)
                } else {
                    JSONObject(target)
                }
                true
            } catch (ignore: JSONException) {
//            ignore.printStackTrace();
                false
            }
            return tag
        }
    }
}