package com.hirezy.web

import android.animation.*
import android.widget.FrameLayout
import kotlin.jvm.JvmOverloads
import android.view.animation.LinearInterpolator
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.util.AttributeSet

/**
 * WebView进度条
 * 1. progress同时返回两次100时进度条出现两次
 * 2. 当一条进度没跑完，又点击其他链接开始第二次进度时，第二次进度不出现
 * 3. 修改消失动画时长，使其消失时看到可以进度跑完
 * 4. [2019.9.29] 修复当第一次进度返回 0 或超过 10，出现不显示进度条的问题
 * 5. 能显示渐变色
 * 6. 进度在95-100时再次开始进度条透明度问题
 *
 * @author hirezy
 */
class WebProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    /**
     * 进度条颜色
     */
    private var mColor = 0

    /**
     * 进度条的画笔
     */
    private var mPaint: Paint? = null

    /**
     * 进度条动画
     */
    private var mAnimator: Animator? = null

    /**
     * 控件的宽度
     */
    private var mTargetWidth = 0

    /**
     * 控件的高度
     */
    private var mTargetHeight = 0

    /**
     * 标志当前进度条的状态
     */
    private var TAG = 0

    /**
     * 第一次过来进度show，后面就是setProgress
     */
    private var isShow = false
    private var mCurrentProgress = 0f
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        mPaint = Paint()
        mColor = Color.parseColor(WEB_PROGRESS_COLOR)
        mPaint!!.isAntiAlias = true
        mPaint!!.color = mColor
        mPaint!!.isDither = true
        mPaint!!.strokeCap = Paint.Cap.SQUARE
        mTargetWidth = context.resources.displayMetrics.widthPixels
        mTargetHeight = dip2px(WEB_PROGRESS_DEFAULT_HEIGHT.toFloat())
    }

    /**
     * 设置单色进度条
     */
    fun setColor(color: Int) {
        mColor = color
        mPaint!!.color = color
    }

    fun setColor(color: String?) {
        this.setColor(Color.parseColor(color))
    }

    fun setColor(startColor: Int, endColor: Int) {
        val linearGradient: LinearGradient = LinearGradient(
            0f,
            0f,
            mTargetWidth.toFloat(),
            mTargetHeight.toFloat(),
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )
        mPaint!!.shader = linearGradient
    }

    /**
     * 设置渐变色进度条
     *
     * @param startColor 开始颜色
     * @param endColor   结束颜色
     */
    fun setColor(startColor: String?, endColor: String?) {
        this.setColor(Color.parseColor(startColor), Color.parseColor(endColor))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        var w = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        var h = MeasureSpec.getSize(heightMeasureSpec)
        if (wMode == MeasureSpec.AT_MOST) {
            w = Math.min(w, context.resources.displayMetrics.widthPixels)
        }
        if (hMode == MeasureSpec.AT_MOST) {
            h = mTargetHeight
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {}
    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawRect(
            0f,
            0f,
            mCurrentProgress / 100 * this.width.toFloat(),
            this.height.toFloat(),
            mPaint!!
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mTargetWidth = measuredWidth
        val screenWidth = context.resources.displayMetrics.widthPixels
        if (mTargetWidth >= screenWidth) {
            CURRENT_MAX_DECELERATE_SPEED_DURATION = MAX_DECELERATE_SPEED_DURATION
            CURRENT_MAX_UNIFORM_SPEED_DURATION = MAX_UNIFORM_SPEED_DURATION
        } else {
            //取比值
            val rate = mTargetWidth / screenWidth.toFloat()
            CURRENT_MAX_UNIFORM_SPEED_DURATION = (MAX_UNIFORM_SPEED_DURATION * rate).toInt()
            CURRENT_MAX_DECELERATE_SPEED_DURATION = (MAX_DECELERATE_SPEED_DURATION * rate).toInt()
        }
    }

    private fun setFinish() {
        isShow = false
        TAG = FINISH
    }

    private fun startAnim(isFinished: Boolean) {
        val v: Float = if (isFinished) 100F else 95.toFloat()
        if (mAnimator != null && mAnimator!!.isStarted) {
            mAnimator!!.cancel()
        }
        mCurrentProgress = if (mCurrentProgress == 0f) 0.00000001f else mCurrentProgress
        // 可能由于透明度造成突然出现的问题
        alpha = 1f
        if (!isFinished) {
            val mAnimator = ValueAnimator.ofFloat(mCurrentProgress, v)
            val residue = 1f - mCurrentProgress / 100 - 0.05f
            mAnimator.interpolator = LinearInterpolator()
            mAnimator.duration = (residue * CURRENT_MAX_UNIFORM_SPEED_DURATION).toLong()
            mAnimator.addUpdateListener(mAnimatorUpdateListener)
            mAnimator.start()
            this.mAnimator = mAnimator
        } else {
            var segment95Animator: ValueAnimator? = null
            if (mCurrentProgress < 95) {
                segment95Animator = ValueAnimator.ofFloat(mCurrentProgress, 95f)
                val residue = 1f - mCurrentProgress / 100f - 0.05f
                segment95Animator.interpolator = LinearInterpolator()
                segment95Animator.duration =
                    (residue * CURRENT_MAX_DECELERATE_SPEED_DURATION).toLong()
                segment95Animator.interpolator = DecelerateInterpolator()
                segment95Animator.addUpdateListener(mAnimatorUpdateListener)
            }
            val mObjectAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
            mObjectAnimator.duration = DO_END_ALPHA_DURATION.toLong()
            val mValueAnimatorEnd = ValueAnimator.ofFloat(95f, 100f)
            mValueAnimatorEnd.duration =
                DO_END_PROGRESS_DURATION.toLong()
            mValueAnimatorEnd.addUpdateListener(mAnimatorUpdateListener)
            var mAnimatorSet = AnimatorSet()
            mAnimatorSet.playTogether(mObjectAnimator, mValueAnimatorEnd)
            if (segment95Animator != null) {
                val mAnimatorSet1 = AnimatorSet()
                mAnimatorSet1.play(mAnimatorSet).after(segment95Animator)
                mAnimatorSet = mAnimatorSet1
            }
            mAnimatorSet.addListener(mAnimatorListenerAdapter)
            mAnimatorSet.start()
            mAnimator = mAnimatorSet
        }
        TAG = STARTED
    }

    private val mAnimatorUpdateListener = AnimatorUpdateListener { animation ->
        val t = animation.animatedValue as Float
        mCurrentProgress = t
        this@WebProgress.invalidate()
    }
    private val mAnimatorListenerAdapter: AnimatorListenerAdapter =
        object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                doEnd()
            }
        }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        /**
         * animator cause leak , if not cancel;
         */
        if (mAnimator != null && mAnimator!!.isStarted) {
            mAnimator!!.cancel()
            mAnimator = null
        }
    }

    private fun doEnd() {
        if (TAG == FINISH && mCurrentProgress == 100f) {
            visibility = GONE
            mCurrentProgress = 0f
            this.alpha = 1f
        }
        TAG = UN_START
    }

    fun reset() {
        mCurrentProgress = 0f
        if (mAnimator != null && mAnimator!!.isStarted) {
            mAnimator!!.cancel()
        }
    }

    fun setProgress(newProgress: Int) {
        setProgress(java.lang.Float.valueOf(newProgress.toFloat()))
    }

    fun offerLayoutParams(): LayoutParams {
        return LayoutParams(mTargetWidth, mTargetHeight)
    }

    private fun dip2px(dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun setHeight(heightDp: Int): WebProgress {
        mTargetHeight = dip2px(heightDp.toFloat())
        return this
    }

    fun setProgress(progress: Float) {
        // fix 同时返回两个 100，产生两次进度条的问题；
        if (TAG == UN_START && progress == 100f) {
            visibility = GONE
            return
        }
        if (visibility == GONE) {
            visibility = VISIBLE
        }
        if (progress < 95) {
            return
        }
        if (TAG != FINISH) {
            startAnim(true)
        }
    }

    /**
     * 显示进度条
     */
    fun show() {
        isShow = true
        visibility = VISIBLE
        mCurrentProgress = 0f
        startAnim(false)
    }

    /**
     * 进度完成后消失
     */
    fun hide() {
        setWebProgress(100)
    }

    /**
     * 为单独处理WebView进度条
     */
    fun setWebProgress(newProgress: Int) {
        if (newProgress >= 0 && newProgress < 95) {
            if (!isShow) {
                show()
            } else {
                setProgress(newProgress)
            }
        } else {
            setProgress(newProgress)
            setFinish()
        }
    }

    companion object {
        /**
         * 默认匀速动画最大的时长
         */
        const val MAX_UNIFORM_SPEED_DURATION = 8 * 1000

        /**
         * 默认加速后减速动画最大时长
         */
        const val MAX_DECELERATE_SPEED_DURATION = 450

        /**
         * 95f-100f时，透明度1f-0f时长
         */
        const val DO_END_ALPHA_DURATION = 630

        /**
         * 95f - 100f动画时长
         */
        const val DO_END_PROGRESS_DURATION = 500

        /**
         * 当前匀速动画最大的时长
         */
        private var CURRENT_MAX_UNIFORM_SPEED_DURATION = MAX_UNIFORM_SPEED_DURATION

        /**
         * 当前加速后减速动画最大时长
         */
        private var CURRENT_MAX_DECELERATE_SPEED_DURATION = MAX_DECELERATE_SPEED_DURATION

        /**
         * 默认的高度(dp)
         */
        var WEB_PROGRESS_DEFAULT_HEIGHT = 3

        /**
         * 进度条颜色默认
         */
        var WEB_PROGRESS_COLOR = "#32B848"
        const val UN_START = 0
        const val STARTED = 1
        const val FINISH = 2
    }

    init {
        init(context, attrs, defStyleAttr)
    }
}