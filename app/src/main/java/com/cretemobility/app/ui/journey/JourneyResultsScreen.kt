package com.cretemobility.app.ui.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.usecase.journey.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Results ViewModel ────────────────────────────────────────────────────────

data class JourneyResultsState(
    val journeys: List<Journey> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val origin: Location? = null,
    val destination: Location? = null,
    val selectedPreference: RoutePreference = RoutePreference.FASTEST
)

@HiltViewModel
class JourneyResultsViewModel @Inject constructor(
    private val planJourneyUseCase: PlanJourneyUseCase,
    private val sortJourneysUseCase: SortJourneysUseCase,
    private val getJourneyDetailsUseCase: GetJourneyDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val originJson = savedStateHandle.get<String>("origin") ?: ""
    private val destinationJson = savedStateHandle.get<String>("destination") ?: ""

    private val _state = MutableStateFlow(JourneyResultsState())
    val state: StateFlow<JourneyResultsState> = _state.asStateFlow()

    private var allJourneys = emptyList<Journey>()

    init {
        loadJourneys()
    }

    private fun loadJourneys() {
        viewModelScope.launch {
            val origin = originJson.fromJson(Location::class.java) ?: return@launch
            val destination = destinationJson.fromJson(Location::class.java) ?: return@launch

            _state.update { it.copy(
                isLoading = true,
                origin = origin,
                destination = destination
            )}

            val request = JourneyRequest(
                origin = origin,
                destination = destination,
                preference = _state.value.selectedPreference
            )

            planJourneyUseCase(request).fold(
                onSuccess = { journeys ->
                    allJourneys = journeys
                    _state.update { it.copy(
                        journeys = sortJourneysUseCase(journeys, it.selectedPreference),
                        isLoading = false
                    )}
                },
                onFailure = { e ->
                    _state.update { it.copy(
                        error = e.message ?: "Could not find routes. Check your connection.",
                        isLoading = false
                    )}
                }
            )
        }
    }

    fun onPreferenceChanged(preference: RoutePreference) {
        _state.update { it.copy(
            selectedPreference = preference,
            journeys = sortJourneysUseCase(allJourneys, preference)
        )}
    }

    fun getWalkingSummary(journey: Journey) = getJourneyDetailsUseCase.getWalkingSummary(journey)
    fun getPriceSummary(journey: Journey) = getJourneyDetailsUseCase.getPriceSummary(journey)
}

fun <T> String.fromJson(clazz: Class<T>): T? = try {
    val json = String(android.util.Base64.decode(this, android.util.Base64.URL_SAFE))
    com.google.gson.Gson().fromJson(json, clazz)
} catch (e: Exception) { null }

// ─── Results Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyResultsScreen(
    originJson: String,
    destinationJson: String,
    time: String,
    viewModel: JourneyResultsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${state.origin?.name ?: "..."} → ${state.destination?.name ?: "..."}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            "Available routes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Preference selector
            PreferenceTabRow(
                selected = state.selectedPreference,
                onSelect = viewModel::onPreferenceChanged
            )

            when {
                state.isLoading -> LoadingView()
                state.error != null -> ErrorView(state.error!!, onRetry = {})
                state.journeys.isEmpty() -> NoRoutesView()
                else -> JourneyList(
                    journeys = state.journeys,
                    walkingSummary = viewModel::getWalkingSummary,
                    priceSummary = viewModel::getPriceSummary
                )
            }
        }
    }
}

@Composable
private fun PreferenceTabRow(
    selected: RoutePreference,
    onSelect: (RoutePreference) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = RoutePreference.values().indexOf(selected),
        edgePadding = 16.dp
    ) {
        listOf(
            RoutePreference.FASTEST to "⚡ Fastest",
            RoutePreference.LEAST_WALKING to "🚶 Less Walk",
            RoutePreference.CHEAPEST to "💶 Cheapest",
            RoutePreference.FEWEST_TRANSFERS to "🎯 Direct"
        ).forEachIndexed { index, (pref, label) ->
            Tab(
                selected = selected == pref,
                onClick = { onSelect(pref) },
                text = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun JourneyList(
    journeys: List<Journey>,
    walkingSummary: (Journey) -> String,
    priceSummary: (Journey) -> String?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(journeys) { journey ->
            JourneyCard(
                journey = journey,
                walkSummary = walkingSummary(journey),
                priceSummary = priceSummary(journey)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyCard(
    journey: Journey,
    walkSummary: String,
    priceSummary: String?
) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: times + duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        journey.departureTime.format(fmt),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " → ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        journey.arrivalTime.format(fmt),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${journey.totalDuration} min",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Mode icons strip
            LegStrip(journey.legs)

            Spacer(Modifier.height(8.dp))

            // Meta row: walking, transfers, price
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetaChip(Icons.Filled.DirectionsWalk, walkSummary)
                if (journey.transferCount > 0) {
                    MetaChip(Icons.Filled.SyncAlt, "${journey.transferCount} change${if (journey.transferCount > 1) "s" else ""}")
                }
                priceSummary?.let { MetaChip(Icons.Filled.Euro, it) }
            }

            // Expanded detail
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                journey.legs.forEach { leg ->
                    LegDetailRow(leg)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun LegStrip(legs: List<JourneyLeg>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        legs.forEachIndexed { i, leg ->
            LegPill(leg)
            if (i < legs.size - 1) {
                Icon(
                    Icons.Filled.ArrowForward,
                    null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegPill(leg: JourneyLeg) {
    val (icon, color, text) = when (leg.mode) {
        TransitMode.BUS -> Triple(Icons.Filled.DirectionsBus, Color(0xFF1565C0), leg.line?.shortName ?: "Bus")
        TransitMode.FERRY -> Triple(Icons.Filled.DirectionsBoat, Color(0xFF006064), "Ferry")
        TransitMode.WALK -> Triple(Icons.Filled.DirectionsWalk, Color(0xFF2E7D32), "${leg.distance}m")
        TransitMode.TAXI -> Triple(Icons.Filled.LocalTaxi, Color(0xFFF9A825), "Taxi")
        TransitMode.BICYCLE -> Triple(Icons.Filled.DirectionsBike, Color(0xFF4CAF50), "Bike")
        TransitMode.SCOOTER -> Triple(Icons.Filled.TwoWheeler, Color(0xFF9C27B0), "Scooter")
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegDetailRow(leg: JourneyLeg) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                leg.from.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (leg.mode != TransitMode.WALK) {
                Text(
                    "Take ${leg.line?.shortName ?: ""} toward ${leg.headsign ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Walk ${leg.distance}m (${leg.duration} min)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            leg.departureTime.format(fmt),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Finding best routes...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.WifiOff,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Try Again") }
        }
    }
}

@Composable
private fun NoRoutesView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🗺️", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "No routes found between these locations.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Try adjusting your origin or destination, or increasing the walking distance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
