package com.example.noiseleveldetector;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1;
    private static final int SAMPLE_RATE = 44100;
    private static final String CHANNEL_ID = "NoiseAlertChannel";

    private TextView noiseLevelTextView, thresholdText;
    private SeekBar thresholdSeekBar;
    private int noiseThreshold = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noiseLevelTextView = findViewById(R.id.noiseLevel);
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar);
        thresholdText = findViewById(R.id.thresholdText);
        Button startButton = findViewById(R.id.startButton);

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
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
            } else {
                startMonitoringService();
            }
        });
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, NoiseMonitoringService.class);
        serviceIntent.putExtra("threshold", noiseThreshold);
        ContextCompat.startForegroundService(this, serviceIntent);
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
}