package es.uc3m.android.chillmates.shoppinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import es.uc3m.android.chillmates.R

data class Supermarket(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun SelectSupermarketScreen(
    supermarkets: List<Supermarket>,
    initialSelectedIds: Set<String> = emptySet(),
    onSupermarketSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCamera = supermarkets.firstOrNull()?.let {
        LatLng(it.latitude, it.longitude)
    } ?: LatLng(40.4168, -3.7038)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCamera, if (supermarkets.isNotEmpty()) 14f else 11f)
    }

    var selectedIds by remember(initialSelectedIds) { mutableStateOf(initialSelectedIds) }
    val selectedMarkets = supermarkets.filter { it.id in selectedIds }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_supermarket),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (supermarkets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.map_no_supermarkets),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    ) {
                        supermarkets.forEach { market ->
                            val isSelected = market.id in selectedIds
                            val markerState = rememberUpdatedMarkerState(
                                position = LatLng(market.latitude, market.longitude)
                            )
                            Marker(
                                state = markerState,
                                title = market.name,
                                snippet = market.id,
                                alpha = if (isSelected) 1f else 0.85f,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (isSelected) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_RED
                                ),
                                onClick = {
                                    selectedIds = if (isSelected) {
                                        selectedIds - market.id
                                    } else {
                                        selectedIds + market.id
                                    }
                                    true
                                }
                            )
                        }
                    }
                }
            }

            if (selectedMarkets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedMarkets, key = { it.id }) { market ->
                        AssistChip(
                            onClick = {
                                selectedIds = selectedIds - market.id
                            },
                            label = { Text(market.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }

                Button(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        onSupermarketSelected(selectedIds.toList())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            }
        }
    }
}