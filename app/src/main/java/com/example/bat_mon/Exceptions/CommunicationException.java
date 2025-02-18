package com.example.bat_mon.Exceptions;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;

public class CommunicationException extends BatMonException {

    public CommunicationException(String errorMessage, ErrorCode code) {
        super(errorMessage, code);
    }

    public CommunicationException(String errorMessage, Throwable cause) {
        super(errorMessage, cause, ErrorCode.COMMUNICATION_ERROR);
    }

    public CommunicationException(String errorMessage, Throwable cause, ErrorCode code) {
        super(errorMessage, cause, code);
    }

}
