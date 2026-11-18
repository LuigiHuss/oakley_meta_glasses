package com.mirela.app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Microfone e alto-falante dos óculos.
 *
 * Entrada: perfil Bluetooth HFP (SCO), o mesmo caminho usado no exemplo oficial da Meta.
 * Saída: depois de gravar, o SCO é liberado e a resposta toca como mídia (A2DP), com melhor
 * qualidade, nos alto-falantes dos óculos.
 */
class GlassesAudio(context: Context) {
  private val ctx = context.applicationContext
  private val am = ctx.getSystemService(AudioManager::class.java)
  private var previousMode = AudioManager.MODE_NORMAL
  private var routed = false
  @Volatile private var stopRequested = false

  companion object {
    private const val TAG = "MirelaAudio"
    const val SAMPLE_RATE = 16000
    /** Volume (RMS) a partir do qual consideramos que há fala. Ajuste se cortar cedo ou não parar. */
    const val VOICE_THRESHOLD = 900.0
    const val SILENCE_TO_STOP_MS = 1200L
    const val NO_SPEECH_TIMEOUT_MS = 6000L
    const val MAX_UTTERANCE_MS = 15000L
  }

  fun glassesAudioAvailable(): Boolean =
      runCatching { am.availableCommunicationDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } }
          .getOrDefault(false)

  /** Direciona a captura para o microfone dos óculos. Retorna false se não houver óculos no SCO. */
  suspend fun routeToGlasses(): Boolean {
    if (routed) return true
    val device =
        runCatching {
              am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            }
            .getOrNull() ?: return false
    return try {
      previousMode = am.mode
      routed = true
      am.mode = AudioManager.MODE_IN_COMMUNICATION
      val ok =
          am.setCommunicationDevice(device) &&
              withTimeoutOrNull(3000L) {
                while (am.communicationDevice?.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO) delay(100)
                true
              } == true
      if (!ok) release()
      ok
    } catch (e: Exception) {
      Log.w(TAG, "Falha ao rotear áudio para os óculos", e)
      release()
      false
    }
  }

  fun release() {
    if (!routed) return
    runCatching { am.clearCommunicationDevice() }
    runCatching { am.mode = previousMode }
    routed = false
  }

  fun requestStop() {
    stopRequested = true
  }

  /**
   * Grava uma fala: começa quando o usuário fala e para sozinha depois de um silêncio curto.
   * Retorna o áudio em WAV (16 kHz, mono) ou null se ninguém falou.
   */
  @SuppressLint("MissingPermission")
  suspend fun recordUtterance(onLevel: (Float) -> Unit): ByteArray? =
      withContext(Dispatchers.IO) {
        stopRequested = false
        val minBuf =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return@withContext null
        val record =
            try {
              AudioRecord(
                  MediaRecorder.AudioSource.VOICE_RECOGNITION,
                  SAMPLE_RATE,
                  AudioFormat.CHANNEL_IN_MONO,
                  AudioFormat.ENCODING_PCM_16BIT,
                  minBuf * 2,
              )
            } catch (e: Exception) {
              Log.w(TAG, "AudioRecord indisponível", e)
              return@withContext null
            }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
          record.release()
          return@withContext null
        }
        if (routed) {
          am.getDevices(AudioManager.GET_DEVICES_INPUTS)
              .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
              ?.let { record.setPreferredDevice(it) }
        }

        val pcm = ByteArrayOutputStream()
        val buffer = ShortArray(SAMPLE_RATE / 20) // 50 ms
        val start = SystemClock.elapsedRealtime()
        var lastVoice = start
        var heardVoice = false
        try {
          record.startRecording()
          while (isActive && !stopRequested) {
            val n = record.read(buffer, 0, buffer.size)
            if (n <= 0) continue
            var sum = 0.0
            for (i in 0 until n) {
              val s = buffer[i].toInt()
              sum += (s * s).toDouble()
              pcm.write(s and 0xff)
              pcm.write((s shr 8) and 0xff)
            }
            val rms = sqrt(sum / n)
            onLevel((rms / 3000.0).coerceIn(0.0, 1.0).toFloat())
            val now = SystemClock.elapsedRealtime()
            if (rms > VOICE_THRESHOLD) {
              lastVoice = now
              heardVoice = true
            }
            if (heardVoice && now - lastVoice > SILENCE_TO_STOP_MS) break
            if (!heardVoice && now - start > NO_SPEECH_TIMEOUT_MS) break
            if (now - start > MAX_UTTERANCE_MS) break
          }
        } finally {
          runCatching { record.stop() }
          record.release()
          onLevel(0f)
        }
        // Parada manual também vale: se o usuário tocou em "Parar" depois de falar.
        if (!heardVoice && !(stopRequested && pcm.size() > SAMPLE_RATE)) null
        else wav(pcm.toByteArray(), SAMPLE_RATE)
      }

  /** Toca o mp3 de resposta. Com o SCO liberado, o Android envia para os óculos como mídia. */
  suspend fun play(mp3: ByteArray) {
    val file = withContext(Dispatchers.IO) {
      File.createTempFile("mirela_", ".mp3", ctx.cacheDir).apply { writeBytes(mp3) }
    }
    suspendCancellableCoroutine { cont ->
      val player = MediaPlayer()
      fun finish() {
        runCatching { player.release() }
        file.delete()
        if (cont.isActive) cont.resume(Unit)
      }
      try {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
        player.setDataSource(file.absolutePath)
        player.setOnCompletionListener { finish() }
        player.setOnErrorListener { _, _, _ ->
          finish()
          true
        }
        player.prepare()
        player.start()
      } catch (e: Exception) {
        Log.w(TAG, "Falha ao tocar resposta", e)
        finish()
      }
      cont.invokeOnCancellation {
        runCatching { player.stop() }
        runCatching { player.release() }
        file.delete()
      }
    }
  }

  private fun wav(pcm: ByteArray, rate: Int): ByteArray {
    val out = ByteArrayOutputStream()
    fun i32(v: Int) {
      out.write(v and 0xff); out.write((v shr 8) and 0xff); out.write((v shr 16) and 0xff); out.write((v shr 24) and 0xff)
    }
    fun i16(v: Int) {
      out.write(v and 0xff); out.write((v shr 8) and 0xff)
    }
    out.write("RIFF".toByteArray()); i32(36 + pcm.size); out.write("WAVE".toByteArray())
    out.write("fmt ".toByteArray()); i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
    out.write("data".toByteArray()); i32(pcm.size); out.write(pcm)
    return out.toByteArray()
  }
}
