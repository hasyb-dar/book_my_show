package com.example.scaler.bms.may2026.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.scaler.bms.may2026.dto.ErrorDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorDTO> handleInvalidRequest(
            InvalidRequestException ex) {

        ErrorDTO error = new ErrorDTO();
        error.setErrorCode("ERROR_400");
        error.setErrorMsg(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    public ResponseEntity<ErrorDTO> handleSeatNotAvailable(
            SeatNotAvailableException ex) {

        ErrorDTO error = new ErrorDTO();
        error.setErrorCode("ERROR_409");
        error.setErrorMsg(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(
            Exception ex) {

        ErrorDTO error = new ErrorDTO();
        error.setErrorCode("ERROR_500");
        error.setErrorMsg("Something went wrong.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}