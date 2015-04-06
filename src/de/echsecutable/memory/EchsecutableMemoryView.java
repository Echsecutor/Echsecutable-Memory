/**********************************************************************
 *
 * @file EchsecutableMemoryView.java
 *
 * @version 1.0.2015-04-04
 *
 *
 * @section DESCRIPTION
 * Main (and only) view of the echsecutable
 * Memory. It launches the game engine to draw in the provided surface.
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


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import android.view.MotionEvent;

import android.media.AudioManager;

import android.util.Log;

//Starts/pauses the engine, relays touch events
class EchsecutableMemoryView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "EchsecutableMemoryView";

    private Context context;

    private EchsecutableMemoryEngine engine=null;

    private SurfaceHolder holder;

    private Bundle savedInstanceForPause=null;

    public EchsecutableMemoryView(Context _context, Bundle savedInstance) {
        super(_context);
        context=_context;
        Log.v(TAG,"opening View...");
        // register our interest in hearing about changes to our surface
        holder = getHolder();
        holder.addCallback(this);
        checkEngine(savedInstance);
        setFocusable(true); // make sure we get key event
        Log.v(TAG,"done.");
    }

    /// create new engine if necessary
    public void checkEngine(Bundle savedInstance){
        if (engine==null){
            try{
                // create thread only; it's started in surfaceCreated()
                engine = new EchsecutableMemoryEngine(holder, context, savedInstance);
            }catch(Exception e){
                Log.wtf(TAG,"Initialisation failed! " +e);
                System.exit(1);
            }
        }

    }


    public void saveState(Bundle outState){
        engine.saveState(outState);
    }

    /*
      focus change => surfaceDestroyed first (!) but onSaveInstance
      not called... o0
    */
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus){
            Log.v(TAG,"pausing");
        }
    }




    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.v(TAG,"resize");
        engine.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        Log.v(TAG,"surfaceCreated. Launching enging...");
        checkEngine(savedInstanceForPause);
        engine.setRunning(true);
        engine.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

	//to be prepared for a resume after focus change:
        Log.v(TAG,"surface Destroyed. Saving state...");
        savedInstanceForPause = new Bundle();
        engine.saveState(savedInstanceForPause);

        Log.v(TAG,"Shutting down...");

        boolean retry = true;
        engine.setRunning(false);
        while (retry) {
            try {
                engine.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
        Log.v(TAG, "Engine joined.");

        engine=null;
        Log.v(TAG,"Clean shutdown.");
    }



    @Override
    public boolean onTouchEvent(MotionEvent e){
        engine.doTouch(e);
        return true;
    }
}
