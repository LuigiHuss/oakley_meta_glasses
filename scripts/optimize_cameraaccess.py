#!/usr/bin/env python3
"""Patch only the official CameraAccess test sample in the Actions workspace.

The official Meta repository is cloned during each build; no upstream sources
are committed into this private repo. Assertions fail the build on upstream
changes rather than silently generating an unoptimized APK.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("meta-official/samples/CameraAccess")

def replace_once(path: Path, old: str, new: str) -> None:
    if not path.is_file():
        raise SystemExit(f"Arquivo esperado não encontrado: {path}")
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Esperada 1 ocorrência em {path}, encontradas {count}: {old!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"OK: {path.relative_to(root)}: {old} -> {new}")

source = root / "app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess"
vm = source / "camera/CameraViewModel.kt"
ui = source / "camera/CameraUiState.kt"
gradle = root / "app/build.gradle.kts"
manifest = root / "app/src/main/AndroidManifest.xml"

# MEDIUM = 504x896; LOW = 360x640. Both use 24 fps and HEVC.
# Only the STREAM quality is reduced: photo capture remains a separate operation.
replace_once(vm, "videoQuality = VideoQuality.MEDIUM,", "videoQuality = VideoQuality.LOW,")

# HFP/SCO microphone prewarm is a possible source of Bluetooth contention.
# Keep microphone toggle functionality; user can enable it before video recording.
replace_once(ui, "val includeAudioInStream: Boolean = true,", "val includeAudioInStream: Boolean = false,")

# Install next to the original diagnostic APK despite GitHub runner signing keys
# being ephemeral (otherwise Android reports INSTALL_FAILED_UPDATE_INCOMPATIBLE).
replace_once(
    gradle,
    'applicationId = "com.meta.wearable.dat.externalsampleapps.cameraaccess"',
    'applicationId = "dev.luigihuss.metaglasses.optimized"',
)

# Distinguish the optimized app from the original Camera Access on the launcher.
replace_once(manifest, 'android:label="@string/app_name"', 'android:label="Meta Glasses LOW24"')

# A source revision that changes framing or recording must be reviewed separately.
checks = {
    vm: ["private const val FRAME_RATE = 24", "compressVideo = true,"],
    ui: ["val includeAudioInStream: Boolean = false,"],
}
for path, markers in checks.items():
    text = path.read_text(encoding="utf-8")
    for marker in markers:
        if marker not in text:
            raise SystemExit(f"Configuração necessária não encontrada: {path}: {marker}")
print("Perfil v0.2: LOW 360x640 / 24 FPS / HEVC / microfone inicialmente desligado")
