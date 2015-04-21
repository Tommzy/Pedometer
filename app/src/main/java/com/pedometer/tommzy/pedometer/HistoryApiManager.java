package com.pedometer.tommzy.pedometer;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * When an activity detected, created a activity history
 * Created by Tommzy on 4/20/2015.
 */
public class HistoryApiManager {

    private String TAG = "HistoryApiManager";

    private GoogleApiClient mClient;
    private Context mContext;
    long startBucketTime, endBucketTime;
    long startTime, endTime;//TODO: initiate starttime and endtime

    public HistoryApiManager (GoogleApiClient client) {
        this.mClient = client;
    }


    public void queryFitnessData(DataType dataType) {
        getTodayDate();
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(dataType)
                .build();

        Fitness.HistoryApi.readData(mClient, dataReadRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult dataReadResult) {
                Log.i(TAG, "queryFitnessData()");
                printData(dataReadResult);
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
                printData(dataReadResult);
            }
        });
    }


    private void printData(DataReadResult dataReadResult) {

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



    private void dumpDataSet(DataSet dataSet) {
        String TAG = "dumpDataSet";

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
                    + " / " + dp.getDataType().getFields().get(0).getName()
                    + " : " + dp.getValue(dp.getDataType().getFields().get(0)));
        }
    }


    private void getTodayDate(){

    }


}
