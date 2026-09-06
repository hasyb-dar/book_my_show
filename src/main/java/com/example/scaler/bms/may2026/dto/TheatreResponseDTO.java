package com.example.scaler.bms.may2026.dto;

import java.util.List;

import lombok.Data;

@Data
public class TheatreResponseDTO {

    private Long theatreId;

    private String name;

    private String cityName;

    private List<Long> screenIds;
}
