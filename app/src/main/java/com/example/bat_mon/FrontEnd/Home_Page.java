package com.example.bat_mon.FrontEnd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.BatMonApplication;
import com.example.bat_mon.R;

import java.util.ArrayList;
import java.util.List;

public class Home_Page extends AppCompatActivity {

    private static Context context;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        context = getApplicationContext();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences preferences= getSharedPreferences("settings", Context.MODE_PRIVATE);
        String theme = preferences.getString("theme","System");
        if (theme.equals("Light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (theme.equals("Dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (theme.equals("System")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        sp = getSharedPreferences("login", MODE_PRIVATE);
        sp.edit().putBoolean("isLoggedIn", false).apply();

        Observer<Boolean> panicModeObserver = this::showContactInformation;
        ErrorHandler.getPanicModeLiveData().observeForever(panicModeObserver);

        getPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set login state to false when we go back to home page
        sp.edit().putBoolean("isLoggedIn", false).apply();
    }

    @SuppressLint("BatteryLife")
    private void getPermissions() {
        // Checks if battery optimization is disabled. If not send request to disable it.
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        List<String> permissionsToRequest = new ArrayList<>();

        // Check SEND_SMS permission
        if (ContextCompat.checkSelfPermission(Home_Page.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS);
        }
        // Check POST_NOTIFICATIONS permission
        if (ContextCompat.checkSelfPermission(Home_Page.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Request permissions
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(Home_Page.this,
                    permissionsToRequest.toArray(new String[0]),
                    0);
        }
    }

    private void showContactInformation(boolean show) {
        ImageView logo = findViewById(R.id.logo);
        ImageView logorub = findViewById(R.id.logorub);
        TextView textView = findViewById(R.id.contactInformation);
        if (show) {
            logo.setVisibility(View.GONE);
            logorub.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        } else {
            logo.setVisibility(View.VISIBLE);
            logorub.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
        }
    }

    public void nextPage(View view) {
        BatMonApplication.startWebsocket();
        Intent intent = new Intent(this, Pass_Fail_Page.class);
        startActivity(intent);
    }

    public static Context getContext() {
        return context;
    }

}