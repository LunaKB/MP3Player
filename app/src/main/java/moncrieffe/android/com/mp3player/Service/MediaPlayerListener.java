package moncrieffe.android.com.mp3player.Service;

/**
 * Created by Student on 4/2/2018.
 */

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import moncrieffe.android.com.mp3player.UI.MP3Controller;

class MediaPlayerListener implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener{
    private MP3Controller mController;
    private Context mContext;

    public MediaPlayerListener(MP3Controller controller, Context context){
        mController = controller;
        mContext = context;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if(!mediaPlayer.isPlaying())
            mediaPlayer.start();

        mController.show(0);

        Intent broadcastIntent = new Intent(mContext, MP3Service.class);
        broadcastIntent.setAction(MP3Service.BROADCAST_AUDIO_PREPARED);
        mContext.sendBroadcast(broadcastIntent);
    }
}
