#!/usr/bin/env python3
"""Compose ONE APK from Meta's CameraAccess example + our local audio meeting recorder.

Runs in GitHub Actions after cloning the official Meta sample. Never commits vendor
sources. Every critical code replacement is verified: upstream drift fails the build.
"""
from pathlib import Path
import re
import shutil
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 2:
    raise SystemExit("usage: build_unified.py /path/to/CameraAccess")
root = Path(sys.argv[1]).resolve()
if not (root / "app/build.gradle.kts").exists():
    raise SystemExit("CameraAccess official Gradle project not found")

pkg = "com.meta.wearable.dat.externalsampleapps.cameraaccess"
src = root / "app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess"
local = Path(__file__).resolve().parent.parent

def exact(path: Path, old: str, new: str) -> None:
    body = path.read_text(encoding="utf-8")
    found = body.count(old)
    if found != 1:
        raise RuntimeError(f"{path}: expected one replacement anchor, got {found}: {old[:120]!r}")
    path.write_text(body.replace(old, new, 1), encoding="utf-8")
    print("Patched:", path.relative_to(root))

# Separate APK ID so the working Camera Access and Audio v0.1 remain installed.
gradle = root / "app/build.gradle.kts"
exact(gradle,
      'applicationId = "com.meta.wearable.dat.externalsampleapps.cameraaccess"',
      'applicationId = "dev.luigihuss.metaglasses.unificado"')

# Add native Java activities and recorder service to the official app module.
for name in ("MetaGlassesHomeActivity.java", "MediaLibraryActivity.java"):
    shutil.copy2(local / "unified-app" / name, src / name)
for original, destination in (("MainActivity.java", "MeetingActivity.java"),
                               ("RecorderService.java", "RecorderService.java")):
    text = (local / "audio-app/app/src/main/java/dev/luigihuss/metaglasses/audio" / original).read_text(encoding="utf-8")
    if "package dev.luigihuss.metaglasses.audio;" not in text:
        raise RuntimeError("Unexpected audio source package")
    text = text.replace("package dev.luigihuss.metaglasses.audio;", "package " + pkg + ";")
    if original == "MainActivity.java":
        if text.count("public final class MainActivity") != 1:
            raise RuntimeError("Unexpected audio Activity class name")
        text = text.replace("public final class MainActivity", "public final class MeetingActivity")
    else:
        text = text.replace("MainActivity.class", "MeetingActivity.class")
    (src / destination).write_text(text, encoding="utf-8")

shutil.copy2(local / "unified-app/MediaArchive.kt", src / "camera/MediaArchive.kt")

# Photos/videos in stock CameraAccess are transient: persist to Android MediaStore
# BEFORE showing the sample's share preview or deleting its temporary MP4.
vm = src / "camera/CameraViewModel.kt"
exact(vm,
      "if (bitmap != null) {\n               _uiState.update {",
      """if (bitmap != null) {
               val archived = withContext(Dispatchers.IO) {
                 MediaArchive.savePhoto(getApplication(), bitmap)
               }
               if (!archived) {
                 wearablesViewModel.setRecentError("Foto capturada, mas não foi possível salvá-la na galeria")
               }
               _uiState.update {""")
exact(vm,
      """is RecordingResult.Completed ->
           _uiState.update { it.copy(activePreview = CapturePreview.Video(result.uri)) }""",
      """is RecordingResult.Completed -> {
           val archived = withContext(Dispatchers.IO) {
             MediaArchive.saveVideo(getApplication(), result.uri)
           }
           if (!archived) {
             wearablesViewModel.setRecentError("Vídeo capturado, mas não foi possível salvá-lo na galeria")
           }
           _uiState.update { it.copy(activePreview = CapturePreview.Video(result.uri)) }
         }""")

# Keep the official MainActivity and its cameraaccess:// registration callback.
# Put a new Home launcher IN THE SAME APK.
manifest = root / "app/src/main/AndroidManifest.xml"
ns = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ns)
ET.register_namespace("tools", "http://schemas.android.com/tools")
A = lambda key: "{" + ns + "}" + key
doc = ET.parse(manifest)
top = doc.getroot()
for permission in ("android.permission.FOREGROUND_SERVICE_MICROPHONE",
                   "android.permission.POST_NOTIFICATIONS"):
    if not any(p.get(A("name")) == permission for p in top.findall("uses-permission")):
        elem = ET.Element("uses-permission", {A("name"): permission})
        top.insert(0, elem)
app = top.find("application")
if app is None:
    raise RuntimeError("Android application element not found")
app.set(A("label"), "Meta Glasses")
camera = next((a for a in app.findall("activity") if a.get(A("name")) == ".MainActivity"), None)
if camera is None:
    raise RuntimeError("Official CameraAccess MainActivity not found")
removed = 0
for intent_filter in list(camera.findall("intent-filter")):
    if any(x.get(A("name")) == "android.intent.action.MAIN" for x in intent_filter.findall("action")):
        camera.remove(intent_filter)
        removed += 1
if removed != 1:
    raise RuntimeError(f"Expected one original launcher, found {removed}")

home = ET.Element("activity", {A("name"): ".MetaGlassesHomeActivity", A("exported"): "true"})
launch = ET.SubElement(home, "intent-filter")
ET.SubElement(launch, "action", {A("name"): "android.intent.action.MAIN"})
ET.SubElement(launch, "category", {A("name"): "android.intent.category.LAUNCHER"})
home.append(ET.Comment("Single entry point; other activities are internal"))
app.insert(0, home)
for name in (".MeetingActivity", ".MediaLibraryActivity"):
    app.append(ET.Element("activity", {A("name"): name, A("exported"): "false"}))
app.append(ET.Element("service", {
    A("name"): ".RecorderService",
    A("exported"): "false",
    A("stopWithTask"): "false",
    A("foregroundServiceType"): "microphone",
}))
ET.indent(doc, space="  ")
doc.write(manifest, encoding="utf-8", xml_declaration=True)
print("Combined application manifest generated:", manifest)
print("APP: Meta Glasses | package: dev.luigihuss.metaglasses.unificado")
print("MODES: Camera DAT, Bluetooth audio meeting recorder, MediaStore media library")
