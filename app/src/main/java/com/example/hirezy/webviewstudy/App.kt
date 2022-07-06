package com.example.hirezy.webviewstudy
import android.app.Application
import com.squareup.leakcanary.LeakCanary
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.smtt.sdk.QbSdk
import android.util.Log


/**
 * @author hirezy
 * @data 2018/2/2
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
        instance = this
        initX5()
    }

    private fun initX5() {
        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
        val cb: PreInitCallback = object : PreInitCallback {
            override fun onViewInitFinished(arg0: Boolean) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                Log.d("app", " onViewInitFinished is $arg0")
            }

            override fun onCoreInitFinished() {}
        }
        //x5内核初始化接口
        QbSdk.initX5Environment(applicationContext, cb)
    }

    companion object {
        var instance: App? = null
            private set
    }
}