package com.pedometer.tommzy.pedometer.apimanager;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.pedometer.tommzy.pedometer.PedoActivity;

/**
 * Created by Tommzy on 4/20/2015.
 */
public class RecordApiManager {

    private String TAG = "RecordApiManager";
    private GoogleApiClient mClient;
    private Context mContext;

    HistoryApiManager mHistoryMgr;

    DataType mIinputType, mOutputType;

    final DataType step = DataType.TYPE_STEP_COUNT_DELTA;
    final DataType aggregateStep = DataType.AGGREGATE_STEP_COUNT_DELTA;

    public RecordApiManager (GoogleApiClient client) {
        this.mClient = client;

        mHistoryMgr = HistoryApiManager.getInstance(mClient,new PedoActivity());
    }

    public void subscribeStep() {

        if (mClient != null) {
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {

                                           @Override
                                           public void onResult(Status status) {
                                               if (status.isSuccess()) {
                                                   if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                                       Log.i(TAG, "Step / " + "Existing subscription for activity detected.");
                                                   } else {
                                                       Log.i(TAG, "Step / " + "Successfully subscribed!");
                                                   }

                                                   mHistoryMgr.queryCurrentDayFitnessData();
//                                                   mHistoryMgr.queryCurrentWeekFitnessData();
//                                                   mHistoryMgr.queryAggregateFitnessData(step, aggregateStep);
                                                   //TODO: LET HistoryManager update the step view
                                               } else {
                                                   Log.i(TAG, "Step / " + "There was a problem subscribing. -> " + status.getStatusMessage());
                                               }
                                           }
                                       }
                    );
        }
    }

    public void unsubscribe() {
        Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {

                                       @Override
                                       public void onResult(Status status) {
                                           String dataTypeStr = DataType.TYPE_STEP_COUNT_DELTA.toString();

                                           if (status.isSuccess()) {
                                               Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr);
                                           } else {
                                               Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr);
                                           }
                                       }
                                   }
                );
    }

}
