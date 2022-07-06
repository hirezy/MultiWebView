package com.example.hirezy.webviewstudy.config

import android.R
import android.widget.FrameLayout
import android.view.MotionEvent
import android.content.Context

/**
 * Created by hirezy on 2019/11/17.
 */
class FullscreenHolder(ctx: Context) : FrameLayout(ctx) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    init {
        setBackgroundColor(ctx.resources.getColor(R.color.black))
    }
}