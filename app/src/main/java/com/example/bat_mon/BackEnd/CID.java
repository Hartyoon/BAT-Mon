package com.example.bat_mon.BackEnd;

import com.example.bat_mon.BackEnd.Cell.CellStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CID extends DataHandler {
    public static final int cellCount = 11;

    private final int seg, cid;
    private final Cell[] cells = new Cell[cellCount];
    private final FaultRegisterManager frManager;


    public CID(int segment, int cid, int firstCellID) {
        for (int cell = 0; cell < cellCount; cell++)
            cells[cell] = new Cell(firstCellID + cell);
        frManager = new FaultRegisterManager(segment, cid);
        this.seg = segment;
        this.cid = cid;
    }

    // Save voltages and temps as flow in Cell Object and then save in Cell Array
    public synchronized void parseCID(String[] dataFrames, int dataPos) {
        int offset = 0;
        for (int cell = 0; cell < cellCount; cell++) {
            // Voltage of every cell
            float voltage = hexToFloat(dataFrames[dataPos + cell]);
            cells[cell].setVolt(voltage);

            // Not every cell has a temp sensor --> if i = 1,3,5,7,9,10
            if (cell % 2 != 0 || cell == 10) {
                float temp = hexToFloat(dataFrames[dataPos + 10 + cell - offset]); // Temp values (6) come after the volt values (11) --> offset 10
                cells[cell].setTemp(temp);
                offset++;
            }
        }

        // Get CID combined voltage and temp
        float temp = hexToFloat(dataFrames[dataPos + 18]);
        float voltage = hexToFloat(dataFrames[dataPos + 19]);
        setTemp(temp);
        setVolt(voltage);

        // 9x 16-bit sized fault registers --> 2 fault registers per data frame
        int[] faultRegisters = new int[9];
        for (int i = 20; i < 25; i++) {
            int regNr = (i - 20) * 2;
            faultRegisters[regNr] = Integer.parseInt(dataFrames[i + dataPos], 16) & 0xFFFF;
            if (regNr + 1 < faultRegisters.length)
                faultRegisters[regNr + 1] = (Integer.parseInt(dataFrames[i + dataPos], 16) & 0xFFFF0000) >> 16;
        }

        frManager.updateFaultRegister(faultRegisters);

        for (int cell = 0; cell < cellCount; cell++) {
            cells[cell].addBalancingTime(frManager.getBalancingTimes()[cell]);
        }
    }

    public long[] getAllCellBalancingTimes() {
        long [] cellBalancings = new long[cellCount];
        for(int cell = 0; cell < cellCount; cell++) {
            cellBalancings[cell] = cells[cell].getBalancingTime();
        }
        return cellBalancings;
    }

    // Convert hex (data frame) to float
    private static float hexToFloat(String hex) {
        Long temp = Long.parseLong(hex, 16);
        return Float.intBitsToFloat(temp.intValue());
    }

    public CellStatus getWorstTempStatus() {
        CellStatus worst = CellStatus.OK;
        for (Cell c : cells) {
            if (c.getTempStatus().isWorseThan(worst))
                worst = c.getTempStatus();
        }
        return worst;
    }

    public CellStatus getWorstVoltStatus() {
        CellStatus worst = CellStatus.OK;
        for (Cell c : cells) {
            if (c.getVoltStatus().isWorseThan(worst))
                worst = c.getVoltStatus();
        }
        return worst;
    }

    @Override
    public void restoreFromJSON(JSONObject json) throws JSONException {
        super.restoreFromJSON(json);

        JSONArray cellsArray = json.getJSONArray("cells");
        for (int i = 0; i < cellsArray.length(); i++) {
            JSONObject cellJson = cellsArray.getJSONObject(i);
            cells[i].restoreFromJSON(cellJson);
        }
    }

    @Override
    public synchronized JSONObject getJSON() throws JSONException {
        JSONObject cidJson = super.getJSON();

        JSONArray cellsJsonArray = new JSONArray();
        for (Cell cell : cells) {
            cellsJsonArray.put(cell.getJSON());
        }

        cidJson.put("cells", cellsJsonArray);
        return cidJson;
    }

    public Cell getCellAt(int pos) {
        return cells[pos];
    }

    public boolean isRegisterPanic() {
        return frManager.isRegisterPanic();
    }

}
