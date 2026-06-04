package com.cretemobility.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.LocalTime

// ─── Geographic primitives ───────────────────────────────────────────────────

@Parcelize
data class LatLng(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    companion object {
        // Crete bounding box
        val CRETE_CENTER = LatLng(35.2401, 24.8093)
        val HERAKLION = LatLng(35.3387, 25.1442)
        val CHANIA = LatLng(35.5138, 24.0180)
        val RETHYMNO = LatLng(35.3711, 24.4683)
        val AGIOS_NIKOLAOS = LatLng(35.1903, 25.7170)
    }
}

// ─── Stops & Stations ────────────────────────────────────────────────────────

enum class StopType {
    BUS_STOP, BUS_STATION, FERRY_PORT, TAXI_RANK, LANDMARK, AIRPORT
}

@Parcelize
data class TransitStop(
    val id: String,
    val name: String,
    val nameEl: String,
    val location: LatLng,
    val type: StopType,
    val region: CreteRegion,
    val code: String? = null,
    val wheelchairAccessible: Boolean = false,
    val hasShelter: Boolean = false,
    val lines: List<String> = emptyList()
) : Parcelable

enum class CreteRegion {
    HERAKLION, CHANIA, RETHYMNO, LASITHI
}

// ─── Transit Lines ────────────────────────────────────────────────────────────

enum class TransitMode {
    BUS, FERRY, TAXI, WALK, BICYCLE, SCOOTER
}

enum class Operator {
    KTEL_HERAKLION_LASSITHI,
    KTEL_CHANIA_RETHYMNO,
    FERRY_MINOAN,
    FERRY_ANEK,
    FERRY_SEAJETS,
    LOCAL_TAXI,
    UNKNOWN
}

@Parcelize
data class TransitLine(
    val id: String,
    val shortName: String,
    val longName: String,
    val mode: TransitMode,
    val operator: Operator,
    val color: String = "#1E88E5",
    val textColor: String = "#FFFFFF",
    val region: CreteRegion
) : Parcelable

// ─── Trips & Departures ──────────────────────────────────────────────────────

@Parcelize
data class Departure(
    val tripId: String,
    val line: TransitLine,
    val stop: TransitStop,
    val scheduledTime: LocalTime,
    val estimatedTime: LocalTime? = null,
    val platformCode: String? = null,
    val headsign: String,
    val isRealtime: Boolean = false,
    val delayMinutes: Int = 0,
    val isCancelled: Boolean = false
) : Parcelable {
    val displayTime: LocalTime get() = estimatedTime ?: scheduledTime
    val isDelayed: Boolean get() = delayMinutes > 2
    val isOnTime: Boolean get() = !isDelayed && !isCancelled
}

// ─── Journey Planning ────────────────────────────────────────────────────────

@Parcelize
data class JourneyRequest(
    val origin: Location,
    val destination: Location,
    val departureTime: LocalDateTime = LocalDateTime.now(),
    val arriveBy: Boolean = false,
    val modes: Set<TransitMode> = setOf(TransitMode.BUS, TransitMode.WALK),
    val maxWalkDistance: Int = 1000, // meters
    val preference: RoutePreference = RoutePreference.FASTEST
) : Parcelable

enum class RoutePreference {
    FASTEST, LEAST_WALKING, CHEAPEST, FEWEST_TRANSFERS
}

@Parcelize
data class Location(
    val name: String,
    val nameEl: String = name,
    val latLng: LatLng,
    val stopId: String? = null,
    val type: LocationType = LocationType.CUSTOM
) : Parcelable

enum class LocationType {
    CURRENT_LOCATION, STOP, LANDMARK, HOTEL, BEACH, VILLAGE, AIRPORT, FERRY_PORT, CUSTOM
}

@Parcelize
data class Journey(
    val id: String,
    val legs: List<JourneyLeg>,
    val totalDuration: Int, // minutes
    val totalWalkDistance: Int, // meters
    val totalPrice: Double?,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val transferCount: Int,
    val preference: RoutePreference
) : Parcelable {
    val isWalkOnly: Boolean get() = legs.all { it.mode == TransitMode.WALK }
}

@Parcelize
data class JourneyLeg(
    val mode: TransitMode,
    val from: Location,
    val to: Location,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val duration: Int, // minutes
    val distance: Int, // meters
    val line: TransitLine? = null,
    val headsign: String? = null,
    val intermediateStops: List<TransitStop> = emptyList(),
    val polyline: List<LatLng> = emptyList(),
    val instructions: List<WalkInstruction> = emptyList(),
    val isRealtime: Boolean = false,
    val price: Double? = null
) : Parcelable

@Parcelize
data class WalkInstruction(
    val text: String,
    val distance: Int,
    val bearing: Double,
    val streetName: String
) : Parcelable

// ─── Favorites ───────────────────────────────────────────────────────────────

data class FavoriteRoute(
    val id: Long = 0,
    val name: String,
    val origin: Location,
    val destination: Location,
    val lastUsed: LocalDateTime,
    val usageCount: Int = 1
)

data class FavoriteStop(
    val id: Long = 0,
    val stop: TransitStop,
    val customName: String? = null,
    val addedAt: LocalDateTime
)

// ─── Real-time vehicle positions ─────────────────────────────────────────────

data class VehiclePosition(
    val vehicleId: String,
    val tripId: String,
    val lineId: String,
    val position: LatLng,
    val bearing: Float,
    val speed: Float, // km/h
    val timestamp: Long,
    val currentStopSequence: Int? = null,
    val occupancyStatus: OccupancyStatus = OccupancyStatus.UNKNOWN
)

enum class OccupancyStatus {
    EMPTY, MANY_SEATS_AVAILABLE, FEW_SEATS_AVAILABLE, STANDING_ROOM_ONLY, FULL, UNKNOWN
}

// ─── Service Alerts ──────────────────────────────────────────────────────────

data class ServiceAlert(
    val id: String,
    val header: String,
    val description: String,
    val severity: AlertSeverity,
    val affectedLines: List<String>,
    val affectedStops: List<String>,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?
)

enum class AlertSeverity { INFO, WARNING, SEVERE }

// ─── Search ──────────────────────────────────────────────────────────────────

data class SearchResult(
    val id: String,
    val name: String,
    val nameEl: String,
    val description: String,
    val location: LatLng,
    val type: LocationType,
    val distance: Int? = null // from current location, meters
)

// ─── Settings ────────────────────────────────────────────────────────────────

enum class AppLanguage(val code: String) {
    ENGLISH("en"), GREEK("el"), DUTCH("nl"), GERMAN("de")
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val touristMode: Boolean = true,
    val defaultRoutePreference: RoutePreference = RoutePreference.FASTEST,
    val maxWalkDistance: Int = 1000,
    val enableNotifications: Boolean = true,
    val enableLastBusWarning: Boolean = true,
    val lastBusWarningMinutes: Int = 30,
    val offlineMapsEnabled: Boolean = true,
    val offlineRegion: CreteRegion? = null,
    val mapProvider: MapProvider = MapProvider.OPENSTREETMAP
)

enum class MapProvider { OPENSTREETMAP, GOOGLE_MAPS }
