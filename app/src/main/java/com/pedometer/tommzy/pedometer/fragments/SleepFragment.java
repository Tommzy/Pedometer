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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        BarChart mChart;
        Typeface tf;
        super.onCreateView(inflater, container, savedInstanceState);
        View dailyLayout = inflater.inflate(R.layout.daily_activity, null);
        // create a new chart object
        mChart = new BarChart(getActivity());
        mChart.setDescription("");
        mChart.setOnChartGestureListener(this);
        mChart.setHighlightIndicatorEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        // choose the font
        tf = Typeface.createFromAsset(getActivity().getAssets(),"OpenSans-Light.ttf");
        // start a new legend using the font
        Legend l = mChart.getLegend();
        l.setTypeface(tf);
        mChart.fitScreen();
        //set Y
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTypeface(tf);
        mChart.getAxisRight().setEnabled(false);
        //set X
        XAxis xAxis = mChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        // set the annimation
        mChart.animateXY(1000, 1000);//animate X and Y value
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        FrameLayout parent = (FrameLayout) dailyLayout.findViewById(R.id.daily_bar_chart);
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
