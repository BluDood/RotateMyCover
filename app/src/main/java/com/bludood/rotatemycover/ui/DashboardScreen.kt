package com.bludood.rotatemycover.ui

import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bludood.rotatemycover.R
import com.bludood.rotatemycover.RotationManager

@Composable
internal fun DashboardScreen(overlayGranted: Boolean, accessibilityEnabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var autoRotation by remember { mutableStateOf(RotationManager.isAutoRotationEnabled) }
    var keepRotation by remember { mutableStateOf(RotationManager.keepRotationOnDisable) }
    var followSystem by remember { mutableStateOf(RotationManager.followSystemRotation) }
    var targetOrientation by remember {
        mutableIntStateOf(
            RotationManager.getTargetOrientation(
                context
            )
        )
    }
    var statusExpanded by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val supported = rememberDeviceSupport()

    DisposableEffect(context, lifecycleOwner) {
        fun refresh() {
            autoRotation = RotationManager.isAutoRotationEnabled
            keepRotation = RotationManager.keepRotationOnDisable
            followSystem = RotationManager.followSystemRotation
            targetOrientation = RotationManager.getTargetOrientation(context)
        }

        val rotationObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            rotationObserver
        )
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            context.contentResolver.unregisterContentObserver(rotationObserver)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    LaunchedEffect(context) { RotationManager.setHasSeenEnjoy(context, true) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll, enabled = scroll.maxValue > 0),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(rotationStatus(autoRotation, targetOrientation)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                if (!supported) {
                    DeviceWarning()
                    Spacer(Modifier.height(24.dp))
                }
                PermissionStatus(overlayGranted, accessibilityEnabled, statusExpanded) {
                    statusExpanded = !statusExpanded
                }
                Spacer(Modifier.height(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    RotationSwitchRow(
                        stringResource(R.string.follow_system),
                        stringResource(R.string.follow_system_description),
                        R.drawable.ms_screen_lock_rotation_filled_24,
                        followSystem,
                        0
                    ) {
                        RotationManager.setFollowSystemRotation(context, it)
                        followSystem = it
                        targetOrientation = RotationManager.getTargetOrientation(context)
                    }
                    RotationSwitchRow(
                        stringResource(R.string.keep_orientation),
                        stringResource(R.string.keep_orientation_description),
                        R.drawable.ms_screen_lock_portrait_filled_24,
                        keepRotation,
                        1
                    ) {
                        RotationManager.setKeepRotationOnDisable(context, it)
                        keepRotation = it
                        targetOrientation = RotationManager.getTargetOrientation(context)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(16.dp))
            RotationButton(autoRotation) {
                if (autoRotation) RotationManager.disableAutoRotation(context)
                else RotationManager.enableAutoRotation(context)
                autoRotation = RotationManager.isAutoRotationEnabled
                targetOrientation = RotationManager.getTargetOrientation(context)
            }
        }
    }
}

internal fun rotationStatus(enabled: Boolean, targetOrientation: Int): Int = when {
    enabled && targetOrientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR -> R.string.rotation_enabled
    enabled -> R.string.rotation_paused
    targetOrientation == ActivityInfo.SCREEN_ORIENTATION_LOCKED -> R.string.rotation_off_keep
    else -> R.string.rotation_off_portrait
}
