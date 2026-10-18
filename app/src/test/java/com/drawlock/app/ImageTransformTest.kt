package com.drawlock.app

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageTransformTest {

    private val center = Offset(500f, 1000f)

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    /** Finds the content point (relative to the image center) that is drawn at [screen]. */
    private fun ImageTransform.contentAt(screen: Offset): Offset =
        (screen - center - offset).rotateBy(-rotation) / scale

    @Test
    fun pan_movesImageByPan() {
        val result = ImageTransform().applyGesture(center, Offset(100f, 100f), Offset(30f, -20f), 1f)
        assertOffsetEquals(Offset(30f, -20f), result.offset)
    }

    @Test
    fun zoom_keepsPointUnderFingersInPlaceAndAngleUnchanged() {
        val start = ImageTransform(offset = Offset(40f, -60f), scale = 1.5f, rotation = 20f)
        val centroid = Offset(300f, 700f)
        val pan = Offset(15f, 25f)
        val grabbed = start.contentAt(centroid)

        val result = start.applyGesture(center, centroid, pan, zoom = 1.3f)

        assertEquals(1.95f, result.scale, 0.001f)
        assertEquals(20f, result.rotation, 0.001f)
        assertOffsetEquals(result.toScreen(center, grabbed), centroid + pan)
    }

    @Test
    fun scale_isClamped() {
        val result = ImageTransform(scale = ImageTransform.MAX_SCALE)
            .applyGesture(center, center, Offset.Zero, zoom = 2f)
        assertEquals(ImageTransform.MAX_SCALE, result.scale, 0.001f)
    }

    @Test
    fun withRotation_wrapsIntoMinus180To180() {
        assertEquals(-170f, ImageTransform().withRotation(190f).rotation, 0.001f)
        assertEquals(170f, ImageTransform().withRotation(-190f).rotation, 0.001f)
        assertEquals(0f, ImageTransform().withRotation(360f).rotation, 0.001f)
        assertEquals(180f, ImageTransform().withRotation(180f).rotation, 0.001f)
        assertEquals(-180f, ImageTransform().withRotation(-180f).rotation, 0.001f)
    }

    @Test
    fun withRotation_keepsOffsetAndScale() {
        val start = ImageTransform(offset = Offset(10f, 20f), scale = 2f, rotation = 5f)
        val result = start.withRotation(95f)
        assertOffsetEquals(start.offset, result.offset)
        assertEquals(2f, result.scale, 0.001f)
    }
}
