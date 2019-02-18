package me.hiten.jkcardlayout.library

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

class JKCardLayoutManager(config: CardLayoutHelper.Config, recyclerView: RecyclerView, animatorStackManager: CardAnimatorManager) : RecyclerView.LayoutManager() {

    private val mRecyclerView: RecyclerView = recyclerView
    private val mAnimatorStackManager: CardAnimatorManager = animatorStackManager

    private var mCardLayoutListener :OnCardLayoutListener? = null


    private var mState :CardLayoutHelper.State = CardLayoutHelper.State.IDLE

    var mConfig = config

    fun setOnCardLayoutListener(onCardLayoutListener: OnCardLayoutListener) {
        this.mCardLayoutListener = onCardLayoutListener
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        if (recycler == null || state == null) {
            return
        }
        if (mAnimatorRunning) {
            return
        }
        if (mPendingOptBack) {
            if (mRecyclerView.childCount < 0) {
                mPendingOptBack = false
                mState = CardLayoutHelper.State.IDLE
                onLayoutChildren(recycler, state)
            } else {
                mAnimatorRunning = true
                if (mRecyclerView.childCount > mConfig.maxCount - 1) {
                    detachAndScrapViewAt(0, recycler)
                }
                val view = recycler.getViewForPosition(0)
                addView(view)
                measureLayoutItemView(view)
                view.alpha = 0f
                doBackAnimator(view)
            }
            return
        }

        if (mPendingOptNext) {
            if (mRecyclerView.childCount < 0) {
                mPendingOptNext = false
                mState = CardLayoutHelper.State.IDLE
                onLayoutChildren(recycler, state)
            } else {
                mAnimatorRunning = true
                val view = mRecyclerView.getChildAt(mRecyclerView.childCount - 1)
                doNextAnimator(view)
            }
            return
        }
        detachAndScrapAttachedViews(recycler)

        if (itemCount < 1) {
            return
        }

        val maxCount = Math.min(mConfig.maxCount, itemCount)

        for (position in maxCount - 1 downTo 0) {
            val view = recycler.getViewForPosition(position)
            addView(view)
            measureLayoutItemView(view)
            view.rotation = 0f
            if (position > 0) {
                view.translationX = mConfig.offset * position.toFloat()
                view.translationY = -mConfig.offset * position.toFloat()
            } else {
                view.translationX = 0f
                view.translationY = 0f
            }

        }

    }

    private fun measureLayoutItemView(view: View){
        measureChildWithMargins(view, 0, 0)

        val widthSpace = width - getDecoratedMeasuredWidth(view)
        val lp = view.layoutParams as RecyclerView.LayoutParams
        layoutDecoratedWithMargins(view, widthSpace / 2, 0,
                widthSpace / 2 + getDecoratedMeasuredWidth(view),
                lp.topMargin + getDecoratedMeasuredHeight(view))
    }


    private fun doBackAnimator(view:View){
        val add = mAnimatorStackManager.takeAdd()
        val objectAnimator = ObjectAnimator.ofObject(view, CardAnimatorManager.AnimatorInfoProperty(mRecyclerView.width.toFloat(), mRecyclerView.height.toFloat()), CardAnimatorManager.AnimatorInfoEvaluator(), add, CardAnimatorManager.AnimatorInfo.ZERO)
        objectAnimator.setDuration(mConfig.duration).addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                view.alpha = 1f
                mCardLayoutListener?.onStateChanged(CardLayoutHelper.State.BACK_ANIM)
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                mPendingOptBack = false
                mAnimatorRunning = false
                mCardLayoutListener?.onStateChanged(CardLayoutHelper.State.IDLE)
                requestLayout()
            }

            override fun onAnimationCancel(animation: Animator?) {
                super.onAnimationCancel(animation)
                mPendingOptBack = false
                mAnimatorRunning = false
                mCardLayoutListener?.onStateChanged(CardLayoutHelper.State.IDLE)
                requestLayout()
            }
        })
        objectAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            val childCount = mRecyclerView.childCount
            val translationXs: Array<Float?> = arrayOfNulls(childCount)
            val translationYs: Array<Float?> = arrayOfNulls(childCount)
            override fun onAnimationUpdate(animation: ValueAnimator?) {
                mCardLayoutListener?.onSwipe(view.translationX,view.translationY)
                val value = animation!!.animatedFraction
                val fl = value * mConfig.offset
                for (i in 0 until childCount) {
                    val childAt = mRecyclerView.getChildAt(i)
                    if (childAt == view) {
                        continue
                    }
                    if (translationXs[i] == null) {
                        translationXs[i] = childAt.translationX
                    }
                    if (translationYs[i] == null) {
                        translationYs[i] = childAt.translationY
                    }
                    childAt.translationX = translationXs[i]!!.toFloat() + fl
                    childAt.translationY = translationYs[i]!!.toFloat() - fl
                }
            }

        })
        objectAnimator.start()
    }

    private fun doNextAnimator(view: View){
        val createRemove = mAnimatorStackManager.createRemove()
        val objectAnimator = ObjectAnimator.ofObject(view, CardAnimatorManager.AnimatorInfoProperty(mRecyclerView.width.toFloat(), mRecyclerView.height.toFloat()), CardAnimatorManager.AnimatorInfoEvaluator(), CardAnimatorManager.AnimatorInfo.ZERO, createRemove)
        objectAnimator.setDuration(mConfig.duration).addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                mCardLayoutListener?.onStateChanged(CardLayoutHelper.State.LEAVE_ANIM)
            }

            override fun onAnimationCancel(animation: Animator?) {
                super.onAnimationCancel(animation)
                mCardLayoutListener?.onStateChanged(CardLayoutHelper.State.IDLE)
            }
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                mPendingOptNext = false
                mAnimatorRunning = false
                mAnimatorStackManager.addRemoveToBackStack(createRemove)
                mCardLayoutListener?.onStateChanged(CardLayoutHelper.State.IDLE)
                requestLayout()
            }
        })
        objectAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            val childCount = mRecyclerView.childCount
            val translationXs: Array<Float?> = arrayOfNulls(childCount)
            val translationYs: Array<Float?> = arrayOfNulls(childCount)
            override fun onAnimationUpdate(animation: ValueAnimator?) {
                mCardLayoutListener?.onSwipe(view.translationX,view.translationY)
                val value = animation!!.animatedFraction
                val fl = value * mConfig.offset
                for (i in 0 until childCount) {
                    val childAt = mRecyclerView.getChildAt(i)
                    if (childAt == view) {
                        continue
                    }
                    if (translationXs[i] == null) {
                        translationXs[i] = childAt.translationX
                    }
                    if (translationYs[i] == null) {
                        translationYs[i] = childAt.translationY
                    }
                    childAt.translationX = translationXs[i]!!.toFloat() - fl
                    childAt.translationY = translationYs[i]!!.toFloat() + fl
                }
            }

        })
        objectAnimator.start()
    }

    private var mPendingOptBack = false
    private var mPendingOptNext = false
    private var mAnimatorRunning = false

    fun canBack():Boolean{
        return !mPendingOptBack&&!mAnimatorRunning
    }

    fun canNext():Boolean{
        return !mPendingOptNext&&!mAnimatorRunning
    }

    fun pendingOptBack() {
        mPendingOptBack = true
    }

    fun pendingOptNext() {
        mPendingOptNext = true
    }
}