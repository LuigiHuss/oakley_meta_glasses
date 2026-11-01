package dev.luigihuss.metaglasses.audio;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.*;
import android.text.*;
import android.view.Gravity;
import android.widget.*;
import java.io.File;
import java.util.*;

/** Manual, consent-based audio recording. Content indexing comes after transcription. */
public final class MainActivity extends Activity {
    private static final int REQUEST = 42;
    private TextView status;
    private EditText meetingTitle, search;
    private Button start, end;
    private LinearLayout list;
    private MediaPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1200);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView root = new ScrollView(this);
        root.setBackgroundColor(Color.rgb(247,249,252));
        LinearLayout main = new LinearLayout(this);
        main.setPadding(dp(20), dp(26), dp(20), dp(30));
        main.setOrientation(LinearLayout.VERTICAL);
        root.addView(main);

        TextView header = text("META GLASSES AUDIO", 24, true);
        header.setTextColor(Color.rgb(24,72,110));
        main.addView(header);
        main.addView(text("Grave reuniões pelo microfone Bluetooth dos óculos, sem vídeo e sem upload.", 15, false));
        status = text("Verificando...", 15, true);
        status.setPadding(0, dp(18), 0, dp(18));
        main.addView(status);

        main.addView(text("Nome da reunião", 15, true));
        meetingTitle = new EditText(this);
        meetingTitle.setSingleLine(true);
        meetingTitle.setHint("Ex.: Reunião de planejamento");
        main.addView(meetingTitle);

        start = button("Iniciar gravação pelo Oakley");
        start.setOnClickListener(v -> askPermissions());
        main.addView(start);
        end = button("Parar e salvar");
        end.setOnClickListener(v -> {
            startService(new Intent(this, RecorderService.class).setAction(RecorderService.STOP));
            status.setText("Finalizando WAV...");
        });
        main.addView(end);
        TextView safety = text("Avise os participantes e obtenha consentimento. A gravação exibe uma notificação; o aplicativo recusa gravar se o Android usar o microfone do celular.", 13, false);
        safety.setPadding(0, dp(6), 0, dp(20));
        main.addView(safety);

        main.addView(text("Reuniões neste celular", 20, true));
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Pesquisar por título");
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { showMeetings(); }
            public void afterTextChanged(Editable s) { }
        });
        main.addView(search);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        main.addView(list);
        main.addView(text("A pesquisa pelo conteúdo falado, decisões e tarefas será adicionada depois da transcrição automática.", 13, false));

        setContentView(root);
        showMeetings();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size);
        t.setTextColor(Color.rgb(49,62,76));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }
    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value); b.setAllCaps(false); return b;
    }
    private boolean has(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    private void askPermissions() {
        if (!has(Manifest.permission.RECORD_AUDIO) || !has(Manifest.permission.BLUETOOTH_CONNECT)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS}, REQUEST);
            return;
        }
        confirmStart();
    }
    @Override public void onRequestPermissionsResult(int id, String[] permissions, int[] result) {
        super.onRequestPermissionsResult(id, permissions, result);
        if (id != REQUEST) return;
        if (has(Manifest.permission.RECORD_AUDIO) && has(Manifest.permission.BLUETOOTH_CONNECT)) confirmStart();
        else new AlertDialog.Builder(this).setMessage("Sem permissões de áudio/Bluetooth. Nenhuma reunião foi gravada.")
                .setPositiveButton("OK", null).show();
    }
    private void confirmStart() {
        new AlertDialog.Builder(this).setTitle("Iniciar gravação?")
                .setMessage("Os participantes foram informados e concordaram? O microfone do telefone NÃO será usado como alternativa.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar", (d, choice) -> {
                    Intent startIntent = new Intent(this, RecorderService.class)
                        .setAction(RecorderService.START)
                        .putExtra(RecorderService.TITLE, meetingTitle.getText().toString());
                    try {
                        startForegroundService(startIntent);
                        status.setText("Conectando ao microfone Bluetooth...");
                    } catch (Exception ex) {
                        status.setText("Falha ao iniciar: " + ex.getMessage());
                    }
                }).show();
    }
    private void refresh() {
        boolean live = getSharedPreferences(RecorderService.PREFS, MODE_PRIVATE)
                .getBoolean("recording", false);
        String info = getSharedPreferences(RecorderService.PREFS, MODE_PRIVATE)
                .getString("status", "Pronto para testar áudio via Bluetooth");
        status.setText((live ? "● " : "○ ") + info);
        status.setTextColor(live ? Color.rgb(15,130,80) : Color.rgb(50,65,80));
        start.setEnabled(!live);
        end.setEnabled(live);
        showMeetings();
    }
    private void showMeetings() {
        if (list == null) return;
        list.removeAllViews();
        File dir = new File(getFilesDir(), "recordings");
        File[] recordings = dir.listFiles((parent, name) -> name.endsWith(".wav"));
        if (recordings == null || recordings.length == 0) {
            list.addView(text("Nenhuma gravação salva ainda.", 14, false)); return;
        }
        Arrays.sort(recordings, Comparator.comparing(File::getName).reversed());
        String query = search == null ? "" : search.getText().toString().toLowerCase(Locale.ROOT).trim();
        int found = 0;
        for (File file : recordings) {
            String key = file.getName().replace(".wav", "");
            String title = getSharedPreferences(RecorderService.PREFS, MODE_PRIVATE)
                    .getString("title_" + key, "Reunião");
            if (!title.toLowerCase(Locale.ROOT).contains(query) && !key.contains(query)) continue;
            found++;
            Button play = button("▶  " + title + "  (" + (file.length()/1024) + " KB)");
            play.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            play.setOnClickListener(v -> play(file));
            play.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Excluir gravação?")
                    .setMessage(title + "\nO arquivo será excluído definitivamente.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Excluir", (d, c) -> {
                        if (player != null) { player.release(); player = null; }
                        if (file.delete()) showMeetings();
                    }).show();
                return true;
            });
            list.addView(play);
        }
        if (found == 0) list.addView(text("Nenhuma reunião encontrada com esse nome.", 14, false));
    }
    private void play(File file) {
        try {
            if (player != null) { player.release(); player = null; }
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.prepare();
            player.start();
            Toast.makeText(this, "Reproduzindo " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro de reprodução: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onResume() { super.onResume(); handler.post(tick); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }
}
