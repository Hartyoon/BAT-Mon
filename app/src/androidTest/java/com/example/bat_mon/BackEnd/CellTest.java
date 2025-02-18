package com.example.bat_mon.BackEnd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.bat_mon.BackEnd.Utils.FloatTimePair;
import com.example.bat_mon.Exceptions.BatMonException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CellTest {

    private Cell cell;

    @Before
    public void setUp() {
        cell = new Cell(69);
    }

    @Test
    public void testSetTemp() {
        float tempValue = 25.0f;
        cell.setTemp(tempValue);
        JSONObject tempJson = null;
        try {
            tempJson = cell.getJSON().getJSONObject("temperature");
        } catch (Exception e) {
            Log.d("Cell", "Test setTemp failed");
        }

        assertTrue("Temperature JSON should have entries.", tempJson.length() > 0);
        try {
            assertEquals("Temperature value should be saved in JSON.", tempValue, tempJson.getDouble(LocalDateTime.now().format(Cell.formatter)), 0.001);
        } catch (Exception e) {
            Log.d("Cell", "assertFailed setTemp");
        }
        assertEquals("Temperature status should be OK.", Cell.CellStatus.OK, cell.getTempStatus());
    }

    @Test
    public void testSetVolt() {
        float voltValue = 3.8f;
        cell.setVolt(voltValue);
        JSONObject voltJson = null;
        try {
            voltJson = cell.getJSON().getJSONObject("voltage");
        } catch (Exception e) {
            Log.d("Cell", "Test setVolt failed");
        }

        assertTrue("Voltage JSON should have entries.", voltJson.length() > 0);
        try {
            assertEquals("Voltage value should be saved in JSON.", voltValue, voltJson.getDouble(LocalDateTime.now().format(Cell.formatter)), 0.001);
        } catch (Exception e) {
            Log.d("Cell", "assertFailed setvolt");
        }
        assertEquals("Voltage status should be OK.", Cell.CellStatus.OK, cell.getVoltStatus());
    }

    @Test
    public void testUpdateCellJSON() throws BatMonException {
        JSONObject newValues = new JSONObject();
        JSONObject tempValues = new JSONObject();
        JSONObject voltValues = new JSONObject();

        try {
            tempValues.put("2024-07-26T10:00:00.00", 22.5);
            voltValues.put("2024-07-26T10:00:00.00", 3.7);

            newValues.put("temperature", tempValues);
            newValues.put("voltage", voltValues);
        } catch (Exception e) {
            Log.d("Cell", "Test updateJSON failed");
        }

        cell.updateJSON(newValues);

        try {
            assertEquals("Temperature JSON should be updated with new values.", 22.5, cell.getJSON().getJSONObject("temperature").getDouble("2024-07-26T10:00:00.00"), 0.001);
        } catch (Exception e) {
            Log.d("Cell", "assertFailed update");
        }
        try {
            assertEquals("Voltage JSON should be updated with new values.", 3.7, cell.getJSON().getJSONObject("voltage").getDouble("2024-07-26T10:00:00.00"), 0.001);
        } catch (Exception e) {
            Log.d("Cell", "assertFailed update");
        }
    }

    @Test
    public void testGetTempValues() {
        LocalDateTime now = LocalDateTime.now();
        cell.setTemp(25.0f);
        cell.setTemp(30.0f);
        cell.setTemp(35.0f);

        List<FloatTimePair> tempValues = cell.getTempValues(now.minusSeconds(50), now.plusSeconds(50));
        assertEquals(tempValues.toString(), 3, tempValues.size());
        assertEquals("Temperature values list size should be 3.", 3, tempValues.size());
        assertTrue("Temperature values should contain 25.0.", tempValues.contains(25.0f));
        assertTrue("Temperature values should contain 30.0.", tempValues.contains(30.0f));
        assertTrue("Temperature values should contain 35.0.", tempValues.contains(35.0f));
    }

    @Test
    public void testGetVoltValues() {
        LocalDateTime now = LocalDateTime.now();
        cell.setVolt(3.6f);
        cell.setVolt(3.8f);
        cell.setVolt(4.0f);

        List<FloatTimePair> voltValues = cell.getVoltValues(now.minusSeconds(10), now.plusSeconds(10));

        assertEquals(voltValues.toString(), 3, voltValues.size());
        assertEquals("Voltage values list size should be 3.", 3, voltValues.size());
        assertTrue("Voltage values should contain 3.6.", voltValues.contains(3.6f));
        assertTrue("Voltage values should contain 3.8.", voltValues.contains(3.8f));
        assertTrue("Voltage values should contain 4.0.", voltValues.contains(4.0f));
    }

    @Test
    public void addBalancingTimes_isCorrect(){
        long balancingTime1 = 2;
        long balancingTime2 = 1;

        cell.addBalancingTime(balancingTime1);
        cell.addBalancingTime(balancingTime2);

        assertEquals("balancingTime values should contain 3",cell.getBalancingTime(),3);
    }

    public void testSetBal() {
        long balTime1 = 1;
        cell.addBalancingTime(balTime1);
        JSONObject balJson = null;
        try {
            balJson = cell.getJSON().getJSONObject("balancing");
        } catch (Exception e) {
            Log.d("Cell", "Test setVolt failed");
        }

        assertTrue("Voltage JSON should have entries.", balJson.length() > 0);
        try {
            assertEquals("Voltage value should be saved in JSON.", balTime1, balJson.getDouble(LocalDateTime.now().format(Cell.formatter)), 0.001);
        } catch (Exception e) {
            Log.d("Cell", "assertFailed setvolt");
        }
        assertEquals("Voltage status should be OK.", Cell.CellStatus.OK, cell.getVoltStatus());
    }

}

