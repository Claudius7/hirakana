package com.mrastudios.hirakana.ui.destinations.main_title;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.data.User;
import com.mrastudios.hirakana.domain.view_related.CustomButtonOnTouchListener;
import com.mrastudios.hirakana.ui.destinations.challenge.ChallengeActivity;
import com.mrastudios.hirakana.ui.destinations.characters.CharactersActivity;
import com.mrastudios.hirakana.ui.destinations.main_title.dialogs.AppInfoDialog;
import com.mrastudios.hirakana.ui.destinations.quiz.QuizActivity;
import com.mrastudios.hirakana.ui.destinations.statistics.StatisticsActivity;
import com.mrastudios.hirakana.ui.receivers.ChallengeNotificationReceiver;
import com.mrastudios.hirakana.ui.services.BgmService;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public final class MainActivity extends AppCompatActivity
{
    public static final String REMINDERS_CHANNEL_ID = "Reminders Channel";

    private static final String SHARED_PREF_NAME = "MainActivitySharedPref777";
    private static final String KEY_PREF_IS_BGM_ENABLED = "UserWantsBGM?";
    private static final String KEY_PREF_LONG_CHALLENGE_DATE_MILLIS = "HasUserChallenged777";

    private BgmService bgmService;
    private SharedPreferences preferences;

    private SoundPool soundPool;
    private int pressedSfxId;
    private int releasedSfxId;

    private boolean isCurrentlyShowingChat;
    private boolean isMovingToAnotherActivity;
    private boolean isInformedBeforeLeaving;

    public final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BgmService.BgmBinder binder = (BgmService.BgmBinder) service;
            bgmService = binder.getService();
            bgmService.startBgm(R.raw.bgm_default);        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    public static boolean isBgmEnabled(@NonNull Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).getBoolean(
                KEY_PREF_IS_BGM_ENABLED, true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);

        createNotifChannels();
        startDailyChallengeNotif();
        initSfx();
        setupViews();

        if(savedInstanceState == null) showMascotChat(getString(R.string.mascot_startup_greetings));
    }

    private void createNotifChannels() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel remindersChannel = new NotificationChannel(REMINDERS_CHANNEL_ID,
                    getString(R.string.notification_channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT);
            remindersChannel.setDescription(getString(R.string.notification_channel_reminders_description));
            notificationManager.createNotificationChannel(remindersChannel);
        }
        notificationManager.cancel(ChallengeNotificationReceiver.NOTIF_ID);
    }

    private void startDailyChallengeNotif() {
        AlarmManager alarmManager = getSystemService(AlarmManager.class);
        Calendar challengeStart = Calendar.getInstance();
        challengeStart.setTime(new Date(System.currentTimeMillis()));
        challengeStart.add(Calendar.DAY_OF_YEAR, 1);
        challengeStart.set(Calendar.HOUR_OF_DAY, 8);
        challengeStart.set(Calendar.MINUTE, 0);
        challengeStart.set(Calendar.SECOND, 0);

        Intent i = new Intent(this, ChallengeNotificationReceiver.class);
        i.putExtra(ChallengeActivity.MAIN_SHARED_PREF_NAME_KEY, SHARED_PREF_NAME);
        i.putExtra(ChallengeActivity.CHALLENGE_DATE_MILLIS_PREF_KEY, KEY_PREF_LONG_CHALLENGE_DATE_MILLIS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 6969, i, Build.VERSION.SDK_INT == Build.VERSION_CODES.S ?
                        PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, challengeStart.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private boolean hasChallengeForToday() {
        Calendar today = Calendar.getInstance();
        if(today.get(Calendar.HOUR_OF_DAY) >= 8) {
            long challengeStart = preferences.getLong(KEY_PREF_LONG_CHALLENGE_DATE_MILLIS, -1);
            Calendar pastChallenge = Calendar.getInstance();
            pastChallenge.setTime(new Date(challengeStart));
            return challengeStart == -1 ||
                    (pastChallenge.get(Calendar.DAY_OF_YEAR) < today.get(Calendar.DAY_OF_YEAR));
        }
        return false;
    }

    private void initSfx() {
        AudioAttributes at = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(at)
                .setMaxStreams(3)
                .build();
        pressedSfxId = soundPool.load(this, R.raw.button_pressed, 1);
        releasedSfxId = soundPool.load(this, R.raw.button_released, 2);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        CustomButtonOnTouchListener customOnTouchListener = new CustomButtonOnTouchListener(
                R.drawable.button_main_pressed, R.drawable.button_main_unpressed, 0);
        customOnTouchListener.setSoundEffects(soundPool, 0.3f, 0.3f,
                pressedSfxId, releasedSfxId);
        ImageView mascot = findViewById(R.id.mascot);
        mascot.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_MOVE &&
                    customOnTouchListener.isMotionEventOutsideView(view, motionEvent))
            {
                view.setPadding(0,0,0, 0);
            }

            if(motionEvent.getAction() != MotionEvent.ACTION_UP) {
                view.setPadding(8,8,8,8);
            }
            return false;
        });
        mascot.setOnClickListener((view) -> {
            mascot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
            view.setPadding(0,0,0,0);

            if(!isCurrentlyShowingChat) {
                String[] mascotMessages = getResources().getStringArray(R.array.mascot_messages);
                int i = ThreadLocalRandom.current().nextInt(mascotMessages.length);
                showMascotChat(mascotMessages[i]);
            }
        });
        findViewById(R.id.chatText).setVisibility(View.INVISIBLE);
        findViewById(R.id.chatBubble).setVisibility(View.INVISIBLE);

        findViewById(R.id.toQuizButton).setOnTouchListener(customOnTouchListener);
        findViewById(R.id.toQuizButton).setOnClickListener(
                (v -> moveToActivity(QuizActivity.class)));

        findViewById(R.id.toStatisticButton).setOnTouchListener(customOnTouchListener);
        findViewById(R.id.toStatisticButton).setOnClickListener(
                (v -> moveToActivity(StatisticsActivity.class)));

        findViewById(R.id.toCharactersButton).setOnTouchListener(customOnTouchListener);
        findViewById(R.id.toCharactersButton).setOnClickListener(
                (v -> moveToActivity(CharactersActivity.class)));

        findViewById(R.id.button_about).setOnClickListener((view) -> {
            AppInfoDialog appInfoDialog = (AppInfoDialog) getSupportFragmentManager()
                    .findFragmentByTag(AppInfoDialog.TAG);
            if(appInfoDialog == null) appInfoDialog = new AppInfoDialog();
            if(!appInfoDialog.isAdded()) appInfoDialog.show(getSupportFragmentManager(), AppInfoDialog.TAG);
        });

        boolean isBgmEnabled = preferences.getBoolean(KEY_PREF_IS_BGM_ENABLED, true);
        ImageView bgmToggleButton = findViewById(R.id.button_bgm);
        bgmToggleButton.setImageResource(isBgmEnabled ?
                R.drawable.ic_main_bgm_on : R.drawable.ic_main_bgm_off);
        bgmToggleButton.setOnClickListener(v -> {
            boolean isEnabled = preferences.getBoolean(KEY_PREF_IS_BGM_ENABLED, true);
            bgmToggleButton.setImageResource(isEnabled ?
                    R.drawable.ic_main_bgm_off : R.drawable.ic_main_bgm_on);

            preferences.edit().putBoolean(KEY_PREF_IS_BGM_ENABLED, !isEnabled).apply();
            if (isEnabled) {
                bgmService.pauseBgm();
            } else {
                bgmService.startBgm(R.raw.bgm_default);
                bgmService.resumeBgm();
            }
        });

        if(hasChallengeForToday()) {
            View challengePopup = findViewById(R.id.challenge_popup);
            challengePopup.setOnClickListener(v -> {
                User.getInstance(this).addChallengeAttempt();
                preferences.edit().putLong(MainActivity.KEY_PREF_LONG_CHALLENGE_DATE_MILLIS,
                        new Date().getTime()).apply();
                moveToActivity(ChallengeActivity.class);

                v.clearAnimation();
                v.setVisibility(View.INVISIBLE);
            });

            challengePopup.setVisibility(View.VISIBLE);
            challengePopup.startAnimation(AnimationUtils.loadAnimation(
                    this, R.anim.zoom_in_and_out_infinite));
        }
    }

    private void showMascotChat(String message) {
        isCurrentlyShowingChat = true;
        TextView chatText = findViewById(R.id.chatText);
        ImageView chatBubble = findViewById(R.id.chatBubble);

        chatText.setVisibility(View.VISIBLE);
        chatBubble.setVisibility(View.VISIBLE);
        chatText.setText(message);

        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.zoom_in);
        chatText.startAnimation(animation);
        chatBubble.startAnimation(animation);

        new Handler().postDelayed(() -> {
            Animation exitAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            chatText.startAnimation(exitAnimation);
            chatBubble.startAnimation(exitAnimation);

            new Handler().postDelayed(() -> {
                isCurrentlyShowingChat = false;
                chatText.setVisibility(View.INVISIBLE);
                chatBubble.setVisibility(View.INVISIBLE);
            }, exitAnimation.getDuration());
        }, 7000);
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
        isMovingToAnotherActivity = false;

        if(bgmService != null) {
            bgmService.changeBgm(R.raw.bgm_default);
            bgmService.resumeBgm();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bgmService != null && !isChangingConfigurations() && !isMovingToAnotherActivity) {
            bgmService.pauseBgm();
        }
    }

    @Override
    protected void onDestroy() {
        if(!isChangingConfigurations()) unbindService(connection);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(!isInformedBeforeLeaving) {
            Toast.makeText(this, R.string.confirm_exit, Toast.LENGTH_SHORT).show();
            isInformedBeforeLeaving = true;
            new Handler().postDelayed(() -> isInformedBeforeLeaving = false, 2000);
            return;
        }
        super.onBackPressed();
    } 

    private void moveToActivity(Class<? extends AppCompatActivity> c) {
        if(isMovingToAnotherActivity) return;
        isMovingToAnotherActivity = true;

        if(!isBgmEnabled(this)) unbindService(connection);
        startActivity(new Intent(this, c));
    }
}