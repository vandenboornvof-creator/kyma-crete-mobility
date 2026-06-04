package com.cretemobility.app.data.repository

import com.cretemobility.app.data.local.dao.*
import com.cretemobility.app.data.local.entity.*
import com.cretemobility.app.data.remote.api.*
import com.cretemobility.app.data.remote.dto.*
import com.cretemobility.app.domain.model.*
import com.cretemobility.app.domain.repository.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopsRepositoryImpl @Inject constructor(
    private val stopDao: StopDao,
    private val stopTimeDao: StopTimeDao,
    private val overpassApi: OverpassApi
) : StopsRepository {

    override fun getAllStops(): Flow<List<TransitStop>> =
        stopDao.getAllStops().map { entities -> entities.map { it.toDomain() } }

    override fun getStopsByRegion(region: CreteRegion): Flow<List<TransitStop>> =
        stopDao.getStopsByRegion(region).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getStopById(id: String): TransitStop? =
        stopDao.getStopById(id)?.toDomain()

    override suspend fun getNearbyStops(location: LatLng, radiusMeters: Int): List<TransitStop> =
        stopDao.getNearbyStops(location.latitude, location.longitude, radiusMeters)
            .map { it.toDomain() }

    override suspend fun searchStops(query: String): List<TransitStop> =
        stopDao.searchStops(query).map { it.toDomain() }

    override suspend fun refreshStops(): Result<Unit> = runCatching {
        val query = """
            [out:json][timeout:60];
            (
              node["highway"="bus_stop"](34.8,23.5,35.7,26.5);
              node["public_transport"="stop_position"](34.8,23.5,35.7,26.5);
              node["amenity"="ferry_terminal"](34.8,23.5,35.7,26.5);
            );
            out body;
        """.trimIndent()

        val response = overpassApi.queryPost(query)
        val stops = response.elements.mapNotNull { element ->
            element.toStopEntity()
        }

        if (stops.isNotEmpty()) {
            stopDao.insertStops(stops)
            Timber.d("Refreshed ${stops.size} stops from OSM")
        }
    }

    override fun observeStopDepartures(stopId: String): Flow<List<Departure>> = flow {
        while (true) {
            val departures = getDepartures(stopId, LocalDate.now())
            emit(departures)
            kotlinx.coroutines.delay(60_000) // refresh every minute
        }
    }

    override suspend fun getDepartures(stopId: String, date: LocalDate): List<Departure> {
        val fromTime = if (date == LocalDate.now()) {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        } else {
            "00:00:00"
        }
        return stopTimeDao.getDeparturesFromStop(stopId, fromTime, 20)
            .mapNotNull { it.toDeparture(stopId, stopDao) }
    }
}

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteRouteDao: FavoriteRouteDao,
    private val favoriteStopDao: FavoriteStopDao,
    private val stopDao: StopDao
) : FavoritesRepository {

    override fun getFavoriteRoutes(): Flow<List<FavoriteRoute>> =
        favoriteRouteDao.getAllFavoriteRoutes().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getFavoriteStops(): Flow<List<FavoriteStop>> =
        favoriteStopDao.getAllFavoriteStops().map { entities ->
            entities.mapNotNull { it.toDomain() }
        }

    override suspend fun addFavoriteRoute(route: FavoriteRoute) {
        favoriteRouteDao.insertFavoriteRoute(route.toEntity())
    }

    override suspend fun removeFavoriteRoute(id: Long) {
        favoriteRouteDao.deleteFavoriteRouteById(id)
    }

    override suspend fun addFavoriteStop(stop: FavoriteStop) {
        favoriteStopDao.insertFavoriteStop(
            FavoriteStopEntity(
                stopId = stop.stop.id,
                customName = stop.customName,
                addedAt = stop.addedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )
    }

    override suspend fun removeFavoriteStop(id: Long) {
        favoriteStopDao.deleteFavoriteStopById(id)
    }

    override suspend fun updateRouteUsage(id: Long) {
        favoriteRouteDao.incrementUsage(id)
    }

    override suspend fun isFavoriteStop(stopId: String): Boolean =
        favoriteStopDao.isFavoriteStop(stopId)
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val nominatimApi: NominatimApi,
    private val stopDao: StopDao,
    private val recentSearchDao: RecentSearchDao,
    private val gson: Gson
) : SearchRepository {

    override suspend fun search(query: String, location: LatLng?): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Search local stops first
        val stopResults = stopDao.searchStops(query).map { it.toSearchResult() }
        results.addAll(stopResults)

        // Then search OSM for landmarks, hotels, etc.
        try {
            val nominatimResults = nominatimApi.search(query = query, limit = 10)
            results.addAll(nominatimResults.map { it.toSearchResult() })
        } catch (e: Exception) {
            Timber.w(e, "Nominatim search failed, using local results only")
        }

        // Sort by distance if location available
        if (location != null) {
            return results.sortedBy { result ->
                haversineDistance(
                    location.latitude, location.longitude,
                    result.location.latitude, result.location.longitude
                )
            }
        }

        return results.distinctBy { it.id }.take(20)
    }

    override suspend fun searchLandmarks(query: String): List<SearchResult> {
        val q = if (query.isBlank()) "landmark crete" else "$query landmark crete"
        return try {
            nominatimApi.search(query = q, limit = 15).map { it.toSearchResult() }
        } catch (e: Exception) {
            Timber.w(e, "Landmark search failed")
            emptyList()
        }
    }

    override suspend fun searchBeaches(query: String): List<SearchResult> {
        val q = if (query.isBlank()) "beach crete" else "$query beach crete"
        return try {
            nominatimApi.searchBeaches(query = q).map { it.toSearchResult() }
        } catch (e: Exception) {
            Timber.w(e, "Beach search failed")
            emptyList()
        }
    }

    override suspend fun searchHotels(query: String): List<SearchResult> {
        val q = if (query.isBlank()) "hotel crete" else "$query hotel crete"
        return try {
            nominatimApi.search(query = q, limit = 15).map { it.toSearchResult() }
        } catch (e: Exception) {
            Timber.w(e, "Hotel search failed")
            emptyList()
        }
    }

    override suspend fun reverseGeocode(location: LatLng): SearchResult? {
        return try {
            nominatimApi.reverse(location.latitude, location.longitude).toSearchResult()
        } catch (e: Exception) {
            Timber.w(e, "Reverse geocode failed")
            null
        }
    }

    override fun getRecentSearches(): Flow<List<SearchResult>> =
        recentSearchDao.getRecentSearches().map { entities ->
            entities.map { it.toSearchResult() }
        }

    override suspend fun saveRecentSearch(result: SearchResult) {
        recentSearchDao.insertSearch(result.toEntity())
        recentSearchDao.trimToLimit()
    }

    override suspend fun clearRecentSearches() = recentSearchDao.clearAll()

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

// ─── Mappers ─────────────────────────────────────────────────────────────────

fun StopEntity.toDomain() = TransitStop(
    id = id,
    name = name,
    nameEl = nameEl,
    location = LatLng(latitude, longitude),
    type = type,
    region = region ?: inferRegionFromLng(longitude),
    code = code,
    wheelchairAccessible = wheelchairAccessible,
    hasShelter = hasShelter
)

private fun inferRegionFromLng(lon: Double): CreteRegion = when {
    lon < 24.5 -> CreteRegion.CHANIA
    lon < 25.0 -> CreteRegion.RETHYMNO
    lon < 25.8 -> CreteRegion.HERAKLION
    else -> CreteRegion.LASITHI
}

fun OverpassElement.toStopEntity(): StopEntity? {
    val lat = lat ?: return null
    val lon = lon ?: return null
    val tags = tags ?: return null
    val name = tags["name"] ?: tags["name:en"] ?: return null

    val type = when {
        tags["amenity"] == "ferry_terminal" -> StopType.FERRY_PORT
        tags["aeroway"] == "aerodrome" -> StopType.AIRPORT
        else -> StopType.BUS_STOP
    }

    val region = when {
        lon < 24.5 -> CreteRegion.CHANIA
        lon < 25.0 -> CreteRegion.RETHYMNO
        lon < 25.8 -> CreteRegion.HERAKLION
        else -> CreteRegion.LASITHI
    }

    return StopEntity(
        id = "osm_${id}",
        name = name,
        nameEl = tags["name:el"] ?: name,
        latitude = lat,
        longitude = lon,
        type = type,
        region = region,
        code = tags["ref"],
        wheelchairAccessible = tags["wheelchair"] == "yes",
        hasShelter = tags["shelter"] == "yes"
    )
}

fun StopEntity.toSearchResult() = SearchResult(
    id = "stop_$id",
    name = name,
    nameEl = nameEl,
    description = "Bus Stop • ${(region ?: CreteRegion.HERAKLION).name.lowercase().replaceFirstChar { it.uppercase() }}",
    location = LatLng(latitude, longitude),
    type = when (type) {
        StopType.FERRY_PORT -> LocationType.FERRY_PORT
        StopType.AIRPORT -> LocationType.AIRPORT
        else -> LocationType.STOP
    }
)

fun NominatimResult.toSearchResult(): SearchResult {
    val type = when {
        address?.hotel != null -> LocationType.HOTEL
        address?.beach != null -> LocationType.BEACH
        address?.village != null -> LocationType.VILLAGE
        placeClass == "aeroway" -> LocationType.AIRPORT
        placeClass == "amenity" && type == "ferry_terminal" -> LocationType.FERRY_PORT
        else -> LocationType.LANDMARK
    }
    return SearchResult(
        id = "osm_$placeId",
        name = displayName.split(",").first().trim(),
        nameEl = namedetails?.get("name:el") ?: displayName.split(",").first().trim(),
        description = displayName,
        location = LatLng(lat.toDoubleOrNull() ?: 0.0, lon.toDoubleOrNull() ?: 0.0),
        type = type
    )
}

fun RecentSearchEntity.toSearchResult() = SearchResult(
    id = id,
    name = name,
    nameEl = nameEl,
    description = description,
    location = LatLng(latitude, longitude),
    type = type
)

fun SearchResult.toEntity() = RecentSearchEntity(
    id = id,
    name = name,
    nameEl = nameEl,
    description = description,
    latitude = location.latitude,
    longitude = location.longitude,
    type = type
)

fun FavoriteRouteEntity.toDomain() = FavoriteRoute(
    id = id,
    name = name,
    origin = Location(
        name = originName,
        latLng = LatLng(originLat, originLng),
        type = originType
    ),
    destination = Location(
        name = destinationName,
        latLng = LatLng(destinationLat, destinationLng),
        type = destinationType
    ),
    lastUsed = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastUsed), ZoneId.systemDefault()),
    usageCount = usageCount
)

fun FavoriteRoute.toEntity() = FavoriteRouteEntity(
    id = id,
    name = name,
    originName = origin.name,
    originLat = origin.latLng.latitude,
    originLng = origin.latLng.longitude,
    originType = origin.type,
    destinationName = destination.name,
    destinationLat = destination.latLng.latitude,
    destinationLng = destination.latLng.longitude,
    destinationType = destination.type,
    lastUsed = lastUsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    usageCount = usageCount
)

fun FavoriteStopWithStop.toDomain(): FavoriteStop? {
    val stopDomain = stop?.toDomain() ?: return null
    return FavoriteStop(
        id = favoriteStop.id,
        stop = stopDomain,
        customName = favoriteStop.customName,
        addedAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(favoriteStop.addedAt),
            ZoneId.systemDefault()
        )
    )
}

suspend fun StopTimeFull.toDeparture(stopId: String, stopDao: StopDao): Departure? {
    val stopEntity = stopDao.getStopById(stopId) ?: return null
    val time = LocalTime.parse(departureTime, DateTimeFormatter.ofPattern("HH:mm:ss"))
    return Departure(
        tripId = tripId,
        line = TransitLine(
            id = lineId,
            shortName = lineId,
            longName = headsign,
            mode = TransitMode.BUS,
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            region = stopEntity.region ?: inferRegionFromLng(stopEntity.longitude)
        ),
        stop = stopEntity.toDomain(),
        scheduledTime = time,
        headsign = headsign
    )
}
