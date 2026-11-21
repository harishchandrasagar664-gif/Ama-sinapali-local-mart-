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

    const val MAX_FIRESTORE_IMAGE_URL_CHARS = 350_000
    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encodeBase64(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        return try {
            val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            if (!encoded.isNullOrEmpty()) encoded else encodeBase64PureKotlin(bytes)
        } catch (_: Throwable) {
            encodeBase64PureKotlin(bytes)
        }
    }

    internal fun encodeBase64PureKotlin(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val sb = StringBuilder(((bytes.size + 2) / 3) * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val hasB1 = i + 1 < bytes.size
            val hasB2 = i + 2 < bytes.size
            val b1 = if (hasB1) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) bytes[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2

            sb.append(BASE64_ALPHABET[(triple ushr 18) and 0x3F])
            sb.append(BASE64_ALPHABET[(triple ushr 12) and 0x3F])
            sb.append(if (hasB1) BASE64_ALPHABET[(triple ushr 6) and 0x3F] else '=')
            sb.append(if (hasB2) BASE64_ALPHABET[triple and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqMaxDim: Int): Int {
        var inSampleSize = 1
        if (width > reqMaxDim || height > reqMaxDim) {
            while ((width / (inSampleSize * 2)) >= reqMaxDim && (height / (inSampleSize * 2)) >= reqMaxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun decodeSampledStream(openStream: () -> java.io.InputStream?, reqMaxDim: Int = 1024): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val opts = if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
            BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, reqMaxDim)
            }
        } else {
            BitmapFactory.Options()
        }
        return openStream()?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    private fun scalePreservingAspectRatio(bitmap: Bitmap, maxDim: Int): Bitmap {
        if (bitmap.width <= maxDim && bitmap.height <= maxDim) return bitmap
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        val (w, h) = if (ratio > 1f) {
            Pair(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
        } else {
            Pair((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
        }
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    /**
     * Converts an Android Bitmap into a compressed universal Base64 data URL
     * suitable for cross-device cloud synchronization via Firebase Firestore
     * (guaranteed <= 350,000 chars to stay within firestore.rules' 400,000-byte cap).
     */
    fun bitmapToDataUrl(bitmap: Bitmap, maxDim: Int = 640, quality: Int = 85): String {
        return try {
            var currentDim = maxDim
            var currentQuality = quality
            repeat(6) {
                val scaled = scalePreservingAspectRatio(bitmap, currentDim)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos)
                val bytes = baos.toByteArray()
                val base64 = encodeBase64(bytes)
                val dataUrl = "data:image/jpeg;base64,$base64"
                if (dataUrl.length <= MAX_FIRESTORE_IMAGE_URL_CHARS) {
                    return dataUrl
                }
                currentQuality = (currentQuality - 15).coerceAtLeast(45)
                currentDim = (currentDim * 0.75f).toInt().coerceAtLeast(180)
            }
            "hero_local_mart"
        } catch (e: Exception) {
            e.printStackTrace()
            "hero_local_mart"
        }
    }

    /**
     * Converts a content Uri from Photo Picker/Gallery into a universal Base64 data URL.
     */
    fun uriToDataUrl(context: Context, uri: Uri, maxDim: Int = 640, quality: Int = 85): String? {
        return try {
            val bitmap = decodeSampledStream(
                openStream = { context.contentResolver.openInputStream(uri) },
                reqMaxDim = maxDim.coerceAtLeast(640)
            )
            if (bitmap != null) bitmapToDataUrl(bitmap, maxDim, quality) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Ensures an image URL is NOT local-only storage (/data/user/0/...).
     * Converts local private files and content URIs to universal Base64 data URLs before cloud push,
     * while preserving web URLs (http/https), bounded data URLs, and drawable preset keys.
     */
    fun toUniversalImageUrl(context: Context? = null, path: String?): String {
        if (path.isNullOrBlank()) return "hero_local_mart"
        val trimmed = path.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("data:")) {
            if (trimmed.length <= MAX_FIRESTORE_IMAGE_URL_CHARS) return trimmed
            return try {
                val base64Part = trimmed.substringAfter(",")
                val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) bitmapToDataUrl(bitmap, maxDim = 480, quality = 75) else "hero_local_mart"
            } catch (_: Throwable) {
                "hero_local_mart"
            }
        }
        if (trimmed == "hero_local_mart" || trimmed == "ic_mart_icon") {
            return trimmed
        }
        if (trimmed.startsWith("content://") && context != null) {
            return try {
                uriToDataUrl(context, Uri.parse(trimmed)) ?: "hero_local_mart"
            } catch (_: Throwable) {
                "hero_local_mart"
            }
        }
        // If it is a local storage path, convert it so every other mobile can display it
        val clean = trimmed.removePrefix("file:")
        if (clean.startsWith("/")) {
            try {
                val file = File(clean)
                if (file.exists()) {
                    val bitmap = decodeSampledStream(
                        openStream = { FileInputStream(file) },
                        reqMaxDim = 640
                    )
                    if (bitmap != null) {
                        return bitmapToDataUrl(bitmap, maxDim = 320, quality = 82)
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
        val cachedPath = saveBitmap(context, bitmap, prefix = "prod", subDir = "products") // local cache
        return if (dataUrl.isNotBlank() && dataUrl != "hero_local_mart") dataUrl else cachedPath
    }

    fun isLocalFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val clean = path.removePrefix("file:")
        return if (clean.startsWith("/")) File(clean).exists() else false
    }
}
