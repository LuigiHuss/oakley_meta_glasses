package com.mirela.app

import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed class MirelaReply {
  /** Resposta normal: áudio mp3 e o texto falado (header X-Mirela-Texto). */
  class Audio(val mp3: ByteArray, val text: String) : MirelaReply()
  /** O n8n pediu uma foto antes de responder. */
  class NeedPhoto(val texto: String, val fala: String) : MirelaReply()
  class Failure(val message: String) : MirelaReply()
}

/** Fala com o webhook /mirela do n8n. */
class MirelaClient {
  private val http =
      OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .writeTimeout(60, TimeUnit.SECONDS)
          .build()

  suspend fun send(s: Settings, audio: ByteArray?, image: ByteArray?, texto: String?): MirelaReply =
      withContext(Dispatchers.IO) {
        val body =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", s.userId)
                .addFormDataPart("nome", s.nome)
                .addFormDataPart("contexto", s.contexto)
                .apply {
                  audio?.let { addFormDataPart("audio", "fala.wav", it.toRequestBody("audio/wav".toMediaType())) }
                  image?.let { addFormDataPart("image", "foto.jpg", it.toRequestBody("image/jpeg".toMediaType())) }
                  if (!texto.isNullOrBlank()) addFormDataPart("texto", texto)
                }
                .build()
        val request =
            try {
              Request.Builder().url(s.webhookUrl).header("X-Mirela-Key", s.apiKey).post(body).build()
            } catch (e: IllegalArgumentException) {
              return@withContext MirelaReply.Failure("O endereço do n8n em Ajustes não é válido.")
            }
        try {
          http.newCall(request).execute().use { r ->
            if (r.code == 401 || r.code == 403) return@withContext MirelaReply.Failure("O n8n recusou a chave. Confira em Ajustes.")
            if (!r.isSuccessful) return@withContext MirelaReply.Failure("O n8n respondeu com erro ${r.code}.")
            val type = r.header("Content-Type") ?: ""
            val bytes = r.body?.bytes() ?: ByteArray(0)
            if (type.contains("json")) {
              val json = JSONObject(String(bytes))
              if (json.optString("acao") == "capturar_foto")
                  MirelaReply.NeedPhoto(json.optString("texto"), json.optString("fala"))
              else MirelaReply.Failure("Resposta inesperada do n8n.")
            } else {
              val text = runCatching { URLDecoder.decode(r.header("X-Mirela-Texto") ?: "", "UTF-8") }.getOrDefault("")
              MirelaReply.Audio(bytes, text)
            }
          }
        } catch (e: IOException) {
          MirelaReply.Failure("Sem conexão com o n8n.")
        } catch (e: Exception) {
          MirelaReply.Failure("Erro ao ler a resposta do n8n.")
        }
      }
}
