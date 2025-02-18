package com.example.bat_mon.BackEnd;

import android.util.Log;

import com.example.bat_mon.BackEnd.Utils.JSONUtils;
import com.example.bat_mon.Exceptions.BatMonException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


// Class to request, convert and save Data from server
public class Data {

    public final static String DATA_JSON_FILE = "values.json";

    public static final int SEGMENT_COUNT = 12;
    public static final int CID_COUNT = 2;

    public static CID[][] segments = new CID[SEGMENT_COUNT][CID_COUNT];

    private static boolean registerPanic;

    // Init Segments array with cells
    public static void init() {
        int firstCellID = 0;
        for (int seg = 0; seg < SEGMENT_COUNT; seg++) {
            for (int cid = 0; cid < CID_COUNT; cid++) {
                segments[seg][cid] = new CID(seg, cid, firstCellID);
                firstCellID += CID.cellCount;
            }
        }
    }

    // Function to parse and decode Hex data frames to usable data
    private static boolean registerPanicLastTime = false;
    public static synchronized void parseData(String[] dataFrames) {
        registerPanic = false;
        int dataPos = 1; // Offset from which the dataFrames are parsed in CID.parseCID(). Starts at 1 because 0 is data stream length
        for (int seg = 0; seg < SEGMENT_COUNT; seg++) {
            for (int cid = 0; cid < CID_COUNT; cid++) {
                CID cidRef = segments[seg][cid];
                cidRef.parseCID(dataFrames, dataPos);
                if (cidRef.isRegisterPanic())
                    registerPanic = true;
                dataPos += 25; // 25 dataFrames for every CID
            }
        }

        // FR will enable panic mode, but only once. They won't disable panic mode automatically. You have to do it manually.
        if (registerPanicLastTime != registerPanic && registerPanic) {
            ErrorHandler.setPanicMode(true, "Fault registers are panicking");
            registerPanicLastTime = registerPanic;
        }
    }

    public static synchronized JSONObject getAllCIDsAsJSON() throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray segmentsArray = new JSONArray();

        for (int seg = 0; seg < SEGMENT_COUNT; seg++) {
            JSONArray cidArray = new JSONArray();
            for (int cid = 0; cid < CID_COUNT; cid++) {
                cidArray.put(segments[seg][cid].getJSON());
            }
            JSONObject segmentObject = new JSONObject();
            segmentObject.put("segment_" + (seg + 1), cidArray);
            segmentsArray.put(segmentObject);
        }

        result.put("segments", segmentsArray);
        return result;
    }

    public static void loadDataFromJSONFile() throws BatMonException {
        Log.d("Data-Loading", "Loading data");
        try {
            // Load data from json
            String jsonString = JSONUtils.loadJSONFromFile(DATA_JSON_FILE);
            JSONObject allCIDsJson = new JSONObject(jsonString);

            // Restore data
            JSONArray segmentsArray = allCIDsJson.getJSONArray("segments");
            for (int i = 0; i < segmentsArray.length(); i++) {
                JSONObject segmentObject = segmentsArray.getJSONObject(i);
                String segmentKey = "segment_" + (i + 1);
                JSONArray cidArray = segmentObject.getJSONArray(segmentKey);
                Log.d("Data-Loading", "Segment: " + segmentKey);

                for (int j = 0; j < cidArray.length(); j++) {
                    Log.d("Data-Loading", "CID: " + j);
                    JSONObject cidJson = cidArray.getJSONObject(j);
                    segments[i][j].restoreFromJSON(cidJson);
                }
            }

            Log.d("Data-Loading", "Done");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new BatMonException("Could not parse JSON data", e, BatMonError.ErrorCode.LOADING_DATA_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BatMonException("Could not load JSON file from storage", e, BatMonError.ErrorCode.LOADING_DATA_FAILED);
        }
    }

    public static synchronized void saveDataJSONToFile() throws BatMonException {
        try {
            // Make deep-copy of JSON array to prevent ConcurrentModification exceptions
            JSONObject allCIDsJson = new JSONObject(Data.getAllCIDsAsJSON().toString());
            String jsonString = allCIDsJson.toString();
            JSONUtils.saveJSONToFile(DATA_JSON_FILE, jsonString);
        } catch (JSONException | OutOfMemoryError e) {
            e.printStackTrace();
            throw new BatMonException("Could not create JSON Array from data", e, BatMonError.ErrorCode.SAVING_DATA_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
            ErrorHandler.addError(new BatMonException("Could not save JSON Array to storage", e, BatMonError.ErrorCode.SAVING_DATA_FAILED));
        }
    }

    public static boolean isRegisterPanic() {
        return registerPanic;
    }
}
