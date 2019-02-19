package me.hiten.jkcardlayout.sample

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

object MockData {


    fun getCards(context: Context, callBack: (Map<String,Any>) -> Unit) {

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
                    val contentStr = optJSONObject?.optString("content")
                    val picUrl = optJSONObject?.optJSONArray("pictures")?.optJSONObject(0)?.optString("middlePicUrl")
                    if (contentStr != null && picUrl != null) {
                        arrayList.add(CardEntity(picUrl, contentStr))
                    }
                }
            }

            val toolbarList = ArrayList<ToolBarEntity>()
            val toolbars = jsonObject.optJSONObject("data")?.optJSONArray("toolbarItems")
            toolbars?.let {
                for (index in 0 until it.length()) {
                    val toolbar = it.optJSONObject(index)
                    val url = toolbar?.optString("url")
                    val picUrl = toolbar?.optString("picUrl")
                    val title = toolbar?.optString("title")
                    if (title != null && picUrl != null) {
                        toolbarList.add(ToolBarEntity(picUrl,title,url))
                    }
                }
            }
            Handler(Looper.getMainLooper()).post {
                val map = HashMap<String,Any>()
                map["cards"] = arrayList
                map["toolbarItems"] = toolbarList
                callBack(map)
            }
        }).start()
    }
}