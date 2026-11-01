package dev.luigihuss.metaglasses.audio;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.*;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** WAV recording without video. Fail closed if Android routes to the phone mic. */
public final class RecorderService extends Service {
    public static final String START = "dev.luigihuss.metaglasses.audio.START";
    public static final String STOP = "dev.luigihuss.metaglasses.audio.STOP";
    public static final String TITLE = "title";
    public static final String PREFS = "meetings";
    private static final int RATE = 16000;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private AudioRecord recorder;
    private AudioManager manager;
    private Thread writer;
    private File part, wav;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent == null) return START_NOT_STICKY;
        if (STOP.equals(intent.getAction())) {
            running.set(false);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!START.equals(intent.getAction()) || running.get()) return START_NOT_STICKY;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            status("Permissão de áudio ou Bluetooth ausente", false);
            stopSelf(); return START_NOT_STICKY;
        }
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("meeting_recording", "Gravação de reuniões", NotificationManager.IMPORTANCE_LOW));
            PendingIntent end = PendingIntent.getService(this, 0, new Intent(this, RecorderService.class).setAction(STOP),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(this, "meeting_recording")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentTitle("Meta Glasses Audio: gravando reunião")
                    .setContentText("Áudio local — toque para abrir")
                    .setOngoing(true).setContentIntent(open)
                    .addAction(android.R.drawable.ic_media_pause, "Parar", end).build();
            startForeground(51, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            begin(intent.getStringExtra(TITLE));
        } catch (Exception e) {
            status("Erro: " + e.getClass().getSimpleName() + " - " + e.getMessage(), false);
            running.set(false);
            releaseAudio();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private static boolean bluetooth(AudioDeviceInfo d) {
        return d != null && (d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                d.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET);
    }

    private void begin(String title) throws Exception {
        manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioDeviceInfo output = null;
        for (AudioDeviceInfo d : manager.getAvailableCommunicationDevices())
            if (bluetooth(d)) { output = d; break; }
        if (output == null)
            throw new IllegalStateException("Nenhum headset Bluetooth de chamadas disponível; veja a configuração Bluetooth do Oakley");
        manager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if (!manager.setCommunicationDevice(output))
            throw new IllegalStateException("Android recusou o canal de comunicação Bluetooth");

        int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IllegalStateException("Áudio PCM não disponível");
        recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(Math.max(min * 2, 8192))
                .build();
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED)
            throw new IllegalStateException("Não foi possível iniciar AudioRecord");
        for (AudioDeviceInfo d : manager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            if (bluetooth(d)) { recorder.setPreferredDevice(d); break; }
        }
        recorder.startRecording();
        AudioDeviceInfo routed = recorder.getRoutedDevice();
        if (!bluetooth(routed)) {
            Thread.sleep(300);
            routed = recorder.getRoutedDevice();
        }
        if (!bluetooth(routed))
            throw new IllegalStateException("Microfone não é Bluetooth. Gravação bloqueada para evitar usar o telefone");

        File dir = new File(getFilesDir(), "recordings");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Não foi possível criar pasta");
        String key = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        part = new File(dir, key + ".part");
        wav = new File(dir, key + ".wav");
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("title_" + key,
                title == null || title.trim().isEmpty() ? "Reunião" : title.trim()).apply();
        running.set(true);
        status("Gravando do microfone Bluetooth (tipo " + routed.getType() + ")", true);
        writer = new Thread(this::writeRecording, "meeting-wav");
        writer.start();
    }

    private void writeRecording() {
        long bytes = 0;
        String error = null;
        boolean valid = false;
        try (RandomAccessFile file = new RandomAccessFile(part, "rw")) {
            file.write(new byte[44]);
            byte[] buffer = new byte[8192];
            while (running.get()) {
                int n = recorder.read(buffer, 0, buffer.length);
                if (n < 0) throw new IOException("Erro de leitura PCM: " + n);
                if (n > 0) { file.write(buffer, 0, n); bytes += n; }
            }
            if (bytes > 0) { file.seek(0); header(file, bytes); valid = true; }
        } catch (Exception e) { error = e.toString(); }
        finally {
            running.set(false);
            if (valid && part.renameTo(wav)) status("Salvo: " + wav.getName(), false);
            else status("Áudio incompleto: " + (error == null ? "sem dados/erro ao salvar" : error), false);
            releaseAudio();
            stopSelf();
        }
    }

    private static void u16(RandomAccessFile f, int v) throws IOException {
        f.write(v & 255); f.write((v >>> 8) & 255);
    }
    private static void u32(RandomAccessFile f, long v) throws IOException {
        for (int i=0; i<4; i++) f.write((int)(v >>> (8*i)) & 255);
    }
    private static void header(RandomAccessFile f, long size) throws IOException {
        if (size > 0xFFFFFFFFL-44) throw new IOException("WAV maior que 4GB");
        f.writeBytes("RIFF"); u32(f, size+36); f.writeBytes("WAVE");
        f.writeBytes("fmt "); u32(f, 16); u16(f, 1); u16(f, 1);
        u32(f, RATE); u32(f, RATE*2); u16(f, 2); u16(f, 16);
        f.writeBytes("data"); u32(f, size);
    }

    private void status(String msg, boolean live) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("recording", live).putString("status", msg).apply();
    }
    private synchronized void releaseAudio() {
        if (recorder != null) {
            try { if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) recorder.stop(); }
            catch (Exception ignored) { }
            recorder.release(); recorder = null;
        }
        if (manager != null) {
            manager.clearCommunicationDevice();
            manager.setMode(AudioManager.MODE_NORMAL);
            manager = null;
        }
    }
    @Override public void onDestroy() {
        running.set(false); // The writer finishes the WAV before releasing the recorder.
        if (writer == null) releaseAudio();
        super.onDestroy();
    }
}
