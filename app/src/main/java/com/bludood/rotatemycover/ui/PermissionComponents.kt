package com.bludood.rotatemycover.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bludood.rotatemycover.R

@Composable
internal fun PermissionStatus(
    overlayGranted: Boolean,
    accessibilityEnabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val ready = overlayGranted && accessibilityEnabled
    val state = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
    val angle by animateFloatAsState(
        if (expanded) 180f else 0f,
        MotionScheme.standard().fastSpatialSpec(), label = "Permission arrow"
    )
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        SegmentedListItem(
            onClick = onClick,
            shapes = ListItemDefaults.segmentedShapes(0, if (expanded) 3 else 1),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { stateDescription = state },
            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            leadingContent = if (ready) ({
                Icon(
                    painterResource(R.drawable.ms_check_24),
                    null
                )
            }) else null,
            trailingContent = {
                Icon(
                    painterResource(R.drawable.ms_arrow_drop_down_24),
                    null,
                    Modifier.rotate(angle)
                )
            }
        ) { Text(stringResource(if (ready) R.string.permissions_ready else R.string.permissions_required)) }
        AnimatedVisibility(
            expanded,
            enter = expandVertically(spatial, expandFrom = Alignment.Top) + fadeIn(effects),
            exit = shrinkVertically(spatial, shrinkTowards = Alignment.Top) + fadeOut(effects)
        ) {
            PermissionItems(
                overlayGranted,
                accessibilityEnabled,
                showDescriptions = false,
                indexOffset = 1
            )
        }
    }
}

@Composable
internal fun PermissionItems(
    overlayGranted: Boolean,
    accessibilityEnabled: Boolean,
    showDescriptions: Boolean,
    indexOffset: Int = 0
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        PermissionItem(
            stringResource(R.string.overlay_permission),
            if (showDescriptions) stringResource(R.string.overlay_description) else null,
            overlayGranted,
            stringResource(if (overlayGranted) R.string.allowed else R.string.allow),
            indexOffset,
            indexOffset + 2
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
            )
        }
        PermissionItem(
            stringResource(R.string.accessibility_service),
            if (showDescriptions) stringResource(R.string.accessibility_description) else null,
            accessibilityEnabled,
            stringResource(if (accessibilityEnabled) R.string.enabled else R.string.open_settings),
            indexOffset + 1,
            indexOffset + 2
        ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String?,
    granted: Boolean,
    action: String,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth / LocalDensity.current.fontScale < 240.dp
        val actionLabel: @Composable () -> Unit = {
            Text(
                action, style = MaterialTheme.typography.labelLarge,
                color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
        SegmentedListItem(
            onClick = onClick,
            shapes = ListItemDefaults.segmentedShapes(index, count),
            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically,
            supportingContent = if (description != null || stacked) ({
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (description != null) Text(description)
                    if (stacked) actionLabel()
                }
            }) else null,
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!stacked) actionLabel()
                    Icon(
                        painterResource(if (granted) R.drawable.ms_check_24 else R.drawable.ms_keyboard_arrow_right_24),
                        null
                    )
                }
            }
        ) { Text(title) }
    }
}

internal fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        .any {
            it.resolveInfo.serviceInfo.packageName == context.packageName
        }
}
