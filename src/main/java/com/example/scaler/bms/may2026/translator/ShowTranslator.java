package com.example.scaler.bms.may2026.translator;

import java.util.List;

import com.example.scaler.bms.may2026.dto.ShowResponseDTO;
import com.example.scaler.bms.may2026.model.Show;
import com.example.scaler.bms.may2026.model.ShowSeat;
import com.example.scaler.bms.may2026.model.ShowSeatStatus;

public class ShowTranslator {

    private ShowTranslator() {
        // Prevent instantiation
    }

    public static ShowResponseDTO transform(Show show, List<ShowSeat> showSeats) {

        ShowResponseDTO dto = new ShowResponseDTO();

        dto.setShowId(show.getId());
        dto.setMovieName(show.getMovie() != null ? show.getMovie().getName() : null);
        dto.setScreenId(show.getScreen() != null ? show.getScreen().getId() : null);
        dto.setStartTime(show.getStartTime());
        dto.setFeatures(show.getFeatures());

        int total = showSeats == null ? 0 : showSeats.size();

        long available = showSeats == null
                ? 0
                : showSeats.stream()
                        .filter(seat -> seat.getShowSeatStatus() == ShowSeatStatus.AVAILABLE)
                        .count();

        dto.setTotalSeats(total);
        dto.setAvailableSeats((int) available);

        return dto;
    }
}
