package com.azhar.aillmgallery.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        val jsonArray = JSONArray()
        for (item in list) {
            jsonArray.put(item)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        if (data == null) return null
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }
}
