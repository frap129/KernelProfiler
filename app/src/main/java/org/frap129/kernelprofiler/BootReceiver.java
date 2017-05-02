package org.frap129.kernelprofiler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences boot = context.getSharedPreferences("onBoot", Context.MODE_PRIVATE);
            if (boot.getBoolean("onBoot", true)) {
                SharedPreferences path = context.getSharedPreferences("profilePath", Context.MODE_PRIVATE);
                String profilePath = path.getString("profilePath", "");
                MainActivity.setProfile(profilePath);
            }
        }
    }
}