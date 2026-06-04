package com.cretemobility.app.data.sync

import com.cretemobility.app.data.local.dao.LineDao
import com.cretemobility.app.data.local.dao.StopDao
import com.cretemobility.app.data.local.dao.StopTimeDao
import com.cretemobility.app.data.local.dao.TripDao
import com.cretemobility.app.data.local.entity.StopEntity
import com.cretemobility.app.domain.model.StopType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GtfsImportPipelineTest {

    private lateinit var stopDao: StopDao
    private lateinit var lineDao: LineDao
    private lateinit var tripDao: TripDao
    private lateinit var stopTimeDao: StopTimeDao
    private lateinit var pipeline: GtfsImportPipeline

    @Before
    fun setup() {
        stopDao = mockk()
        lineDao = mockk()
        tripDao = mockk()
        stopTimeDao = mockk()
        pipeline = GtfsImportPipeline(stopDao, lineDao, tripDao, stopTimeDao)

        coEvery { stopDao.insertStops(any()) } just Runs
        coEvery { lineDao.insertLines(any()) } just Runs
        coEvery { tripDao.insertTrips(any()) } just Runs
        coEvery { stopTimeDao.insertStopTimes(any()) } just Runs
    }

    @Test
    fun `parse minimal stops_txt successfully`() = runTest {
        val stopsContent = """
stop_id,stop_name,stop_lat,stop_lon,stop_code
1,Heraklion KTEL,35.3387,25.1442,HER01
2,Agios Nikolaos,35.1908,25.7178,AN01
        """.trimIndent()

        val zip = createZip(mapOf("stops.txt" to stopsContent))
        val result = pipeline.importFromZipStream(zip, "test")

        assertEquals(2, result.stopsImported)
        assertTrue(result.errors.none { it.startsWith("stops.txt") })

        val slot = slot<List<StopEntity>>()
        coVerify { stopDao.insertStops(capture(slot)) }
        assertEquals(2, slot.captured.size)
        assertEquals("test_1", slot.captured[0].id)
        assertEquals("Heraklion KTEL", slot.captured[0].name)
        assertEquals(35.3387, slot.captured[0].latitude, 0.0001)
    }

    @Test
    fun `handles missing optional files gracefully`() = runTest {
        val stopsContent = "stop_id,stop_name,stop_lat,stop_lon\n1,Test Stop,35.0,25.0"
        val zip = createZip(mapOf("stops.txt" to stopsContent))
        val result = pipeline.importFromZipStream(zip, "test")

        assertTrue(result.errors.any { it.contains("routes.txt") })
        assertTrue(result.errors.any { it.contains("trips.txt") })
        assertTrue(result.errors.any { it.contains("stop_times.txt") })
    }

    @Test
    fun `correctly detects ferry type from location_type=4`() = runTest {
        val stopsContent = """
stop_id,stop_name,stop_lat,stop_lon,location_type
1,Heraklion Port,35.3433,25.1378,4
2,Bus Stop,35.3387,25.1442,0
        """.trimIndent()

        val zip = createZip(mapOf("stops.txt" to stopsContent))
        pipeline.importFromZipStream(zip, "test")

        val slot = slot<List<StopEntity>>()
        coVerify { stopDao.insertStops(capture(slot)) }
        assertEquals(StopType.FERRY_PORT, slot.captured[0].type)
        assertEquals(StopType.BUS_STOP, slot.captured[1].type)
    }

    @Test
    fun `handles quoted CSV fields with commas`() = runTest {
        val stopsContent = """
stop_id,stop_name,stop_lat,stop_lon
1,"Heraklion, Central Station",35.3387,25.1442
        """.trimIndent()

        val zip = createZip(mapOf("stops.txt" to stopsContent))
        pipeline.importFromZipStream(zip, "test")

        val slot = slot<List<StopEntity>>()
        coVerify { stopDao.insertStops(capture(slot)) }
        assertEquals("Heraklion, Central Station", slot.captured[0].name)
    }

    @Test
    fun `empty stops file produces no inserts`() = runTest {
        val stopsContent = "stop_id,stop_name,stop_lat,stop_lon\n"
        val zip = createZip(mapOf("stops.txt" to stopsContent))
        pipeline.importFromZipStream(zip, "test")
        coVerify(exactly = 0) { stopDao.insertStops(any()) }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun createZip(files: Map<String, String>): java.io.InputStream {
        val baos = java.io.ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(baos.toByteArray())
    }
}
