package com.example.scaler.bms.may2026.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Seat extends BaseModel {

    private String seatNumber;

    private int rowNo;

    private int colNo;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;
}
