package com.example.data.customer

import android.content.Context
import org.json.JSONObject

class CartStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("green_shop_cart", Context.MODE_PRIVATE)

    fun loadQuantities(): Map<String, Int> = runCatching {
        val json = JSONObject(preferences.getString("quantities", "{}") ?: "{}")
        json.keys().asSequence().mapNotNull { id ->
            json.optInt(id, 0).takeIf { it > 0 }?.let { id to it }
        }.toMap()
    }.getOrDefault(emptyMap())

    fun saveQuantities(quantities: Map<String, Int>) {
        val json = JSONObject()
        quantities.filterValues { it > 0 }.forEach { (id, quantity) -> json.put(id, quantity) }
        preferences.edit().putString("quantities", json.toString()).apply()
    }

    fun clear() { preferences.edit().remove("quantities").apply() }
}
