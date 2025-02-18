package com.example.bat_mon.FrontEnd.CustomWidgets;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import com.example.bat_mon.BackEnd.Utils.FloatTimePair;
import com.example.bat_mon.FrontEnd.Helpers.CustomMarkerView;
import com.example.bat_mon.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatMonLineChart extends LineChart {

    private String label;
    private String markerDecimalFormat;
    private int chartMaxValues = 100;
    private float yBoundMargin;
    private int labelColor, foregroundColor;
    private Context context;

    public BatMonLineChart(Context context) {
        super(context);
    }

    public BatMonLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BatMonLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setup(Context context, float granularity, String label, float yBoundMargin, String markerDecimalFormat, int labelColor, int foregroundColor) {
        this.context = context;
        this.label = label;
        this.markerDecimalFormat = markerDecimalFormat;
        this.yBoundMargin = yBoundMargin;
        changeLabelColor(labelColor, foregroundColor);

        setDescription(null);
        getAxisRight().setEnabled(false);
        getAxisRight().setDrawAxisLine(true);
        getAxisLeft().setGranularity(granularity);
    }

    public void changeLabelColor(int labelColor, int foregroundColor) {
        this.labelColor = labelColor;
        this.foregroundColor = foregroundColor;
        getAxisLeft().setTextColor(labelColor);
        getXAxis().setTextColor(labelColor);
        getLegend().setTextColor(labelColor);
    }

    private void printList(List<FloatTimePair> values) {
        List<FloatTimePair> testList = values.subList(0, Math.min(values.size(), 20));
        List<Float> testList2 = new ArrayList<>();
        for (FloatTimePair x : testList)
            testList2.add(x.value);
        Log.d("Chart-List", Arrays.toString(testList2.toArray()));
    }

    String v = "";
    private FloatTimePair calcAverage(List<FloatTimePair> toBeAveraged) {
        if (toBeAveraged.isEmpty())
            throw new IllegalArgumentException("toBeAveraged cannot be empty");

        v = "{";
        float avg = 0;
        for (FloatTimePair x : toBeAveraged) {
            v += x.value + ",";
            avg += x.value;
        }
        avg /= toBeAveraged.size();
        v += "}";

        int midIndex = toBeAveraged.size() / 2;
        FloatTimePair newPair = new FloatTimePair(toBeAveraged.get(midIndex).time, avg);

        return newPair;
    }

    private FloatTimePair lastPair; // Previously last pair in values list. We need this to know how many new values have been added since the last update
    private List<FloatTimePair> currentValues = new ArrayList<>(); // All data points the graph currently shows
    private int currentAveraging = 0; // Previously number of values that were averaged. When this changes with new update, we generate all data points completely new
    private ArrayList<FloatTimePair> newValues = new ArrayList<>(); // List of values we haven't averaged yet

    private void updateList(List<FloatTimePair> values) {
        Log.d("Chart","values size: " + values.size());

        int averagedValues = values.size() / chartMaxValues;
        if (currentValues.isEmpty() || averagedValues == 0) { // First time or maxChartValues not reached
            generateNewList(values);
            return;
        }

        if (currentAveraging != averagedValues) { // When the number of values that will be averaged changes, we generate the list completely new
            Log.d("Chart", "Current averaging changed " + currentAveraging + " -> " + averagedValues);
            generateNewList(values);
            currentAveraging = averagedValues;
            return;
        }

        // Add new values to temporary list
        int lastIndex = values.lastIndexOf(lastPair);
        lastPair = values.get(values.size() - 1);
        if (lastIndex != -1) {
            List<FloatTimePair> newValuesToBeAdded = values.subList(lastIndex, values.size() - 1);
            newValues.addAll(newValuesToBeAdded);
            Log.d("Chart", "Last Index: " + lastIndex + " add new values " + Arrays.toString(newValuesToBeAdded.toArray()));
        }

        // Remove oldest values and add new value to current value list, when we have enough values to average
        if (newValues.size() >= currentAveraging) {
            Log.d("Chart", "remove old value and add new average");
            currentValues.remove(0);
            currentValues.add(calcAverage(newValues));
            newValues.clear();
        }
    }

    private void generateNewList(List<FloatTimePair> values) {
        currentValues.clear();
        if (values.size() <= chartMaxValues) {
            Log.d("Chart", "Generate new List -> no averaging");
            currentValues = values;
            return;
        }

        int segmentSize = values.size() / chartMaxValues;
        int remaining = values.size() % chartMaxValues;
        boolean alternatingSegSize = remaining > chartMaxValues / 2;
        Log.d("Chart", "Generate new List -> Averaging with segmentSize: " + segmentSize + " remaining: " + remaining + " alternating: " + alternatingSegSize);
        int from = 0;
        for (int i = 0; i < values.size(); i++) {
            Log.d("Chart", "generateNewList running");
            int currentSegSize = alternatingSegSize ? segmentSize + (i % 2) : segmentSize;
            int to = Math.min(from + currentSegSize, values.size() - 1);
            if (from >= to)
                break;
            List<FloatTimePair> toBeAveraged = values.subList(from, to);
//            if (toBeAveraged.isEmpty())
//                continue;
            Log.d("Chart-Avg", "toBeAveraged size: " + toBeAveraged.size() + " from " + from + " to " + to);
            currentValues.add(calcAverage(toBeAveraged));
            from += currentSegSize;
        }
    }

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public void updateValues(List<FloatTimePair> values) {
        updateList(values);
        values = currentValues;
        Log.d("Chart-List", "current values size: " + currentValues.size());
        printList(values);

        // Add data to chart
        int maxIndex = 0;
        int minIndex = 0;
        List<Entry> entries = new ArrayList<>();
        List<String> xAxisLabels = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            float value = values.get(i).value;
            entries.add(new Entry(i, value));

            // Get index of max and min values
            if (value >= values.get(maxIndex).value)
                maxIndex = i;
            if (value <= values.get(minIndex).value)
                minIndex = i;

            // X Axis labels
            Long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), values.get(i).time);
            xAxisLabels.add(seconds.toString());
        }

        // Prevent Graph from going blank
        if (!entries.isEmpty()) {
            final int finalMaxIndex = maxIndex;
            final int finalMinIndex = minIndex;

            mainHandler.post(() -> updateGraph(entries, xAxisLabels, finalMaxIndex, finalMinIndex));
        }
    }

    private void updateGraph(List<Entry> entries, List<String> xAxisLabels, int maxIndex, int minIndex) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(3);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(foregroundColor);

        // Set highlight options
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(Color.RED);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setHighlightLineWidth(0.5f);

        LineData lineData = new LineData(dataSet);
        lineData.setDrawValues(false);

        // Set Y axis margins
        getAxisLeft().setAxisMaximum(dataSet.getYMax() + yBoundMargin);
        getAxisLeft().setAxisMinimum(dataSet.getYMin() - yBoundMargin);

        getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        getXAxis().setLabelCount(Math.min(10, entries.size()), true);

        setMarker(new CustomMarkerView(context, R.layout.graph_marker_view, markerDecimalFormat));

        setData(lineData);
        invalidate();

        // Highlight max and min values with lines
        highlightValue(maxIndex, 0);
//        highlightValue(minIndex, 0);
    }

}
