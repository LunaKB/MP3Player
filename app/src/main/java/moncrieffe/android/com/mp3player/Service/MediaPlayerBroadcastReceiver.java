package moncrieffe.android.com.mp3player.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Student on 4/2/2018.
 */

class MediaPlayerBroadcastReceiver {
    private MP3Service mService;

    public MediaPlayerBroadcastReceiver(MP3Service service) {
        mService = service;
    }

    public BroadcastReceiver onNoisyBroadcastReceiver(){
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //pause audio on ACTION_AUDIO_BECOMING_NOISY
                mService.pauseMedia();
                mService.buildNotification(MP3Service.PlaybackStatus.PAUSED);
            }
        };
    }

    public BroadcastReceiver onAudioPreparedReceiver(){
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mService.updateMetaData();
                mService.buildNotification(MP3Service.PlaybackStatus.PLAYING);
            }
        };
    }
}
