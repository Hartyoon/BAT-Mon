package com.example.bat_mon.Exceptions;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;

public class BatMonException extends Exception {

    private final ErrorCode code;

    public BatMonException(String errorMessage, ErrorCode code) {
        super(errorMessage);
        this.code = code;
    }

    public BatMonException(String errorMessage, Throwable cause, ErrorCode code) {
        super (errorMessage, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

}
