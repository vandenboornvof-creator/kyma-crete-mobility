package com.cretemobility.app.core.di

import android.content.Context
import androidx.room.Room
import com.cretemobility.app.BuildConfig
import com.cretemobility.app.data.local.CreteMobilityDatabase
import com.cretemobility.app.data.local.dao.*
import com.cretemobility.app.data.remote.api.*
import com.cretemobility.app.data.remote.interceptor.UserAgentInterceptor
import com.cretemobility.app.data.repository.*
import com.cretemobility.app.domain.repository.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

// ─── Database Module ─────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CreteMobilityDatabase =
        Room.databaseBuilder(
            context,
            CreteMobilityDatabase::class.java,
            CreteMobilityDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideStopDao(db: CreteMobilityDatabase): StopDao = db.stopDao()
    @Provides fun provideLineDao(db: CreteMobilityDatabase): LineDao = db.lineDao()
    @Provides fun provideTripDao(db: CreteMobilityDatabase): TripDao = db.tripDao()
    @Provides fun provideStopTimeDao(db: CreteMobilityDatabase): StopTimeDao = db.stopTimeDao()
    @Provides fun provideFavoriteRouteDao(db: CreteMobilityDatabase): FavoriteRouteDao = db.favoriteRouteDao()
    @Provides fun provideFavoriteStopDao(db: CreteMobilityDatabase): FavoriteStopDao = db.favoriteStopDao()
    @Provides fun provideRecentSearchDao(db: CreteMobilityDatabase): RecentSearchDao = db.recentSearchDao()
    @Provides fun provideCachedJourneyDao(db: CreteMobilityDatabase): CachedJourneyDao = db.cachedJourneyDao()
}

// ─── Network Module ──────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(userAgentInterceptor: UserAgentInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("nominatim")
    fun provideNominatimRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.NOMINATIM_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("overpass")
    fun provideOverpassRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OVERPASS_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("otp")
    fun provideOtpRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OTP_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideNominatimApi(@Named("nominatim") retrofit: Retrofit): NominatimApi =
        retrofit.create(NominatimApi::class.java)

    @Provides
    @Singleton
    fun provideOverpassApi(@Named("overpass") retrofit: Retrofit): OverpassApi =
        retrofit.create(OverpassApi::class.java)

    @Provides
    @Singleton
    fun provideOtpApi(@Named("otp") retrofit: Retrofit): OtpApi =
        retrofit.create(OtpApi::class.java)
}

// ─── Repository Bindings ─────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindStopsRepository(impl: StopsRepositoryImpl): StopsRepository

    @Binds @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds @Singleton
    abstract fun bindJourneyRepository(impl: JourneyRepositoryImpl): JourneyRepository

    @Binds @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindRealtimeRepository(impl: RealtimeRepositoryImpl): RealtimeRepository
}
