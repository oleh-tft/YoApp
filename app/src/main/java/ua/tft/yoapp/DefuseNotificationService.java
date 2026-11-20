package ua.tft.yoapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class DefuseNotificationService extends Service {

    private static final String CHANNEL_SERVICE_ID = "defuse_service_channel";
    private static final String CHANNEL_ALERT_ID = "defuse_alert_channel";
    private static final int FOREGROUND_ID = 101;
    private static final int ALERT_ID = 202;
    private static final long DELAY_MS = 10_000L; // 30 секунд

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable notifyRunnable = () -> {
        showAlertNotification();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannels();

        Notification serviceNotification = new NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("DAILY DEFUSING")
                .setContentText("Очікування повернення агента...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, serviceNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(FOREGROUND_ID, serviceNotification);
        }

        // Перезапускаємо таймер на 30 секунд
        handler.removeCallbacks(notifyRunnable);
        handler.postDelayed(notifyRunnable, DELAY_MS);

        return START_NOT_STICKY;
    }

    private void showAlertNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification alert = new NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle("DAILY DEFUSING // CASE #404")
                .setContentText("Ти вже 10 секунд не грав в Daily Defusing. Повертайся!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Ти вже 10 секунд не грав в Daily Defusing. Повертайся!"))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build();

        manager.notify(ALERT_ID, alert);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager.getNotificationChannel(CHANNEL_SERVICE_ID) == null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_SERVICE_ID,
                        "Фонова служба",
                        NotificationManager.IMPORTANCE_LOW
                );
                manager.createNotificationChannel(serviceChannel);
            }

            if (manager.getNotificationChannel(CHANNEL_ALERT_ID) == null) {
                NotificationChannel alertChannel = new NotificationChannel(
                        CHANNEL_ALERT_ID,
                        "Нагадування Daily Defusing",
                        NotificationManager.IMPORTANCE_HIGH
                );
                alertChannel.setDescription("Сповіщення про повернення в гру");
                alertChannel.enableVibration(true);
                manager.createNotificationChannel(alertChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Якщо користувач повернувся в додаток раніше — скасовуємо сповіщення
        handler.removeCallbacks(notifyRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}