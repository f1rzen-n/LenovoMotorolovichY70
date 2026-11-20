package com.moto.resolutionmaster;

import android.app.Activity;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class TouchActivity extends Activity {

    private Button btnBack;
    private TextView tvTouchSpeedVal;
    private SeekBar sbTouchSpeed;
    private Button btnTouchStock;
    private Button btnTouchGame;
    private Button btnTouchTurbo;
    private Button btnApplyTurbo;
    private Button btnResetStock;
    private TextView tvAnimSpeedVal;
    private Button btnAnimBoost;
    private Button btnAnimStock;

    private final ContentObserver mSettingsObserver =
        new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                TouchSpeedHelper.invalidateCache();
                updateUI();
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch);

        btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        setupControls();

        try {
            getContentResolver().registerContentObserver(
                Settings.System.getUriFor("pointer_speed"), false, mSettingsObserver);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        try {
            getContentResolver().unregisterContentObserver(mSettingsObserver);
        } catch (Throwable ignored) {}
        super.onDestroy();
    }

    private void setupControls() {
        tvTouchSpeedVal = findViewById(R.id.tv_menu_touch_speed_val);
        sbTouchSpeed = findViewById(R.id.sb_menu_touch_speed);
        btnTouchStock = findViewById(R.id.btn_menu_touch_stock);
        btnTouchGame = findViewById(R.id.btn_menu_touch_game);
        btnTouchTurbo = findViewById(R.id.btn_menu_touch_turbo);
        btnApplyTurbo = findViewById(R.id.btn_menu_apply_turbo);
        btnResetStock = findViewById(R.id.btn_menu_reset_stock);
        tvAnimSpeedVal = findViewById(R.id.tv_menu_anim_speed_val);
        btnAnimBoost = findViewById(R.id.btn_menu_anim_boost);
        btnAnimStock = findViewById(R.id.btn_menu_anim_stock);

        if (sbTouchSpeed != null) {
            sbTouchSpeed.setMax(14);
            int curSpeed = TouchSpeedHelper.getPointerSpeed(this);
            sbTouchSpeed.setProgress(TouchSpeedHelper.speedToProgress(curSpeed));
            updateTouchSpeedDisplay(curSpeed);

            sbTouchSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        int speed = TouchSpeedHelper.progressToSpeed(progress);
                        updateTouchSpeedDisplay(speed);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int speed = TouchSpeedHelper.progressToSpeed(seekBar.getProgress());
                    applyTouchSpeed(speed, false);
                }
            });
        }

        if (btnTouchStock != null) {
            btnTouchStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyTouchSpeed(0, true);
                }
            });
        }

        if (btnTouchGame != null) {
            btnTouchGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyTouchSpeed(5, true);
                }
            });
        }

        if (btnTouchTurbo != null) {
            btnTouchTurbo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyTouchSpeed(7, true);
                }
            });
        }

        if (btnApplyTurbo != null) {
            btnApplyTurbo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean ok = TouchSpeedHelper.enableFullTurboMode(TouchActivity.this);
                    if (sbTouchSpeed != null) {
                        sbTouchSpeed.setProgress(TouchSpeedHelper.speedToProgress(7));
                    }
                    updateUI();
                    if (ok) {
                        Toast.makeText(TouchActivity.this, "⚡ Турбо-режим: 144 Гц + Макс. отклик + UI 0.5x!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TouchActivity.this, "Ошибка применения Турбо-режима (проверьте Shizuku/Root)", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnResetStock != null) {
            btnResetStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean ok = TouchSpeedHelper.resetToStock(TouchActivity.this);
                    if (sbTouchSpeed != null) {
                        sbTouchSpeed.setProgress(TouchSpeedHelper.speedToProgress(0));
                    }
                    updateUI();
                    if (ok) {
                        Toast.makeText(TouchActivity.this, "🔄 Настройки сброшены к стандартным заводским!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TouchActivity.this, "Ошибка сброса настроек", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnAnimBoost != null) {
            btnAnimBoost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyAnimScale(0.5f);
                }
            });
        }

        if (btnAnimStock != null) {
            btnAnimStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyAnimScale(1.0f);
                }
            });
        }

        updateUI();
    }

    private void updateUI() {
        int curSpeed = TouchSpeedHelper.getPointerSpeed(this);
        updateTouchSpeedDisplay(curSpeed);
        if (sbTouchSpeed != null) {
            sbTouchSpeed.setProgress(TouchSpeedHelper.speedToProgress(curSpeed));
        }
        updateAnimSpeedDisplay();
    }

    private void updateTouchSpeedDisplay(int speed) {
        String label = TouchSpeedHelper.getSpeedLabel(speed);
        if (tvTouchSpeedVal != null) {
            tvTouchSpeedVal.setText(label);
        }
    }

    private void applyTouchSpeed(int speed, boolean updateSeekBar) {
        boolean ok = TouchSpeedHelper.setPointerSpeed(this, speed);
        if (updateSeekBar && sbTouchSpeed != null) {
            sbTouchSpeed.setProgress(TouchSpeedHelper.speedToProgress(speed));
        }
        updateTouchSpeedDisplay(speed);
        if (ok) {
            Toast.makeText(this, "Чувствительность тача: " + TouchSpeedHelper.getSpeedLabel(speed), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Не удалось изменить чувствительность (проверьте Shizuku/Root)", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAnimSpeedDisplay() {
        float scale = TouchSpeedHelper.getAnimationScale(this);
        if (tvAnimSpeedVal != null) {
            tvAnimSpeedVal.setText(TouchSpeedHelper.getAnimationScaleLabel(scale));
        }
    }

    private void applyAnimScale(float scale) {
        boolean ok = TouchSpeedHelper.setAnimationScales(this, scale);
        updateAnimSpeedDisplay();
        if (ok) {
            Toast.makeText(this, "Масштаб анимаций: " + TouchSpeedHelper.getAnimationScaleLabel(scale), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Не удалось изменить масштаб (проверьте Shizuku/Root)", Toast.LENGTH_SHORT).show();
        }
    }
}
