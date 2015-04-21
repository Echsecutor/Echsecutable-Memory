/**********************************************************************
*
* @file AudioThread.java
*
* @version 1.0.2015-04-04
*
*
* @section DESCRIPTION 
* This is the audio thread. ;)
*
* @copyright 2015 Sebastian Schmittner <sebastian@schmittner.pw>
*
* @section LICENSE
* This file is part of the echsecutable Memory App.
*
* The echsecutable Memory App is free software: you can redistribute
* it and/or modify it under the terms of the GNU General Public
* License as published by the Free Software Foundation, either version
* 3 of the License, or (at your option) any later version.
*
* The echsecutable Memory App is distributed in the hope that it will
* be useful, but WITHOUT ANY WARRANTY; without even the implied
* warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with the echsecutable Memory App.  If not, see
* <http://www.gnu.org/licenses/>.
*
***********************************************************************/

package de.echsecutable.memory;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.content.Context;

import android.os.Handler;
import android.os.Handler.Callback;

import android.os.Handler;
import android.os.Message;

import android.os.Looper;

import android.util.Log;

import android.os.Build.VERSION;

class AudioThread extends Thread implements OnAudioFocusChangeListener, OnCompletionListener, Handler.Callback{

    private static final String TAG = "AudioThread";

    public static final int MSG_STOP_THREAD=0;
    public static final int MSG_PLAY_SOUND=1;


    private AudioManager am;
    private MediaPlayer mediaPlayer;
    private Context context;

    private boolean running=false;

    private Handler handler;

    public AudioThread(Context _context){
	Log.v(TAG, "Constructing...");

        context=_context;
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	Log.v(TAG, "Constructed.");

    }

    public Handler getHandler(){
	return handler;
    }

    public int getAPIlevel(){
	int re=Integer.parseInt(VERSION.SDK);
	if(re>=4)
	    return VERSION.SDK_INT;
	return re;
    }

    @Override
    public boolean handleMessage(Message msg) {

	Log.v(TAG, "Received msg...");

        switch(msg.what){
        case MSG_STOP_THREAD:
	    Log.v(TAG, "Stopping loop...");
	    //quitSafely is only avaiable from API level 18 on
	    if(getAPIlevel()>=18){
		handler.getLooper().quitSafely();
	    }else{
		handler.getLooper().quit();
	    }
	    return true;
        case MSG_PLAY_SOUND:
	    Log.v(TAG, "Playing sound...");
            playSound(msg.arg1);
	    return true;
        default:
	    Log.e(TAG,"Message type unknown.");
	    return false;
        }


    }

    public void playSound(int resID){
        //transient duck focus for a quick jingle
	Log.v(TAG,"Requesting Audio Focus...");
        int result = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
	    Log.v(TAG,"Got focus. Playing sound...");
            mediaPlayer = MediaPlayer.create(context, resID);
            if (mediaPlayer==null){
		Log.e(TAG,"Failed to load sound file.");
            }else{
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.start();
            }

        }

    }

    @Override
    public void onCompletion (MediaPlayer mp){
	Log.v(TAG,"Sound Finished. Releasing...");
        mp.release();
        mediaPlayer=null;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
	Log.v(TAG, "Audio Focus change.");
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
            // Pause playback

        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            // Raise it back to normal volume
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if(mediaPlayer != null){
		Log.v(TAG, "Stop Player...");
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer=null;
                }
                am.abandonAudioFocus(this);
            }else  if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
            }

        }

    }

    @Override
    public void run(){
	Log.v(TAG,"entering msg loop...");

        Looper.prepare();
        handler = new Handler(this);

        Looper.loop();

    }//end run()


}
