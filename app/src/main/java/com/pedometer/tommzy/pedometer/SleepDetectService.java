package com.pedometer.tommzy.pedometer;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class SleepDetectService extends Service {
    private static final int SAMPLE_RATE = 1000;
    public static final String TAG = "SleepDetectService";

    SensorManager sensorMgr = null;
    Sensor lightSensor = null;
    float lightIntensity;


    // Raw Sensor Data
    private int audioAmplitude;

    Timer timer;

    // Calibrated Sensor Data (defaults set if left uncalibrated)
    private float calibratedLight = 15;
    private int calibratedAmplitude = 200;
    private int calibratedSleepHour = 8;
    private int calibratedWakeHour = 12;

    // Calibration
    private final int CALIBRATE_TIME = 10;
    private final int NOISE_MARGIN = 200;
    private final int LIGHT_MARGIN = 15;
    private CountDownTimer calibrateTimer;
    private float avgBy = 0;
    private int threshold = 3;
    private int wakeup = 0;
    MediaRecorder mRecorder;

    public SleepDetectService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"SleepService onCreate");

        // Create the calibrateTimer
        timer = new Timer();

        // Set up light sensor
        sensorMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        lightSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Set up media recorder
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        String mFileName = this.getCacheDir().getAbsolutePath();
        mFileName += "/sleep_audio.3gp";
        mRecorder.setOutputFile(mFileName);

        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

    }

    SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lightIntensity = event.values[0];
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

            int value =60;
            int sample = 0;


            @Override
            public void run() {


                while(true){
                    if (mRecorder.getMaxAmplitude()<220){
                        sample = sample + 35;
                    }
                    if(lightIntensity<15){
                        sample = sample + 5;
                    }
                    if( sample>value){
                        Intent i = new Intent("SleepDetectingResult");
                        i.putExtra("soundAndLight", Integer.toString(sample));
                        sendBroadcast(i);
                    }

                }

            }
        }, 0, SAMPLE_RATE);
    }
}
