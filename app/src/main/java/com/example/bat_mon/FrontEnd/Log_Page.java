package com.example.bat_mon.FrontEnd;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bat_mon.BackEnd.BatMonError;
import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.R;

import java.util.ArrayList;



public class Log_Page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Log");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView log=findViewById(R.id.Log);
        log.setText(ErrorHandler.listToString());

    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent intent = new Intent(this, Settings_Page.class);
            startActivity(intent);
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void filterLog(View v0){
        ArrayList<BatMonError> allErrors=ErrorHandler.getErrorList();
        EditText text =findViewById(R.id.editFilter);
        TextView log=findViewById(R.id.Log);
        String filter = text.getText().toString();

        //convert BatMonError Array to String Array
        ArrayList<String> errorString = new ArrayList<>();
        for(BatMonError error : allErrors){
            errorString.add(error.toString());
        }
        //deletes all entries which do not contain the filter word
        errorString.removeIf(s -> !(s.toLowerCase().contains(filter.toLowerCase())));

        String res="";
        for(String string : errorString){
            res+= string + "\n\n";
        }
        log.setText(res);
    }
}