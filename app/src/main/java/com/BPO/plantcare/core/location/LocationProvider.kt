package com.BPO.plantcare.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

    /**
     * Devuelve la mejor ubicacion conocida en cache. No fuerza un fix nuevo
     * (eso requiere callback async + Looper). Para weather con tolerancia
     * de horas, la ultima cacheada es mas que suficiente.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        val mgr = manager ?: return null
        val providers = mgr.getProviders(true) // activos
        var best: Pair<Double, Double>? = null
        var bestTime = 0L
        for (provider in providers) {
            val loc = mgr.getLastKnownLocation(provider) ?: continue
            if (loc.time > bestTime) {
                bestTime = loc.time
                best = loc.latitude to loc.longitude
            }
        }
        return best
    }
}
