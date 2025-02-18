package com.example.bat_mon.FrontEnd;

import static com.example.bat_mon.FrontEnd.CID_Fragment.CID_NUMBER;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bat_mon.BackEnd.CID;
import com.example.bat_mon.BackEnd.Data;
import com.example.bat_mon.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Random;

public class Balancing_Fragment extends Fragment {

    private CID cid1Ref, cid2Ref; // Reference to the CID this fragment represents
    private int cid, seg;
    private BarChart balancingChart1, balancingChart2;
    BarDataSet barDataSet;
    private int foregroundColor, labelColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.balancing_fragment, container, false);
        balancingChart1 = view.findViewById(R.id.balancingCID1);
        balancingChart2 = view.findViewById(R.id.balancingCID2);

        CID_Page cidActivity = (CID_Page) getActivity();
        if (cidActivity != null) {
            seg = cidActivity.getSegment();
        }

        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            cid = getArguments().getInt(CID_NUMBER);
        }

        switchColors();

        cid1Ref = Data.segments[seg - 1][0];
        cid2Ref = Data.segments[seg - 1][1];
        setupGraph(balancingChart1, cid1Ref);
        setupGraph(balancingChart2, cid2Ref);
    }

    private void setupGraph(BarChart barChart, CID cidRef) {
        ArrayList<BarEntry> balancingEntries = new ArrayList<>();
        long[] balancingTimes = cidRef.getAllCellBalancingTimes();

        for (int i = 0; i < cidRef.getAllCellBalancingTimes().length; i++) {
//            balancingEntries.add(new BarEntry(i, balancingTimes[i]));

            // Since we don't get any balancing in the test data, we generate a random long to make the balancing graph more presentable
            // TODO: Remove in production
            Random random = new Random();
            long randomLong = 100 + (long) (random.nextDouble() * (1000 - 100));
            balancingEntries.add(new BarEntry(i, randomLong));
        }

        barDataSet = new BarDataSet(balancingEntries, "Balancing time in S");
        barDataSet.setValueTextSize(12.0f);
        barDataSet.setColor(foregroundColor);
        BarData barData = new BarData(barDataSet);

        barDataSet.setValueTextColor(labelColor);
        barChart.setDescription(null);
        barChart.getLegend().setTextColor(labelColor);
        barChart.getAxisLeft().setTextColor(labelColor);
        barChart.getAxisRight().setEnabled(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(11, true);
        xAxis.setTextColor(labelColor);
        xAxis.setDrawGridLines(false);


        // Set data to the chart
        barChart.setData(barData);
    }

    private void switchColors() {
        int defaultNightMode = AppCompatDelegate.getDefaultNightMode();
        boolean nightMode = defaultNightMode == AppCompatDelegate.MODE_NIGHT_YES; // Determine if night mode is explicitly set in settings
        if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            // If we follow system standard, read night mode state from system
            int uiMode = Home_Page.getContext().getResources().getConfiguration().uiMode;
            nightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        }

        // Set colors
        if (nightMode) {
            foregroundColor = ContextCompat.getColor(this.getContext(), R.color.rub_green);
            labelColor = ContextCompat.getColor(this.getContext(), R.color.rub_grey);
        } else {
            foregroundColor = ContextCompat.getColor(this.getContext(), R.color.rub_blue);
            labelColor = ContextCompat.getColor(this.getContext(), R.color.rub_blue);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switchColors();
        balancingChart1 = getView().findViewById(R.id.balancingCID1);
        balancingChart2 = getView().findViewById(R.id.balancingCID2);
        setupGraph(balancingChart1, cid1Ref);
        setupGraph(balancingChart2, cid2Ref);
    }

}
