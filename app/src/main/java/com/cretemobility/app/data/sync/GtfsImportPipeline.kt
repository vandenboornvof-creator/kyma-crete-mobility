package com.cretemobility.app.data.sync

import com.cretemobility.app.data.local.dao.LineDao
import com.cretemobility.app.data.local.dao.StopDao
import com.cretemobility.app.data.local.dao.StopTimeDao
import com.cretemobility.app.data.local.dao.TripDao
import com.cretemobility.app.data.local.entity.LineEntity
import com.cretemobility.app.data.local.entity.StopEntity
import com.cretemobility.app.data.local.entity.StopTimeEntity
import com.cretemobility.app.data.local.entity.TripEntity
import com.cretemobility.app.domain.model.StopType
import com.cretemobility.app.domain.model.TransitMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses GTFS zip archives and imports them into the local Room database.
 *
 * GTFS spec: https://developers.google.com/transit/gtfs/reference
 *
 * Files processed:
 *   - stops.txt       → StopEntity
 *   - routes.txt      → LineEntity
 *   - trips.txt       → TripEntity
 *   - stop_times.txt  → StopTimeEntity
 *
 * Files not yet processed (future):
 *   - calendar.txt / calendar_dates.txt  → CalendarEntity
 *   - shapes.txt                         → RoutePolyline
 *   - feed_info.txt                      → FeedMetadata
 */
@Singleton
class GtfsImportPipeline @Inject constructor(
    private val stopDao: StopDao,
    private val lineDao: LineDao,
    private val tripDao: TripDao,
    private val stopTimeDao: StopTimeDao
) {
    data class ImportResult(
        val stopsImported: Int = 0,
        val linesImported: Int = 0,
        val tripsImported: Int = 0,
        val stopTimesImported: Int = 0,
        val errors: List<String> = emptyList()
    )

    suspend fun importFromZipStream(
        stream: InputStream,
        feedId: String = "default"
    ): ImportResult = withContext(Dispatchers.IO) {
        val result = ImportResult()
        val errors = mutableListOf<String>()
        val files = mutableMapOf<String, String>()

        // Read all files from ZIP into memory (GTFS zips are usually <50MB)
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    files[entry.name] = zip.bufferedReader().readText()
                    Timber.d("GTFS: read ${entry.name} (${files[entry.name]!!.length} chars)")
                }
                entry = zip.nextEntry
            }
        }

        var stopsImported = 0
        var linesImported = 0
        var tripsImported = 0
        var stopTimesImported = 0

        // 1. Import stops.txt
        files["stops.txt"]?.let { content ->
            try {
                val stops = parseStops(content, feedId)
                stopDao.insertStops(stops)
                stopsImported = stops.size
                Timber.d("GTFS: imported $stopsImported stops")
            } catch (e: Exception) {
                Timber.e(e, "GTFS: failed to parse stops.txt")
                errors.add("stops.txt: ${e.message}")
            }
        } ?: errors.add("stops.txt not found in GTFS zip")

        // 2. Import routes.txt
        files["routes.txt"]?.let { content ->
            try {
                val lines = parseRoutes(content, feedId)
                lineDao.insertLines(lines)
                linesImported = lines.size
                Timber.d("GTFS: imported $linesImported routes")
            } catch (e: Exception) {
                Timber.e(e, "GTFS: failed to parse routes.txt")
                errors.add("routes.txt: ${e.message}")
            }
        } ?: errors.add("routes.txt not found in GTFS zip")

        // 3. Import trips.txt
        files["trips.txt"]?.let { content ->
            try {
                val trips = parseTrips(content, feedId)
                tripDao.insertTrips(trips)
                tripsImported = trips.size
                Timber.d("GTFS: imported $tripsImported trips")
            } catch (e: Exception) {
                Timber.e(e, "GTFS: failed to parse trips.txt")
                errors.add("trips.txt: ${e.message}")
            }
        } ?: errors.add("trips.txt not found in GTFS zip")

        // 4. Import stop_times.txt (largest file — batch in chunks of 1000)
        files["stop_times.txt"]?.let { content ->
            try {
                val stopTimes = parseStopTimes(content, feedId)
                stopTimes.chunked(1000).forEach { chunk ->
                    stopTimeDao.insertStopTimes(chunk)
                }
                stopTimesImported = stopTimes.size
                Timber.d("GTFS: imported $stopTimesImported stop times")
            } catch (e: Exception) {
                Timber.e(e, "GTFS: failed to parse stop_times.txt")
                errors.add("stop_times.txt: ${e.message}")
            }
        } ?: errors.add("stop_times.txt not found in GTFS zip")

        ImportResult(stopsImported, linesImported, tripsImported, stopTimesImported, errors)
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Private parsers
    // ──────────────────────────────────────────────────────────────────────

    private fun parseStops(csv: String, feedId: String): List<StopEntity> {
        val lines = csv.lines()
        if (lines.isEmpty()) return emptyList()
        val headers = lines[0].split(",").map { it.trim().removeSurrounding("\"") }

        val stopIdIdx = headers.indexOf("stop_id")
        val nameIdx = headers.indexOf("stop_name")
        val latIdx = headers.indexOf("stop_lat")
        val lonIdx = headers.indexOf("stop_lon")
        val typeIdx = headers.indexOf("location_type")
        val codeIdx = headers.indexOf("stop_code")

        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(line)
            runCatching {
                StopEntity(
                    id = "${feedId}_${cols.getOrElse(stopIdIdx) { return@mapNotNull null }}",
                    name = cols.getOrElse(nameIdx) { "" },
                    latitude = cols.getOrElse(latIdx) { "0" }.toDoubleOrNull() ?: 0.0,
                    longitude = cols.getOrElse(lonIdx) { "0" }.toDoubleOrNull() ?: 0.0,
                    type = when (cols.getOrElse(typeIdx) { "0" }) {
                        "2" -> StopType.FERRY_PORT
                        else -> StopType.BUS_STOP
                    },
                    code = cols.getOrElse(codeIdx) { null },
                    region = null,
                    operatorId = feedId,
                    isActive = true
                )
            }.getOrNull()
        }
    }

    private fun parseRoutes(csv: String, feedId: String): List<LineEntity> {
        val lines = csv.lines()
        if (lines.isEmpty()) return emptyList()
        val headers = lines[0].split(",").map { it.trim().removeSurrounding("\"") }

        val idIdx = headers.indexOf("route_id")
        val shortNameIdx = headers.indexOf("route_short_name")
        val longNameIdx = headers.indexOf("route_long_name")
        val typeIdx = headers.indexOf("route_type")
        val colorIdx = headers.indexOf("route_color")

        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(line)
            runCatching {
                LineEntity(
                    id = "${feedId}_${cols.getOrElse(idIdx) { return@mapNotNull null }}",
                    shortName = cols.getOrElse(shortNameIdx) { "" },
                    longName = cols.getOrElse(longNameIdx) { "" },
                    mode = when (cols.getOrElse(typeIdx) { "3" }) {
                        "4" -> TransitMode.FERRY
                        "2" -> TransitMode.FERRY
                        else -> TransitMode.BUS
                    },
                    color = cols.getOrElse(colorIdx) { "1565C0" }.let { "#$it" },
                    operatorId = feedId,
                    isActive = true
                )
            }.getOrNull()
        }
    }

    private fun parseTrips(csv: String, feedId: String): List<TripEntity> {
        val lines = csv.lines()
        if (lines.isEmpty()) return emptyList()
        val headers = lines[0].split(",").map { it.trim().removeSurrounding("\"") }

        val tripIdIdx = headers.indexOf("trip_id")
        val routeIdIdx = headers.indexOf("route_id")
        val serviceIdIdx = headers.indexOf("service_id")
        val headSignIdx = headers.indexOf("trip_headsign")

        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(line)
            runCatching {
                TripEntity(
                    id = "${feedId}_${cols.getOrElse(tripIdIdx) { return@mapNotNull null }}",
                    lineId = "${feedId}_${cols.getOrElse(routeIdIdx) { "" }}",
                    serviceId = cols.getOrElse(serviceIdIdx) { "" },
                    headsign = cols.getOrElse(headSignIdx) { "" }
                )
            }.getOrNull()
        }
    }

    private fun parseStopTimes(csv: String, feedId: String): List<StopTimeEntity> {
        val lines = csv.lines()
        if (lines.isEmpty()) return emptyList()
        val headers = lines[0].split(",").map { it.trim().removeSurrounding("\"") }

        val tripIdIdx = headers.indexOf("trip_id")
        val arrivalIdx = headers.indexOf("arrival_time")
        val departureIdx = headers.indexOf("departure_time")
        val stopIdIdx = headers.indexOf("stop_id")
        val seqIdx = headers.indexOf("stop_sequence")

        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(line)
            runCatching {
                StopTimeEntity(
                    id = "${feedId}_${cols.getOrElse(tripIdIdx) { return@mapNotNull null }}_${cols.getOrElse(seqIdx) { "0" }}",
                    tripId = "${feedId}_${cols.getOrElse(tripIdIdx) { "" }}",
                    stopId = "${feedId}_${cols.getOrElse(stopIdIdx) { "" }}",
                    arrivalTime = cols.getOrElse(arrivalIdx) { "" },
                    departureTime = cols.getOrElse(departureIdx) { "" },
                    stopSequence = cols.getOrElse(seqIdx) { "0" }.toIntOrNull() ?: 0
                )
            }.getOrNull()
        }
    }

    /**
     * Handles quoted CSV fields correctly (fields may contain commas inside quotes).
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }
}
