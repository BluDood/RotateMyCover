package com.bludood.rotatemycover

import android.content.Context
import android.content.pm.ActivityInfo
import android.provider.Settings
import androidx.core.content.edit

object RotationManager {
    private const val PREFS_NAME = "rotation_prefs"
    private const val KEY_AUTO_ROTATE = "auto_rotate"
    private const val KEY_KEEP_ROTATION = "keep_rotation"
    private const val KEY_FOLLOW_SYSTEM = "follow_system"
    private const val KEY_HAS_SEEN_ENJOY = "has_seen_enjoy"

    private var _isAutoRotationEnabled = true
    val isAutoRotationEnabled: Boolean get() = _isAutoRotationEnabled

    private var _keepRotationOnDisable = false
    val keepRotationOnDisable: Boolean get() = _keepRotationOnDisable

    private var _followSystemRotation = true
    val followSystemRotation: Boolean get() = _followSystemRotation

    private var _hasSeenEnjoy = false
    val hasSeenEnjoy: Boolean get() = _hasSeenEnjoy

    private var listener: ((Boolean) -> Unit)? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isAutoRotationEnabled = prefs.getBoolean(KEY_AUTO_ROTATE, true)
        _keepRotationOnDisable = prefs.getBoolean(KEY_KEEP_ROTATION, false)
        _followSystemRotation = prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)
        _hasSeenEnjoy = prefs.getBoolean(KEY_HAS_SEEN_ENJOY, false)
    }

    fun setListener(listener: ((Boolean) -> Unit)?) {
        this.listener = listener
    }

    fun enableAutoRotation(context: Context) {
        _isAutoRotationEnabled = true
        saveState(context)
        listener?.invoke(true)
    }

    fun disableAutoRotation(context: Context) {
        _isAutoRotationEnabled = false
        saveState(context)
        listener?.invoke(false)
    }

    fun setKeepRotationOnDisable(context: Context, keep: Boolean) {
        _keepRotationOnDisable = keep
        saveState(context)
    }

    fun setFollowSystemRotation(context: Context, follow: Boolean) {
        _followSystemRotation = follow
        saveState(context)
        listener?.invoke(true) // Trigger refresh
    }

    fun setHasSeenEnjoy(context: Context, seen: Boolean) {
        _hasSeenEnjoy = seen
        saveState(context)
    }

    private fun saveState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_AUTO_ROTATE, _isAutoRotationEnabled)
            putBoolean(KEY_KEEP_ROTATION, _keepRotationOnDisable)
            putBoolean(KEY_FOLLOW_SYSTEM, _followSystemRotation)
            putBoolean(KEY_HAS_SEEN_ENJOY, _hasSeenEnjoy)
        }
    }

    fun getTargetOrientation(context: Context): Int {
        val systemAutoRotate = try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1
        } catch (_: Exception) {
            true
        }

        val effectiveAutoRotate = _isAutoRotationEnabled && (!_followSystemRotation || systemAutoRotate)

        return if (effectiveAutoRotate) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        } else if (_keepRotationOnDisable) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
