package com.drawlock.app

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max

/** Big enough to stay sharp when zoomed in, small enough not to run out of memory. */
private const val MAX_IMAGE_SIDE = 3072

/** Decodes [uri], downscaling so the longest side is at most [MAX_IMAGE_SIDE]. Blocking. */
fun decodeScaledImage(resolver: ContentResolver, uri: Uri): ImageBitmap {
    val source = ImageDecoder.createSource(resolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        val ratio = max(width, height).toFloat() / MAX_IMAGE_SIDE
        if (ratio > 1f) {
            decoder.setTargetSize((width / ratio).toInt(), (height / ratio).toInt())
        }
    }.asImageBitmap()
}
