package moncrieffe.android.com.mp3player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import moncrieffe.android.com.mp3player.Service.MP3Service;

public class MainActivity extends AppCompatActivity {

    private MP3Service mService;
    private boolean mServiceBound;
    private Intent mServiceIntent;
    private ServiceConnection mServiceConnection = createConnection();

    @Override
    protected void onStart() {
        super.onStart();
        // for media service
        if(mServiceIntent == null){
            mServiceIntent = new Intent(this, MP3Service.class);
            bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            startService(mServiceIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // for media service
        if (mServiceBound) {
            unbindService(mServiceConnection);
            //service is active
            mService.stopSelf();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // for media service
        savedInstanceState.putBoolean("ServiceState", mServiceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // for media service
        mServiceBound = savedInstanceState.getBoolean("ServiceState");
    }

    private ServiceConnection createConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                MP3Service.LocalBinder binder = (MP3Service.LocalBinder) service;
                mService = binder.getService();
                mServiceBound = true;
                // Initialize music ui here
                // set the music ui into the music player service thing here
                // set the current playlist here
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mServiceBound = false;
            }
        };
    }
}
