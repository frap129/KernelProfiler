package org.frap129.kernelprofiler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;

import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class BootReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences boot = context.getSharedPreferences("onBoot", Context.MODE_PRIVATE);
            SharedPreferences bootNoti = context.getSharedPreferences("onBootNoti", Context.MODE_PRIVATE);
            SharedPreferences prof = context.getSharedPreferences("profile", Context.MODE_PRIVATE);
            if (boot.getBoolean("onBoot", true)) {
                SharedPreferences path = context.getSharedPreferences("profilePath", Context.MODE_PRIVATE);

                Map<String, ?> all = path.getAll();
                if(all.get("profilePath") instanceof String) {
                    String profilePath = path.getString("profilePath", "");
                    MainActivity.setProfile(profilePath, context);
                }
                else {
                    int profilePath = path.getInt("profilePath", 0);
                    MainActivity.setProfile(profilePath, context);
                }
                if (bootNoti.getBoolean("onBootNoti", true)) {
                    PackageManager pm = context.getPackageManager();
                    Intent launchIntent = pm.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
                    String profileNotification = "Successfully applied " + prof.getString("profile", "profile") + "!";
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.mipmap.ic_notification)
                                    .setContentTitle("Kernel Profiler")
                                    .setContentText(profileNotification)
                                    .setContentIntent(contentIntent)
                                    .setAutoCancel(true);
                    int mNotificationId = 1;
                    NotificationManager mNotifyMgr =
                            (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.notify(mNotificationId, mBuilder.build());
                }
            } else {
                SharedPreferences.Editor editor = prof.edit();
                editor.putString("profile", "");
                editor.apply();
            }
        }
    }
}