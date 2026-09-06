package com.example.scaler.bms.may2026.dto;

import com.example.scaler.bms.may2026.model.SeatType;

import lombok.Data;

@Data
public class SeatTypePriceDTO {

    private SeatType seatType;

    private Double price;
}
