package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ImageStorageHelper {

    fun encodeBase64(bytes: ByteArray): String {
        return try {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    /**
     * Converts an Android Bitmap into a compressed universal Base64 data URL
     * suitable for cross-device cloud synchronization via Firebase.
     */
    fun bitmapToDataUrl(bitmap: Bitmap, maxDim: Int = 320, quality: Int = 82): String {
        return try {
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val (w, h) = if (ratio > 1f) Pair(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
                else Pair((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
                Bitmap.createScaledBitmap(bitmap, w, h, true)
            } else bitmap

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val bytes = baos.toByteArray()
            val base64 = encodeBase64(bytes)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            "hero_local_mart"
        }
    }

    /**
     * Converts a content Uri from Photo Picker/Gallery into a universal Base64 data URL.
     */
    fun uriToDataUrl(context: Context, uri: Uri, maxDim: Int = 320, quality: Int = 82): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) bitmapToDataUrl(bitmap, maxDim, quality) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Ensures an image URL is NOT local-only storage (/data/user/0/...).
     * Converts local private files to universal Base64 data URLs before cloud push,
     * while preserving web URLs (http/https), data URLs, and drawable preset keys.
     */
    fun toUniversalImageUrl(context: Context? = null, path: String?): String {
        if (path.isNullOrBlank()) return "hero_local_mart"
        val trimmed = path.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:")) {
            return trimmed
        }
        if (trimmed == "hero_local_mart" || trimmed == "ic_mart_icon") {
            return trimmed
        }
        // If it is a local storage path, convert it so every other mobile can display it
        val clean = trimmed.removePrefix("file:")
        if (clean.startsWith("/")) {
            try {
                val file = File(clean)
                if (file.exists()) {
                    FileInputStream(file).use { fis ->
                        val bitmap = BitmapFactory.decodeStream(fis)
                        if (bitmap != null) {
                            return bitmapToDataUrl(bitmap, maxDim = 320, quality = 82)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "hero_local_mart"
        }
        return trimmed
    }

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
        // First convert to universal cloud-sharable data URL so other mobiles can display it
        val dataUrl = uriToDataUrl(context, uri)
        if (!dataUrl.isNullOrBlank()) {
            copyUri(context, uri, prefix = "prod", subDir = "products") // local cache
            return dataUrl
        }
        return copyUri(context, uri, prefix = "prod", subDir = "products")
    }

    fun saveProductBitmap(context: Context, bitmap: Bitmap): String {
        // Return universal cloud-sharable data URL so other mobiles can display it
        val dataUrl = bitmapToDataUrl(bitmap)
        saveBitmap(context, bitmap, prefix = "prod", subDir = "products") // local cache
        return if (dataUrl.isNotBlank() && dataUrl != "hero_local_mart") dataUrl else saveBitmap(context, bitmap, prefix = "prod", subDir = "products")
    }

    fun isLocalFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val clean = path.removePrefix("file:")
        return if (clean.startsWith("/")) File(clean).exists() else false
    }
}
