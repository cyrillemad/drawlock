package com.drawlock.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val UNLOCK_HINT_MILLIS = 3000L

@Composable
fun TracingScreen(vm: TracingViewModel, onPickImage: () -> Unit, onLock: () -> Unit) {
    // While locked, back does nothing; leaving is done with the volume key.
    BackHandler(enabled = vm.locked) {}

    Box(Modifier.fillMaxSize().background(Color.White)) {
        val image = vm.image
        if (image != null) {
            TracedImage(vm, image)
        } else {
            Text(
                text = stringResource(if (vm.loadFailed) R.string.load_failed else R.string.pick_image_hint),
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        if (vm.locked) {
            UnlockHint(Modifier.align(Alignment.TopCenter))
        } else {
            Toolbar(vm, onPickImage, onLock, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TracedImage(vm: TracingViewModel, image: ImageBitmap) {
    val gestures = if (vm.locked) {
        Modifier // Touches through the paper must not move the image.
    } else {
        Modifier.pointerInput(Unit) {
            // Finger twist is ignored on purpose: the angle is set precisely in the toolbar.
            detectTransformGestures { centroid, pan, zoom, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                vm.transform = vm.transform.applyGesture(center, centroid, pan, zoom)
            }
        }
    }
    Box(Modifier.fillMaxSize().then(gestures)) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = if (vm.grayscale) grayscaleContrast(vm.contrast) else null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = vm.transform
                    translationX = t.offset.x
                    translationY = t.offset.y
                    scaleX = if (vm.mirrored) -t.scale else t.scale
                    scaleY = t.scale
                    rotationZ = t.rotation
                },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Toolbar(
    vm: TracingViewModel,
    onPickImage: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (vm.image != null) AngleControls(vm)
        if (vm.grayscale) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.contrast), color = Color.White)
                Slider(
                    value = vm.contrast,
                    onValueChange = { vm.contrast = it },
                    valueRange = 1f..4f,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = onPickImage) { Text(stringResource(R.string.pick_image)) }
            FilterChip(
                selected = vm.mirrored,
                onClick = { vm.mirrored = !vm.mirrored },
                label = { Text(stringResource(R.string.mirror)) },
            )
            FilterChip(
                selected = vm.grayscale,
                onClick = { vm.grayscale = !vm.grayscale },
                label = { Text(stringResource(R.string.grayscale)) },
            )
            FilledTonalButton(onClick = vm::reset) { Text(stringResource(R.string.reset)) }
            Button(onClick = onLock, enabled = vm.image != null) { Text(stringResource(R.string.lock)) }
        }
    }
}

/** Whole-degree angle: fine steps with buttons, coarse with the slider. */
@Composable
private fun AngleControls(vm: TracingViewModel) {
    val angle = vm.transform.rotation
    val setAngle = { degrees: Float -> vm.transform = vm.transform.withRotation(degrees) }
    val compact = PaddingValues(horizontal = 10.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.angle, angle.roundToInt()),
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { setAngle(angle - 1f) }, contentPadding = compact) { Text("−1°") }
        TextButton(onClick = { setAngle(angle + 1f) }, contentPadding = compact) { Text("+1°") }
        TextButton(onClick = { setAngle(angle + 90f) }, contentPadding = compact) { Text("+90°") }
        TextButton(onClick = { setAngle(0f) }, contentPadding = compact) { Text("0°") }
    }
    Slider(
        value = angle,
        onValueChange = { setAngle(it.roundToInt().toFloat()) },
        valueRange = -180f..180f,
    )
}

@Composable
private fun UnlockHint(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(UNLOCK_HINT_MILLIS)
        visible = false
    }
    if (visible) {
        Text(
            text = stringResource(R.string.unlock_hint),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = modifier
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Black & white with contrast stretched around mid-gray, so lines show through paper better. */
private fun grayscaleContrast(contrast: Float): ColorFilter {
    val r = 0.2126f * contrast
    val g = 0.7152f * contrast
    val b = 0.0722f * contrast
    val shift = 128f * (1f - contrast)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                r, g, b, 0f, shift,
                r, g, b, 0f, shift,
                r, g, b, 0f, shift,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
