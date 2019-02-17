package me.hiten.jkcardlayout.library

import android.graphics.Canvas
import androidx.recyclerview.widget.RecyclerView
import me.hiten.jkcardlayout.library.ItemTouchHelper.*
import java.util.*

class CardLayoutHelper {


    class Config(var maxCount: Int, var offset: Int,var duration:Long = 300,var swipeThreshold:Float = 0.5f)

    interface BindDataSource {
        fun bind(): List<Any>
    }

    enum class State {
        IDLE, SWIPE, BACK_ANIM, LEAVE_ANIM;
    }

    private var mBindDataSource: BindDataSource? = null

    private var mRemovedDataStack = LinkedList<Any>()

    private var mOnCardLayoutListener: OnCardLayoutListener?= null

    private lateinit var mJKCardLayoutManager: JKCardLayoutManager

    private lateinit var mAnimatorStackManager: CardAnimatorManager

    private lateinit var mConfig: Config

    private var mState = State.IDLE


    private lateinit var mRecyclerView: RecyclerView


    fun bindDataSource(bindDataSource: BindDataSource) {
        this.mBindDataSource = bindDataSource
    }

    fun setConfig(config: Config){
        this.mConfig = config
        mJKCardLayoutManager.mConfig = config
    }

    fun setOnCardLayoutListener(onCardLayoutListener: OnCardLayoutListener) {
        mOnCardLayoutListener = onCardLayoutListener
        mJKCardLayoutManager.setOnCardLayoutListener(onCardLayoutListener)
    }

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        mAnimatorStackManager = CardAnimatorManager()
        mConfig = Config(2, 8.dp,swipeThreshold = 0.2f)
        mJKCardLayoutManager = JKCardLayoutManager(mConfig, recyclerView, mAnimatorStackManager)
        recyclerView.layoutManager = mJKCardLayoutManager
        setItemTouchHelper(recyclerView)
    }


    private fun setItemTouchHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            private var mLastIsActive: Boolean? = null

            private var mAnimationType: Int? = null

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ACTION_STATE_SWIPE) {
                    notifyStateListener(State.SWIPE)
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                notifyStateListener(State.IDLE)
                mLastIsActive = null
                mAnimationType = null
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.layoutPosition
                getDataList()?.let {
                    if (it !is MutableList<Any>) {
                        return
                    }
                    val removeAt = it.removeAt(position)
                    mRemovedDataStack.push(removeAt)
                    mRecyclerView.adapter?.notifyDataSetChanged()
                }
                mAnimatorStackManager.let {
                    val animatorInfo = CardAnimatorManager.AnimatorInfo()
                    animatorInfo.targetXr = viewHolder.itemView.translationX / recyclerView.width
                    animatorInfo.targetYr = viewHolder.itemView.translationY / recyclerView.height
                    animatorInfo.targetRotation = viewHolder.itemView.rotation
                    it.addRemoveToBackStack(animatorInfo)
                }
                notifyStateListener(State.IDLE)
                mLastIsActive = null
                mAnimationType = null
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.adapterPosition != 0) {
                    return makeMovementFlags(0, 0)
                }
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return mConfig.swipeThreshold
            }

            override fun getAnimationDuration(recyclerView: RecyclerView, animationType: Int, animateDx: Float, animateDy: Float): Long {
                mAnimationType = animationType
//                if(animationType == ANIMATION_TYPE_SWIPE_CANCEL){
//                    return (mConfig.duration*Math.min(mConfig.swipeThreshold,0.3f)).toLong()
//                }
//                return (mConfig.duration*(1-mConfig.swipeThreshold)).toLong()
                return (mConfig.duration*0.5f).toLong()
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                if (mLastIsActive != null && mLastIsActive!! && !isCurrentlyActive) {
                    //TOUCH UP
                    mAnimationType?.let {
                        if (it == ANIMATION_TYPE_SWIPE_CANCEL) {
                            notifyStateListener(State.BACK_ANIM)
                        } else if (it == ANIMATION_TYPE_SWIPE_SUCCESS) {
                            notifyStateListener(State.LEAVE_ANIM)
                        }
                    }
                }
                mLastIsActive = isCurrentlyActive
                notifyDxDyListener(viewHolder.itemView.translationX,viewHolder.itemView.translationY)
                val dXY = Math.sqrt((dX * dX + dY * dY).toDouble()).toFloat()
                val wh = Math.sqrt((recyclerView.width * recyclerView.width + recyclerView.height * recyclerView.height).toDouble()).toFloat()
                val ratio = Math.min(Math.abs(dXY) / (wh * 0.5f), 1f)

                val childCount = recyclerView.childCount
                if (childCount <= 1) {
                    return
                }
                for (i in 0 until childCount - 1) {
                    val childAt = recyclerView.getChildAt(i)
                    childAt.translationX = mConfig.offset * (childCount - 1 - i - ratio)
                    childAt.translationY = -mConfig.offset * (childCount - 1 - i - ratio)
                }
                viewHolder.itemView.rotation = Math.signum(-dX) * Math.min(1f, Math.abs(dX) / recyclerView.width) * 10f
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }


    private fun notifyStateListener(state:State){
        if(state!=mState) {
            mState = state
            mOnCardLayoutListener?.onStateChanged(mState)
        }
    }

    private fun notifyDxDyListener(dx:Float,dy:Float){
        mOnCardLayoutListener?.onSwipe(dx,dy)
    }

    private fun getDataList(): List<Any>? {
        return mBindDataSource?.bind()
    }


    fun canNext(): Boolean {
        val data = getDataList()
        return data != null && data.isNotEmpty() && mState == State.IDLE && mJKCardLayoutManager.canNext()
    }

    fun canBack(): Boolean {
        return mRemovedDataStack.isNotEmpty() && mState == State.IDLE && mJKCardLayoutManager.canBack()
    }

    fun doNext() {
        val data = getDataList() as? MutableList<Any> ?: return

        if (!canNext()) {
            return
        }
        val removeData = data.removeAt(0)
        mJKCardLayoutManager.pendingOptNext()
        mRemovedDataStack.push(removeData)
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun doBack() {
        val data = getDataList() as? MutableList<Any> ?: return

        if (!canBack()) {
            return
        }
        if (mRemovedDataStack.size > 0) {
            val pop = mRemovedDataStack.pop()
            if (pop != null) {
                mJKCardLayoutManager.pendingOptBack()
                data.add(0, pop)
                mRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

}