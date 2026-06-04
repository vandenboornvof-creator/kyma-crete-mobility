package com.cretemobility.app.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.LocationRepository
import com.cretemobility.app.domain.repository.SettingsRepository
import com.cretemobility.app.domain.usecase.journey.*
import com.cretemobility.app.domain.usecase.stops.GetNearbyStopsUseCase
import com.cretemobility.app.domain.usecase.stops.SearchStopsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Journey Planner State ────────────────────────────────────────────────────

data class JourneyPlannerState(
    val origin: Location? = null,
    val destination: Location? = null,
    val departureTime: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    val arriveBy: Boolean = false,
    val preference: RoutePreference = RoutePreference.FASTEST,
    val nearbyStops: List<TransitStop> = emptyList(),
    val searchResults: List<SearchResult> = emptyList(),
    val searchQuery: String = "",
    val isSearchingOrigin: Boolean = false,
    val isLoadingNearby: Boolean = false,
    val currentLocation: LatLng? = null,
    val error: String? = null
)

sealed class JourneyPlannerEvent {
    data class SetOrigin(val location: Location) : JourneyPlannerEvent()
    data class SetDestination(val location: Location) : JourneyPlannerEvent()
    data class UpdateSearch(val query: String, val forOrigin: Boolean) : JourneyPlannerEvent()
    object SwapLocations : JourneyPlannerEvent()
    object UseCurrentLocationAsOrigin : JourneyPlannerEvent()
    data class SetPreference(val preference: RoutePreference) : JourneyPlannerEvent()
    data class SetDepartureTime(val time: java.time.LocalDateTime) : JourneyPlannerEvent()
    object ClearError : JourneyPlannerEvent()
    object PlanJourney : JourneyPlannerEvent()
}

// ─── Journey Planner ViewModel ────────────────────────────────────────────────

@HiltViewModel
class JourneyPlannerViewModel @Inject constructor(
    private val getNearbyStopsUseCase: GetNearbyStopsUseCase,
    private val searchStopsUseCase: SearchStopsUseCase,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JourneyPlannerState())
    val state: StateFlow<JourneyPlannerState> = _state.asStateFlow()

    private val _navigateToResults = MutableSharedFlow<Pair<Location, Location>>()
    val navigateToResults = _navigateToResults.asSharedFlow()

    init {
        observeLocation()
        loadNearbyStops()
        loadSettings()
    }

    fun onEvent(event: JourneyPlannerEvent) {
        when (event) {
            is JourneyPlannerEvent.SetOrigin -> {
                _state.update { it.copy(origin = event.location, searchResults = emptyList()) }
            }
            is JourneyPlannerEvent.SetDestination -> {
                _state.update { it.copy(destination = event.location, searchResults = emptyList()) }
            }
            is JourneyPlannerEvent.UpdateSearch -> {
                _state.update { it.copy(
                    searchQuery = event.query,
                    isSearchingOrigin = event.forOrigin
                )}
                searchLocations(event.query)
            }
            is JourneyPlannerEvent.SwapLocations -> {
                _state.update {
                    it.copy(origin = it.destination, destination = it.origin)
                }
            }
            is JourneyPlannerEvent.UseCurrentLocationAsOrigin -> {
                val loc = _state.value.currentLocation ?: return
                _state.update {
                    it.copy(
                        origin = Location(
                            name = "My Location",
                            nameEl = "Τοποθεσία μου",
                            latLng = loc,
                            type = LocationType.CURRENT_LOCATION
                        )
                    )
                }
            }
            is JourneyPlannerEvent.SetPreference -> {
                _state.update { it.copy(preference = event.preference) }
            }
            is JourneyPlannerEvent.SetDepartureTime -> {
                _state.update { it.copy(departureTime = event.time) }
            }
            is JourneyPlannerEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is JourneyPlannerEvent.PlanJourney -> {
                planJourney()
            }
        }
    }

    private fun observeLocation() {
        locationRepository.observeCurrentLocation()
            .onEach { location ->
                _state.update { it.copy(currentLocation = location) }
                if (_state.value.nearbyStops.isEmpty()) {
                    loadNearbyStops()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadNearbyStops() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingNearby = true) }
            val stops = getNearbyStopsUseCase(radiusMeters = 500)
            _state.update { it.copy(nearbyStops = stops, isLoadingNearby = false) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _state.update { it.copy(preference = settings.defaultRoutePreference) }
        }
    }

    private fun searchLocations(query: String) {
        if (query.length < 2) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val stops = searchStopsUseCase(query)
            val searchResults = stops.map { stop ->
                SearchResult(
                    id = stop.id,
                    name = stop.name,
                    nameEl = stop.nameEl,
                    description = "${stop.type.displayName} • ${stop.region?.displayName ?: "Crete"}",
                    location = stop.location,
                    type = when (stop.type) {
                        StopType.FERRY_PORT -> LocationType.FERRY_PORT
                        StopType.AIRPORT -> LocationType.AIRPORT
                        else -> LocationType.STOP
                    }
                )
            }
            _state.update { it.copy(searchResults = searchResults) }
        }
    }

    private fun planJourney() {
        val origin = _state.value.origin
        val destination = _state.value.destination
        if (origin == null || destination == null) {
            _state.update { it.copy(error = "Please set both origin and destination") }
            return
        }
        viewModelScope.launch {
            _navigateToResults.emit(Pair(origin, destination))
        }
    }
}

val StopType.displayName: String
    get() = when (this) {
        StopType.BUS_STOP -> "Bus Stop"
        StopType.BUS_STATION -> "Bus Station"
        StopType.FERRY_PORT -> "Ferry Port"
        StopType.TAXI_RANK -> "Taxi"
        StopType.LANDMARK -> "Landmark"
        StopType.AIRPORT -> "Airport"
    }

val CreteRegion.displayName: String
    get() = when (this) {
        CreteRegion.HERAKLION -> "Heraklion"
        CreteRegion.CHANIA -> "Chania"
        CreteRegion.RETHYMNO -> "Rethymno"
        CreteRegion.LASITHI -> "Lasithi"
    }
