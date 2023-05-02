package com.mrastudios.hirakana.domain.view_related;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * A {@link View.OnTouchListener} that is used only for {@link android.widget.ImageView}s that either
 * have text centered horizontally or none. <br/> <br/>
 *
 * This class is used for views that uses images of 3D buttons that needs to swap to
 * pressed image or released image and play the appropriate sound effect when pressed/released.
 * This can be used without sound effects, however it is recommended to be used with
 * {@link SoundPool} set by using {@link #setSoundEffects(SoundPool, float, float, int, int)}.
 */
public final class CustomButtonOnTouchListener implements View.OnTouchListener
{
    private final int pressedDrawableRes;
    private final int releasedDrawableRes;
    private final float multiplier;

    private SoundPool soundPool;
    private float leftVolume;
    private float rightVolume;
    private int pressedSfxId;
    private int releasedSfxId;

    private boolean isActionCancelled;

    /**
     * @param pressedDrawableRes image to change to when pressed.
     * @param releasedDrawableRes image to change to when released.
     * @param multiplier the multiplier for the rising or falling effect of text. Must not be negative.
     */
    public CustomButtonOnTouchListener(@DrawableRes int pressedDrawableRes,
                                       @DrawableRes int releasedDrawableRes,
                                       float multiplier)
    {
        this.pressedDrawableRes = pressedDrawableRes;
        this.releasedDrawableRes = releasedDrawableRes;
        this.multiplier = multiplier;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, @NonNull MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        int textEffect = multiplier <= 0.0f ? 8 : (int) Math.ceil(8 * multiplier);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if(isActionCancelled) return true;
                if(isMotionEventOutsideView(view, motionEvent)) {
                    playSfx(releasedSfxId);
                    view.setPadding(0,0,0, textEffect);
                    view.setBackgroundResource(releasedDrawableRes);
                    isActionCancelled = true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if(isActionCancelled) {
                    isActionCancelled = false;
                    return true;
                }
                playSfx(releasedSfxId);
                view.setPadding(0,0,0, textEffect);
                view.setBackgroundResource(releasedDrawableRes);
                return false;
            case MotionEvent.ACTION_DOWN:
                playSfx(pressedSfxId);
                view.setPadding(0, textEffect,0,0);
                view.setBackgroundResource(pressedDrawableRes);
                return false;
            default:
                return false;
        }
    }

    private void playSfx(int sfxId) {
        if(soundPool != null) soundPool.play(sfxId, leftVolume > 1.0f ? 0.3f : leftVolume,
                rightVolume > 1.0f ? 0.3f : rightVolume, 1, 0, 1);
    }

    /**
     * @param soundPool the soundpool for the sfx
     * @param leftVolume volume on the left side (0.0 - 1.0)
     * @param rightVolume volume on the right side (0.0 - 1.0)
     * @param pressedSfxId the soundId to play when pressed that is returned by any of
     * {@link SoundPool} load methods
     * @param releasedSfxId the soundId to play when released that is returned by any of
     * {@link SoundPool} load methods
     */
    public void setSoundEffects(@NonNull SoundPool soundPool,
                                float leftVolume,
                                float rightVolume,
                                int pressedSfxId,
                                int releasedSfxId)
    {
        this.soundPool = soundPool;
        this.leftVolume = leftVolume;
        this.rightVolume = rightVolume;
        this.pressedSfxId = pressedSfxId;
        this.releasedSfxId = releasedSfxId;
    }

    public boolean isMotionEventOutsideView(@NonNull View touchedView,
                                            @NonNull MotionEvent motionEvent)
    {
        Rect viewRect = new Rect(touchedView.getLeft(), touchedView.getTop(),
                touchedView.getRight(), touchedView.getBottom());
        return !viewRect.contains(
                touchedView.getLeft() + (int) motionEvent.getX(),
                touchedView.getTop() + (int) motionEvent.getY());
    }
}
