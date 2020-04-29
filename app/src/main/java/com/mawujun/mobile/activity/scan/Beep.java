package com.mawujun.mobile.activity.scan;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.mawujun.mobile.R;


public class Beep {

    public static final String TAG = Beep.class.getSimpleName();

    private Context mContext = null;

    private SoundPool soundPool = null;

    private float volume;

    /*声音是否打开*/
    private boolean beepEnable = true;

    public Beep(Context context) {
        this.mContext = context;
        ((Activity)mContext).setVolumeControlStream(AudioManager.STREAM_MUSIC);

    }

    public void play() {
        soundPool.load(mContext, R.raw.beep, 1);
    }

    public void initBeepPlayer() {
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(sampleId, volume, volume, 1, 0, 1f);
            }
        });
        AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = streamVolumeCurrent / streamVolumeMax;
    }
}
