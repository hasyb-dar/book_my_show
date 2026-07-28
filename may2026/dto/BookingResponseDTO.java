package com.example.scaler.bms.may2026.dto;

import java.util.List;

import com.example.scaler.bms.may2026.model.BookingStatus;

import lombok.Data;

@Data
public class BookingResponseDTO {

    private Long bookingId;

    private Long showId;

    private Long userId;

    private Double totalAmount;

    private BookingStatus bookingStatus;

    private List<String> bookedSeats;

}
