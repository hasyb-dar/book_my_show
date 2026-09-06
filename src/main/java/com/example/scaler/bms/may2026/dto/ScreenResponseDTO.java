package com.example.scaler.bms.may2026.dto;

import java.util.List;

import com.example.scaler.bms.may2026.model.Feature;

import lombok.Data;

@Data
public class ScreenResponseDTO {

    private Long screenId;

    private String name;

    private Long theatreId;

    private List<Feature> features;

    private int totalSeats;
}
