/**********************************************************************
*
* @file EchsecutableMemory.java
*
* @version 1.0.2015-04-04
*
*
* @section DESCRIPTION
* Main (and only) activity of the echsecutable Memory. Does nothing
* but opening the full screen view.
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

import android.app.Activity;
import android.os.Bundle;
import android.media.AudioManager;


import android.util.Log;

import android.content.res.Configuration;

//just loads EchsecutableMemoryView
public class EchsecutableMemory extends Activity
{
    private static final String TAG = "EchsecutableMemory";

    private EchsecutableMemoryView myView;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	Log.v(TAG,"Creating EchsecutbaleMemory...");
	setVolumeControlStream(AudioManager.STREAM_MUSIC);

	myView = new EchsecutableMemoryView(this, savedInstanceState);
        setContentView(myView);

	Log.v(TAG,"EchsecutbaleMemory created");
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
	myView.saveState(outState);
        Log.v(TAG, "Saved state.");
    }

}
