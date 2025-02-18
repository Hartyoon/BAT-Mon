package com.example.bat_mon.FrontEnd;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.Cell;
import com.example.bat_mon.BackEnd.Cell.CellStatus;
import com.example.bat_mon.BackEnd.Data;
import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.BackEnd.Utils.FloatTimePair;
import com.example.bat_mon.Exceptions.BatMonException;
import com.example.bat_mon.FrontEnd.CustomWidgets.BatMonLineChart;
import com.example.bat_mon.FrontEnd.CustomWidgets.BatMonTimeSpinner;
import com.example.bat_mon.R;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Cell_Page extends AppCompatActivity {
    private Cell cellRef; // Reference to the actual cell, this page represents
    private TextView tempText, voltText, balancingText;
    private BatMonLineChart tempChart, voltChart;
    private BatMonTimeSpinner spinner;
    private int foregroundColor, labelColor;
    private ScheduledExecutorService exec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cell_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get Intent
        Intent lastIntent = getIntent();
        int seg = lastIntent.getIntExtra("segment", 1);
        int cid = lastIntent.getIntExtra("cid_number", 1);
        int cell = lastIntent.getIntExtra("cell", 1);

        // TODO: When no data is received on app startup, this throws an exception
        cellRef = Data.segments[seg - 1][cid - 1].getCellAt(cell - 1);

        setupUI(seg, cid, cell);

        // Start Graph Thread
        startRunnable();
    }

    private void setupUI(int seg, int cid, int cell) {
        // Get current cell status
        tempText = findViewById(R.id.temperaturtext);
        voltText = findViewById(R.id.voltagetext);
        balancingText = findViewById(R.id.balancing);
        TextView cellTitleText = findViewById(R.id.cellNumber);
        cellTitleText.setText("Cell " + cell);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Seg " + seg + " CID " + cid);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Apply dark / light mode
        switchColors();

        //TODO: add this on CID PAGE

        // Set up Graphs
        tempChart  = findViewById(R.id.chartTemp);
        tempChart.setup(this,
                0.1f,
                "Temperature",
                1f,
                "##0.00",
                 labelColor, foregroundColor);
        voltChart = findViewById(R.id.chartVolt);
        voltChart.setup(this,
                0.001f,
                "Voltage",
                0.01f,
                "##0.000",
                labelColor, foregroundColor);

        // Time span drop down set up
        spinner = findViewById(R.id.spinner);
    }

    // Adjust foreground and label colors depending on dark or light mode
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
            foregroundColor = ContextCompat.getColor(this, R.color.rub_green);
            labelColor = ContextCompat.getColor(this, R.color.rub_grey);
        } else {
            foregroundColor = ContextCompat.getColor(this, R.color.rub_blue);
            labelColor = ContextCompat.getColor(this, R.color.rub_blue);
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d("Cell-Page-Runnable", "Cell-Page running");

                /* Overview */

                // Volt
                float volt = cellRef.getVoltage();
                String text = "Voltage: " + String.format("%.3f", volt) + " V";
                updateText(voltText, text, cellRef.getVoltStatus());

                // Temp
                float temp = cellRef.getTemp();
                text = "Temp: " + String.format("%.2f", temp) + " °C";
                updateText(tempText, text, cellRef.getTempStatus());

                //Balancing
                if(cellRef.isBalancing()) { text = "Balancing: Yes"; }
                else { text = "Balancing: No"; }
                text += " | Total Time: " + cellRef.getBalancingTime();
                balancingText.setText(text);

                /* Graphs */

                // Get Data
                LocalDateTime end = LocalDateTime.now();
                LocalDateTime start = end.minusMinutes(spinner.getSelectedTimeSpan().getMinutes());
                List<FloatTimePair> voltValues = cellRef.getVoltValues(start, end);
                List<FloatTimePair> tempValues = cellRef.getTempValues(start, end);

                // Update Graph
                if (cellRef.getVoltage() != 0.0f)
                    voltChart.updateValues(voltValues);
                if (cellRef.getTemp() != 0.0f)
                    tempChart.updateValues(tempValues);
            } catch (Exception e) {
                e.printStackTrace();
                ErrorHandler.addError(
                        new BatMonException("Cell-Page runnable stopped working", e, ErrorCode.UI_ERROR)
                );
            }
        }
    };

    private void startRunnable() {
        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.SECONDS);
        Log.d("Cell-Page-Runnable", "Cell-Page runnable scheduled");
    }

    private void shutdownRunnable() {
        if (exec != null && !exec.isShutdown()) {
            Log.d("Cell-Page-Runnable", "Cell-Page runnable shutdown");
            exec.shutdown();
        }
    }

    // TODO: Color should properly defined in an xml file
    private void updateText(TextView view, String text, CellStatus status) {
        switch (status) {
            case OK:
                view.setTextColor(labelColor);
                break;
            case High:
                view.setTextColor(ContextCompat.getColor(this, R.color.orange));
                text += " High";
                break;
            case Low:
                view.setTextColor(ContextCompat.getColor(this, R.color.orange));
                text += " Low";
                break;
            case Panic:
                view.setTextColor(ContextCompat.getColor(this, R.color.red));
                text += " PANIC";
                break;
            case Error:
                view.setTextColor(labelColor);
                text += " (ERROR)";
                break;
        }

        view.setText(text);
    }

    // Callback for dark mode
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switchColors();
        tempChart.changeLabelColor(labelColor, foregroundColor);
        voltChart.changeLabelColor(labelColor, foregroundColor);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Shut down runnable when activity ends
        shutdownRunnable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    // Toolbar options
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent intent = new Intent(this, Settings_Page.class);
            startActivity(intent);
        } else if (id == R.id.log) {
            Intent intent = new Intent(this, Log_Page.class);
            startActivity(intent);
        } else if (id == R.id.home_button) {
            Intent intent = new Intent(this, Pass_Fail_Page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
