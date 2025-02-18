package com.example.bat_mon.BackEnd;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.Exceptions.GetDataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class GetData {

    private static final String USER_AGENT = "Mozilla/5.0";

    public static String[] getData(String url, String username, String pw) throws GetDataException {
        String dataStream;
        try {
            dataStream = GetData.sendGET(url, username, pw);
        } catch (IOException e) {
            throw new GetDataException("Get request failed", e);
        }

        byte[] decoded = Base64.getDecoder().decode(dataStream);
        String[] dataFrames = convertBase64ToHex(decoded);

        checksum(decoded, dataFrames); // Will throw a GetDataException if checksums or message length don't match

        return dataFrames;
    }

    private static String sendGET(String urlString, String username, String password) throws IOException, GetDataException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set up the request method and headers
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(3000); // TODO: Make this configurable

        // Add basic authentication header
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        String authHeaderValue = "Basic " + encodedAuth;
        connection.setRequestProperty("Authorization", authHeaderValue);

        // Get the response code
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) { // uccess
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            throw new GetDataException("Server response code: " + responseCode, ErrorCode.BAD_SERVER_RESPONSE);
        }
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