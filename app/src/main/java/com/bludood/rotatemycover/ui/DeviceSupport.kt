package com.bludood.rotatemycover.ui

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bludood.rotatemycover.R

@Composable
internal fun DeviceWarning() {
    ListItem(
        leadingContent = { Icon(painterResource(R.drawable.ms_info_24), null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Text(stringResource(R.string.device_notice)) }
}

@Composable
internal fun rememberDeviceSupport(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val pm = context.packageManager
        displayManager.displays.size > 1 ||
                pm.hasSystemFeature("com.samsung.android.feature.cover_display") ||
                pm.hasSystemFeature("com.samsung.android.feature.sub_display") ||
                pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
    }
}
