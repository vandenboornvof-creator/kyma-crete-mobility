package com.cretemobility.app.data.local.dao

import androidx.room.*
import com.cretemobility.app.data.local.entity.*
import com.cretemobility.app.domain.model.CreteRegion
import com.cretemobility.app.domain.model.StopType
import kotlinx.coroutines.flow.Flow

// ─── Stops DAO ────────────────────────────────────────────────────────────────

@Dao
interface StopDao {
    @Query("SELECT * FROM transit_stops ORDER BY name ASC")
    fun getAllStops(): Flow<List<StopEntity>>

    @Query("SELECT * FROM transit_stops WHERE region = :region ORDER BY name ASC")
    fun getStopsByRegion(region: CreteRegion): Flow<List<StopEntity>>

    @Query("SELECT * FROM transit_stops WHERE id = :id")
    suspend fun getStopById(id: String): StopEntity?

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, 
        (6371000 * acos(cos(radians(:lat)) * cos(radians(latitude)) * 
        cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude)))) 
        AS distance 
        FROM transit_stops 
        WHERE (6371000 * acos(cos(radians(:lat)) * cos(radians(latitude)) * 
        cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude)))) < :radiusMeters
        ORDER BY distance ASC
        LIMIT 50
    """)
    suspend fun getNearbyStops(lat: Double, lng: Double, radiusMeters: Int): List<StopEntity>

    @Query("""
        SELECT * FROM transit_stops 
        WHERE name LIKE '%' || :query || '%' 
        OR name_el LIKE '%' || :query || '%'
        OR code LIKE '%' || :query || '%'
        ORDER BY name ASC
        LIMIT 30
    """)
    suspend fun searchStops(query: String): List<StopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: StopEntity)

    @Query("DELETE FROM transit_stops")
    suspend fun deleteAllStops()

    @Query("SELECT COUNT(*) FROM transit_stops")
    suspend fun getStopCount(): Int
}

// ─── Lines DAO ────────────────────────────────────────────────────────────────

@Dao
interface LineDao {
    @Query("SELECT * FROM transit_lines ORDER BY short_name ASC")
    fun getAllLines(): Flow<List<LineEntity>>

    @Query("SELECT * FROM transit_lines WHERE operator_id = :operator ORDER BY short_name ASC")
    fun getLinesByOperator(operator: String): Flow<List<LineEntity>>

    @Query("SELECT * FROM transit_lines WHERE id = :id")
    suspend fun getLineById(id: String): LineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<LineEntity>)

    @Query("DELETE FROM transit_lines")
    suspend fun deleteAllLines()
}

// ─── Stop Times DAO ──────────────────────────────────────────────────────────

@Dao
interface StopTimeDao {
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT st.*, t.headsign, t.line_id, t.service_id
        FROM stop_times st
        INNER JOIN trips t ON st.trip_id = t.id
        WHERE st.stop_id = :stopId
        AND st.departure_time >= :fromTime
        ORDER BY st.departure_time ASC
        LIMIT :limit
    """)
    suspend fun getDeparturesFromStop(
        stopId: String,
        fromTime: String,
        limit: Int = 20
    ): List<StopTimeFull>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopTimes(stopTimes: List<StopTimeEntity>)

    @Query("DELETE FROM stop_times")
    suspend fun deleteAllStopTimes()
}

data class StopTimeFull(
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "stop_id") val stopId: String,
    @ColumnInfo(name = "stop_sequence") val stopSequence: Int,
    @ColumnInfo(name = "arrival_time") val arrivalTime: String,
    @ColumnInfo(name = "departure_time") val departureTime: String,
    val headsign: String,
    @ColumnInfo(name = "line_id") val lineId: String,
    @ColumnInfo(name = "service_id") val serviceId: String
)

// ─── Trips DAO ────────────────────────────────────────────────────────────────

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE line_id = :lineId")
    suspend fun getTripsByLine(lineId: String): List<TripEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()
}

// ─── Favorites DAO ────────────────────────────────────────────────────────────

@Dao
interface FavoriteRouteDao {
    @Query("SELECT * FROM favorite_routes ORDER BY usage_count DESC, last_used DESC")
    fun getAllFavoriteRoutes(): Flow<List<FavoriteRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteRoute(route: FavoriteRouteEntity): Long

    @Delete
    suspend fun deleteFavoriteRoute(route: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_routes WHERE id = :id")
    suspend fun deleteFavoriteRouteById(id: Long)

    @Query("UPDATE favorite_routes SET usage_count = usage_count + 1, last_used = :now WHERE id = :id")
    suspend fun incrementUsage(id: Long, now: Long = System.currentTimeMillis())
}

@Dao
interface FavoriteStopDao {
    @Transaction
    @Query("SELECT * FROM favorite_stops ORDER BY added_at DESC")
    fun getAllFavoriteStops(): Flow<List<FavoriteStopWithStop>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteStop(stop: FavoriteStopEntity): Long

    @Query("DELETE FROM favorite_stops WHERE id = :id")
    suspend fun deleteFavoriteStopById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stops WHERE stop_id = :stopId)")
    suspend fun isFavoriteStop(stopId: String): Boolean

    @Query("DELETE FROM favorite_stops WHERE stop_id = :stopId")
    suspend fun deleteFavoriteByStopId(stopId: String)
}

data class FavoriteStopWithStop(
    @Embedded val favoriteStop: FavoriteStopEntity,
    @Relation(
        parentColumn = "stop_id",
        entityColumn = "id"
    )
    val stop: StopEntity?
)

// ─── Recent Searches DAO ──────────────────────────────────────────────────────

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searched_at DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()

    @Query("DELETE FROM recent_searches WHERE id NOT IN (SELECT id FROM recent_searches ORDER BY searched_at DESC LIMIT 20)")
    suspend fun trimToLimit()
}

// ─── Cached Journeys DAO ──────────────────────────────────────────────────────

@Dao
interface CachedJourneyDao {
    @Query("SELECT * FROM cached_journeys WHERE expires_at > :now ORDER BY cached_at DESC LIMIT 10")
    suspend fun getValidCachedJourneys(now: Long = System.currentTimeMillis()): List<CachedJourneyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedJourney(journey: CachedJourneyEntity)

    @Query("DELETE FROM cached_journeys WHERE expires_at < :now")
    suspend fun deleteExpiredJourneys(now: Long = System.currentTimeMillis())
}
