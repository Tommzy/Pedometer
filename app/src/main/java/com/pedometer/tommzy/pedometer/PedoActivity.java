package com.pedometer.tommzy.pedometer;
import android.Manifest;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
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
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.result.DataSourcesResult;
//circleprogeess
import com.github.lzyzsd.circleprogress.DonutProgress;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.pedometer.tommzy.pedometer.activities.SettingsActivity;
import com.pedometer.tommzy.pedometer.services.ActivityRecognitionIntentService;
import com.pedometer.tommzy.pedometer.fragments.DailyFragment;
import com.pedometer.tommzy.pedometer.apimanager.HistoryApiManager;
import com.pedometer.tommzy.pedometer.apimanager.RecordApiManager;
import com.pedometer.tommzy.pedometer.apimanager.SessionApiManager;
import com.pedometer.tommzy.pedometer.services.SleepDetectService;
import com.pedometer.tommzy.pedometer.services.SleepService;
import com.pedometer.tommzy.pedometer.fragments.WeeklyFragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
public class PedoActivity extends ActionBarActivity implements IStepView {
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO_AND_WRITE_EXTERNAL_STORAGE = 1;
    // public final static String EXTRA_MESSAGE = "com.pedometer.tommzy.pedometer.MESSAGE";
    private GoogleApiClient mClient = null;
    public static final String TAG = "PedoActivity";
    private static final int REQUEST_OAUTH = 1;
    private TextView stepTextView=null;
    private DonutProgress donutView=null;
    private int dailyStepCount = 0;
    private long calorie=0;
    // [START mListener_variable_reference]
    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener mListener;
    // [END mListener_variable_reference]
    private boolean firstConnect = true;
    private CharSequence mTitle;
    private boolean initspead;
    private long lastEvent;
    private String currentActivity=null;
    private long latestSessionTime=0;
    private String identifier = null;
    private String[] mFunctionTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    // Activity calorie unit
    private double bicycleCalorieUnit = 7.35;
    private double onFootCalorieUnit = 5;
    private double runningCalorieUnit = 12;
    private double WalkingCalorieUnit = 5;
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
    private BroadcastReceiver receiver;
    private SessionApiManager sessionApiManager;
    private HistoryApiManager historyApiManager;
    private RecordApiManager recordApiManager;
    private IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
//        Create a new fragment and specify the planet to show based on position
        Fragment fragment;
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
        if(position == 0){
            Log.i(TAG, "Choose 0 Fragment");
            FragmentManager fragmentManager = getFragmentManager();

            dailyStepCount = HistoryApiManager.getInstance(mClient,this).getDailySteps();
            updateViewStepCounter(0);

            linearLayout.setEnabled(true);
            fragment = new DailyFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame,fragment)
                    .remove(fragment)
                    .commit();
            mDrawerList.setItemChecked(position, true);
            mDrawerLayout.closeDrawer(mDrawerList);
            return;
        }else if (position == 1){
            Log.i(TAG, "Choose Daily Fragment");
            fragment = new DailyFragment();
            // Insert the fragment by replacing any existing fragment
            linearLayout.setEnabled(false);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }else if(position ==2){
            Log.i(TAG, "Choose weekly Fragment");
            fragment = new WeeklyFragment();
            // Insert the fragment by replacing any existing fragment
            linearLayout.setEnabled(false);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }else{
            Log.i(TAG, "Choose Else Fragment");
            mDrawerList.setItemChecked(position, true);
            fragment = null;
            mDrawerLayout.closeDrawer(mDrawerList);
            return;
        }

        // Highlight thet selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }
    /**
     * Menu Settings
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ped, menu);
        return true;
    }
    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED ||
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_RECORD_AUDIO_AND_WRITE_EXTERNAL_STORAGE);

        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedo);
        mFunctionTitles = getResources().getStringArray(R.array.functions_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mFunctionTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        //select the part of the view that need update
        stepTextView = (TextView) findViewById(R.id.daily_step_count);
        //instantiate the donutView
        donutView = (DonutProgress) findViewById(R.id.donut_progress);
        Intent i = new Intent(this, ActivityRecognitionIntentService.class);
        final PendingIntent mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        aim = Integer.parseInt(sharedPref.getString(SettingsActivity.STEP_GOAL, "5000"));
        initspead = true;//init spead = 0
        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.SESSIONS_API)
                .addApi(ActivityRecognition.API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                findFitnessDataSources();
                                subscribeFitnessData();
                                retrieveFitnessData();
                                ActivityRecognition
                                        .ActivityRecognitionApi
                                        .requestActivityUpdates(mClient, 0, mActivityRecognitionPendingIntent);
                                firstConnect = false;
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
        lastEvent = System.currentTimeMillis();//initiate the current time for acceleration
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            historyApiManager=HistoryApiManager.getInstance(mClient,this);
            recordApiManager=new RecordApiManager(mClient);
            sessionApiManager=SessionApiManager.getInstance(mClient);
        }else{
            mClient.connect();
            historyApiManager=HistoryApiManager.getInstance(mClient,this);
            recordApiManager=new RecordApiManager(mClient);
            sessionApiManager=SessionApiManager.getInstance(mClient);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("Activity_Message");
        //Setup the boradcast receiver
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                String action = intent.getAction();
                if(action.equalsIgnoreCase("Activity_Message")){
                    Bundle extra = intent.getExtras();
                    String activityType = extra.getString("ActivityType");
                    Log.i(TAG, activityType);
                    if((!activityType.equalsIgnoreCase("unknown"))
                            &&(!activityType.equalsIgnoreCase("still"))
                            &&(!activityType.equalsIgnoreCase("tilting"))
                            &&(!activityType.equalsIgnoreCase("sleeping"))){

                        if(getActivityType(activityType)!=null){
                            setUpSession(getActivityType(activityType));
                        }else{
                            Log.i("Returned Null!!!!!",activityType);
                        }

                    }else if (activityType.equalsIgnoreCase("sleeping")){
                        long start= extra.getLong("start");
                        long end= extra.getLong("end");
                        sessionApiManager.insertSession(start,end,"sleep");
                        Log.i("The sleeping session added", activityType);
                    }else{
                        if(currentActivity!=null) {
                            stopCurrentSession();
                            currentActivity=null;
                        }
                    }
                }
            }
        };
        registerReceiver(receiver, filter);
        mClient.connect();
    }
    public void startSleepService(){
        if (!isServiceRunning(this, SleepService.class)) {
            this.startService(new Intent(this, SleepService.class));
            this.startService(new Intent(this,SleepDetectService.class));
        }
    }
    private String getActivityType(String activityType) {
        switch (activityType){
            case "in_vehicle":
                return FitnessActivities.IN_VEHICLE;
            case "on_bicycle":
                return FitnessActivities.BIKING;
            case "running":
                return FitnessActivities.RUNNING;
            case "walking":
                return FitnessActivities.WALKING;
            case "on_foot":
                return FitnessActivities.ON_FOOT;
        }
        return null;
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
                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA) // Can specify whether data type is raw or derived.
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
    public void retrieveFitnessData(){
        historyApiManager.queryCurrentDayFitnessData();
        historyApiManager.queryEachDayFitnessData();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent call = new Intent(this, SettingsActivity.class);
            this.startActivity(call);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // [START auth_connection_flow_in_activity_lifecycle_methods]
    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Validating connection ...");
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            mClient.connect();
        }
        dailyStepCount = HistoryApiManager.getInstance(mClient,this).getDailySteps();
        updateViewStepCounter(0);
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    protected void OnDestroy(){
        super.onDestroy();
        if (mClient.isConnected() || mClient.isConnecting()) {
            mClient.disconnect();
        }
        stopService(new Intent(PedoActivity.this,ActivityRecognitionIntentService.class));
        stopService(new Intent(PedoActivity.this,SleepDetectService.class));
        stopService(new Intent(PedoActivity.this,SleepService.class));
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
    public void updateViewStepCounter(int count){
        dailyStepCount = count + dailyStepCount;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stepTextView.setText(String.valueOf(dailyStepCount));
                int persentageNoCast = dailyStepCount * 100 / aim;
                Log.i(TAG, "persentage increamental results: "+ persentageNoCast);
                donutView.setProgress(persentageNoCast);
                Log.i(TAG, "current steps: " + dailyStepCount);
                Log.i(TAG, "Daily Calorie: " + calorie);
            }
        });
    }
    private void setUpSession(String activityType) {
        long currentDate = new Date().getTime();
        if(currentActivity == null){
            Log.i("In setUpSession activity", activityType);
            Log.i("In setUpSession startTime",String.valueOf(currentDate));
            identifier = sessionApiManager.startSession(currentDate,activityType);
            currentActivity = activityType;
            latestSessionTime = currentDate;
        }else{
            if(!currentActivity.equalsIgnoreCase(activityType)){
                Log.i("Stopping session", identifier);
                sessionApiManager.stopSession(identifier, currentActivity);
                Log.i("In setUpSession activity", activityType);
                Log.i("In setUpSession startTime", String.valueOf(currentDate));
                identifier = sessionApiManager.startSession(currentDate,activityType);
                currentActivity = activityType;
                latestSessionTime = currentDate;
            }
        }
    }
    public int calculateCalorie(long duration, int activity){
        switch(activity) {
            case DetectedActivity.IN_VEHICLE:
                return 0;
            case DetectedActivity.ON_BICYCLE:
                return (int)(bicycleCalorieUnit * ( duration / (1000*60) ));
            case DetectedActivity.ON_FOOT:
                return (int)(onFootCalorieUnit * (duration / (1000*60) ));
            case DetectedActivity.STILL:
                return 0;
            case DetectedActivity.UNKNOWN:
                return 0;
            case DetectedActivity.TILTING:
                return 0;
            case DetectedActivity.RUNNING:
                return (int)(runningCalorieUnit * (duration / (1000 * 60)));
            case DetectedActivity.WALKING:
                return (int)(WalkingCalorieUnit * (duration / (1000 * 60)));
        }
        return 0;
    }
    public void initStepCounter(int count){
        this.dailyStepCount = count;
        updateViewStepCounter(0);
    }
    private void stopCurrentSession(){
        sessionApiManager.stopSession(identifier, currentActivity);
    }
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }
    public void instanciateHistoryManger(){
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            historyApiManager = HistoryApiManager.getInstance(mClient,this);
        }
    }
    public boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public long updateDailyCalorie(){
        List<String> rawDataSets = HistoryApiManager.getInstance(mClient,this).getDailyActivitiesTime();
        Iterator itr = rawDataSets.iterator();
        int i = 0;
        long calorie = 0;
        while(itr.hasNext()) {
            if (i == 0) {
                String data = (String) itr.next();
                calorie = calorie + calculateCalorie(Integer.valueOf(data), DetectedActivity.WALKING);
                i++;
            } else if (i == 1) {
                String data = (String) itr.next();
                calorie = calorie + calculateCalorie(Integer.valueOf(data), DetectedActivity.IN_VEHICLE);
                i++;
            } else if (i == 2) {
                String data = (String) itr.next();
                calorie = calorie + calculateCalorie(Integer.valueOf(data), DetectedActivity.RUNNING);
                i++;
            } else if (i == 3) {
                String data = (String) itr.next();
                calorie = calorie + calculateCalorie(Integer.valueOf(data), DetectedActivity.ON_BICYCLE);
                i++;
            }else{
                itr.next();
            }
        }
        return calorie;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO_AND_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startSleepService();
                } else {
                    System.exit(0);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
