package com.example.bat_mon.BackEnd;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.bat_mon.Exceptions.CommunicationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests if error list methods and panic mode implementation is correct.
 * WARNING: This tests sends a lot of SMS, so you probably don't want to run this test on a real device!
 * Also tests on a real device might fail because it takes longer to send SMS compared to a virtual device.
 */
@RunWith(AndroidJUnit4.class)
public class ErrorHandlerTest {

    @Rule
    public GrantPermissionRule ruleReadSMS = GrantPermissionRule.grant(Manifest.permission.READ_SMS);
    @Rule
    public GrantPermissionRule ruleSendSMS = GrantPermissionRule.grant(Manifest.permission.SEND_SMS);

    private static CommunicationManager cm = new CommunicationManagerMock();

    @BeforeClass
    public static void setup() {
        cm.SEND_MAILS = false;
        cm.SEND_SMS = true;
        cm.SEND_NOTIFICATIONS = false;
        ErrorHandler.secondsToPanicResolveMessage = 1;
        ErrorHandler.getCommunicationManager().SEND_SMS = true;
    }

    /**
     * Make sure SMS from last test won't give us a false positive
     */
    @Before
    public void clearSMS() throws InterruptedException, CommunicationException {
        cm.sendMessage("Blah", "Blub");
        Thread.sleep(1000);      // Wait so SMS can be received
        Assert.assertEquals("Blah", getLatestSMS());
    }

    /**
     * SMS is sent when panic mode is deactivated
     */
    @Test
    public void panicModeResolved_NotInterrupted() throws InterruptedException {
        ErrorHandler.setPanicMode(true);
        Assert.assertTrue(ErrorHandler.isPanicMode());
        ErrorHandler.setPanicMode(false);   // ErrorHandler should send an SMS after 1 sec
        Thread.sleep(1000);         // Wait so SMS can be received

        Assert.assertEquals("Panic mode resolved", getLatestSMS());
    }

    /**
     * When panic mode disabled, but enabled again after a short time, the "Panic mode resolved" SMS should not have bend sent
     */
    @Test
    public void panicModeResolved_Interrupted() throws InterruptedException {
        ErrorHandler.setPanicMode(true);
        ErrorHandler.setPanicMode(false); // ErrorHandler should send resolve SMS after some time

        // Wait some time before reactivating panic mode
        Thread.sleep(500);
        ErrorHandler.setPanicMode(true);
        Thread.sleep(1000);

        // We shouldn't have sent the panic mode resolved SMS, since it was reactivated shortly after we deactivated it.
        Assert.assertNotEquals("Panic mode resolved", getLatestSMS());
    }

    /**
     * SMS is sent when panic mode is activated
     */
    @Test
    public void panicModeActivated_NotInterrupted() throws InterruptedException {
        ErrorHandler.setPanicMode(true);
        Thread.sleep(1000);
        Assert.assertEquals("Batmon is panicking", getLatestSMS());
    }

    /**
     * When panic mode enabled, but disabled again after a short time, no SMS should be sent
     */
    @Test
    public void panicModeActivated_Interrupted() throws InterruptedException {
        ErrorHandler.setPanicMode(true);
        Thread.sleep(500);
        ErrorHandler.setPanicMode(false);
        Thread.sleep(1000);
        Assert.assertNotEquals("Batmon is panicking", getLatestSMS());
    }

    private String getLatestSMS() {
        // Get context for contentResolver
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assert.assertEquals("com.example.bat_mon", appContext.getPackageName());

        // Read latest SMS
        ContentResolver contentResolver = appContext.getContentResolver();
        Cursor cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();
        String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
        cursor.close();

        return body;
    }

}
