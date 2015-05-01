package com.pedometer.tommzy.pedometer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Tommzy on 4/29/2015.
 */
public class WeeklyFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View weeklyLayout = inflater.inflate(R.layout.daily_activity, container, false);
        return weeklyLayout;
    }
}
