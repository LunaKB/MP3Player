package moncrieffe.android.com.mp3player.Song;


import android.net.Uri;

public class SongFile {

    private Uri songUri;

    public SongFile(Uri uri)
    {
        songUri = uri;
    }

    public Uri GetSongUri() {
        return songUri;
    }
}
