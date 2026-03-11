package com.example.lspandroid.ui

import android.content.Context
import android.util.Log
import com.example.lspandroid.model.PortMetadata
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Simplified layout override manager for custom UI layouts.
 * Supports JSON files to override default layout generated from metadata.
 */
object LayoutOverrideManager {

    private const val TAG = "LayoutOverrideManager"
    private val layoutOverrides = ConcurrentHashMap<String, LayoutOverride>()

    /**
     * Loads a layout override from JSON string.
     */
    fun loadLayoutOverride(pluginId: String, json: String): LayoutOverride? {
        return try {
            val jsonObject = JSONObject(json)
            val override = parseLayoutOverride(pluginId, jsonObject)
            layoutOverrides[pluginId] = override
            Log.d(TAG, "Successfully loaded layout override for $pluginId")
            override
        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing error for $pluginId", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading layout for $pluginId", e)
            null
        }
    }

    /**
     * Gets a layout override for a plugin.
     */
    fun getLayoutOverride(pluginId: String): LayoutOverride? {
        return layoutOverrides[pluginId]
    }

    /**
     * Clears a layout override for a plugin.
     */
    fun clearLayoutOverride(pluginId: String) {
        layoutOverrides.remove(pluginId)
    }

    private fun parseLayoutOverride(pluginId: String, json: JSONObject): LayoutOverride {
        val sections = mutableListOf<LayoutSection>()

        val sectionsArray = json.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until sectionsArray.length()) {
            val sectionJson = sectionsArray.getJSONObject(i)
            val section = parseLayoutSection(sectionJson)
            sections.add(section)
        }

        return LayoutOverride(
            pluginId = pluginId,
            version = json.optString("version", "1.0"),
            sections = sections,
            globalSettings = GlobalLayoutSettings(
                theme = json.optString("theme", "default"),
                compactMode = json.optBoolean("compactMode", false),
                showUnits = json.optBoolean("showUnits", true),
                animationDuration = json.optInt("animationDuration", 300),
                touchSensitivity = json.optDouble("touchSensitivity", 1.0).toFloat()
            )
        )
    }

    private fun parseLayoutSection(json: JSONObject): LayoutSection {
        val items = mutableListOf<LayoutItem>()

        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        for (i in 0 until itemsArray.length()) {
            val itemJson = itemsArray.getJSONObject(i)
            val item = parseLayoutItem(itemJson)
            items.add(item)
        }

        return LayoutSection(
            name = json.optString("name", null),
            items = items,
            columns = json.optInt("columns", 1),
            spacing = json.optDouble("spacing", 16.0).toFloat(),
            backgroundColor = json.optString("backgroundColor", null),
            borderColor = json.optString("borderColor", null),
            isCollapsible = json.optBoolean("isCollapsible", false),
            defaultExpanded = json.optBoolean("defaultExpanded", true)
        )
    }

    private fun parseLayoutItem(json: JSONObject): LayoutItem {
        val customProperties = mutableMapOf<String, String>()
        val propsJson = json.optJSONObject("customProperties")
        propsJson?.let {
            it.keys().forEach { key ->
                customProperties[key] = it.getString(key)
            }
        }

        return LayoutItem(
            portName = json.getString("portName"),
            controlType = json.optString("controlType", null),
            customLabel = json.optString("customLabel", null),
            isVisible = json.optBoolean("isVisible", true),
            width = json.optDouble("width", 1.0).toFloat(),
            height = if (json.has("height")) json.getDouble("height").toFloat() else null,
            x = if (json.has("x")) json.getDouble("x").toFloat() else null,
            y = if (json.has("y")) json.getDouble("y").toFloat() else null,
            customProperties = customProperties
        )
    }
}