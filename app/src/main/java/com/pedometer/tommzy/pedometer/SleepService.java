//package com.pedometer.tommzy.pedometer; /**
//* SleepService.java
//* Sam Fitness
//*
//* @version 1.0.0
//*
//* @author Jake Haas
//* @author Evan Safford
//* @author Nate Ford
//* @author Haley Andrews
//*
//* Copyright (c) 2014, 2015. Wellness-App-MQP. All Rights Reserved.
//*
//* THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
//* KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
//* IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
//* PARTICULAR PURPOSE.
//*/
//
//import android.app.Service;
//import android.content.Intent;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.media.MediaRecorder;
//import android.os.CountDownTimer;
//import android.os.IBinder;
//import android.util.Log;
//import android.widget.Toast;
//
//
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class SleepService extends Service {
//    private static final int SAMPLE_RATE = 1000;
//
//    SensorManager sensorMgr = null;
//    Sensor lightSensor = null;
//    float lightIntensity;
//
//
//    // Raw Sensor Data
//    private int audioAmplitude;
//
//    Timer timer;
//
//    // Calibrated Sensor Data (defaults set if left uncalibrated)
//    private float calibratedLight = 15;
//    private int calibratedAmplitude = 200;
//    private int calibratedSleepHour = 8;
//    private int calibratedWakeHour = 12;
//
//    // Calibration
//    private final int CALIBRATE_TIME = 10;
//    private final int NOISE_MARGIN = 200;
//    private final int LIGHT_MARGIN = 15;
//    private CountDownTimer calibrateTimer;
//    private float avgBy = 0;
//    private int threshold = 3;
//    private int wakeup = 0;
//    MediaRecorder mRecorder;
//
//    // Tracking Statuses
//    private boolean isTracking = true;
//    private boolean isAsleep = false;
//    private int numWakeups = 0;
//    private int efficiency;
//
//    SimpleDateFormat dateFormat;
//    private int date;
//    private Date now;
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d("SleepService", "onCreate");
//
//        // Create the calibrateTimer
//        timer = new Timer();
//
//        // Set up light sensor
//        sensorMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
//        lightSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
//
//        // Set up media recorder
//        mRecorder = new MediaRecorder();
//        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//
//        String mFileName = this.getCacheDir().getAbsolutePath();
//        mFileName += "/sleep_audio.3gp";
//        mRecorder.setOutputFile(mFileName);
//
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//
//    }
//
//    SensorEventListener sensorListener = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            lightIntensity = event.values[0];
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        }
//    };
//
//    @Override
//    public void onDestroy() {
//        Log.d("SleepService", "onDestroy");
//
//        // Alert the user
//        Toast.makeText(this, "Sleep Tracking Stopped", Toast.LENGTH_LONG).show();
//
//        // Stop the calibrateTimer
//        timer.cancel();
//
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startid) {
//        Log.d("SleepService", "onStartCommand");
//
//
//        // Start the light sensor
//        sensorMgr.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
//
//        // Start sampling the sensors
//        //TODO change sample Sensors to pass data to some methods
//        sampleSensors();
//
//        // Don't want to auto restart the service
//        return START_NOT_STICKY;
//    }
//
//    /**
//     * sampleSensors()
//     * Gets light and sound data from sensors at a fixed rate and sends broadcast with the values in
//     * it (reveiced in the SleepFragment)
//     */
//    private void sampleSensors() {
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                Intent i = new Intent("SensorData");
//
//                //TODO change here
//                i.putExtra("maxAmplitude", Integer.toString(mRecorder.getMaxAmplitude()));
//                i.putExtra("lightIntensity", Float.toString(lightIntensity));
//                sendBroadcast(i);
//
//                mRecorder.getMaxAmplitude();
//
//            }
//        }, 0, SAMPLE_RATE);
//    }
//
//    /**
//     * checkSleepStatus()
//     * Checks time and light/sound levels to see if the user falls within all sleep thresholds,
//     * sets isAsleep equal to true or false and sets fallAsleepTime and wakeUpTime
//     */
//    private void checkSleepStatus() {
//
//        //Time check first
//        if (!SleepHourCheck()) {
//            Log.d("StopSleepTracking:", "Outside hour range.");
//        } else {
//            // Check all conditions to see if user fell asleep
//            if (!isAsleep && SleepHourCheck() && SleepLightCheck() && SleepAudioCheck()) {
//                Log.d("SleepMonitor", "Fell Asleep:" + fallAsleepTime);
//
//                isAsleep = true;
//
//                fallAsleepTime = getTime('S');
//            }
//
//            // Check to see if user woke up
//            if (isAsleep && (!SleepHourCheck() || !SleepLightCheck() || !SleepAudioCheck())) {
//                Log.d("SleepMonitor", "Woke Up:" + fallAsleepTime);
//
//                isAsleep = false;
//                wakeUpTime = getTime('W');
//                wakeup++;
//
//                if(wakeup >= threshold){
//                    numWakeups++;
//                    wakeup = 0;
//                }
//
//                if(numWakeups > 12){
//                    numWakeups = threshold - 2;
//                    wakeup = 0;
//                    threshold += 2;
//                }
//
//                totalDuration = getDuration();
//                Log.d("getDuration", "Adding " + Float.toString(getDuration()));
//                db.addHoursSlept(new HoursSlept(String.valueOf(date), Float.valueOf(totalDuration)));
//
//                Utils.todaysSleepHours = db.getTodaysSleepTotal(date);
//
//                todaysHours.setText("Hours Slept Today: " + getHoursTodayFormatted());
//                todaysEfficiency.setText("Today's Efficiency: " + getEfficiency());
//            }
//        }
//    }
//
//    /**
//     * SleepHourCheck()
//     * Checks to see if the current hour is between the valid sleeping hours
//     *
//     * @return true if hour is valid, false if hour is not valid
//     */
//    private boolean SleepHourCheck() {
//        Calendar c = Calendar.getInstance();
//        int hour = c.get(Calendar.HOUR);
//        String amPm = getAmPm();
//
//        if (hour == 0) {
//            hour = 12;
//        }
//
//        if (hour == 12 && amPm.equals("PM")) {
//            return false;
//        }
//
//        if (hour == 12 && amPm.equals("AM")) {
//            return true;
//        }
//
//        if ((hour >= calibratedSleepHour && amPm.equals("PM")) || (hour < calibratedWakeHour && amPm.equals("AM"))) {
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * getAmPm()
//     * Checks to see if time of day is AM or PM
//     *
//     * @return string containing either "AM" or "PM"
//     */
//    private String getAmPm() {
//        Calendar c = Calendar.getInstance();
//        int am_pm = c.get(Calendar.AM_PM);
//        String amPm;
//
//        if (am_pm == 0)
//            amPm = "AM";
//        else
//            amPm = "PM";
//
//        return amPm;
//    }
//
//
//    /**
//     * SleepLightCheck()
//     * Checks to see if the light level is below the valid sleeping light level
//     *
//     * @return true if light level is valid, false if light level is not valid
//     */
//    private boolean SleepLightCheck() {
//        if (lightIntensity < calibratedLight) {
//            return true;
//        }
//        return false;
//    }
//
//    //check to see if audio is below valid level
//
//    /**
//     * SleepAudioCheck()
//     * Checks to see if the sound level is below the valid sleeping sound level
//     *
//     * @return true if sound level is valid, false if sound level is not valid
//     */
//    private boolean SleepAudioCheck() {
//        if (audioAmplitude < calibratedAmplitude) {
//            return true;
//        }
//        return false;
//    }
//
//}
