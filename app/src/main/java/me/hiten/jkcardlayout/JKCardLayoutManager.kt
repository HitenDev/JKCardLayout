package me.hiten.jkcardlayout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

class JKCardLayoutManager(config: Config, recyclerView: RecyclerView, animatorStackManager: AnimatorStackManager) : RecyclerView.LayoutManager() {


    private val mRecyclerView: RecyclerView = recyclerView
    private val mAnimatorStackManager: AnimatorStackManager = animatorStackManager

    class Config(maxCount: Int, offset: Int) {
        var maxCount = maxCount
        var offset = offset
    }

    var mConfig = config


    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)

        if (recycler == null || state == null) {
            return
        }

        if (mAnimationRunning) {
            return
        }

        if (mRunPrevAnimation) {
            if (mRecyclerView.childCount < 0) {
                mRunPrevAnimation = false
                onLayoutChildren(recycler, state)
            } else {
                mAnimationRunning = true
                if (mRecyclerView.childCount > mConfig.maxCount-1) {
                    detachAndScrapViewAt(0, recycler)
                }
                val view = recycler.getViewForPosition(0)
                addView(view)
                measureChildWithMargins(view, 0, 0)

                val widthSpace = width - getDecoratedMeasuredWidth(view)
                val heightSpace = height - getDecoratedMeasuredHeight(view)
                layoutDecoratedWithMargins(view, widthSpace / 2, heightSpace / 2,
                        widthSpace / 2 + getDecoratedMeasuredWidth(view),
                        heightSpace / 2 + getDecoratedMeasuredHeight(view))
//

                val add = mAnimatorStackManager.takeAdd()
                view.alpha = 0f
                view.translationX = add.startX * mRecyclerView.width
                view.translationY = add.startY * mRecyclerView.height
                view.rotation = add.startRotation
                view.animate().setDuration(300).rotation(add.endRotation).translationX(add.targetX * mRecyclerView.width).translationY(add.targetY * mRecyclerView.height)
                        .setListener(object : AnimatorListenerAdapter() {


                            override fun onAnimationStart(animation: Animator?) {
                                super.onAnimationStart(animation)
                                view.alpha = 1f
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                mRunPrevAnimation = false
                                mAnimationRunning = false
                                requestLayout()
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                                super.onAnimationCancel(animation)
                                mRunPrevAnimation = false
                                mAnimationRunning = false
                                requestLayout()
                            }
                        })
                        .setUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                            val childCount = mRecyclerView.childCount
                            val translationXs: Array<Float?> = arrayOfNulls(childCount)
                            val translationYs: Array<Float?> = arrayOfNulls(childCount)
                            override fun onAnimationUpdate(animation: ValueAnimator?) {
                                val value = animation!!.animatedValue as Float
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
                        .start()
            }
            return
        }

        if (mRunNextAnimation) {
            if (mRecyclerView.childCount < 0) {
                mRunNextAnimation = false
                onLayoutChildren(recycler, state)
            } else {
                mAnimationRunning = true
                val view = mRecyclerView.getChildAt(mRecyclerView.childCount - 1)
                val createRemove = mAnimatorStackManager.createRemove()
                view.animate().setDuration(300).translationX(createRemove.targetX*mRecyclerView.width).translationY(createRemove.targetY*mRecyclerView.height).rotation(createRemove.endRotation)
                        .setListener(object :AnimatorListenerAdapter(){

                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                mRunNextAnimation = false
                                mAnimationRunning = false
                                mAnimatorStackManager.addRemoveToBackStack(createRemove)
                                requestLayout()
                            }
                        })
                        .setUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                            val childCount = mRecyclerView.childCount
                            val translationXs: Array<Float?> = arrayOfNulls(childCount)
                            val translationYs: Array<Float?> = arrayOfNulls(childCount)
                            override fun onAnimationUpdate(animation: ValueAnimator?) {
                                val value = animation!!.animatedValue as Float
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
                        .start()

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
            measureChildWithMargins(view, 0, 0)

            val widthSpace = width - getDecoratedMeasuredWidth(view)
            val heightSpace = height - getDecoratedMeasuredHeight(view)
            layoutDecoratedWithMargins(view, widthSpace / 2, heightSpace / 2,
                    widthSpace / 2 + getDecoratedMeasuredWidth(view),
                    heightSpace / 2 + getDecoratedMeasuredHeight(view))

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


    var mRunPrevAnimation = false
    var mRunNextAnimation = false
    var mAnimationRunning = false

    fun pendingOptPrev() {
        mRunPrevAnimation = true
    }

    fun pendingOptNext() {
        mRunNextAnimation = true
    }
}