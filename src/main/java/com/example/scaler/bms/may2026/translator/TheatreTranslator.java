package com.example.scaler.bms.may2026.translator;

import java.util.List;
import java.util.stream.Collectors;

import com.example.scaler.bms.may2026.dto.TheatreResponseDTO;
import com.example.scaler.bms.may2026.model.Theatre;

public class TheatreTranslator {

    private TheatreTranslator() {
        // Prevent instantiation
    }

    public static TheatreResponseDTO transform(Theatre theatre) {

        TheatreResponseDTO dto = new TheatreResponseDTO();

        dto.setTheatreId(theatre.getId());
        dto.setName(theatre.getName());
        dto.setCityName(theatre.getCity() != null ? theatre.getCity().getName() : null);

        List<Long> screenIds = theatre.getScreens() == null
                ? List.of()
                : theatre.getScreens().stream()
                        .map(screen -> screen.getId())
                        .collect(Collectors.toList());

        dto.setScreenIds(screenIds);

        return dto;
    }
}
