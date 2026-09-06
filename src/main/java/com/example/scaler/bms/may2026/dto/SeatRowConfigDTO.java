package com.example.scaler.bms.may2026.dto;

import com.example.scaler.bms.may2026.model.SeatType;

import lombok.Data;

/**
 * One row's worth of seats to generate for a screen, e.g.
 * rowLabel="A", numberOfSeats=10, seatType=GOLD produces
 * seats A1..A10, all of type GOLD.
 */
@Data
public class SeatRowConfigDTO {

    private String rowLabel;

    private int numberOfSeats;

    private SeatType seatType;
}
