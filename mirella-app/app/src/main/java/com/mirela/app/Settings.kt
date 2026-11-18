package com.mirela.app

import android.content.Context

/** Configurações salvas no celular (endereço do n8n, chave, usuário). */
class Settings(context: Context) {
  private val prefs = context.getSharedPreferences("mirela", Context.MODE_PRIVATE)

  var webhookUrl: String
    get() = prefs.getString("webhook_url", "") ?: ""
    set(v) = prefs.edit().putString("webhook_url", v.trim()).apply()

  var apiKey: String
    get() = prefs.getString("api_key", "") ?: ""
    set(v) = prefs.edit().putString("api_key", v.trim()).apply()

  var userId: String
    get() = prefs.getString("user_id", "luigi") ?: "luigi"
    set(v) = prefs.edit().putString("user_id", v.trim()).apply()

  var nome: String
    get() = prefs.getString("nome", "Luigi") ?: "Luigi"
    set(v) = prefs.edit().putString("nome", v.trim()).apply()

  var contexto: String
    get() = prefs.getString("contexto", "pessoal") ?: "pessoal"
    set(v) = prefs.edit().putString("contexto", v).apply()

  /** Se os óculos não estiverem no áudio Bluetooth, usa o microfone do celular (útil para testar). */
  var allowPhoneMic: Boolean
    get() = prefs.getBoolean("allow_phone_mic", true)
    set(v) = prefs.edit().putBoolean("allow_phone_mic", v).apply()
}
