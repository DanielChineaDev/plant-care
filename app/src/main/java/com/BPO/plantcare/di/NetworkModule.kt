package com.BPO.plantcare.di

import com.BPO.plantcare.BuildConfig
import com.BPO.plantcare.data.remote.OpenMeteoApi
import com.BPO.plantcare.data.remote.PlantNetApi
import com.BPO.plantcare.data.remote.WikipediaApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val RETROFIT_PLANTNET = "plantnet"
    private const val RETROFIT_WIKIPEDIA = "wikipedia"
    private const val RETROFIT_OPENMETEO = "openmeteo"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        // Wikipedia REST API rechaza requests sin User-Agent identificable
        // y devuelve 403. Lo aplicamos a todos los clientes; ninguna otra API
        // se queja por tenerlo.
        val userAgentInterceptor = okhttp3.Interceptor { chain ->
            val req = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "PlantCare-Android/1.0 (https://github.com/DanielChineaDev/PlantCare; contact@plantcare.app)",
                )
                .build()
            chain.proceed(req)
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named(RETROFIT_PLANTNET)
    fun providePlantNetRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(PlantNetApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named(RETROFIT_WIKIPEDIA)
    fun provideWikipediaRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(WikipediaApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named(RETROFIT_OPENMETEO)
    fun provideOpenMeteoRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun providePlantNetApi(@Named(RETROFIT_PLANTNET) retrofit: Retrofit): PlantNetApi =
        retrofit.create(PlantNetApi::class.java)

    @Provides
    @Singleton
    fun provideWikipediaApi(@Named(RETROFIT_WIKIPEDIA) retrofit: Retrofit): WikipediaApi =
        retrofit.create(WikipediaApi::class.java)

    @Provides
    @Singleton
    fun provideOpenMeteoApi(@Named(RETROFIT_OPENMETEO) retrofit: Retrofit): OpenMeteoApi =
        retrofit.create(OpenMeteoApi::class.java)
}
