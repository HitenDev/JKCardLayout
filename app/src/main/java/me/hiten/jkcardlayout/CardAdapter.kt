package me.hiten.jkcardlayout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class CardAdapter(data: List<CardEntity>) : RecyclerView.Adapter<CardAdapter.VH>() {


    private var mList : List<CardEntity> = data


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false))
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val exploreEntity = mList[position]
        holder.textView.text = exploreEntity.text
        val noAnimation = RequestOptions.noAnimation()
        Glide.with(holder.itemView.context).load(exploreEntity.picUrl).apply(noAnimation).into(holder.bgIv)
        holder.itemView.setOnClickListener {
            Toast.makeText(it.context,exploreEntity.text,Toast.LENGTH_SHORT).show()
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView:TextView = itemView.findViewById(R.id.tv_text)
        val bgIv:ImageView = itemView.findViewById(R.id.iv_background)

    }

}