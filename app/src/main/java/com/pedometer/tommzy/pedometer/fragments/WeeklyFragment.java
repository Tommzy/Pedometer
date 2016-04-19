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
        mChart.invalidate();

        Legend l =mChart.getLegend();
        l.setTypeface(tf);

//        mChart.fitScreen();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTypeface(tf);

        mChart.getAxisRight().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        mChart.animateXY(1000, 1000);//animate X and Y value
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

//        mChart.setDescription("");    // Hide the description
//        mChart.getAxisLeft().setDrawLabels(false);
//        mChart.getAxisRight().setDrawLabels(false);
//        mChart.getXAxis().setDrawLabels(false);

        mChart.getLegend().setEnabled(false);

        // programatically add the chart
        FrameLayout parent = (FrameLayout) weeklyLayout.findViewById(R.id.weekly_bar_chart);
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

//        BarDataSet walkingSet ;
//        BarDataSet runningSet;
//        BarDataSet cyclingSet;
//        BarDataSet DrivingSet;
        BarDataSet firstB ;
        BarDataSet secondB ;
        BarDataSet thirdB ;
        BarDataSet fourthB ;
        BarDataSet fifthB ;
        BarDataSet sixthB ;
        BarDataSet seventhB ;



        ArrayList<BarEntry> weeklyWalkTime = new ArrayList<>();
        ArrayList<BarEntry> weeklyRunningTime = new ArrayList<>();
        ArrayList<BarEntry> weeklyCyclingTime = new ArrayList<>();
        ArrayList<BarEntry> weeklyDrivingTime = new ArrayList<>();
        ArrayList<BarEntry> weeklySleepTime = new ArrayList<>();

        int ct;
        weeklySleepTime.clear();
        for(ct = 0; ct<7;ct++){
            weeklySleepTime.add(new BarEntry((float) ((double)Math.random()*640),ct));
        }

        //rewrite here instead of hard code
        ArrayList<BarEntry> first = new ArrayList<>();
        ArrayList<BarEntry> second = new ArrayList<>();
        ArrayList<BarEntry> third = new ArrayList<>();
        ArrayList<BarEntry> fourth = new ArrayList<>();
        ArrayList<BarEntry> fifth = new ArrayList<>();
        ArrayList<BarEntry> sixth = new ArrayList<>();
        ArrayList<BarEntry> seventh = new ArrayList<>();



        boolean walkFlag=false;
        boolean runningFlag=false;
        boolean cyclingFlag=false;
        boolean drivingFlag=false;


        Iterator itr = rawDataSets.iterator();
        int i = 0;
        while(itr.hasNext()){

            ArrayList<String> data = (ArrayList<String>) itr.next();//take out the activity item in a String formart

            if(!walkFlag){
                i=0;
                for (String dataEntry: data){
                    weeklyWalkTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                walkFlag = true;
            }else if(!runningFlag){
                i=0;
                for(String dataEntry: data){
                    weeklyRunningTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                runningFlag=true;
            }else if(!cyclingFlag){
                i=0;
                for(String dataEntry: data){
                    weeklyCyclingTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                cyclingFlag=true;
            }else if(!drivingFlag){
                i=0;
                for(String dataEntry: data){
                    weeklyDrivingTime.add(new BarEntry(Integer.valueOf(dataEntry)/60000,i));
                    i++;
                }
                drivingFlag=true;
            }else{
                Log.i(TAG, "get unexpected data!");
            }
        }

        for(int cnt =0; cnt <7; cnt++){
            if(cnt==0) {
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                first.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                first.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                first.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                first.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                first.add(entry5);
            }else if (cnt ==1){
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                second.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                second.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                second.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                second.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                second.add(entry5);
            }else if (cnt ==2){
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                third.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                third.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                third.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                third.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                third.add(entry5);
            }else if (cnt ==3){
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                fourth.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                fourth.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                fourth.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                fourth.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                fourth.add(entry5);
            }else if (cnt ==4){
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                fifth.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                fifth.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                fifth.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                fifth.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                fifth.add(entry5);
            }else if (cnt ==5){
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                sixth.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                sixth.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                sixth.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                sixth.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                sixth.add(entry5);
            }else if (cnt ==6){
                BarEntry entry1 = weeklyWalkTime.get(cnt);
                entry1.setXIndex(0);
                seventh.add(entry1);

                BarEntry entry2 = weeklyRunningTime.get(cnt);
                entry2.setXIndex(1);
                seventh.add(entry2);

                BarEntry entry3 = weeklyDrivingTime.get(cnt);
                entry3.setXIndex(2);
                seventh.add(entry3);

                BarEntry entry4 = weeklyCyclingTime.get(cnt);
                entry4.setXIndex(3);
                seventh.add(entry4);

                BarEntry entry5 = weeklySleepTime.get(cnt);
                entry5.setXIndex(4);
                seventh.add(entry5);
            }
        }

        firstB = new BarDataSet(first, "Yesterday ");
        firstB.setColors(ColorTemplate.VORDIPLOM_COLORS);
        secondB = new BarDataSet(second, "Yesterday ");
        secondB.setColors(ColorTemplate.VORDIPLOM_COLORS);
        thirdB = new BarDataSet(third, "Yesterday ");
        thirdB.setColors(ColorTemplate.VORDIPLOM_COLORS);
        fourthB = new BarDataSet(fourth, "Yesterday ");
        fourthB.setColors(ColorTemplate.VORDIPLOM_COLORS);
        fifthB = new BarDataSet(fifth, "Yesterday ");
        fifthB.setColors(ColorTemplate.VORDIPLOM_COLORS);
        sixthB = new BarDataSet(sixth, "Yesterday ");
        sixthB.setColors(ColorTemplate.VORDIPLOM_COLORS);
        seventhB = new BarDataSet(seventh, "Yesterday ");
        seventhB.setColors(ColorTemplate.VORDIPLOM_COLORS);


//        Log.i(TAG,walkingSet.toString());
//        Log.i(TAG,runningSet.toString());
//        Log.i(TAG,cyclingSet.toString());
//        Log.i(TAG,DrivingSet.toString());

        Log.i(TAG,firstB.toString());
        Log.i(TAG,secondB.toString());
        Log.i(TAG,thirdB.toString());
        Log.i(TAG,fourthB.toString());
        Log.i(TAG,fifthB.toString());
        Log.i(TAG,sixthB.toString());
        Log.i(TAG, seventhB.toString());


        ArrayList<String> labels = new ArrayList<String>();
        labels.clear();
        labels.add("Walking");
        labels.add("Running");
        labels.add("Driving");
        labels.add("Cycling");
        labels.add("Sleeping");


        Log.i(TAG,labels.toString());


        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.clear();
        dataSets.add(firstB);
        dataSets.add(secondB);
        dataSets.add(thirdB);
        dataSets.add(fourthB);
        dataSets.add(fifthB);
        dataSets.add(sixthB);
        dataSets.add(seventhB);

        BarData data = new BarData(labels,dataSets);
        data.setGroupSpace(0);



        Log.i(TAG,"Successfully create new data");

        return data;
    }
}
