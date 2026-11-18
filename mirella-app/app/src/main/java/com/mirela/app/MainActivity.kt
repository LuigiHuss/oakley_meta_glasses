package com.mirela.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.PermissionStatus
import com.mirela.app.ui.MirelaApp
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainActivity : ComponentActivity() {
  private val vm: MirelaViewModel by viewModels()
  private var sdkStarted = false

  private val androidPermissions =
      arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.RECORD_AUDIO)

  private val androidPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) initSdk()
        else vm.notice("Permita o Bluetooth para conectar os óculos.")
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) vm.notice("Permita o microfone para falar com a Mirella.")
      }

  // Pedido de permissão de câmera dos óculos, feito pelo app Meta AI.
  private var datContinuation: CancellableContinuation<PermissionStatus>? = null
  private val datMutex = Mutex()
  private val datPermissionLauncher =
      registerForActivityResult(Wearables.RequestPermissionContract()) { result ->
        datContinuation?.resume(result.getOrDefault(PermissionStatus.Denied))
        datContinuation = null
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    vm.permissionRequester = { permission ->
      datMutex.withLock {
        suspendCancellableCoroutine { cont ->
          datContinuation = cont
          cont.invokeOnCancellation { datContinuation = null }
          datPermissionLauncher.launch(permission)
        }
      }
    }
    setContent { MirelaApp(vm) }

    if (androidPermissions.all { hasPermission(it) }) initSdk()
    else androidPermissionLauncher.launch(androidPermissions)
  }

  private fun initSdk() {
    if (sdkStarted) return
    sdkStarted = true
    Wearables.initialize(this)
    vm.onSdkReady()
  }

  private fun hasPermission(p: String) =
      ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
}
