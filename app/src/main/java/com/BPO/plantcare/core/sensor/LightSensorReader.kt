package com.BPO.plantcare.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LightSensorReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    val isAvailable: Boolean get() = sensor != null

    /**
     * Emite el valor del sensor de luz en lux. Se desuscribe automaticamente
     * cuando el coleccionador se cancela.
     */
    fun readLux(): Flow<Float> = callbackFlow {
        val device = sensor
        val mgr = manager
        if (device == null || mgr == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                event.values.firstOrNull()?.let { trySend(it) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        mgr.registerListener(listener, device, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { mgr.unregisterListener(listener) }
    }
}
