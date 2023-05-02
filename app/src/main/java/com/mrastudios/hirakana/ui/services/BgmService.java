package com.mrastudios.hirakana.ui.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.mrastudios.hirakana.ui.destinations.main_title.MainActivity;

public final class BgmService extends Service
{
    private MediaPlayer bgmPlayer;
    private int currentMusicId = -1;

    private final IBinder binder = new BgmBinder();

    public class BgmBinder extends Binder
    {
        @NonNull
        public BgmService getService() { return BgmService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        Log.e(getClass().getSimpleName(), "BGM service is no longer running");
        releasePlayer();
    }

    private void releasePlayer() {
        if(bgmPlayer == null) return;
        if(bgmPlayer.isPlaying()) bgmPlayer.stop();

        bgmPlayer.reset();
        bgmPlayer.release();

        bgmPlayer = null;
        currentMusicId = -1;
    }

    /**
     * Starts the player only if {@link MainActivity#isBgmEnabled(Context)} returns true.
     */
    public void startBgm(@RawRes int audioRes) {
        if(!MainActivity.isBgmEnabled(BgmService.this) || bgmPlayer != null) return;

        bgmPlayer = MediaPlayer.create(this, audioRes);
        currentMusicId = audioRes;

        float volume = (float) (1 - (Math.log(100 - 40) / Math.log(100)));
        bgmPlayer.setVolume(volume, volume);
        bgmPlayer.setLooping(true);
        bgmPlayer.start();
    }

    public void changeBgm(@RawRes int audioRes) {
        if(bgmPlayer == null || audioRes == currentMusicId) return;
        releasePlayer();
        startBgm(audioRes);
    }

    public void pauseBgm() { if(bgmPlayer != null) bgmPlayer.pause(); }

    public void resumeBgm() {
        if(!MainActivity.isBgmEnabled(this) ||
                bgmPlayer == null || bgmPlayer.isPlaying()) return;
        bgmPlayer.start();
    }
}
