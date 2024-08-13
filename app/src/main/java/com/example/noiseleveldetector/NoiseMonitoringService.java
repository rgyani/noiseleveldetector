package com.example.noiseleveldetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class NoiseMonitoringService extends Service {

    private static final int SAMPLE_RATE = 44100;
    private static final String CHANNEL_ID = "NoiseAlertChannel";
    private static final String NOISE_LEVEL_UPDATE = "com.example.noiseleveldetector.NOISE_LEVEL_UPDATE";
    private AudioRecord recorder;
    private Thread monitoringThread;
    private int noiseThreshold;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        noiseThreshold = intent.getIntExtra("threshold", 80);
        startForegroundService();
        startMonitoring();
        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Noise Monitoring")
                .setContentText("Monitoring noise levels in the background")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void startMonitoring() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));

        recorder.startRecording();

        monitoringThread = new Thread(() -> {
            short[] buffer = new short[1024];
            while (!Thread.currentThread().isInterrupted()) {
                recorder.read(buffer, 0, buffer.length);
                double amplitude = calculateAmplitude(buffer);
                double decibels = 20 * Math.log10(amplitude / 32767.0);

                handler.post(() -> {
                    broadcastNoiseLevel(decibels);
                    if (decibels > noiseThreshold) {
                        triggerNotification(decibels);
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        monitoringThread.start();
    }

    private double calculateAmplitude(short[] buffer) {
        double sum = 0;
        for (short s : buffer) {
            sum += Math.abs(s);
        }
        return sum / buffer.length;
    }

    private void broadcastNoiseLevel(double decibels) {
        Intent intent = new Intent(NOISE_LEVEL_UPDATE);
        intent.putExtra("noise_level", decibels);
        sendBroadcast(intent);
    }

    private void triggerNotification(double decibels) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Noise Alert")
                .setContentText(String.format("Noise level exceeded: %.2f dB", decibels))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(2, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
