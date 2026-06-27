package com.bludood.rotatemycover

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bludood.rotatemycover.ui.theme.RotateMyCoverTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RotationManager.init(this)
        enableEdgeToEdge()

        setContent {
            RotateMyCoverTheme {
                MainContainer()
            }
        }
    }
}

@Composable
fun MainContainer() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                accessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isSetupComplete = overlayGranted && accessibilityEnabled
    var wasOnboardingShown by remember { mutableStateOf(false) }

    LaunchedEffect(isSetupComplete) {
        if (!isSetupComplete) {
            wasOnboardingShown = true
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = isSetupComplete,
                label = "ScreenTransition"
            ) { complete ->
                if (!complete) {
                    OnboardingScreen(overlayGranted, accessibilityEnabled)
                } else {
                    DashboardScreen(overlayGranted, accessibilityEnabled, wasOnboardingShown)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(overlayGranted: Boolean, accessibilityEnabled: Boolean) {
    val context = LocalContext.current
    val isDeviceSupported = remember {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val pm = context.packageManager
        displayManager.displays.size > 1 || 
                pm.hasSystemFeature("com.samsung.android.feature.cover_display") ||
                pm.hasSystemFeature("com.samsung.android.feature.sub_display") ||
                pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isDeviceSupported) {
            DeviceWarning()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(0.5f))
        
        Text(
            text = "Welcome to RotateMyCover!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Grant these permissions to start rotating your cover screen.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        PermissionItem(
            title = "Overlay Permission",
            subtitle = "Allows drawing over other apps",
            isGranted = overlayGranted,
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            title = "Accessibility Service",
            subtitle = "Keeps the rotation service active. Does not access screen contents.",
            isGranted = accessibilityEnabled,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (overlayGranted && accessibilityEnabled) {
            Text(
                text = "Everything is ready",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DashboardScreen(
    overlayGranted: Boolean,
    accessibilityEnabled: Boolean,
    showEnjoyOverride: Boolean = false
) {
    var isAutoRotationEnabled by remember { mutableStateOf(RotationManager.isAutoRotationEnabled) }
    var keepRotationOnDisable by remember { mutableStateOf(RotationManager.keepRotationOnDisable) }
    var followSystemRotation by remember { mutableStateOf(RotationManager.followSystemRotation) }
    var statusExpanded by remember { mutableStateOf(!overlayGranted || !accessibilityEnabled) }
    val context = LocalContext.current

    val isDeviceSupported = remember {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val pm = context.packageManager
        displayManager.displays.size > 1 || 
                pm.hasSystemFeature("com.samsung.android.feature.cover_display") ||
                pm.hasSystemFeature("com.samsung.android.feature.sub_display") ||
                pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
    }

    val initialHasSeenEnjoy = remember { RotationManager.hasSeenEnjoy }
    val showEnjoy = !initialHasSeenEnjoy || showEnjoyOverride

    LaunchedEffect(Unit) {
        RotationManager.setHasSeenEnjoy(context, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (Static)
        Text(
            text = if (showEnjoy) "Enjoy RotateMyCover!" else "RotateMyCover",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Text(
            text = if (isAutoRotationEnabled) "Automatic rotation is active!" else "Automatic rotation is disabled",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isDeviceSupported) {
                DeviceWarning()
                Spacer(modifier = Modifier.height(24.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusExpanded = !statusExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Permission Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (statusExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = statusExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            PermissionItem(
                                title = "Overlay Permission",
                                isGranted = overlayGranted,
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PermissionItem(
                                title = "Accessibility Service",
                                isGranted = accessibilityEnabled,
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle Options
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Follow system rotation lock",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Disable cover rotation when system auto-rotate is off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = followSystemRotation,
                        onCheckedChange = { 
                            RotationManager.setFollowSystemRotation(context, it)
                            followSystemRotation = it
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep rotation after disabling",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Freeze current orientation instead of resetting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = keepRotationOnDisable,
                        onCheckedChange = { 
                            RotationManager.setKeepRotationOnDisable(context, it)
                            keepRotationOnDisable = it
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer Button (Static)
        Button(
            onClick = {
                if (isAutoRotationEnabled) {
                    RotationManager.disableAutoRotation(context)
                } else {
                    RotationManager.enableAutoRotation(context)
                }
                isAutoRotationEnabled = RotationManager.isAutoRotationEnabled
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isAutoRotationEnabled) "Disable Automatic Rotation" else "Enable Automatic Rotation",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun DeviceWarning() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Multiple displays not detected. This app is designed specifically for foldable devices and will not work on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                MaterialTheme.colorScheme.surfaceContainerHighest 
            else 
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Missing",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    for (service in enabledServices) {
        if (service.resolveInfo.serviceInfo.packageName == context.packageName) {
            return true
        }
    }
    return false
}
