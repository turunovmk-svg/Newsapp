package com.mikhail.newsapp.data

import org.json.JSONArray
import org.json.JSONObject

data class Source(
    val type: String,   // "RSS", "REDDIT", "YOUTUBE"
    val url: String
) {
    companion object {
        fun toJsonList(sources: List<Source>): String {
            val array = JSONArray()
            sources.forEach { source ->
                array.put(
                    JSONObject().put("type", source.type).put("url", source.url)
                )
            }
            return array.toString()
        }

        fun fromJsonList(json: String): List<Source> {
            val sources = mutableListOf<Source>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    sources.add(
                        Source(obj.getString("type"), obj.getString("url"))
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return sources
        }
    }
}
