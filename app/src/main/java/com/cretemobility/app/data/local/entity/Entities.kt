package com.cretemobility.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cretemobility.app.domain.model.CreteRegion
import com.cretemobility.app.domain.model.LocationType
import com.cretemobility.app.domain.model.Operator
import com.cretemobility.app.domain.model.RoutePreference
import com.cretemobility.app.domain.model.StopType
import com.cretemobility.app.domain.model.TransitMode

// ─── Stop entity ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "transit_stops",
    indices = [
        Index("region"),
        Index("type"),
        Index("latitude", "longitude"),
        Index("operator_id")
    ]
)
data class StopEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_el") val nameEl: String = "",
    val latitude: Double,
    val longitude: Double,
    val type: StopType = StopType.BUS_STOP,
    val region: CreteRegion? = null,
    @ColumnInfo(name = "operator_id") val operatorId: String? = null,
    val code: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "wheelchair_accessible") val wheelchairAccessible: Boolean = false,
    @ColumnInfo(name = "has_shelter") val hasShelter: Boolean = false,
    @ColumnInfo(name = "lines_json") val linesJson: String = "[]",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// ─── Line entity ──────────────────────────────────────────────────────────────

@Entity(
    tableName = "transit_lines",
    indices = [Index("operator_id"), Index("region")]
)
data class LineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "short_name") val shortName: String,
    @ColumnInfo(name = "long_name") val longName: String,
    val mode: TransitMode = TransitMode.BUS,
    @ColumnInfo(name = "operator_id") val operatorId: String? = null,
    val operator: Operator? = null,
    val color: String = "#1E88E5",
    @ColumnInfo(name = "text_color") val textColor: String = "#FFFFFF",
    val region: CreteRegion? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// ─── Trip entity ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = LineEntity::class,
            parentColumns = ["id"],
            childColumns = ["line_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("line_id"), Index("service_id")]
)
data class TripEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "line_id") val lineId: String,
    @ColumnInfo(name = "service_id") val serviceId: String,
    val headsign: String = "",
    @ColumnInfo(name = "direction_id") val directionId: Int = 0,
    @ColumnInfo(name = "shape_id") val shapeId: String? = null
)

// ─── Stop Time entity ────────────────────────────────────────────────────────

@Entity(
    tableName = "stop_times",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("stop_id"),
        Index("trip_id"),
        Index("departure_time")
    ]
)
data class StopTimeEntity(
    val id: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "stop_id") val stopId: String,
    @ColumnInfo(name = "stop_sequence") val stopSequence: Int,
    @ColumnInfo(name = "arrival_time") val arrivalTime: String,   // HH:MM:SS or HH:MM
    @ColumnInfo(name = "departure_time") val departureTime: String, // HH:MM:SS or HH:MM
    @ColumnInfo(name = "pickup_type") val pickupType: Int = 0,
    @ColumnInfo(name = "drop_off_type") val dropOffType: Int = 0
)

// ─── Calendar / Service days ─────────────────────────────────────────────────

@Entity(tableName = "calendar_services")
data class CalendarEntity(
    @PrimaryKey @ColumnInfo(name = "service_id") val serviceId: String,
    val monday: Boolean = true,
    val tuesday: Boolean = true,
    val wednesday: Boolean = true,
    val thursday: Boolean = true,
    val friday: Boolean = true,
    val saturday: Boolean = true,
    val sunday: Boolean = true,
    @ColumnInfo(name = "start_date") val startDate: String = "",  // YYYYMMDD
    @ColumnInfo(name = "end_date") val endDate: String = ""
)

// ─── Favorite Routes ─────────────────────────────────────────────────────────

@Entity(tableName = "favorite_routes")
data class FavoriteRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "origin_name") val originName: String,
    @ColumnInfo(name = "origin_lat") val originLat: Double,
    @ColumnInfo(name = "origin_lng") val originLng: Double,
    @ColumnInfo(name = "origin_type") val originType: LocationType,
    @ColumnInfo(name = "destination_name") val destinationName: String,
    @ColumnInfo(name = "destination_lat") val destinationLat: Double,
    @ColumnInfo(name = "destination_lng") val destinationLng: Double,
    @ColumnInfo(name = "destination_type") val destinationType: LocationType,
    val preference: RoutePreference = RoutePreference.FASTEST,
    @ColumnInfo(name = "last_used") val lastUsed: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "usage_count") val usageCount: Int = 1
)

// ─── Favorite Stops ──────────────────────────────────────────────────────────

@Entity(
    tableName = "favorite_stops",
    indices = [Index("stop_id")]
)
data class FavoriteStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stop_id") val stopId: String,
    @ColumnInfo(name = "custom_name") val customName: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

// ─── Recent Searches ─────────────────────────────────────────────────────────

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_el") val nameEl: String = "",
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val type: LocationType,
    @ColumnInfo(name = "searched_at") val searchedAt: Long = System.currentTimeMillis()
)

// ─── Cached Journey ──────────────────────────────────────────────────────────

@Entity(tableName = "cached_journeys")
data class CachedJourneyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "origin_name") val originName: String,
    @ColumnInfo(name = "destination_name") val destinationName: String,
    @ColumnInfo(name = "journey_json") val journeyJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "expires_at") val expiresAt: Long
)
