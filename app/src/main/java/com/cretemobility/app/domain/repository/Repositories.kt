package com.cretemobility.app.domain.repository

import com.cretemobility.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ─── Journey Planning Repository ─────────────────────────────────────────────

interface JourneyRepository {
    suspend fun planJourney(request: JourneyRequest): Result<List<Journey>>
    suspend fun planJourneyOffline(request: JourneyRequest): Result<List<Journey>>
    fun getLastJourneys(): Flow<List<JourneyRequest>>
}

// ─── Stops Repository ────────────────────────────────────────────────────────

interface StopsRepository {
    fun getAllStops(): Flow<List<TransitStop>>
    fun getStopsByRegion(region: CreteRegion): Flow<List<TransitStop>>
    suspend fun getStopById(id: String): TransitStop?
    suspend fun getNearbyStops(location: LatLng, radiusMeters: Int): List<TransitStop>
    suspend fun searchStops(query: String): List<TransitStop>
    suspend fun refreshStops(): Result<Unit>
    fun observeStopDepartures(stopId: String): Flow<List<Departure>>
    suspend fun getDepartures(stopId: String, date: LocalDate): List<Departure>
}

// ─── Lines Repository ────────────────────────────────────────────────────────

interface LinesRepository {
    fun getAllLines(): Flow<List<TransitLine>>
    fun getLinesByOperator(operator: Operator): Flow<List<TransitLine>>
    suspend fun getLineById(id: String): TransitLine?
    suspend fun refreshLines(): Result<Unit>
}

// ─── Favorites Repository ────────────────────────────────────────────────────

interface FavoritesRepository {
    fun getFavoriteRoutes(): Flow<List<FavoriteRoute>>
    fun getFavoriteStops(): Flow<List<FavoriteStop>>
    suspend fun addFavoriteRoute(route: FavoriteRoute)
    suspend fun removeFavoriteRoute(id: Long)
    suspend fun addFavoriteStop(stop: FavoriteStop)
    suspend fun removeFavoriteStop(id: Long)
    suspend fun updateRouteUsage(id: Long)
    suspend fun isFavoriteStop(stopId: String): Boolean
}

// ─── Real-time Repository ────────────────────────────────────────────────────

interface RealtimeRepository {
    fun observeVehiclePositions(lineId: String): Flow<List<VehiclePosition>>
    fun observeServiceAlerts(): Flow<List<ServiceAlert>>
    suspend fun getVehiclePosition(vehicleId: String): VehiclePosition?
    fun startRealtimeUpdates()
    fun stopRealtimeUpdates()
}

// ─── Search Repository ───────────────────────────────────────────────────────

interface SearchRepository {
    suspend fun search(query: String, location: LatLng? = null): List<SearchResult>
    suspend fun searchLandmarks(query: String): List<SearchResult>
    suspend fun searchBeaches(query: String): List<SearchResult>
    suspend fun searchHotels(query: String): List<SearchResult>
    suspend fun reverseGeocode(location: LatLng): SearchResult?
    fun getRecentSearches(): Flow<List<SearchResult>>
    suspend fun saveRecentSearch(result: SearchResult)
    suspend fun clearRecentSearches()
}

// ─── Location Repository ─────────────────────────────────────────────────────

interface LocationRepository {
    fun observeCurrentLocation(): Flow<LatLng?>
    suspend fun getLastKnownLocation(): LatLng?
    fun startLocationUpdates()
    fun stopLocationUpdates()
}

// ─── Settings Repository ─────────────────────────────────────────────────────

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTouristMode(enabled: Boolean)
    suspend fun setRoutePreference(preference: RoutePreference)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun clearCache()
}
