package com.vishnu.sjcemap.miscellaneous;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

public class SoundNotify {
    public static void playGeoFenceBoundaryExceedAlert() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR);

        new Handler().postDelayed(toneGenerator::release, 500);
    }

    public static void playAuthSuccessAlert() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_SUP_PIP);

        new Handler().postDelayed(toneGenerator::release, 500);
    }

}
