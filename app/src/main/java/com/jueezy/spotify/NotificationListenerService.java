package com.jueezy.spotify;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    SharedPreferences sharedPreferences;
    private static final String SHARED_PREF = "Data_Saved";

    private int adsCounter, songCounter;
    private boolean isMute, isKill;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();
        if(!pack.equals("com.spotify.music"))
            return;

        sharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        isMute = sharedPreferences.getBoolean("sw1", true);
        boolean isRootAccessGranted = RootUtil.isDeviceRooted();
        isKill = sharedPreferences.getBoolean("sw2", false) && isRootAccessGranted;
        adsCounter = sharedPreferences.getInt("adsCounter", 0);
        songCounter = sharedPreferences.getInt("songCounter", 0);
        boolean isAd = sbn.getNotification().actions.length == 3;

        String previousSong = sharedPreferences.getString("previousSong", "")
                , newSong;

        try {
            newSong = sbn.getNotification().extras.getCharSequence("android.text").toString();
        } catch (NullPointerException e){
            e.printStackTrace();
            return;
        }

        if (isKill && RootAccess.hasRootAccess()) {
            Toast.makeText(this, "Kill Option", Toast.LENGTH_SHORT).show();
            if (isAd) {
                //Kill the app
                Process suProcess = null;
                try {
                    suProcess = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                    os.writeBytes("adb shell" + "\n");
                    os.flush();
                    os.writeBytes("am force-stop com.spotify.music" + "\n");
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "E : " + e, Toast.LENGTH_SHORT).show();
                }

                //Not working
                /*ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                am.killBackgroundProcesses("com.spotify.music");*/

                //Restart it
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent playSpotify = new Intent("com.spotify.mobile.android.ui.widget.NEXT");
                        playSpotify.setPackage("com.spotify.music");
                        sendBroadcast(playSpotify);
                    }
                }, 1000);
            }
        }

        if (isMute) {
            Toast.makeText(this, "Mute Option", Toast.LENGTH_SHORT).show();
            if (newSong.equals(previousSong)) {
                Log.d("DDDD", "Equal");
                SharedPreferences.Editor editor = sharedPreferences.edit();     // play & Pause
                editor.putString("previousSong", previousSong);
                editor.apply();
            } else {
                Log.d("DDDD", "UnEqual");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("previousSong", newSong);                      // Next & Previous
                editor.putInt("songCounter", songCounter + 1);
                editor.apply();
            }

            if (isAd) {
                Toast.makeText(NotificationListenerService.this, "Ad Appeared", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("adsCounter", adsCounter + 1);
                editor.apply();
                Muter.mute(getApplicationContext());
            } else
                Muter.unMute(getApplicationContext());
        }
    }


}
