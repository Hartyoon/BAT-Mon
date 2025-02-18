package com.example.bat_mon.BackEnd;

import android.media.MediaPlayer;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.BackEnd.Utils.JSONUtils;
import com.example.bat_mon.BatMonApplication;
import com.example.bat_mon.Exceptions.BatMonException;
import com.example.bat_mon.Exceptions.CommunicationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The ErrorHandler keeps track of all errors and manages the panic state. When panic mode is activated
 * the user gets notified by SMS, E-Mail an error sound and a notification inside the UI.
 * When panic is resolved, the user also gets notified.
 */
public class ErrorHandler {

    private static final String ERROR_LIST_FILE = "errorList.json";


    private static CommunicationManager cm = new CommunicationManager();
    private static MediaPlayer mediaPlayer;

    private static final ArrayList<BatMonError> errorList = new ArrayList<>();
    private static boolean panicMode;
    private static MutableLiveData<Boolean> panicModeLiveData = new MutableLiveData<>();
    public static int secondsToPanicResolveMessage = 3;

    public static void addError(BatMonException e) {
        addError(new BatMonError(e));
    }

    public static void addError(BatMonError error) {
        Log.i("Error-Handler", error.toString());
        errorList.add(error);
    }

    public static void setPanicMode(boolean p_panicMode) {
        setPanicMode(p_panicMode, "No reason");
    }

    public static void setPanicMode(boolean p_panicMode, String reason) {
        Log.d("Panic-Mode", "Panic mode " + panicMode + " -> " + p_panicMode + ": " + reason);
        if (panicMode != p_panicMode) { // Only if panic status changed
            String subject, body, shortText;
            boolean currentPanicMode = p_panicMode;

            if (p_panicMode) {
                subject = "Batmon is panicking";
                shortText = ErrorHandler.getLatestError(Priority.FATAL).getMessage();
                body = "Error Protocol:\n\n" + listToString();
                Log.e("Error-Handler", "PANIC MODE ACTIVATED");
            } else {
                subject = "Panic mode resolved";
                shortText = "Panic mode resolved";
                body = "Batmon is not panicking anymore";
                Log.i("Error-Handler", "PANIC MODE RESOLVED");
            }

            int alarmTimeout = BatMonApplication.getPrefInt("alarmTime");
            if (alarmTimeout == -1)
                alarmTimeout = 3;
            Log.d("Error-Handler", "timeout" + alarmTimeout);

            // Delay message by n seconds
            new Timer().schedule(new TimerTask() {
                @Override
                public  void run() {
                    // If panic mode was deactivated / reactivated after n seconds, don't send the message
                    if (panicMode != currentPanicMode)
                        return;

                    try {
                        cm.sendMessage(subject, body, shortText);
                    } catch (CommunicationException e) {
                        ErrorHandler.addError(e);
                    }
                }
            }, alarmTimeout);
        }

        panicMode = p_panicMode;
        panicModeLiveData.postValue(p_panicMode);
    }

    private static int errorListSizeLastTime = 0;
    public static synchronized void saveErrorListToFile() throws BatMonException {
        // No need to save when no new errors were added
        if (errorListSizeLastTime == errorList.size())
            return;

        try {
            synchronized (errorList) {
                JSONArray errorArray = new JSONArray();
                for (BatMonError error : errorList) {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("message", error.getMessage());
                    errorJson.put("priority", error.getPriority());
                    errorJson.put("code", error.getErrorCode());
                    errorJson.put("time", error.getTime());
                    errorArray.put(errorJson);
                }
                String jsonString = errorArray.toString();
                JSONUtils.saveJSONToFile(ERROR_LIST_FILE, jsonString);
            }
            errorListSizeLastTime = errorList.size();
            Log.d("Data-Saving", "Saved error list");
        } catch (JSONException | ConcurrentModificationException e) {
            e.printStackTrace();
            throw new BatMonException("Could not create JSON Array from error list", e, BatMonError.ErrorCode.SAVING_DATA_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
            ErrorHandler.addError(new BatMonException("Could not save error list JSON Array to storage", e, BatMonError.ErrorCode.SAVING_DATA_FAILED));
        }
    }

    public static void loadErrorListFromFile() throws BatMonException {
        try {
            String jsonString = JSONUtils.loadJSONFromFile(ERROR_LIST_FILE);
            JSONArray errorArray = new JSONArray(jsonString);
            for (int i = 0; i < errorArray.length(); i++) {
                JSONObject errorJson = errorArray.getJSONObject(i);
                String message = errorJson.getString("message");
                Priority priority = Priority.valueOf(errorJson.getString("priority"));
                ErrorCode errorCode = ErrorCode.valueOf(errorJson.getString("code"));
                LocalDateTime time = LocalDateTime.parse(errorJson.getString("time"));
                BatMonError error = new BatMonError(time, message, priority, errorCode);
                ErrorHandler.addError(error);
            }
            Log.d("Data-Loading", "Error list size after loading " + errorList.size());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new BatMonException("Could not parse JSON error list", e, BatMonError.ErrorCode.LOADING_DATA_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BatMonException("Could not load error JSON file from storage", e, BatMonError.ErrorCode.LOADING_DATA_FAILED);
        }
    }

    public static boolean isPanicMode() {
        return panicMode;
    }

    public static LiveData<Boolean> getPanicModeLiveData() {
        return panicModeLiveData;
    }

    public static BatMonError getLatestError() {
        return errorList.get(errorList.size() - 1);
    }

    public static BatMonError getLatestError(BatMonError.Priority priority) {
        // Read list backwards to get latest error. Don't use ArrayList.reverse() because it copies the whole list!
        for (int i = errorList.size() - 1; i >= 0; i--) {
            if (errorList.get(i).getPriority() == priority)
                return errorList.get(i);
        }
        return null;
    }

    public static ArrayList<BatMonError> getErrorList() {
        return errorList;
    }

    public static String listToString() {
        StringBuilder res = new StringBuilder();

        // Copy list to avoid ConcurrentModificationExceptions
        List<BatMonError> errorListCopy = new ArrayList<>(errorList);

        for (BatMonError error : errorListCopy) {
            res.append(error.toString()).append("\n");
        }
        return res.toString();
    }

    public static CommunicationManager getCommunicationManager() {
        return cm;
    }

}
