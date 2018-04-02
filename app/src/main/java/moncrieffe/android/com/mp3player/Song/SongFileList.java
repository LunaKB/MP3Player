package moncrieffe.android.com.mp3player.Song;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class SongFileList {
    private List<SongFile> songFiles;

    public SongFileList()
    {
        songFiles = new ArrayList<>();
    }

    public void addSongFile(SongFile songFile)
    {
        songFiles.add(songFile);
    }

    public SongFile getSongFile(int position)
    {
        return songFiles.get(position);
    }

    public SongFile getSongFile(String uri)
    {
        for(SongFile sf: songFiles)
        {
            if(sf.GetSongUri().equals(Uri.parse(uri)))
                return sf;
        }
        return null;
    }

    public int getSongFilePos(SongFile songFile)
    {
        for(SongFile sf: songFiles)
        {
            if(sf.equals(songFile))
                return songFiles.indexOf(sf);
        }
        return -1;
    }

    public int getListLength()
    {
        return songFiles.size();
    }
}