package com.cretemobility.app.domain.usecase.journey

import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.JourneyRepository
import com.cretemobility.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PlanJourneyUseCase @Inject constructor(
    private val journeyRepository: JourneyRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(request: JourneyRequest): Result<List<Journey>> {
        return try {
            journeyRepository.planJourney(request)
        } catch (e: Exception) {
            // Fallback to offline planning
            journeyRepository.planJourneyOffline(request)
        }
    }
}

class PlanJourneyFromCurrentLocationUseCase @Inject constructor(
    private val journeyRepository: JourneyRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(
        destination: Location,
        preference: RoutePreference = RoutePreference.FASTEST
    ): Result<List<Journey>> {
        val currentLatLng = locationRepository.getLastKnownLocation()
            ?: return Result.failure(IllegalStateException("Location unavailable"))

        val origin = Location(
            name = "My Location",
            nameEl = "Τοποθεσία μου",
            latLng = currentLatLng,
            type = LocationType.CURRENT_LOCATION
        )

        val request = JourneyRequest(
            origin = origin,
            destination = destination,
            preference = preference
        )

        return journeyRepository.planJourney(request)
    }
}

class GetSuggestedRoutesUseCase @Inject constructor(
    private val journeyRepository: JourneyRepository
) {
    operator fun invoke() = journeyRepository.getLastJourneys()
}

// Sort and filter journeys based on preference
class SortJourneysUseCase @Inject constructor() {
    operator fun invoke(
        journeys: List<Journey>,
        preference: RoutePreference
    ): List<Journey> = when (preference) {
        RoutePreference.FASTEST -> journeys.sortedBy { it.totalDuration }
        RoutePreference.LEAST_WALKING -> journeys.sortedBy { it.totalWalkDistance }
        RoutePreference.CHEAPEST -> journeys.sortedBy { it.totalPrice ?: Double.MAX_VALUE }
        RoutePreference.FEWEST_TRANSFERS -> journeys.sortedBy { it.transferCount }
    }
}

class GetJourneyDetailsUseCase @Inject constructor() {
    fun getWalkingSummary(journey: Journey): String {
        val walkLegs = journey.legs.filter { it.mode == TransitMode.WALK }
        val totalWalk = walkLegs.sumOf { it.distance }
        return when {
            totalWalk < 100 -> "Minimal walking"
            totalWalk < 500 -> "${totalWalk}m walk"
            else -> String.format("%.1fkm walk", totalWalk / 1000.0)
        }
    }

    fun getPriceSummary(journey: Journey): String? {
        return journey.totalPrice?.let { "€%.2f".format(it) }
    }

    fun isLastBusWarningNeeded(journey: Journey, warningMinutes: Int): Boolean {
        // Check if this is the last available bus and warn user
        val now = java.time.LocalDateTime.now()
        val departure = journey.departureTime
        val minutesUntilDeparture = java.time.Duration.between(now, departure).toMinutes()
        return minutesUntilDeparture in 0..warningMinutes.toLong()
    }
}
