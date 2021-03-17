/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.bluetoothspp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.AsyncTask;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;
import android.os.Bundle;
import java.io.IOException;
import android.view.KeyEvent;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.bluetoothspp.bluetooth.BluetoothClientService;
import com.theta360.pluginapplication.task.EnableBluetoothClassicTask;
import com.theta360.pluginapplication.task.TakePictureTask;
import com.theta360.pluginapplication.task.TakePictureTask.Callback;
import com.theta360.pluginapplication.task.GetLiveViewTask;
import com.theta360.pluginapplication.task.MjisTimeOutTask;
import com.theta360.pluginapplication.view.MJpegInputStream;
import com.theta360.pluginapplication.oled.Oled;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class MainActivity extends PluginActivity implements ServiceConnection {
    private static final String TAG = "ExtendedPreview";

    //Button Resorce
    private boolean onKeyDownModeButton = false;
    private boolean onKeyLongPressWlan = false;
    private boolean onKeyLongPressFn = false;

    //Preview Resorce
    private int previewFormatNo;
    GetLiveViewTask mGetLiveViewTask;
    private byte[]		latestLvFrame;

    //Preview Timeout Resorce
    private static final long FRAME_READ_TIMEOUT_MSEC  = 1000;
    MjisTimeOutTask mTimeOutTask;
    MJpegInputStream mjis;

    //WebServer Resorce
    private Context context;
    private WebServer webServer;

    //OLED Dislay Resorce
    Oled oledDisplay = null;
    private boolean mFinished;

    //Attitude
    private SensorManager sensorManager;
    private Attitude attitude;


    private TakePictureTask.Callback mTakePictureTaskCallback = new Callback() {
        @Override
        public void onTakePicture(String fileUrl) {
            startPreview(mGetLiveViewTaskCallback, previewFormatNo);
        }
    };

    private boolean mServiceEnable = false;
    private EnableBluetoothClassicTask.Callback mEnableBluetoothClassicTask = new EnableBluetoothClassicTask.Callback() {
        @Override
        public void onEnableBluetoothClassic(String result) {
            if( result.equals("OK") ) {
                mServiceEnable = true;

                getApplicationContext()
                        .startService(
                                new Intent(getApplicationContext(), BluetoothClientService.class));

                getApplicationContext()
                        .bindService(
                                new Intent(getApplicationContext(), BluetoothClientService.class),
                                MainActivity.this,
                                Context.BIND_AUTO_CREATE);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new EnableBluetoothClassicTask(getApplicationContext(), mEnableBluetoothClassicTask).execute();

        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //init OLED
        oledDisplay = new Oled(getApplicationContext());
        oledDisplay.brightness(100);
        oledDisplay.clear(oledDisplay.black);
        oledDisplay.draw();

        //init Attitude
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        attitude = new Attitude(sensorManager);

        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyReceiver.KEYCODE_CAMERA :
                        stopPreview();
                        new TakePictureTask(mTakePictureTaskCallback).execute();
                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD :
                        // Disable onKeyUp of startup operation.
                        onKeyDownModeButton = true;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF :
                        if (onKeyLongPressWlan) {
                            onKeyLongPressWlan=false;
                        } else {
                            //WLAN短押し処理を書く

                        }

                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD :
                        if (onKeyDownModeButton) {
                            if (mGetLiveViewTask!=null) {
                                stopPreview();
                            } else {
                                startPreview(mGetLiveViewTaskCallback, previewFormatNo);
                            }
                            onKeyDownModeButton = false;
                        }
                        break;
                    case KeyEvent.KEYCODE_FUNCTION :
                        if (onKeyLongPressFn) {
                            onKeyLongPressFn=false;
                        } else {

                            //NOP : KEYCODE_FUNCTION

                        }

                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF:
                        onKeyLongPressWlan=true;

                        //NOP : KEYCODE_WLAN_ON_OFF

                        break;
                    case KeyEvent.KEYCODE_FUNCTION :
                        onKeyLongPressFn=true;

                        //NOP : KEYCODE_FUNCTION

                        break;
                    default:
                        break;
                }

            }
        });

        this.context = getApplicationContext();
        this.webServer = new WebServer(this.context, mWebServerCallback);
        try {
            this.webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isApConnected()) {

        }

        //Start LivePreview
        previewFormatNo = GetLiveViewTask.FORMAT_NO_640_30FPS;
        startPreview(mGetLiveViewTaskCallback, previewFormatNo);

        //Start OLED thread
        mFinished = false;
        drawOledThread();
    }

    @Override
    protected void onPause() {
        // Do end processing
        //close();

        //Stop Web server
        this.webServer.stop();

        //Stop LivePreview
        stopPreview();

        //Stop OLED thread
        mFinished = true;

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mServiceEnable) {
            getApplicationContext().unbindService(MainActivity.this);
            getApplicationContext().stopService(new Intent(getApplicationContext(), BluetoothClientService.class));
        }

        super.onDestroy();
        if (this.webServer != null) {
            this.webServer.stop();
        }
    }

    private Messenger _messenger;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "サービスに接続しました");
        _messenger = new Messenger(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "サービスから切断しました");
        _messenger = null;
    }

    private void startPreview(GetLiveViewTask.Callback callback, int formatNo){
        if (mGetLiveViewTask!=null) {
            stopPreview();

            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mGetLiveViewTask = new GetLiveViewTask(callback, formatNo);
        mGetLiveViewTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopPreview(){
        //At the intended stop, timeout monitoring also stops.
        if (mTimeOutTask!=null) {
            mTimeOutTask.cancel(false);
            mTimeOutTask=null;
        }

        if (mGetLiveViewTask!=null) {
            mGetLiveViewTask.cancel(false);
            mGetLiveViewTask = null;
        }
    }


    /**
     * GetLiveViewTask Callback.
     */
    private GetLiveViewTask.Callback mGetLiveViewTaskCallback = new GetLiveViewTask.Callback() {

        @Override
        public void onGetResorce(MJpegInputStream inMjis) {
            mjis = inMjis;
        }

        @Override
        public void onLivePreviewFrame(byte[] previewByteArray) {
            latestLvFrame = previewByteArray;

            //Update timeout monitor
            if (mTimeOutTask!=null) {
                mTimeOutTask.cancel(false);
                mTimeOutTask=null;
            }
            mTimeOutTask = new MjisTimeOutTask(mMjisTimeOutTaskCallback, FRAME_READ_TIMEOUT_MSEC);
            mTimeOutTask.execute();
        }

        @Override
        public void onCancelled(Boolean inTimeoutOccurred) {
            mGetLiveViewTask = null;
            latestLvFrame = null;

            if (inTimeoutOccurred) {
                startPreview(mGetLiveViewTaskCallback, previewFormatNo);
            }
        }

    };


    /**
     * MjisTimeOutTask Callback.
     */
    private MjisTimeOutTask.Callback mMjisTimeOutTaskCallback = new MjisTimeOutTask.Callback() {
        @Override
        public void onTimeoutExec(){
            if (mjis!=null) {
                try {
                    // Force an IOException to `mjis.readMJpegFrame()' in GetLiveViewTask()
                    mjis.close();
                } catch (IOException e) {
                    Log.d(TAG, "[timeout] mjis.close() IOException");
                    e.printStackTrace();
                }
                mjis=null;
            }
        }
    };

    /**
     * WebServer Callback.
     */
    private WebServer.Callback mWebServerCallback = new WebServer.Callback() {
        @Override
        public void execStartPreview(int format) {
            previewFormatNo = format;
            startPreview(mGetLiveViewTaskCallback, format);
        }

        @Override
        public void execStopPreview() {
            stopPreview();
        }

        @Override
        public boolean execGetPreviewStat() {
            if (mGetLiveViewTask==null) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public byte[] getLatestFrame() {
            return latestLvFrame;
        }
    };

    //==============================================================
    // OLED Thread
    //==============================================================
    public void drawOledThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int outFps=0;
                long startTime = System.currentTimeMillis();
                Bitmap beforeBmp = null;

                while (mFinished == false) {

                    byte[] jpegFrame = latestLvFrame;
                    if ( jpegFrame != null ) {

                        //JPEG -> Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrame, 0, jpegFrame.length);
                        //Resize Bitmap : width=128pix, height=64pix.
                        Bitmap smallBmp = Bitmap.createScaledBitmap(bitmap, 128,  64, true);

                        Bitmap resultBmp=null;
                        resultBmp=smallBmp;
                        try {
                            Thread.sleep(22);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //[Output to OLED]
                        // After creating the 128x24 image by cutting the top and bottom 20pix each, it is output.)
                        //-- The code in the comments below is slow. --
                        //oledDisplay.setBitmap(0,0, Oled.OLED_WIDTH, Oled.OLED_HEIGHT, 0, 20, 128, resultBmp);
                        //oledDisplay.draw();
                        //-- The following code is fast. That's because it doesn't process the brightness threshold. --
                        Bitmap cutBmp = Bitmap.createBitmap(resultBmp, 0, 20, 128, 24);
                        Intent imageIntent = new Intent("com.theta360.plugin.ACTION_OLED_IMAGE_SHOW");
                        imageIntent.putExtra("bitmap", cutBmp);
                        sendBroadcast(imageIntent);

                        // Get Attitude
                        double corrAzimath = attitude.getDegAzimath();
                        double corrPitch = attitude.getDegPitch();
                        double corrRoll = attitude.getDegRoll();

                        //Send Attitude
                        String csvAttitude = String.valueOf(corrAzimath) + ","
                                            + String.valueOf(corrPitch) + ","
                                            + String.valueOf(corrRoll)  + "\r\n" ;
                        //Log.d(TAG, "[csvAttitude]:" + csvAttitude  );

                        if (_messenger!=null) {
                            try {
                                Log.d(TAG, "[csvAttitude]:" + csvAttitude  );
                                _messenger.send(Message.obtain(null, 0, csvAttitude));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        outFps++;
                    } else {
                        try {
                            Thread.sleep(33);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    long curTime = System.currentTimeMillis();
                    long diffTime = curTime - startTime;
                    if (diffTime >= 1000 ) {
                        //Log.d(TAG, "[OLED]" + String.valueOf(outFps) + "[fps]" );
                        startTime = curTime;
                        outFps =0;
                    }

                }
            }
        }).start();
    }
}

