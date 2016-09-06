package com.pedometer.tommzy.pedometer.apimanager;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.pedometer.tommzy.pedometer.services.ActivityRecognitionIntentService;
import com.pedometer.tommzy.pedometer.IStepView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * When an activity detected, created a activity history
 * Created by Tommzy on 4/20/2015.
 */
public class HistoryApiManager {
    private static HistoryApiManager historyApiManager;
    private String TAG = "HistoryApiManager";
    private GoogleApiClient mClient;
    private Context mContext;
    private long startBucketTime, endBucketTime;
    private long startTime, endTime;//TODO: initiate starttime and endtime
    private int dailySteps = 0;
    private long walkingTime = 0;
    private long drivingTime = 0;
    private long runningTime = 0;
    private long cyclingTime = 0;
    private long sleepingTime = 0;
    private List<Integer> weekySteps = new ArrayList<Integer>();
    private List<Long> weeklyWalkTime = new ArrayList<Long>();
    private List<Long> weeklyRunningTime = new ArrayList<Long>();
    private List<Long> weeklyCyclingTime = new ArrayList<Long>();
    private List<Long> weeklyDrivingTime = new ArrayList<Long>();
    private List<Long> weeklySleepingTime = new ArrayList<Long>();
    private IStepView activity;
    private HistoryApiManager (GoogleApiClient client, IStepView activity) {
        this.activity = activity;
        this.mClient = client;
        getCurrentDate();
        getCurrentWeek();
    }
    public static synchronized HistoryApiManager getInstance (GoogleApiClient client, IStepView activity){
        if (historyApiManager == null) {
            historyApiManager = new HistoryApiManager(client, activity);
        }
        return historyApiManager;
    }
    public static synchronized HistoryApiManager getInstance() throws Exception {
        if (historyApiManager == null){
            throw new Exception("HistoryApiManager: getInstance() didn't instantiated yet");
        }
        return historyApiManager;
    }
    public void queryCurrentDayFitnessData() {
        getCurrentDate();//update the time
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime,endTime,TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, dataReadRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult (DataReadResult dataReadResult) {
                Log.i(TAG, "Querying Current Day FitnessData...");
                updateData(dataReadResult);
            }
        });
    }
    public void queryEachDayFitnessData() {
        weekySteps.clear();
        weeklyWalkTime.clear();
        weeklyRunningTime.clear();
        weeklyCyclingTime.clear();
        weeklyDrivingTime.clear();
        Log.i("QueryEachDayFitnessData", "Start Query Each Day FitnessData...");
        long startTime, endTime;
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY,0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        startTime = startOfDay.getTimeInMillis();
        int i;
        for(i =0; i<7;i++) {
            startOfDay.add(Calendar.DAY_OF_YEAR,-1);
            endTime = startOfDay.getTimeInMillis();
            Log.i(TAG,"Start Of the day toString():" + startOfDay.toString());
            Log.i(TAG,"Start time of the day:" + startTime);
            Log.i(TAG,"End time of the day:" + endTime);
            DataReadRequest dataReadRequest1 = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(endTime,startTime, TimeUnit.MILLISECONDS)
                    .build();
            DataReadRequest dataReadRequest2 = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(endTime,startTime, TimeUnit.MILLISECONDS)
                    .build();
            Fitness.HistoryApi.readData(mClient, dataReadRequest1).setResultCallback(new ResultCallback<DataReadResult>() {
                @Override
                public void onResult(DataReadResult dataReadResult) {
                    Log.i(TAG, "Query Current Day Step Counts...");
                    updateEachDayData(dataReadResult);
                }
            });
            Fitness.HistoryApi.readData(mClient, dataReadRequest2).setResultCallback(new ResultCallback<DataReadResult>() {
                @Override
                public void onResult(DataReadResult dataReadResult) {
                    Log.i(TAG, "Querying Current Day Activities Data...");
                    updateEachDayData(dataReadResult);
                }
            });
            startTime=endTime;
        }
    }
    private void updateData(DataReadResult dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                    Log.i("Data that will be dumpted!", dataSet.toString());
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
                Log.i("Data that will be dumpted!!", dataSet.toString());
            }
        }
        this.activity.initStepCounter(this.dailySteps);
        this.activity.updateDailyCalorie();
    }
    private void updateEachDayData(DataReadResult dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpEachDayDataSet(dataSet);
                    Log.i("Data that will be dumpted!", dataSet.toString());
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpEachDayDataSet(dataSet);
                Log.i("Data that will be dumpted!", dataSet.toString());
            }
        }
        Log.i(TAG,"weeklysteps" + weekySteps.toString());
        Log.i(TAG,"weeklywalktime" + weeklyWalkTime.toString());
        Log.i(TAG,"weeklyrunningtime" + weeklyRunningTime.toString());
        Log.i(TAG,"weeklycyclingtime" + weeklyCyclingTime.toString());
        Log.i(TAG,"weeklydrivingtime" + weeklyDrivingTime.toString());
        Log.i(TAG,"weeklysleepingtime" + weeklySleepingTime.toString());
    }
    private void dumpDataSet(DataSet dataSet) {
        String TAG = "dumpDataSet()";
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Activity Data Type(Steps/Activities): "+ dp.getDataType().getFields().get(0).getName());
            String activityDataType = dp.getDataType().getFields().get(0).getName();
            if(activityDataType.equalsIgnoreCase("steps")){
                for(Field field : dp.getDataType().getFields()) {
                    Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                    dailySteps = dp.getValue(field).asInt();
                }
            }else if(activityDataType.equalsIgnoreCase("activity")){
                Log.i(TAG,  "The activity is: " + ActivityRecognitionIntentService
                        .getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0))))));
                String activityName = ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))));
                if(activityName.equalsIgnoreCase("in_vehicle")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")) {
                            drivingTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }
                }else if (activityName.equalsIgnoreCase("on_bicycle")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +  " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            cyclingTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }
                }else if (activityName.equalsIgnoreCase("on_foot")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "Field: " + field.getName() +  " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            walkingTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }
                }else if (activityName.equalsIgnoreCase("running")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            runningTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }
                }else if (activityName.equalsIgnoreCase("walking")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            walkingTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }
                } else if (activityName.equalsIgnoreCase("sleeping")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            sleepingTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }
                }else{
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() +
                                " Value: " + dp.getValue(field));
                    }
                }
            }else{
                Log.i(TAG,"Unexpected data reade! "); //unexpected data
            }
        }
    }
    private void dumpEachDayDataSet(DataSet dataSet) {
        String TAG = "Dumping Each Day DataSet";
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        boolean steps = false;
        boolean walk = false;
        boolean running = false;
        boolean cycling = false;
        boolean driving = false;
        boolean sleeping=false;
        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, dp.getDataType().getFields().get(0).getName());
            if(dp.getDataType().getFields().get(0).getName().equalsIgnoreCase("steps")){
                steps = true;
                for(Field field : dp.getDataType().getFields()) {
                    Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                    weekySteps.add(dp.getValue(field).asInt());
                }
            }else if(dp.getDataType().getFields().get(0).getName().equalsIgnoreCase("activity")){
                Log.i(TAG,  "The activity is: "+ActivityRecognitionIntentService.getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0))))));
                if(ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("in_vehicle")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")) {
                            weeklyDrivingTime.add((long) dp.getValue(field).asInt());
                            driving=true;
                        }
                    }
                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("on_bicycle")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyCyclingTime.add((long) dp.getValue(field).asInt());
                            cycling = true;
                        }
                    }
                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("on_foot")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyWalkTime.add((long) dp.getValue(field).asInt());
                            walk = true;
                        }
                    }
                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("running")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyRunningTime.add((long) dp.getValue(field).asInt());
                            running=true;
                        }
                    }
                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("walking")){
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyWalkTime.add((long) dp.getValue(field).asInt());
                            walk=true;
                        }
                    }
                }
                else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("sleeping")) {
                    for (Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                        if (field.getName().equalsIgnoreCase("duration")) {
                            weeklySleepingTime.add((long) dp.getValue(field).asInt());
                            sleeping = true;
                        }
                    }
                }else{
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, " Field: " + field.getName() + " Value: " + dp.getValue(field));
                    }
                }
            }else{
                Log.i(TAG,"Unexpected data reade!");
            }
        }
        if(!steps){
            if (!walk){
                weeklyWalkTime.add((long) 0);
            }
            if (!running){
                weeklyRunningTime.add((long) 0);
            }
            if(!cycling){
                weeklyCyclingTime.add((long) 0);
            }
            if(!driving){
                weeklyDrivingTime.add((long) 0);
            }
            if(!sleeping){
                weeklySleepingTime.add((long) 0);
            }
        }
    }
    private void getCurrentDate(){
        Calendar cal = Calendar.getInstance();
        Calendar startOfDay = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        endTime = cal.getTimeInMillis();
        startOfDay.set(Calendar.HOUR_OF_DAY,0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        startTime = startOfDay.getTimeInMillis();
    }
    private void getCurrentWeek(){
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        endBucketTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        startBucketTime = cal.getTimeInMillis();
    }
    public int getDailySteps(){
        queryCurrentDayFitnessData();
        return this.dailySteps;
    }
    public long getDailyWalkingTime(){
        queryCurrentDayFitnessData();
        return this.walkingTime;
    }
    public long getDailyDrivingTime(){
        queryCurrentDayFitnessData();
        return this.drivingTime;
    }
    public long getDailyRunningTime(){
        queryCurrentDayFitnessData();
        return this.runningTime;
    }
    public long getDailyCyclingTime(){
        queryCurrentDayFitnessData();
        return this.cyclingTime;
    }
    public List<String> getDailyActivitiesTime(){
        List<String> dailyActivitiesTime = new ArrayList<String>();
        dailyActivitiesTime.add(String.valueOf(walkingTime));
        dailyActivitiesTime.add(String.valueOf(drivingTime));
        dailyActivitiesTime.add(String.valueOf(runningTime));
        dailyActivitiesTime.add(String.valueOf(cyclingTime));
        dailyActivitiesTime.add(String.valueOf(sleepingTime));
        Log.d(TAG, "Daily"+dailyActivitiesTime.toString());
        return dailyActivitiesTime;
    }
    public List<ArrayList<String>> getWeeklyActivitiesTime(){
        List<ArrayList<String>> weeklyActivitiesTime= new ArrayList<ArrayList<String>>();
        ArrayList<String> weeklyWalkTimeString = new ArrayList<String>();
        for(Long time: weeklyWalkTime){
            weeklyWalkTimeString.add(time.toString());
        }
        weeklyActivitiesTime.add(weeklyWalkTimeString);
        ArrayList<String> weeklyRunningTimeString = new ArrayList<String>();
        for(Long time: weeklyRunningTime){
            weeklyRunningTimeString.add(time.toString());
        }
        weeklyActivitiesTime.add(weeklyRunningTimeString);
        ArrayList<String> weeklyCyclingTimeString = new ArrayList<String>();
        for(Long time: weeklyCyclingTime){
            weeklyCyclingTimeString.add(time.toString());
        }
        weeklyActivitiesTime.add(weeklyCyclingTimeString);
        ArrayList<String> weeklyDrivingTimeString = new ArrayList<String>();
        for(Long time: weeklyDrivingTime){
            weeklyDrivingTimeString.add(time.toString());
        }
        weeklyActivitiesTime.add(weeklyDrivingTimeString);
        ArrayList<String> weeklySleepingTimeString = new ArrayList<String>();
        for(Long time: weeklySleepingTime){
            weeklySleepingTimeString.add(time.toString());
        }
        weeklyActivitiesTime.add(weeklySleepingTimeString);
        Log.d(TAG, "Weekly: "+weeklyActivitiesTime.toString());
        return weeklyActivitiesTime;
    }
}