package com.cretemobility.app.ui.departures

import androidx.compose.foundation.background
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
import com.cretemobility.app.domain.repository.FavoritesRepository
import com.cretemobility.app.domain.repository.StopsRepository
import com.cretemobility.app.domain.usecase.stops.GetDeparturesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Departures ViewModel ─────────────────────────────────────────────────────

data class DeparturesState(
    val stop: TransitStop? = null,
    val departures: List<Departure> = emptyList(),
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null,
    val lastUpdated: LocalDateTime? = null
)

@HiltViewModel
class DeparturesViewModel @Inject constructor(
    private val stopsRepository: StopsRepository,
    private val getDeparturesUseCase: GetDeparturesUseCase,
    private val favoritesRepository: FavoritesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stopId = savedStateHandle.get<String>("stopId") ?: ""
    private val _state = MutableStateFlow(DeparturesState())
    val state: StateFlow<DeparturesState> = _state.asStateFlow()

    init {
        loadStop()
        loadDepartures()
    }

    private fun loadStop() {
        viewModelScope.launch {
            val stop = stopsRepository.getStopById(stopId)
            val isFav = favoritesRepository.isFavoriteStop(stopId)
            _state.update { it.copy(stop = stop, isFavorite = isFav) }
        }
    }

    private fun loadDepartures() {
        getDeparturesUseCase(stopId)
            .onEach { departures ->
                _state.update {
                    it.copy(
                        departures = departures,
                        isLoading = false,
                        lastUpdated = LocalDateTime.now()
                    )
                }
            }
            .catch { e ->
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite() {
        val stop = _state.value.stop ?: return
        viewModelScope.launch {
            if (_state.value.isFavorite) {
                favoritesRepository.removeFavoriteStop(stop.id.hashCode().toLong())
            } else {
                favoritesRepository.addFavoriteStop(
                    FavoriteStop(stop = stop, addedAt = LocalDateTime.now())
                )
            }
            _state.update { it.copy(isFavorite = !it.isFavorite) }
        }
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        loadDepartures()
    }
}

// ─── Departures Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    stopId: String,
    viewModel: DeparturesViewModel = hiltViewModel(),
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
                            state.stop?.name ?: "Stop",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        state.stop?.let {
                            Text(
                                it.type.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            "Favorite",
                            tint = if (state.isFavorite) Color(0xFFF9A825) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.stop?.let { stop ->
                        IconButton(onClick = {
                            onNavigateToMap(stop.location.latitude, stop.location.longitude)
                        }) {
                            Icon(Icons.Filled.Map, "Show on map")
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingDepartures(modifier = Modifier.padding(padding))
            state.error != null -> ErrorDepartures(state.error!!, modifier = Modifier.padding(padding))
            state.departures.isEmpty() -> NoDepartures(modifier = Modifier.padding(padding))
            else -> DeparturesList(
                departures = state.departures,
                lastUpdated = state.lastUpdated,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DeparturesList(
    departures: List<Departure>,
    lastUpdated: LocalDateTime?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lastUpdated?.let {
            item {
                Text(
                    "Updated ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        items(departures) { departure ->
            DepartureRow(departure)
        }
    }
}

@Composable
private fun DepartureRow(departure: Departure) {
    val now = LocalTime.now()
    val minutesUntil = java.time.Duration.between(now, departure.displayTime).toMinutes().toInt()

    val statusColor = when {
        departure.isCancelled -> MaterialTheme.colorScheme.error
        departure.isDelayed -> Color(0xFFF57C00)
        minutesUntil <= 2 -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line badge
            Surface(
                color = Color(android.graphics.Color.parseColor(
                    departure.line.color.let { if (it.startsWith("#")) it else "#$it" }
                )),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(width = 48.dp, height = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        departure.line.shortName,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    departure.headsign,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (departure.isRealtime) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "LIVE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (departure.isDelayed) {
                        Text(
                            "+${departure.delayMinutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF57C00)
                        )
                    }
                    departure.platformCode?.let {
                        Text(
                            "Platform $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (departure.isCancelled) "Cancelled"
                    else departure.displayTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (departure.isCancelled) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when {
                        departure.isCancelled -> ""
                        minutesUntil <= 0 -> "Now"
                        minutesUntil == 1 -> "1 min"
                        minutesUntil < 60 -> "$minutesUntil min"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingDepartures(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading departures...")
        }
    }
}

@Composable
private fun ErrorDepartures(error: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.Error, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text(error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NoDepartures(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🚌", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text("No more departures today", style = MaterialTheme.typography.titleMedium)
            Text("Check back tomorrow", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun StopType.displayName() = when (this) {
    StopType.BUS_STOP -> "Bus Stop"
    StopType.BUS_STATION -> "Bus Station"
    StopType.FERRY_PORT -> "Ferry Port"
    StopType.AIRPORT -> "Airport"
    StopType.TAXI_RANK -> "Taxi Stand"
    StopType.LANDMARK -> "Landmark"
}
