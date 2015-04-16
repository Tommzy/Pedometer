package com.pedometer.tommzy.pedometer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;


import android.os.Trace;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.result.DataSourcesResult;
//circleprogeess
import com.github.lzyzsd.circleprogress.DonutProgress;

import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.SensorRequest;

import java.util.concurrent.TimeUnit;

import static android.util.FloatMath.sqrt;


public class PedoActivity extends BaseActivity {
    // public final static String EXTRA_MESSAGE = "com.pedometer.tommzy.pedometer.MESSAGE";
    private GoogleApiClient mClient = null;
    public static final String TAG = "StepSensorsApi";
    private static final int REQUEST_OAUTH = 1;
    private TextView stepTextView=null;
    private DonutProgress donutView=null;
    private int dailyStepCount = 0;
    // [START mListener_variable_reference]
    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener mListener;
    // [END mListener_variable_reference]
    private boolean firstConnect = true;

    private CharSequence mTitle;

    private boolean initspead;
    private long lastEvent;



    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    //initiate the donut progress bar
    private DonutProgress donutProgress;

    private SensorManager sm;

    public int aim;

    //Accerlate sensor
    /*
     * SensorEventListener接口的实现，需要实现两个方法
     * 方法1 onSensorChanged 当数据变化的时候被触发调用
     * 方法2 onAccuracyChanged 当获得数据的精度发生变化的时候被调用，比如突然无法获得数据时
     * */
    final SensorEventListener myAccelerometerListener = new SensorEventListener(){

        //复写onSensorChanged方法
        public void onSensorChanged(SensorEvent sensorEvent){
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
//                Log.i(TAG,"onSensorChanged");

                //图解中已经解释三个值的含义
                float X_lateral = sensorEvent.values[0];
                float Y_longitudinal = sensorEvent.values[1];
                float Z_vertical = sensorEvent.values[2];
//                Log.i(TAG,"\n heading "+X_lateral);
//                Log.i(TAG,"\n pitch "+Y_longitudinal);
//                Log.i(TAG,"\n roll "+Z_vertical);

                long now;
                long interval;
                now = System.currentTimeMillis();
                interval = now-lastEvent;
                lastEvent=now;

                float speed = 0;
                float linearAccelerate;
                linearAccelerate = sqrt(X_lateral * X_lateral + Y_longitudinal * Y_longitudinal);

                if(initspead){
                    speed = linearAccelerate*interval/1000;
//                    Log.i(TAG,"\n Time Interval "+interval);
//                    Log.i(TAG,"\n Current speed "+speed);
                    if(speed > 5.6 ){
                        Log.i(TAG,"You Are Running!");
                    }
                }else{
                    speed = speed + linearAccelerate*interval;
                    Log.i(TAG,"\n Current speed "+speed);
                }
            }
        }
        //复写onAccuracyChanged方法
        public void onAccuracyChanged(Sensor sensor , int accuracy){
            Log.i(TAG, "onAccuracyChanged");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedo);
        //select the part of the view that need update
        stepTextView = (TextView) findViewById(R.id.daily_step_count);
        //instantiate the donutView
        donutView = (DonutProgress) findViewById(R.id.donut_progress);

        initspead=true;//init spead = 0


        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        //TODO: change aim to dynamic
        aim = 300;
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
//                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                // Now you can make calls to the Fitness APIs.
                                // Put application specific code here.
                                // [END auth_build_googleapiclient_beginning]
                                //  What to do? Find some data sources!
//                                if (firstConnect) {
                                    findFitnessDataSources();
                                    subscribeFitnessData();
                                    firstConnect = false;
//                                    Log.i(TAG, "First connect! Regester Everything!");
//                                } else {
//                                    Log.i(TAG, "just resumed...");
//                                }
                                // [START auth_build_googleapiclient_ending]
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                if (!result.hasResolution()) {
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            PedoActivity.this, 0).show();
                                    return;
                                }
                                // The failure has a resolution. Resolve it.
                                // Called typically when the app is not yet authorized, and an
                                // authorization dialog is displayed to the user.
                                if (!authInProgress) {
                                    try {
                                        Log.i(TAG, "Attempting to resolve failed connection");
                                        authInProgress = true;
                                        result.startResolutionForResult(PedoActivity.this,
                                                REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.e(TAG,
                                                "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        }
                )
                .build();
        //make an sensor manager to get system service
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //initiate the ACCELEROMETER
        int sensorType = Sensor.TYPE_ACCELEROMETER;

        //register a listener at here
        sm.registerListener(myAccelerometerListener,
                sm.getDefaultSensor(sensorType),
                SensorManager.SENSOR_DELAY_NORMAL);
        lastEvent=System.currentTimeMillis();//initiate the current time for acceleration


    }

    /**
     * Find available data sources and attempt to register on a specific {@link DataType}.
     * If the application cares about a data type but doesn't care about the source of the data,
     * this can be skipped entirely, instead calling
     *     {@link com.google.android.gms.fitness.SensorsApi
     *     #register(GoogleApiClient, SensorRequest, DataSourceListener)},
     * where the {@link com.google.android.gms.fitness.request.SensorRequest} contains the desired data type.
     */
    private void findFitnessDataSources() {

        // [START find_data_sources]
        Fitness.SensorsApi.findDataSources(mClient, new DataSourcesRequest.Builder()
                // At least one datatype must be specified.
//                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                        // Can specify whether data type is raw or derived.
                .setDataSourceTypes(DataSource.TYPE_DERIVED)
                .build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        Log.i(TAG, "Result: " + dataSourcesResult.getStatus().toString());
                        Log.i(TAG, "Result: " + dataSourcesResult.getClass().getName());
                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                            Log.i(TAG, "Data source found: " + dataSource.toString());
                            Log.i(TAG, "Data Source type: " + dataSource.getDataType().getName());

                            //Let's register a listener to receive Activity data!
                            if (dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)
                                    && mListener == null) {
                                Log.i(TAG, "Data source for TYPE_STEP_COUNT_DELTA found!  Registering.");
                                registerFitnessDataListener(dataSource,
                                        DataType.TYPE_STEP_COUNT_DELTA);
                            }
                        }
                    }
                });
        // [END find_data_sources]
    }


    private void subscribeFitnessData(){

        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                        }
                    }
                });
    }


    /**
     * Register a listener with the Sensors API for the provided {@link DataSource} and
     * {@link DataType} combo.
     */
    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        // [START register_data_listener]
        mListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    Log.i(TAG, "Detected DataPoint value: " + val);
                    updateViewStepCounter(val.asInt());
                }
            }
        };

        Fitness.SensorsApi.add(
                mClient,
                new SensorRequest.Builder()
                        .setDataSource(dataSource) // Optional but recommended for custom data sets.
                        .setDataType(dataType) // Can't be omitted.
                        .setSamplingRate(1, TimeUnit.SECONDS)
                        .build(),
                mListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered!");
                        } else {
                            Log.i(TAG, "Listener not registered.");
                        }
                    }
                });
        // [END register_data_listener]
    }


    /**
     * Unregister the listener with the Sensors API.
     */
    private void unregisterFitnessDataListener() {
        if (mListener == null) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return;
        }

        // [START unregister_data_listener]
        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.
        Fitness.SensorsApi.remove(
                mClient,
                mListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener was removed!");
                        } else {
                            Log.i(TAG, "Listener was not removed.");
                        }
                    }
                });
        // [END unregister_data_listener]
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ped, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            unregisterFitnessDataListener();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // [START auth_connection_flow_in_activity_lifecycle_methods]
    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Connecting...");
        mClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mClient.isConnected()||mClient.isConnecting()) {
            mClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
    // [END auth_connection_flow_in_activity_lifecycle_methods]


    protected void updateViewStepCounter(int count){
        dailyStepCount = count + dailyStepCount;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stepTextView.setText(String.valueOf(dailyStepCount));
                int persentageNoCast=dailyStepCount*100/aim;
                Log.i(TAG, "persentage increamental results: "+ persentageNoCast);
                donutView.setProgress(persentageNoCast);
                Log.i(TAG, "current steps: " + dailyStepCount);
            }
        });
//        stepTextView.setText(String.valueOf(dailyStepCount));
//        Log.i(TAG, "current steps: " + dailyStepCount);
    }



    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        setContentView(R.layout.activity_pedo);
    }

}
