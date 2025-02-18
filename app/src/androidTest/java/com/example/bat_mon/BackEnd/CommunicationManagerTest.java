package com.example.bat_mon.BackEnd;

import android.Manifest;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.bat_mon.Exceptions.CommunicationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

@RunWith(AndroidJUnit4.class)
public class CommunicationManagerTest {

    @Rule
    public GrantPermissionRule ruleReadSMS = GrantPermissionRule.grant(Manifest.permission.READ_SMS);
    @Rule
    public GrantPermissionRule ruleSendSMS = GrantPermissionRule.grant(Manifest.permission.SEND_SMS);
    @Rule
    public GrantPermissionRule ruleInternet = GrantPermissionRule.grant(Manifest.permission.INTERNET);
    @Rule
    public GrantPermissionRule ruleNotification = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS);

    /**
     * This test checks if the send SMS functionality works correctly.
     * First we send an SMS. After that we will read the latest SMS to check if it was actually sent.
     * @throws InterruptedException when the Thread.sleep method is interrupted
     * @throws CommunicationException when the SMS was not sent
     */
    @Test
    public void sendSMS_isCorrect() throws InterruptedException, CommunicationException {
        // Get context for contentResolver
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assert.assertEquals("com.example.bat_mon", appContext.getPackageName());

        // Send test SMS
        CommunicationManager cm = new CommunicationManagerMock();
        cm.SEND_SMS = true;
        cm.SEND_MAILS = false;
        cm.SEND_NOTIFICATIONS = false;
        String subject = "Test subject " + new Random().nextInt(Integer.MAX_VALUE);
        cm.sendMessage(subject, "");

        // Wait so SMS can be received
        Thread.sleep(1000);

        // Read latest SMS
        ContentResolver contentResolver = appContext.getContentResolver();
        Cursor cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();
        String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
        cursor.close();

        Assert.assertEquals(subject, body);
    }

    /**
     * Checks if we throw an exception when the SMS body is too long.
     */
    @Test
    public void sendSMS_tooLong() {
        // Send test SMS
        CommunicationManager cm = new CommunicationManagerMock();
        cm.SEND_SMS = true;
        cm.SEND_MAILS = false;
        cm.SEND_NOTIFICATIONS = false;
        String subject = "";
        for (int i = 0; i < 161; i++)
            subject += " ";

        try {
            cm.sendMessage(subject, "");
        } catch (CommunicationException e) {
            Assert.assertTrue(e.getMessage().contains("Could not send SMS: Text too long"));
        }
    }

    /**
     * Check if the E-Mail sending functionality is working correctly.
     * We first send an E-Mail. After that we read E-Mails from the mail server using POP3. When
     * the E-Mail we just sent to ourselves arrived, the test passes.
     * @throws InterruptedException when the Thread.sleep method is interrupted
     * @throws CommunicationException when the SMS was not sent
     * @throws MessagingException when we were not able to read E-Mails from the mail server
     * @throws IOException when we were not able to read E-Mails from the mail server
     */
    @Test
    public void sendMail_isCorrect() throws InterruptedException, CommunicationException, MessagingException, IOException {
        // Send test SMS
        CommunicationManagerMock cm = new CommunicationManagerMock();
        cm.SEND_SMS = false;
        cm.SEND_MAILS = true;
        cm.SEND_NOTIFICATIONS = false;

        String subject = "Test subject " + new Random().nextInt(Integer.MAX_VALUE);
        String body = "Test body " + new Random().nextInt(Integer.MAX_VALUE);
        cm.sendMessage(subject, body);

        // Wait so E-Mail is sent
        Thread.sleep(1000);

        // Read all Mails from Server
        // https://www.tutorialspoint.com/javamail_api/javamail_api_checking_emails.htm
        Message latestMessage = null;
        try {
            String host = "pop.gmail.com";
            String user = "BatmonApp@gmail.com";
            String password = "cfod dtza wkvc tecx";

            // Create properties field
            Properties properties = new Properties();

            properties.put("mail.pop3.host", host);
            properties.put("mail.pop3.port", "995");
            properties.put("mail.pop3.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            // Create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("pop3s");

            store.connect(host, user, password);

            // Create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // Retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();
            latestMessage = messages[messages.length - 1];
            System.out.println("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                Log.d("Mail-Test", "---------------------------------");
                Log.d("Mail-Test", "Email Number " + (i + 1));
                Log.d("Mail-Test", "Subject: " + message.getSubject());
                Log.d("Mail-Test", "From: " + message.getFrom()[0]);
                Log.d("Mail-Test", "Text: " + message.getContent().toString());
            }

            // Close the store and folder objects
            emailFolder.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(subject, latestMessage.getSubject());
        Assert.assertEquals(body + "\r\n", latestMessage.getContent().toString());
    }

    @Test
    public void sendNotification_isCorrect() throws InterruptedException, CommunicationException {
        // Send test Notification
        CommunicationManager cm = new CommunicationManagerMock();
        cm.SEND_SMS = false;
        cm.SEND_MAILS = false;
        cm.SEND_NOTIFICATIONS = true;

        String title = "Test Notification";
        String text = "This is a test notification";

        // Send notification
        cm.sendMessage(title, text);

        // Wait for the notification to be received
        Thread.sleep(200);

        // Get context for NotificationManager
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assert.assertEquals("com.example.bat_mon", appContext.getPackageName());

        // Check if the notification was received
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications().toArray(new StatusBarNotification[0]);

        boolean notificationReceived = false;
        for (StatusBarNotification notification : notifications) {
            if (notification.getNotification().extras.getString(Notification.EXTRA_TITLE).equals(title) &&
                    notification.getNotification().extras.getString(Notification.EXTRA_TEXT).equals(text)) {
                notificationReceived = true;
                break;
            }
        }

        Assert.assertTrue(notificationReceived);
    }

}