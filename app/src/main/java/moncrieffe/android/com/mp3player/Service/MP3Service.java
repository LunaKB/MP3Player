package moncrieffe.android.com.mp3player.Service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import moncrieffe.android.com.mp3player.R;
import moncrieffe.android.com.mp3player.Song.SongFile;
import moncrieffe.android.com.mp3player.Song.SongFileList;
import moncrieffe.android.com.mp3player.UI.MP3Controller;
import moncrieffe.android.com.mp3player.UI.MediaPlayerControl;

public class MP3Service extends Service
        implements MediaPlayerControl,
        AudioManager.OnAudioFocusChangeListener{

    static final String ACTION_PLAY = "moncrieffe.android.com.mp3player.Service.ACTION_PLAY";
    static final String ACTION_PAUSE = "moncrieffe.android.com.mp3player.Service.ACTION_PAUSE";
    static final String ACTION_PREVIOUS = "moncrieffe.android.com.mp3player.Service.ACTION_PREVIOUS";
    static final String ACTION_NEXT = "moncrieffe.android.com.mp3player.Service.ACTION_NEXT";
    static final String ACTION_STOP = "moncrieffe.android.com.mp3player.Service.ACTION_STOP";
    static final String BROADCAST_AUDIO_PREPARED = "moncrieffe.android.com.mp3player.Service.AUDIO_PREPARED";
    static final int NOTIFICATION_ID = 101;

    enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    private MediaPlayer mPlayer;
    private Context mContext;
    private SongFile mSongFile;
    private SongFileList mSongFileList;
    private int mSongIndex;

    private final IBinder mBinder = new LocalBinder(this);
    private AudioManager mAudioManager;
    private int mResumePosition;

    private MediaSessionManager mSessionManager;
    private MediaSessionCompat mSession;
    private MediaControllerCompat.TransportControls mControls;

    private MediaPlayerListener mListener;
    private MediaPlayerBroadcastReceiver mBroadCastReceiver = new MediaPlayerBroadcastReceiver(this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        requestAudioFocus();
        registerBecomingNoisyReceiver();
        registerOnAudioPreparedReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPlayer != null)
        {
            stopMedia();
            shutDownSongPlayer();
        }
        removeAudioFocus();
        removeNotification();
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(onAudioPreparedReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void start() {
        mPlayer.start();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mPlayer == null) initMediaPlayer();
                else if (!mPlayer.isPlaying()) mPlayer.start();
                mPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mPlayer.isPlaying()) mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mPlayer.isPlaying()) mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mPlayer.isPlaying()) mPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public MP3Service()
    {
        initMediaPlayer();
    }

    public void shutDownSongPlayer()
    {
        mPlayer.release();
        mPlayer = null;
    }

    public void restartSongPlayer()
    {
        shutDownSongPlayer();
        initMediaPlayer();
    }

    private void initMediaPlayer()
    {
        // initialize
        mPlayer = new MediaPlayer();
        mPlayer.reset();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    // call after construction and restart
    public void setServicePlayerListeners(MP3Controller controller, Context context)
    {
        mListener = new MediaPlayerListener(controller, context);

        // set listeners
        mPlayer.setOnCompletionListener(mListener);
        mPlayer.setOnPreparedListener(mListener);
    }

    public void SetList(SongFileList fileList)
    {
        mSongFileList = fileList;
    }

    public void setSong(SongFile song, Context context)
    {
        mSongFile = song;
        mSongIndex = mSongFileList.getSongFilePos(mSongFile);

        mContext = context;
    }

    public void playSong() throws Exception
    {
        mPlayer.reset();
        mPlayer.setDataSource(mContext, mSongFile.GetSongUri());
        mPlayer.prepare();

    }

    private void playMedia(){
        if(!mPlayer.isPlaying())
            mPlayer.start();
    }

    void pauseMedia(){
        if(mPlayer == null) return;
        if(mPlayer.isPlaying()) {
            mPlayer.pause();
            mResumePosition = mPlayer.getCurrentPosition();
        }
    }

    private void stopMedia(){
        if(mPlayer == null) return;
        if(mPlayer.isPlaying()) {
            mPlayer.stop();
        }
    }

    private void resumeMedia() {
        if (!mPlayer.isPlaying()){
            mPlayer.seekTo(mResumePosition);
            mPlayer.start();
        }
    }

    private void skipToNext() {
        if (mSongIndex == mSongFileList.getListLength() - 1) {
            //if last in playlist
            mSongIndex = 0;
            mSongFile = mSongFileList.getSongFile(mSongIndex);
        } else {
            //get next in playlist
            mSongFile = mSongFileList.getSongFile(++mSongIndex);
        }

        stopMedia();
        //reset mediaPlayer
        mPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {
        if (mSongIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            mSongIndex = mSongFileList.getListLength() - 1;
            mSongFile = mSongFileList.getSongFile(mSongIndex);
        } else {
            //get previous in playlist
            mSongFile = mSongFileList.getSongFile(--mSongIndex);
        }

        stopMedia();
        //reset mediaPlayer
        mPlayer.reset();
        initMediaPlayer();
    }

    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.abandonAudioFocus(this);
    }

    private BroadcastReceiver becomingNoisyReceiver = mBroadCastReceiver.onNoisyBroadcastReceiver();
    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void initMediaSession() throws RemoteException {
        if (mSessionManager != null) return;

        mSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        mControls = mSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    void updateMetaData() {
        if(mSongFile != null) {
            mSession.setMetadata(new MediaMetadataCompat.Builder()
                /*.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())*/
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mSongFile.GetSongUri().toString())
                    .build());
        }
    }
    // Media session end

    void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        //Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
        //        R.drawable.image); //replace with your own image

        // Create a new Notification
        if (mSongFile!=null) {
            NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                    .setShowWhen(false)
                    // Set the Notification style
                    .setStyle(new NotificationCompat.MediaStyle()
                            // Attach our MediaSession token
                            .setMediaSession(mSession.getSessionToken())
                            // Show our playback controls in the compact notification view.
                            .setShowActionsInCompactView(0, 1, 2))
                    // Set the Notification color
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    // Set the large and small icons
                    //.setLargeIcon(largeIcon)
                    .setSmallIcon(android.R.drawable.stat_sys_headset)
                    // Set Notification content information
                    .setContentText(mSongFile.GetSongUri().toString())
                    .setContentTitle(mSongFile.GetSongUri().toString())
                    .setContentInfo(mSongFile.GetSongUri().toString())
                    // Add playback actions
                    .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                    .addAction(notificationAction, "pause", play_pauseAction)
                    .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MP3Service.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                PendingIntent i = PendingIntent.getService(this, actionNumber, playbackAction, 0);
                return i;
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            mControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            mControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            mControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            mControls.stop();
        }
    }

    private BroadcastReceiver onAudioPreparedReceiver = mBroadCastReceiver.onAudioPreparedReceiver();
    private void registerOnAudioPreparedReceiver () {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(BROADCAST_AUDIO_PREPARED);
        registerReceiver(onAudioPreparedReceiver, filter);
    }
}
