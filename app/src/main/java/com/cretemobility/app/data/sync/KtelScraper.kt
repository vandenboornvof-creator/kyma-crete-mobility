package com.cretemobility.app.data.sync

import com.cretemobility.app.domain.model.CreteRegion
import com.cretemobility.app.domain.model.LatLng
import com.cretemobility.app.domain.model.Operator
import com.cretemobility.app.domain.model.StopType
import com.cretemobility.app.domain.model.TransitMode
import com.cretemobility.app.domain.model.TransitStop
import timber.log.Timber
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seed data and scraper adapters for KTEL Crete schedule data.
 *
 * KTEL Heraklion-Lasithi: https://www.ktel-heraklion-lasithi.gr/
 * KTEL Chania-Rethymno:   https://www.bus-service-crete-ktel.com/
 *
 * These sites do not expose public APIs, so we provide hardcoded seed data
 * that was manually verified from their published schedules.
 *
 * When official GTFS feeds become available, replace this with GtfsImportPipeline.
 */
@Singleton
class KtelScraper @Inject constructor() {

    data class ScrapedRoute(
        val routeId: String,
        val shortName: String,
        val longName: String,
        val operator: Operator,
        val departureTimes: List<String>,  // "HH:mm" format
        val originStop: String,
        val destinationStop: String,
        val daysOfOperation: Set<Int>,     // 1=Mon, 7=Sun
        val priceEur: Double?
    )

    data class ScrapedStop(
        val id: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val operator: Operator
    )

    /**
     * Hardcoded seed data for KTEL Heraklion major routes.
     * Last verified from KTEL website: 2024.
     */
    fun getHeraklionSeedData(): List<ScrapedRoute> = listOf(
        ScrapedRoute(
            routeId = "ktel_hir_1",
            shortName = "1",
            longName = "Heraklion - Malia - Agios Nikolaos",
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            departureTimes = listOf("07:30", "09:00", "10:30", "12:00", "14:00", "16:30", "18:30"),
            originStop = "Heraklion KTEL Station",
            destinationStop = "Agios Nikolaos",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 7.60
        ),
        ScrapedRoute(
            routeId = "ktel_hir_2",
            shortName = "2",
            longName = "Heraklion - Rethymno",
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            departureTimes = listOf("07:00", "09:00", "11:00", "13:00", "15:00", "17:00", "19:00"),
            originStop = "Heraklion KTEL Station",
            destinationStop = "Rethymno",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 8.00
        ),
        ScrapedRoute(
            routeId = "ktel_hir_3",
            shortName = "3",
            longName = "Heraklion - Chersonisos",
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            departureTimes = listOf("07:00", "08:00", "09:00", "10:00", "11:00", "12:00",
                "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "20:00"),
            originStop = "Heraklion KTEL Station",
            destinationStop = "Chersonisos",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 3.50
        ),
        ScrapedRoute(
            routeId = "ktel_hir_4",
            shortName = "4",
            longName = "Heraklion - Ierapetra",
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            departureTimes = listOf("07:30", "10:00", "12:00", "14:30", "17:00"),
            originStop = "Heraklion KTEL Station",
            destinationStop = "Ierapetra",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 14.00
        ),
        ScrapedRoute(
            routeId = "ktel_hir_5",
            shortName = "5",
            longName = "Heraklion - Sitia",
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            departureTimes = listOf("08:30", "13:00", "17:30"),
            originStop = "Heraklion KTEL Station",
            destinationStop = "Sitia",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 16.00
        ),
        ScrapedRoute(
            routeId = "ktel_hir_6",
            shortName = "A",
            longName = "Heraklion Airport - City Centre",
            operator = Operator.KTEL_HERAKLION_LASSITHI,
            departureTimes = listOf("06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00",
                "20:00", "21:00", "22:00", "23:00"),
            originStop = "Heraklion International Airport",
            destinationStop = "Heraklion KTEL Station",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 1.50
        )
    )

    fun getChaniaRethymnoSeedData(): List<ScrapedRoute> = listOf(
        ScrapedRoute(
            routeId = "ktel_ch_1",
            shortName = "1",
            longName = "Chania - Rethymno - Heraklion",
            operator = Operator.KTEL_CHANIA_RETHYMNO,
            departureTimes = listOf("06:30", "08:30", "10:00", "12:00", "14:30", "16:30", "19:00"),
            originStop = "Chania KTEL Station",
            destinationStop = "Heraklion KTEL Station",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 15.50
        ),
        ScrapedRoute(
            routeId = "ktel_ch_2",
            shortName = "2",
            longName = "Chania - Kissamos",
            operator = Operator.KTEL_CHANIA_RETHYMNO,
            departureTimes = listOf("08:00", "10:30", "13:00", "16:00", "18:30"),
            originStop = "Chania KTEL Station",
            destinationStop = "Kissamos",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 4.20
        ),
        ScrapedRoute(
            routeId = "ktel_ch_3",
            shortName = "3",
            longName = "Chania Airport - City Centre",
            operator = Operator.KTEL_CHANIA_RETHYMNO,
            departureTimes = listOf("07:00", "09:00", "11:00", "13:00", "15:00", "17:00", "20:00"),
            originStop = "Chania International Airport",
            destinationStop = "Chania KTEL Station",
            daysOfOperation = setOf(1, 2, 3, 4, 5, 6, 7),
            priceEur = 2.30
        )
    )

    /**
     * Key stops with GPS coordinates for offline use.
     * Coordinates verified against OpenStreetMap.
     */
    fun getSeedStops(): List<ScrapedStop> = listOf(
        // Heraklion
        ScrapedStop("ktel_hir_central", "Heraklion KTEL Station", 35.3387, 25.1442, Operator.KTEL_HERAKLION_LASSITHI),
        ScrapedStop("hir_airport", "Heraklion International Airport (HER)", 35.3397, 25.1803, Operator.KTEL_HERAKLION_LASSITHI),
        ScrapedStop("hir_port", "Heraklion Port", 35.3433, 25.1378, Operator.KTEL_HERAKLION_LASSITHI),
        // Chania
        ScrapedStop("ktel_ch_central", "Chania KTEL Station", 35.5125, 24.0181, Operator.KTEL_CHANIA_RETHYMNO),
        ScrapedStop("ch_airport", "Chania International Airport (CHQ)", 35.5317, 24.1497, Operator.KTEL_CHANIA_RETHYMNO),
        ScrapedStop("ch_port", "Chania Port (Souda)", 35.5190, 24.0839, Operator.KTEL_CHANIA_RETHYMNO),
        // Rethymno
        ScrapedStop("ktel_reth_central", "Rethymno KTEL Station", 35.3742, 24.4736, Operator.KTEL_CHANIA_RETHYMNO),
        // Lasithi / East
        ScrapedStop("agios_nikolaos", "Agios Nikolaos Bus Station", 35.1908, 25.7178, Operator.KTEL_HERAKLION_LASSITHI),
        ScrapedStop("ierapetra", "Ierapetra Bus Station", 35.0106, 25.7378, Operator.KTEL_HERAKLION_LASSITHI),
        ScrapedStop("sitia", "Sitia Bus Station", 35.2031, 26.1031, Operator.KTEL_HERAKLION_LASSITHI),
        // Popular tourist spots
        ScrapedStop("chersonisos", "Chersonisos Bus Stop", 35.2986, 25.3897, Operator.KTEL_HERAKLION_LASSITHI),
        ScrapedStop("malia", "Malia Bus Stop", 35.2903, 25.4658, Operator.KTEL_HERAKLION_LASSITHI),
        ScrapedStop("kissamos", "Kissamos Bus Stop", 35.4878, 23.6511, Operator.KTEL_CHANIA_RETHYMNO)
    )

    fun scrapedStopToTransitStop(scraped: ScrapedStop): TransitStop = TransitStop(
        id = scraped.id,
        name = scraped.name,
        nameEl = scraped.name, // fallback: use EN name until translated
        location = LatLng(scraped.latitude, scraped.longitude),
        type = if (scraped.name.contains("Port", ignoreCase = true) ||
            scraped.name.contains("Ferry", ignoreCase = true))
            StopType.FERRY_PORT else StopType.BUS_STOP,
        region = when (scraped.operator) {
            Operator.KTEL_HERAKLION_LASSITHI -> CreteRegion.HERAKLION
            Operator.KTEL_CHANIA_RETHYMNO -> CreteRegion.CHANIA
            else -> CreteRegion.HERAKLION
        },
        code = null
    )

    fun getNextDepartureTimes(
        route: ScrapedRoute,
        fromNow: LocalTime = LocalTime.now(),
        maxResults: Int = 5
    ): List<LocalTime> {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        return route.departureTimes
            .mapNotNull { runCatching { LocalTime.parse(it, fmt) }.getOrNull() }
            .filter { it.isAfter(fromNow) }
            .take(maxResults)
    }
}
