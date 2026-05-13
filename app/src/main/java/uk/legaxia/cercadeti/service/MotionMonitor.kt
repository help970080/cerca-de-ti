package uk.legaxia.cercadeti.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.getSystemService
import kotlin.math.sqrt

/**
 * Monitor de acelerómetro y giroscopio.
 *
 * Detecta:
 * - Impactos fuertes (caída, golpe)
 * - Movimiento intenso continuo (forcejeo)
 * - Cambios bruscos de orientación
 * - Inmovilidad súbita tras movimiento intenso (caída + víctima inconsciente)
 */
class MotionMonitor(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager? = context.getSystemService()
    private val acelerometro: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val giroscopio: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    /**
     * Historial rodante de muestras (últimos ~60s a 5 Hz = 300 muestras)
     */
    private val muestras = ArrayDeque<MotionSample>()
    private val maxMuestras = 300

    private var ultimaActualizacionMs = 0L
    private val intervaloMuestraMs = 200L  // ~5 Hz

    private var continuousHighIntensityStart = 0L
    private var continuousHighIntensityMs = 0L
    private var orientationChanges = 0
    private var lastOrientation = 0  // estimación rudimentaria

    fun iniciar() {
        if (acelerometro == null) {
            Log.e(TAG, "Sin acelerómetro disponible")
            return
        }
        sensorManager?.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_NORMAL)
        giroscopio?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.i(TAG, "MotionMonitor iniciado")
    }

    fun detener() {
        sensorManager?.unregisterListener(this)
        muestras.clear()
        continuousHighIntensityMs = 0L
        orientationChanges = 0
        Log.i(TAG, "MotionMonitor detenido")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ahora = System.currentTimeMillis()
        if (ahora - ultimaActualizacionMs < intervaloMuestraMs) return
        ultimaActualizacionMs = ahora

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> procesarAceleracion(event.values, ahora)
            Sensor.TYPE_GYROSCOPE -> procesarGiroscopio(event.values, ahora)
        }
    }

    private fun procesarAceleracion(values: FloatArray, ahora: Long) {
        val x = values[0]; val y = values[1]; val z = values[2]
        // Magnitud descontando la gravedad
        val magnitudTotal = sqrt(x * x + y * y + z * z).toDouble()
        val magnitudLineal = magnitudTotal - GRAVEDAD

        // Detectar movimiento intenso continuo
        if (magnitudLineal > HIGH_INTENSITY_THRESHOLD) {
            if (continuousHighIntensityStart == 0L) continuousHighIntensityStart = ahora
            continuousHighIntensityMs = ahora - continuousHighIntensityStart
        } else {
            continuousHighIntensityStart = 0L
            continuousHighIntensityMs = 0L
        }

        // Detectar cambio brusco de orientación: signo dominante del eje
        val orientacionActual = when {
            kotlin.math.abs(z) > kotlin.math.abs(x) && kotlin.math.abs(z) > kotlin.math.abs(y) -> if (z > 0) 1 else 2
            kotlin.math.abs(x) > kotlin.math.abs(y) -> if (x > 0) 3 else 4
            else -> if (y > 0) 5 else 6
        }
        if (lastOrientation != 0 && orientacionActual != lastOrientation) {
            orientationChanges++
        }
        lastOrientation = orientacionActual

        sincronizarMuestra(magnitudLineal, null, ahora)
    }

    private fun procesarGiroscopio(values: FloatArray, ahora: Long) {
        val magnitud = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]).toDouble()
        sincronizarMuestra(null, magnitud, ahora)
    }

    private fun sincronizarMuestra(accel: Double?, gyro: Double?, ahora: Long) {
        synchronized(muestras) {
            // Buscar o crear muestra para este timestamp aproximado
            val ultima = muestras.lastOrNull()
            if (ultima != null && ahora - ultima.timestampMs < intervaloMuestraMs) {
                // Actualizar muestra existente
                muestras.removeLast()
                muestras.addLast(MotionSample(
                    accelMagnitude = accel ?: ultima.accelMagnitude,
                    gyroMagnitude = gyro ?: ultima.gyroMagnitude,
                    timestampMs = ultima.timestampMs
                ))
            } else {
                muestras.addLast(MotionSample(
                    accelMagnitude = accel ?: 0.0,
                    gyroMagnitude = gyro ?: 0.0,
                    timestampMs = ahora
                ))
            }

            while (muestras.size > maxMuestras) muestras.removeFirst()
        }
    }

    fun snapshot(): MotionSnapshot {
        val lista = synchronized(muestras) { muestras.toList() }
        if (lista.isEmpty()) {
            return MotionSnapshot(0.0, 0.0, 0.0, 0, 0L, 0, 0)
        }

        val maxAccel = lista.maxOf { it.accelMagnitude }
        val avgAccel = lista.map { it.accelMagnitude }.average()
        val maxGyro = lista.maxOf { it.gyroMagnitude }

        // Conteo de paradas bruscas: transición de alta a casi cero
        var paradas = 0
        for (i in 1 until lista.size) {
            if (lista[i - 1].accelMagnitude > HIGH_INTENSITY_THRESHOLD &&
                lista[i].accelMagnitude < LOW_INTENSITY_THRESHOLD) {
                paradas++
            }
        }

        val duracionSec = ((lista.last().timestampMs - lista.first().timestampMs) / 1000).toInt()

        return MotionSnapshot(
            maxAccelMagnitude = maxAccel,
            avgAccelMagnitude = avgAccel,
            maxGyroMagnitude = maxGyro,
            suddenStops = paradas,
            continuousHighIntensityMs = continuousHighIntensityMs,
            orientationChanges = orientationChanges.also { orientationChanges = 0 },
            windowDurationSec = duracionSec
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    private data class MotionSample(
        val accelMagnitude: Double,
        val gyroMagnitude: Double,
        val timestampMs: Long
    )

    companion object {
        private const val TAG = "MotionMonitor"
        private const val GRAVEDAD = 9.81
        private const val HIGH_INTENSITY_THRESHOLD = 5.0   // m/s² lineales
        private const val LOW_INTENSITY_THRESHOLD = 1.0
    }
}
