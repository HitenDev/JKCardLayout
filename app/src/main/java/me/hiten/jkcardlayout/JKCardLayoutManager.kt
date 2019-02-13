package me.hiten.jkcardlayout

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class JKCardLayoutManager(config: Config) : RecyclerView.LayoutManager() {

    class Config(maxCount: Int, offset: Int) {
        var maxCount = maxCount
        var offset = offset
    }

    var mConfig = config


    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)

        if (recycler == null || state == null) {
            return
        }


        detachAndScrapAttachedViews(recycler)

        if (itemCount < 1) {
            return
        }

        val maxCount = Math.min(mConfig.maxCount, itemCount)

        for (position in maxCount-1 downTo 0) {
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
            }else{
                view.translationX = 0f
                view.translationY = 0f
            }

        }

    }

}