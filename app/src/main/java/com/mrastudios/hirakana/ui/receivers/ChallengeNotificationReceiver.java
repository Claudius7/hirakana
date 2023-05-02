package com.mrastudios.hirakana.ui.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.ui.destinations.challenge.ChallengeActivity;
import com.mrastudios.hirakana.ui.destinations.main_title.MainActivity;

import java.util.concurrent.ThreadLocalRandom;

public final class ChallengeNotificationReceiver extends BroadcastReceiver
{
    public static final int NOTIF_ID = 69;

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String[] messages = context.getResources().getStringArray(
                R.array.notification_daily_challenge_messages);
        int random = ThreadLocalRandom.current().nextInt(messages.length);
        String notifBody = messages[random];
        String notifTitle = context.getResources().getString(R.string.notification_daily_challenge_title);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, ChallengeActivity.class);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                MainActivity.REMINDERS_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_translate)
                .setContentTitle(notifTitle)
                .setContentText(notifBody)
                .setContentIntent(PendingIntent.getActivity(context,
                        143, intent, PendingIntent.FLAG_IMMUTABLE));
//                .addAction(R.drawable.ic_alarm_set,
//                        context.getString(R.string.notification_action_dismiss),
//                        cancelAlarmIntent);

        NotificationManagerCompat notifManagerCompat = NotificationManagerCompat.from(context);
        notifManagerCompat.notify(NOTIF_ID, builder.build());
    }
}