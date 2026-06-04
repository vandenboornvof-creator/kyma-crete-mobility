package com.cretemobility.app.domain.usecase.stops

import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.LocationRepository
import com.cretemobility.app.domain.repository.StopsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class GetNearbyStopsUseCase @Inject constructor(
    private val stopsRepository: StopsRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(
        radiusMeters: Int = 500,
        maxResults: Int = 10
    ): List<TransitStop> {
        val location = locationRepository.getLastKnownLocation() ?: return emptyList()
        return stopsRepository.getNearbyStops(location, radiusMeters)
            .take(maxResults)
    }

    suspend operator fun invoke(location: LatLng, radiusMeters: Int = 500): List<TransitStop> {
        return stopsRepository.getNearbyStops(location, radiusMeters)
    }
}

class GetDeparturesUseCase @Inject constructor(
    private val stopsRepository: StopsRepository
) {
    operator fun invoke(stopId: String): Flow<List<Departure>> =
        stopsRepository.observeStopDepartures(stopId)

    suspend fun getForDate(stopId: String, date: LocalDate): List<Departure> =
        stopsRepository.getDepartures(stopId, date)

    suspend fun getUpcoming(
        stopId: String,
        limit: Int = 10,
        fromTime: LocalTime = LocalTime.now()
    ): List<Departure> {
        val today = stopsRepository.getDepartures(stopId, LocalDate.now())
        return today.filter { it.displayTime >= fromTime }
            .sortedBy { it.displayTime }
            .take(limit)
    }
}

class SearchStopsUseCase @Inject constructor(
    private val stopsRepository: StopsRepository
) {
    suspend operator fun invoke(query: String): List<TransitStop> {
        if (query.isBlank()) return emptyList()
        return stopsRepository.searchStops(query.trim())
    }
}

class GetStopDetailUseCase @Inject constructor(
    private val stopsRepository: StopsRepository
) {
    suspend operator fun invoke(stopId: String): TransitStop? =
        stopsRepository.getStopById(stopId)
}

class RefreshTransitDataUseCase @Inject constructor(
    private val stopsRepository: StopsRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        stopsRepository.refreshStops()
}
