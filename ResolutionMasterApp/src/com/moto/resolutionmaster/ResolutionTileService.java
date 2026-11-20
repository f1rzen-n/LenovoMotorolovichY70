package com.moto.resolutionmaster;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ResolutionTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();

        // 1. Gently collapse status bar to prevent SystemUI freeze / sticking in Modern QS style
        try {
            ShellHelper.runCommand(this, "cmd statusbar collapse");
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Throwable ignored) {}

        Point p = DisplayManagerHelper.getCurrentResolution(this);
        int minRes = Math.min(p.x, p.y);

        android.content.SharedPreferences prefs = getSharedPreferences("resolution_master_prefs", MODE_PRIVATE);
        int fhdDpi = prefs.getInt("selected_fhd_dpi", DisplayManagerHelper.FHD_DENSITY);
        int hdDpi = prefs.getInt("selected_hd_dpi", DisplayManagerHelper.HD_DENSITY);
        int qhdDpi = prefs.getInt("selected_qhd_dpi", DisplayManagerHelper.NATIVE_DENSITY);

        if (minRes > 1080) {
            // Currently QHD+ (1440p) -> switch to FHD+ (1080p)
            DisplayManagerHelper.setResolution(this,
                DisplayManagerHelper.FHD_WIDTH,
                DisplayManagerHelper.FHD_HEIGHT,
                fhdDpi);
        } else if (minRes > 720) {
            // Currently FHD+ (1080p) -> switch to HD+ (720p)
            DisplayManagerHelper.setResolution(this,
                DisplayManagerHelper.HD_WIDTH,
                DisplayManagerHelper.HD_HEIGHT,
                hdDpi);
        } else {
            // Currently HD+ (720p) -> switch to Native QHD+ (1440p)
            if (qhdDpi == DisplayManagerHelper.NATIVE_DENSITY) {
                DisplayManagerHelper.resetResolution(this);
            } else {
                DisplayManagerHelper.setResolution(this,
                    DisplayManagerHelper.QHD_WIDTH,
                    DisplayManagerHelper.QHD_HEIGHT,
                    qhdDpi);
            }
        }

        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        Point p = DisplayManagerHelper.getCurrentResolution(this);
        int minRes = Math.min(p.x, p.y);

        String modeText;
        String label;
        String subtitle;

        if (minRes > 1080) {
            modeText = "QHD";
            label = "QHD+";
            subtitle = "1440p • 144Hz";
        } else if (minRes > 720) {
            modeText = "FHD";
            label = "FHD+";
            subtitle = "1080p • Баланс";
        } else {
            modeText = "HD";
            label = "HD+";
            subtitle = "720p • Энерго";
        }

        tile.setState(Tile.STATE_ACTIVE);
        tile.setLabel(label);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(subtitle);
        }

        try {
            tile.setIcon(createResolutionIcon(modeText));
        } catch (Throwable ignored) {}

        tile.updateTile();
    }

    private Icon createResolutionIcon(String text) {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        float textSize;
        if (text.length() >= 3) {
            textSize = 38f;
        } else {
            textSize = 48f;
        }
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float y = (size / 2f) - bounds.exactCenterY();
        canvas.drawText(text, size / 2f, y, paint);

        return Icon.createWithBitmap(bitmap);
    }
}
