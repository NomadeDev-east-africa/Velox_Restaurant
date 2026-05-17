package com.example.velox_restaurant.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

fun Context.isValidImageUri(uri: Uri): Boolean {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    return try {
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        opts.outWidth > 0 && opts.outHeight > 0
    } catch (_: Exception) {
        false
    }
}

fun Context.compressImageUri(uri: Uri, maxDimension: Int = 1080, quality: Int = 85): ByteArray {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

    val rawMax = maxOf(opts.outWidth, opts.outHeight)
    opts.inSampleSize = if (rawMax > maxDimension) {
        var s = 1
        while (rawMax / (s * 2) >= maxDimension) s *= 2
        s
    } else 1
    opts.inJustDecodeBounds = false

    val bmp = contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: error("Impossible de décoder l'image")

    return ByteArrayOutputStream().also { out ->
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bmp.recycle()
    }.toByteArray()
}
