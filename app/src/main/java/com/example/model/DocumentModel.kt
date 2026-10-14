package com.example.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ScannedDocItem(
    val id: String,
    val title: String,
    val folderName: String = "All",
    val pdfPath: String,
    val thumbnailPath: String? = null,
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

object DocumentManager {
    val documents = mutableStateListOf<ScannedDocItem>()
    val customFolders = mutableStateListOf<String>()

    private const val PREFS_NAME = "alltoolspro_docs_prefs"
    private const val KEY_DOCS = "key_saved_scanned_docs"
    private const val KEY_FOLDERS = "key_custom_folders"

    private val defaultFolders = listOf("All", "Invoices", "ID Cards", "Personal", "Work")

    fun getAllFolders(): List<String> {
        val list = mutableListOf<String>()
        list.addAll(defaultFolders)
        customFolders.forEach {
            if (!list.contains(it)) list.add(it)
        }
        return list
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load folders
        val foldersJson = prefs.getString(KEY_FOLDERS, "[]") ?: "[]"
        try {
            val fArray = JSONArray(foldersJson)
            customFolders.clear()
            for (i in 0 until fArray.length()) {
                val f = fArray.getString(i)
                if (!customFolders.contains(f)) customFolders.add(f)
            }
        } catch (_: Exception) {}

        // Load documents
        val docsJson = prefs.getString(KEY_DOCS, "[]") ?: "[]"
        try {
            val dArray = JSONArray(docsJson)
            documents.clear()
            for (i in 0 until dArray.length()) {
                val obj = dArray.getJSONObject(i)
                val pdfFile = File(obj.getString("pdfPath"))
                if (pdfFile.exists()) {
                    documents.add(
                        ScannedDocItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            folderName = obj.optString("folderName", "All"),
                            pdfPath = obj.getString("pdfPath"),
                            thumbnailPath = obj.optString("thumbnailPath", null),
                            pageCount = obj.optInt("pageCount", 1),
                            fileSizeBytes = obj.optLong("fileSizeBytes", pdfFile.length()),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Seed sample documents if completely empty so the user has immediate visual documents in Docs manager
        if (documents.isEmpty()) {
            seedInitialDocs(context)
        }
    }

    private fun seedInitialDocs(context: Context) {
        try {
            val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
            val sampleInvoice = File(docsDir, "Sample_Business_Invoice.pdf")
            if (!sampleInvoice.exists()) {
                // Generate a real sample PDF using SampleDataProvider
                val bmp = com.example.util.SampleDataProvider.createSampleDocumentBitmap()
                val pdf = kotlinx.coroutines.runBlocking {
                    com.example.util.PdfProcessor.createPdfFromBitmaps(context, listOf(bmp), outputName = "Sample_Business_Invoice")
                }
                pdf.copyTo(sampleInvoice, overwrite = true)
            }
            if (sampleInvoice.exists()) {
                val item = ScannedDocItem(
                    id = "sample_invoice_1",
                    title = "Sample Business Invoice",
                    folderName = "Invoices",
                    pdfPath = sampleInvoice.absolutePath,
                    thumbnailPath = null,
                    pageCount = 1,
                    fileSizeBytes = sampleInvoice.length(),
                    timestamp = System.currentTimeMillis() - 86400000L
                )
                documents.add(item)
                RecentFileManager.addRecent(context, sampleInvoice, "application/pdf", "doc_scanner")
                persist(context)
            }
        } catch (_: Exception) {}
    }

    fun addDocument(
        context: Context,
        title: String,
        folderName: String,
        pdfFile: File,
        thumbnailFile: File? = null,
        pageCount: Int = 1
    ): ScannedDocItem {
        // Copy PDF into persistent documents directory
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val finalPdfFile = File(docsDir, "${cleanTitle}_${System.currentTimeMillis()}.pdf")
        pdfFile.copyTo(finalPdfFile, overwrite = true)

        var thumbPath: String? = null
        if (thumbnailFile != null && thumbnailFile.exists()) {
            val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val finalThumbFile = File(thumbsDir, "thumb_${System.currentTimeMillis()}.jpg")
            thumbnailFile.copyTo(finalThumbFile, overwrite = true)
            thumbPath = finalThumbFile.absolutePath
        }

        val item = ScannedDocItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            folderName = if (folderName.isBlank()) "All" else folderName,
            pdfPath = finalPdfFile.absolutePath,
            thumbnailPath = thumbPath,
            pageCount = pageCount,
            fileSizeBytes = finalPdfFile.length(),
            timestamp = System.currentTimeMillis()
        )

        documents.add(0, item)
        persist(context)

        // Also sync with RecentFileManager
        RecentFileManager.addRecent(context, finalPdfFile, "application/pdf", "doc_scanner")
        return item
    }

    fun renameDocument(context: Context, item: ScannedDocItem, newTitle: String) {
        val index = documents.indexOfFirst { it.id == item.id }
        if (index != -1) {
            documents[index] = item.copy(title = newTitle)
            persist(context)
        }
    }

    fun moveToFolder(context: Context, item: ScannedDocItem, targetFolder: String) {
        val index = documents.indexOfFirst { it.id == item.id }
        if (index != -1) {
            documents[index] = item.copy(folderName = targetFolder)
            persist(context)
        }
    }

    fun moveMultipleToFolder(context: Context, items: List<ScannedDocItem>, targetFolder: String) {
        val ids = items.map { it.id }.toSet()
        for (i in documents.indices) {
            if (ids.contains(documents[i].id)) {
                documents[i] = documents[i].copy(folderName = targetFolder)
            }
        }
        persist(context)
    }

    fun deleteDocument(context: Context, item: ScannedDocItem) {
        try {
            val f = File(item.pdfPath)
            if (f.exists()) f.delete()
            item.thumbnailPath?.let {
                val tf = File(it)
                if (tf.exists()) tf.delete()
            }
        } catch (_: Exception) {}
        documents.remove(item)
        persist(context)
    }

    fun deleteMultiple(context: Context, items: List<ScannedDocItem>) {
        items.forEach { item ->
            try {
                val f = File(item.pdfPath)
                if (f.exists()) f.delete()
                item.thumbnailPath?.let {
                    val tf = File(it)
                    if (tf.exists()) tf.delete()
                }
            } catch (_: Exception) {}
            documents.remove(item)
        }
        persist(context)
    }

    fun addFolder(context: Context, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !getAllFolders().contains(trimmed)) {
            customFolders.add(trimmed)
            persistFolders(context)
        }
    }

    fun deleteFolder(context: Context, folderName: String) {
        customFolders.remove(folderName)
        // Reset documents in this folder to "All"
        for (i in documents.indices) {
            if (documents[i].folderName == folderName) {
                documents[i] = documents[i].copy(folderName = "All")
            }
        }
        persist(context)
        persistFolders(context)
    }

    private fun persist(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        documents.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("folderName", it.folderName)
            obj.put("pdfPath", it.pdfPath)
            obj.put("thumbnailPath", it.thumbnailPath ?: "")
            obj.put("pageCount", it.pageCount)
            obj.put("fileSizeBytes", it.fileSizeBytes)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        prefs.edit().putString(KEY_DOCS, array.toString()).apply()
    }

    private fun persistFolders(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        customFolders.forEach { array.put(it) }
        prefs.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }
}
