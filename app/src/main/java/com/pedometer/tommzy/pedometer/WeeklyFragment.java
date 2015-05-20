package com.pedometer.tommzy.pedometer;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Tommzy on 4/29/2015.
 */
public class WeeklyFragment extends Fragment implements OnChartGestureListener {
    public static final String ARG_PLANET_NUMBER = "function_number";
    public static final String TAG = "WeeklyFragment";

    public static Fragment newInstance() {
        return new WeeklyFragment();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        BarChart mChart;
        Typeface tf;

        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(TAG, "finish super call!");
        View weeklyLayout = inflater.inflate(R.layout.weekly_activity, null);
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
            mChart.setData(generateBarData(HistoryApiManager.getInstance().getWeeklyActivitiesTime()));
        } catch (Exception e) {
            Log.i(TAG,"Encounter a problem during generating data!");
            e.printStackTrace();
        }

        Legend l =mChart.getLegend();
        l.setTypeface(tf);

        mChart.fitScreen();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTypeface(tf);

        mChart.getAxisRight().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setEnabled(false);

        // programatically add the chart
        FrameLayout parent = (FrameLayout) weeklyLayout.findViewById(R.id.daily_bar_chart);
//        ((ViewGroup)mChart.getParent()).removeView(mChart);
        parent.addView(mChart);

        return weeklyLayout;
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

    protected BarData generateBarData(List<ArrayList<String>> rawDataSets) {

        BarDataSet walkingSet ;
        BarDataSet runningSet;
        BarDataSet cyclingSet;
        BarDataSet DrivingSet;

        ArrayList<BarEntry> weeklyWalkTime = new ArrayList<>();
        ArrayList<BarEntry> weeklyRunningTime = new ArrayList<>();
        ArrayList<BarEntry> weeklyCyclingTime = new ArrayList<>();
        ArrayList<BarEntry> weeklyDrivingTime = new ArrayList<>();

        boolean walkFlag=false;
        boolean runningFlag=false;
        boolean cyclingFlag=false;
        boolean drivingFlag=false;


        Iterator itr = rawDataSets.iterator();
        int i = 0;
        while(itr.hasNext()){

            ArrayList<String> data = (ArrayList<String>) itr.next();//take out the activity item in a String formart

            if(!walkFlag){
                for (String dataEntry: data){
                    weeklyWalkTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                walkFlag = true;
            }else if(!runningFlag){
                for(String dataEntry: data){
                    weeklyRunningTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                runningFlag=true;
            }else if(!cyclingFlag){
                for(String dataEntry: data){
                    weeklyCyclingTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                cyclingFlag=true;
            }else if(!drivingFlag){
                for(String dataEntry: data){
                    weeklyDrivingTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                drivingFlag=true;
            }else{
                Log.i(TAG, "get unexpected data!");
            }
        }

        walkingSet = new BarDataSet(weeklyWalkTime, "walking time In Minutes");
        runningSet = new BarDataSet(weeklyRunningTime, "running time In Minutes");
        cyclingSet = new BarDataSet(weeklyCyclingTime, "cycling time In Minutes");
        DrivingSet = new BarDataSet(weeklyDrivingTime, "driving time In Minutes");


        Log.i(TAG,walkingSet.toString());
        Log.i(TAG,runningSet.toString());
        Log.i(TAG,cyclingSet.toString());
        Log.i(TAG,DrivingSet.toString());


        ArrayList<String> labels = new ArrayList<String>();
        labels.clear();
        labels.add("Walking");
        labels.add("Running");
        labels.add("Driving");
        labels.add("Cycling");

        Log.i(TAG,labels.toString());

        BarData data = new BarData();
        data.addDataSet(walkingSet);
        data.addDataSet(runningSet);
        data.addDataSet(cyclingSet);
        data.addDataSet(DrivingSet);


        Log.i(TAG,"Successfully create new data");

        return data;
    }
}
