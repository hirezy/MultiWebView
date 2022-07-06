package com.hirezy.web

import android.widget.FrameLayout
import android.view.MotionEvent
import kotlin.jvm.JvmOverloads
import android.os.Build
import androidx.annotation.RequiresApi
import com.hirezy.web.ByLoadJsHolder
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import android.app.Activity
import com.hirezy.web.ByWebView
import com.hirezy.web.ByFullscreenHolder
import com.hirezy.web.OnTitleProgressCallback
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import com.hirezy.web.R
import android.content.Intent
import com.hirezy.web.ByWebChromeClient
import android.webkit.PermissionRequest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import com.hirezy.web.ByWebTools
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.ViewGroup
import com.hirezy.web.OnByWebClientCallback
import androidx.annotation.LayoutRes
import com.hirezy.web.ByWebViewClient
import android.content.DialogInterface
import android.graphics.Shader
import android.view.View.MeasureSpec
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.view.animation.DecelerateInterpolator
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.AnimatorListenerAdapter
import android.content.Context

class ByFullscreenHolder(context: Context) : FrameLayout(context) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    init {
        setBackgroundColor(context.resources.getColor(android.R.color.black))
    }
}