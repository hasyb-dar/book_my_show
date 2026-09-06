package com.example.scaler.bms.may2026.dto;

import java.util.List;

import com.example.scaler.bms.may2026.model.Feature;

import lombok.Data;

@Data
public class CreateScreenRequestDTO {

    private Long theatreId;

    private String name;

    private List<Feature> features;

    private List<SeatRowConfigDTO> seatRows;
}
