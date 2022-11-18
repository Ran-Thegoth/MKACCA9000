package rs.fncore2.core.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;


import rs.fncore3.R;

public class NotificationsHelper {

    public static final String CHANNEL_ID = "fiscal_core";
    public static final String CHANNEL_NAME = "Фискальное ядро";
    public static final String CHANNEL_ID_STORAGE = "fiscal_storage";
    public static final String CHANNEL_STORAGE_NAME = "Фискальный накопитель";
    public static final int NOTIFICATION_ID = 5000;
    public static final int NOTIFICATION_ID_STORAGE = 5001;

    public static void createServiceChannel(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // create channels
            NotificationChannel channelStorage = new NotificationChannel(CHANNEL_ID_STORAGE,
                    CHANNEL_STORAGE_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            channelStorage.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelStorage.setDescription("Оповещения присутствия фискального накопителя");
            channelStorage.setShowBadge(true);
            notificationManager.createNotificationChannel(channelStorage);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setDescription("Оповещения приложения");
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressWarnings("deprecation")
	private static Notification.Builder getNotificationBuilder(Context context){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return new Notification.Builder(context, CHANNEL_ID);
        } else {
            return new Notification.Builder(context);
        }
    }

    public static void clear(Service context) {
    	NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	notificationManager.cancel(NOTIFICATION_ID);
    	notificationManager.cancel(NOTIFICATION_ID_STORAGE);
    }
    public static void initNotifications(Service context) {
        Notification.Builder nb = getNotificationBuilder(context);
        Notification notification = nb
                .setSmallIcon(R.drawable.ic_launcher_bw)
                .setLargeIcon(((BitmapDrawable) context.getDrawable(R.drawable.ic_launcher)).getBitmap())
                .setContentTitle(context.getString(R.string.app_name))
                .setStyle(new Notification.BigTextStyle().bigText(context.getString(R.string.app_name)))
                .setAutoCancel(false)
                .setContentText("Сервис запущен")
                .build();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForeground(NOTIFICATION_ID, notification);
        } else {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, nb.build());
        }
    }
}
