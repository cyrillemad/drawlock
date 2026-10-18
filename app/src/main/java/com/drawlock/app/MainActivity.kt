package com.drawlock.app

import android.app.ActivityManager
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** How long "volume down" must be held to leave lock mode. */
private const val UNLOCK_HOLD_MILLIS = 2000L

class MainActivity : ComponentActivity() {

    private val vm: TracingViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val unlockAfterHold = Runnable { unlock() }

    private val pickImage = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) vm.pickImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TracingScreen(
                    vm = vm,
                    onPickImage = { pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    onLock = ::lock,
                )
            }
        }
        if (vm.locked) applyLockedWindow(true)
    }

    private fun lock() {
        vm.locked = true
        applyLockedWindow(true)
        // App pinning blocks back/home gestures from a pencil near the screen edge.
        // Android asks the user to confirm it.
        runCatching { startLockTask() }
    }

    private fun unlock() {
        handler.removeCallbacks(unlockAfterHold)
        vm.locked = false
        applyLockedWindow(false)
        val activityManager = getSystemService(ActivityManager::class.java)
        if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            runCatching { stopLockTask() }
        }
    }

    private fun applyLockedWindow(locked: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (locked) {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            } else {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
        if (locked) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        requestedOrientation = if (locked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        setSystemBarsHidden(locked)
    }

    private fun setSystemBarsHidden(hidden: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs (e.g. the pinning prompt) and swipes bring the bars back; hide them again.
        if (hasFocus && vm.locked) setSystemBarsHidden(true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        if (!vm.locked || !isVolumeKey) return super.dispatchKeyEvent(event)

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            when (event.action) {
                KeyEvent.ACTION_DOWN ->
                    if (event.repeatCount == 0) handler.postDelayed(unlockAfterHold, UNLOCK_HOLD_MILLIS)
                KeyEvent.ACTION_UP -> handler.removeCallbacks(unlockAfterHold)
            }
        }
        return true // Volume stays unchanged while locked.
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(unlockAfterHold)
    }
}
