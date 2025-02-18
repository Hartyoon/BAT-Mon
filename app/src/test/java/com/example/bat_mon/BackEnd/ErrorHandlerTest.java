package com.example.bat_mon.BackEnd;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.ErrorType;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.Exceptions.BatMonException;
import com.example.bat_mon.Exceptions.CommunicationException;
import com.example.bat_mon.Exceptions.GetDataException;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class ErrorHandlerTest {

    /**
     * Check if the internal error protocol is working correctly.
     * We will add a few exceptions manually with certain values. After that we will read the error
     * list manually and by using the getLastError method. We will then check if the results are
     * correctly saved and in the correct order.
     */
    @Test
    public void addTest_isCorrect() {
        ErrorHandler.getErrorList().clear();

        BatMonException e1 = new BatMonException("Test Exception 1", ErrorCode.UNKNOWN);
        BatMonException e2 = new CommunicationException("Test Exception 2", new CommunicationException("Test Cause", ErrorCode.UNKNOWN), ErrorCode.SMS_NOT_SEND);
        BatMonException e3 = new GetDataException("Test Exception 3", ErrorCode.NO_VALID_DATA_TIMEOUT);
        BatMonException e4 = new GetDataException("Test Exception 4", ErrorCode.GET_REQUEST_FAILED);
        BatMonError e5 = new BatMonError("Test Error 5", Priority.INFO, ErrorCode.NO_INTERNET_CONNECTION);
        BatMonError e6 = new BatMonError("Test Error 6", Priority.WARNING, ErrorType.COMMUNICATION, ErrorCode.SMS_NOT_SEND);
        ErrorHandler.addError(e1);
        ErrorHandler.addError(e2);
        ErrorHandler.addError(e3);
        ErrorHandler.addError(e4);
        ErrorHandler.addError(e5);
        ErrorHandler.addError(e6);
        ArrayList<BatMonError> list = ErrorHandler.getErrorList();

        BatMonError l1 = list.get(0);
        Assert.assertEquals("Test Exception 1", l1.getMessage());
        Assert.assertEquals(ErrorCode.UNKNOWN, l1.getErrorCode());
        Assert.assertEquals(Priority.UNKNOWN, l1.getPriority());
        Assert.assertEquals(ErrorType.UNKNOWN, l1.getErrorType());

        BatMonError l2 = list.get(1);
        Assert.assertEquals("Test Exception 2: Test Cause", l2.getMessage());
        Assert.assertEquals(ErrorCode.SMS_NOT_SEND, l2.getErrorCode());
        Assert.assertEquals(Priority.ERROR, l2.getPriority());
        Assert.assertEquals(ErrorType.COMMUNICATION, l2.getErrorType());

        BatMonError l3 = ErrorHandler.getLatestError(Priority.FATAL);
        Assert.assertEquals("Test Exception 3", l3.getMessage());
        Assert.assertEquals(ErrorCode.NO_VALID_DATA_TIMEOUT, l3.getErrorCode());
        Assert.assertEquals(Priority.FATAL, l3.getPriority());
        Assert.assertEquals(ErrorType.GET_DATA, l3.getErrorType());

        BatMonError l4 = list.get(3);
        Assert.assertEquals("Test Exception 4", l4.getMessage());
        Assert.assertEquals(ErrorCode.GET_REQUEST_FAILED, l4.getErrorCode());
        Assert.assertEquals(Priority.ERROR, l4.getPriority());
        Assert.assertEquals(ErrorType.GET_DATA, l4.getErrorType());

        BatMonError l5 = ErrorHandler.getLatestError(Priority.INFO);
        Assert.assertEquals("Test Error 5", l5.getMessage());
        Assert.assertEquals(ErrorCode.NO_INTERNET_CONNECTION, l5.getErrorCode());
        Assert.assertEquals(Priority.INFO, l5.getPriority());
        Assert.assertEquals(ErrorType.GET_DATA, l5.getErrorType());

        BatMonError l6 = ErrorHandler.getLatestError();
        Assert.assertEquals("Test Error 6", l6.getMessage());
        Assert.assertEquals(ErrorCode.SMS_NOT_SEND, l6.getErrorCode());
        Assert.assertEquals(Priority.WARNING, l6.getPriority());
        Assert.assertEquals(ErrorType.COMMUNICATION, l6.getErrorType());
    }

}