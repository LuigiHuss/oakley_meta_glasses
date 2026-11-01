package com.meta.wearable.dat.externalsampleapps.cameraaccess.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Persist captures to Android's gallery before CameraAccess discards its temporary previews. */
internal object MediaArchive {
  private const val TAG = "MetaGlassesArchive"

  private fun name(prefix: String, ext: String): String {
    val now = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return "MG_${prefix}_${now}.${ext}"
  }

  fun savePhoto(context: Context, bitmap: Bitmap): Boolean =
      persist(
          context = context,
          collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          mime = "image/jpeg",
          filename = name("IMG", "jpg"),
          album = "Pictures/MetaGlasses/",
      ) { stream -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream) }

  fun saveVideo(context: Context, uri: Uri): Boolean =
      persist(
          context = context,
          collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
          mime = "video/mp4",
          filename = name("VID", "mp4"),
          album = "Movies/MetaGlasses/",
      ) { stream ->
        val input = context.contentResolver.openInputStream(uri) ?: return@persist false
        input.use { it.copyTo(stream) }
        true
      }

  private fun persist(
      context: Context,
      collection: Uri,
      mime: String,
      filename: String,
      album: String,
      copy: (java.io.OutputStream) -> Boolean,
  ): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
      put(MediaStore.MediaColumns.MIME_TYPE, mime)
      put(MediaStore.MediaColumns.RELATIVE_PATH, album)
      put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    var target: Uri? = null
    return try {
      target = resolver.insert(collection, values)
          ?: throw IllegalStateException("MediaStore insert returned null")
      val stream = resolver.openOutputStream(target!!, "w")
          ?: throw IllegalStateException("Could not open MediaStore output")
      val ok = stream.use(copy)
      if (!ok) throw IllegalStateException("Image/video encoding failed")
      values.clear()
      values.put(MediaStore.MediaColumns.IS_PENDING, 0)
      resolver.update(target!!, values, null, null)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Unable to archive $filename", e)
      target?.let { runCatching { resolver.delete(it, null, null) } }
      false
    }
  }
}
