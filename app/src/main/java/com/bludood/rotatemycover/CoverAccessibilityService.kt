package com.bludood.rotatemycover

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class CoverAccessibilityService : AccessibilityService() {

    private var receiver: BroadcastReceiver? = null
    private var rotationObserver: ContentObserver? = null
    private var overlayView: View? = null
    private var currentDisplayId: Int = -1

    override fun onServiceConnected() {
        super.onServiceConnected()
        RotationManager.init(this)
        RotationManager.setListener { 
            refreshOverlay()
        }
        registerScreenReceiver()
        registerRotationObserver()
        refreshOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            refreshOverlay()
        }
    }

    override fun onInterrupt() = Unit

    private fun refreshOverlay() {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        
        val coverDisplay = displays.find { it.displayId != Display.DEFAULT_DISPLAY } ?: return
        
        if (coverDisplay.state == Display.STATE_ON) {
            addOverlayToDisplay(coverDisplay)
        } else {
            removeOverlay()
        }
    }

    private fun addOverlayToDisplay(display: Display) {
        val targetOrientation = RotationManager.getTargetOrientation(this)

        overlayView?.let { view ->
            if (currentDisplayId == display.displayId) {
                val windowManager = view.context.getSystemService(WINDOW_SERVICE) as WindowManager
                val params = view.layoutParams as WindowManager.LayoutParams
                
                if (params.screenOrientation != targetOrientation) {
                    params.screenOrientation = targetOrientation
                    runCatching {
                        windowManager.updateViewLayout(view, params)
                    }
                }
                return
            }
        }

        removeOverlay()

        val windowContext = createDisplayContext(display).createWindowContext(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            null,
        )
        val windowManager = windowContext.getSystemService(WINDOW_SERVICE) as WindowManager

        val view = View(windowContext)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            screenOrientation = targetOrientation
            alpha = 0.01f
        }

        runCatching {
            windowManager.addView(view, params)
            overlayView = view
            currentDisplayId = display.displayId
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            val windowManager = view.context.getSystemService(WINDOW_SERVICE) as WindowManager
            runCatching {
                windowManager.removeView(view)
            }
        }
        overlayView = null
        currentDisplayId = -1
    }

    private fun registerScreenReceiver() {
        if (receiver != null) return

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshOverlay()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun registerRotationObserver() {
        if (rotationObserver != null) return
        
        rotationObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshOverlay()
            }
        }
        
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            rotationObserver!!
        )
    }

    override fun onDestroy() {
        RotationManager.setListener(null)
        removeOverlay()
        receiver?.let {
            unregisterReceiver(it)
        }
        receiver = null
        
        rotationObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        rotationObserver = null

        super.onDestroy()
    }
}
