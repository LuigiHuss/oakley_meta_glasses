package com.mirela.app.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mirela.app.MirelaViewModel
import com.mirela.app.Phase
import com.mirela.app.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab { HOME, VOZ, OCULOS, AJUSTES }

@Composable
fun MirelaApp(vm: MirelaViewModel) {
  val ui by vm.ui.collectAsStateWithLifecycle()
  var tab by remember { mutableStateOf(Tab.HOME) }
  MirelaTheme {
    Box(Modifier.fillMaxSize().background(M.Bg)) {
      // Brilhos de fundo (aurora)
      Box(Modifier.offset((-140).dp, (-120).dp).size(360.dp).blur(90.dp).background(M.Violet.copy(alpha = 0.45f), CircleShape))
      Box(Modifier.offset(200.dp, 520.dp).size(360.dp).blur(90.dp).background(M.Magenta.copy(alpha = 0.40f), CircleShape))

      Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.weight(1f)) {
          when (tab) {
            Tab.HOME -> HomeScreen(ui, vm, openVoice = { tab = Tab.VOZ })
            Tab.VOZ -> VoiceScreen(ui, vm)
            Tab.OCULOS -> GlassesScreen(ui, vm)
            Tab.AJUSTES -> SettingsScreen(vm)
          }
        }
        Dock(tab, ui.phase, onTab = { tab = it }, onOrb = {
          tab = Tab.VOZ
          vm.talk()
        })
      }
    }
  }
}

// ---------------- Dock ----------------

@Composable
private fun Dock(tab: Tab, phase: Phase, onTab: (Tab) -> Unit, onOrb: () -> Unit) {
  Row(
      Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          .fillMaxWidth()
          .height(70.dp)
          .glass(RoundedCornerShape(35.dp))
          .background(Color(0xB814141A))
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    DockIcon(Icons.Outlined.Home, "Início", tab == Tab.HOME) { onTab(Tab.HOME) }
    DockIcon(Icons.Outlined.ChatBubbleOutline, "Conversa", tab == Tab.VOZ) { onTab(Tab.VOZ) }
    Box(
        Modifier.size(54.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(SphereBrushColors))
            .clickable(onClick = onOrb),
        contentAlignment = Alignment.Center,
    ) {
      if (phase == Phase.LISTENING) StatusDot(Color.White)
    }
    DockIcon(Icons.Outlined.Visibility, "Óculos", tab == Tab.OCULOS) { onTab(Tab.OCULOS) }
    DockIcon(Icons.Outlined.Settings, "Ajustes", tab == Tab.AJUSTES) { onTab(Tab.AJUSTES) }
  }
}

@Composable
private fun DockIcon(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
  Box(
      Modifier.size(48.dp)
          .clip(CircleShape)
          .background(if (on) Color.White.copy(alpha = 0.12f) else Color.Transparent)
          .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = label, tint = if (on) Color.White else M.Dim)
  }
}

// ---------------- Início ----------------

private data class Module(val label: String, val icon: ImageVector, val exemplo: String, val foto: Boolean)

private val modules =
    listOf(
        Module("Finanças", Icons.Outlined.CreditCard, "“Gastei 45 no mercado”", false),
        Module("Nutrição", Icons.Outlined.Restaurant, "“Analisa meu prato”", true),
        Module("Compras", Icons.Outlined.ShoppingCart, "“Coloca leite na lista”", false),
        Module("Notas", Icons.Outlined.EditNote, "“Anota uma ideia…”", false),
        Module("Onde deixei", Icons.Outlined.Place, "“Guarda onde estacionei”", true),
        Module("Traduzir", Icons.Outlined.Translate, "“Lê e traduz isso”", true),
        Module("Cozinhar", Icons.Outlined.Kitchen, "“Me ensina um strogonoff”", false),
        Module("Galeria", Icons.Outlined.PhotoLibrary, "“Guarda esta foto”", true),
    )

@Composable
fun HomeScreen(ui: UiState, vm: MirelaViewModel, openVoice: () -> Unit) {
  val today = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR")).format(Date()) }
  var exemplo by remember { mutableStateOf("Toque num atalho para falar. A Mirella entende frases como as de baixo de cada um.") }
  Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        T(today.replaceFirstChar { it.uppercase() }, size = 13.sp, color = M.Muted)
        T("Olá, ${vm.settings.nome}", size = 26.sp, family = Jura, weight = FontWeight.Medium)
      }
      GlassesPill(ui)
    }

    Segmented(listOf("Pessoal" to "pessoal", "Profissional" to "profissional"), ui.contexto, vm::setContexto)

    if (!ui.configured) Banner("Configure o endereço do n8n em Ajustes para a Mirella responder.", error = true)
    else if (ui.phase == Phase.ERROR) Banner(ui.info, error = true)

    GradientCard(MagBrush, Modifier.fillMaxWidth(), onClick = {
      openVoice()
      vm.talk()
    }) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.radialGradient(SphereBrushColors)))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          T("M.I.R.E.L.A.", size = 16.sp, family = Jura, weight = FontWeight.Medium, spacing = 3.sp, color = Color.White)
          T("Toque para falar", size = 13.sp, color = Color.White.copy(alpha = 0.85f))
          T("Microfone: ${ui.micSource}", size = 11.sp, color = Color.White.copy(alpha = 0.7f))
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      PillButton("Foto + pergunta", Modifier.weight(1f), primary = true) {
        openVoice()
        vm.talk(withPhoto = true)
      }
      PillButton("Só foto (teste)", Modifier.weight(1f), primary = false) { vm.testPhoto() }
    }

    SectionLabel("Atalhos de voz")
    modules.chunked(4).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEach { m ->
          Column(
              Modifier.weight(1f)
                  .glass(RoundedCornerShape(22.dp))
                  .clickable {
                    exemplo = "${m.label}: diga ${m.exemplo}"
                    openVoice()
                    vm.talk(withPhoto = m.foto)
                  }
                  .padding(vertical = 14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(m.icon, contentDescription = null, tint = M.Lilac)
            T(m.label, size = 11.sp, align = TextAlign.Center)
          }
        }
      }
    }
    T(exemplo, size = 12.sp, color = M.Muted, lineHeight = 17.sp)

    SectionLabel("Respostas recentes")
    if (ui.history.isEmpty()) {
      T("Nada ainda. Fale com a Mirella pelos óculos e as respostas aparecem aqui.", size = 13.sp, color = M.Muted, lineHeight = 18.sp)
    } else {
      Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp))) {
        ui.history.forEach { h ->
          Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusDot(if (h.comFoto) M.Pink else M.Lilac)
            T(h.fala, size = 14.sp, modifier = Modifier.weight(1f), lineHeight = 19.sp)
            T(h.hora, size = 13.sp, color = M.Muted, family = Jura)
          }
        }
      }
    }
    Spacer(Modifier.height(8.dp))
  }
}

@Composable
private fun GlassesPill(ui: UiState) {
  val ok = ui.registered && ui.devices > 0
  Row(
      Modifier.glass(CircleShape).padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    StatusDot(if (ok) M.Green else if (ui.registered) M.Lilac else M.Coral)
    T(if (ok) "HSTN" else if (ui.registered) "Sem óculos" else "Desconectado", size = 13.sp, family = Jura, weight = FontWeight.Medium)
  }
}

// ---------------- Conversa ----------------

private fun phaseLabel(p: Phase) =
    when (p) {
      Phase.IDLE -> "Pronta"
      Phase.LISTENING -> "Escutando…"
      Phase.PROCESSING -> "Processando…"
      Phase.CAPTURING -> "Olhando…"
      Phase.SPEAKING -> "Respondendo…"
      Phase.ERROR -> "Ops"
    }

@Composable
fun VoiceScreen(ui: UiState, vm: MirelaViewModel) {
  Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Box(Modifier.glass(CircleShape).padding(horizontal = 14.dp, vertical = 10.dp)) {
        T("Contexto · ${if (ui.contexto == "pessoal") "Pessoal" else "Profissional"}", size = 12.sp, family = Jura, weight = FontWeight.Medium)
      }
      Box(Modifier.glass(CircleShape).padding(horizontal = 14.dp, vertical = 10.dp)) {
        T("Mic · ${ui.micSource}", size = 12.sp, family = Jura, weight = FontWeight.Medium)
      }
    }

    MirelaOrb(
        ui.phase,
        ui.level,
        Modifier.size(290.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { vm.talk() },
    )

    T(phaseLabel(ui.phase), size = 32.sp, family = Jura, weight = FontWeight.Light)
    T(
        ui.info.ifBlank { if (ui.phase == Phase.IDLE) "Toque na esfera e fale" else "" },
        size = 13.sp,
        color = if (ui.phase == Phase.ERROR) Color(0xFFFFD0DD) else M.Muted,
        align = TextAlign.Center,
        lineHeight = 18.sp,
    )

    if (ui.fala.isNotBlank()) {
      Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).background(M.Violet.copy(alpha = 0.18f)).padding(16.dp)) {
        T(ui.fala, size = 15.sp, lineHeight = 21.sp)
      }
    }

    ui.lastPhoto?.let { bmp ->
      Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Última foto dos óculos",
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(120.dp).clip(RoundedCornerShape(20.dp)),
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      PillButton(if (ui.phase == Phase.LISTENING) "Parar e enviar" else "Falar", Modifier.weight(1f)) { vm.talk() }
      PillButton("Foto + pergunta", Modifier.weight(1f), primary = false) { vm.talk(withPhoto = true) }
    }
  }
}

// ---------------- Óculos ----------------

@Composable
fun GlassesScreen(ui: UiState, vm: MirelaViewModel) {
  val activity = LocalContext.current as Activity
  Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    T("Óculos", size = 30.sp, family = Jura, weight = FontWeight.Light)

    GradientCard(VioBrush, Modifier.fillMaxWidth()) {
      T("Oakley Meta HSTN", size = 20.sp, family = Jura, weight = FontWeight.Medium, color = Color.White)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(if (ui.registered) M.Green else M.Coral)
        T(ui.registrationLabel, size = 13.sp, color = Color.White)
      }
      T("Óculos disponíveis: ${ui.devices} · Câmera: ${ui.cameraPermission}", size = 12.sp, color = Color.White.copy(alpha = 0.8f))
      if (ui.registered) PillButton("Desconectar do Meta AI", Modifier.fillMaxWidth(), primary = false) { vm.unregister(activity) }
      else PillButton("Conectar ao Meta AI", Modifier.fillMaxWidth()) { vm.register(activity) }
    }

    if (ui.info.isNotBlank()) Banner(ui.info, error = ui.phase == Phase.ERROR)

    SectionLabel("Testes")
    PillButton("Testar áudio nos óculos", Modifier.fillMaxWidth(), primary = false) { vm.testAudio() }
    PillButton("Testar microfone", Modifier.fillMaxWidth(), primary = false) { vm.testMic() }
    PillButton("Testar foto dos óculos", Modifier.fillMaxWidth(), primary = false) { vm.testPhoto() }

    ui.lastPhoto?.let { bmp ->
      Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Foto de teste",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(24.dp)),
      )
    }

    Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
      T(
          "Antes de conectar: ative o Modo Desenvolvedor no app Meta AI. " +
              "O microfone usa o Bluetooth dos óculos; a resposta toca nos alto-falantes. " +
              "O “Oi Mirella” por voz entra na próxima versão — por enquanto, toque na esfera.",
          size = 12.sp,
          color = M.Muted,
          lineHeight = 18.sp,
      )
    }
  }
}

// ---------------- Ajustes ----------------

@Composable
fun SettingsScreen(vm: MirelaViewModel) {
  val s = vm.settings
  var url by remember { mutableStateOf(s.webhookUrl) }
  var key by remember { mutableStateOf(s.apiKey) }
  var user by remember { mutableStateOf(s.userId) }
  var nome by remember { mutableStateOf(s.nome) }
  var phoneMic by remember { mutableStateOf(s.allowPhoneMic) }

  Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    T("Ajustes", size = 30.sp, family = Jura, weight = FontWeight.Light)
    SectionLabel("Conexão com o n8n")
    Field("Endereço do webhook", url, "https://seu-n8n/webhook/mirela", KeyboardType.Uri) { url = it }
    Field("Chave do app (X-Mirela-Key)", key, "a mesma senha da credencial no n8n", KeyboardType.Password, secret = true) { key = it }
    SectionLabel("Você")
    Field("Seu nome", nome, "Luigi") { nome = it }
    Field("ID de usuário", user, "luigi") { user = it }

    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        T("Usar microfone do celular", size = 14.sp)
        T("Quando os óculos não estiverem no áudio Bluetooth", size = 12.sp, color = M.Muted)
      }
      Switch(
          checked = phoneMic,
          onCheckedChange = { phoneMic = it },
          colors = SwitchDefaults.colors(checkedTrackColor = M.Magenta, checkedThumbColor = Color.White),
      )
    }

    PillButton("Salvar", Modifier.fillMaxWidth()) { vm.saveSettings(url, key, user, nome, phoneMic) }
    Spacer(Modifier.height(4.dp))
    T("Versão 1.0-teste · SDK Meta Wearables 1.0.1", size = 11.sp, color = M.Dim, modifier = Modifier.width(300.dp))
  }
}

@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    type: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    onChange: (String) -> Unit,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onChange,
      label = { T(label, size = 12.sp, color = M.Muted) },
      placeholder = { T(placeholder, size = 13.sp, color = M.Dim) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = type),
      visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
      shape = RoundedCornerShape(18.dp),
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedBorderColor = M.Pink,
              unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
              focusedTextColor = M.Text,
              unfocusedTextColor = M.Text,
              cursorColor = M.Pink,
          ),
      modifier = Modifier.fillMaxWidth(),
  )
}
