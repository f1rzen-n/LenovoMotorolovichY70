package com.moto.resolutionmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class NetworkSpeedActivity extends Activity {

    private static final int REQ_POST_NOTIFICATIONS = 1002;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Button btnBack;
    private Button btnToggleNetSpeed;
    private Button btnNetSpeedHideIdle;
    private Button btnNetSpeedAutostart;
    private Button btnNetSpeedIconMode;
    private LinearLayout layoutNetSpeedOffset;
    private TextView tvNetSpeedOffsetVal;
    private SeekBar sbNetSpeedOffset;
    private TextView tvNetSpeedOffsetYVal;
    private SeekBar sbNetSpeedOffsetY;
    private Button btnNetSpeedUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_speed);

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
        btnToggleNetSpeed = findViewById(R.id.btn_toggle_net_speed);
        btnNetSpeedHideIdle = findViewById(R.id.btn_net_speed_hide_idle);
        btnNetSpeedAutostart = findViewById(R.id.btn_net_speed_autostart);
        btnNetSpeedIconMode = findViewById(R.id.btn_net_speed_icon_mode);
        layoutNetSpeedOffset = findViewById(R.id.layout_net_speed_offset);
        tvNetSpeedOffsetVal = findViewById(R.id.tv_net_speed_offset_val);
        sbNetSpeedOffset = findViewById(R.id.sb_net_speed_offset);
        tvNetSpeedOffsetYVal = findViewById(R.id.tv_net_speed_offset_y_val);
        sbNetSpeedOffsetY = findViewById(R.id.sb_net_speed_offset_y);
        btnNetSpeedUnits = findViewById(R.id.btn_net_speed_units);

        if (btnToggleNetSpeed != null) {
            btnToggleNetSpeed.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean currentlyActive = NetworkSpeedHelper.isActiveNow(NetworkSpeedActivity.this);
                    applyNetSpeedToggle(!currentlyActive);
                }
            });
        }

        if (btnNetSpeedHideIdle != null) {
            btnNetSpeedHideIdle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean cur = NetworkSpeedHelper.isHideOnIdleEnabled(NetworkSpeedActivity.this);
                    NetworkSpeedHelper.setHideOnIdleEnabled(NetworkSpeedActivity.this, !cur);
                    updateUI();
                    Toast.makeText(NetworkSpeedActivity.this,
                        !cur ? "💤 Индикатор будет скрываться при 0 КБ/с" : "📶 Индикатор виден всегда (даже при 0 КБ/с)",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnNetSpeedAutostart != null) {
            btnNetSpeedAutostart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean cur = NetworkSpeedHelper.isAutoStartEnabled(NetworkSpeedActivity.this);
                    NetworkSpeedHelper.setAutoStartEnabled(NetworkSpeedActivity.this, !cur);
                    updateUI();
                    Toast.makeText(NetworkSpeedActivity.this,
                        !cur ? "🔄 Автозапуск после перезагрузки: Включён" : "🔄 Автозапуск после перезагрузки: Выключен",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnNetSpeedIconMode != null) {
            btnNetSpeedIconMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final int mode = NetworkSpeedHelper.cycleIconMode(NetworkSpeedActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    Toast.makeText(NetworkSpeedActivity.this,
                                        "👁️ Вид индикатора: " + NetworkSpeedHelper.getIconModeLabel(mode),
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (sbNetSpeedOffset != null) {
            sbNetSpeedOffset.setMax(205); // 15..220 dp
            sbNetSpeedOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int dp = 15 + progress;
                    if (tvNetSpeedOffsetVal != null) {
                        tvNetSpeedOffsetVal.setText(dp + " dp");
                    }
                    if (fromUser) {
                        NetworkSpeedHelper.setOverlayOffsetDp(NetworkSpeedActivity.this, dp);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (sbNetSpeedOffsetY != null) {
            sbNetSpeedOffsetY.setMax(20); // -10..+10 dp (progress 10 = 0 dp auto)
            sbNetSpeedOffsetY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int dp = progress - 10;
                    if (tvNetSpeedOffsetYVal != null) {
                        if (dp == 0) {
                            tvNetSpeedOffsetYVal.setText("0 dp (Авто)");
                        } else if (dp > 0) {
                            tvNetSpeedOffsetYVal.setText("+" + dp + " dp (Ниже)");
                        } else {
                            tvNetSpeedOffsetYVal.setText(dp + " dp (Выше)");
                        }
                    }
                    if (fromUser) {
                        NetworkSpeedHelper.setOverlayOffsetYDp(NetworkSpeedActivity.this, dp);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (btnNetSpeedUnits != null) {
            btnNetSpeedUnits.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean curBits = NetworkSpeedHelper.isBitsMode(NetworkSpeedActivity.this);
                    NetworkSpeedHelper.setBitsMode(NetworkSpeedActivity.this, !curBits);
                    updateUI();
                    Toast.makeText(NetworkSpeedActivity.this,
                        !curBits ? "📊 Единицы: Кбит/с • Мбит/с (Биты)" : "📊 Единицы: КБ/с • МБ/с (Байты)",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void applyNetSpeedToggle(final boolean enable) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int result = NetworkSpeedHelper.setEnabled(NetworkSpeedActivity.this, enable);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == NetworkSpeedHelper.RESULT_NEED_NOTIF_PERM) {
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_POST_NOTIFICATIONS);
                            } else {
                                Toast.makeText(NetworkSpeedActivity.this, "Разрешите уведомления для отображения скорости в статус-баре", Toast.LENGTH_LONG).show();
                            }
                        } else if (result == NetworkSpeedHelper.RESULT_NEED_OVERLAY_PERM) {
                            new AlertDialog.Builder(NetworkSpeedActivity.this)
                                .setTitle("Разрешение «Поверх других окон»")
                                .setMessage("Чтобы закрепить скорость интернета справа у батареи (чтобы она не смещалась от уведомлений), разрешите отображение поверх других окон (или подключите Shizuku для автоматической выдачи).")
                                .setPositiveButton("Открыть настройки", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                            startActivity(intent);
                                        } catch (Throwable ignored) {}
                                    }
                                })
                                .setNegativeButton("Отмена", null)
                                .show();
                        } else if (result == NetworkSpeedHelper.RESULT_OK_NATIVE_ROOT) {
                            Toast.makeText(NetworkSpeedActivity.this, "📶 Нативный индикатор скорости Motorola активирован (Root)!", Toast.LENGTH_SHORT).show();
                        } else if (result == NetworkSpeedHelper.RESULT_OK_SERVICE) {
                            Toast.makeText(NetworkSpeedActivity.this, "📶 Скорость интернета в статус-баре включена!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NetworkSpeedActivity.this, "Индикатор скорости отключён", Toast.LENGTH_SHORT).show();
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        }, 150);
                    }
                });
            }
        }).start();
    }

    private void updateUI() {
        boolean active = NetworkSpeedHelper.isActiveNow(this);
        boolean nativeRoot = NetworkSpeedHelper.isNativeRootActive(this);

        if (btnToggleNetSpeed != null) {
            if (active) {
                btnToggleNetSpeed.setText(nativeRoot
                    ? "📶 Скорость в статус-баре (Native Root): ВКЛ ✓"
                    : "📶 Скорость в статус-баре: ВКЛЮЧЕНА ✓");
                btnToggleNetSpeed.setBackgroundResource(R.drawable.btn_green);
            } else {
                btnToggleNetSpeed.setText("📶 Показать скорость в статус-баре");
                btnToggleNetSpeed.setBackgroundResource(R.drawable.btn_primary);
            }
        }

        boolean hideIdle = NetworkSpeedHelper.isHideOnIdleEnabled(this);
        if (btnNetSpeedHideIdle != null) {
            if (hideIdle) {
                btnNetSpeedHideIdle.setText("💤 Скрывать при 0: Вкл ✓");
                btnNetSpeedHideIdle.setTextColor(0xFF10B981);
            } else {
                btnNetSpeedHideIdle.setText("💤 Скрывать при 0: Выкл");
                btnNetSpeedHideIdle.setTextColor(0xFFF3F4F6);
            }
        }

        boolean autoStart = NetworkSpeedHelper.isAutoStartEnabled(this);
        if (btnNetSpeedAutostart != null) {
            if (autoStart) {
                btnNetSpeedAutostart.setText("🔄 Автозапуск: Вкл ✓");
                btnNetSpeedAutostart.setTextColor(0xFF10B981);
            } else {
                btnNetSpeedAutostart.setText("🔄 Автозапуск: Выкл");
                btnNetSpeedAutostart.setTextColor(0xFFF3F4F6);
            }
        }

        int iconMode = NetworkSpeedHelper.getIconMode(this);
        if (btnNetSpeedIconMode != null) {
            btnNetSpeedIconMode.setText("👁️ Вид: " + NetworkSpeedHelper.getIconModeLabel(iconMode));
        }

        if (layoutNetSpeedOffset != null) {
            layoutNetSpeedOffset.setVisibility(iconMode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY ? View.VISIBLE : View.GONE);
        }
        int offsetDp = NetworkSpeedHelper.getOverlayOffsetDp(this);
        if (tvNetSpeedOffsetVal != null) {
            tvNetSpeedOffsetVal.setText(offsetDp + " dp");
        }
        if (sbNetSpeedOffset != null) {
            sbNetSpeedOffset.setProgress(Math.max(0, offsetDp - 15));
        }

        int offsetYDp = NetworkSpeedHelper.getOverlayOffsetYDp(this);
        if (tvNetSpeedOffsetYVal != null) {
            if (offsetYDp == 0) {
                tvNetSpeedOffsetYVal.setText("0 dp (Авто)");
            } else if (offsetYDp > 0) {
                tvNetSpeedOffsetYVal.setText("+" + offsetYDp + " dp (Ниже)");
            } else {
                tvNetSpeedOffsetYVal.setText(offsetYDp + " dp (Выше)");
            }
        }
        if (sbNetSpeedOffsetY != null) {
            sbNetSpeedOffsetY.setProgress(Math.max(0, Math.min(20, offsetYDp + 10)));
        }

        boolean bitsMode = NetworkSpeedHelper.isBitsMode(this);
        if (btnNetSpeedUnits != null) {
            btnNetSpeedUnits.setText(bitsMode
                ? "📊 Единицы измерения: Кбит/с • Мбит/с (Биты)"
                : "📊 Единицы измерения: КБ/с • МБ/с (Байты)");
        }
    }
}
