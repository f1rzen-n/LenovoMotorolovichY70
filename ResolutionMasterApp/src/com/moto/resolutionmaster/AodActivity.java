package com.moto.resolutionmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AodActivity extends Activity {

    private Button btnBack;
    private Button btnToggleAodTimeout;
    private LinearLayout layoutAodSettings;
    private TextView tvAodTimeoutVal;
    private Button btnAod5s;
    private Button btnAod10s;
    private Button btnAod15s;
    private Button btnAod30s;
    private Button btnAod60s;
    private Button btnAodKeepCharging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aod);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void setupControls() {
        btnToggleAodTimeout = findViewById(R.id.btn_toggle_aod_timeout);
        layoutAodSettings = findViewById(R.id.layout_aod_settings);
        tvAodTimeoutVal = findViewById(R.id.tv_aod_timeout_val);
        btnAod5s = findViewById(R.id.btn_aod_5s);
        btnAod10s = findViewById(R.id.btn_aod_10s);
        btnAod15s = findViewById(R.id.btn_aod_15s);
        btnAod30s = findViewById(R.id.btn_aod_30s);
        btnAod60s = findViewById(R.id.btn_aod_60s);
        btnAodKeepCharging = findViewById(R.id.btn_aod_keep_charging);

        if (btnToggleAodTimeout != null) {
            btnToggleAodTimeout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!ShellHelper.hasWriteSecureSettings(AodActivity.this) && !ShellHelper.isRootAvailable()) {
                        showSecureSettingsDialog();
                        return;
                    }
                    boolean enabled = !AodTimeoutHelper.isEnabled(AodActivity.this);
                    AodTimeoutHelper.setEnabled(AodActivity.this, enabled);
                    if (enabled) {
                        AodTimeoutService.start(AodActivity.this);
                        Toast.makeText(AodActivity.this, "Умное затухание AOD включено", Toast.LENGTH_SHORT).show();
                    } else {
                        AodTimeoutService.stop(AodActivity.this);
                        AodTimeoutHelper.setDozeAlwaysOn(AodActivity.this, true);
                        Toast.makeText(AodActivity.this, "Умное затухание AOD отключено", Toast.LENGTH_SHORT).show();
                    }
                    updateUI();
                }
            });
        }

        View.OnClickListener durationListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int secs = 15;
                int id = v.getId();
                if (id == R.id.btn_aod_5s) secs = 5;
                else if (id == R.id.btn_aod_10s) secs = 10;
                else if (id == R.id.btn_aod_15s) secs = 15;
                else if (id == R.id.btn_aod_30s) secs = 30;
                else if (id == R.id.btn_aod_60s) secs = 60;

                AodTimeoutHelper.setTimeoutSeconds(AodActivity.this, secs);
                updateUI();
                Toast.makeText(AodActivity.this, "AOD гаснет через " + secs + " сек", Toast.LENGTH_SHORT).show();
            }
        };

        if (btnAod5s != null) btnAod5s.setOnClickListener(durationListener);
        if (btnAod10s != null) btnAod10s.setOnClickListener(durationListener);
        if (btnAod15s != null) btnAod15s.setOnClickListener(durationListener);
        if (btnAod30s != null) btnAod30s.setOnClickListener(durationListener);
        if (btnAod60s != null) btnAod60s.setOnClickListener(durationListener);

        if (btnAodKeepCharging != null) {
            btnAodKeepCharging.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean cur = AodTimeoutHelper.isKeepOnWhileCharging(AodActivity.this);
                    AodTimeoutHelper.setKeepOnWhileCharging(AodActivity.this, !cur);
                    updateUI();
                }
            });
        }

        updateUI();
    }

    private void updateUI() {
        boolean enabled = AodTimeoutHelper.isEnabled(this);
        if (btnToggleAodTimeout != null) {
            if (enabled) {
                btnToggleAodTimeout.setText("✓ Умное затухание включено");
                btnToggleAodTimeout.setBackgroundResource(R.drawable.btn_purple);
            } else {
                btnToggleAodTimeout.setText("Включить умное затухание AOD");
                btnToggleAodTimeout.setBackgroundResource(R.drawable.btn_primary);
            }
        }

        if (layoutAodSettings != null) {
            layoutAodSettings.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }

        int currentSecs = AodTimeoutHelper.getTimeoutSeconds(this);
        if (tvAodTimeoutVal != null) {
            if (currentSecs == 15) {
                tvAodTimeoutVal.setText("Время до затухания: 15 сек (Рекомендуемое)");
            } else {
                tvAodTimeoutVal.setText("Время до затухания: " + currentSecs + " сек");
            }
        }
        updateDurationButton(btnAod5s, currentSecs == 5);
        updateDurationButton(btnAod10s, currentSecs == 10);
        updateDurationButton(btnAod15s, currentSecs == 15);
        updateDurationButton(btnAod30s, currentSecs == 30);
        updateDurationButton(btnAod60s, currentSecs == 60);

        if (btnAodKeepCharging != null) {
            boolean keepCharging = AodTimeoutHelper.isKeepOnWhileCharging(this);
            if (keepCharging) {
                btnAodKeepCharging.setText("✓ Не гасить AOD при зарядке (Активно)");
                btnAodKeepCharging.setTextColor(0xFFC084FC);
            } else {
                btnAodKeepCharging.setText("Не гасить AOD при зарядке (Выкл)");
                btnAodKeepCharging.setTextColor(0xFF9E9E9E);
            }
        }
    }

    private void updateDurationButton(Button btn, boolean isSelected) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.btn_purple);
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundResource(R.drawable.btn_chip);
            btn.setTextColor(0xFFD1D5DB);
        }
    }

    private void showSecureSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Требуются права доступа")
            .setMessage("Для управления Always On Display приложению требуется разрешение WRITE_SECURE_SETTINGS.\n\nПодключите Shizuku на главном экране или предоставьте права через ADB.")
            .setPositiveButton("Понятно", null)
            .show();
    }
}
