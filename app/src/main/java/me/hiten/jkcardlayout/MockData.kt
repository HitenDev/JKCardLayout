package me.hiten.jkcardlayout

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

object MockData {


    fun getCards(context: Context, callBack: (List<CardEntity>) -> Unit) {

        Thread(Runnable {
            val arrayList = ArrayList<CardEntity>()
            val inputStream = context.resources.assets.open("jk_daily_cards.json")
            val content =  String(inputStream.readBytes())


            val jsonObject = JSONObject(content)
            val cards = jsonObject.optJSONObject("data")?.optJSONArray("cards")
            cards?.let {
                for (index in 0 until it.length()) {
                    val card = it.optJSONObject(index)
                    val optJSONObject = card?.optJSONObject("originalPost")
                    val content = optJSONObject?.optString("content")
                    val picUrl = optJSONObject?.optJSONArray("pictures")?.optJSONObject(0)?.optString("middlePicUrl")
                    if (content != null && picUrl != null) {
                        arrayList.add(CardEntity(picUrl, content))
                    }
                }
            }
            Handler(Looper.getMainLooper()).post {
                callBack(arrayList)
            }
        }).start()
    }
}