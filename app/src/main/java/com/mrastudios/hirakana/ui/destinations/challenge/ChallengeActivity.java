package com.mrastudios.hirakana.ui.destinations.challenge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.ui.destinations.main_title.MainActivity;
import com.mrastudios.hirakana.ui.services.BgmService;

import java.util.Date;

/**
 * An activity for challenging quizzes that is only available once a day.
 *
 * This activity should only be started by either {@link MainActivity} or
 * {@link com.mrastudios.hirakana.ui.receivers.ChallengeNotificationReceiver} through notifications
 * when tapped by the user.
 */
public final class ChallengeActivity extends AppCompatActivity
{
    public static final String MAIN_SHARED_PREF_NAME_KEY = "MainActivitySharedPrefName";
    public static final String CHALLENGE_DATE_MILLIS_PREF_KEY = "MainSharedPrefValueKey";

    private BgmService bgmService;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BgmService.BgmBinder binder = (BgmService.BgmBinder) service;
            bgmService = binder.getService();
            bgmService.startBgm(R.raw.bgm_challenge);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private boolean isReturningToMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        if(savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.getExtras() == null || intent.getExtras().isEmpty()) return;
            String prefName = intent.getStringExtra(MAIN_SHARED_PREF_NAME_KEY);
            String prefKeyToPutValue = intent.getStringExtra(CHALLENGE_DATE_MILLIS_PREF_KEY);
            SharedPreferences preferences = getSharedPreferences(prefName, MODE_PRIVATE);
            preferences.edit().putLong(prefKeyToPutValue, new Date().getTime()).apply();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BgmService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bgmService != null) bgmService.resumeBgm();
   }

    @Override
    protected void onPause() {
        super.onPause();
        if(bgmService != null && !isChangingConfigurations() && !isReturningToMain) bgmService.pauseBgm();
    }

    @Override
    protected void onDestroy() {
        if(bgmService != null && !isChangingConfigurations()) unbindService(connection);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //TODO: warn the user about the consequences of leaving the challenge which is once a day only
        if(isTaskRoot()) {
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            super.onBackPressed();
        }
        isReturningToMain = true;
    }
}
