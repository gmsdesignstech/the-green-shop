package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPaleGreen
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow
import com.example.ui.theme.GreenShopYellowBright
import com.example.ui.theme.GreenShopYellowLight

/**
 * Status representation for runtime location permissions following Android best practices.
 */
enum class LocationPermissionStatus {
    /** Permissions have not yet been prompted or determined. */
    NOT_REQUESTED,

    /** User granted fine/precise location (best experience for real-time map tracking). */
    GRANTED_PRECISE,

    /** User granted approximate location (Android 12+ approximate choice). */
    GRANTED_APPROXIMATE,

    /** User soft-denied permission; rationale should be shown before requesting again. */
    RATIONALE_NEEDED,

    /** User denied and selected "Don't ask again" or denied repeatedly. Must direct to Settings. */
    PERMANENTLY_DENIED,

    /** Permissions granted, but device-wide location/GPS is turned off. */
    GPS_DISABLED
}

/**
 * State holder for location permission handling in Jetpack Compose.
 */
@Stable
class LocationPermissionState(
    val status: LocationPermissionStatus,
    val hasAnyLocationPermission: Boolean,
    val isPrecise: Boolean,
    val isGpsEnabled: Boolean,
    val requestPermissions: () -> Unit,
    val openAppSettings: () -> Unit,
    val openLocationSettings: () -> Unit,
    val refresh: () -> Unit
)

/**
 * Safely resolves the nearest Activity from a given Context.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Creates and remembers a [LocationPermissionState] adhering to Android runtime permission guidelines.
 * Requests both ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION simultaneously to give users
 * full choice on Android 12+ while supporting backward compatibility.
 * Automatically updates when the app returns from background or system Settings.
 */
@Composable
fun rememberLocationPermissionState(
    onPermissionResult: ((granted: Boolean, isPrecise: Boolean) -> Unit)? = null
): LocationPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasFine by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCoarse by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isGpsEnabled by remember {
        mutableStateOf(isDeviceLocationEnabled(context))
    }

    var hasBeenPromptedOnce by remember { mutableStateOf(false) }

    fun refreshInternal() {
        hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        isGpsEnabled = isDeviceLocationEnabled(context)
    }

    // Refresh when activity resumes (e.g., returning from Android Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshInternal()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBeenPromptedOnce = true
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasFine = fineGranted
        hasCoarse = coarseGranted
        isGpsEnabled = isDeviceLocationEnabled(context)

        val anyGranted = fineGranted || coarseGranted
        onPermissionResult?.invoke(anyGranted, fineGranted)
    }

    val activity = context.findActivity()

    val status = when {
        hasFine && !isGpsEnabled -> LocationPermissionStatus.GPS_DISABLED
        hasCoarse && !isGpsEnabled -> LocationPermissionStatus.GPS_DISABLED
        hasFine -> LocationPermissionStatus.GRANTED_PRECISE
        hasCoarse -> LocationPermissionStatus.GRANTED_APPROXIMATE
        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) -> LocationPermissionStatus.RATIONALE_NEEDED
        hasBeenPromptedOnce -> LocationPermissionStatus.PERMANENTLY_DENIED
        else -> LocationPermissionStatus.NOT_REQUESTED
    }

    return remember(status, hasFine, hasCoarse, isGpsEnabled) {
        LocationPermissionState(
            status = status,
            hasAnyLocationPermission = hasFine || hasCoarse,
            isPrecise = hasFine,
            isGpsEnabled = isGpsEnabled,
            requestPermissions = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            openAppSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            openLocationSettings = {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            refresh = { refreshInternal() }
        )
    }
}

/**
 * Checks if the device location services (GPS or Network provider) are enabled.
 */
private fun isDeviceLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

/**
 * A production-ready, reusable Modal Dialog explaining why location access is needed
 * before or during the permission request flow. Adheres to Google's Material 3 guidelines.
 */
@Composable
fun LocationPermissionRationaleDialog(
    permissionState: LocationPermissionState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    customTitle: String? = null,
    customSubtitle: String? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, GreenShopLightBorder, RoundedCornerShape(28.dp))
                .testTag("location_permission_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Badge
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(GreenShopMintSurface)
                        .border(2.dp, GreenShopPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (permissionState.status == LocationPermissionStatus.GPS_DISABLED) {
                            Icons.Default.GpsFixed
                        } else {
                            Icons.Default.LocationOn
                        },
                        contentDescription = "Location Access",
                        tint = GreenShopDarkGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                val title = when (permissionState.status) {
                    LocationPermissionStatus.PERMANENTLY_DENIED ->
                        stringResource(R.string.location_denied_permanent_title)
                    LocationPermissionStatus.GPS_DISABLED ->
                        stringResource(R.string.location_gps_disabled_title)
                    else ->
                        customTitle ?: stringResource(R.string.location_rationale_title)
                }

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenShopDarkText,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Narrative
                val subtitle = when (permissionState.status) {
                    LocationPermissionStatus.PERMANENTLY_DENIED ->
                        stringResource(R.string.location_denied_permanent_desc)
                    LocationPermissionStatus.GPS_DISABLED ->
                        stringResource(R.string.location_gps_disabled_desc)
                    else ->
                        customSubtitle ?: stringResource(R.string.location_rationale_subtitle)
                }

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = GreenShopMutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Benefit Highlights
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GreenShopPaleGreen)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RationaleFeatureItem(
                        icon = Icons.Default.Speed,
                        title = stringResource(R.string.location_feature_live_tracking_title),
                        description = stringResource(R.string.location_feature_live_tracking_desc)
                    )

                    RationaleFeatureItem(
                        icon = Icons.Default.NearMe,
                        title = stringResource(R.string.location_feature_doorstep_title),
                        description = stringResource(R.string.location_feature_doorstep_desc)
                    )

                    RationaleFeatureItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.location_feature_privacy_title),
                        description = stringResource(R.string.location_feature_privacy_desc)
                    )
                }

                // Approximate location hint if approximate is granted
                if (permissionState.status == LocationPermissionStatus.GRANTED_APPROXIMATE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenShopYellowLight)
                            .border(1.dp, GreenShopYellowBright, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.location_approximate_notice),
                            fontSize = 11.sp,
                            color = GreenShopDarkText,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                when (permissionState.status) {
                    LocationPermissionStatus.PERMANENTLY_DENIED -> {
                        Button(
                            onClick = { permissionState.openAppSettings() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("location_permission_settings_button"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.location_btn_open_settings),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    LocationPermissionStatus.GPS_DISABLED -> {
                        Button(
                            onClick = { permissionState.openLocationSettings() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("location_enable_gps_button"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.location_btn_enable_gps),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = { permissionState.requestPermissions() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("location_permission_allow_button"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.location_btn_allow),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dismiss / Secondary Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("location_permission_dismiss_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GreenShopMutedText
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
                ) {
                    Text(
                        text = stringResource(R.string.location_btn_not_now),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * An inline, non-modal Card component explaining location benefits.
 * Can be embedded directly within screens like OrderTrackingScreen, Address Selector, or Checkout.
 */
@Composable
fun LocationPermissionRationaleCard(
    permissionState: LocationPermissionState,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("location_rationale_card"),
        colors = CardDefaults.cardColors(containerColor = GreenShopMintSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopPrimary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GreenShopWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Access",
                            tint = GreenShopDarkGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Delivery Map Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GreenShopDarkText
                        )
                        Text(
                            text = "Pinpoint delivery rider arrival in real-time",
                            fontSize = 11.sp,
                            color = GreenShopMutedText
                        )
                    }
                }

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("location_card_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = GreenShopMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enable location access to see the live courier scooter icon moving towards your doorstep and receive accurate arrival countdowns.",
                fontSize = 12.sp,
                color = GreenShopDarkText,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (permissionState.status) {
                    LocationPermissionStatus.PERMANENTLY_DENIED -> {
                        Button(
                            onClick = { permissionState.openAppSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("location_card_settings_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                        ) {
                            Text("Open Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LocationPermissionStatus.GPS_DISABLED -> {
                        Button(
                            onClick = { permissionState.openLocationSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("location_card_gps_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                        ) {
                            Text("Turn On GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        Button(
                            onClick = { permissionState.requestPermissions() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("location_card_allow_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                        ) {
                            Text("Enable Live Tracking", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (onDismiss != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("location_card_not_now_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenShopMutedText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
                    ) {
                        Text("Not Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * A compact, persistent banner informing users of location precision status.
 */
@Composable
fun LocationPrecisionBanner(
    isPrecise: Boolean,
    onRequestPrecise: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isPrecise) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GreenShopYellowLight)
                .border(1.dp, GreenShopYellowBright, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("location_approximate_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "📍", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Approximate location active. Switch to Precise for live turn tracking.",
                    fontSize = 11.sp,
                    color = GreenShopDarkText,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upgrade",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GreenShopDarkGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GreenShopWhite)
                    .clickable { onRequestPrecise() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Single educational bullet point row inside the rationale container.
 */
@Composable
private fun RationaleFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GreenShopWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GreenShopPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GreenShopDarkText
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = GreenShopMutedText,
                lineHeight = 15.sp
            )
        }
    }
}
