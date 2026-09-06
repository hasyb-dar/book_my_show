package com.example.scaler.bms.may2026.dto;

import java.util.List;

import com.example.scaler.bms.may2026.model.Feature;

import lombok.Data;

@Data
public class CreateMovieRequestDTO {

    private String name;

    private String rating;

    private List<Feature> features;

    private List<String> languages;

    private Long duration;

    private List<Long> actorIds;
}
