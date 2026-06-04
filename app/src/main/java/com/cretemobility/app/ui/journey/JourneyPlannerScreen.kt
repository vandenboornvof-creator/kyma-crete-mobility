package com.cretemobility.app.ui.journey

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cretemobility.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyPlannerScreen(
    viewModel: JourneyPlannerViewModel = hiltViewModel(),
    onNavigateToResults: (String, String, String) -> Unit,
    onNavigateToMap: (Double, Double) -> Unit,
    onNavigateToDepartures: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToResults.collect { (origin, destination) ->
            onNavigateToResults(
                origin.toJson(),
                destination.toJson(),
                "now"
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.Directions, null) },
                    label = { Text("Plan") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToMap(35.3387, 25.1442) },
                    icon = { Icon(Icons.Outlined.Map, null) },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToFavorites,
                    icon = { Icon(Icons.Outlined.Star, null) },
                    label = { Text("Saved") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { JourneyHeader() }

            item {
                JourneyInputCard(
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }

            item {
                PreferenceChips(
                    selected = state.preference,
                    onSelect = { viewModel.onEvent(JourneyPlannerEvent.SetPreference(it)) }
                )
            }

            // Search results
            if (state.searchResults.isNotEmpty()) {
                items(state.searchResults) { result ->
                    SearchResultItem(
                        result = result,
                        onClick = {
                            val location = Location(
                                name = result.name,
                                nameEl = result.nameEl,
                                latLng = result.location,
                                type = result.type
                            )
                            if (state.isSearchingOrigin) {
                                viewModel.onEvent(JourneyPlannerEvent.SetOrigin(location))
                            } else {
                                viewModel.onEvent(JourneyPlannerEvent.SetDestination(location))
                            }
                        }
                    )
                }
            } else {
                // Nearby stops
                if (state.nearbyStops.isNotEmpty()) {
                    item {
                        Text(
                            "Nearby Stops",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(state.nearbyStops.take(5)) { stop ->
                        NearbyStopCard(
                            stop = stop,
                            onStopClick = { onNavigateToDepartures(stop.id) },
                            onAddAsOrigin = {
                                viewModel.onEvent(JourneyPlannerEvent.SetOrigin(
                                    Location(stop.name, stop.nameEl, stop.location, stop.id, LocationType.STOP)
                                ))
                            },
                            onAddAsDestination = {
                                viewModel.onEvent(JourneyPlannerEvent.SetDestination(
                                    Location(stop.name, stop.nameEl, stop.location, stop.id, LocationType.STOP)
                                ))
                            }
                        )
                    }
                }
            }
        }
    }

    // Error dialog
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(JourneyPlannerEvent.ClearError) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(JourneyPlannerEvent.ClearError) }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun JourneyHeader() {
    Column {
        Text(
            text = "🏖️ Crete Transit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Plan your journey across Crete",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyInputCard(
    state: JourneyPlannerState,
    onEvent: (JourneyPlannerEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Origin field
            LocationField(
                label = "From",
                value = state.origin?.name ?: "",
                hint = "Origin — tap to search or use GPS",
                icon = Icons.Filled.RadioButtonChecked,
                iconColor = MaterialTheme.colorScheme.primary,
                onValueChange = { query ->
                    onEvent(JourneyPlannerEvent.UpdateSearch(query, forOrigin = true))
                },
                onGpsClick = { onEvent(JourneyPlannerEvent.UseCurrentLocationAsOrigin) },
                hasGps = true,
                isActive = state.isSearchingOrigin
            )

            // Swap button + divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onEvent(JourneyPlannerEvent.SwapLocations) }
                ) {
                    Icon(
                        Icons.Filled.SwapVert,
                        contentDescription = "Swap",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Destination field
            LocationField(
                label = "To",
                value = state.destination?.name ?: "",
                hint = "Destination — beach, hotel, town...",
                icon = Icons.Filled.Place,
                iconColor = MaterialTheme.colorScheme.error,
                onValueChange = { query ->
                    onEvent(JourneyPlannerEvent.UpdateSearch(query, forOrigin = false))
                },
                hasGps = false,
                isActive = !state.isSearchingOrigin
            )

            // Plan button
            Button(
                onClick = { onEvent(JourneyPlannerEvent.PlanJourney) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = state.origin != null && state.destination != null
            ) {
                Icon(Icons.Filled.Directions, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Find Routes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationField(
    label: String,
    value: String,
    hint: String,
    icon: ImageVector,
    iconColor: Color,
    onValueChange: (String) -> Unit,
    onGpsClick: (() -> Unit)? = null,
    hasGps: Boolean,
    isActive: Boolean
) {
    var text by remember { mutableStateOf(value) }

    LaunchedEffect(value) { text = value }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(hint, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (hasGps && onGpsClick != null) {
                IconButton(onClick = onGpsClick) {
                    Icon(Icons.Filled.MyLocation, "Use GPS", tint = MaterialTheme.colorScheme.primary)
                }
            } else if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    onValueChange("")
                }) {
                    Icon(Icons.Filled.Clear, "Clear", modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun PreferenceChips(
    selected: RoutePreference,
    onSelect: (RoutePreference) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        listOf(
            RoutePreference.FASTEST to "Fastest",
            RoutePreference.LEAST_WALKING to "Less Walk",
            RoutePreference.CHEAPEST to "Cheapest",
            RoutePreference.FEWEST_TRANSFERS to "Direct"
        ).forEach { (pref, label) ->
            FilterChip(
                selected = selected == pref,
                onClick = { onSelect(pref) },
                label = { Text(label, fontSize = 12.sp) },
                leadingIcon = if (selected == pref) {
                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(result.name, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(result.description, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(
                imageVector = when (result.type) {
                    LocationType.STOP, LocationType.CURRENT_LOCATION -> Icons.Filled.DirectionsBus
                    LocationType.FERRY_PORT -> Icons.Filled.DirectionsBoat
                    LocationType.AIRPORT -> Icons.Filled.FlightTakeoff
                    LocationType.BEACH -> Icons.Filled.BeachAccess
                    LocationType.HOTEL -> Icons.Filled.Hotel
                    else -> Icons.Filled.Place
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun NearbyStopCard(
    stop: TransitStop,
    onStopClick: () -> Unit,
    onAddAsOrigin: () -> Unit,
    onAddAsDestination: () -> Unit
) {
    Card(
        onClick = onStopClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (stop.type) {
                            StopType.FERRY_PORT -> Icons.Filled.DirectionsBoat
                            StopType.AIRPORT -> Icons.Filled.FlightTakeoff
                            else -> Icons.Filled.DirectionsBus
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stop.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${stop.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }} • ${stop.region?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Crete"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onAddAsOrigin, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.TripOrigin, "Set as origin", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onAddAsDestination, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Place, "Set as destination",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun Location.toJson(): String {
    return android.util.Base64.encodeToString(
        com.google.gson.Gson().toJson(this).toByteArray(),
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
    )
}
