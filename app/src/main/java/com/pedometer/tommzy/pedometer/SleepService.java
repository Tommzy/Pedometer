package com.pedometer.tommzy.pedometer;
/**
* SleepService.java
* Sam Fitness
*
* @version 1.0.0
*
* @author Jake Haas
* @author Evan Safford
* @author Nate Ford
* @author Haley Andrews
*
* Copyright (c) 2014, 2015. Wellness-App-MQP. All Rights Reserved.
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
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SleepService extends Service {
    private static final int SAMPLE_RATE = 1000;

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

    // Tracking Statuses
    private boolean isTracking = true;
    private boolean isAsleep = false;
    private int numWakeups = 0;
    private int efficiency;

    // Final Sleep Times
    private String fallAsleepTime = "";
    private String wakeUpTime = "";
    private int sleepHour;
    private int sleepMin;
    private String sleepAmPm;
    private int wakeHour;
    private int wakeMin;
    private String wakeAmPm;
    private float totalDuration = 0.0F;

    SimpleDateFormat dateFormat;
    private int date;
    private Date now;

    private TextView trackingStatus;
    private TextView todaysHours;
    private TextView todaysEfficiency;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SleepService", "onCreate");

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

        // Alert the user
        Toast.makeText(this, "Sleep Tracking Stopped", Toast.LENGTH_LONG).show();

        // Stop the calibrateTimer
        timer.cancel();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.d("SleepService", "onStartCommand");


        // Start the light sensor
        sensorMgr.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Start sampling the sensors
        //TODO change sample Sensors to pass data to some methods
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
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(new Date());

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMddyyyy", Locale.US);
                date = Integer.valueOf(dateFormat.format(calendar.getTime()));

                audioAmplitude = Integer.valueOf(mRecorder.getMaxAmplitude());
                lightIntensity = Float.valueOf(lightIntensity);

                mRecorder.getMaxAmplitude();

                checkSleepStatus();

            }
        }, 0, SAMPLE_RATE);
    }

    /**
     * checkSleepStatus()
     * Checks time and light/sound levels to see if the user falls within all sleep thresholds,
     * sets isAsleep equal to true or false and sets fallAsleepTime and wakeUpTime
     */
    private void checkSleepStatus() {

        //Time check first
        if (!SleepHourCheck()) {
            Log.d("StopSleepTracking:", "Outside hour range.");
        } else {
            // Check all conditions to see if user fell asleep
            if (!isAsleep && SleepHourCheck() && SleepLightCheck() && SleepAudioCheck()) {
                Log.d("SleepMonitor", "Fell Asleep:" + fallAsleepTime);

                isAsleep = true;

                fallAsleepTime = getTime('S');
            }

            // Check to see if user woke up
            if (isAsleep && (!SleepHourCheck() || !SleepLightCheck() || !SleepAudioCheck())) {
                Log.d("SleepMonitor", "Woke Up:" + fallAsleepTime);

                isAsleep = false;
                wakeUpTime = getTime('W');
                wakeup++;

                if(wakeup >= threshold){
                    numWakeups++;
                    wakeup = 0;
                }

                if(numWakeups > 12){
                    numWakeups = threshold - 2;
                    wakeup = 0;
                    threshold += 2;
                }

                totalDuration = getDuration();
                Log.d("getDuration", "Adding " + Float.toString(getDuration()));
//
//                todaysHours.setText("Hours Slept Today: " + getHoursTodayFormatted());
//                todaysEfficiency.setText("Today's Efficiency: " + getEfficiency());
            }
        }
    }

    /**
     * SleepHourCheck()
     * Checks to see if the current hour is between the valid sleeping hours
     *
     * @return true if hour is valid, false if hour is not valid
     */
    private boolean SleepHourCheck() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        String amPm = getAmPm();

        if (hour == 0) {
            hour = 12;
        }

        if (hour == 12 && amPm.equals("PM")) {
            return false;
        }

        if (hour == 12 && amPm.equals("AM")) {
            return true;
        }

        if ((hour >= calibratedSleepHour && amPm.equals("PM")) || (hour < calibratedWakeHour && amPm.equals("AM"))) {
            return true;
        }
        return false;
    }

    /**
     * getAmPm()
     * Checks to see if time of day is AM or PM
     *
     * @return string containing either "AM" or "PM"
     */
    private String getAmPm() {
        Calendar c = Calendar.getInstance();
        int am_pm = c.get(Calendar.AM_PM);
        String amPm;

        if (am_pm == 0)
            amPm = "AM";
        else
            amPm = "PM";

        return amPm;
    }


    /**
     * SleepLightCheck()
     * Checks to see if the light level is below the valid sleeping light level
     *
     * @return true if light level is valid, false if light level is not valid
     */
    private boolean SleepLightCheck() {
        if (lightIntensity < calibratedLight) {
            return true;
        }
        return false;
    }

    //check to see if audio is below valid level

    /**
     * SleepAudioCheck()
     * Checks to see if the sound level is below the valid sleeping sound level
     *
     * @return true if sound level is valid, false if sound level is not valid
     */
    private boolean SleepAudioCheck() {
        if (audioAmplitude < calibratedAmplitude) {
            return true;
        }
        return false;
    }

    /**
     * getTime(char set)
     * Gets the time in HH:MM:SS format, takes in a char if the sleep/wake variables need to be reset
     *
     * @param set flag if the sleep/wake times need to be updated
     * @return current time in HH:MM:SS format
     */
    private String getTime(char set) {
        Log.d("getTime", "Getting current time");
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);

        //12:00 AM is treated as hour 0
        if (hour == 0) {
            hour = 12;
        }

        Log.d("CurrentTime", Integer.toString(hour) + ":" + Integer.toString(minute) + getAmPm());

        //if the new sleep time is gotten, set it globally
        if (set == 'S') {
            sleepHour = hour;
            sleepMin = minute;
            sleepAmPm = getAmPm();
            Log.d("SetSleepTime", Integer.toString(sleepHour) + ":" + Integer.toString(sleepMin) + sleepAmPm);
        }

        //if the new wake time is gotten, set it globally
        if (set == 'W') {
            wakeHour = hour;
            wakeMin = minute;
            wakeAmPm = getAmPm();
            Log.d("SetWakeTime", Integer.toString(wakeHour) + ":" + Integer.toString(wakeMin) + wakeAmPm);
        }

        return Integer.toString(hour) + ":" + Integer.toString(minute) + getAmPm();
    }

    /**
     * getDuration()
     * Calculates the duration of time slept based on the time the user fell asleep, woke up, and previous
     * duration during the night
     *
     * @return duration of sleep for a night
     */
    private float getDuration() {
        Log.d("getDuration", "Getting current duration...");
        int newHours;
        int newMins;
        double duration;

        //both AM or both PM: simply subtract
        if ((sleepAmPm.equals("PM") && wakeAmPm.equals("PM")) || (sleepAmPm.equals("AM") && wakeAmPm.equals("AM"))) {
            newHours = Math.abs(sleepHour - wakeHour);
            newMins = Math.abs(sleepMin - wakeMin);
//            Log.d("getDuration1", "newHours: " + Integer.toString(newHours));
//            Log.d("getDuration1", "newMins: " + Integer.toString(newMins));
        }
        //crossed over midnight: have to take day change into account
        else {
            //take into account waking up in the midnight hour
            if(wakeHour == 12 && wakeAmPm.equals("AM")){
                wakeHour = 0;
            }

            newHours = (12 - sleepHour) + wakeHour - 1;
            newMins = (60 - sleepMin) + wakeMin;
            //           Log.d("getDuration2", "newHours: " + Integer.toString(newHours));
            //           Log.d("getDuration2", "newMins: " + Integer.toString(newMins));
        }

        //check for full hour
        if(newHours == 1 && sleepMin > wakeMin){
            newHours--;
            newMins = (60 - sleepMin) + wakeMin;
//            Log.d("getDuration3", "newHours: " + Integer.toString(newHours));
//            Log.d("getDuration3", "newMins: " + Integer.toString(newMins));

        }

        //add appropriate minutes
        if (newMins >= 60) {
            newMins -= 60;
            newHours += 1;
//            Log.d("getDuration4", "newHours: " + Integer.toString(newHours));
//            Log.d("getDuration4", "newMins: " + Integer.toString(newMins));
        }

//        Log.d("getDuration", "newHours: " + Integer.toString(newHours));
//        Log.d("getDuration", "newMins: " + Integer.toString(newMins));
        //convert to hours and partial hours
        duration = newHours + (newMins / 60.0);

//      Log.d("getDuration", "duration: " + Float.toString((float)duration));

        return (float)duration;
    }

}
