package com.example.bat_mon.BackEnd;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.BackEnd.Utils.FloatTimePair;
import com.example.bat_mon.Exceptions.BatMonException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataHandler {

    protected float temp, voltage;
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS");
    protected JSONArray dataPoints = new JSONArray();
    protected long millis; // Milliseconds since last save
    protected long lastSave; // Time in milliseconds when last save happened
    protected long pollingRate = 1000; // Every second = 1000 millis seconds
    protected List<Float> toBeAveragedVolt = new ArrayList<>(); // Values that will be averaged on next save
    protected List<Float> toBeAveragedTemp = new ArrayList<>(); // Values that will be averaged on next save

    private synchronized void saveValues() {
        if (millis >= pollingRate) { // It's time to save
            millis = 0;
            lastSave = System.currentTimeMillis();

            // Calculate averages
            float t = calcAverage(toBeAveragedTemp);
            float v = calcAverage(toBeAveragedVolt);
            toBeAveragedTemp.clear();
            toBeAveragedVolt.clear();

            // Format time
            LocalDateTime time = LocalDateTime.now();
            String formattedTime = time.format(formatter);

            // Save new JSON entries
            try {
                JSONObject dataPoint = new JSONObject();
                dataPoint.put("time", formattedTime);

                if (Float.isNaN(t))
                    t = 0.0f;
                if (Float.isNaN(v))
                    v = 0.0f;

                v = Math.round(v * 1000.0f) / 1000.0f;
                t = Math.round(t * 100.0f) / 100.0f;

                dataPoint.put("temp", t);
                dataPoint.put("volt", v);
                synchronized (dataPoints) {
                    dataPoints.put(dataPoint);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                ErrorHandler.addError(new BatMonError(
                        "Creating Datapoint failed", Priority.ERROR, ErrorCode.BATMON_INTERNAL_ERROR
                ));
            }
        } else {
            millis += System.currentTimeMillis() - lastSave;
        }
    }

    private float calcAverage(List<Float> toBeAveraged) {
        float avg = 0;
        for (float x : toBeAveraged)
            avg += x;
        avg /= toBeAveraged.size();
        return avg;
    }

    // Get Temp values in time interval
    public synchronized List<FloatTimePair> getTempValues(LocalDateTime startDate, LocalDateTime endDate) {
        return getValues("temp", startDate, endDate);
    }

    // Get voltage values in time interval
    public synchronized List<FloatTimePair> getVoltValues(LocalDateTime startDate, LocalDateTime endDate) {
        return getValues("volt", startDate, endDate);
    }

    private synchronized List<FloatTimePair> getValues(String key, LocalDateTime startDate, LocalDateTime endDate) {
        List<FloatTimePair> values = new ArrayList<>();

        try {
            for (int i = 0; i < dataPoints.length(); i++) {
                JSONObject dataPoint;
                synchronized (dataPoints){
                    dataPoint = dataPoints.getJSONObject(i);
                }
                LocalDateTime timestamp = LocalDateTime.parse(dataPoint.getString("time"), formatter);
                if (timestamp.isAfter(startDate)
                        && timestamp.isBefore(endDate)
                        && dataPoint.has(key)) {
                    values.add(new FloatTimePair(timestamp, (float)dataPoint.getDouble(key)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            ErrorHandler.addError(new BatMonException("Could not read JSON values", e, ErrorCode.LOADING_DATA_FAILED));
        }

        return values;
    }

    public void restoreFromJSON(JSONObject json) throws JSONException {
        dataPoints = json.getJSONArray("dataPoints");
    }

    public synchronized JSONObject getJSON() throws JSONException {
        JSONObject json = new JSONObject();
        synchronized (dataPoints) {
            json.put("dataPoints", dataPoints);
        }
        return json;
    }

    public void setTemp(float t) {
        this.temp = t;
        toBeAveragedTemp.add(t);
    }

    // Update voltage for this cell
    public void setVolt(float v) {
        this.voltage = v;
        toBeAveragedVolt.add(v);
        saveValues(); // Since saveValues() saves both volt and temp, we only call it in setVolt().
    }

    public float getTemp() {
        return this.temp;
    }

    public float getVoltage() {
        return this.voltage;
    }
}
