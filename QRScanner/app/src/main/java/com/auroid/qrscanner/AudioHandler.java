package com.auroid.qrscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.preference.PreferenceManager;

class AudioHandler {

    private SoundPool mSoundPool;
    private int mBeep;
    private Context mContext;
    private SharedPreferences mSharedPrefs;

    public AudioHandler(Context mContext) {
        this.mContext = mContext;
    }

    public void setupAudioBeep() {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        mBeep = mSoundPool.load(mContext, R.raw.beep, 1);
    }

    public void playAudioBeep() {
        if (mSharedPrefs.getBoolean("audio_beep", true)) {
            mSoundPool.play(mBeep, 1, 1, 0, 0, 1);
        }
    }

    public void release() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }
}
