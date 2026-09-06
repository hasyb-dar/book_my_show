package com.example.scaler.bms.may2026.exception;

public class SeatNotAvailableException extends Exception {

    public SeatNotAvailableException() {
        super();
    }

    public SeatNotAvailableException(String message) {
        super(message);
    }

    public SeatNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
