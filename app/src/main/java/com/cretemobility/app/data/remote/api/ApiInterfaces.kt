package com.cretemobility.app.data.remote.api

import com.cretemobility.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

// ─── Nominatim (OSM geocoding) ────────────────────────────────────────────────

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countrycodes: String = "gr",
        @Query("viewbox") viewbox: String = "23.5,34.8,26.5,35.7",
        @Query("bounded") bounded: Int = 1,
        @Query("accept-language") language: String = "en"
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("accept-language") language: String = "en"
    ): NominatimResult

    @GET("search")
    suspend fun searchBeaches(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("featuretype") featuretype: String = "natural",
        @Query("countrycodes") countrycodes: String = "gr",
        @Query("viewbox") viewbox: String = "23.5,34.8,26.5,35.7",
        @Query("bounded") bounded: Int = 1
    ): List<NominatimResult>
}

// ─── Overpass API (OSM data queries for stops, POIs) ─────────────────────────

interface OverpassApi {
    @GET("interpreter")
    suspend fun query(
        @Query("data") query: String
    ): OverpassResponse

    @POST("interpreter")
    @FormUrlEncoded
    suspend fun queryPost(
        @Field("data") query: String
    ): OverpassResponse
}

// ─── OTP (OpenTripPlanner) API ────────────────────────────────────────────────
// If self-hosted OTP is available; fallback to local routing

interface OtpApi {
    @GET("plan")
    suspend fun plan(
        @Query("fromPlace") fromPlace: String, // "lat,lng"
        @Query("toPlace") toPlace: String,
        @Query("date") date: String, // "YYYY-MM-DD"
        @Query("time") time: String, // "HH:MM:SS"
        @Query("arriveBy") arriveBy: Boolean = false,
        @Query("mode") mode: String = "TRANSIT,WALK",
        @Query("maxWalkDistance") maxWalkDistance: Int = 1000,
        @Query("numItineraries") numItineraries: Int = 5,
        @Query("locale") locale: String = "en"
    ): OtpPlanResponse
}

// ─── GTFS Static feed ────────────────────────────────────────────────────────
// For downloading GTFS zip bundles from KTEL

interface GtfsFeedApi {
    @GET
    @Streaming
    suspend fun downloadGtfsFeed(
        @Url url: String
    ): Response<okhttp3.ResponseBody>
}

// ─── Scraper adapter for KTEL Heraklion ──────────────────────────────────────
// KTEL doesn't expose a public API, so we scrape structured schedule pages

interface KtelHeraklionApi {
    @GET("lines")
    suspend fun getLines(): Response<String> // HTML response, parsed separately

    @GET("schedule/{lineId}")
    suspend fun getLineSchedule(
        @Path("lineId") lineId: String
    ): Response<String>
}

// ─── Ferry APIs (Minoan Lines, ANEK, SeaJets) ────────────────────────────────

interface MinoanFerryApi {
    @GET("schedules")
    suspend fun getSchedules(
        @Query("departure") departure: String,
        @Query("arrival") arrival: String,
        @Query("date") date: String
    ): FerryScheduleResponse
}

interface SeaJetsApi {
    @GET("schedules/search")
    suspend fun searchSchedules(
        @Query("origin") origin: String = "HER",
        @Query("destination") destination: String,
        @Query("date") date: String
    ): SeaJetsResponse
}
