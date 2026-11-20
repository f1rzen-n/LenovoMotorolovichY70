package com.moto.resolutionmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainMenuActivity extends Activity {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Top Header & Privilege Button
    private Button btnHeaderPrivilege;

    // HUD Views
    private TextView tvMenuRes;
    private TextView tvMenuDpi;
    private TextView tvMenuFps;
    private TextView tvMenuShizuku;
    private TextView tvMenuSecure;
    private TextView tvMenuTouchStatus;

    // HUD Row 2
    private TextView tvHudOled;
    private TextView tvHudAod;
    private TextView tvHudPower;
    private TextView tvHudNfc;

    // 9 Category Cards
    private LinearLayout cardMenuDisplay;
    private LinearLayout cardMenuEyecare;
    private LinearLayout cardMenuAod;
    private LinearLayout cardMenuTouch;
    private LinearLayout cardMenuPower;
    private LinearLayout cardMenuNet;
    private LinearLayout cardMenuAudio;
    private LinearLayout cardMenuDebloat;
    private LinearLayout cardMenuNfc;

    // 9 Subtitle Status TextViews
    private TextView tvSubDisplay;
    private TextView tvSubEyecare;
    private TextView tvSubAod;
    private TextView tvSubTouch;
    private TextView tvSubPower;
    private TextView tvSubNet;
    private TextView tvSubAudio;
    private TextView tvSubDebloat;
    private TextView tvSubNfc;

    private final ContentObserver mSettingsObserver =
        new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                TouchSpeedHelper.invalidateCache();
                updateStatus();
            }
        };

    private final Shizuku.OnBinderReceivedListener mBinderReceivedListener =
        new Shizuku.OnBinderReceivedListener() {
            @Override
            public void onBinderReceived() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus();
                    }
                });
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        initViews();
        setupClickListeners();

        try {
            Shizuku.addBinderReceivedListenerSticky(mBinderReceivedListener);
        } catch (Throwable ignored) {}

        try {
            getContentResolver().registerContentObserver(
                Settings.Global.getUriFor("oneplus_screen_refresh_rate"), false, mSettingsObserver);
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor("user_refresh_rate"), false, mSettingsObserver);
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor("doze_always_on"), false, mSettingsObserver);
            getContentResolver().registerContentObserver(
                Settings.System.getUriFor("pointer_speed"), false, mSettingsObserver);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    protected void onDestroy() {
        try {
            getContentResolver().unregisterContentObserver(mSettingsObserver);
        } catch (Throwable ignored) {}
        try {
            Shizuku.removeBinderReceivedListener(mBinderReceivedListener);
        } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateStatus();
    }

    private void initViews() {
        btnHeaderPrivilege = findViewById(R.id.btn_header_privilege);

        // HUD
        tvMenuRes = findViewById(R.id.tv_menu_res);
        tvMenuDpi = findViewById(R.id.tv_menu_dpi);
        tvMenuFps = findViewById(R.id.tv_menu_fps);
        tvMenuShizuku = findViewById(R.id.tv_menu_shizuku);
        tvMenuSecure = findViewById(R.id.tv_menu_secure);
        tvMenuTouchStatus = findViewById(R.id.tv_menu_touch_status);

        tvHudOled = findViewById(R.id.tv_hud_oled);
        tvHudAod = findViewById(R.id.tv_hud_aod);
        tvHudPower = findViewById(R.id.tv_hud_power);
        tvHudNfc = findViewById(R.id.tv_hud_nfc);

        // Category Cards
        cardMenuDisplay = findViewById(R.id.card_menu_display);
        cardMenuEyecare = findViewById(R.id.card_menu_eyecare);
        cardMenuAod = findViewById(R.id.card_menu_aod);
        cardMenuTouch = findViewById(R.id.card_menu_touch);
        cardMenuPower = findViewById(R.id.card_menu_power);
        cardMenuNet = findViewById(R.id.card_menu_net);
        cardMenuAudio = findViewById(R.id.card_menu_audio);
        cardMenuDebloat = findViewById(R.id.card_menu_debloat);
        cardMenuNfc = findViewById(R.id.card_menu_nfc);

        // Subtitles
        tvSubDisplay = findViewById(R.id.tv_sub_display);
        tvSubEyecare = findViewById(R.id.tv_sub_eyecare);
        tvSubAod = findViewById(R.id.tv_sub_aod);
        tvSubTouch = findViewById(R.id.tv_sub_touch);
        tvSubPower = findViewById(R.id.tv_sub_power);
        tvSubNet = findViewById(R.id.tv_sub_net);
        tvSubAudio = findViewById(R.id.tv_sub_audio);
        tvSubDebloat = findViewById(R.id.tv_sub_debloat);
        tvSubNfc = findViewById(R.id.tv_sub_nfc);
    }

    private void setupClickListeners() {
        // 1. Display
        if (cardMenuDisplay != null) {
            cardMenuDisplay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
                }
            });
        }

        // 2. Eye Care
        if (cardMenuEyecare != null) {
            cardMenuEyecare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, EyeCareActivity.class));
                }
            });
        }

        // 3. AOD
        if (cardMenuAod != null) {
            cardMenuAod.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, AodActivity.class));
                }
            });
        }

        // 4. Touch
        if (cardMenuTouch != null) {
            cardMenuTouch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, TouchActivity.class));
                }
            });
        }

        // 5. Power
        if (cardMenuPower != null) {
            cardMenuPower.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, PowerActivity.class));
                }
            });
        }

        // 6. Network Speed
        if (cardMenuNet != null) {
            cardMenuNet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, NetworkSpeedActivity.class));
                }
            });
        }

        // 7. Audio
        if (cardMenuAudio != null) {
            cardMenuAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, AudioActivity.class));
                }
            });
        }

        // 8. Debloater
        if (cardMenuDebloat != null) {
            cardMenuDebloat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, DebloatActivity.class));
                }
            });
        }

        // 9. NFC Fix
        if (cardMenuNfc != null) {
            cardMenuNfc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainMenuActivity.this, NfcFixActivity.class));
                }
            });
        }

        // Top Privilege Button Click
        if (btnHeaderPrivilege != null) {
            btnHeaderPrivilege.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPrivilegeActionDialog();
                }
            });
        }

        // Footer Author Click
        View layoutFooterAuthor = findViewById(R.id.layout_footer_author);
        TextView tvAppAuthor = findViewById(R.id.tv_app_author);
        View.OnClickListener authorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShellHelper.openUrl(MainMenuActivity.this, "https://4pda.to/forum/index.php?showtopic=1121132&view=findpost&p=145909028");
            }
        };
        if (tvAppAuthor != null) tvAppAuthor.setOnClickListener(authorClickListener);
        if (layoutFooterAuthor != null) layoutFooterAuthor.setOnClickListener(authorClickListener);
    }

    private void updateStatus() {
        // 1. Resolution & DPI
        Point realSize = DisplayManagerHelper.getCurrentResolution(this);
        int currentDpi = DisplayManagerHelper.getCurrentDensity(this);
        int physicalDpi = DisplayManagerHelper.NATIVE_DENSITY;

        if (tvMenuRes != null) {
            tvMenuRes.setText(realSize.x + " × " + realSize.y);
        }
        if (tvMenuDpi != null) {
            String dpiText = currentDpi + " DPI";
            if (currentDpi == physicalDpi) {
                dpiText += " (Сток)";
            }
            tvMenuDpi.setText(dpiText);
        }

        // 2. Refresh Rate
        float currentFps = DisplayManagerHelper.getCurrentRefreshRate(this);
        float configuredFps = DisplayManagerHelper.getConfiguredRefreshRate(this);
        if (tvMenuFps != null) {
            String fpsStr = String.format("%.0f Гц", currentFps);
            if (configuredFps > 0) {
                fpsStr += String.format(" (Фикс.)", configuredFps);
            }
            tvMenuFps.setText(fpsStr);
        }

        // 3. Shizuku Status
        if (tvMenuShizuku != null) {
            if (ShellHelper.isShizukuPermissionGranted()) {
                tvMenuShizuku.setText("Shizuku: Подключен ✓");
                tvMenuShizuku.setTextColor(0xFF10B981);
            } else if (ShellHelper.isShizukuRunning()) {
                tvMenuShizuku.setText("Shizuku: Требует доступ");
                tvMenuShizuku.setTextColor(0xFFF59E0B);
            } else {
                tvMenuShizuku.setText("Shizuku: Не запущен");
                tvMenuShizuku.setTextColor(0xFF9E9E9E);
            }
        }

        // 4. Secure Settings / Tiles
        boolean hasSecure = ShellHelper.hasWriteSecureSettings(this);
        if (tvMenuSecure != null) {
            if (hasSecure) {
                tvMenuSecure.setText("Тайлы шторки: Активны ✓");
                tvMenuSecure.setTextColor(0xFF10B981);
            } else {
                tvMenuSecure.setText("Тайлы шторки: Требуют права");
                tvMenuSecure.setTextColor(0xFFF59E0B);
            }
        }

        // 5. Touch Speed
        int touchSpeed = TouchSpeedHelper.getPointerSpeed(this);
        String touchLabel = TouchSpeedHelper.getSpeedLabel(touchSpeed);
        if (tvMenuTouchStatus != null) {
            tvMenuTouchStatus.setText("Отклик тача: " + touchLabel);
        }

        // 6. HUD Row 2: OLED / AOD / Power / NFC
        boolean dcActive = EyeCareHelper.isDcDimmingActive(this);
        if (tvHudOled != null) {
            if (dcActive) {
                tvHudOled.setText("DC Dimming: Вкл ✓");
                tvHudOled.setTextColor(0xFF10B981);
            } else {
                tvHudOled.setText("DC Dimming: Выкл");
                tvHudOled.setTextColor(0xFF9E9E9E);
            }
        }

        if (tvHudAod != null) {
            boolean aodEnabled = AodTimeoutHelper.isEnabled(this);
            if (aodEnabled) {
                tvHudAod.setText("AOD: " + AodTimeoutHelper.getTimeoutSeconds(this) + "с ✓");
                tvHudAod.setTextColor(0xFFC084FC);
            } else {
                tvHudAod.setText("AOD: Постоянный");
                tvHudAod.setTextColor(0xFF9E9E9E);
            }
        }

        if (tvHudPower != null) {
            boolean bypass = PowerHelper.isBypassChargingActive(this);
            if (bypass) {
                tvHudPower.setText("Bypass: Вкл ✓");
                tvHudPower.setTextColor(0xFF10B981);
            } else {
                tvHudPower.setText("Bypass: Выкл");
                tvHudPower.setTextColor(0xFF9E9E9E);
            }
        }

        // 7. Update Top Privilege Button
        updatePrivilegeButton();

        // 8. Update Subtitles in Category List
        if (tvSubDisplay != null) {
            tvSubDisplay.setText(realSize.x + " × " + realSize.y + " • " + currentDpi + " DPI • " + Math.round(currentFps) + " Гц");
        }

        if (tvSubEyecare != null) {
            boolean night = EyeCareHelper.isNightLightActive(this);
            boolean bf = EyeCareHelper.isBlackFloorActive(this);
            String text = "DC Dimming: " + (dcActive ? "Вкл ✓" : "Выкл");
            if (night) text += " • Ночной: Вкл";
            if (bf) text += " • Анти-шлейф: Вкл";
            tvSubEyecare.setText(text);
            tvSubEyecare.setTextColor(dcActive ? 0xFF10B981 : 0xFF06B6D4);
        }

        if (tvSubAod != null) {
            boolean aod = AodTimeoutHelper.isEnabled(this);
            if (aod) {
                tvSubAod.setText("Умное затухание: " + AodTimeoutHelper.getTimeoutSeconds(this) + " сек ✓");
                tvSubAod.setTextColor(0xFFC084FC);
            } else {
                tvSubAod.setText("Режим: Заводской (Постоянный)");
                tvSubAod.setTextColor(0xFF9E9E9E);
            }
        }

        if (tvSubTouch != null) {
            tvSubTouch.setText("Отклик: " + touchLabel);
        }

        if (tvSubPower != null) {
            PowerHelper.BatteryInfo bInfo = PowerHelper.getBatteryInfo(this);
            String pText = bInfo.isBypassActive ? "Bypass: ВКЛЮЧЕН ⚡" : ("Батарея: " + bInfo.level + "% • " + String.format("%.1f°C", bInfo.temperature));
            tvSubPower.setText(pText);
            tvSubPower.setTextColor(bInfo.isBypassActive ? 0xFF8B5CF6 : 0xFF10B981);
        }

        if (tvSubNet != null) {
            boolean netActive = NetworkSpeedHelper.isActiveNow(this);
            int offset = NetworkSpeedHelper.getOverlayOffsetDp(this);
            tvSubNet.setText(netActive ? ("Оверлей активен ✓ • " + offset + " dp") : "Оверлей отключён");
            tvSubNet.setTextColor(netActive ? 0xFF10B981 : 0xFF9E9E9E);
        }

        if (tvSubAudio != null) {
            int vol = AudioHelper.getMediaVolumePercent(this);
            boolean safe = AudioHelper.isSafeVolumeBypassed(this);
            tvSubAudio.setText("Громкость: " + vol + "%" + (safe ? " • Обход лимита ✓" : ""));
        }

        // Check NFC Async
        new Thread(new Runnable() {
            @Override
            public void run() {
                final NfcFixHelper.RoutingInfo nfcInfo = NfcFixHelper.getLiveRoutingInfo(MainMenuActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tvHudNfc != null) {
                            if (nfcInfo.status == NfcFixHelper.NfcStatus.FIXED_100_PERCENT) {
                                tvHudNfc.setText("NFC: Исправлен ✓");
                                tvHudNfc.setTextColor(0xFF10B981);
                            } else {
                                tvHudNfc.setText("NFC: Сток");
                                tvHudNfc.setTextColor(0xFF9E9E9E);
                            }
                        }
                        if (tvSubNfc != null) {
                            tvSubNfc.setText(nfcInfo.getDisplayStatus());
                            tvSubNfc.setTextColor(nfcInfo.getStatusColor());
                        }
                    }
                });
            }
        }).start();
    }

    private void updatePrivilegeButton() {
        if (btnHeaderPrivilege == null) return;

        boolean isShizukuPerm = ShellHelper.isShizukuPermissionGranted();
        boolean isShizukuRunning = ShellHelper.isShizukuRunning();
        boolean isRoot = ShellHelper.isRootAvailable();

        if (isShizukuPerm) {
            btnHeaderPrivilege.setText("🛡️ Shizuku: Активен ✓");
            btnHeaderPrivilege.setBackgroundResource(R.drawable.btn_badge_shizuku);
        } else if (isRoot) {
            btnHeaderPrivilege.setText("⚡ Root: Активен ✓");
            btnHeaderPrivilege.setBackgroundResource(R.drawable.btn_badge_root);
        } else if (isShizukuRunning) {
            btnHeaderPrivilege.setText("⚠️ Shizuku: Дать доступ");
            btnHeaderPrivilege.setBackgroundResource(R.drawable.btn_badge_warning);
        } else if (ShellHelper.isShizukuInstalled(this)) {
            btnHeaderPrivilege.setText("⚡ Запустить Shizuku");
            btnHeaderPrivilege.setBackgroundResource(R.drawable.btn_badge_warning);
        } else {
            btnHeaderPrivilege.setText("📥 Скачать Shizuku");
            btnHeaderPrivilege.setBackgroundResource(R.drawable.btn_badge_download);
        }
    }

    private void showPrivilegeActionDialog() {
        boolean isShizukuPerm = ShellHelper.isShizukuPermissionGranted();
        boolean isShizukuRunning = ShellHelper.isShizukuRunning();
        boolean isInstalled = ShellHelper.isShizukuInstalled(this);
        boolean isRoot = ShellHelper.isRootAvailable();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (isShizukuPerm) {
            builder.setTitle("Shizuku подключен ✓");
            builder.setMessage("Приложение имеет полный доступ через Shizuku. Все функции переключения разрешения, герцовки, AOD, NFC и оверлеев работают в 1 клик.");
            builder.setPositiveButton("Открыть Shizuku", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShellHelper.openShizuku(MainMenuActivity.this);
                }
            });
            builder.setNegativeButton("Закрыть", null);
        } else if (isRoot) {
            builder.setTitle("Root-доступ активен ⚡");
            builder.setMessage("Обнаружен прямой доступ SuperUser. Все системные настройки и фиксы применяются с наивысшим приоритетом.");
            builder.setPositiveButton("Отлично", null);
        } else if (isShizukuRunning) {
            builder.setTitle("Предоставить доступ Shizuku");
            builder.setMessage("Служба Shizuku запущена в системе, но этому приложению ещё не выдан доступ.\n\nНажмите кнопку ниже, чтобы подтвердить запрос разрешения.");
            builder.setPositiveButton("Запросить доступ", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShellHelper.requestShizukuPermission();
                }
            });
            builder.setNegativeButton("Отмена", null);
        } else if (isInstalled) {
            builder.setTitle("Запуск службы Shizuku");
            builder.setMessage("Приложение Shizuku установлено на вашем телефоне, но служба ещё не запущена.\n\nЗапустите Shizuku через «Беспроводную отладку» (Wireless Debugging) в настройках разработчика.");
            builder.setPositiveButton("Открыть Shizuku", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShellHelper.openShizuku(MainMenuActivity.this);
                }
            });
            builder.setNegativeButton("Позже", null);
        } else {
            builder.setTitle("Установка Shizuku");
            builder.setMessage("Shizuku позволяет применять все оптимизации (разрешение, 144 Гц, фикс NFC, AOD) без компьютера и без Root-прав.\n\nРекомендуется скачать и установить Shizuku.");
            builder.setPositiveButton("Скачать с GitHub", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShellHelper.openUrl(MainMenuActivity.this, "https://github.com/RikkaApps/Shizuku/releases");
                }
            });
            builder.setNeutralButton("Google Play", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShellHelper.openUrl(MainMenuActivity.this, "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api");
                }
            });
            builder.setNegativeButton("Отмена", null);
        }

        builder.show();
    }
}
