package com.example.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.location.LocationService
import com.example.BuildConfig
import com.example.ui.components.rememberLocationPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Checkout map picker adapted from Google's android-places-compose landmark-selection sample.
 * The pin follows map taps, the camera follows device location, and Android's geocoder fills
 * editable address fields before the coordinates are confirmed.
 */
@Composable
fun AddressMapPicker(
    initialLatitude: Double,
    initialLongitude: Double,
    onBack: () -> Unit,
    onConfirm: (addressLine: String, locality: String, latitude: Double, longitude: Double) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember(context) { LocationService.create(context) }
    val permissionState = rememberLocationPermissionState()
    var selectedLocation by remember { mutableStateOf(LatLng(initialLatitude, initialLongitude)) }
    var addressLine by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val markerState = rememberMarkerState(position = selectedLocation)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 16f)
    }

    suspend fun selectLocation(location: LatLng) {
        selectedLocation = location
        markerState.position = location
        cameraState.animate(CameraUpdateFactory.newLatLngZoom(location, 16f))
        isLoading = true
        message = null
        val result = runCatching {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
            }
        }
        result.onSuccess { address ->
            addressLine = address?.getAddressLine(0).orEmpty()
            locality = listOfNotNull(address?.subLocality, address?.locality, address?.adminArea)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
            if (addressLine.isBlank()) message = "Address was not found. Enter it manually."
        }.onFailure {
            message = "Could not look up this address. Enter it manually."
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { selectLocation(selectedLocation) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Select delivery location", style = MaterialTheme.typography.titleMedium)
        }

        Box(modifier = Modifier.fillMaxWidth().height(330.dp)) {
            if (BuildConfig.MAPS_CONFIGURED) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize().testTag("delivery_address_map"),
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = permissionState.hasAnyLocationPermission),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
                    onMapClick = { location -> scope.launch { selectLocation(location) } }
                ) {
                    Marker(
                        state = markerState,
                        title = "Delivery location",
                        snippet = addressLine.ifBlank { "Tap the map to move this pin" }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Map is not configured", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Use your current location or enter the delivery address manually below.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                onClick = {
                    if (!permissionState.hasAnyLocationPermission) {
                        permissionState.requestPermissions()
                    } else if (!permissionState.isGpsEnabled) {
                        permissionState.openLocationSettings()
                    } else {
                        scope.launch {
                            isLoading = true
                            locationService.getCurrentLocation().fold(
                                onSuccess = { selectLocation(LatLng(it.latitude, it.longitude)) },
                                onFailure = { message = it.message ?: "Current location is unavailable" }
                            )
                            isLoading = false
                        }
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Use current location")
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (BuildConfig.MAPS_CONFIGURED) "Tap the map to place the delivery pin."
                else "Enter the address manually. Coordinates can still come from your device location.",
                style = MaterialTheme.typography.bodySmall
            )
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Text("  Looking up address…", style = MaterialTheme.typography.bodySmall)
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(
                value = addressLine,
                onValueChange = { addressLine = it },
                label = { Text("Full address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = locality,
                onValueChange = { locality = it },
                label = { Text("Locality / city") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onConfirm(
                        addressLine.trim(),
                        locality.trim(),
                        selectedLocation.latitude,
                        selectedLocation.longitude
                    )
                },
                enabled = addressLine.length >= 5 && locality.length >= 2 && !isLoading && !isSaving,
                modifier = Modifier.fillMaxWidth().testTag("confirm_delivery_address")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else Text("Confirm delivery address")
            }
        }
    }
}
