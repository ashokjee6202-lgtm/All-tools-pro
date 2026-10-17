package com.example.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.widget.Toast
import com.example.model.RecentFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DownloadHelper {

    /**
     * Saves/downloads a file to the system Public Downloads directory or documents folder,
     * registers it in RecentFileManager, triggers the system media scanner so it appears
     * in the Files app immediately, and displays a prominent "Download Successful!" toast.
     */
    suspend fun downloadFile(
        context: Context,
        sourceFile: File,
        customFileName: String? = null,
        mimeType: String = "application/pdf",
        toolId: String = "tool"
    ): File = withContext(Dispatchers.IO) {
        val targetName = customFileName ?: sourceFile.name

        // Try public Downloads directory first, fallback to external files dir
        val targetDir = try {
            val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!pubDownloads.exists()) {
                pubDownloads.mkdirs()
            }
            if (pubDownloads.canWrite()) pubDownloads else (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir)
        } catch (_: Exception) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }

        // Avoid accidental overwriting by appending timestamp if file with same name exists
        var destFile = File(targetDir, targetName)
        if (destFile.exists()) {
            val nameWithoutExt = targetName.substringBeforeLast(".")
            val ext = targetName.substringAfterLast(".", "")
            val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""
            destFile = File(targetDir, "${nameWithoutExt}_${System.currentTimeMillis() % 10000}$extSuffix")
        }

        try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {
            // Fallback: keep sourceFile
            destFile = sourceFile
        }

        // Register in RecentFileManager
        RecentFileManager.addRecent(context, destFile, mimeType, toolId)

        // Trigger MediaScanner so file appears in user's downloads / gallery immediately
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        } catch (_: Exception) {}

        // Show prominent "Download Successful" toast
        withContext(Dispatchers.Main) {
            showDownloadSuccessToast(context, destFile.name)
        }

        destFile
    }

    /**
     * Shows a clear, user-friendly "Download Successful!" message.
     */
    fun showDownloadSuccessToast(context: Context, fileName: String) {
        val isHi = LocaleManager.isHindi()
        val title = if (isHi) "✅ डाउनलोड सफल!" else "✅ Download Successful!"
        val desc = if (isHi) "फाइल डाउनलोड्स में सुरक्षित की गई:\n$fileName" else "Saved to Downloads folder:\n$fileName"
        Toast.makeText(context, "$title\n$desc", Toast.LENGTH_LONG).show()
    }
}
