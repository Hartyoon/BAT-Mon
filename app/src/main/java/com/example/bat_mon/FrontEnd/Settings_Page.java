package com.example.bat_mon.FrontEnd;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bat_mon.BatMonApplication;
import com.example.bat_mon.R;


public class Settings_Page extends AppCompatActivity{
    Button button_password, button_change;

    boolean alarmIsOn, smsIsOn, emailIsOn;
    RadioGroup radioGroup;
    RadioButton radioLight;
    RadioButton radioDark;
    RadioButton radioSystem;
    String mode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings_page);

        EdgeToEdge.enable(this);

        //setContentView(R.layout.activity_settings_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Settings");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        radioGroup= findViewById(R.id.RGroup);

        button_password=findViewById(R.id.password_button);
        button_change = findViewById(R.id.buttonChangeInfo);

        SharedPreferences preferences= getSharedPreferences("settings", Context.MODE_PRIVATE);

        EditText alarmTime= findViewById(R.id.editTextAlarmTimeout);
        EditText mail_1= findViewById(R.id.editTextTextEmailAddress1);
        EditText mail_2= findViewById(R.id.editTextTextEmailAddress2);
        EditText phone_1= findViewById(R.id.editTextPhone1);
        EditText phone_2= findViewById(R.id.editTextPhone2);
        EditText mailServerAddr= findViewById(R.id.editTextServerMailAddress);
        EditText mailServerPsw= findViewById(R.id.editTextServerMailPassword);
        EditText smtpHost= findViewById(R.id.editTextSMTPHost);
        EditText smtpPort= findViewById(R.id.editTextSMTPPort);
        EditText tsacUrl=  findViewById(R.id.editTextUrl);
        EditText tsacUser= findViewById(R.id.editTextUser);
        EditText tsacPassword= findViewById(R.id.editTextTsacPassword);

        radioLight= findViewById(R.id.radioLight);
        radioDark= findViewById(R.id.radioDark);
        radioSystem=findViewById(R.id.radioSystem);

        mode=preferences.getString("theme","System");
        if(mode.equals("Light")){
            radioLight.setChecked(true);
        }
        else if (mode.equals("Dark")){
            radioDark.setChecked(true);
        }
        else if (mode.equals("System")){
            radioSystem.setChecked(true);
        }

        alarmTime.setText(preferences.getString("alarmTime","3"));
        mail_1.setText(preferences.getString("email1", ""));
        mail_2.setText(preferences.getString("email2",""));
        phone_1.setText(preferences.getString("phone1",""));
        phone_2.setText(preferences.getString("phone2",""));
        mailServerAddr.setText((preferences.getString("MailServerAddr","BatmonApp@gmail.com")));
        mailServerPsw.setText(preferences.getString("MailServerPsw","cfod dtza wkvc tecx"));
        smtpHost.setText(preferences.getString("MailHost","smtp.gmail.com"));
        smtpPort.setText(preferences.getString("MailPort","465"));
        tsacUrl.setText(preferences.getString("url","https://tsac.rubmotorsport.de/live"));
        tsacUser.setText(preferences.getString("user","selab"));
        tsacPassword.setText(preferences.getString("tsacPw","DawQDtwyzzrw"));

        Switch alarm = findViewById(R.id.alarmSwitch);
        Switch sms = findViewById(R.id.smsSwitch);
        Switch email= findViewById(R.id.emailSwitch);

        alarm.setChecked(preferences.getBoolean("alarm", true));
        sms.setChecked(preferences.getBoolean("sms", true));
        email.setChecked(preferences.getBoolean("email", true));

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioDark) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    mode= "Dark";
                } else if (checkedId == R.id.radioLight) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    mode= "Light";

                } else if (checkedId == R.id.radioSystem) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    mode= "System";

                }
                SharedPreferences.Editor edit= preferences.edit();
                edit.putString("theme", mode);
                edit.apply();

            }
        });

        alarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this,"Changed Switch",Toast.LENGTH_SHORT).show();
            //Saves State of the alarm switch
                SharedPreferences.Editor edit= preferences.edit();
                edit.putBoolean("alarm", isChecked);
                edit.apply();
                alarmIsOn= isChecked;
            }
        );
        sms.setOnCheckedChangeListener((buttonView, isChecked) -> {

                    //Saves State of the sms switch
                    SharedPreferences.Editor edit= preferences.edit();
                    edit.putBoolean("sms", isChecked);
                    edit.apply();
                    smsIsOn= isChecked;
                }
        );
        email.setOnCheckedChangeListener((buttonView, isChecked) -> {

                    //Saves State of the email switch
                    SharedPreferences.Editor edit= preferences.edit();
                    edit.putBoolean("email", isChecked);
                    edit.apply();

                    emailIsOn= isChecked;
                }
        );

    }

    public void changePassword(View v0){
        SharedPreferences pref_settings = getSharedPreferences("settings",Context.MODE_PRIVATE);

        String old = pref_settings.getString("password","123");

        EditText old_password = findViewById(R.id.editTextTextPasswordold);
        EditText new_password = findViewById(R.id.editTextTextPasswordnew);

        String value=old_password.getText().toString();

        if(value.equals(old)) {
            // Save new Password
            SharedPreferences.Editor editor = pref_settings.edit();
            editor.putString("password", new_password.getText().toString());
            editor.apply();
            Toast.makeText(this,"Password Changed", Toast.LENGTH_SHORT).show();

        }
        else Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();

        old_password.getText().clear();
        new_password.getText().clear();
    }

    public void applyChanges(View v0) {
        SharedPreferences preferences= getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        //Change contacts for notification
        EditText alarmTimeout = findViewById(R.id.editTextAlarmTimeout);
        EditText mail_1= findViewById(R.id.editTextTextEmailAddress1);
        EditText mail_2= findViewById(R.id.editTextTextEmailAddress2);
        EditText phone_1= findViewById(R.id.editTextPhone1);
        EditText phone_2= findViewById(R.id.editTextPhone2);

        String alarmTimeoutValue = alarmTimeout.getText().toString();
        String receiver_mail1 = mail_1.getText().toString();
        String receiver_mail2 = mail_2.getText().toString();
        String receiver_phone1 = phone_1.getText().toString();
        String receiver_phone2 = phone_2.getText().toString();

        editor.putString("alarmTime", alarmTimeoutValue);
        editor.putString("email1", receiver_mail1);
        editor.putString("email2", receiver_mail2);
        editor.putString("phone1", receiver_phone1);
        editor.putString("phone2", receiver_phone2);

        //change Mail-Server Information
        EditText mailServerAddr= findViewById(R.id.editTextServerMailAddress);
        EditText mailServerPsw= findViewById(R.id.editTextServerMailPassword);
        EditText smtpHost= findViewById(R.id.editTextSMTPHost);
        EditText smtpPort= findViewById(R.id.editTextSMTPPort);

        String SenderMail= mailServerAddr.getText().toString();
        String ServerPsw= mailServerPsw.getText().toString();
        String host =smtpHost.getText().toString();
        String port= smtpPort.getText().toString();

        editor.putString("MailServerAddr",SenderMail);
        editor.putString("MailServerPsw", ServerPsw);
        editor.putString("MailHost",host);
        editor.putString("MailPort", port);

        //Change TSAC Server Info

        EditText tsacUrl=  findViewById(R.id.editTextUrl);
        EditText tsacUser= findViewById(R.id.editTextUser);
        EditText tsacPassword= findViewById(R.id.editTextTsacPassword);

        String url = tsacUrl.getText().toString();
        String user = tsacUser.getText().toString();
        String  tsacPw= tsacPassword.getText().toString();

        editor.putString("url",url);
        editor.putString("user", user);
        editor.putString("tsacPw",tsacPw);

        editor.apply();

        BatMonApplication.startWebsocket();

        Toast.makeText(this, "Saved Changes", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}

