package com.mirela.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.core.types.RegistrationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { IDLE, LISTENING, PROCESSING, CAPTURING, SPEAKING, ERROR }

data class Exchange(val fala: String, val hora: String, val comFoto: Boolean)

data class UiState(
    val phase: Phase = Phase.IDLE,
    val level: Float = 0f,
    val fala: String = "",
    val info: String = "",
    val micSource: String = "—",
    val sdkReady: Boolean = false,
    val registered: Boolean = false,
    val registrationLabel: String = "SDK não iniciado",
    val devices: Int = 0,
    val cameraPermission: String = "não verificada",
    val history: List<Exchange> = emptyList(),
    val lastPhoto: Bitmap? = null,
    val contexto: String = "pessoal",
    val configured: Boolean = false,
)

class MirelaViewModel(app: Application) : AndroidViewModel(app) {
  val settings = Settings(app)
  private val audio = GlassesAudio(app)
  private val client = MirelaClient()
  private val camera = GlassesCamera()

  private val _ui =
      MutableStateFlow(UiState(contexto = settings.contexto, configured = settings.webhookUrl.isNotBlank()))
  val ui: StateFlow<UiState> = _ui.asStateFlow()

  private var job: Job? = null
  private var tts: TextToSpeech? = null
  private var sdkCollectorsStarted = false

  /** Definido pela MainActivity: abre o pedido de permissão do app Meta AI. */
  var permissionRequester: (suspend (Permission) -> PermissionStatus)? = null

  init {
    tts = TextToSpeech(app) { status ->
      if (status == TextToSpeech.SUCCESS) tts?.language = Locale.forLanguageTag("pt-BR")
    }
  }

  // ---------- SDK dos óculos ----------

  fun onSdkReady() {
    if (sdkCollectorsStarted) return
    sdkCollectorsStarted = true
    _ui.update { it.copy(sdkReady = true) }
    viewModelScope.launch {
      Wearables.registrationState.collect { state ->
        val registered = state == RegistrationState.REGISTERED
        _ui.update {
          it.copy(registered = registered, registrationLabel = if (registered) "Conectado ao Meta AI" else state.toString())
        }
      }
    }
    viewModelScope.launch { Wearables.devices.collect { d -> _ui.update { it.copy(devices = d.size) } } }
  }

  fun register(activity: Activity) {
    if (!_ui.value.sdkReady) return notice("Permita o Bluetooth para iniciar o SDK dos óculos.")
    Wearables.startRegistration(activity)
  }

  fun unregister(activity: Activity) {
    if (_ui.value.sdkReady) Wearables.startUnregistration(activity)
  }

  // ---------- Conversa ----------

  /** Toque na esfera: começa a ouvir. Se já estiver ouvindo, encerra a fala e envia. */
  fun talk(withPhoto: Boolean = false) {
    if (_ui.value.phase == Phase.LISTENING) {
      audio.requestStop()
      return
    }
    if (job?.isActive == true) return
    job = viewModelScope.launch {
      if (settings.webhookUrl.isBlank()) {
        fail("Configure o endereço do n8n em Ajustes.")
        return@launch
      }
      var photo: ByteArray? = null
      if (withPhoto) {
        phase(Phase.CAPTURING, "Tirando foto pelos óculos…")
        photo = takePhoto().getOrElse {
          fail(it.message ?: "Falha ao tirar a foto.")
          return@launch
        }
      }
      val wav = listen() ?: return@launch
      phase(Phase.PROCESSING, "Enviando para a Mirella…")
      handle(client.send(settings, wav, photo, null), photo != null)
    }
  }

  private suspend fun listen(): ByteArray? {
    if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) !=
        PackageManager.PERMISSION_GRANTED) {
      fail("Permita o uso do microfone para o app Mirella.")
      return null
    }
    phase(Phase.LISTENING, "Pode falar")
    val onGlasses = audio.routeToGlasses()
    if (!onGlasses && !settings.allowPhoneMic) {
      audio.release()
      fail("O áudio dos óculos não está disponível. Verifique o Bluetooth.")
      return null
    }
    _ui.update { it.copy(micSource = if (onGlasses) "Óculos" else "Celular") }
    val wav =
        try {
          audio.recordUtterance { lvl -> _ui.update { it.copy(level = lvl) } }
        } finally {
          audio.release()
        }
    if (wav == null) {
      phase(Phase.IDLE, "Não ouvi nada. Toque e fale de novo.")
      return null
    }
    return wav
  }

  private suspend fun handle(reply: MirelaReply, comFoto: Boolean) {
    when (reply) {
      is MirelaReply.Audio -> {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val texto = reply.text.ifBlank { "(resposta em áudio)" }
        _ui.update {
          it.copy(
              phase = Phase.SPEAKING,
              fala = reply.text,
              info = "",
              history = (listOf(Exchange(texto, hora, comFoto)) + it.history).take(30),
          )
        }
        audio.play(reply.mp3)
        phase(Phase.IDLE, "")
      }
      is MirelaReply.NeedPhoto -> {
        phase(Phase.CAPTURING, reply.fala.ifBlank { "Tirando foto…" })
        val photo = takePhoto().getOrElse {
          fail(it.message ?: "Falha ao tirar a foto.")
          return
        }
        phase(Phase.PROCESSING, "Analisando a foto…")
        handle(client.send(settings, null, photo, reply.texto), true)
      }
      is MirelaReply.Failure -> fail(reply.message)
    }
  }

  // ---------- Câmera ----------

  private suspend fun takePhoto(): Result<ByteArray> {
    if (!_ui.value.sdkReady) return Result.failure(IllegalStateException("O SDK dos óculos ainda não iniciou."))
    if (!_ui.value.registered)
        return Result.failure(IllegalStateException("Conecte o app ao Meta AI na aba Óculos."))
    if (!ensureCameraPermission()) return Result.failure(IllegalStateException("Permissão de câmera dos óculos negada."))
    val result = camera.capture()
    result.onSuccess { bytes ->
      _ui.update { it.copy(lastPhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) }
    }
    return result
  }

  private suspend fun ensureCameraPermission(): Boolean {
    var status: PermissionStatus? = null
    Wearables.checkPermissionStatus(Permission.CAMERA).onSuccess { status = it }
    if (status == PermissionStatus.Granted) {
      _ui.update { it.copy(cameraPermission = "permitida") }
      return true
    }
    val requester = permissionRequester ?: return false
    val result = requester(Permission.CAMERA)
    val granted = result == PermissionStatus.Granted
    _ui.update { it.copy(cameraPermission = if (granted) "permitida" else "negada") }
    return granted
  }

  // ---------- Testes ----------

  fun testPhoto() {
    if (job?.isActive == true) return
    job = viewModelScope.launch {
      phase(Phase.CAPTURING, "Tirando foto pelos óculos…")
      takePhoto().fold(
          onSuccess = { phase(Phase.IDLE, "Foto recebida dos óculos.") },
          onFailure = { fail(it.message ?: "Falha ao tirar a foto.") },
      )
    }
  }

  fun testMic() {
    if (job?.isActive == true) return
    job = viewModelScope.launch {
      val wav = listen() ?: return@launch
      val secs = (wav.size - 44) / (GlassesAudio.SAMPLE_RATE * 2f)
      phase(Phase.IDLE, "Gravação ok: %.1f s pelo microfone do %s.".format(secs, _ui.value.micSource.lowercase()))
      speak("Ouvi você. O microfone está funcionando.")
    }
  }

  fun testAudio() = speak("Oi, ${settings.nome}. Este é um teste de áudio da Mirella.")

  // ---------- Ajustes ----------

  fun setContexto(c: String) {
    settings.contexto = c
    _ui.update { it.copy(contexto = c) }
  }

  fun saveSettings(url: String, key: String, userId: String, nome: String, phoneMic: Boolean) {
    settings.webhookUrl = url
    settings.apiKey = key
    settings.userId = userId.ifBlank { "luigi" }
    settings.nome = nome.ifBlank { "Luigi" }
    settings.allowPhoneMic = phoneMic
    _ui.update { it.copy(configured = url.isNotBlank(), info = "Ajustes salvos.", phase = Phase.IDLE) }
  }

  fun notice(msg: String) = _ui.update { it.copy(info = msg) }

  // ---------- Auxiliares ----------

  private fun phase(p: Phase, info: String) = _ui.update { it.copy(phase = p, info = info) }

  private fun fail(msg: String) {
    _ui.update { it.copy(phase = Phase.ERROR, info = msg, level = 0f) }
    speak(msg)
  }

  private fun speak(text: String) {
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mirela")
  }

  override fun onCleared() {
    audio.release()
    tts?.shutdown()
    super.onCleared()
  }
}
