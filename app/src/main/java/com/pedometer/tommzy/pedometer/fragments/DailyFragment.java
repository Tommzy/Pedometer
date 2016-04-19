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
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.pedometer.tommzy.pedometer.R;
import com.pedometer.tommzy.pedometer.apimanager.HistoryApiManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Tommzy on 4/29/2015.
 */
public class DailyFragment extends Fragment implements OnChartGestureListener {

    public static final String ARG_PLANET_NUMBER = "function_number";
    public static final String TAG = "DailyFragment";

    public static Fragment newInstance() {
        return new DailyFragment();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        BarChart mChart;
        Typeface tf;

        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(TAG,"finish super call!");
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

        try {
            mChart.setData(generateBarData(HistoryApiManager.getInstance().getDailyActivitiesTime()));
        } catch (Exception e) {
            Log.i(TAG,"Encounter a problem during generating data!");
            e.printStackTrace();
        }

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


    protected BarData generateBarData(List<String> rawDataSets) {

        BarDataSet sets ;

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.clear();

        Iterator itr = rawDataSets.iterator();
        int i = 0;
        while(itr.hasNext()){

            String data = (String) itr.next();
            entries.add(new BarEntry(Integer.valueOf(data)/60000,i));
            i++;
        }

        entries.add(new BarEntry(357,i));
        sets = new BarDataSet(entries, "Daily Activities In Minutes");
        Log.i(TAG,entries.toString());
        sets.setColors(ColorTemplate.VORDIPLOM_COLORS);


        ArrayList<String> labels = new ArrayList<String>();
        labels.clear();
        labels.add("Walking");
        labels.add("Driving");
        labels.add("Running");
        labels.add("Cycling");
        labels.add("Sleeping");

        Log.i(TAG,labels.toString());

        BarData data = new BarData(labels, sets);

        Log.i(TAG,"Successfully create new data");

        return data;
    }

}
