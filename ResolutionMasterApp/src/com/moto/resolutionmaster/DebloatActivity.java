package com.moto.resolutionmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DebloatActivity extends Activity {

    private Button btnTabPreset;
    private Button btnTabManual;
    private ScrollView scrollPresetTab;
    private LinearLayout layoutManualTab;

    // Preset Views
    private int currentPresetType = DebloatPreset.TYPE_AUTHOR;
    private Button btnPresetAuthor;
    private Button btnPresetAosp;
    private Button btnPresetBalanced;
    private Button btnPresetMinimal;
    private TextView tvPresetInfoTitle;
    private TextView tvPresetTarget;
    private TextView tvPresetWhatInside;
    private TextView tvPresetCounter;
    private ProgressBar pbPresetBatch;
    private Button btnPresetApplyAll;
    private Button btnPresetEnableAll;
    private LinearLayout llPresetContainer;

    // Manual Views
    private EditText etSearch;
    private Button btnFilterAll;
    private Button btnFilterSystem;
    private Button btnFilterUser;
    private Button btnFilterDisabled;
    private ProgressBar pbLoadingApps;
    private ListView lvApps;

    private AppListAdapter appListAdapter;
    private final List<AppInfo> installedApps = new ArrayList<>();
    private boolean isAppsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayManagerHelper.bypassHiddenApi(this);
        setContentView(R.layout.activity_debloat);

        Button btnBack = findViewById(R.id.btn_back_menu);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        btnTabPreset = findViewById(R.id.btn_tab_preset);
        btnTabManual = findViewById(R.id.btn_tab_manual);
        scrollPresetTab = findViewById(R.id.scroll_preset_tab);
        layoutManualTab = findViewById(R.id.layout_manual_tab);

        // Tab Switching
        if (btnTabPreset != null) {
            btnTabPreset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(true);
                }
            });
        }
        if (btnTabManual != null) {
            btnTabManual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(false);
                }
            });
        }

        // Preset Init
        btnPresetAuthor = findViewById(R.id.btn_preset_author);
        btnPresetAosp = findViewById(R.id.btn_preset_aosp);
        btnPresetBalanced = findViewById(R.id.btn_preset_balanced);
        btnPresetMinimal = findViewById(R.id.btn_preset_minimal);
        tvPresetInfoTitle = findViewById(R.id.tv_preset_info_title);
        tvPresetTarget = findViewById(R.id.tv_preset_target);
        tvPresetWhatInside = findViewById(R.id.tv_preset_what_inside);
        tvPresetCounter = findViewById(R.id.tv_preset_counter);
        pbPresetBatch = findViewById(R.id.pb_preset_batch);
        btnPresetApplyAll = findViewById(R.id.btn_preset_apply_all);
        btnPresetEnableAll = findViewById(R.id.btn_preset_enable_all);
        llPresetContainer = findViewById(R.id.ll_preset_container);

        Button btnShowTable = findViewById(R.id.btn_show_preset_table);
        if (btnShowTable != null) {
            btnShowTable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPresetComparisonDialog();
                }
            });
        }

        View.OnClickListener presetTabClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_preset_author) {
                    selectPreset(DebloatPreset.TYPE_AUTHOR);
                } else if (id == R.id.btn_preset_aosp) {
                    selectPreset(DebloatPreset.TYPE_AOSP);
                } else if (id == R.id.btn_preset_balanced) {
                    selectPreset(DebloatPreset.TYPE_BALANCED);
                } else if (id == R.id.btn_preset_minimal) {
                    selectPreset(DebloatPreset.TYPE_MINIMAL);
                }
            }
        };

        if (btnPresetAuthor != null) btnPresetAuthor.setOnClickListener(presetTabClick);
        if (btnPresetAosp != null) btnPresetAosp.setOnClickListener(presetTabClick);
        if (btnPresetBalanced != null) btnPresetBalanced.setOnClickListener(presetTabClick);
        if (btnPresetMinimal != null) btnPresetMinimal.setOnClickListener(presetTabClick);

        if (btnPresetApplyAll != null) {
            btnPresetApplyAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmBatchPreset(true);
                }
            });
        }

        if (btnPresetEnableAll != null) {
            btnPresetEnableAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmBatchPreset(false);
                }
            });
        }

        Button btnDebloatRollback = findViewById(R.id.btn_debloat_rollback);
        if (btnDebloatRollback != null) {
            btnDebloatRollback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRollbackOptionsDialog();
                }
            });
        }

        Button btnFullStockRollback = findViewById(R.id.btn_full_stock_rollback);
        if (btnFullStockRollback != null) {
            btnFullStockRollback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeGlobalRollback();
                }
            });
        }

        // Manual Init
        etSearch = findViewById(R.id.et_search);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterSystem = findViewById(R.id.btn_filter_system);
        btnFilterUser = findViewById(R.id.btn_filter_user);
        btnFilterDisabled = findViewById(R.id.btn_filter_disabled);
        pbLoadingApps = findViewById(R.id.pb_loading_apps);
        lvApps = findViewById(R.id.lv_apps);

        setupFilters();
        setupSearch();

        // Populate preset list immediately
        refreshPresetList();
    }

    private void switchTab(boolean showPreset) {
        if (showPreset) {
            btnTabPreset.setBackgroundResource(R.drawable.btn_primary);
            btnTabPreset.setTextColor(0xFFFFFFFF);
            btnTabManual.setBackgroundColor(0x00000000);
            btnTabManual.setTextColor(0xFF9CA3AF);

            scrollPresetTab.setVisibility(View.VISIBLE);
            layoutManualTab.setVisibility(View.GONE);
            refreshPresetList();
        } else {
            btnTabManual.setBackgroundResource(R.drawable.btn_primary);
            btnTabManual.setTextColor(0xFFFFFFFF);
            btnTabPreset.setBackgroundColor(0x00000000);
            btnTabPreset.setTextColor(0xFF9CA3AF);

            scrollPresetTab.setVisibility(View.GONE);
            layoutManualTab.setVisibility(View.VISIBLE);

            if (!isAppsLoaded) {
                loadInstalledApps();
            }
        }
    }

    private void selectPreset(int type) {
        currentPresetType = type;
        updatePresetTabButtons();
        refreshPresetList();
    }

    private void updatePresetTabButtons() {
        int chipBg = R.drawable.btn_chip;
        int activeBg = R.drawable.btn_primary;
        int textMuted = 0xFF9CA3AF;
        int textWhite = 0xFFFFFFFF;

        if (btnPresetAuthor != null) {
            btnPresetAuthor.setBackgroundResource(currentPresetType == DebloatPreset.TYPE_AUTHOR ? activeBg : chipBg);
            btnPresetAuthor.setTextColor(currentPresetType == DebloatPreset.TYPE_AUTHOR ? textWhite : textMuted);
        }
        if (btnPresetAosp != null) {
            btnPresetAosp.setBackgroundResource(currentPresetType == DebloatPreset.TYPE_AOSP ? activeBg : chipBg);
            btnPresetAosp.setTextColor(currentPresetType == DebloatPreset.TYPE_AOSP ? textWhite : textMuted);
        }
        if (btnPresetBalanced != null) {
            btnPresetBalanced.setBackgroundResource(currentPresetType == DebloatPreset.TYPE_BALANCED ? activeBg : chipBg);
            btnPresetBalanced.setTextColor(currentPresetType == DebloatPreset.TYPE_BALANCED ? textWhite : textMuted);
        }
        if (btnPresetMinimal != null) {
            btnPresetMinimal.setBackgroundResource(currentPresetType == DebloatPreset.TYPE_MINIMAL ? activeBg : chipBg);
            btnPresetMinimal.setTextColor(currentPresetType == DebloatPreset.TYPE_MINIMAL ? textWhite : textMuted);
        }

        if (tvPresetInfoTitle != null) {
            tvPresetInfoTitle.setText(DebloatPreset.getPresetTitle(currentPresetType));
        }
        if (tvPresetTarget != null) {
            tvPresetTarget.setText("🎯 Для кого: " + DebloatPreset.getPresetTarget(currentPresetType));
        }
        if (tvPresetWhatInside != null) {
            tvPresetWhatInside.setText("📦 Что внутри:\n" + DebloatPreset.getPresetWhatInside(currentPresetType));
        }
    }

    private void showPresetComparisonDialog() {
        String msg =
            "══════════════════════════════\n" +
            "1. МИНИМАЛЬНЫЙ (SAFE)\n" +
            "• Для кого: Для всех, 100% без рисков\n" +
            "• Что внутри: Рекламные загрузчики, кредитные хвосты (PayJoy, PAKS AppOps-freeze), иностранные операторы.\n\n" +
            "══════════════════════════════\n" +
            "2. БАЛАНС\n" +
            "• Для кого: Максимум автономности без потери фишек Moto\n" +
            "• Что внутри: Минимальный + вся телеметрия Moto/Google, предустановленный мусор, отключение Doze-трекеров, ненужные сервисы Google. Жесты и камера сохраняются!\n\n" +
            "══════════════════════════════\n" +
            "3. ЧИСТЫЙ\n" +
            "• Для кого: Для фанатов чистого Android / Google Pixel\n" +
            "• Что внутри: Рекомендуемый + отключение надстроек Moto (Smart Connect, Freeform, Moto App, Launcher-оверлеи) с переходом на чистый стиль. Лаунчер Launcher3 защищен!\n\n" +
            "══════════════════════════════\n" +
            "4. АВТОРСКИЙ (Y70 EDITION)\n" +
            "• Для кого: Проверено сообществом (эталон Y70)\n" +
            "• Что внутри: Эталонный список из 111 пакетов для сообщества 4PDA, настроенный и проверенный на Legion Y70.";

        new AlertDialog.Builder(this)
            .setTitle("Таблица сценариев очистки")
            .setMessage(msg)
            .setPositiveButton("Понятно", null)
            .show();
    }

    private int mPresetLoadGen = 0;

    private static class PresetRowState {
        final DebloatPreset.PresetItem item;
        final boolean installed;
        final boolean enabled;
        final boolean isProtected;

        PresetRowState(DebloatPreset.PresetItem item, boolean installed, boolean enabled, boolean isProtected) {
            this.item = item;
            this.installed = installed;
            this.enabled = enabled;
            this.isProtected = isProtected;
        }
    }

    private void refreshPresetList() {
        if (llPresetContainer == null) return;
        updatePresetTabButtons();

        final int gen = ++mPresetLoadGen;
        final int presetType = currentPresetType;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<DebloatPreset.PresetItem> items = DebloatPreset.getPresetList(presetType);
                final List<PresetRowState> states = new ArrayList<>(items.size());
                int disabled = 0;
                for (DebloatPreset.PresetItem item : items) {
                    boolean installed = PackageHelper.isPackageInstalled(DebloatActivity.this, item.packageName);
                    boolean enabled = installed && PackageHelper.isPackageEnabled(DebloatActivity.this, item.packageName);
                    boolean isProtected = PackageHelper.isProtectedOrNonDisable(item.packageName);
                    if (!enabled) disabled++;
                    states.add(new PresetRowState(item, installed, enabled, isProtected));
                }
                final int total = items.size();
                final int disabledCount = disabled;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed() || gen != mPresetLoadGen) return;
                        llPresetContainer.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(DebloatActivity.this);

                        for (final PresetRowState st : states) {
                            final DebloatPreset.PresetItem item = st.item;
                            View row = inflater.inflate(R.layout.item_preset_app, llPresetContainer, false);
                            TextView tvTitle = row.findViewById(R.id.tv_preset_title);
                            TextView tvPkg = row.findViewById(R.id.tv_preset_pkg);
                            TextView tvDesc = row.findViewById(R.id.tv_preset_desc);
                            TextView tvCat = row.findViewById(R.id.tv_preset_category);
                            TextView tvStatus = row.findViewById(R.id.tv_preset_status);
                            final Button btnToggle = row.findViewById(R.id.btn_preset_toggle);

                            tvTitle.setText(item.title);
                            tvPkg.setText(item.packageName);
                            tvDesc.setText(item.description);
                            tvCat.setText(item.category);

                            if (!st.installed) {
                                tvStatus.setText("Не установлен / Удален");
                                tvStatus.setTextColor(0xFF6B7280);
                                btnToggle.setVisibility(View.GONE);
                            } else if (!st.enabled) {
                                tvStatus.setText(st.isProtected ? "Заморожено (AppOps) ✓" : "Отключено ✓");
                                tvStatus.setTextColor(0xFF10B981);
                                btnToggle.setText("Включить");
                                btnToggle.setBackgroundResource(R.drawable.btn_green);
                            } else {
                                tvStatus.setText("Активно");
                                tvStatus.setTextColor(0xFFF59E0B);
                                btnToggle.setText(st.isProtected ? "Заморозить" : "Отключить");
                                btnToggle.setBackgroundResource(R.drawable.btn_red);
                            }

                            btnToggle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    btnToggle.setEnabled(false);
                                    btnToggle.setText("...");
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            final boolean ok = st.enabled
                                                ? PackageHelper.disablePackage(DebloatActivity.this, item.packageName)
                                                : PackageHelper.enablePackage(DebloatActivity.this, item.packageName);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isFinishing() || isDestroyed()) return;
                                                    if (ok) {
                                                        String msg;
                                                        if (st.enabled) {
                                                            msg = st.isProtected ?
                                                                item.title + " заморожен в фоне (AppOps)" :
                                                                item.title + " отключен";
                                                        } else {
                                                            msg = item.title + " включен";
                                                        }
                                                        Toast.makeText(DebloatActivity.this, msg, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(DebloatActivity.this, "Ошибка (требуются права Shizuku/Root)", Toast.LENGTH_SHORT).show();
                                                    }
                                                    refreshPresetList();
                                                }
                                            });
                                        }
                                    }).start();
                                }
                            });

                            llPresetContainer.addView(row);
                        }

                        if (tvPresetCounter != null) {
                            tvPresetCounter.setText("Отключено: " + disabledCount + " из " + total + " пакетов пресета ✓");
                        }
                        if (btnPresetApplyAll != null) {
                            btnPresetApplyAll.setText("🚫 Применить пресет (" + total + ")");
                        }
                    }
                });
            }
        }).start();
    }

    private void confirmBatchPreset(final boolean applyPreset) {
        if (!applyPreset) {
            // Restore all packages of current preset
            new AlertDialog.Builder(this)
                .setTitle("Включить пакеты?")
                .setMessage("Все приложения из текущего пресета («" + DebloatPreset.getPresetTitle(currentPresetType) + "») будут снова включены.")
                .setPositiveButton("Включить все", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeEnableCurrentPreset();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
            return;
        }

        // Apply Preset: Smart bidirectional switching!
        List<DebloatPreset.PresetItem> targetItems = DebloatPreset.getPresetList(currentPresetType);
        final java.util.Set<String> targetSet = new java.util.HashSet<>();
        for (DebloatPreset.PresetItem it : targetItems) {
            targetSet.add(it.packageName);
        }

        final List<String> toDisable = new ArrayList<>();
        for (String pkg : targetSet) {
            if (PackageHelper.isPackageInstalled(this, pkg) && PackageHelper.isPackageEnabled(this, pkg)) {
                toDisable.add(pkg);
            }
        }

        final List<String> toEnable = new ArrayList<>();
        java.util.Set<String> allKnown = DebloatPreset.getAllKnownPackages();
        for (String pkg : allKnown) {
            if (!targetSet.contains(pkg) && PackageHelper.isPackageInstalled(this, pkg) && !PackageHelper.isPackageEnabled(this, pkg)) {
                toEnable.add(pkg);
            }
        }

        String msg = "Синхронизация с профилем «" + DebloatPreset.getPresetTitle(currentPresetType) + "»:\n\n" +
            "• Будет отключено/заморожено: " + toDisable.size() + " приложений\n" +
            "• Будет включено обратно: " + toEnable.size() + " приложений\n\n" +
            "🛡️ Родной лаунчер Launcher3 и важные системные службы гарантированно защищены.";

        new AlertDialog.Builder(this)
            .setTitle("Применить пресет?")
            .setMessage(msg)
            .setPositiveButton("Применить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    executeApplyPreset(toDisable, toEnable);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void executeApplyPreset(final List<String> toDisable, final List<String> toEnable) {
        final int totalOperations = toDisable.size() + toEnable.size();
        if (totalOperations == 0) {
            Toast.makeText(this, "Все приложения уже точно соответствуют выбранному пресету!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbPresetBatch != null) {
            pbPresetBatch.setMax(totalOperations);
            pbPresetBatch.setProgress(0);
            pbPresetBatch.setVisibility(View.VISIBLE);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                int disabledOk = 0;
                int enabledOk = 0;

                for (String pkg : toDisable) {
                    boolean ok = PackageHelper.disablePackage(DebloatActivity.this, pkg);
                    if (ok) disabledOk++;
                    count++;
                    final int progress = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pbPresetBatch != null) pbPresetBatch.setProgress(progress);
                        }
                    });
                }

                for (String pkg : toEnable) {
                    boolean ok = PackageHelper.enablePackage(DebloatActivity.this, pkg);
                    if (ok) enabledOk++;
                    count++;
                    final int progress = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pbPresetBatch != null) pbPresetBatch.setProgress(progress);
                        }
                    });
                }

                final int finalDisabled = disabledOk;
                final int finalEnabled = enabledOk;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (pbPresetBatch != null) pbPresetBatch.setVisibility(View.GONE);
                        refreshPresetList();
                        Toast.makeText(DebloatActivity.this, 
                            "Пресет применен! Отключено: " + finalDisabled + ", Включено обратно: " + finalEnabled, 
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void executeEnableCurrentPreset() {
        List<DebloatPreset.PresetItem> targetItems = DebloatPreset.getPresetList(currentPresetType);
        final List<String> toEnable = new ArrayList<>();
        for (DebloatPreset.PresetItem it : targetItems) {
            if (PackageHelper.isPackageInstalled(this, it.packageName) && !PackageHelper.isPackageEnabled(this, it.packageName)) {
                toEnable.add(it.packageName);
            }
        }

        if (toEnable.isEmpty()) {
            Toast.makeText(this, "Все приложения пресета уже включены!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbPresetBatch != null) {
            pbPresetBatch.setMax(toEnable.size());
            pbPresetBatch.setProgress(0);
            pbPresetBatch.setVisibility(View.VISIBLE);
        }

        PackageHelper.runBatchEnable(this, toEnable, new PackageHelper.BatchCallback() {
            @Override
            public void onProgress(final int current, final int total, String packageName, boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (pbPresetBatch != null) pbPresetBatch.setProgress(current);
                    }
                });
            }

            @Override
            public void onComplete(final int successCount, final int total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (pbPresetBatch != null) pbPresetBatch.setVisibility(View.GONE);
                        refreshPresetList();
                        Toast.makeText(DebloatActivity.this, "Включено " + successCount + " из " + total + " пакетов пресета.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showRollbackOptionsDialog() {
        final String[] options = new String[]{
            "🔄 Откат текущего пресета («" + DebloatPreset.getPresetTitle(currentPresetType) + "»)",
            "⚡ Полный откат к стоку (Включить ВСЕ приложения устройства)"
        };

        new AlertDialog.Builder(this)
            .setTitle("Откат отключенных приложений")
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        confirmBatchPreset(false);
                    } else if (which == 1) {
                        executeGlobalRollback();
                    }
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void executeGlobalRollback() {
        final java.util.Set<String> toEnableSet = new java.util.HashSet<>();
        toEnableSet.addAll(DebloatPreset.getAllKnownPackages());

        if (installedApps != null && !installedApps.isEmpty()) {
            for (AppInfo info : installedApps) {
                if (!info.isEnabled) {
                    toEnableSet.add(info.packageName);
                }
            }
        }

        final List<String> toEnable = new ArrayList<>();
        for (String pkg : toEnableSet) {
            if (PackageHelper.isPackageInstalled(this, pkg) && !PackageHelper.isPackageEnabled(this, pkg)) {
                toEnable.add(pkg);
            }
        }

        if (toEnable.isEmpty()) {
            Toast.makeText(this, "Все приложения уже включены! Система находится в заводском состоянии.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Полный откат к заводскому стоку?")
            .setMessage(
                "Будет включено обратно " + toEnable.size() + " системных приложений и служб Lenovo, Motorola и Google.\n\n" +
                "Также будет снята изоляция AppOps (PAKS, PayJoy). Система вернется к заводскому состоянию «из коробки».\n\n" +
                "Продолжить?"
            )
            .setPositiveButton("Включить всё", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    performGlobalRollback(toEnable);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void performGlobalRollback(final List<String> toEnable) {
        if (pbPresetBatch != null) {
            pbPresetBatch.setMax(toEnable.size());
            pbPresetBatch.setProgress(0);
            pbPresetBatch.setVisibility(View.VISIBLE);
        }

        PackageHelper.runBatchEnable(this, toEnable, new PackageHelper.BatchCallback() {
            @Override
            public void onProgress(final int current, final int total, String packageName, boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (pbPresetBatch != null) {
                            pbPresetBatch.setProgress(current);
                        }
                    }
                });
            }

            @Override
            public void onComplete(final int successCount, final int total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (pbPresetBatch != null) {
                            pbPresetBatch.setVisibility(View.GONE);
                        }
                        refreshPresetList();
                        if (isAppsLoaded) {
                            loadInstalledApps();
                        }
                        new AlertDialog.Builder(DebloatActivity.this)
                            .setTitle("Откат успешно выполнен! 🔄")
                            .setMessage("Включено обратно: " + successCount + " из " + total + " приложений.\n\nВсе заводские службы восстановлены.")
                            .setPositiveButton("Отлично", null)
                            .show();
                    }
                });
            }
        });
    }

    private void loadInstalledApps() {
        if (pbLoadingApps != null) pbLoadingApps.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppInfo> apps = PackageHelper.getInstalledApps(DebloatActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        installedApps.clear();
                        installedApps.addAll(apps);
                        isAppsLoaded = true;

                        if (pbLoadingApps != null) pbLoadingApps.setVisibility(View.GONE);

                        appListAdapter = new AppListAdapter(DebloatActivity.this, installedApps, new AppListAdapter.OnAppToggleListener() {
                            @Override
                            public void onToggle(final AppInfo app, final boolean enable) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final boolean ok = enable
                                            ? PackageHelper.enablePackage(DebloatActivity.this, app.packageName)
                                            : PackageHelper.disablePackage(DebloatActivity.this, app.packageName);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isFinishing() || isDestroyed()) return;
                                                if (ok) {
                                                    app.isEnabled = enable;
                                                    appListAdapter.notifyDataSetChanged();
                                                    String state = enable ? "включено" : "отключено";
                                                    Toast.makeText(DebloatActivity.this, app.label + " " + state, Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(DebloatActivity.this, "Ошибка! Проверьте Shizuku/Root", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });

                        if (lvApps != null) {
                            lvApps.setAdapter(appListAdapter);
                        }
                    }
                });
            }
        }).start();
    }

    private void setupFilters() {
        View.OnClickListener filterListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFilterButtons();
                int id = v.getId();
                if (id == R.id.btn_filter_all) {
                    btnFilterAll.setBackgroundResource(R.drawable.btn_primary);
                    btnFilterAll.setTextColor(0xFFFFFFFF);
                    if (appListAdapter != null) appListAdapter.setFilter(0);
                } else if (id == R.id.btn_filter_system) {
                    btnFilterSystem.setBackgroundResource(R.drawable.btn_primary);
                    btnFilterSystem.setTextColor(0xFFFFFFFF);
                    if (appListAdapter != null) appListAdapter.setFilter(1);
                } else if (id == R.id.btn_filter_user) {
                    btnFilterUser.setBackgroundResource(R.drawable.btn_primary);
                    btnFilterUser.setTextColor(0xFFFFFFFF);
                    if (appListAdapter != null) appListAdapter.setFilter(2);
                } else if (id == R.id.btn_filter_disabled) {
                    btnFilterDisabled.setBackgroundResource(R.drawable.btn_primary);
                    btnFilterDisabled.setTextColor(0xFFFFFFFF);
                    if (appListAdapter != null) appListAdapter.setFilter(3);
                }
            }
        };

        btnFilterAll.setOnClickListener(filterListener);
        btnFilterSystem.setOnClickListener(filterListener);
        btnFilterUser.setOnClickListener(filterListener);
        btnFilterDisabled.setOnClickListener(filterListener);
    }

    private void resetFilterButtons() {
        int chipBg = R.drawable.btn_chip;
        int textMuted = 0xFF9CA3AF;

        btnFilterAll.setBackgroundResource(chipBg);
        btnFilterAll.setTextColor(textMuted);
        btnFilterSystem.setBackgroundResource(chipBg);
        btnFilterSystem.setTextColor(textMuted);
        btnFilterUser.setBackgroundResource(chipBg);
        btnFilterUser.setTextColor(textMuted);
        btnFilterDisabled.setBackgroundResource(chipBg);
        btnFilterDisabled.setTextColor(textMuted);
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (appListAdapter != null) {
                    appListAdapter.setQuery(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
