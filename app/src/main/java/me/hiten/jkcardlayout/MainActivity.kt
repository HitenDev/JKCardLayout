package me.hiten.jkcardlayout

import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {


    private var mRemoveDataStack = LinkedList<String>()
    private var list = ArrayList<String>()
    private var cardAdapter : CardAdapter? = null

    private var animatorStackManager: AnimatorStackManager? = null

    private var jKCardLayoutManager: JKCardLayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = JKCardLayoutManager.Config(2,24)

        for (i in 0..40){
            list.add(i.toString())
        }

        animatorStackManager = AnimatorStackManager()

        jKCardLayoutManager = JKCardLayoutManager(config,recycler_view,animatorStackManager!!)

        recycler_view.layoutManager = jKCardLayoutManager

        cardAdapter = CardAdapter(list)

        recycler_view.adapter =cardAdapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val layoutPosition = viewHolder.layoutPosition
                val removeAt = list.removeAt(layoutPosition)
                mRemoveDataStack.push(removeAt)
                recycler_view.adapter?.notifyDataSetChanged()
                animatorStackManager?.let {
                    val animatorInfo = AnimatorStackManager.AnimatorInfo()
                    animatorInfo.startX = 0f
                    animatorInfo.startY = 0f
                    animatorInfo.targetX = viewHolder.itemView.translationX/recycler_view.width
                    animatorInfo.targetY = viewHolder.itemView.translationY/recycler_view.height
                    animatorInfo.startRotation = 0f
                    animatorInfo.endRotation = viewHolder.itemView.rotation
                    animatorInfo.isAdd = false
                    it.addRemoveToBackStack(animatorInfo)
                }
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.adapterPosition!=0){
                    return makeMovementFlags(0,0)
                }
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.2f
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                val dXY = Math.sqrt((dX * dX + dY * dY).toDouble()).toFloat()
                val wh = Math.sqrt((recyclerView.width*recyclerView.width+recyclerView.height*recyclerView.height).toDouble()).toFloat()
                var ratio = 1-Math.min(Math.abs(dXY)/(wh*0.5f),1f)

                val childCount = recyclerView.childCount
                if (childCount<=1){
                    return
                }
                for (i in 0 until childCount-1){
                    val childAt = recyclerView.getChildAt(i)
                    childAt.translationX = config.offset * (i+ratio)
                    childAt.translationY = -config.offset * (i+ratio)
                }

                viewHolder.itemView.rotation = Math.signum(-dX)*Math.min(1f,Math.abs(dX)/recyclerView.width)*10f

            }
        })
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    override fun onBackPressed() {
        if (mRemoveDataStack.size>0) {
            val pop = mRemoveDataStack.pop()
            if (pop != null) {
                list.add(0, pop)
                jKCardLayoutManager?.pendingOptPrev()
                cardAdapter?.notifyDataSetChanged()
                return
            }else{
                super.onBackPressed()
            }
        }
        super.onBackPressed()
    }

}
