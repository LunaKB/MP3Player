package moncrieffe.android.com.mp3player.Service;

import android.os.Binder;

/**
 * Created by Student on 4/2/2018.
 */

public class LocalBinder extends Binder {
    private MP3Service mService;

    public LocalBinder(MP3Service service){
        mService = service;
    }

    public MP3Service getService(){
        return mService;
    }
}
