package com.cretemobility.app.data.sync

import com.cretemobility.app.domain.model.CreteRegion
import com.cretemobility.app.domain.model.Operator
import com.cretemobility.app.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class KtelScraperTest {

    private lateinit var scraper: KtelScraper

    @Before
    fun setup() {
        scraper = KtelScraper()
    }

    @Test
    fun `heraklion seed data returns non-empty list`() {
        val routes = scraper.getHeraklionSeedData()
        assertTrue(routes.isNotEmpty())
    }

    @Test
    fun `airport route is present in heraklion seed data`() {
        val routes = scraper.getHeraklionSeedData()
        val airportRoute = routes.find { it.routeId == "ktel_hir_6" }
        assertNotNull(airportRoute)
        assertEquals("A", airportRoute!!.shortName)
        assertEquals(1.50, airportRoute.priceEur!!, 0.01)
    }

    @Test
    fun `chania seed data has correct operator`() {
        val routes = scraper.getChaniaRethymnoSeedData()
        assertTrue(routes.all { it.operator == Operator.KTEL_CHANIA_RETHYMNO })
    }

    @Test
    fun `seed stops have valid coordinates`() {
        val stops = scraper.getSeedStops()
        assertTrue(stops.isNotEmpty())
        stops.forEach { stop ->
            assertTrue("${stop.id} lat out of range", stop.latitude in 34.5..36.0)
            assertTrue("${stop.id} lon out of range", stop.longitude in 23.0..27.0)
        }
    }

    @Test
    fun `scrapedStopToTransitStop maps ferry port correctly`() {
        val ferryStop = KtelScraper.ScrapedStop(
            id = "test_port",
            name = "Heraklion Port",
            latitude = 35.343,
            longitude = 25.137,
            operator = Operator.KTEL_HERAKLION_LASSITHI
        )
        val transitStop = scraper.scrapedStopToTransitStop(ferryStop)
        assertEquals(StopType.FERRY_PORT, transitStop.type)
    }

    @Test
    fun `scrapedStopToTransitStop maps heraklion operator to heraklion region`() {
        val stop = KtelScraper.ScrapedStop(
            id = "test_bus",
            name = "Test Bus Stop",
            latitude = 35.3,
            longitude = 25.1,
            operator = Operator.KTEL_HERAKLION_LASSITHI
        )
        val transitStop = scraper.scrapedStopToTransitStop(stop)
        assertEquals(CreteRegion.HERAKLION, transitStop.region)
    }

    @Test
    fun `getNextDepartureTimes filters out past departures`() {
        val route = scraper.getHeraklionSeedData().first()
        val fromTime = LocalTime.of(23, 59) // very late
        val upcoming = scraper.getNextDepartureTimes(route, fromTime, 5)
        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun `getNextDepartureTimes respects maxResults`() {
        val route = scraper.getHeraklionSeedData()
            .find { it.routeId == "ktel_hir_6" }!! // airport route has 18 departures
        val upcoming = scraper.getNextDepartureTimes(route, LocalTime.of(0, 0), 3)
        assertTrue(upcoming.size <= 3)
    }

    @Test
    fun `all heraklion routes operate all 7 days`() {
        val routes = scraper.getHeraklionSeedData()
        routes.forEach { route ->
            assertEquals("Route ${route.routeId} should operate 7 days", 7, route.daysOfOperation.size)
        }
    }
}
