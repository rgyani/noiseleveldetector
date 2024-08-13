package com.example.noiseleveldetector;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1;
    private static final String CHANNEL_ID = "NoiseAlertChannel";
    private static final String NOISE_LEVEL_UPDATE = "com.example.noiseleveldetector.NOISE_LEVEL_UPDATE";

    private TextView noiseLevelTextView, thresholdText;
    private SeekBar thresholdSeekBar;
    private Button startButton;
    private int noiseThreshold = 80;
    private boolean isMonitoring = false;
    private NoiseLevelReceiver noiseLevelReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noiseLevelTextView = findViewById(R.id.noiseLevel);
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar);
        thresholdText = findViewById(R.id.thresholdText);
        startButton = findViewById(R.id.startButton);

        createNotificationChannel();

        thresholdSeekBar.setProgress(noiseThreshold);
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                noiseThreshold = progress;
                thresholdText.setText(String.format("Threshold: %d dB", noiseThreshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        startButton.setOnClickListener(v -> {
            if (isMonitoring) {
                stopMonitoringService();
                startButton.setText("Start Monitoring");
            } else {
                startMonitoringService();
                startButton.setText("Stop Monitoring");
            }
        });

        checkMicrophonePermission();
    }



    private void checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        }
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, NoiseMonitoringService.class);
        serviceIntent.putExtra("threshold", noiseThreshold);
        ContextCompat.startForegroundService(this, serviceIntent);
        isMonitoring = true;
    }

    private void stopMonitoringService() {
        Intent serviceIntent = new Intent(this, NoiseMonitoringService.class);
        stopService(serviceIntent);
        isMonitoring = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Noise Alert Channel";
            String description = "Channel for noise alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        noiseLevelReceiver = new NoiseLevelReceiver();
        registerReceiver(noiseLevelReceiver, new IntentFilter(NOISE_LEVEL_UPDATE), RECEIVER_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (noiseLevelReceiver != null) {
            unregisterReceiver(noiseLevelReceiver);
        }
    }

    private class NoiseLevelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            double noiseLevel = intent.getDoubleExtra("noise_level", 0.0);
            noiseLevelTextView.setText(String.format("Noise Level: %.2f dB", noiseLevel));
        }
    }
}
