package com.example.bat_mon.FrontEnd.CustomWidgets;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.R;

public class BatMonToolbar extends Toolbar {

    private int backgroundColor;
    private Context context;
    private Observer<Boolean> panicModeObserver;
    private ImageView bellIcon;

    public BatMonToolbar(Context context) {
        super(context);
        setup(context);
    }

    public BatMonToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public BatMonToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    private void setup(Context context) {
        Log.d("Toolbar", "Setup");

        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.toolbar, this, true);
        bellIcon = view.findViewById(R.id.bellIcon);
        bellIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ErrorHandler.setPanicMode(false, "Manually disabled");
            }
        });
        changeColor();

        panicModeObserver = this::setPanicMode;
        ErrorHandler.getPanicModeLiveData().observeForever(panicModeObserver);
    }

    public void setPanicMode(boolean panicMode) {
        if (context == null)
            return;

        Log.d("Toolbar", "Set panic mode " + panicMode);
        if (panicMode) {
            setBackgroundColor(ContextCompat.getColor(context, R.color.red));
            bellIcon.setVisibility(View.VISIBLE);
        } else {
            bellIcon.setVisibility(View.GONE);
            setBackgroundColor(backgroundColor);
        }
        Log.d("Toolbar", "Panic mode was set");
    }

    private void changeColor() {
        if (context == null)
            return;

        int uiMode = context.getResources().getConfiguration().uiMode;
        if ((uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            backgroundColor = ContextCompat.getColor(context, R.color.rub_green);
        } else {
            backgroundColor = ContextCompat.getColor(context, R.color.rub_blue);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ErrorHandler.getPanicModeLiveData().removeObserver(panicModeObserver);
    }

}
