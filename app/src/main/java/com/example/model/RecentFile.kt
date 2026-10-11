package com.example.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RecentFile(
    val id: String,
    val name: String,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val toolId: String
)

object RecentFileManager {
    val recentFiles = mutableStateListOf<RecentFile>()
    val favoriteToolIds = mutableStateListOf<String>()

    private const val PREFS_NAME = "alltoolspro_prefs"
    private const val KEY_RECENTS = "key_recent_files"
    private const val KEY_FAVORITES = "key_favorite_tools"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentsJson = prefs.getString(KEY_RECENTS, "[]") ?: "[]"
        try {
            val array = JSONArray(recentsJson)
            recentFiles.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val file = File(obj.getString("path"))
                if (file.exists()) {
                    recentFiles.add(
                        RecentFile(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            path = obj.getString("path"),
                            mimeType = obj.getString("mimeType"),
                            sizeBytes = obj.optLong("sizeBytes", file.length()),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            toolId = obj.optString("toolId", "unknown")
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val favsJson = prefs.getString(KEY_FAVORITES, "[\"doc_scanner\",\"photo_resize\",\"pdf_compress\",\"handwriting\",\"qr_generator\"]") ?: "[]"
        try {
            val favArray = JSONArray(favsJson)
            favoriteToolIds.clear()
            for (i in 0 until favArray.length()) {
                favoriteToolIds.add(favArray.getString(i))
            }
        } catch (_: Exception) {}
    }

    fun addRecent(context: Context, file: File, mimeType: String, toolId: String) {
        val recent = RecentFile(
            id = System.currentTimeMillis().toString(),
            name = file.name,
            path = file.absolutePath,
            mimeType = mimeType,
            sizeBytes = file.length(),
            timestamp = System.currentTimeMillis(),
            toolId = toolId
        )
        recentFiles.removeAll { it.path == file.absolutePath }
        recentFiles.add(0, recent)
        if (recentFiles.size > 50) {
            recentFiles.removeRange(50, recentFiles.size)
        }
        persist(context)
    }

    fun deleteRecent(context: Context, item: RecentFile) {
        try {
            val file = File(item.path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
        recentFiles.remove(item)
        persist(context)
    }

    fun toggleFavorite(context: Context, toolId: String) {
        if (favoriteToolIds.contains(toolId)) {
            favoriteToolIds.remove(toolId)
        } else {
            favoriteToolIds.add(toolId)
        }
        persistFavorites(context)
    }

    fun isFavorite(toolId: String): Boolean = favoriteToolIds.contains(toolId)

    private fun persist(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        recentFiles.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("path", it.path)
            obj.put("mimeType", it.mimeType)
            obj.put("sizeBytes", it.sizeBytes)
            obj.put("timestamp", it.timestamp)
            obj.put("toolId", it.toolId)
            array.put(obj)
        }
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }

    private fun persistFavorites(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        favoriteToolIds.forEach { array.put(it) }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }
}
