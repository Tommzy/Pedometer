package com.pedometer.tommzy.pedometer.services;
/**
 * SleepDetectService.java
 * Pedometer
 *
 * @version 1.0.1
 *
 * @author Hui Zheng
 *
 * Copyright (c) 2014, 2015. Pedometer App. All Rights Reserved.
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import Util.Utils;


public class SleepDetectService extends Service {
    private static final int SAMPLE_RATE = 5000;
    private static final String TAG = "SleepDetectService";

    private MediaRecorder mRecorder;
    private SensorManager sensorMgr = null;
    private Sensor lightSensor = null;
    private float lightIntensity;
    private Timer timer;

    @Override
    public IBinder onBind(Intent intent) {
        throw null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "SleepService onCreate");

        // Create the calibrateTimer
        timer = new Timer();
        // Set up light sensor
        sensorMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        lightSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Set up media recorder
        mRecorder = new MediaRecorder();
        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }catch (IllegalStateException e){
            Log.i(TAG, "setup Audio Source failed");
        }
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(Utils.getAudioSampleFilePath(this));
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    //setup and instantiate sensor listener here
    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float max = event.values[0];
            lightIntensity = max > lightIntensity? max:lightIntensity;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public void onDestroy() {
        Log.d("SleepService", "onDestroy");
        // Stop the calibrateTimer
        timer.cancel();

        // Stop the audio recorder
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.d("SleepService", "onStartCommand");

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("SleepService", "MediaRecorder prepare() failed");
        }

        // Start the audio recorder
        mRecorder.start();

        // Start the light sensor
        sensorMgr.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Start sampling the sensors
        sampleSensors();

        // Don't want to auto restart the service
        return START_NOT_STICKY;
    }


    /**
     * sampleSensors()
     * Gets light and sound data from sensors at a fixed rate and sends broadcast with the values in
     * it (reveiced in the SleepFragment)
     */
    private void sampleSensors() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Intent i = new Intent("SensorData");
                i.putExtra("maxAmplitude", Integer.toString(mRecorder.getMaxAmplitude()));
                i.putExtra("lightIntensity", Float.toString(lightIntensity));
                sendBroadcast(i);

            }
        }, 0, SAMPLE_RATE);
    }

}
