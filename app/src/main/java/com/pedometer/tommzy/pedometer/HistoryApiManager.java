package com.pedometer.tommzy.pedometer;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
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
    long startBucketTime, endBucketTime;
    long startTime, endTime;//TODO: initiate starttime and endtime
    int dailySteps = 0;
    long walkingTime = 0;
    long drivingTime = 0;
    long runningTime = 0;
    long cyclingTime = 0;
    private List<Integer> weekySteps = new ArrayList<Integer>();
    private List<Long> weeklyWalkTime = new ArrayList<Long>();
    private List<Long> weeklyRunningTime = new ArrayList<Long>();
    private List<Long> weekyCyclingTime = new ArrayList<Long>();
    private List<Long> weeklyDrivingTime = new ArrayList<Long>();
    private List<String> dailyActivitiesTime = new ArrayList<String>();


    private HistoryApiManager (GoogleApiClient client) {
        this.mClient = client;
        getCurrentDate();
        getCurrentWeek();
    }

    public static synchronized HistoryApiManager getInstance(GoogleApiClient client){
        if(historyApiManager==null){
            historyApiManager=new HistoryApiManager(client);
        }
        return historyApiManager;
    }

    public static synchronized HistoryApiManager getInstance() throws Exception {
        if(historyApiManager==null){
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
            public void onResult(DataReadResult dataReadResult) {
                Log.i(TAG, "queryCurrentDayFitnessData()");
                updateData(dataReadResult);


            }
        });
    }

    public void queryCurrentWeekFitnessData() {
        getCurrentWeek();//update the time
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startBucketTime,endBucketTime,TimeUnit.MILLISECONDS)
                .build();

        Fitness.HistoryApi.readData(mClient, dataReadRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult dataReadResult) {
                Log.i(TAG, "queryCurrentWeekFitnessData()");
                updateWeeklyData(dataReadResult);
            }
        });

    }


    public void queryAggregateFitnessData(DataType inputType, DataType outputType) {
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .aggregate(inputType, outputType)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startBucketTime, endBucketTime, TimeUnit.MILLISECONDS)
                .build();

        Fitness.HistoryApi.readData(mClient, dataReadRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult dataReadResult) {
                Log.i(TAG, "queryAggregateFitnessData()");
                updateData(dataReadResult);
            }
        });
    }


    private void updateData(DataReadResult dataReadResult) {

        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
    }

    private void updateWeeklyData(DataReadResult dataReadResult) {

        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpWeeklyDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpWeeklyDataSet(dataSet);
            }
        }


        Log.i(TAG,weekySteps.toString());
        Log.i(TAG,weeklyWalkTime.toString());
        Log.i(TAG,weeklyRunningTime.toString());
        Log.i(TAG,weekyCyclingTime.toString());
        Log.i(TAG,weeklyDrivingTime.toString());
    }

    private void dumpWeeklyDataSet(DataSet dataSet) {
        String TAG = "dumpWeeklyDataSet";

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        for (DataPoint dp : dataSet.getDataPoints()) {
//            Log.i(TAG, "\tType: " + dp.getDataType().getName());
//            Log.i(TAG, dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
//                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
//                    + " / " + dp.getDataType().getFields().get(0).getName()
//                    + " : " + ActivityRecognitionIntentService.getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0))))));

            if(dp.getDataType().getFields().get(0).getName().equalsIgnoreCase("steps")){

                for(Field field : dp.getDataType().getFields()) {
                    Log.i(TAG, "\tField: " + field.getName() +
                            " Value: " + dp.getValue(field));
                    weekySteps.add(dp.getValue(field).asInt());
                }

            }else if(dp.getDataType().getFields().get(0).getName().equalsIgnoreCase("activity")){

                if(ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("in_vehicle")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")) {
                            weeklyDrivingTime.add(Long.valueOf(String.valueOf(dp.getValue(field))));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("on_bicycle")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weekyCyclingTime.add(Long.valueOf(String.valueOf(dp.getValue(field))));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("on_foot")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyWalkTime.add(Long.valueOf(String.valueOf(dp.getValue(field))));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("running")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyRunningTime.add(Long.valueOf(String.valueOf(dp.getValue(field))));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("walking")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            weeklyWalkTime.add(Long.valueOf(String.valueOf(dp.getValue(field))));
                        }
                    }

                }else{
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                    }
                }


            }else{
                Log.i(TAG,"BUGGGYYYYYYYYYYYYYYYYYYYYYYYYYY");
            }

        }
    }



    private void dumpDataSet(DataSet dataSet) {
        String TAG = "dumpDataSet";

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        for (DataPoint dp : dataSet.getDataPoints()) {
//            Log.i(TAG, "\tType: " + dp.getDataType().getName());
//            Log.i(TAG, dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
//                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
//                    + " / " + dp.getDataType().getFields().get(0).getName()
//                    + " : " + ActivityRecognitionIntentService.getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0))))));

            if(dp.getDataType().getFields().get(0).getName().equalsIgnoreCase("steps")){

                for(Field field : dp.getDataType().getFields()) {
                    Log.i(TAG, "\tField: " + field.getName() +
                            " Value: " + dp.getValue(field));
                    dailySteps=dp.getValue(field).asInt();
                }

            }else if(dp.getDataType().getFields().get(0).getName().equalsIgnoreCase("activity")){

                if(ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("in_vehicle")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")) {
                            drivingTime = Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("on_bicycle")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            cyclingTime=Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("on_foot")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            walkingTime=Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("running")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            runningTime=Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }

                }else if (ActivityRecognitionIntentService.
                        getNameFromType(Integer.valueOf(String.valueOf(dp.getValue(dp.getDataType().getFields().get(0)))))
                        .equalsIgnoreCase("walking")){

                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                        if(field.getName().equalsIgnoreCase("duration")){
                            walkingTime=Long.valueOf(String.valueOf(dp.getValue(field)));
                        }
                    }

                }else{
                    for(Field field : dp.getDataType().getFields()) {
                        Log.i(TAG, "\tField: " + field.getName() +
                                " Value: " + dp.getValue(field));
                    }
                }


            }else{
                Log.i(TAG,"BUGGGYYYYYYYYYYYYYYYYYYYYYYYYYY");
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

    public List<Integer> getWeeklySteps(){
        queryCurrentWeekFitnessData();
        return this.weekySteps;
    }

    public List<Long> getWeeklyWalkingTime(){
        queryCurrentWeekFitnessData();
        return this.weeklyWalkTime;
    }

    public List<Long> getWeeklyRunningTime(){
        queryCurrentWeekFitnessData();
        return this.weeklyRunningTime;
    }

    public List<Long> getWeekyCyclingTime(){
        queryCurrentWeekFitnessData();
        return this.weekyCyclingTime;
    }


    public List<Long> getWeeklyDrivingTime(){
        queryCurrentWeekFitnessData();
        return this.weeklyDrivingTime;
    }

    public List<String> getDailyActivitiesTime(){
        queryCurrentDayFitnessData();

        dailyActivitiesTime.add(String.valueOf(walkingTime));
        dailyActivitiesTime.add(String.valueOf(drivingTime));
        dailyActivitiesTime.add(String.valueOf(runningTime));
        dailyActivitiesTime.add(String.valueOf(cyclingTime));

        return dailyActivitiesTime;
    }




}
