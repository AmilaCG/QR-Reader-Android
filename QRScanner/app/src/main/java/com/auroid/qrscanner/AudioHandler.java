package com.auroid.qrscanner;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.auroid.qrscanner.settings.PreferenceUtils;

class AudioHandler {

    private SoundPool mSoundPool;
    private int mBeep;
    private Context mContext;

    public AudioHandler(Context mContext) {
        this.mContext = mContext;
    }

    public void setupAudioBeep() {
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
        if (PreferenceUtils.shouldPlayAudioBeep(mContext)) {
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
