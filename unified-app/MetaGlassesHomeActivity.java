package com.meta.wearable.dat.externalsampleapps.cameraaccess;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Single launcher: all modes belong to one installed Meta Glasses application. */
public final class MetaGlassesHomeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245, 249, 252));
        LinearLayout content = new LinearLayout(this);
        content.setPadding(dp(22), dp(34), dp(22), dp(22));
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        TextView title = label("META GLASSES", 30, true);
        title.setTextColor(Color.rgb(26, 77, 117));
        content.addView(title);
        content.addView(label("Seu assistente de captura • v0.3", 16, false));
        content.addView(label("Escolha o que deseja registrar com seus Oakley Meta HSTN.", 15, false));

        Button audio = button("🎙  Reuniões e gravações de áudio");
        audio.setOnClickListener(v -> startActivity(new Intent(this, MeetingActivity.class)));
        content.addView(audio);

        Button camera = button("📷  Câmera, fotos e vídeos");
        camera.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        content.addView(camera);

        Button gallery = button("▣  Biblioteca de fotos e vídeos");
        gallery.setOnClickListener(v -> startActivity(new Intent(this, MediaLibraryActivity.class)));
        content.addView(gallery);

        content.addView(label("As fotos e os vídeos desta versão ficam salvos nos álbuns MetaGlasses da galeria. Os áudios ficam privados no aplicativo. As transcrições e a busca pelo que foi dito serão a próxima etapa.", 14, false));
        content.addView(label("Para gravar uma reunião, encerre antes a sessão de câmera, se estiver ativa. Informe os participantes e obtenha consentimento.", 13, false));
        setContentView(scroll);
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String str, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(str);
        view.setTextSize(size);
        view.setGravity(Gravity.START);
        view.setTextColor(Color.rgb(39, 56, 75));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(13), 0, dp(13));
        return view;
    }
    private Button button(String text) {
        Button result = new Button(this);
        result.setText(text);
        result.setAllCaps(false);
        result.setTextSize(17);
        return result;
    }
}
