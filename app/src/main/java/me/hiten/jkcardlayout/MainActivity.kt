package me.hiten.jkcardlayout

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import me.hiten.jkcardlayout.library.*
import java.util.*

class MainActivity : AppCompatActivity() {


    private var list = ArrayList<CardEntity>()
    private var cardAdapter : CardAdapter? = null


    private lateinit var mCardLayoutHelper : CardLayoutHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mCardLayoutHelper = CardLayoutHelper()

        mCardLayoutHelper.attachToRecyclerView(recycler_view)

        mCardLayoutHelper.bindDataSource(object : CardLayoutHelper.BindDataSource{
            override fun bind(): List<Any> {
                return list
            }
        })

        mCardLayoutHelper.setOnCardLayoutListener(object :OnCardLayoutListener{
            override fun onSwipe(dx: Float, dy: Float) {
                Log.d("onStateChanged","dx:$dx dy:$dy")
            }

            override fun onStateChanged(state: CardLayoutHelper.State) {
                Log.d("onStateChanged",state.name)
            }

        })

        cardAdapter = CardAdapter(list)

        recycler_view.adapter =cardAdapter


        MockData.getCards(this) {
            list.clear()
            list.addAll(it)
            cardAdapter?.notifyDataSetChanged()
        }



        btn_prev.setOnClickListener {
            onBackPressed()
        }

        btn_next.setOnClickListener {
            onNextPressed()
        }
    }

    private fun onNextPressed(){
        if (mCardLayoutHelper.canNext()) {
            mCardLayoutHelper.doNext()
        }
    }

    override fun onBackPressed() {
        if (mCardLayoutHelper.canBack()){
            mCardLayoutHelper.doBack()
        }else{
            super.onBackPressed()
        }
    }

}
