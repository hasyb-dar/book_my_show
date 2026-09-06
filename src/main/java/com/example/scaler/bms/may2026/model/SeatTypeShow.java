package com.example.scaler.bms.may2026.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class SeatTypeShow extends BaseModel {

    @ManyToOne
    private Show show;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    private Double price;
}
