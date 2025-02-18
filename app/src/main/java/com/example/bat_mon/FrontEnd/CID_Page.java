package com.example.bat_mon.FrontEnd;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bat_mon.BackEnd.BatMonError;
import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.Exceptions.BatMonException;
import com.example.bat_mon.FrontEnd.Helpers.VPAdapter;
import com.example.bat_mon.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CID_Page extends AppCompatActivity {

    private ViewPager2 viewPager;
    private VPAdapter vpAdapter;
    private TabLayout tabLayout;
    FragmentManager fragmentManager;
    private int seg;

    private ScheduledExecutorService exec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cid_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent lastIntent = getIntent();
        seg = lastIntent.getIntExtra("segment", 1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Segment " + seg);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = findViewById(R.id.pager);
        tabLayout = findViewById(R.id.tab_layout);
        vpAdapter = new VPAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(vpAdapter);
        fragmentManager = getSupportFragmentManager();

        new TabLayoutMediator(tabLayout, viewPager, (tab, i) -> {
            if (i == 0 || i == 1)
                tab.setText("CID " + (i+1));
            else
                tab.setText("Balancing");
        }).attach();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d("CID-Page-Runnable", "CID page running");

                // Loop through all CID Fragments and update their UI
                List<Fragment> fragments = fragmentManager.getFragments();
                for (Fragment fragment : fragments) {
                    if (fragment instanceof CID_Fragment) {
                        // Since we are going to change UI elements, we have to make sure our code runs on the UI thread.
                        runOnUiThread(() -> ((CID_Fragment) fragment).updateFragmentUI());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ErrorHandler.addError(
                        new BatMonException("CID-Page runnable stopped working", e, BatMonError.ErrorCode.UI_ERROR)
                );
            }
        }
    };

    private void startRunnable() {
        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.SECONDS);
        Log.d("Cell-Page-Runnable", "CID-Page runnable scheduled");
    }

    private void shutdownRunnable() {
        if (exec != null && !exec.isShutdown()) {
            Log.d("Cell-Page-Runnable", "CID-Page runnable shutdown");
            exec.shutdown();
        }
    }

    //Toolbar options
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        shutdownRunnable();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRunnable();
    }

    public int getSegment() {
        return seg;
    }

}