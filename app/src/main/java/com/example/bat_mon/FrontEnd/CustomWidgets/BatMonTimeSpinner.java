package com.example.bat_mon.FrontEnd.CustomWidgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.example.bat_mon.R;

public class BatMonTimeSpinner extends androidx.appcompat.widget.AppCompatSpinner {

    public enum TimeSpan {
        Minute_1(1), Minutes_2(2), Minutes_5(5), Minutes_30(30),
        Hours_1(60), Hours_2(120), Hours_5(300), Hours_12(720), Hours_24(1440);
        private final int minutes;

        TimeSpan(int minutes) {
            this.minutes = minutes;
        }

        public int getMinutes() {
            return minutes;
        }
    }
    private TimeSpan selectedTimeSpan = TimeSpan.Minute_1;

    public BatMonTimeSpinner(@NonNull Context context) {
        super(context);
        setup(context);
    }

    public BatMonTimeSpinner(@NonNull Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setup(context);
    }

    private void setup(Context context) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.timeSpanDropDown, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(adapter);

        setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Log.d("Cell-Page", "Drop down new item selected pos: " + position);
                selectedTimeSpan = TimeSpan.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedTimeSpan = TimeSpan.Minute_1; // Default is 1 Minute
            }
        });
    }

    public TimeSpan getSelectedTimeSpan() {
        return selectedTimeSpan;
    }

}
