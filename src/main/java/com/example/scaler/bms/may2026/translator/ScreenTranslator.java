package com.example.scaler.bms.may2026.translator;

import com.example.scaler.bms.may2026.dto.ScreenResponseDTO;
import com.example.scaler.bms.may2026.model.Screen;

public class ScreenTranslator {

    private ScreenTranslator() {
        // Prevent instantiation
    }

    /**
     * @param theatreId optional, since a bare Screen entity doesn't carry a
     *                   back-reference to its Theatre; pass null if unknown.
     */
    public static ScreenResponseDTO transform(Screen screen, Long theatreId) {

        ScreenResponseDTO dto = new ScreenResponseDTO();

        dto.setScreenId(screen.getId());
        dto.setName(screen.getName());
        dto.setTheatreId(theatreId);
        dto.setFeatures(screen.getFeatures());
        dto.setTotalSeats(screen.getSeats() == null ? 0 : screen.getSeats().size());

        return dto;
    }
}
