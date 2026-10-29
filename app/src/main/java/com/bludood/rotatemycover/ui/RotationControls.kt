package com.bludood.rotatemycover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bludood.rotatemycover.R

@Composable
internal fun RotationButton(enabled: Boolean, onClick: () -> Unit) {
    val size = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = size),
        shapes = ButtonDefaults.shapesFor(size),
        contentPadding = ButtonDefaults.contentPaddingFor(size, hasStartIcon = true)
    ) {
        Icon(
            painterResource(R.drawable.ms_screen_rotation_24),
            null,
            Modifier.size(ButtonDefaults.iconSizeFor(size))
        )
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(size)))
        Text(
            stringResource(if (enabled) R.string.disable_rotation else R.string.enable_rotation),
            style = ButtonDefaults.textStyleFor(size),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun RotationSwitchRow(
    title: String,
    description: String,
    icon: Int,
    checked: Boolean,
    index: Int,
    onChecked: (Boolean) -> Unit
) {
    val shapes = ListItemDefaults.segmentedShapes(index, 2)
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val resting = ListItemDefaults.segmentedColors()
    val feedback = LocalHapticFeedback.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth / LocalDensity.current.fontScale < 240.dp
        val switch: @Composable () -> Unit = { Switch(checked = checked, onCheckedChange = null) }
        SegmentedListItem(
            checked = checked,
            onCheckedChange = {
                onChecked(it)
                feedback.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            },
            shapes = shapes.copy(selectedShape = shapes.shape),
            colors = ListItemDefaults.segmentedColors(
                containerColor = surface, selectedContainerColor = surface,
                selectedContentColor = resting.contentColor,
                selectedLeadingContentColor = resting.leadingContentColor,
                selectedSupportingContentColor = resting.supportingContentColor,
                selectedTrailingContentColor = resting.trailingContentColor
            ),
            modifier = Modifier.semantics { role = Role.Switch },
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = if (stacked) null else ({ Icon(painterResource(icon), null) }),
            trailingContent = if (stacked) null else switch,
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(description)
                    if (stacked) switch()
                }
            }
        ) { Text(title) }
    }
}
