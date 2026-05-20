package com.BPO.plantcare

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.BPO.plantcare.core.notification.PlantCareNotifications
import com.BPO.plantcare.core.work.WateringReminderManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PlantCareApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var reminderManager: WateringReminderManager
    @Inject lateinit var plantSyncManager: com.BPO.plantcare.data.sync.PlantSyncManager
    @Inject lateinit var authRepository: com.BPO.plantcare.domain.repository.AuthRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.BPO.plantcare.core.locale.LocaleHelper.wrap(base))
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        PlantCareNotifications.registerChannels(this)
        applicationScope.launch { reminderManager.applyOnStartup() }
        // Sincroniza las plantas con la nube segun la sesion activa.
        plantSyncManager.start(applicationScope, authRepository)
    }

    /**
     * Coil 3 NO trae engine de red por defecto (a diferencia de Coil 2).
     * Hay que registrar explicitamente el OkHttpNetworkFetcherFactory para
     * que las URLs http(s):// se puedan descargar. Sin esto, AsyncImage no
     * carga nada (y solo veias el icono fallback).
     *
     * Le metemos un User-Agent identificable porque algunos CDN (Wikipedia
     * en concreto) bloquean clientes sin UA con 403.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "PlantCare-Android/1.0 (https://github.com/DanielChineaDev/PlantCare; contact@plantcare.app)",
                    )
                    .build()
                chain.proceed(req)
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttp }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
