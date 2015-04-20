package com.pedometer.tommzy.pedometer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by Tommzy on 4/16/2015.
 */
public class ActivityRecognitionIntentService extends IntentService {

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntent");
        Log.v("EXAMPLE","constructor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v("EXAMPLE","new activity update");
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

            Log.i("inSerice the activity dectetect is : ", getNameFromType(type));

            Intent mIntent = new Intent("Activity_Message")
                    .putExtra("ActivityType", getNameFromType(type));
//        getApplicationContext().sendBroadcast(mIntent);
            this.sendBroadcast(mIntent);
        }
    }

    private DetectedActivity walkingOrRunning(List<DetectedActivity> probableActivities) {
        DetectedActivity myActivity = null;
        int confidence = 0;
        for (DetectedActivity activity : probableActivities) {
            if (activity.getType() != DetectedActivity.RUNNING && activity.getType() != DetectedActivity.WALKING)
                continue;

            if (activity.getConfidence() > confidence)
                myActivity = activity;
        }

        return myActivity;
    }

    private String getNameFromType(int activityType) {
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
        }
        return "unknown";
    }


}
