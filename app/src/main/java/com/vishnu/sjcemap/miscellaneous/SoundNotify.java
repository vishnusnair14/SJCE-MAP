package com.vishnu.sjcemap.miscellaneous;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

public class SoundNotify {
    public static void playGeoFenceBoundaryExceedAlert() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT);

        new Handler().postDelayed(toneGenerator::release, 500);
    }

    public static void playQRScanSuccessAlert() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK);

        new Handler().postDelayed(toneGenerator::release, 500);
    }

}
