package com.example.bat_mon.BackEnd;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

public class CommunicationManagerMock extends CommunicationManager {

    @Override
    protected String getPref(String key) {
        switch (key) {
            case "MailHost": return "smtp.gmail.com";
            case "MailPort": return "465";
            case "MailServerAddr": return "BatmonApp@gmail.com";
            case "MailServerPsw": return "cfod dtza wkvc tecx";
            default: return null;
        }
    }

    @Override
    protected Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    }

}
