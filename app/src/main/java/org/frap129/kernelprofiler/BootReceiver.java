package org.frap129.kernelprofiler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Map;

public class BootReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences boot = context.getSharedPreferences("onBoot", Context.MODE_PRIVATE);
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

            } else {
                SharedPreferences prof = context.getSharedPreferences("profile", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prof.edit();
                editor.putString("profile", "");
                editor.apply();
            }
        }
    }
}