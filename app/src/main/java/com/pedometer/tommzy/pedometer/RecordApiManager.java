package com.pedometer.tommzy.pedometer;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

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

        mHistoryMgr = new HistoryApiManager(mClient);
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

                                                   mHistoryMgr.queryFitnessData(step);
                                                   mHistoryMgr.queryAggregateFitnessData(step, aggregateStep);
                                                   //TODO: LET HistoryManager update the step view
                                                   //TODO:
                                                   SessionApiManager sessionMgr = new SessionApiManager(mClient);

//                                                   //^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                                                   Calendar calStart = new GregorianCalendar(2015, 01, 06, 17, 01, 30);
//                                                   Calendar calEnd = new GregorianCalendar(2015, 01, 06, 17, 03, 28);
//
//                                                   long sessionStart = calStart.getTimeInMillis();
//                                                   long sessionEnd = calEnd.getTimeInMillis();
//
//                                                   SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
//                                                   Log.i("***********************", dateFormat.format(sessionStart) + " " +  dateFormat.format(sessionEnd));
//
//                                                   sessionMgr.readSessionData(sessionStart, sessionEnd);
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
