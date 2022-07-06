package com.example.hirezy.webviewstudy.utils
import android.net.ConnectivityManager
import android.content.Context
import java.lang.Exception

/**
 * 用于判断是不是联网状态
 */
object CheckNetwork {
    /**
     * 判断网络是否连通
     */
    fun isNetworkConnected(context: Context?): Boolean {
        return try {
            if (context != null) {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val info = cm.activeNetworkInfo
                info != null && info.isConnected
            } else {
                /**如果context为空，就返回false，表示网络未连接 */
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isWifiConnected(context: Context?): Boolean {
        return if (context != null) {
            val cm =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = cm.activeNetworkInfo
            info != null && info.type == ConnectivityManager.TYPE_WIFI
        } else {
            /**如果context为null就表示为未连接 */
            false
        }
    }
}