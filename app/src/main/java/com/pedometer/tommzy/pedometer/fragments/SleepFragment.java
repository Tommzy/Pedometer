package com.pedometer.tommzy.pedometer.fragments;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.pedometer.tommzy.pedometer.R;
import com.pedometer.tommzy.pedometer.apimanager.HistoryApiManager;

/**
 * Created by Tommzy on 7/8/2015.
 */
public class SleepFragment extends Fragment implements OnChartGestureListener {

    public static final String ARG_PLANET_NUMBER = "function_number";
    public static final String TAG = "SleepFragment";

    public static Fragment newInstance() {
        return new SleepFragment();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        BarChart mChart;
        Typeface tf;

        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(TAG, "finish super call!");
        View dailyLayout = inflater.inflate(R.layout.daily_activity, null);
        Log.i(TAG,"finish Inflate!!");


        Log.i(TAG, "I get called!");

        // create a new chart object
        mChart = new BarChart(getActivity());
        mChart.setDescription("");
        mChart.setOnChartGestureListener(this);

        mChart.setHighlightIndicatorEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);

        tf = Typeface.createFromAsset(getActivity().getAssets(),"OpenSans-Light.ttf");


        Legend l =mChart.getLegend();
        l.setTypeface(tf);

//        mChart.setData(generateBarData(1, 20000, 12));
        mChart.fitScreen();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTypeface(tf);

        mChart.getAxisRight().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        mChart.animateXY(1000, 1000);//animate X and Y value
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);


        // programatically add the chart
        FrameLayout parent = (FrameLayout) dailyLayout.findViewById(R.id.daily_bar_chart);
//        ((ViewGroup)mChart.getParent()).removeView(mChart);
        parent.addView(mChart);

        return dailyLayout;
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }
}
