package uk.legaxia.cercadeti.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import uk.legaxia.cercadeti.stt.KeywordSpotter
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Monitor de audio v2.
 *
 * Funciones:
 * 1. Ring buffer de 60s en RAM para el snapshot del detector
 * 2. Estadísticas rodantes (dB, ZCR) por chunks de 500ms
 * 3. Pasa cada chunk al KeywordSpotter (si está activo) para STT
 *
 * El audio NUNCA se persiste en disco salvo al disparar una alerta.
 */
class AudioMonitor(private val context: Context) {

    private val sampleRate = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val readBufferSize = (sampleRate * 0.5).toInt()  // 500 ms por lectura

    private var audioRecord: AudioRecord? = null
    private var capturaThread: Thread? = null
    @Volatile private var corriendo = false

    /** Ring buffer circular en RAM, 60 segundos PCM16 16kHz ≈ 1.92 MB */
    private val totalSamples = sampleRate * 60
    private val ringBuffer = ShortArray(totalSamples)
    private var writeIndex = 0

    /** Historial de stats por chunk de 500ms (últimos 120 = 60s). */
    private val statsHistory = ArrayDeque<ChunkStats>()

    /** Spotter de palabras clave (puede ser null si Vosk no está listo). */
    @Volatile private var keywordSpotter: KeywordSpotter? = null

    /** Configura el KeywordSpotter desde GuardianService cuando Vosk esté listo. */
    fun setKeywordSpotter(spotter: KeywordSpotter?) {
        keywordSpotter?.cerrar()
        keywordSpotter = spotter
        if (spotter != null) Log.i(TAG, "KeywordSpotter activado")
        else Log.i(TAG, "KeywordSpotter desactivado")
    }

    fun iniciar() {
        if (corriendo) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Sin permiso RECORD_AUDIO; no se inicia AudioMonitor")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                maxOf(minBufferSize, readBufferSize) * 2
            )
            audioRecord?.startRecording()
            corriendo = true

            capturaThread = Thread { bucleCaptura() }.apply {
                name = "CercaDeTi-AudioCapture"
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
            Log.i(TAG, "AudioMonitor iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando AudioMonitor", e)
            corriendo = false
        }
    }

    fun detener() {
        corriendo = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo AudioMonitor", e)
        }
        audioRecord = null
        capturaThread?.interrupt()
        capturaThread = null
        keywordSpotter?.cerrar()
        keywordSpotter = null
        synchronized(ringBuffer) {
            ringBuffer.fill(0)
            writeIndex = 0
        }
        statsHistory.clear()
        Log.i(TAG, "AudioMonitor detenido")
    }

    private fun bucleCaptura() {
        val temp = ShortArray(readBufferSize)
        while (corriendo) {
            val leidos = audioRecord?.read(temp, 0, temp.size) ?: -1
            if (leidos <= 0) {
                Thread.sleep(50)
                continue
            }

            // 1. Copiar al ring buffer
            synchronized(ringBuffer) {
                for (i in 0 until leidos) {
                    ringBuffer[writeIndex] = temp[i]
                    writeIndex = (writeIndex + 1) % totalSamples
                }
            }

            // 2. Stats
            val stats = calcularStats(temp, leidos)
            synchronized(statsHistory) {
                statsHistory.addLast(stats)
                while (statsHistory.size > 120) statsHistory.removeFirst()
            }

            // 3. Alimentar al KeywordSpotter (en mismo thread; Vosk es muy rápido)
            keywordSpotter?.procesarChunk(temp, leidos)
        }
    }

    private fun calcularStats(samples: ShortArray, length: Int): ChunkStats {
        var sumSquares = 0.0
        var peak = 0.0
        for (i in 0 until length) {
            val v = samples[i].toDouble()
            sumSquares += v * v
            val ab = abs(v)
            if (ab > peak) peak = ab
        }
        val rms = sqrt(sumSquares / length)
        val avgDb = if (rms > 0) 20.0 * log10(rms / 32768.0) + 90.0 else 0.0
        val peakDb = if (peak > 0) 20.0 * log10(peak / 32768.0) + 90.0 else 0.0

        var zeroCrossings = 0
        var prev = samples[0].toInt()
        for (i in 1 until length) {
            val curr = samples[i].toInt()
            if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) zeroCrossings++
            prev = curr
        }
        val zcr = zeroCrossings.toDouble() / length

        return ChunkStats(
            avgDb = avgDb,
            peakDb = peakDb,
            zeroCrossingRate = zcr,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun snapshot(): AudioSnapshot {
        val historial = synchronized(statsHistory) { statsHistory.toList() }
        if (historial.isEmpty()) {
            return AudioSnapshot(
                avgDb = 0.0, peakDb = 0.0, pitchVariance = 0.0,
                voiceActivityRatio = 0.0, palabrasClaveDetectadas = emptyList(),
                windowDurationSec = 0
            )
        }

        val avgDb = historial.map { it.avgDb }.average()
        val peakDb = historial.maxOf { it.peakDb }
        val zcrMean = historial.map { it.zeroCrossingRate }.average()
        val zcrVar = historial.map { (it.zeroCrossingRate - zcrMean).let { d -> d * d } }.average()
        val voicedChunks = historial.count { it.avgDb > VAD_DB_THRESHOLD }
        val vadRatio = voicedChunks.toDouble() / historial.size

        val palabras = keywordSpotter?.obtenerPalabrasRecientes() ?: emptyList()

        return AudioSnapshot(
            avgDb = avgDb,
            peakDb = peakDb,
            pitchVariance = zcrVar * 1000.0,
            voiceActivityRatio = vadRatio,
            palabrasClaveDetectadas = palabras,
            windowDurationSec = (historial.size * 0.5).toInt()
        )
    }

    fun volcarBuffer(): ShortArray {
        synchronized(ringBuffer) {
            val resultado = ShortArray(totalSamples)
            val startIdx = writeIndex
            for (i in 0 until totalSamples) {
                resultado[i] = ringBuffer[(startIdx + i) % totalSamples]
            }
            return resultado
        }
    }

    private data class ChunkStats(
        val avgDb: Double,
        val peakDb: Double,
        val zeroCrossingRate: Double,
        val timestampMs: Long
    )

    companion object {
        private const val TAG = "AudioMonitor"
        private const val VAD_DB_THRESHOLD = 45.0
    }
}
