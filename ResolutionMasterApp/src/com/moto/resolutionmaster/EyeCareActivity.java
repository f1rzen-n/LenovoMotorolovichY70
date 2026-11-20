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

public class EyeCareActivity extends Activity {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Button btnBack;
    private Button btnToggleDcDimming;
    private Button btnToggleNightLight;
    private Button btnToggleExtraDim;

    // Anti-Black Smearing Submenu Controls
    private Button btnToggleSmearingMenu;
    private LinearLayout layoutSmearingSubmenu;
    private TextView tvBlackFloorVal;
    private SeekBar sbBlackFloorLevel;
    private Button btnBfLow;
    private Button btnBfMed;
    private Button btnBfHigh;
    private Button btnBlackFloorToggle;
    private Button btnSmearingCombo;
    private TextView tvExtraDimLevelVal;
    private SeekBar sbExtraDimLevel;
    private Button btnBright25;
    private Button btnBright30;
    private Button btnBright35;
    private Button btnSmearing120Hz;
    private Button btnSmearing144Hz;
    private Button btnSmearingDc;

    private boolean isSmearingMenuExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye_care);

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
        btnToggleDcDimming = findViewById(R.id.btn_toggle_dc_dimming);
        btnToggleNightLight = findViewById(R.id.btn_toggle_night_light);
        btnToggleExtraDim = findViewById(R.id.btn_toggle_extra_dim);

        if (btnToggleDcDimming != null) {
            btnToggleDcDimming.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            EyeCareHelper.toggleDcDimming(EyeCareActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean active = EyeCareHelper.isDcDimmingActive(EyeCareActivity.this);
                                    Toast.makeText(EyeCareActivity.this, "DC Dimming (Анти-ШИМ): " + (active ? "Включен 👁️" : "Выключен"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnToggleNightLight != null) {
            btnToggleNightLight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            EyeCareHelper.toggleNightLight(EyeCareActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean active = EyeCareHelper.isNightLightActive(EyeCareActivity.this);
                                    Toast.makeText(EyeCareActivity.this, "Ночной режим: " + (active ? "Включен 🌙" : "Выключен"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnToggleExtraDim != null) {
            btnToggleExtraDim.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            EyeCareHelper.toggleExtraDim(EyeCareActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean active = EyeCareHelper.isExtraDimActive(EyeCareActivity.this);
                                    Toast.makeText(EyeCareActivity.this, "Экстра-затемнение: " + (active ? "Включено 🌑" : "Выключено"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        // Anti-Black Smearing Submenu Init
        btnToggleSmearingMenu = findViewById(R.id.btn_toggle_smearing_menu);
        layoutSmearingSubmenu = findViewById(R.id.layout_smearing_submenu);
        tvBlackFloorVal = findViewById(R.id.tv_black_floor_val);
        sbBlackFloorLevel = findViewById(R.id.sb_black_floor_level);
        btnBfLow = findViewById(R.id.btn_bf_low);
        btnBfMed = findViewById(R.id.btn_bf_med);
        btnBfHigh = findViewById(R.id.btn_bf_high);
        btnBlackFloorToggle = findViewById(R.id.btn_black_floor_toggle);
        btnSmearingCombo = findViewById(R.id.btn_smearing_combo);
        tvExtraDimLevelVal = findViewById(R.id.tv_extra_dim_level_val);
        sbExtraDimLevel = findViewById(R.id.sb_extra_dim_level);
        btnBright25 = findViewById(R.id.btn_bright_25);
        btnBright30 = findViewById(R.id.btn_bright_30);
        btnBright35 = findViewById(R.id.btn_bright_35);
        btnSmearing120Hz = findViewById(R.id.btn_smearing_120hz);
        btnSmearing144Hz = findViewById(R.id.btn_smearing_144hz);
        btnSmearingDc = findViewById(R.id.btn_smearing_dc);

        if (btnToggleSmearingMenu != null && layoutSmearingSubmenu != null) {
            btnToggleSmearingMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isSmearingMenuExpanded = !isSmearingMenuExpanded;
                    layoutSmearingSubmenu.setVisibility(isSmearingMenuExpanded ? View.VISIBLE : View.GONE);
                    updateUI();
                }
            });
        }

        if (sbBlackFloorLevel != null) {
            sbBlackFloorLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvBlackFloorVal != null) {
                        tvBlackFloorVal.setText(EyeCareHelper.formatBlackFloorLabel(progress > 0, progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int lvl = seekBar.getProgress();
                    if (lvl <= 0) {
                        applyBlackFloorAction(false, 0);
                    } else {
                        applyBlackFloorAction(true, lvl);
                    }
                }
            });
        }

        if (btnBfLow != null) {
            btnBfLow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBlackFloorAction(true, 3);
                }
            });
        }

        if (btnBfMed != null) {
            btnBfMed.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBlackFloorAction(true, 4);
                }
            });
        }

        if (btnBfHigh != null) {
            btnBfHigh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBlackFloorAction(true, 6);
                }
            });
        }

        if (btnBlackFloorToggle != null) {
            btnBlackFloorToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean active = EyeCareHelper.isBlackFloorActive(EyeCareActivity.this);
                    int lvl = EyeCareHelper.getBlackFloorLevel(EyeCareActivity.this);
                    if (sbBlackFloorLevel != null && sbBlackFloorLevel.getProgress() > 0) {
                        lvl = sbBlackFloorLevel.getProgress();
                    }
                    applyBlackFloorAction(!active, lvl);
                }
            });
        }

        if (btnSmearingCombo != null) {
            btnSmearingCombo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean enabled = EyeCareHelper.toggleBrightExtraDimCombo(EyeCareActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    Toast.makeText(EyeCareActivity.this,
                                        enabled ? "💡 Связка активна: яркость матрицы 30% + Extra Dim" : "🔄 Связка отключена, яркость восстановлена",
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (sbExtraDimLevel != null) {
            sbExtraDimLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvExtraDimLevelVal != null) {
                        tvExtraDimLevelVal.setText(progress + "%");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    final int level = seekBar.getProgress();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            EyeCareHelper.setExtraDimLevel(EyeCareActivity.this, level);
                            if (!EyeCareHelper.isExtraDimActive(EyeCareActivity.this)) {
                                EyeCareHelper.setExtraDimActive(EyeCareActivity.this, true);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnBright25 != null) {
            btnBright25.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyHardwareBrightness(25);
                }
            });
        }

        if (btnBright30 != null) {
            btnBright30.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyHardwareBrightness(30);
                }
            });
        }

        if (btnBright35 != null) {
            btnBright35.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyHardwareBrightness(35);
                }
            });
        }

        if (btnSmearing120Hz != null) {
            btnSmearing120Hz.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DisplayManagerHelper.setRefreshRate(EyeCareActivity.this, 120.0f);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    Toast.makeText(EyeCareActivity.this, "Частота зафиксирована: 120 Гц", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnSmearing144Hz != null) {
            btnSmearing144Hz.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DisplayManagerHelper.setRefreshRate(EyeCareActivity.this, 144.0f);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    Toast.makeText(EyeCareActivity.this, "Частота зафиксирована: 144 Гц", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnSmearingDc != null) {
            btnSmearingDc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            EyeCareHelper.toggleDcDimming(EyeCareActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean active = EyeCareHelper.isDcDimmingActive(EyeCareActivity.this);
                                    Toast.makeText(EyeCareActivity.this, "DC Dimming: " + (active ? "Вкл 👁️" : "Выкл"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }
    }

    private void applyHardwareBrightness(final int percent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EyeCareHelper.setHardwareBrightnessPercent(EyeCareActivity.this, percent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(EyeCareActivity.this, "Аппаратная яркость матрицы: " + percent + "%", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void applyBlackFloorAction(final boolean enable, final int level) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int result = EyeCareHelper.applyBlackFloor(EyeCareActivity.this, enable, level);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == EyeCareHelper.RESULT_NEED_OVERLAY_PERM) {
                            new AlertDialog.Builder(EyeCareActivity.this)
                                .setTitle("Разрешение «Поверх других окон»")
                                .setMessage("Без Root-доступа подъём уровня чёрного OLED (#040404) работает через аппаратный слой SurfaceFlinger (HWC).\n\nРазрешите отображение поверх других окон (или подключите Shizuku для автоматической выдачи).")
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
                        } else if (result == EyeCareHelper.RESULT_OK_ROOT) {
                            Toast.makeText(EyeCareActivity.this, "✨ Подъём чёрного активен (SurfaceFlinger ColorMatrix Root): " + EyeCareHelper.formatBlackFloorLabel(true, level), Toast.LENGTH_SHORT).show();
                        } else if (result == EyeCareHelper.RESULT_OK_OVERLAY) {
                            Toast.makeText(EyeCareActivity.this, "✨ Подъём чёрного активен (HWC Layer): " + EyeCareHelper.formatBlackFloorLabel(true, level), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EyeCareActivity.this, "Подъём уровня чёрного отключён (#000000)", Toast.LENGTH_SHORT).show();
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
        boolean dcActive = EyeCareHelper.isDcDimmingActive(this);
        if (btnToggleDcDimming != null) {
            if (dcActive) {
                btnToggleDcDimming.setText("👁️ DC Dimming (Анти-ШИМ): Включен ✓");
                btnToggleDcDimming.setBackgroundResource(R.drawable.btn_green);
            } else {
                btnToggleDcDimming.setText("👁️ DC Dimming (Анти-ШИМ): Выключен");
                btnToggleDcDimming.setBackgroundResource(R.drawable.btn_primary);
            }
        }

        boolean nightActive = EyeCareHelper.isNightLightActive(this);
        if (btnToggleNightLight != null) {
            if (nightActive) {
                btnToggleNightLight.setText("🌙 Ночной режим: Вкл ✓");
                btnToggleNightLight.setTextColor(0xFF10B981);
            } else {
                btnToggleNightLight.setText("🌙 Ночной режим: Выкл");
                btnToggleNightLight.setTextColor(0xFFF3F4F6);
            }
        }

        boolean extraDimActive = EyeCareHelper.isExtraDimActive(this);
        if (btnToggleExtraDim != null) {
            if (extraDimActive) {
                btnToggleExtraDim.setText("🌑 Экстра-затемнение: Вкл ✓");
                btnToggleExtraDim.setTextColor(0xFF10B981);
            } else {
                btnToggleExtraDim.setText("🌑 Экстра-затемнение: Выкл");
                btnToggleExtraDim.setTextColor(0xFFF3F4F6);
            }
        }

        // Update Anti-Black Smearing Submenu state
        boolean bfActive = EyeCareHelper.isBlackFloorActive(this);
        int bfLevel = EyeCareHelper.getBlackFloorLevel(this);
        boolean comboActive = EyeCareHelper.isBrightExtraDimComboActive(this);

        if (btnToggleSmearingMenu != null) {
            String arrow = isSmearingMenuExpanded ? "▲" : "▼";
            String badge = (bfActive || comboActive) ? " • Активен ✓ " : "  ";
            btnToggleSmearingMenu.setText("🌑 Анти-шлейф чёрного (Anti-Black Smearing)" + badge + arrow);
            btnToggleSmearingMenu.setTextColor((bfActive || comboActive) ? 0xFF10B981 : 0xFF8B5CF6);
        }

        if (tvBlackFloorVal != null) {
            tvBlackFloorVal.setText(EyeCareHelper.formatBlackFloorLabel(bfActive, bfLevel));
            tvBlackFloorVal.setTextColor(bfActive ? 0xFF10B981 : 0xFF8B5CF6);
        }

        if (sbBlackFloorLevel != null) {
            sbBlackFloorLevel.setProgress(bfActive ? bfLevel : 0);
        }

        if (btnBlackFloorToggle != null) {
            String hex = String.format("#%02X%02X%02X", bfLevel, bfLevel, bfLevel);
            if (bfActive) {
                btnBlackFloorToggle.setText("✨ Подъём чёрного ВКЛ (" + hex + ") • Выключить");
                btnBlackFloorToggle.setBackgroundResource(R.drawable.btn_green);
            } else {
                btnBlackFloorToggle.setText("✨ Включить подъём чёрного (" + hex + ")");
                btnBlackFloorToggle.setBackgroundResource(R.drawable.btn_purple);
            }
        }

        if (btnSmearingCombo != null) {
            if (comboActive) {
                btnSmearingCombo.setText("🔄 Выключить связку (Вернуть обычную яркость)");
                btnSmearingCombo.setBackgroundResource(R.drawable.btn_green);
            } else {
                btnSmearingCombo.setText("💡 Включить связку (Яркость 30% + Extra Dim)");
                btnSmearingCombo.setBackgroundResource(R.drawable.btn_primary);
            }
        }

        int extraDimLevel = EyeCareHelper.getExtraDimLevel(this);
        if (tvExtraDimLevelVal != null) {
            tvExtraDimLevelVal.setText(extraDimLevel + "%");
        }
        if (sbExtraDimLevel != null) {
            sbExtraDimLevel.setProgress(extraDimLevel);
        }

        float cfgHz = DisplayManagerHelper.getConfiguredRefreshRate(this);
        if (btnSmearing120Hz != null) {
            boolean is120 = Math.round(cfgHz) == 120;
            btnSmearing120Hz.setText(is120 ? "120 Гц ✓" : "120 Гц");
            btnSmearing120Hz.setTextColor(is120 ? 0xFF10B981 : 0xFFF3F4F6);
        }
        if (btnSmearing144Hz != null) {
            boolean is144 = Math.round(cfgHz) == 144;
            btnSmearing144Hz.setText(is144 ? "144 Гц ✓" : "144 Гц");
            btnSmearing144Hz.setTextColor(is144 ? 0xFF10B981 : 0xFFF3F4F6);
        }
        if (btnSmearingDc != null) {
            btnSmearingDc.setText(dcActive ? "DC Dim: Вкл ✓" : "DC Dim: Выкл");
            btnSmearingDc.setTextColor(dcActive ? 0xFF10B981 : 0xFFF3F4F6);
        }
    }
}
