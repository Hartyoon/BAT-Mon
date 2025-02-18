package com.example.bat_mon.BackEnd.Utils;

import android.content.Context;
import android.os.Environment;

import com.example.bat_mon.BatMonApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JSONUtils {

    public static synchronized void saveJSONToFile(String fileName, String jsonString) throws IOException {
        FileOutputStream fos = BatMonApplication.getAppContext().openFileOutput(fileName, Context.MODE_PRIVATE);
        GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
        gzipOS.write(jsonString.getBytes());
        gzipOS.close();
        fos.close();
        copyFileToExternalStorage(BatMonApplication.getAppContext(), fileName, fileName);
    }

    public static String loadJSONFromFile(String fileName) throws IOException {
        FileInputStream fis = BatMonApplication.getAppContext().openFileInput(fileName);
        GZIPInputStream gzipIS = new GZIPInputStream(fis);
        InputStreamReader inputStreamReader = new InputStreamReader(gzipIS);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        gzipIS.close();
        fis.close();
        return builder.toString();
    }

    public static void copyFileToExternalStorage(Context context, String internalFileName, String externalFileName) throws IOException {
        try {
            FileInputStream fis = context.openFileInput(internalFileName);
            File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), externalFileName);
            FileOutputStream fos = new FileOutputStream(externalFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();
        } catch (Exception e) {
            // Nobody cares
        }

    }

}
