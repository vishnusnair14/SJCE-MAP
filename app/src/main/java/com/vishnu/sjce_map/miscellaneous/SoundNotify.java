package com.vishnu.sjce_map.miscellaneous;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

public class SoundNotify {
    public static void playGeoFenceBoundaryExceedNotify() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT);

        new Handler().postDelayed(toneGenerator::release, 500);
    }
}
