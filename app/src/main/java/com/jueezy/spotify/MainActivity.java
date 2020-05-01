package com.jueezy.spotify;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static final String SHARED_PREF = "Data_Saved";
    private static final String ADS_COUNTER = "Ads_Counter";
    BroadcastReceiver mReceiver;
    IntentFilter filter;
    TextView rootDescription, adsBlocker, songCounter;
    LinearLayout leftLayout, rightLayout;
    SharedPreferences sharedPreferences;
    View view;
    Switch sw1, sw2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootDescription = (TextView) findViewById(R.id.root_description);
        adsBlocker = (TextView) findViewById(R.id.ads_block);
        songCounter = (TextView) findViewById(R.id.song_counter);
        leftLayout = (LinearLayout) findViewById(R.id.left_layout);
        rightLayout = (LinearLayout) findViewById(R.id.right_layout);
        view = (View) findViewById(R.id.view);
        sw1 = (Switch) findViewById(R.id.muter);
        sw2 = (Switch) findViewById(R.id.killer);

        sharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            adsBlocker.setText("Ads blocked :- " + (sharedPreferences.getInt("adsCounter", 0))/2);
            songCounter.setText("Song Count :- " + sharedPreferences.getInt("songCounter", 0));
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);


        startService(new Intent(this, NotificationCollectorMonitorService.class));


        if (RootUtil.isDeviceRooted()) {
            Toast.makeText(this, "Your Decice is Rooted", LENGTH_LONG).show();
            rootDescription.setText("Your Device is Rooted");
        } else {
            Toast.makeText(this, "Your Decice is Non-Rooted", LENGTH_LONG).show();
            rootDescription.setText("Your Device is Non-Rooted");
            rightLayout.setVisibility(View.GONE);     // If  Device is Non Rooted only One option Shown.
            view.setVisibility(View.GONE);
        }

        sw1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("sw1", true);
                    editor.apply();
                    sw1.setChecked(true);
                    Toast.makeText(MainActivity.this, "Muter On", Toast.LENGTH_SHORT).show();
                    if (sw2.isChecked()) {
                        sw2.setChecked(false);
                    }
                } else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("sw1", false);
                    editor.apply();
                    sw1.setChecked(false);
                }
                if (!isNotificationServiceEnabled()) {
                    startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                }
            }
        });

        sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (RootAccess.hasRootAccess()){
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("sw2", true);
                        editor.apply();
                        sw2.setChecked(true);
                        Toast.makeText(MainActivity.this, "Killer On", Toast.LENGTH_SHORT).show();
                        if (sw1.isChecked()) {
                            sw1.setChecked(false);
                        }
                    } else {
                        sw2.setChecked(false);
                    }

                } else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("sw2", false);
                    editor.apply();
                    sw2.setChecked(false);
                }
                if (!isNotificationServiceEnabled()) {
                    startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                }
            }
        });
        
       registerReceiver();
    }

    // Functions :------------------

    private void registerReceiver(){
        filter = new IntentFilter();
        filter.addAction("com.spotify.music.playbackstatechanged");
        filter.addAction("com.spotify.music.metadatachanged");
        filter.addAction("com.spotify.music.queuechanged");

            mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();

                String trackId;
                trackId = sharedPreferences.getString("id", "lets see");
                Log.d("DDD", "MA1 " + trackId );
                //Toast.makeText(MainActivity.this, "out " + trackId , Toast.LENGTH_SHORT).show();
                if (trackId == null){
                    trackId = intent.getStringExtra("id");
                    Log.d("DDD", "MA2 " + trackId);
                    SharedPreferences.Editor editor1 = sharedPreferences.edit();
                    editor1.putString("id", trackId);
                    editor1.apply();
                    trackId = sharedPreferences.getString("id", "---");
                    /*Toast.makeText(MainActivity.this, "In If " + trackId, Toast.LENGTH_SHORT).show();*/
                }
                trackId = intent.getStringExtra("id");
                /*Log.d("DDD", "MAB " + trackId);*/
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("id", trackId);
                editor.apply();

                if (trackId != null){
                    Log.d("DD", "MA ping");
                    NotificationListenerService.pingMe(getApplicationContext());
                }
            }
        };
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        sw1.setChecked(sharedPreferences.getBoolean("sw1", false));
        sw2.setChecked(sharedPreferences.getBoolean("sw2", false));
        if (sw1.isChecked()) {
            sw1.setChecked(sharedPreferences.getBoolean("sw1", true));
            sw2.setChecked(sharedPreferences.getBoolean("sw2", false));
        }
        if (sw2.isChecked()) {
            sw2.setChecked(sharedPreferences.getBoolean("sw2", true));
            sw1.setChecked(sharedPreferences.getBoolean("sw1", false));
        }
        if (!isNotificationServiceEnabled()) {
            sw1.setChecked(false);
            sw2.setChecked(false);
        }
        adsBlocker.setText("Ads blocked :- " + (sharedPreferences.getInt("adsCounter", 0))/2);
        songCounter.setText("Song Count :- " + sharedPreferences.getInt("songCounter", 0));

        registerReceiver(mReceiver, filter);
    }



    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    };



    @Override
    protected void onPause() {
        super.onPause();
        Log.d("DDD", "onPause " + mReceiver);
        //unregisterReceiver(mReceiver);
    }
}
