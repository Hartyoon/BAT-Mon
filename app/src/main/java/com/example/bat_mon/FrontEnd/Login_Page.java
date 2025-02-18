package com.example.bat_mon.FrontEnd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bat_mon.R;

public class Login_Page extends AppCompatActivity {

    private int segment;
    SharedPreferences pref_password;
    SharedPreferences pref_login;

    private static final boolean SKIP_LOGIN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent lastIntent = getIntent();
        segment = lastIntent.getIntExtra("segment", 0);
        Toast.makeText(this, "Segment: " + segment, Toast.LENGTH_SHORT).show();

        pref_login = getSharedPreferences("login", MODE_PRIVATE);

        // Skip login
        if (SKIP_LOGIN) {
            pref_login.edit().putBoolean("isLoggedIn", true).apply();
            Intent intent = new Intent(this, CID_Page.class);
            intent.putExtra("segment", segment);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
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

    public void login(View view) {
        //Check if password is correct

        SharedPreferences pref_password=getSharedPreferences("settings",Context.MODE_PRIVATE);
        String saved_password = pref_password.getString("password","123") ;

        EditText password = (EditText) findViewById(R.id.TextPassword);
        String value = password.getText().toString();

        if (value.equals(saved_password)) {
            pref_login.edit().putBoolean("isLoggedIn", true).apply();
            Intent intent = new Intent(this, CID_Page.class);
            intent.putExtra("segment", segment);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Falsches Passwort!", Toast.LENGTH_SHORT).show();
            password.getText().clear();
        }
    }
}