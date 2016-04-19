package com.pedometer.tommzy.pedometer.services;
/**
 * ActivityRecognitionIntentService.java
 * Pedometer
 *
 * @version 1.0.1
 *
 * @author Hui Zheng
 *
 * Copyright (c) 2014, 2015. Pedometer App. All Rights Reserved.
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.List;

/**
 * Created by Hui Zheng on 4/16/2015.
 * This service will return the strongest probability of current activity
 * 1.walking 2. running 3. tilting 3.unknown 4. cycling 5. driving
 */
public class ActivityRecognitionIntentService extends IntentService {

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntent");
        Log.v("ActivityRecognitionIntentService","Loading");
    }

    /**
     * filter out the most probable activity
     * @param intent the intent that
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get update & most probable activity
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity activity = result.getMostProbableActivity();
            //get the type of the activity
            int type = activity.getType();

            if (type == DetectedActivity.ON_FOOT) {
                DetectedActivity betterActivity = walkingOrRunning(result.getProbableActivities());
                if (null != betterActivity) {
                    activity = betterActivity;
                    type = activity.getType();
                }
            }

            Log.i("Activity detected: ", getNameFromType(type));

            Intent mIntent = new Intent("Activity_Message")
                    .putExtra("ActivityType", getNameFromType(type));
            this.sendBroadcast(mIntent);
        }
    }

    /**
     * choose secondary activity from on foot
     * @param probableActivities on_foot
     * @return walking or running
     */
    private DetectedActivity walkingOrRunning(List<DetectedActivity> probableActivities) {
        DetectedActivity myActivity = null;
        int confidence = 0;
        for (DetectedActivity activity : probableActivities) {
            if (activity.getType() != DetectedActivity.RUNNING && activity.getType() != DetectedActivity.WALKING)
                continue;

            if (activity.getConfidence() > confidence) {
                myActivity = activity;
                break;
            }
        }

        return myActivity;
    }

    /**
     * convert integer value of activity type to string
     * @param activityType integer value of activity type
     * @return string name of activity
     */
    static public String getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
            case DetectedActivity.RUNNING:
                return "running";
            case DetectedActivity.WALKING:
                return "walking";
        }
        return "unknown";
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }
}
