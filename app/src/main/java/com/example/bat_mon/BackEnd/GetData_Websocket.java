package com.example.bat_mon.BackEnd;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.Exceptions.GetDataException;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class GetData_Websocket {

    private static OkHttpClient client;
    private final String websocketID = "batmontest";
    private static final int SECONDS_TO_TIMEOUT = 10;

    public WebSocket connectWebSocket(String url, String user, String password) throws GetDataException {
        // Setup basic auth
        String login = user + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(login.getBytes());

        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", basicAuth)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new GetDataException("Could not build request: ", e);
        }

        client = new OkHttpClient();

        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            LocalDateTime lastValidData = LocalDateTime.now();

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.d("WebSocket","Closed");
                ErrorHandler.addError(new BatMonError(
                        "Websocket connection was closed", Priority.WARNING, ErrorCode.WEBSOCKET_CLOSED
                ));
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                Log.d("WebSocket","Closing");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);

                ErrorHandler.addError(new GetDataException("Websocket Failure", t));
                Log.d("WebSocket","WebSocket failure; Response: " + response + "\n Error: " + t);

                Log.d("Websocket", "Start timeout timer");

                Executors.newScheduledThreadPool(1).schedule(() -> {
                    // If panic mode was deactivated / reactivated after n seconds, don't send the message
                    long secondsSinceLastValidData = ChronoUnit.SECONDS.between(lastValidData, LocalDateTime.now());
                    Log.d("Websocket", "Check for timeout " + secondsSinceLastValidData);
                    if (secondsSinceLastValidData >= SECONDS_TO_TIMEOUT) {
                        Log.d("Websocket", "No valid data received since timer started");
                        ErrorHandler.addError(new GetDataException("Received no data for " + secondsSinceLastValidData + " seconds", ErrorCode.NO_VALID_DATA_TIMEOUT));
                        ErrorHandler.setPanicMode(true, "No valid data for " +secondsSinceLastValidData + " s");
                    }
                }, SECONDS_TO_TIMEOUT, TimeUnit.SECONDS);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.d("WebSocket-Data","Response: " + text);

                // Only try to safe the data not the server responses from "start" "setID/" "ok"
                if (text.contains("WQI")) { // Looks like it always starts with "WQI"
                    // Save Data
                    try {
                        String[] dataFrames = prepareData(text);
                        Data.parseData(dataFrames);
                        lastValidData = LocalDateTime.now();
                    } catch (GetDataException e) {
                        ErrorHandler.addError(e);
                    }
                } else if (!text.contains("ok")) {
                    ErrorHandler.addError(new GetDataException(
                        "Unexpected server response", ErrorCode.BAD_SERVER_RESPONSE
                    ));
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
                Log.d("WebSocket","on Message");
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                if (response.toString().contains("code=101, message=Switching Protocols")) {
                    webSocket.send("setID/"+websocketID);
                    webSocket.send("start"); //start sending data
                }

                Log.d("WebSocket","Connected: " + response);
            }
        });

        client.dispatcher().executorService().shutdown();
        return webSocket;
    }

    private static String[] prepareData(String data) throws GetDataException {
        byte[] decoded = Base64.getDecoder().decode(data);
        String[] dataFrames = convertBase64ToHex(decoded);

        checksum(decoded, dataFrames);

        return dataFrames;
    }

    private static void checksum(byte[] decoded, String[] dataFrames) throws GetDataException {
        // Calculate Checksum
        int length = Integer.parseInt(dataFrames[0], 16) * 4;
        long crcData = 0;
        for (int i = 0; i < length; i++) {
            crcData = crcData ^ ((long) Byte.toUnsignedInt(decoded[i]) << (i % 24));
        }

        long checksum = Long.parseLong(dataFrames[dataFrames.length - 1], 16);
        if (checksum != crcData) {
            throw new GetDataException("Checksums did not match: " + crcData + "\t" + checksum, ErrorCode.CHECKSUMS_DONT_MATCH);
        }

        // Message length is at pos 0. It counts all 32-bit data fields minus the checksum. That's why we need to multiply it by 4 and add 4
        int messageLength = Integer.parseInt(dataFrames[0], 16) * 4 + 4;
        if (messageLength != decoded.length) {
            throw new GetDataException("Message length does not match: " + messageLength + "\t" + decoded.length, ErrorCode.BAD_MESSAGE_LENGTH);
        }

//        Log.d("Get-Data", "Checksums and message lengths OK");
    }

    // Convert Base64 string to data frames in hex format
    private static String[] convertBase64ToHex(byte[] decoded) {
        // Decode Base64 into Hex
        String hex = String.format("%040x", new BigInteger(1, decoded));

        // Convert hex string into 4 byte sized hex String array
        String[] dataFrames = new String[hex.length() / 8];
        for (int i = 0; i < dataFrames.length; i++) {
            String temp = hex.substring(i * 8, (i + 1) * 8);

            // The data stream uses little endian. Convert to big endian, since it's easier to work with.
            dataFrames[i] = temp.substring(6, 8) + temp.substring(4, 6) + temp.substring(2, 4) + temp.substring(0, 2);
        }

        return dataFrames;
    }
}

