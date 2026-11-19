package com.moto.resolutionmaster;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class RefreshRateTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (!ShellHelper.hasWriteSecureSettings(this)) {
            ShellHelper.grantWriteSecureSettings(this);
        }

        float current = DisplayManagerHelper.getConfiguredRefreshRate(this);
        int curHz = Math.round(current);

        float next;
        if (curHz <= 0) {
            next = DisplayManagerHelper.HZ_60;
        } else if (curHz < 75) {
            next = DisplayManagerHelper.HZ_90;
        } else if (curHz < 105) {
            next = DisplayManagerHelper.HZ_120;
        } else if (curHz < 135) {
            next = DisplayManagerHelper.HZ_144;
        } else {
            next = DisplayManagerHelper.HZ_AUTO;
        }

        DisplayManagerHelper.setRefreshRate(this, next);
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        float current = DisplayManagerHelper.getConfiguredRefreshRate(this);
        int curHz = Math.round(current);

        String numberStr;
        String subtitle;
        int state;

        if (curHz >= 135) {
            numberStr = "144";
            subtitle = "144 Гц";
            state = Tile.STATE_ACTIVE;
        } else if (curHz >= 105) {
            numberStr = "120";
            subtitle = "120 Гц";
            state = Tile.STATE_ACTIVE;
        } else if (curHz >= 75) {
            numberStr = "90";
            subtitle = "90 Гц";
            state = Tile.STATE_ACTIVE;
        } else if (curHz >= 45) {
            numberStr = "60";
            subtitle = "60 Гц";
            state = Tile.STATE_ACTIVE;
        } else {
            numberStr = "Авто";
            subtitle = "Динамич.";
            state = Tile.STATE_INACTIVE;
        }

        tile.setState(state);
        tile.setLabel(numberStr);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(subtitle);
        }

        try {
            String iconText = "Авто".equals(numberStr) ? "A" : numberStr;
            tile.setIcon(createFpsIcon(iconText));
        } catch (Throwable ignored) {}

        tile.updateTile();
    }

    private Icon createFpsIcon(String text) {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        float textSize;
        if (text.length() >= 3) {
            textSize = 42f;
        } else if (text.length() == 2) {
            textSize = 50f;
        } else {
            textSize = 52f;
        }
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float y = (size / 2f) - bounds.exactCenterY();
        canvas.drawText(text, size / 2f, y, paint);

        return Icon.createWithBitmap(bitmap);
    }
}
