package com.example.scaler.bms.may2026.translator;

import com.example.scaler.bms.may2026.dto.CityResponseDTO;
import com.example.scaler.bms.may2026.model.City;

public class CityTranslator {

    private CityTranslator() {
        // Prevent instantiation
    }

    public static CityResponseDTO transform(City city) {

        CityResponseDTO dto = new CityResponseDTO();

        dto.setCityId(city.getId());
        dto.setName(city.getName());

        return dto;
    }
}
