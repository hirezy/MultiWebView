package com.example.hirezy.webview.utils
import com.example.hirezy.webview.R
import android.text.TextUtils
import android.os.Build
import android.widget.Toast
import com.example.hirezy.webview.App
import android.content.pm.PackageManager
import android.app.Activity
import android.content.*
import android.net.Uri
import android.util.Log
import com.example.hirezy.webview.BuildConfig
import java.lang.Exception

/**
 * Created by hirezy on 2017/2/13.
 */
object WebTools {
    /**
     * 将 Android5.0以下手机不能直接打开mp4后缀的链接
     *
     * @param url 视频链接
     */
    fun getVideoHtmlBody(title: String?, url: String?): String {
        return "<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width\">" +
                "<title>" + title + "</title>" +
                "<style type=\"text/css\" abt=\"234\"></style>" +
                "</head>" +
                "<body>" +
                "<video controls=\"\" autoplay=\"\" name=\"media\" style=\"display:block;width:100%;position:absolute;left:0;top:20%;\">" +
                "<source src=\"" + url + "\" type=\"video/mp4\">" +
                "</video>" +
                "</body>" +
                "</html>"
    }

    /**
     * 实现文本复制功能
     *
     * @param content 复制的文本
     */
    fun copy(content: String?) {
        if (!TextUtils.isEmpty(content)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                val clipboard = App.Companion.instance?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.text = content
            } else {
                val clipboard = App.Companion.instance?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(content, content)
                clipboard.setPrimaryClip(clip)
            }
        }
    }

    /**
     * 使用浏览器打开链接
     */
    fun openLink(context: Context, content: String) {
        if (!TextUtils.isEmpty(content) && content.startsWith("http")) {
            val issuesUrl = Uri.parse(content)
            val intent = Intent(Intent.ACTION_VIEW, issuesUrl)
            context.startActivity(intent)
        }
    }

    /**
     * 分享
     */
    fun share(context: Context, extraText: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.action_share))
        intent.putExtra(Intent.EXTRA_TEXT, extraText)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.action_share)
            )
        )
    }

    /**
     * 通过包名找应用,不需要权限
     */
    fun hasPackage(context: Context?, packageName: String?): Boolean {
        return if (null == context || TextUtils.isEmpty(packageName)) {
            false
        } else try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_GIDS)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // 抛出找不到的异常，说明该程序已经被卸载
            false
        }
    }

    /**
     * 默认处理流程：网页里可能唤起其他的app
     */
    fun handleThirdApp(activity: Activity, backUrl: String?): Boolean {
        /**http开头直接跳过 */
        if (backUrl!!.startsWith("http")) {
            // 可能有提示下载Apk文件
            if (backUrl.contains(".apk")) {
                startActivity(activity, backUrl)
                return true
            }
            return false
        }
        if (backUrl.contains("alipays")) {
            // 网页跳支付宝支付
            if (hasPackage(activity, "com.eg.android.AlipayGphone")) {
                startActivity(activity, backUrl)
            }
        } else if (backUrl.contains("weixin://wap/pay")) {
            // 微信支付
            if (hasPackage(activity, "com.tencent.mm")) {
                startActivity(activity, backUrl)
            }
        } else {

            // 会唤起手机里有的App，如果不想被唤起，复制出来然后添加屏蔽即可
            var isJump = true
            if (backUrl.contains("tbopen:") // 淘宝
                || backUrl.contains("openapp.jdmobile:") // 京东
                || backUrl.contains("jdmobile:") //京东
                || backUrl.contains("zhihu:") // 知乎
                || backUrl.contains("vipshop:") //
                || backUrl.contains("youku:") //优酷
                || backUrl.contains("uclink:") // UC
                || backUrl.contains("ucbrowser:") // UC
                || backUrl.contains("newsapp:") //
                || backUrl.contains("sinaweibo:") // 新浪微博
                || backUrl.contains("suning:") //
                || backUrl.contains("pinduoduo:") // 拼多多
                || backUrl.contains("qtt:") //
                || backUrl.contains("baiduboxapp:") // 百度
                || backUrl.contains("baiduhaokan:") // 百度看看
            ) {
                isJump = false
            }
            if (isJump) {
                startActivity(activity, backUrl)
            }
        }
        return true
    }

    private fun startActivity(context: Activity, url: String?) {
        try {

            // 用于DeepLink测试
            if (url!!.startsWith("will://")) {
                val uri = Uri.parse(url)
                Log.e(
                    "---------scheme",
                    uri.scheme + "；host: " + uri.host + "；Id: " + uri.pathSegments[0]
                )
            }
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun showToast(content: String?) {
        if (!TextUtils.isEmpty(content)) {
            Toast.makeText(App.Companion.instance, content, Toast.LENGTH_SHORT).show()
        }
    }
}