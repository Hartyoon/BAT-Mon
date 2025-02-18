package com.example.bat_mon.FrontEnd;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bat_mon.BackEnd.BatMonError;
import com.example.bat_mon.BackEnd.CID;
import com.example.bat_mon.BackEnd.Cell;
import com.example.bat_mon.BackEnd.Data;
import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.BackEnd.Utils.FloatTimePair;
import com.example.bat_mon.FrontEnd.CustomWidgets.BatMonLineChart;
import com.example.bat_mon.FrontEnd.CustomWidgets.BatMonTimeSpinner;
import com.example.bat_mon.R;

import java.time.LocalDateTime;
import java.util.List;

public class CID_Fragment extends Fragment {

    private CID cidRef; // Reference to the CID this fragment represents
    private int cid, seg;
    private LinearLayout[] buttons = new LinearLayout[11];
    private LinearLayout buttonsContainer;
    private TextView cidVoltText, cidTempText;

    public static final String CID_NUMBER = "cid_number";
    private BatMonLineChart tempChart, voltChart;
    private BatMonTimeSpinner spinner;
    private int foregroundColor, labelColor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cid1, container, false);
        buttonsContainer = view.findViewById(R.id.buttonsContainer);
        generateButtons(inflater);
        CID_Page cidActivity = (CID_Page) getActivity();
        if (cidActivity != null) {
            seg = cidActivity.getSegment();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            cid = getArguments().getInt(CID_NUMBER);
        }
        this.cidRef = Data.segments[seg - 1][cid - 1];

        cidVoltText = getView().findViewById(R.id.cidVoltText);
        cidTempText = getView().findViewById(R.id.cidTempText);
        spinner = getView().findViewById(R.id.spinner);

        switchColors();

        // Set up Graphs
        tempChart = getView().findViewById(R.id.chartTemp);
        tempChart.setup(getContext(),
                0.1f,
                "Temperature",
                1f,
                "##0.00",
                labelColor, foregroundColor);
        voltChart = getView().findViewById(R.id.chartVolt);
        voltChart.setup(getContext(),
                0.001f,
                "Voltage",
                0.01f,
                "##0.000",
                labelColor, foregroundColor);

        updateFragmentUI();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void generateButtons(LayoutInflater inflater) {
        Context context = getContext();
        if (context == null) {
            ErrorHandler.addError(new BatMonError(
                    "Could not generate CID-Fragment Buttons: Context null",
                    BatMonError.Priority.ERROR,
                    BatMonError.ErrorCode.UI_ERROR
            ));
            return;
        }

        for (int i = 1; i <= CID.cellCount; i++) {
            LinearLayout newButton = (LinearLayout) inflater.inflate(R.layout.cell_button, buttonsContainer, false);

            // Update the text of the cloned button
            TextView cellText = newButton.findViewById(R.id.cellNumText);
            cellText.setText("A" + (i < 10 ? "0" + i : i));

            final int cellIndex = i; // to use inside the OnClickListener
            newButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), Cell_Page.class);
                intent.putExtra("segment", seg);
                intent.putExtra("cid_number", cid);
                intent.putExtra("cell", cellIndex);
                startActivity(intent);
            });

            buttons[i - 1] = newButton;
            buttonsContainer.addView(newButton);
        }
    }

    // Must be run from UI Thread!
    public void updateFragmentUI() {
        if (cidRef == null)
            return;

        // Asynchroner Task für das Abrufen der Daten
        new Thread(() -> {
            Log.d("CID-Fragment", "Values updated");

            // Volt und Temp Werte abrufen
            float volt = cidRef.getVoltage();
            float temp = cidRef.getTemp();

            // Daten für die Diagramme abrufen
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusMinutes(spinner.getSelectedTimeSpan().getMinutes());
            List<FloatTimePair> voltValues = cidRef.getVoltValues(start, end);
            List<FloatTimePair> tempValues = cidRef.getTempValues(start, end);

            Log.d("CID-Fragment", "Volts :" + voltValues.size() + " Temps: " + tempValues.size());

            // UI-Aktualisierungen im UI-Thread
            try {
                requireActivity().runOnUiThread(() -> {
                    // updateCellValues im Hauptthread ausführen
                    updateCellValues();

                    // UI-Elemente aktualisieren
                    String voltText = "Voltage: " + String.format("%.3f", volt) + " V";
                    cidVoltText.setText(voltText);

                    String tempText = "Temp: " + String.format("%.2f", temp) + " °C";
                    cidTempText.setText(tempText);

                    // Diagramme aktualisieren, wenn Werte vorhanden sind
                    if (volt != 0.0f)
                        voltChart.updateValues(voltValues);
                    if (temp != 0.0f)
                        tempChart.updateValues(tempValues);
                });
            } catch (IllegalStateException | NullPointerException e) {
                e.printStackTrace();
                // No time to fix this properly
            }
        }).start();
    }

    public void updateCellValues() {
        try {
            getActivity().runOnUiThread(() -> {
                String s = "";
                for (int i = 0; i < buttons.length; i++) {
                    Cell.CellStatus tempStatus = cidRef.getCellAt(i).getTempStatus();
                    Cell.CellStatus voltStatus = cidRef.getCellAt(i).getVoltStatus();

                    // Update voltage textView
                    float voltage = cidRef.getCellAt(i).getVoltage();
                    s = voltStatus == Cell.CellStatus.Error ? "ERROR" : String.format("%.3f", voltage) + "V";
                    TextView voltText = (TextView) buttons[i].getChildAt(1);
                    voltText.setText(s);

                    // Update temp textView
                    float temp = cidRef.getCellAt(i).getTemp();
                    s = tempStatus == Cell.CellStatus.Error ? "ERROR" : String.format("%.1f", temp) + "°C";
                    if (temp == 0.0f) s = ""; // When temperature is exactly 0, we assume the cell does not have a sensor
                    TextView tempText = (TextView) buttons[i].getChildAt(2);
                    tempText.setText(s);

                    updateButtonColor(buttons[i], tempStatus, voltStatus);
                }
            });
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
            // No time to fix this properly
        }
    }

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
            foregroundColor = ContextCompat.getColor(this.getContext(), R.color.rub_green);
            labelColor = ContextCompat.getColor(this.getContext(), R.color.rub_grey);
        } else {
            foregroundColor = ContextCompat.getColor(this.getContext(), R.color.rub_blue);
            labelColor = ContextCompat.getColor(this.getContext(), R.color.rub_blue);
        }
    }

    private void updateButtonColor(LinearLayout button, Cell.CellStatus tempStatus, Cell.CellStatus voltStatus) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        Cell.CellStatus worst = tempStatus.isWorseThan(voltStatus) ? tempStatus : voltStatus;
        switch (worst) {
            case OK:
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.rub_green));
                break;
            case High:
            case Low:
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.orange));
                break;
            case Panic:
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
                break;
            case Error:
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.rub_green));
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switchColors();
        tempChart.changeLabelColor(labelColor, foregroundColor);
        voltChart.changeLabelColor(labelColor, foregroundColor);
    }

}
