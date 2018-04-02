package moncrieffe.android.com.mp3player.UI;

/**
 * Created by Student on 4/2/2018.
 */

public interface MediaPlayerControl {
    void    start();
    void    pause();
    int     getDuration();
    int     getCurrentPosition();
    void    seekTo(int pos);
    boolean isPlaying();
}
