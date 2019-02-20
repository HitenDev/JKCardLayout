package me.hiten.jkcardlayout.sample

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * 下拉手势布局，仿即刻探索页交互
 */
class PullDownLayout : ConstraintLayout {
    companion object {
        private const val DRAG_RATIO = 0.6f

        private const val ANIM_DURATION = 200

        private const val PARALLAX_RATIO = 1.1f
    }

    private var mRecyclerView: RecyclerView

    private var mTopMenu: View

    private var mMaskView : View

    private var mBottomMenu : View

    private var mTouchSlop: Int

    /**
     * 已经开始拖拽标志
     */
    private var mIsBeingDragged: Boolean = false

    /**
     * 不能进行拖拽标志
     */
    private var mIsUnableToDrag: Boolean = false


    private var mLastMotionX: Float = 0f
    private var mLastMotionY: Float = 0f
    private var mInitialMotionX: Float = 0f
    private var mInitialMotionY: Float = 0f

    /**
     * 多点触控下激活的手指Id
     */
    private var mActivePointerId = -1

    /**
     * 记录总共的dx
     */
    private var mTotalDx: Float = 0f

    /**
     * 记录总共的dy
     */
    private var mTotalDy: Float = 0f

    /**
     * 拖拽阻尼系数
     */
    private var mDragRatio = DRAG_RATIO

    /**
     * 动画执行时间
     */
    private var mDuration = ANIM_DURATION

    /**
     * 视觉差系数,等同于topMenu位移/mRecyclerView位移比例
     */
    private var mParallaxRatio = PARALLAX_RATIO

    /**
     * 菜单打开/关闭标志
     */
    private var mOpened = false

    private var mObjectAnimator: ObjectAnimator? = null

    /**
     * 最大TranslationY,一般取topMenu的高度作为参考值
     */
    private var mMaxTranslationY = 0


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.layout_card, this, true)
        mTopMenu = findViewById(R.id.layout_top_menu)
        mMaskView = findViewById(R.id.view_mask)
        mBottomMenu = findViewById(R.id.layout_bottom)
        mRecyclerView = findViewById(R.id.recycler_view)
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mMaskView.visibility = View.GONE
        mMaskView.setOnClickListener {
            closeMenu()
        }
        clipChildren = false
        clipToPadding = false
    }


    /**
     * 设置视觉差比例系数
     */
    fun setParallaxRatio(parallaxRatio: Float){
        this.mParallaxRatio = parallaxRatio
    }

    fun getParallaxRatio():Float{
        return this.mParallaxRatio
    }

    /**
     * 设置拖拽阻尼系数比例
     */
    fun setDragRatio(dragRatio: Float){
        this.mDragRatio = dragRatio
    }


    fun getDragRatio():Float{
        return this.mDragRatio
    }

    /**
     * 设置动画时长
     */
    fun setDuration(duration: Int){
        this.mDuration = duration
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        //动画执行时，拦截一切触摸事件
        if (isAnimRunning()){
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val actionMasked = ev.actionMasked
        if (actionMasked != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                return true
            }
            if (mIsUnableToDrag) {
                return false
            }
        }

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                this.mLastMotionX = ev.x
                this.mInitialMotionX = ev.x
                this.mLastMotionY = ev.y
                this.mInitialMotionY = ev.y
                mActivePointerId = ev.getPointerId(0)
                mIsUnableToDrag = false
                mIsBeingDragged = false
                this.mTotalDx = 0f
                this.mTotalDy = 0f
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                this.mLastMotionX = ev.x
                this.mInitialMotionX = ev.x
                this.mLastMotionY = ev.y
                this.mInitialMotionY = ev.y
                val index = ev.actionIndex
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_MOVE -> {
                val x = ev.getX(ev.findPointerIndex(mActivePointerId))
                val y = ev.getY(ev.findPointerIndex(mActivePointerId))
                val dx = x - mInitialMotionX
                val dy = y - mInitialMotionY
                if (mOpened) {
                    if (Math.abs(dy) >= mTouchSlop || Math.abs(dx) >= mTouchSlop) {
                        mIsBeingDragged = true
                        return true
                    }
                } else {
                    if (dy >= mTouchSlop && Math.abs(dx) <= mTouchSlop) {
                        mIsBeingDragged = true
                        return true
                    }
                    if (Math.abs(dy) >= mTouchSlop) {
                        mIsUnableToDrag = true
                        return false
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mIsUnableToDrag = false
            }
        }

        return super.onInterceptTouchEvent(ev)
    }


    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                this.mLastMotionX = ev.x
                this.mInitialMotionX = ev.x
                this.mLastMotionY = ev.y
                this.mInitialMotionY = ev.y
                this.mTotalDx = 0f
                this.mTotalDy = 0f
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mLastMotionY = ev.getY(index)
                mLastMotionX = ev.getX(index)
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mIsBeingDragged) {
                    val pointerIndex = ev.findPointerIndex(mActivePointerId)
                    if (pointerIndex == -1) {
                        mIsBeingDragged = false
                        return true
                    }
                    val curX = ev.getX(pointerIndex)
                    val curY = ev.getY(pointerIndex)
                    performDrag(curX, curY)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId))
                mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId))
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!mIsBeingDragged || mIsUnableToDrag) {
                    return true
                }
                mIsBeingDragged = false
                mIsUnableToDrag = false

                var notAnim = false //是否不需要执行动画
                if (mTopMenu.translationY == 0f) {
                    mOpened = false
                    notAnim = true
                } else if (mTopMenu.translationY == mMaxTranslationY.toFloat()) {
                    mOpened = true
                    notAnim = true
                }
                //需要执行动画
                if (!notAnim) {
                    if (mOpened) {
                        closeMenu()
                    } else {
                        if (mTopMenu.translationY > mMaxTranslationY / 4) {
                            openMenu()
                        } else {
                            closeMenu()
                        }
                    }
                }else{
                    mMaskView.visibility = if(mOpened) View.VISIBLE else View.GONE
                }
            }
        }
        return true
    }

    private fun startAnimator(open: Boolean) {
        if (mParallaxRatio <= 0f){
            mParallaxRatio= PARALLAX_RATIO
        }
        val topMenuStartY = mTopMenu.translationY
        val topMenuEndY = if (open) mMaxTranslationY.toFloat() else 0f
        val rvStartY = mRecyclerView.translationY
        val rvEndY = if (open) mMaxTranslationY / mParallaxRatio else 0f
        val startAlpha = mMaskView.alpha
        val endAlpha = if (open) 1f else 0f
        if (topMenuStartY == topMenuEndY) return
        mObjectAnimator = ObjectAnimator.ofFloat(mTopMenu, "translationY", topMenuStartY, topMenuEndY)
        mObjectAnimator!!.duration = (Math.abs(topMenuEndY - topMenuStartY) / mMaxTranslationY * mDuration).toLong()
        mObjectAnimator!!.addUpdateListener {
            val animatedFraction = it.animatedFraction
            val translationY = rvStartY + (rvEndY - rvStartY) * animatedFraction
            mRecyclerView.translationY = translationY
            mBottomMenu.translationY = mRecyclerView.translationY

            val alpha = startAlpha + (endAlpha-startAlpha)*animatedFraction
            mMaskView.alpha = alpha
            mMaskView.translationY = mRecyclerView.translationY
        }
        mObjectAnimator!!.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                mMaskView.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                mOpened = open
                if(!mOpened)mMaskView.visibility = View.GONE
                mMaskView.translationY = mRecyclerView.translationY
            }
        })
        mObjectAnimator!!.start()

    }


    /**
     * 打开菜单
     */
    fun openMenu() {
        if (mIsBeingDragged) {
            return
        }
        if (isAnimRunning()){
            return
        }
        startAnimator(true)
    }


    private fun isAnimRunning():Boolean{
        if (mObjectAnimator != null && mObjectAnimator!!.isRunning) {
            return true
        }
        return false
    }

    /**
     * 关闭菜单
     */
    fun closeMenu() {
        if (mIsBeingDragged) {
            return
        }
        if (isAnimRunning()){
            return
        }
        startAnimator(false)
    }

    private fun performDrag(curX: Float, curY: Float) {
        val dx = curX - mLastMotionX
        val dy = curY - mLastMotionY
        mLastMotionX = curX
        mLastMotionY = curY
        mTotalDx += dx
        mTotalDy += dy

        if (mParallaxRatio <= 0f){
            mParallaxRatio= PARALLAX_RATIO
        }

        val ryTransitionY = Math.max(Math.min(mRecyclerView.translationY + dy*mDragRatio,mMaxTranslationY / mParallaxRatio),0f)

        val translationY  = Math.max(Math.min(mTopMenu.translationY + dy * mDragRatio*mParallaxRatio, mMaxTranslationY.toFloat()), 0f)

        mTopMenu.translationY = translationY
        mRecyclerView.translationY = ryTransitionY
        mMaskView.visibility = View.VISIBLE
        mMaskView.translationY = mRecyclerView.translationY
        mMaskView.alpha = translationY/mMaxTranslationY.toFloat()
        mBottomMenu.translationY = mRecyclerView.translationY
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mMaxTranslationY = mTopMenu.measuredHeight
        val measuredHeight = mBottomMenu.measuredHeight
        val layoutParams:MarginLayoutParams = mRecyclerView.layoutParams as MarginLayoutParams
        if (layoutParams.bottomMargin!=measuredHeight) {
            layoutParams.bottomMargin = measuredHeight
            mRecyclerView.layoutParams = layoutParams
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isAnimRunning()){
            mObjectAnimator?.cancel()
        }
    }

}