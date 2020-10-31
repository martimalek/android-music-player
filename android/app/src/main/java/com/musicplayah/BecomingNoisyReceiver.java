package com.musicplayah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

public class BecomingNoisyReceiver extends BroadcastReceiver {
    private Runnable onNoisyReceivedRunnable;

    public static IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    BecomingNoisyReceiver(Runnable onNoisyReceived) {
        this.onNoisyReceivedRunnable = onNoisyReceived;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) this.onNoisyReceivedRunnable.run();
    }
}
