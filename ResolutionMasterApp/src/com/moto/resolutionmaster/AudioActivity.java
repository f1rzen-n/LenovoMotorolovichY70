package com.moto.resolutionmaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class AudioActivity extends Activity {

    private Button btnBack;
    private Button btnAudioBtAbsVol;
    private Button btnAudioSafeVol;
    private TextView tvMediaVolumeVal;
    private SeekBar sbMediaVolume;
    private Button btnOpenMultiVolume;
    private Button btnOpenDolby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

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
        btnAudioBtAbsVol = findViewById(R.id.btn_audio_bt_abs_vol);
        btnAudioSafeVol = findViewById(R.id.btn_audio_safe_vol);
        tvMediaVolumeVal = findViewById(R.id.tv_media_volume_val);
        sbMediaVolume = findViewById(R.id.sb_media_volume);
        btnOpenMultiVolume = findViewById(R.id.btn_open_multivolume);
        btnOpenDolby = findViewById(R.id.btn_open_dolby);

        if (btnAudioBtAbsVol != null) {
            btnAudioBtAbsVol.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AudioHelper.toggleBluetoothAbsoluteVolume(AudioActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean disabled = AudioHelper.isBluetoothAbsoluteVolumeDisabled(AudioActivity.this);
                                    Toast.makeText(AudioActivity.this, "Раздельная громкость BT: " + (disabled ? "Включена 🎧" : "Выключена (Сток)"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnAudioSafeVol != null) {
            btnAudioSafeVol.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AudioHelper.toggleSafeVolumeBypass(AudioActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                    boolean bypassed = AudioHelper.isSafeVolumeBypassed(AudioActivity.this);
                                    Toast.makeText(AudioActivity.this, "Обход лимита громкости: " + (bypassed ? "Активен 🛡️" : "Выключен (Сток)"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (sbMediaVolume != null) {
            int curVol = AudioHelper.getMediaVolumePercent(this);
            sbMediaVolume.setProgress(curVol);
            if (tvMediaVolumeVal != null) tvMediaVolumeVal.setText(curVol + "%");

            sbMediaVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvMediaVolumeVal != null) {
                        tvMediaVolumeVal.setText(progress + "%");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    final int progress = seekBar.getProgress();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AudioHelper.setMediaVolumePercent(AudioActivity.this, progress);
                        }
                    }).start();
                    if (tvMediaVolumeVal != null) tvMediaVolumeVal.setText(progress + "%");
                }
            });
        }

        if (btnOpenMultiVolume != null) {
            btnOpenMultiVolume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioHelper.launchMultiVolume(AudioActivity.this);
                }
            });
        }

        if (btnOpenDolby != null) {
            btnOpenDolby.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioHelper.launchDolbyAtmos(AudioActivity.this);
                }
            });
        }
    }

    private void updateUI() {
        boolean btDisabled = AudioHelper.isBluetoothAbsoluteVolumeDisabled(this);
        if (btnAudioBtAbsVol != null) {
            if (btDisabled) {
                btnAudioBtAbsVol.setText("🎧 Раздельная BT: Вкл ✓");
                btnAudioBtAbsVol.setTextColor(0xFF10B981);
            } else {
                btnAudioBtAbsVol.setText("🎧 Раздельная BT: Выкл");
                btnAudioBtAbsVol.setTextColor(0xFFF3F4F6);
            }
        }

        boolean safeBypassed = AudioHelper.isSafeVolumeBypassed(this);
        if (btnAudioSafeVol != null) {
            if (safeBypassed) {
                btnAudioSafeVol.setText("🛡️ Обход лимита: Вкл ✓");
                btnAudioSafeVol.setTextColor(0xFF10B981);
            } else {
                btnAudioSafeVol.setText("🛡️ Обход лимита: Выкл");
                btnAudioSafeVol.setTextColor(0xFFF3F4F6);
            }
        }

        int curVol = AudioHelper.getMediaVolumePercent(this);
        if (sbMediaVolume != null) {
            sbMediaVolume.setProgress(curVol);
        }
        if (tvMediaVolumeVal != null) {
            tvMediaVolumeVal.setText(curVol + "%");
        }
    }
}
