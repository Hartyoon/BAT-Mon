package com.example.bat_mon.BackEnd;


import org.json.JSONException;
import org.json.JSONObject;

public class Cell extends DataHandler {

    // Temp / Volt ranges - When you change these values, you need to adjust the Cell & CID Unit tests!!
    private final static float[] TEMP_RANGES = {
            -0.00002f, // Max low - Below or above will be ignored (sensor is broken)
            -0.00001f,  // Panic low - Below or above will start panic mode
            0.0f,   // Warn low - Below or above will give warning
            45.0f,  // Warn high
            60.0f,  // Panic high
            150.0f  // Max high
    };

    private final static float[] VOLT_RANGES = {
            0.0f,  // Max low - Below or above will be ignored (sensor is broken)
            3.0f,  // Panic low - Below or above will start panic mode
            3.4f,  // Warn low - Below or above will give warning
            4.0f,  // Warn high
            4.2f,  // Panic high
            10.0f   // Max high
    };

//    private final static float[] TEMP_RANGES = {
//            -0.00002f, // Max low - Below or above will be ignored (sensor is broken)
//            -0.00001f,  // Panic low - Below or above will start panic mode
//            20.0f,   // Warn low - Below or above will give warning
//            25.0f,  // Warn high
//            26.0f,  // Panic high
//            150.0f  // Max high
//    };
//
//    private final static float[] VOLT_RANGES = {
//            0.0f,  // Max low - Below or above will be ignored (sensor is broken)
//            3.0f,  // Panic low - Below or above will start panic mode
//            3.72f,  // Warn low - Below or above will give warning
//            4.76f,  // Warn high
//            4.2f,  // Panic high
//            10.0f   // Max high
//    };

    public enum CellStatus {
        Error(-1), OK(0), Low(1), High(2), Panic(3);
        private final int severity;
        CellStatus(int severity) { this.severity = severity; }
        public boolean isWorseThan(CellStatus other) { return this.severity > other.severity; }
    }
    public CellStatus voltStatus = CellStatus.OK;
    public CellStatus tempStatus = CellStatus.OK;

    private final int id;

    private long balancingTime;
    private boolean isBalancing;
    private JSONObject balancingJson = new JSONObject();

    public Cell(int id) {
        this.id = id;
    }

    public void addBalancingTime(long time){
        balancingTime += time;
    }

    @Override
    public void setTemp(float t) {
        super.setTemp(t);
        tempStatus = getCellStatus(TEMP_RANGES, t);
    }

    @Override
    public void setVolt(float v) {
        super.setVolt(v);
        voltStatus = getCellStatus(VOLT_RANGES, v);
    }

    private CellStatus getCellStatus(float[] range, float value) {
        if (value < range[0] || value > range[5])
            return CellStatus.Error;
        else if (value < range[1] || value > range[4])
            return CellStatus.Panic;
        else if (value < range[2])
            return CellStatus.Low;
        else if (value > range[3])
            return CellStatus.High;
        else
            return CellStatus.OK;
    }

    @Override
    public void restoreFromJSON(JSONObject json) throws JSONException {
        super.restoreFromJSON(json);
        balancingTime = json.getLong("balancing");
    }

    @Override
    public synchronized JSONObject getJSON() throws JSONException {
        JSONObject cellJson = super.getJSON();
        cellJson.put("balancing", balancingTime);
        return cellJson;
    }

    public void setBalancing(boolean bool){
        isBalancing = bool;
    }

    public boolean isBalancing() {
        return isBalancing;
    }

    public CellStatus getVoltStatus() {
        return voltStatus;
    }

    public CellStatus getTempStatus() {
        return tempStatus;
    }

    public long getBalancingTime() {
        return balancingTime;
    }

}

