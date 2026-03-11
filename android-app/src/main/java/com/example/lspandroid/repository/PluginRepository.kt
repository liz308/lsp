package com.example.lspandroid.repository

import com.example.lspandroid.model.PluginInfo

class PluginRepository {
    fun getAvailablePlugins(): List<PluginInfo> {
        return emptyList()
    }
    
    fun getPluginById(id: String): PluginInfo? {
        return null
    }
}
