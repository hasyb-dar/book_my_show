package com.example.scaler.bms.may2026.translator;

import java.util.List;
import java.util.stream.Collectors;

import com.example.scaler.bms.may2026.dto.CreateBookingResponseDTO;
import com.example.scaler.bms.may2026.model.Booking;
import com.example.scaler.bms.may2026.model.ShowSeat;

public class BookingTranslator {

    private BookingTranslator() {
        // Prevent instantiation
    }

    public static CreateBookingResponseDTO transform(Booking booking) {

        CreateBookingResponseDTO response = new CreateBookingResponseDTO();

        response.setTicketId(booking.getId());
        response.setBookingStatus(booking.getBookingStatus());

        List<String> bookedSeats = booking.getShowSeats()
                .stream()
                .map(showSeat -> showSeat.getSeat().getSeatNumber())
                .collect(Collectors.toList());

        response.setBookedSeatNumbers(bookedSeats);

        return response;
    }
}
