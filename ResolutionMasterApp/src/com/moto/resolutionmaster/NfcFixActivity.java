package com.moto.resolutionmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class NfcFixActivity extends Activity {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Button btnBack;
    private TextView tvNfcStatus;
    private TextView tvNfcAuthor;
    private Button btnInstallNfc;
    private Button btnUninstallNfc;
    private Button btnExportNfc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_fix);

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
        updateNfcStatusUI();
    }

    private void setupControls() {
        tvNfcStatus = findViewById(R.id.tv_nfc_status);
        tvNfcAuthor = findViewById(R.id.tv_nfc_author);
        btnInstallNfc = findViewById(R.id.btn_install_nfc_module);
        btnUninstallNfc = findViewById(R.id.btn_uninstall_nfc_module);
        btnExportNfc = findViewById(R.id.btn_export_nfc_zip);

        if (tvNfcStatus != null) {
            tvNfcStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tvNfcStatus.setText("Маршруты NFC: Проверка статуса...");
                    tvNfcStatus.setTextColor(0xFFF59E0B);
                    updateNfcStatusUI();
                }
            });
        }

        if (tvNfcAuthor != null) {
            tvNfcAuthor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShellHelper.openUrl(NfcFixActivity.this, "https://4pda.to/forum/index.php?showtopic=1121132&view=findpost&p=146040708");
                }
            });
        }

        if (btnInstallNfc != null) {
            btnInstallNfc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!ShellHelper.isShizukuPermissionGranted() && !ShellHelper.isRootAvailable()) {
                        new AlertDialog.Builder(NfcFixActivity.this)
                            .setTitle("Требуется Shizuku или Root")
                            .setMessage("Для активации фикса NFC выберите удобный вариант:\n\n" +
                                "1. Shizuku (без ПК) — активируйте Shizuku через «Беспроводную отладку», и фикс применится в 1 клик прямо на телефоне.\n\n" +
                                "2. Root — предоставьте Root-доступ приложению.\n\n" +
                                "3. Либо нажмите «📦 Экспорт ZIP» и прошейте архив через Magisk / KernelSU.")
                            .setPositiveButton("Открыть Shizuku", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ShellHelper.openShizuku(NfcFixActivity.this);
                                }
                            })
                            .setNegativeButton("Закрыть", null)
                            .show();
                        return;
                    }

                    if (tvNfcStatus != null) {
                        tvNfcStatus.setText("Маршруты NFC: Применение фикса...");
                        tvNfcStatus.setTextColor(0xFFF59E0B);
                    }
                    if (btnInstallNfc != null) {
                        btnInstallNfc.setEnabled(false);
                        btnInstallNfc.setText("⏳ Применение...");
                    }
                    if (btnUninstallNfc != null) btnUninstallNfc.setEnabled(false);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean ok = NfcFixHelper.applyMasterFix(NfcFixActivity.this);
                            final NfcFixHelper.RoutingInfo updatedInfo = NfcFixHelper.getLiveRoutingInfo(NfcFixActivity.this);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    applyNfcInfoToViews(updatedInfo);
                                    if (ok || updatedInfo.status == NfcFixHelper.NfcStatus.FIXED_100_PERCENT) {
                                        new AlertDialog.Builder(NfcFixActivity.this)
                                            .setTitle("Фикс NFC успешно применён! ⚡")
                                            .setMessage("Таблица маршрутизации контроллера перенаправлена на Host Android (DH, DH, DH):\n\n" +
                                                "• Empty AID ➔ 0x00 (DH)\n" +
                                                "• PROTOCOL ISO-DEP ➔ 0x00 (DH)\n" +
                                                "• TECHNOLOGY A & B ➔ 0x00 (DH)\n\n" +
                                                "Оплата в Mir Pay, СБПэй и Т-Банке теперь работает на 100% терминалов РФ (включая Сбер, ВТБ и транспорт).\n\n" +
                                                "Перезагрузка смартфона НЕ требуется — оплачивать можно сразу!")
                                            .setPositiveButton("Отлично", null)
                                            .show();
                                    } else {
                                        Toast.makeText(NfcFixActivity.this, "Ошибка применения фикса (проверьте права Shizuku/Root)", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    updateNfcStatusUI();
                                }
                            }, 1000);
                        }
                    }).start();
                }
            });
        }

        if (btnExportNfc != null) {
            btnExportNfc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    java.io.File zip = NfcFixHelper.exportModuleZip(NfcFixActivity.this);
                    if (zip != null && zip.exists()) {
                        new AlertDialog.Builder(NfcFixActivity.this)
                            .setTitle("ZIP-модуль создан! 📦")
                            .setMessage("Файл успешно сохранен в папку Загрузки:\n\n" + zip.getAbsolutePath() + "\n\nКак прошить:\n1. Откройте приложение Magisk (или KernelSU / APatch)\n2. Перейдите во вкладку «Модули»\n3. Нажмите «Установить из хранилища» и выберите этот ZIP\n4. Перезагрузите устройство.")
                            .setPositiveButton("Отлично", null)
                            .show();
                    } else {
                        Toast.makeText(NfcFixActivity.this, "Не удалось сохранить ZIP-архив", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnUninstallNfc != null) {
            btnUninstallNfc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNfcRollbackDialog();
                }
            });
        }
    }

    private void updateNfcStatusUI() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final NfcFixHelper.RoutingInfo nfcInfo = NfcFixHelper.getLiveRoutingInfo(NfcFixActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        applyNfcInfoToViews(nfcInfo);
                    }
                });
            }
        }).start();
    }

    private void applyNfcInfoToViews(NfcFixHelper.RoutingInfo nfcInfo) {
        if (tvNfcStatus != null) {
            tvNfcStatus.setText("Маршруты NFC: " + nfcInfo.getDisplayStatus());
            tvNfcStatus.setTextColor(nfcInfo.getStatusColor());
        }
        if (nfcInfo.status == NfcFixHelper.NfcStatus.FIXED_100_PERCENT) {
            if (btnInstallNfc != null) {
                btnInstallNfc.setEnabled(true);
                btnInstallNfc.setText("⚡ Переприменить фикс (DH, DH, DH)");
            }
        } else {
            if (btnInstallNfc != null) {
                btnInstallNfc.setEnabled(true);
                btnInstallNfc.setText("⚡ Активировать фикс (в 1 клик)");
            }
        }
        if (btnUninstallNfc != null) {
            btnUninstallNfc.setEnabled(true);
            btnUninstallNfc.setVisibility(View.VISIBLE);
            btnUninstallNfc.setText("🔄 Откат к стоку (eSE1)");
        }
    }

    private void showNfcRollbackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Откат фикса NFC к заводскому стоку");
        builder.setMessage(
            "Откат вернет маршруты NFC к исходному заводскому состоянию (eSE1):\n\n" +
            "• Прямой откат (Shizuku / Root): мгновенный возврат маршрутов в сток прямо на смартфоне без перезагрузки.\n" +
            "• Экспорт ZIP отката: сохранение архива «moto_y70_nfc_revert_stock.zip» в Загрузки для прошивки через Magisk / KernelSU.\n\n" +
            "Выберите способ отката:"
        );

        if (ShellHelper.isShizukuPermissionGranted() || ShellHelper.isRootAvailable()) {
            builder.setPositiveButton("Откатить сейчас", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (tvNfcStatus != null) {
                        tvNfcStatus.setText("Маршруты NFC: Откат к стоку...");
                        tvNfcStatus.setTextColor(0xFFF59E0B);
                    }
                    if (btnInstallNfc != null) btnInstallNfc.setEnabled(false);
                    if (btnUninstallNfc != null) {
                        btnUninstallNfc.setEnabled(false);
                        btnUninstallNfc.setText("⏳ Откат...");
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean ok = NfcFixHelper.revertToStock(NfcFixActivity.this);
                            final NfcFixHelper.RoutingInfo updatedInfo = NfcFixHelper.getLiveRoutingInfo(NfcFixActivity.this);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    applyNfcInfoToViews(updatedInfo);
                                    if (ok || updatedInfo.status == NfcFixHelper.NfcStatus.FACTORY_STOCK) {
                                        new AlertDialog.Builder(NfcFixActivity.this)
                                            .setTitle("NFC возвращен к стоку! 🔄")
                                            .setMessage("Маршруты контроллера успешно переведены в заводской режим (eSE1, eSE1, eSE1). Перезагрузка не требуется.")
                                            .setPositiveButton("Понятно", null)
                                            .show();
                                    } else {
                                        Toast.makeText(NfcFixActivity.this, "Сброс выполнен", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    updateNfcStatusUI();
                                }
                            }, 1000);
                        }
                    }).start();
                }
            });
        }

        builder.setNeutralButton("📦 Экспорт ZIP отката", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                java.io.File zip = NfcFixHelper.exportRollbackZip(NfcFixActivity.this);
                if (zip != null && zip.exists()) {
                    new AlertDialog.Builder(NfcFixActivity.this)
                        .setTitle("ZIP отката создан! 📦")
                        .setMessage("Файл успешно сохранен в Загрузки:\n\n" + zip.getAbsolutePath() + "\n\nПрошейте этот архив в Magisk / KernelSU (Модули -> Установить из хранилища) для возврата к стоку.")
                        .setPositiveButton("Понятно", null)
                        .show();
                } else {
                    Toast.makeText(NfcFixActivity.this, "Не удалось создать ZIP отката", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}
