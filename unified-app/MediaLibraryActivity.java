package com.meta.wearable.dat.externalsampleapps.cameraaccess;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import android.view.Gravity;
import java.util.*;

/** Locates images/videos this application has saved in its MediaStore albums. */
public final class MediaLibraryActivity extends Activity {
    private LinearLayout list;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(247, 250, 253));
        scroll.addView(root);
        TextView title = label("Fotos e vídeos salvos", 23);
        root.addView(title);
        root.addView(label("Capturas arquivadas automaticamente na galeria do Android (MetaGlasses).", 14));
        Button reload = new Button(this);
        reload.setText("Atualizar biblioteca");
        reload.setOnClickListener(v -> refresh());
        root.addView(reload);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        setContentView(scroll);
        refresh();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(Color.rgb(36, 63, 84));
        t.setPadding(0, dp(10), 0, dp(10)); t.setGravity(Gravity.START);
        return t;
    }

    private void refresh() {
        list.removeAllViews();
        int n = 0;
        n += load(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures/MetaGlasses/", "image/*", "Foto");
        n += load(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/MetaGlasses/", "video/*", "Vídeo");
        if (n == 0) list.addView(label("Ainda não existem fotos ou vídeos arquivados por esta versão.", 14));
    }

    private int load(Uri collection, String album, String mime, String kind) {
        int total = 0;
        String[] projection = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED};
        try (Cursor c = getContentResolver().query(
                collection,
                projection,
                MediaStore.MediaColumns.RELATIVE_PATH + "=?",
                new String[]{album},
                MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (c == null) return 0;
            while (c.moveToNext() && total < 100) {
                long id = c.getLong(0);
                String name = c.getString(1);
                Uri uri = ContentUris.withAppendedId(collection, id);
                Button show = new Button(this);
                show.setText(kind + ": " + name);
                show.setAllCaps(false);
                show.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, mime);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Não foi possível abrir: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                list.addView(show);
                total++;
            }
        } catch (Exception e) {
            list.addView(label("Não foi possível consultar " + kind + ": " + e.getMessage(), 13));
        }
        return total;
    }
}
