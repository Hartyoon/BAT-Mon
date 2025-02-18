package com.example.bat_mon.BackEnd;

import com.example.bat_mon.Exceptions.BatMonException;

import java.time.LocalDateTime;

/**
 * Our internal error class helps to classify and filter errors. To do that each error has a unique code,
 * a priority, a message, a timestamp and an ErrorType, which makes it easy to identify in which module
 * the error has occurred.
 * BatManError can either be constructed automatically by passing a BatMonException or by specifying
 * each variable manually.
 */
public class BatMonError {

    /**
     * Every Error has a code which makes it easy to identify what the problem is.
     * Additionally ErrorCode will assign an ErrorType to itself.
     * 1XXX = GetData Error
     * 2XXX = Communication Error
     */
    public enum ErrorCode {
        UNKNOWN(0),
        NONE(1),
        GET_REQUEST_FAILED(1000),
        BAD_SERVER_RESPONSE(1001),
        CHECKSUMS_DONT_MATCH(1002),
        BAD_MESSAGE_LENGTH(1003),
        NO_VALID_DATA_TIMEOUT(1004),
        NO_INTERNET_CONNECTION(1005),
        INTERNET_CONNECTION_AVAILABLE(1006),
        WEBSOCKET_CLOSED(1007),
        COMMUNICATION_ERROR(2000),
        EMAIL_NOT_SEND(2001),
        SMS_NOT_SEND(2002),
        FAULT_REGISTER_FAULT(3000),
        BATMON_INTERNAL_ERROR(4000),
        SAVING_DATA_FAILED(4001),
        LOADING_DATA_FAILED(4002),
        DATA_HANDLING(4003),
        UI_ERROR(4004);

        private final int code;

        ErrorCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public ErrorType getType() {
            if (code >= 1000 && code < 2000)
                return ErrorType.GET_DATA;
            if (code >= 2000 && code < 3000)
                return ErrorType.COMMUNICATION;
            if (code >= 3000 && code < 4000)
                return ErrorType.FAULT_REGISTER;
            if (code >= 4000 && code < 5000)
                return ErrorType.BATMON;
            return ErrorType.UNKNOWN;
        }
    }

    public enum Priority { INFO, WARNING, ERROR, FATAL, UNKNOWN, NONE }
    public enum ErrorType { GET_DATA, COMMUNICATION, FAULT_REGISTER, BATMON, UNKNOWN }

    private String message;
    private LocalDateTime time;
    private final Priority priority;
    private final ErrorType errorType;
    private final ErrorCode errorCode;

    public BatMonError(String message, Priority priority, ErrorType errorType, ErrorCode errorCode) {
        this.message = message;
        this.priority = priority;
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.time = LocalDateTime.now();
    }

    public BatMonError(String message, Priority priority, ErrorCode errorCode) {
        this.message = message;
        this.priority = priority;
        this.errorType = errorCode.getType();
        this.errorCode = errorCode;
        this.time = LocalDateTime.now();
    }

    public BatMonError(LocalDateTime time, String message, Priority priority, ErrorCode errorCode) {
        this.message = message;
        this.priority = priority;
        this.errorType = errorCode.getType();
        this.errorCode = errorCode;
        this.time = time;
    }

    public BatMonError(BatMonException e) {
        this.time = LocalDateTime.now();
        if (e.getCause() != null)
            message = e.getMessage() + ": " + e.getCause().getMessage();
        else
            message = e.getMessage();
        errorCode = e.getCode();
        errorType = e.getCode().getType();

        // Priority
        switch (e.getCode()) {
            case NO_VALID_DATA_TIMEOUT:
                priority = Priority.FATAL;
                break;
            case UNKNOWN:
                priority = Priority.UNKNOWN;
                break;
            default: // Assume that an exception is always an error
                priority = Priority.ERROR;
        }
    }

    @Override
    public String toString() {
        return time + "\t" + errorType + "\t" + priority + "\t" + errorCode + "\t" + message;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Priority getPriority() {
        return priority;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
