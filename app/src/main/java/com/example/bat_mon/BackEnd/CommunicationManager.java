package com.example.bat_mon.BackEnd;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.ErrorType;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.BatMonApplication;
import com.example.bat_mon.Exceptions.CommunicationException;
import com.example.bat_mon.FrontEnd.Home_Page;
import com.example.bat_mon.R;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public class CommunicationManager {

    private final static String CHANNEL_ID = "BatMonChannel";
    private static int notificationID = 1;

    public boolean SEND_MAILS = true;
    public boolean SEND_SMS = true;
    public boolean SEND_NOTIFICATIONS = true;

    public void sendMessage(String subject, String text, String shortText) throws CommunicationException {
        Log.i("Communication", "Sending message: " + subject);

        Log.d("Communication", Boolean.toString(getPrefBoolean("email")) + " " + getPref("email1") + " " + getPref("email2"));
        Log.d("Communication", Boolean.toString(getPrefBoolean("sms")) + " " + getPref("phone1") + " " + getPref("phone2"));

        if (SEND_MAILS && getPrefBoolean("email")) {
            sendEmail(subject, text, getPref("email1"));
            sendEmail(subject, text, getPref("email2"));
            Log.i("Communication", subject + " " + text);
            ErrorHandler.addError(new BatMonError(
                    "Send E-Mail successful: " + subject,
                    Priority.INFO,
                    ErrorType.COMMUNICATION,
                    ErrorCode.NONE));
        }

        if (SEND_SMS && getPrefBoolean("sms")) {
            sendSMS(shortText, getPref("phone1"));
            sendSMS(shortText, getPref("phone2"));
            ErrorHandler.addError(new BatMonError(
                    "Send SMS successful: " + subject,
                    Priority.INFO,
                    ErrorType.COMMUNICATION,
                    ErrorCode.NONE));
        }

        if (SEND_NOTIFICATIONS) {
            sendNotification(subject, shortText);
            ErrorHandler.addError(new BatMonError(
                    "Send Notification successful: " + subject,
                    Priority.INFO,
                    ErrorType.COMMUNICATION,
                    ErrorCode.NONE));
        }
    }

    private void sendSMS(String text, String number) throws CommunicationException {
        if (text.length() > 160) {
            throw new CommunicationException("Could not send SMS: Text too long", ErrorCode.SMS_NOT_SEND);
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, text, null, null);
            Log.d("Communication", "SMS sent");
        } catch (SecurityException | IllegalArgumentException e) {
            throw new CommunicationException("Could not send SMS: ", e, ErrorCode.SMS_NOT_SEND);
        }
    }

    private void sendEmail(String subject, String text, String receiver) throws CommunicationException {
        try {
            Properties properties = System.getProperties();
            properties.setProperty("mail.transport.protocol", "smtp");
            properties.setProperty("mail.host", getPref("MailHost"));
            properties.put("mail.smtp.host", getPref("MailHost"));
            properties.put("mail.smtp.port", getPref("MailPort"));
            properties.put("mail.smtp.socketFactory.fallback", "false");
            properties.setProperty("mail.smtp.quitwait", "false");
            properties.put("mail.smtp.socketFactory.port", getPref("MailPort"));
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.auth", "true");

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getPref("MailServerAddr"), getPref("MailServerPsw"));
                }
            });

            MimeMessage mimeMessage = new MimeMessage(session);
            Address receiverAddress = new Address() {
                @Override
                public String getType() {
                    return null;
                }

                @Override
                public String toString() {
                    return receiver;
                }

                @Override
                public boolean equals(Object o) {
                    return false;
                }
            };
            mimeMessage.addRecipient(Message.RecipientType.TO, receiverAddress);

            mimeMessage.setSubject(subject);
            mimeMessage.setText(text);

            Thread thread = new Thread(() -> {
                try {
                    Transport.send(mimeMessage);
                } catch (MessagingException e) {
                    if (e instanceof AuthenticationFailedException)
                        ErrorHandler.addError(new CommunicationException("Could not send E-Mail: Authentication failed", e));
                    else
                        ErrorHandler.addError(new CommunicationException("Could not send E-Mail", e));
                    e.printStackTrace();
                }
            });
            thread.start();
        } catch (MessagingException e) {
            throw new CommunicationException("Could not send E-Mail", e, ErrorCode.EMAIL_NOT_SEND);
        }
    }

    private void sendNotification(String title, String text) throws CommunicationException {
        Context context = getContext();
        if (context == null) {
            throw new CommunicationException("Could not send SMS: Context null", ErrorCode.COMMUNICATION_ERROR);
        }

        // Check if we have the permission to send SMS
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            throw new CommunicationException("No permission to send Notification", ErrorCode.COMMUNICATION_ERROR);
        }

        // Create channel
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "BatMon Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications for BatMon");
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notificationID++, builder.build());
    }

    protected String getPref(String key) {
        return readSharedPref(key);
    }

    protected boolean getPrefBoolean(String key) {
        return readSharedPrefBoolean(key);
    }

    private static String readSharedPref(String key) {
        SharedPreferences pref = BatMonApplication.getAppContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }

    private static boolean readSharedPrefBoolean(String key) {
        SharedPreferences pref = BatMonApplication.getAppContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
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

    protected Context getContext() {
        return Home_Page.getContext();
    }
}
