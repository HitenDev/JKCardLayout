package me.hiten.jkcardlayout

import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = JKCardLayoutManager.Config(2,24)

        val list = ArrayList<String>()

        for (i in 0..40){
            list.add(i.toString())
        }

        recycler_view.layoutManager = JKCardLayoutManager(config)

        recycler_view.adapter = CardAdapter(list)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val layoutPosition = viewHolder.layoutPosition
                list.removeAt(layoutPosition)
                recycler_view.adapter?.notifyDataSetChanged()
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
        val defaultItemAnimator = DefaultItemAnimator()
        defaultItemAnimator.removeDuration = 100
        recycler_view.itemAnimator = defaultItemAnimator
    }



}
