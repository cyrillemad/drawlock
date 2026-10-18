package com.drawlock.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Position of the image on screen. The image is drawn centered in its container, then
 * scaled and rotated around its center and finally moved by [offset].
 * [rotation] is in degrees, clockwise, matching `graphicsLayer.rotationZ`.
 */
data class ImageTransform(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    /** Where the content point [local] (relative to the image center) ends up on screen. */
    fun toScreen(center: Offset, local: Offset): Offset =
        center + offset + local.rotateBy(rotation) * scale

    /**
     * Applies one step of a pan/zoom gesture so that the content point under [centroid]
     * stays under the fingers: it ends up at `centroid + pan`. The angle is not touched:
     * it is set precisely with [withRotation] instead.
     */
    fun applyGesture(center: Offset, centroid: Offset, pan: Offset, zoom: Float): ImageTransform {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val effectiveZoom = newScale / scale
        val fromImageCenter = centroid - center - offset
        val newOffset = centroid - center + pan - fromImageCenter * effectiveZoom
        return copy(offset = newOffset, scale = newScale)
    }

    /** Rotates around the image center; the angle is wrapped into -180..180. */
    fun withRotation(degrees: Float): ImageTransform {
        var wrapped = degrees % 360f
        if (wrapped > 180f) wrapped -= 360f
        if (wrapped < -180f) wrapped += 360f
        return copy(rotation = wrapped)
    }

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 20f
    }
}

/** Rotates clockwise on screen (y axis points down). */
fun Offset.rotateBy(degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return Offset(x * c - y * s, x * s + y * c)
}
