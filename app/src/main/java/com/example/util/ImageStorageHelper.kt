package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {
    fun saveBitmap(context: Context, bitmap: Bitmap, prefix: String = "dp", subDir: String = "profiles"): String {
        val dir = File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }

    fun copyUri(context: Context, uri: Uri, prefix: String = "dp", subDir: String = "profiles"): String? {
        return try {
            val dir = File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyProductUri(context: Context, uri: Uri): String? {
        return copyUri(context, uri, prefix = "prod", subDir = "products")
    }

    fun saveProductBitmap(context: Context, bitmap: Bitmap): String {
        return saveBitmap(context, bitmap, prefix = "prod", subDir = "products")
    }

    fun isLocalFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val clean = path.removePrefix("file:")
        return if (clean.startsWith("/")) File(clean).exists() else false
    }
}
