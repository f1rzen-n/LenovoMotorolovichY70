package com.moto.resolutionmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PowerActivity extends Activity {

    private Button btnBack;
    private TextView tvBatteryStatus;
    private Button btnToggleBypassCharging;
    private Button btnTogglePowerSaver;
    private Button btnOpenBatteryCare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power);

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
        tvBatteryStatus = findViewById(R.id.tv_battery_status);
        btnToggleBypassCharging = findViewById(R.id.btn_toggle_bypass_charging);
        btnTogglePowerSaver = findViewById(R.id.btn_toggle_power_saver);
        btnOpenBatteryCare = findViewById(R.id.btn_open_battery_care);

        if (btnToggleBypassCharging != null) {
            btnToggleBypassCharging.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final boolean cur = PowerHelper.isBypassChargingActive(PowerActivity.this);
                    final boolean target = !cur;

                    if (target && !ShellHelper.isShizukuPermissionGranted() && !ShellHelper.isRootAvailable()) {
                        Toast.makeText(PowerActivity.this, "Для сквозной зарядки (Bypass) требуется Shizuku или Root", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnToggleBypassCharging.setEnabled(false);
                    btnToggleBypassCharging.setText("⏳ Переключение режима...");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PowerHelper.setBypassCharging(PowerActivity.this, target);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnToggleBypassCharging.setEnabled(true);
                                    updateUI();
                                    if (target) {
                                        new AlertDialog.Builder(PowerActivity.this)
                                            .setTitle("Сквозное питание активно! ⚡")
                                            .setMessage("Смартфон переведен на прямое питание от зарядного устройства.\n\n" +
                                                "• Аккумулятор не заряжается и не нагревается\n" +
                                                "• Энергия поступает напрямую в систему\n" +
                                                "• Идеально для тяжелых игр и длительных сессий\n\n" +
                                                "Для возврата к обычному режиму нажмите «Отключить Bypass».")
                                            .setPositiveButton("Отлично", null)
                                            .show();
                                    } else {
                                        Toast.makeText(PowerActivity.this, "Сквозное питание отключено. Стандартная зарядка возобновлена.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnTogglePowerSaver != null) {
            btnTogglePowerSaver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PowerHelper.togglePowerSaveMode(PowerActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean cur = PowerHelper.isPowerSaveMode(PowerActivity.this);
                                    Toast.makeText(PowerActivity.this, "Энергосбережение: " + (cur ? "Включено 🔋" : "Выключено"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnOpenBatteryCare != null) {
            btnOpenBatteryCare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PowerHelper.launchBatteryCare(PowerActivity.this);
                }
            });
        }
    }

    private void updateUI() {
        PowerHelper.BatteryInfo info = PowerHelper.getBatteryInfo(this);
        if (tvBatteryStatus != null) {
            String text = String.format("🔋 Батарея: %d%% • %.1f°C • %s", info.level, info.temperature, info.statusText);
            tvBatteryStatus.setText(text);
            if (info.isBypassActive) {
                tvBatteryStatus.setTextColor(0xFF8B5CF6);
            } else if (info.isCharging) {
                tvBatteryStatus.setTextColor(0xFF10B981);
            } else {
                tvBatteryStatus.setTextColor(0xFFF3F4F6);
            }
        }

        if (btnToggleBypassCharging != null) {
            if (info.isBypassActive) {
                btnToggleBypassCharging.setText("⚡ Сквозное питание (Bypass): ВКЛЮЧЕНО ✓");
                btnToggleBypassCharging.setBackgroundResource(R.drawable.btn_purple);
            } else {
                btnToggleBypassCharging.setText("⚡ Включить сквозное питание (Bypass)");
                btnToggleBypassCharging.setBackgroundResource(R.drawable.btn_primary);
            }
        }

        boolean powerSave = PowerHelper.isPowerSaveMode(this);
        if (btnTogglePowerSaver != null) {
            if (powerSave) {
                btnTogglePowerSaver.setText("🔋 Энергосбережение: Вкл ✓");
                btnTogglePowerSaver.setTextColor(0xFF10B981);
            } else {
                btnTogglePowerSaver.setText("🔋 Энергосбережение: Выкл");
                btnTogglePowerSaver.setTextColor(0xFFF3F4F6);
            }
        }
    }
}
