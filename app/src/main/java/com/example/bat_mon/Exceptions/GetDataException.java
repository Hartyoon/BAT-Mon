package com.example.bat_mon.Exceptions;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;

public class GetDataException extends BatMonException {

    public GetDataException(String errorMessage, ErrorCode code) {
        super(errorMessage, code);
    }

    public GetDataException(String errorMessage, Throwable cause) {
        super(errorMessage, cause, ErrorCode.GET_REQUEST_FAILED);
    }

    public GetDataException(String errorMessage, Throwable cause, ErrorCode code) {
        super(errorMessage, cause, code);
    }

}
