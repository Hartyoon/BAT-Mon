package com.example.bat_mon;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.Observer;

import com.example.bat_mon.BackEnd.BatMonError;
import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.BackEnd.Data;
import com.example.bat_mon.BackEnd.ErrorHandler;
import com.example.bat_mon.BackEnd.GetData_Websocket;
import com.example.bat_mon.Exceptions.BatMonException;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;

import okhttp3.WebSocket;

public class BatMonApplication extends Application {

    private static BatMonApplication instance;
    private final long MAX_TIMEOUT = 30;
    private LocalDateTime lastValidData;
    private static WebSocket webSocket;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean saving;

    private static MediaPlayer mediaPlayer;
    private Observer<Boolean> panicModeObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // Init data structure and restore old data
        Data.init();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Data.loadDataFromJSONFile();
                ErrorHandler.loadErrorListFromFile();
            } catch (BatMonException e) {
                ErrorHandler.addError(e);
            }
        });

        startServerConnection();
        setupPanicHandling();
        setupCallbacks();
    }

    public static void startWebsocket() {
        if (webSocket != null) {
            Log.d("Network", "Close websocket before restarting");
            webSocket.close(1000,"(Re)start");
        }

        try {
            Log.d("Network", "Start websocket");
            webSocket = new GetData_Websocket().connectWebSocket(getPref("url"), getPref("user"), getPref("tsacPw"));
        } catch (BatMonException e) {
            ErrorHandler.addError(e);
            Log.d("Network", "Starting websocket failed");
        }
    }

    private void startServerConnection() {
        Log.d("Network", "Start Server Connection called");
        lastValidData = LocalDateTime.now();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network)  {
                super.onAvailable(network);
                Log.d("Network", "Internet Connection Available");
                ErrorHandler.addError(new BatMonError(
                        "Internet Connection Available",
                        Priority.INFO,
                        ErrorCode.NONE
                ));
                if (webSocket != null) // When we start the app fresh, we want to start the websocket only after we pressed the start button.
                    startWebsocket();
            }

            @Override
            public void onLost(Network network)  {
                super.onLost(network);
                Log.d("Network", "Internet Connection Lost");
                ErrorHandler.addError(new BatMonError(
                        "Internet Connection Lost",
                        Priority.WARNING,
                        ErrorCode.NO_INTERNET_CONNECTION));
                if (webSocket != null)
                    webSocket.close(1000, "Activity destroyed");
                // TODO: Last valid data
            }
        };

        Log.d("Network", "Connectivity Manager callback setup...");
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(networkRequest,networkCallback);
    }

    private void setupPanicHandling() {
        panicModeObserver = this::startStopAlarm;
        ErrorHandler.getPanicModeLiveData().observeForever(panicModeObserver);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.alarm);
        if (mediaPlayer == null) {
            ErrorHandler.addError(new BatMonError(
                    "Could not start alarm: MediaPlayer is null",
                    BatMonError.Priority.WARNING,
                    BatMonError.ErrorCode.UI_ERROR
            ));
        } else {
            Log.d("Alarm", "Media player created");
            mediaPlayer.setLooping(true);
        }
    }

    private void startStopAlarm(boolean enable) {
        if (mediaPlayer == null)
            return;

        if (enable && getPrefBoolean("alarm")) {
            Log.d("Alarm", "Start alarm");
            mediaPlayer.start();
        } else {
            Log.d("Alarm", "Stop alarm");
            mediaPlayer.stop();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Log.d("Network", "Close network connections: onLowMemory called");

        // Close network Connections
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        if (webSocket != null) {
            webSocket.close(1000, "Application destroyed");
        }
    }

    private void setupCallbacks() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {
                if (saving) { // Don't start another saving thread when we are already saving
                    Log.d("Data-Saving", "Already saving, don't start another Thread");
                    return;
                }
                saving = true;

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        Data.saveDataJSONToFile();
                        ErrorHandler.saveErrorListToFile();
                    } catch (BatMonException e) {
                        ErrorHandler.addError(e);
                    }
                });
                saving = false;
                Log.d("Data-Saving", "Saving done");
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    public static String getPref(String key) {
        try {
            SharedPreferences pref = getAppContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            return pref.getString(key, "");
        } catch (NullPointerException e) {
            ErrorHandler.addError(new BatMonException(
                    "Could not read preferences", e, BatMonError.ErrorCode.BATMON_INTERNAL_ERROR
            ));
            return null;
        }
    }

    public static boolean getPrefBoolean(String key) {
        return getPrefBoolean("settings", key);
    }

    public static boolean getPrefBoolean(String name, String key) {
        SharedPreferences pref = getAppContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        Object value = pref.getAll().get(key);
        // For whatever reason the switches are not stored as booleans, so we might need to convert from a string...
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else {
            return false;
        }
    }

    public static int getPrefInt(String key) {
        SharedPreferences pref = getAppContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        Object value = pref.getAll().get(key);
        // For whatever reason the switches are not stored as booleans, so we might need to convert from a string...
        if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else {
            return -1;
        }
    }

}
