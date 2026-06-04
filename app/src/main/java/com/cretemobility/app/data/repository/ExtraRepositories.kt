package com.cretemobility.app.data.repository

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cretemobility.app.data.local.dao.CachedJourneyDao
import com.cretemobility.app.data.local.dao.StopDao
import com.cretemobility.app.data.local.entity.CachedJourneyEntity
import com.cretemobility.app.data.remote.api.OtpApi
import com.cretemobility.app.data.remote.dto.*
import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.*
import com.google.android.gms.location.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

// ─── Journey Repository ───────────────────────────────────────────────────────

@Singleton
class JourneyRepositoryImpl @Inject constructor(
    private val otpApi: OtpApi,
    private val stopDao: StopDao,
    private val cachedJourneyDao: CachedJourneyDao,
    private val gson: Gson
) : JourneyRepository {

    override suspend fun planJourney(request: JourneyRequest): Result<List<Journey>> =
        runCatching {
            val from = "${request.origin.latLng.latitude},${request.origin.latLng.longitude}"
            val to = "${request.destination.latLng.latitude},${request.destination.latLng.longitude}"
            val date = request.departureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = request.departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            val mode = request.modes.joinToString(",") { it.toOtpMode() }

            val response = otpApi.plan(
                fromPlace = from,
                toPlace = to,
                date = date,
                time = time,
                arriveBy = request.arriveBy,
                mode = mode,
                maxWalkDistance = request.maxWalkDistance,
                numItineraries = 5
            )

            response.plan?.itineraries?.map { it.toDomain(request) }
                ?: throw Exception("No routes found")
        }

    override suspend fun planJourneyOffline(request: JourneyRequest): Result<List<Journey>> =
        runCatching {
            // Build a simple walk-only journey as fallback
            val distance = haversineDistance(
                request.origin.latLng, request.destination.latLng
            )
            val walkMinutes = (distance / 80).toInt() // ~80m/minute average walking

            val walkJourney = Journey(
                id = "offline_walk_${System.currentTimeMillis()}",
                legs = listOf(
                    JourneyLeg(
                        mode = TransitMode.WALK,
                        from = request.origin,
                        to = request.destination,
                        departureTime = request.departureTime,
                        arrivalTime = request.departureTime.plusMinutes(walkMinutes.toLong()),
                        duration = walkMinutes,
                        distance = distance.toInt()
                    )
                ),
                totalDuration = walkMinutes,
                totalWalkDistance = distance.toInt(),
                totalPrice = null,
                departureTime = request.departureTime,
                arrivalTime = request.departureTime.plusMinutes(walkMinutes.toLong()),
                transferCount = 0,
                preference = request.preference
            )
            listOf(walkJourney)
        }

    override fun getLastJourneys(): Flow<List<JourneyRequest>> = flow { emit(emptyList()) }

    private fun haversineDistance(from: LatLng, to: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

fun TransitMode.toOtpMode() = when (this) {
    TransitMode.BUS -> "BUS"
    TransitMode.FERRY -> "FERRY"
    TransitMode.WALK -> "WALK"
    TransitMode.BICYCLE -> "BICYCLE"
    else -> "WALK"
}

fun OtpItinerary.toDomain(request: JourneyRequest): Journey {
    val legs = this.legs.map { it.toDomainLeg() }
    return Journey(
        id = "otp_${startTimeMs}",
        legs = legs,
        totalDuration = (duration / 60).toInt(),
        totalWalkDistance = walkDistance.toInt(),
        totalPrice = fare?.fare?.values?.firstOrNull()?.let { it.cents / 100.0 },
        departureTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startTimeMs), ZoneId.systemDefault()
        ),
        arrivalTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endTimeMs), ZoneId.systemDefault()
        ),
        transferCount = transfers,
        preference = request.preference
    )
}

fun OtpLeg.toDomainLeg(): JourneyLeg {
    val mode = when (mode.uppercase()) {
        "BUS" -> TransitMode.BUS
        "FERRY" -> TransitMode.FERRY
        "WALK" -> TransitMode.WALK
        "BICYCLE" -> TransitMode.BICYCLE
        else -> TransitMode.WALK
    }
    return JourneyLeg(
        mode = mode,
        from = from.toDomainLocation(),
        to = to.toDomainLocation(),
        departureTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startTimeMs), ZoneId.systemDefault()
        ),
        arrivalTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endTimeMs), ZoneId.systemDefault()
        ),
        duration = (duration / 60).toInt(),
        distance = distance.toInt(),
        line = if (mode != "WALK") TransitLine(
            id = route ?: "unknown",
            shortName = routeShortName ?: "",
            longName = routeLongName ?: "",
            mode = when (mode.uppercase()) {
                "BUS" -> TransitMode.BUS
                "FERRY" -> TransitMode.FERRY
                else -> TransitMode.BUS
            },
            operator = Operator.UNKNOWN,
            color = routeColor?.let { "#$it" } ?: "#1E88E5",
            textColor = routeTextColor?.let { "#$it" } ?: "#FFFFFF",
            region = CreteRegion.HERAKLION
        ) else null,
        headsign = headsign,
        isRealtime = realTime,
        instructions = steps?.map { step ->
            WalkInstruction(
                text = "${step.relativeDirection} on ${step.streetName}",
                distance = step.distance.toInt(),
                bearing = 0.0,
                streetName = step.streetName
            )
        } ?: emptyList()
    )
}

fun OtpPlace.toDomainLocation() = Location(
    name = name,
    latLng = LatLng(lat, lon),
    stopId = stopId
)

// ─── Location Repository ──────────────────────────────────────────────────────

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun observeCurrentLocation(): Flow<LatLng?> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).apply {
            setMinUpdateIntervalMillis(3000L)
            setMinUpdateDistanceMeters(10f)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(LatLng(loc.latitude, loc.longitude))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, callback, context.mainLooper)
        } catch (e: SecurityException) {
            Timber.w(e, "Location permission not granted")
            trySend(null)
        }

        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LatLng? {
        return try {
            kotlinx.coroutines.tasks.await(fusedLocationClient.lastLocation)?.let { loc ->
                LatLng(loc.latitude, loc.longitude)
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not get last known location")
            null
        }
    }

    override fun startLocationUpdates() { /* managed via Flow */ }
    override fun stopLocationUpdates() { /* managed via Flow */ }
}

// ─── Settings Repository ──────────────────────────────────────────────────────

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val TOURIST_MODE = booleanPreferencesKey("tourist_mode")
        val ROUTE_PREFERENCE = stringPreferencesKey("route_preference")
        val MAX_WALK_DISTANCE = intPreferencesKey("max_walk_distance")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_BUS_WARNING = booleanPreferencesKey("last_bus_warning")
        val LAST_BUS_WARNING_MINUTES = intPreferencesKey("last_bus_warning_minutes")
        val OFFLINE_MAPS = booleanPreferencesKey("offline_maps")
        val MAP_PROVIDER = stringPreferencesKey("map_provider")
    }

    override fun observeSettings(): Flow<AppSettings> =
        context.dataStore.data.map { prefs -> prefs.toSettings() }

    override suspend fun getSettings(): AppSettings =
        context.dataStore.data.first().toSettings()

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = settings.language.code
            prefs[Keys.TOURIST_MODE] = settings.touristMode
            prefs[Keys.ROUTE_PREFERENCE] = settings.defaultRoutePreference.name
            prefs[Keys.MAX_WALK_DISTANCE] = settings.maxWalkDistance
            prefs[Keys.NOTIFICATIONS_ENABLED] = settings.enableNotifications
            prefs[Keys.LAST_BUS_WARNING] = settings.enableLastBusWarning
            prefs[Keys.LAST_BUS_WARNING_MINUTES] = settings.lastBusWarningMinutes
            prefs[Keys.OFFLINE_MAPS] = settings.offlineMapsEnabled
            prefs[Keys.MAP_PROVIDER] = settings.mapProvider.name
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    override suspend fun setTouristMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TOURIST_MODE] = enabled }
    }

    override suspend fun setRoutePreference(preference: RoutePreference) {
        context.dataStore.edit { it[Keys.ROUTE_PREFERENCE] = preference.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun clearCache() {
        context.dataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings() = AppSettings(
        language = AppLanguage.values()
            .firstOrNull { it.code == this[Keys.LANGUAGE] } ?: AppLanguage.ENGLISH,
        touristMode = this[Keys.TOURIST_MODE] ?: true,
        defaultRoutePreference = this[Keys.ROUTE_PREFERENCE]
            ?.let { RoutePreference.valueOf(it) } ?: RoutePreference.FASTEST,
        maxWalkDistance = this[Keys.MAX_WALK_DISTANCE] ?: 1000,
        enableNotifications = this[Keys.NOTIFICATIONS_ENABLED] ?: true,
        enableLastBusWarning = this[Keys.LAST_BUS_WARNING] ?: true,
        lastBusWarningMinutes = this[Keys.LAST_BUS_WARNING_MINUTES] ?: 30,
        offlineMapsEnabled = this[Keys.OFFLINE_MAPS] ?: true,
        mapProvider = this[Keys.MAP_PROVIDER]
            ?.let { MapProvider.valueOf(it) } ?: MapProvider.OPENSTREETMAP
    )
}

// ─── Realtime Repository ──────────────────────────────────────────────────────

@Singleton
class RealtimeRepositoryImpl @Inject constructor() : RealtimeRepository {

    // GTFS-Realtime would be integrated here when feeds become available for Crete.
    // Currently Crete's KTEL operators do not publish public GTFS-RT feeds.
    // The architecture is ready to plug in when available.

    override fun observeVehiclePositions(lineId: String): Flow<List<VehiclePosition>> =
        flow { emit(emptyList()) }

    override fun observeServiceAlerts(): Flow<List<ServiceAlert>> =
        flow { emit(emptyList()) }

    override suspend fun getVehiclePosition(vehicleId: String): VehiclePosition? = null

    override fun startRealtimeUpdates() { Timber.d("Realtime: No feed available yet") }
    override fun stopRealtimeUpdates() {}
}
