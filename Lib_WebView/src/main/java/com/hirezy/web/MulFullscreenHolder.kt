package com.hirezy.web

import android.widget.FrameLayout
import android.view.MotionEvent
import android.content.Context

class MulFullscreenHolder(context: Context) : FrameLayout(context) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    init {
        setBackgroundColor(context.resources.getColor(android.R.color.black))
    }
}