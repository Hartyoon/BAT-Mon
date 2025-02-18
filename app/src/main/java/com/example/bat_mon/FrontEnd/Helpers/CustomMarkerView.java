package com.example.bat_mon.FrontEnd.Helpers;

import android.content.Context;
import android.widget.TextView;

import com.example.bat_mon.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.DecimalFormat;

public class CustomMarkerView extends MarkerView {

    private final TextView tvContent;
    private final DecimalFormat format;

    public CustomMarkerView(Context context, int layoutResource, String decimalFormat) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        format = new DecimalFormat(decimalFormat); // Format for displaying values
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvContent.setText(format.format(e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }

}
