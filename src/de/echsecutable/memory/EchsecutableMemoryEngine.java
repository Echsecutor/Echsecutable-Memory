/**********************************************************************
*
* @file EchsecutableMemoryEngine.java
*
* @version 1.0.2015-04-04
*
*
* @section DESCRIPTION 
* This is the main file of this App. The game engine thread reacts on
* clicks, draws the game in the main loop and delegates audio to the
* audio thread.
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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import android.graphics.drawable.BitmapDrawable;

import android.view.MotionEvent;

import java.util.Random;
import java.util.ArrayList;


import java.lang.Exception;

import android.util.Log;
import android.graphics.Color;

class EchsecutableMemoryEngine extends Thread{

    private static final String TAG = "EchsecutableMemoryEngine";

    private SurfaceHolder mSurfaceHolder;

    private Context context;

    private boolean mRun = false;
    private final Object mRunLock = new Object();

    public final static int STATE_COVERED=0;
    public final static int STATE_ONE_UNCOVERED=1;
    public final static int STATE_TWO_UNCOVERED=2;
    public final static int STATE_WON=42;

    private int state=STATE_COVERED;

    private int hiddenPairs;

    private int indexField1=0;
    private int indexField2=0;

    Bitmap pic[];

    // matching[fieldId] = imageId
    private int matching[];

    private boolean covered[];

    private Resources res;

    private AudioThread at;

    private int imgnmbr;

    private int boardWidth;
private int boardHeight;
    private int bgColor;

    private Bitmap fieldBg;
    private int fieldSep;
    private int fieldWidth;
    private int fieldHeight;

    public EchsecutableMemoryEngine(SurfaceHolder surfaceHolder, Context _context, Bundle savedInstance) throws Exception{

        Log.v(TAG, "Constructing Engine...");

        context=_context;
        // get handles to some important objects
        mSurfaceHolder = surfaceHolder;

        res = context.getResources();
	
        boardWidth = res.getInteger(R.integer.board_width);
        boardHeight = res.getInteger(R.integer.board_height);

        if(boardWidth*boardHeight % 2 == 1){
            boardHeight--;
        }

        bgColor = res.getColor(R.color.bgColor);

        fieldBg = BitmapFactory.decodeResource(res,R.drawable.fieldbg);
        fieldSep = res.getInteger(R.integer.separation);

        matching = new int[boardWidth*boardHeight];
        covered = new boolean[boardWidth*boardHeight];

        //determine resource IDs of images
        int resID=0;

        imgnmbr = 0;
        ArrayList<Integer> images = new ArrayList<Integer>();

        Log.v(TAG, "Loading Images...");

        do {
            resID=res.getIdentifier("img_"+imgnmbr, "drawable", "de.echsecutable.memory");
            if (resID!=0){
                images.add(resID);
                imgnmbr++;
            }
        }while (resID!=0);
        if(imgnmbr<boardWidth*boardHeight/2){
            Log.e(TAG, "Not enough Images!");
            throw new Exception("Not enough images for this board size!");
        }
        Log.i(TAG,"Found " + imgnmbr + " images in current set.");

        pic=new Bitmap[imgnmbr];

        for(int i=0;i<imgnmbr;i++){
            pic[i]=BitmapFactory.decodeResource(res,images.get(i));
        }

        //new launch
        if(savedInstance==null){
            shuffle();
        }else{
            //recreating (e.g. due to orientation change)
            Log.v(TAG, "Loading saved instance...");
            hiddenPairs=savedInstance.getInt("hiddenPairs");
            covered=savedInstance.getBooleanArray("covered");
            matching=savedInstance.getIntArray("matching");
            state=savedInstance.getInt("state");
            indexField1=savedInstance.getInt("indexField1");
            indexField2=savedInstance.getInt("indexField2");

        }

        Log.v(TAG, "Constructed.");
    }


    //Save state to restore after orientation change, etc.
    public void saveState(Bundle outState){
        outState.putIntArray("matching",matching);
        outState.putBooleanArray("covered",covered);
        outState.putInt("hiddenPairs", hiddenPairs);
        outState.putInt("state",state);
        outState.putInt("indexField1",indexField1);
        outState.putInt("indexField2",indexField2);
    }

    public void shuffle(){
        Log.v(TAG, "Shuffling...");
        hiddenPairs = boardWidth * boardHeight/2;

        //shuffle memory cards
        Random rnd = new Random();
        for(int i=0;i<boardWidth * boardHeight;i++){
            matching[i]=-1;
            covered[i]=true;
        }
        int offset = 0;
        if(imgnmbr>boardWidth * boardHeight/2){
            offset = rnd.nextInt(imgnmbr-boardWidth * boardHeight/2);
        }

        // matching[fieldId] = imageId
        for(int i=0;i<boardWidth * boardHeight;i++){
            int x=rnd.nextInt(boardWidth * boardHeight);
            while(matching[x]!=-1){
                x=rnd.nextInt(boardWidth * boardHeight);
            }
            matching[x]=(int)(i/2) + offset;
        }
        Log.v(TAG, "Shuffled...");
    }

    public void setRunning(boolean b) {
        // Do not allow mRun to be modified while any canvas operations
        // are potentially in-flight. See doDraw().
        synchronized (mRunLock) {
            mRun = b;
        }
    }


    public void doTouch(MotionEvent e){
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            Log.v(TAG,"Pointer up received.");

	    int fieldId = (int)(x/fieldWidth) + (int)(y/fieldHeight) * boardWidth;

            try{
                //disregard clicks on uncovered cards
                if((state==STATE_COVERED||state==STATE_ONE_UNCOVERED) && !covered[fieldId]){
                    Log.v(TAG,"Card already uncovered.");
                    return;
                }

                Log.v(TAG,"Card clickt...");


                switch(state){
                case STATE_COVERED:
                    indexField1= fieldId;
		    covered[indexField1]=false;
                    state=STATE_ONE_UNCOVERED;
                    Log.v(TAG,"First uncovered.");
                    break;
                case STATE_ONE_UNCOVERED:
                    indexField2=fieldId;
                    covered[indexField2]=false;
                    BitmapDrawable tmp;
                    if(matching[indexField1]!=matching[indexField2]){
                        state=STATE_TWO_UNCOVERED;
                        playNoPairSound();
                        Log.v(TAG,"No match.");
                    }else{
                        //found pair
                        Log.v(TAG,"Found pair!");
                        playFoundPairSound();

                        hiddenPairs --;
                        if(hiddenPairs ==0){
                            //found all
                            Log.v(TAG,"Found all!");

                            state=STATE_WON;
                        }else{
                            Log.v(TAG,hiddenPairs + " remaining.");
                            state=STATE_COVERED;
                        }
                    }
                    break;
                case STATE_TWO_UNCOVERED:
                    Log.v(TAG, "Covering.");
                    covered[indexField1]=true;
                    covered[indexField2]=true;
                    state=STATE_COVERED;
                    Log.v(TAG, "Re-click to avoid 'lag'.");
		    doTouch(MotionEvent e);
                    break;

                case STATE_WON:
                    Log.v(TAG, "Game reset.");
                    shuffle();
                    state=STATE_COVERED;
                    break;
                }
            }catch (Exception ex){
                //ignore this touch event
            }
        }
    }


    /* Callback invoked when the surface dimensions change. */
    public void setSurfaceSize(int width, int height) {
        // synchronized to make sure these all change atomically
        synchronized (mSurfaceHolder) {
	    fieldWidth = width/boardWidth;
	    fieldHeight = height/boardHeight;
        }
    }

    //Running animations (flipping stuff, delayed stuff,etc.)
    public void animate(){

    }



    public void doDraw(Canvas canvas){
        canvas.drawColor(bgColor);

        for(int x=0;x<boardWidth;x++){
            for(int y=0;y<boardHeight;y++){
                int fieldId=y*boardWidth+x;
                Bitmap img=fieldBg;
                if(!covered[fieldId]){
                    // matching[fieldId] = imageId
                    img = pic[matching[fieldId]];
                }
                canvas.drawBitmap(img,null, new Rect(x * fieldWidth + fieldSep, y* fieldHeight + fieldSep,(x+1) * fieldWidth-fieldSep, (y+1)* fieldHeight-fieldSep),null);
            }
        }
    }



    @Override
    public void run() {
        Log.v(TAG, "Engine thread launched. Creating Audio Thread...");

        //start my audio thread:
        at = new AudioThread(context);
        at.start();

        Log.v(TAG, "Entering main Loop.");

        //main loop
        while (mRun) {
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas(null);
                synchronized (mSurfaceHolder) {
                    animate();
                    // Critical section. Do not allow mRun to be set false until
                    // we are sure all canvas draw operations are complete.
                    //
                    // If mRun has been toggled false, inhibit canvas operations.
                    synchronized (mRunLock) {
                        if (mRun) doDraw(c);
                    }
                }
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }

        Log.v(TAG, "Leaving main loop. Cleaning up...");

        //finish him!
        at.getHandler().sendEmptyMessage(AudioThread.MSG_STOP_THREAD);
        boolean retry = true;
        while (retry) {
            try {
                at.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
        Log.v(TAG, "Audio thread joined.");

    }//end run()



    public void playFoundPairSound(){
        Log.v(TAG, "Play found pair sound!");
        Message msg = Message.obtain();
        msg.what=AudioThread.MSG_PLAY_SOUND;
        msg.arg1 = R.raw.foundpair;

        //Handler handels synchro...?
        at.getHandler().sendMessage(msg);

    }

    public void playNoPairSound(){
        Log.v(TAG, "Play no pair sound!");
        Message msg = Message.obtain();
        msg.what=AudioThread.MSG_PLAY_SOUND;
        msg.arg1 = R.raw.nopair;
        at.getHandler().sendMessage(msg);
    }


}//end thread
