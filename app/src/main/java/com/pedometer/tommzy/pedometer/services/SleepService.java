package com.pedometer.tommzy.pedometer.services;
/**
 * SleepService.java
 * Sam Fitness, Pedometer App
 *
 * @version 2.0.1
 *
 * @author Jake Haas
 * @author Evan Safford
 * @author Nate Ford
 * @author Haley Andrews
 * @author Hui Zheng
 *
 * Copyright (c) 2014, 2015. WPI Computer Science Dept, Wellness-App-MQP & Pedometer App. All Rights Reserved.
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import Util.Utils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;

/**
 * This class in charge of judging user's sleep duration
 * Through:
 * 1. listen to the activity and environment changeing
 * 2. phone status changing(user has any interaction with phone?)
 * 3. count the probability of sleep depends on the equation
 */
public class SleepService extends Service {
    private static final String TAG = "SleepSerivce";

    // Raw Sensor Data
    private float lightIntensity;
    private int audioAmplitude;
    private Timer timer;

    // Calibrated Sensor Data (defaults set if left uncalibrated)
    private float calibratedLight = 19;
    private int calibratedAmplitude = 300;
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
    // Tracking Statuses
    private boolean isTracking = true;
    private boolean isAsleep = false;
    private int numWakeups = 0;

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

    //probablities initialize
    private double audioValue = 0.00;
    private double sleeplight = 0.00;
    private double charged = 0.00;
    private double locked = 0.00;
    private double stationary=0.00;
    /**
     * BroadcastReceiver that receive
     * 1. sensor data from sleep detect service
     * 2. current activity status from ActivityRecognitionIntentService
     */
    private final BroadcastReceiver recieveFromSleepService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());//set current time

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMddyyyy", Locale.US);

            //take the raw data and save it locally
            if (action.equals("SensorData")) {
                Bundle extras = intent.getExtras();
                try {
                    String maxAmplitudeIn = extras.getString("maxAmplitude");
                    String lightIntensityIn = extras.getString("lightIntensity");
                    audioAmplitude = Integer.parseInt(maxAmplitudeIn);
                    lightIntensity = Float.parseFloat(lightIntensityIn);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            //update the current activity state probability score
            if(action.equalsIgnoreCase("Activity_Message")){
                Bundle extra = intent.getExtras();
                String activityType = extra.getString("ActivityType");
                Log.i(TAG, activityType);

                if(activityType.equalsIgnoreCase("still")){
                    stationary=54.45;
                }else{
                    stationary=0.00;
                    Log.d(TAG,"The ActivityType From the sleepService"+activityType);
                }
            }
            checkSleepStatus();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SleepService onCreate");
        isTracking = true;
        timer = new Timer();

        //filter the broadcast of type sensorData and Actitivity_Message
        IntentFilter intentFilter = new IntentFilter("SensorData");
        intentFilter.addAction("Activity_Message");
        registerReceiver(recieveFromSleepService, intentFilter);

        calibrateSensors();
        startSleepTracking();
    }

    /**
     * startSleepTracking()
     * Begins tracking sleep
     */
    private void startSleepTracking() {
        isTracking = true;
        Log.d("SleepFragment", "Starting sleep service");
        totalDuration = 0.0F;//initialize the duration
    }


    /**
     * stopSleepTracking()
     * Stops tracking sleep
     */
    private void stopSleepTracking() {
        Log.d("SleepFragment", "Stopping sleep service");
        isTracking = false;
    }

    /**
     * calibrateSensors()
     * Calibrates the light/sound levels by getting avgs (get values every 1sec for 10sec period) and
     * adding a preset margin to allow for movement/daybreak
     */
    private void calibrateSensors() {
        calibrateTimer = new CountDownTimer((CALIBRATE_TIME * 1000), 1000) {
            public void onTick(long millisUntilFinished) {
                //add up total values for averaging
                calibratedLight += lightIntensity;
                calibratedAmplitude += audioAmplitude;
                avgBy++;
            }

            public void onFinish() {
                //calculate avgs
                calibratedLight = Math.round(calibratedLight / avgBy);
                calibratedAmplitude = calibratedAmplitude / Math.round(avgBy);

                //after averages are calculated, add in some margin of noise/light
                calibratedLight += LIGHT_MARGIN;
                calibratedAmplitude += NOISE_MARGIN;

                Log.d("SleepMonitor", "Calibrated sensors");

                checkSleepStatus();

                calibrateTimer.cancel();
            }
        }.start();

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
            stopSleepTracking();
        }else {
            calibrateSensors();
            // Check all conditions to see if user fell asleep
            if (!isAsleep&&SleepHourCheck()) {

                SleepLightCheck();
                SleepAudioCheck();
                if(getSleepSum()>90.315) {
                    Log.d("SleepMonitor", "Fell Asleep:" + fallAsleepTime);
                    isAsleep = true;
                    fallAsleepTime = getTime('S');

                    Log.i(TAG, "Light Value Changed: "
                            + "Sound: "
                            + audioAmplitude
                            + "  "
                            + "lightIntensity: "
                            + Float.toString(lightIntensity));
                }
            }

            // Check to see if user woke up
            if (isAsleep && (!SleepHourCheck() || getSleepSum()<90.315)) {
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

                Utils.todaysSleepHours += Float.valueOf(totalDuration);
            }
            Log.i(TAG, "TodaysSleepHourse"+Utils.todaysSleepHours);
        }
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
     * SleepLightCheck()
     * Checks to see if the light level is below the valid sleeping light level
     *
     * @return true if light level is valid, false if light level is not valid
     */
    private boolean SleepLightCheck() {
        //check current calibrated light value is normal or not.
        Log.d("Light intensity is : ", String.valueOf(lightIntensity));
        if(lightIntensity>75){
            if(lightIntensity>150){
                sleeplight=0;
                return false;
            }else{
                sleeplight=(1-(lightIntensity-75)/75)*4.15;
            }
        }else{
            sleeplight=4.15;
        }
        return true;
    }

    /**
     * SleepAudioCheck()
     * Checks to see if the sound level is below the valid sleeping sound level
     * @return true if sound level is valid, false if sound level is not valid
     */
    private boolean SleepAudioCheck() {
        if(audioAmplitude>500){
            if(audioAmplitude>1000){
                audioValue=0;
                return false;
            }else {
                audioValue = (1 - (audioAmplitude - 500) / 500) * 34.84;
            }
        }else{
            audioValue=34.84;
        }
        return true;
    }


    /**
     * getDuration()
     * Calculates the duration of time slept based on the time the user fell asleep, woke up, and previous
     * duration during the night
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
        }

        //crossed over midnight: have to take day change into account
        else {
            //take into account waking up in the midnight hour
            if(wakeHour == 12 && wakeAmPm.equals("AM")){
                wakeHour = 0;
            }

            newHours = (12 - sleepHour) + wakeHour - 1;
            newMins = (60 - sleepMin) + wakeMin;
        }

        //check for full hour
        if(newHours == 1 && sleepMin > wakeMin){
            newHours--;
            newMins = (60 - sleepMin) + wakeMin;


        }

        //add appropriate minutes
        if (newMins >= 60) {
            newMins -= 60;
            newHours += 1;

        }


        //convert to hours and partial hours
        duration = newHours + (newMins / 60.0);


        return (float)duration;
    }

    /**
     * Calculate and return all weighted probabilist values of
     * audio, light environment, phone status(charge, lock), stationary
     * @return the current total probability
     */
    private double getSleepSum(){


        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        // Check charging status
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        KeyguardManager myKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = myKM.inKeyguardRestrictedInputMode();

        if(isCharging){
            charged=4.69;
            Log.d("Charged Value: ", String.valueOf(charged));
        }
        if(isLocked){
            locked = 5.12;
            Log.d("locked Value: ", String.valueOf(locked));
        }
        Log.d("Light value: ", String.valueOf(sleeplight));
        Log.d("Sound value: ", String.valueOf(audioValue));

//        audio 34.84
//        light 4.15
//        phone charging 4.69
//        stationary 54.45
//        phone lock: 5.12
//        total  103.52
        Log.d("SleepValueSum",String.valueOf(audioValue+sleeplight+charged+stationary+locked));
        return audioValue+sleeplight+charged+stationary+locked;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }


}
