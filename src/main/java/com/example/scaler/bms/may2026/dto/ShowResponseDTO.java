package com.example.scaler.bms.may2026.dto;

import java.util.Date;
import java.util.List;

import com.example.scaler.bms.may2026.model.Feature;

import lombok.Data;

@Data
public class ShowResponseDTO {

    private Long showId;

    private String movieName;

    private Long screenId;

    private Date startTime;

    private List<Feature> features;

    private int totalSeats;

    private int availableSeats;
}
