package com.example.bat_mon.FrontEnd;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.CID;
import com.example.bat_mon.BackEnd.Cell.CellStatus;
import com.example.bat_mon.BackEnd.Data;
import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.BatMonApplication;
import com.example.bat_mon.Exceptions.BatMonException;
import com.example.bat_mon.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Pass_Fail_Page extends AppCompatActivity {

    private boolean isText = true;
    private ScheduledExecutorService exec;

    private final List<Button> segmentButtons = new ArrayList<>();
    private final int[] buttonIds = {
            R.id.segment1, R.id.segment2, R.id.segment3,
            R.id.segment4, R.id.segment5, R.id.segment6,
            R.id.segment7, R.id.segment8, R.id.segment9,
            R.id.segment10, R.id.segment11, R.id.segment12
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pass_fail_page);

        // Toolbar
        View view = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupButtons();

        // Start Thread to update UI
        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleWithFixedDelay(updateRunnable, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateButton();
    }

    private void setupButtons() {
        for (int i = 0; i < Data.SEGMENT_COUNT; i++) {
            Button button = findViewById(buttonIds[i]);
            button.setVisibility(View.VISIBLE);
            int finalI = i;
            button.setOnClickListener(v -> openCID(finalI + 1));
            segmentButtons.add(button);
        }
    }

    private void openCID(int segmentNumber) {
        boolean loggedIn = BatMonApplication.getPrefBoolean("login", "isLoggedIn");
        Log.d("Login", "pref " + loggedIn + " panic " + ErrorHandler.isPanicMode());
        if (loggedIn || ErrorHandler.isPanicMode()) {
            Intent intent = new Intent(this, CID_Page.class);
            intent.putExtra("segment", segmentNumber);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, Login_Page.class);
            intent.putExtra("segment", segmentNumber);
            startActivity(intent);
        }
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
        } else if(id == R.id.log) {
            Intent intent = new Intent(this, Log_Page.class);
            startActivity(intent);
        } else if(id == R.id.home_button) {
            Intent intent = new Intent(this, Pass_Fail_Page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exec != null && !exec.isShutdown()) {
            exec.shutdown();
        }
    }

    public void updateButton(){
        for (int i = 0; i < segmentButtons.size(); i++) {
            CID cid1 = Data.segments[i][0];
            CID cid2 = Data.segments[i][1];

            CellStatus tempStatus1 = cid1.getWorstTempStatus();
            CellStatus tempStatus2 = cid2.getWorstTempStatus();
            CellStatus worstTemp = tempStatus1.isWorseThan(tempStatus2) ? tempStatus1 : tempStatus2;

            CellStatus voltStatus1 = cid1.getWorstVoltStatus();
            CellStatus voltStatus2 = cid2.getWorstVoltStatus();
            CellStatus worstVolt = voltStatus1.isWorseThan(voltStatus2) ? voltStatus1 : voltStatus2;

            updateButtonColor(segmentButtons.get(i), worstTemp, worstVolt, i);
        }
        isText = !isText;
    }

    private void updateButtonColor(Button button, CellStatus tempStatus, CellStatus voltStatus, int i) {
        boolean isVolt;
        CellStatus worst; // = tempStatus.isWorseThan(voltStatus) ? tempStatus : voltStatus;
        if (tempStatus.isWorseThan(voltStatus)) {
            isVolt = false;
            worst = tempStatus;
        } else {
            isVolt = true;
            worst = voltStatus;
        }

        switch (worst) {
            case OK:
                button.setBackgroundColor(getColor(R.color.rub_green));
                button.setText(String.valueOf(i+1));
                break;
            case High:
            case Low:
                button.setBackgroundColor(getColor(R.color.orange));
                changeText(button, isVolt, i);
                break;
            case Panic:
                button.setBackgroundColor(getColor(R.color.red));
                changeText(button, isVolt, i);
                break;
            case Error:
                button.setBackgroundColor(getColor(R.color.rub_green));
                button.setText(String.valueOf(i+1));
                break;
        }
    }

    private void changeText(Button button, boolean isVolt, int i) {
        if (isText) {
            if(isVolt) {
                button.setText("⚡");
            } else {
                button.setText("\uD83C\uDF21");
            }
        } else {
            button.setText(String.valueOf(i+1));
        }

    }

    Runnable updateRunnable = () -> {
        try {
            Log.d("Pass-Fail-Page", "Pass-Fail-Running");
            runOnUiThread(this::updateButton);
        } catch (Exception e) {
            e.printStackTrace();
            ErrorHandler.addError(
                new BatMonException("Pass-Fail runnable stopped working", e, ErrorCode.UI_ERROR)
            );
        }
    };

}