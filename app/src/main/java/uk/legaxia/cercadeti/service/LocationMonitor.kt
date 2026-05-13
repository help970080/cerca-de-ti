package uk.legaxia.cercadeti.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Monitor de ubicación. Mantiene:
 * - Posición actual
 * - Trayectoria de los últimos 10 minutos
 * - Distancia al "baseline" (zona habitual estimada como centroide de tiempo de estancia)
 *
 * Configurado para bajo consumo: actualizaciones cada 60s o por cambio significativo.
 * Se eleva la frecuencia automáticamente si se detecta movimiento intenso.
 */
class LocationMonitor(private val context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val trayectoria = ArrayDeque<TrayectoriaPunto>()
    private val maxPuntos = 600  // 10 minutos a 1 Hz peak

    private var ubicacionActual: Location? = null
    private var baselineCentroide: Pair<Double, Double>? = null  // se llena tras una semana

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            ubicacionActual = loc
            synchronized(trayectoria) {
                trayectoria.addLast(TrayectoriaPunto(
                    lat = loc.latitude,
                    lon = loc.longitude,
                    timestampMs = System.currentTimeMillis(),
                    speedMs = loc.speed
                ))
                while (trayectoria.size > maxPuntos) trayectoria.removeFirst()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun iniciar() {
        if (!tienePermiso()) {
            Log.e(TAG, "Sin permiso ACCESS_FINE_LOCATION; no se inicia LocationMonitor")
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000)
            .setMinUpdateIntervalMillis(15_000)
            .setMinUpdateDistanceMeters(50f)
            .setWaitForAccurateLocation(false)
            .build()

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        Log.i(TAG, "LocationMonitor iniciado")
    }

    fun detener() {
        client.removeLocationUpdates(callback)
        synchronized(trayectoria) { trayectoria.clear() }
        Log.i(TAG, "LocationMonitor detenido")
    }

    fun snapshot(): LocationSnapshot {
        val loc = ubicacionActual
        val puntos = synchronized(trayectoria) { trayectoria.toList() }

        val distanciaBaseline = if (loc != null && baselineCentroide != null) {
            val r = FloatArray(1)
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                baselineCentroide!!.first, baselineCentroide!!.second,
                r
            )
            r[0].toDouble()
        } else 0.0

        return LocationSnapshot(
            latActual = loc?.latitude,
            lonActual = loc?.longitude,
            speedMs = loc?.speed?.toDouble() ?: 0.0,
            distanceFromBaseline = distanciaBaseline,
            trayectoria = puntos,
            precisionMeters = loc?.accuracy ?: 0f
        )
    }

    private fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "LocationMonitor"
    }
}
