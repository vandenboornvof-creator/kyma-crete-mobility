package com.cretemobility.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cretemobility.app.data.local.dao.*
import com.cretemobility.app.data.local.entity.*
import com.cretemobility.app.domain.model.*

@Database(
    entities = [
        StopEntity::class,
        LineEntity::class,
        TripEntity::class,
        StopTimeEntity::class,
        CalendarEntity::class,
        FavoriteRouteEntity::class,
        FavoriteStopEntity::class,
        RecentSearchEntity::class,
        CachedJourneyEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class CreteMobilityDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao
    abstract fun lineDao(): LineDao
    abstract fun tripDao(): TripDao
    abstract fun stopTimeDao(): StopTimeDao
    abstract fun favoriteRouteDao(): FavoriteRouteDao
    abstract fun favoriteStopDao(): FavoriteStopDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun cachedJourneyDao(): CachedJourneyDao

    companion object {
        const val DATABASE_NAME = "crete_mobility.db"
    }
}

class DatabaseConverters {

    // ── Non-nullable enums ────────────────────────────────────────────────

    @TypeConverter fun fromStopType(v: StopType): String = v.name
    @TypeConverter fun toStopType(v: String): StopType = StopType.valueOf(v)

    @TypeConverter fun fromTransitMode(v: TransitMode): String = v.name
    @TypeConverter fun toTransitMode(v: String): TransitMode = TransitMode.valueOf(v)

    @TypeConverter fun fromLocationType(v: LocationType): String = v.name
    @TypeConverter fun toLocationType(v: String): LocationType = LocationType.valueOf(v)

    @TypeConverter fun fromRoutePreference(v: RoutePreference): String = v.name
    @TypeConverter fun toRoutePreference(v: String): RoutePreference = RoutePreference.valueOf(v)

    // ── Nullable enums (stored as nullable String) ────────────────────────

    @TypeConverter fun fromOperatorNullable(v: Operator?): String? = v?.name
    @TypeConverter fun toOperatorNullable(v: String?): Operator? = v?.let { Operator.valueOf(it) }

    @TypeConverter fun fromCreteRegionNullable(v: CreteRegion?): String? = v?.name
    @TypeConverter fun toCreteRegionNullable(v: String?): CreteRegion? = v?.let { CreteRegion.valueOf(it) }
}
