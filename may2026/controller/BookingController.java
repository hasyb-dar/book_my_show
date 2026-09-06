package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.CreateBookingRequestDTO;
import com.example.scaler.bms.may2026.dto.CreateBookingResponseDTO;
import com.example.scaler.bms.may2026.dto.TicketDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.exception.SeatNotAvailableException;
import com.example.scaler.bms.may2026.model.Booking;
import com.example.scaler.bms.may2026.service.BookingServiceImpl;
import com.example.scaler.bms.may2026.translator.BookingTranslator;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingServiceImpl bookingServiceImpl;

    @PostMapping("/ticket")
    public ResponseEntity<CreateBookingResponseDTO> bookTicket(
            @RequestBody CreateBookingRequestDTO requestDTO)
            throws InvalidRequestException, SeatNotAvailableException {

        if (isInvalidRequest(requestDTO)) {
            throw new InvalidRequestException("Invalid Request");
        }

        Booking booking = bookingServiceImpl.createBooking(
                requestDTO.getSeatNumbers(),
                requestDTO.getShowId(),
                requestDTO.getUserId());

        log.info("Booking created successfully with id : {}", booking.getId());

        CreateBookingResponseDTO response =
                BookingTranslator.transform(booking);

        return ResponseEntity.ok(response);
    }

    private boolean isInvalidRequest(CreateBookingRequestDTO requestDTO) {

        if (requestDTO == null) {
            return true;
        }

        if (requestDTO.getShowId() == null) {
            return true;
        }

        if (requestDTO.getUserId() == null) {
            return true;
        }

        if (requestDTO.getSeatNumbers() == null ||
                requestDTO.getSeatNumbers().isEmpty()) {
            return true;
        }

        return false;
    }

    @GetMapping("/ticket/{id}")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long id) {

        TicketDTO ticket = new TicketDTO();
        ticket.setTicketId(id);
        ticket.setCreatedBy(null);
        ticket.setTotalAmount(1000.0);

        return ResponseEntity.ok(ticket);
    }
}