package me.hiten.jkcardlayout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView


class PullDownLayout : ConstraintLayout {
    companion object {
        private const val DRAG_RATIO = 0.6f

        private const val ANIM_DURATION = 200

        private const val PARALLAX_RATIO = 0.9f
    }

    private var mRecyclerView: RecyclerView

    private var mTopMenu: View

    private var mMaskView : View

    private var mBottomMenu : View

    private var mTouchSlop: Int

    private var mIsBeingDragged: Boolean = false

    private var mIsUnableToDrag: Boolean = false

    private var mLastMotionX: Float = 0f
    private var mLastMotionY: Float = 0f
    private var mInitialMotionX: Float = 0f
    private var mInitialMotionY: Float = 0f
    private var mActivePointerId = -1

    private var mTotalDx: Float = 0f
    private var mTotalDy: Float = 0f
    private val mDragRatio = DRAG_RATIO

    private val mDuration = ANIM_DURATION

    private val mParallaxRatio = PARALLAX_RATIO

    private var mOpened = false

    private var mObjectAnimator: ObjectAnimator? = null

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


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
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

                var notAnim = false
                if (mTopMenu.translationY == 0f) {
                    mOpened = false
                    notAnim = true
                } else if (mTopMenu.translationY == mMaxTranslationY.toFloat()) {
                    mOpened = true
                    notAnim = true
                }
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
        val startY = mTopMenu.translationY
        val endY = if (open) mMaxTranslationY.toFloat() else 0f
        val startY2 = mRecyclerView.translationY
        val endY2 = if (open) mMaxTranslationY * mParallaxRatio else 0f
        val startAlpha = mMaskView.alpha
        val endAlpha = if (open) 1f else 0f
        if (startY == endY) return
        mObjectAnimator = ObjectAnimator.ofFloat(mTopMenu, "translationY", startY, endY)
        mObjectAnimator!!.duration = (Math.abs(endY - startY) / mMaxTranslationY * mDuration).toLong()
        mObjectAnimator!!.addUpdateListener {
            val animatedFraction = it.animatedFraction
            val translationY = startY2 + (endY2 - startY2) * animatedFraction
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

    private fun closeMenu() {
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

        val translationY: Int = Math.max(Math.min((mTopMenu.translationY + dy * mDragRatio).toInt(), mMaxTranslationY), 0)
        mTopMenu.translationY = translationY.toFloat()
        mRecyclerView.translationY = translationY * mParallaxRatio
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