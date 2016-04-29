package com.pedometer.tommzy.pedometer.apimanager;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tommzy on 4/20/2015.
 */
public class SessionApiManager {

    private static SessionApiManager sessionApiManager;

    private String TAG = "SessionApiManager";

    private GoogleApiClient mClient;

    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";

    private Session newSession;
    private int cnt = 0;
    private boolean resultFlag;
    private Calendar calendar;

    private long startTime, endTime;

    private SessionApiManager (GoogleApiClient client) {
        this.mClient = client;
    }

    public static synchronized SessionApiManager getInstance(GoogleApiClient client){
        if(sessionApiManager==null){
            sessionApiManager=new SessionApiManager(client);
        }
        return sessionApiManager;
    }

    public static synchronized SessionApiManager getInstance() throws Exception {
        if(sessionApiManager==null){
            throw new Exception("HistoryApiManager: getInstance() didn't instantiated yet");
        }
        return sessionApiManager;
    }

    public String startSession(final long startTime, final String activity) {
        cnt++;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        Date date = calendar.getTime();
        String identifier;
        identifier = String.valueOf(startTime)+activity;

        Log.i("In startSession activity", activity);
        Log.i("In StartSession startTime",String.valueOf(startTime));

        newSession = new Session.Builder()
                .setName(activity+"session")
                .setIdentifier(identifier)
                .setDescription(activity.toString() + dateFormat.format(date) + " " + String.valueOf(cnt))
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setActivity(activity)
                .build();

        Fitness.SessionsApi.startSession(mClient, newSession)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if(status.isSuccess()) {
                            resultFlag = true;
                            Log.i(TAG, "Successfully start session " + String.valueOf(startTime)+activity);
                        } else {
                            resultFlag = false;
                            Log.i(TAG, "Failed to start session " + String.valueOf(startTime)+activity);
                        }
                    }
                });

        return identifier;
    }

    public String insertSession(final long startTime, final long endTime, final String activity) {
        cnt++;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        Date date = calendar.getTime();
        String identifier;
        identifier = String.valueOf(startTime)+activity;

        Log.i("In startSession activity", activity);
        Log.i("In StartSession startTime",String.valueOf(startTime));

        newSession = new Session.Builder()
                .setName(activity+"session")
                .setIdentifier(identifier)
                .setDescription(activity.toString() + dateFormat.format(date) + " " + String.valueOf(cnt))
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime,TimeUnit.MILLISECONDS)
                .setActivity(activity)
                .build();

        // Build a session insert request
        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(newSession)
                .build();


        Fitness.SessionsApi.insertSession(mClient,insertRequest);
        return identifier;
    }



    public void stopSession(String identifier, final String activity) {
        final String sessionName = activity+"session";

        Fitness.SessionsApi.stopSession(mClient, identifier)
                .setResultCallback(new ResultCallback<SessionStopResult>() {
                    @Override
                    public void onResult(SessionStopResult sessionStopResult) {

                        for (Session session : sessionStopResult.getSessions()) {

                            Log.i(TAG, "Successfully stop session");
                            Log.i(TAG,  new SimpleDateFormat("HH:mm:ss").format(session.getStartTime(TimeUnit.MILLISECONDS))
                                    + " ~ "
                                    + new SimpleDateFormat("HH:mm:ss").format(session.getEndTime(TimeUnit.MILLISECONDS)));

                            readSessionData(session.getStartTime(TimeUnit.MILLISECONDS), session.getEndTime(TimeUnit.MILLISECONDS), sessionName);
                        }
                    }
                });
    }


    public void readSessionData(long startTime, long endTime, String sessionName) {
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .setSessionName(sessionName)
                .build();

        Fitness.SessionsApi.readSession(mClient, readRequest)
                .setResultCallback(new ResultCallback<SessionReadResult>() {
                                       @Override
                                       public void onResult(SessionReadResult sessionReadResult) {

                                           Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                                                   + sessionReadResult.getSessions().size());
                                           for (Session session : sessionReadResult.getSessions()) {
                                               // Process the session
                                               dumpSession(session);

                                               // Process the data sets for this session
                                               List<DataSet> dataSets = sessionReadResult.getDataSet(session);
                                               for (DataSet dataSet : dataSets) {
                                                   dumpDataSet(dataSet);
                                               }
                                           }
                                       }
                                   }
                );
    }

    private void dumpDataSet(DataSet dataSet) {
        String TAG = "dumpDataSet";

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
                    + " / " + dp.getDataType().getFields().get(0).getName()
                    + " : " + dp.getValue(dp.getDataType().getFields().get(0)));
        }
    }

    private void dumpSession(Session session) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    }


}
