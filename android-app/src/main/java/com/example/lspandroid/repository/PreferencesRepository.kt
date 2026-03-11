package com.example.lspandroid.repository

import android.content.Context
import android.content.SharedPreferences

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lsp_prefs", Context.MODE_PRIVATE)
    
    fun getString(key: String, default: String = ""): String {
        return prefs.getString(key, default) ?: default
    }
    
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return prefs.getBoolean(key, default)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
