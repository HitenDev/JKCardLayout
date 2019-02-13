package me.hiten.jkcardlayout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(data: List<String>) : RecyclerView.Adapter<CardAdapter.VH>() {


    private var mList : List<String> = data


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false))
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.textView.text = mList[position]
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView:TextView = itemView.findViewById(R.id.tv_text)

    }

}