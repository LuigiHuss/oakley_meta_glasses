package com.mirela.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Tira uma foto pela câmera dos óculos e devolve em JPEG.
 * Abre uma sessão, liga o stream por alguns instantes, captura e fecha tudo.
 */
class GlassesCamera {

  suspend fun capture(timeoutMs: Long = 20_000L): Result<ByteArray> {
    var session: DeviceSession? = null
    var camera: Camera? = null
    try {
      val result =
          withTimeoutOrNull(timeoutMs) {
            var err: String? = null

            Wearables.createSession(AutoDeviceSelector())
                .onSuccess { session = it }
                .onFailure { e, _ -> err = e.description }
            val s = session
                ?: return@withTimeoutOrNull Result.failure<ByteArray>(
                    Exception(err ?: "Não foi possível abrir a sessão com os óculos."))

            s.start()
            s.state.first { it == DeviceSessionState.STARTED }

            s.addCamera(StreamConfiguration(videoQuality = VideoQuality.HIGH, frameRate = 15))
                .onSuccess { camera = it }
                .onFailure { e, _ -> err = e.description }
            val cam = camera
                ?: return@withTimeoutOrNull Result.failure<ByteArray>(
                    Exception(err ?: "A câmera dos óculos não está disponível."))

            cam.stream.start().onFailure { e, _ -> err = e.description }
            err?.let { return@withTimeoutOrNull Result.failure<ByteArray>(Exception(it)) }

            cam.stream.state.first { it == StreamState.STREAMING }
            delay(400) // deixa a exposição estabilizar antes da foto

            var jpeg: ByteArray? = null
            cam.stream.capturePhoto()
                .onSuccess { photo -> jpeg = withContext(Dispatchers.Default) { toJpeg(photo) } }
                .onFailure { e, _ -> err = e.description }

            val bytes = jpeg
            if (bytes != null) Result.success(bytes)
            else Result.failure<ByteArray>(Exception(err ?: "Não consegui converter a foto."))
          }
      return result ?: Result.failure(Exception("Os óculos não responderam a tempo."))
    } finally {
      runCatching { camera?.stop() }
      runCatching { session?.stop() }
    }
  }

  private fun toJpeg(photo: PhotoData): ByteArray? {
    val bitmap: Bitmap =
        when (photo) {
          is PhotoData.Bitmap -> photo.bitmap
          is PhotoData.HEIC -> {
            val buf = photo.data.duplicate().apply { rewind() }
            val bytes = ByteArray(buf.remaining())
            buf.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
          }
          else -> return null
        }
    // Limita o tamanho para economizar upload e tokens de visão.
    val maxSide = 1280
    val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
    val finalBitmap =
        if (scale < 1f)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        else bitmap
    return ByteArrayOutputStream().also { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
  }
}
