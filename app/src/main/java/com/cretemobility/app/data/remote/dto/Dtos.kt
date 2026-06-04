package com.cretemobility.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Nominatim DTOs ──────────────────────────────────────────────────────────

data class NominatimResult(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("display_name") val displayName: String,
    val lat: String,
    val lon: String,
    val type: String?,
    val importance: Double,
    val address: NominatimAddress?,
    @SerializedName("class") val placeClass: String?,
    val namedetails: Map<String, String>?
)

data class NominatimAddress(
    val road: String?,
    val village: String?,
    val town: String?,
    val city: String?,
    val municipality: String?,
    val county: String?,
    val country: String?,
    @SerializedName("country_code") val countryCode: String?,
    val postcode: String?,
    val beach: String?,
    val hotel: String?
)

// ─── Overpass DTOs ────────────────────────────────────────────────────────────

data class OverpassResponse(
    val version: Double?,
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String, // node, way, relation
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>?,
    val nodes: List<Long>?,
    val members: List<OverpassMember>?
)

data class OverpassMember(
    val type: String,
    val ref: Long,
    val role: String
)

// ─── OTP (OpenTripPlanner) DTOs ───────────────────────────────────────────────

data class OtpPlanResponse(
    val plan: OtpPlan?,
    val error: OtpError?
)

data class OtpPlan(
    val from: OtpPlace,
    val to: OtpPlace,
    val itineraries: List<OtpItinerary>
)

data class OtpError(
    val id: Int,
    val msg: String,
    val description: String?
)

data class OtpItinerary(
    val duration: Long, // seconds
    @SerializedName("startTime") val startTimeMs: Long,
    @SerializedName("endTime") val endTimeMs: Long,
    val walkTime: Long,
    val walkDistance: Double,
    val transfers: Int,
    val fare: OtpFare?,
    val legs: List<OtpLeg>
)

data class OtpFare(
    val fare: Map<String, OtpFareComponent>?
)

data class OtpFareComponent(
    val cents: Int,
    val currency: OtpCurrency
)

data class OtpCurrency(
    val currency: String,
    val defaultFractionDigits: Int,
    val symbol: String
)

data class OtpLeg(
    @SerializedName("startTime") val startTimeMs: Long,
    @SerializedName("endTime") val endTimeMs: Long,
    val duration: Long,
    val distance: Double,
    val mode: String,
    val from: OtpPlace,
    val to: OtpPlace,
    val legGeometry: OtpGeometry,
    val route: String?,
    val routeShortName: String?,
    val routeLongName: String?,
    val routeColor: String?,
    val routeTextColor: String?,
    val agencyId: String?,
    val agencyName: String?,
    val headsign: String?,
    val tripId: String?,
    val realTime: Boolean = false,
    val arrivalDelay: Int = 0,
    val departureDelay: Int = 0,
    val steps: List<OtpStep>?,
    val intermediateStops: List<OtpPlace>?
)

data class OtpPlace(
    val name: String,
    val lon: Double,
    val lat: Double,
    val arrival: Long?,
    val departure: Long?,
    val stopId: String?,
    val stopCode: String?,
    val platformCode: String?
)

data class OtpGeometry(
    val points: String, // encoded polyline
    val length: Int
)

data class OtpStep(
    val distance: Double,
    val relativeDirection: String,
    val streetName: String,
    val lat: Double,
    val lon: Double,
    val absoluteDirection: String
)

// ─── Ferry DTOs ───────────────────────────────────────────────────────────────

data class FerryScheduleResponse(
    val schedules: List<FerrySchedule>
)

data class FerrySchedule(
    val id: String,
    val operator: String,
    val vessel: String,
    val departure: String,
    val arrival: String,
    @SerializedName("departure_time") val departureTime: String,
    @SerializedName("arrival_time") val arrivalTime: String,
    val duration: String,
    val price: Double?
)

data class SeaJetsResponse(
    val trips: List<SeaJetsTrip>?
)

data class SeaJetsTrip(
    val id: String,
    val vessel: String,
    val origin: String,
    val destination: String,
    val departure: String,
    val arrival: String,
    val price: Double?
)

// ─── GTFS-Realtime (protocol buffer → JSON adapter) ──────────────────────────

data class GtfsRtVehiclePosition(
    @SerializedName("vehicle_id") val vehicleId: String,
    @SerializedName("trip_id") val tripId: String,
    @SerializedName("route_id") val routeId: String,
    val latitude: Float,
    val longitude: Float,
    val bearing: Float?,
    val speed: Float?,
    val timestamp: Long,
    @SerializedName("current_stop_sequence") val currentStopSequence: Int?,
    @SerializedName("occupancy_status") val occupancyStatus: Int?
)

data class GtfsRtTripUpdate(
    @SerializedName("trip_id") val tripId: String,
    @SerializedName("route_id") val routeId: String,
    @SerializedName("stop_time_update") val stopTimeUpdates: List<GtfsRtStopTimeUpdate>
)

data class GtfsRtStopTimeUpdate(
    @SerializedName("stop_sequence") val stopSequence: Int?,
    @SerializedName("stop_id") val stopId: String?,
    val arrival: GtfsRtTimeEvent?,
    val departure: GtfsRtTimeEvent?
)

data class GtfsRtTimeEvent(
    val delay: Int?,
    val time: Long?
)

data class GtfsRtServiceAlert(
    @SerializedName("alert_id") val alertId: String,
    @SerializedName("informed_entity") val informedEntities: List<GtfsRtInformedEntity>,
    @SerializedName("header_text") val headerText: String,
    @SerializedName("description_text") val descriptionText: String,
    val severity: String?,
    @SerializedName("active_period") val activePeriod: GtfsRtActivePeriod?
)

data class GtfsRtInformedEntity(
    @SerializedName("route_id") val routeId: String?,
    @SerializedName("stop_id") val stopId: String?
)

data class GtfsRtActivePeriod(
    val start: Long?,
    val end: Long?
)
