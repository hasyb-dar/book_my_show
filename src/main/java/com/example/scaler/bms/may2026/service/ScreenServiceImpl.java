package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.scaler.bms.may2026.dto.CreateScreenRequestDTO;
import com.example.scaler.bms.may2026.dto.SeatRowConfigDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Screen;
import com.example.scaler.bms.may2026.model.Seat;
import com.example.scaler.bms.may2026.model.Theatre;
import com.example.scaler.bms.may2026.repository.ScreenRepository;
import com.example.scaler.bms.may2026.repository.SeatRepository;
import com.example.scaler.bms.may2026.repository.TheatreRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScreenServiceImpl {

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Transactional
    public Screen createScreen(CreateScreenRequestDTO requestDTO) throws InvalidRequestException {

        if (requestDTO.getTheatreId() == null) {
            throw new InvalidRequestException("Theatre Id is required.");
        }

        if (requestDTO.getName() == null || requestDTO.getName().isBlank()) {
            throw new InvalidRequestException("Screen name is required.");
        }

        if (requestDTO.getSeatRows() == null || requestDTO.getSeatRows().isEmpty()) {
            throw new InvalidRequestException("At least one seat row is required.");
        }

        Optional<Theatre> theatreOptional = theatreRepository.findById(requestDTO.getTheatreId());

        if (theatreOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Theatre Id");
        }

        Theatre theatre = theatreOptional.get();

        List<Seat> seats = new ArrayList<>();
        int rowNo = 0;

        for (SeatRowConfigDTO rowConfig : requestDTO.getSeatRows()) {

            rowNo++;

            if (rowConfig.getRowLabel() == null || rowConfig.getRowLabel().isBlank()) {
                throw new InvalidRequestException("Each seat row needs a row label.");
            }

            if (rowConfig.getNumberOfSeats() <= 0) {
                throw new InvalidRequestException(
                        "Row " + rowConfig.getRowLabel() + " must have at least one seat.");
            }

            if (rowConfig.getSeatType() == null) {
                throw new InvalidRequestException(
                        "Row " + rowConfig.getRowLabel() + " is missing a seat type.");
            }

            for (int col = 1; col <= rowConfig.getNumberOfSeats(); col++) {

                Seat seat = new Seat();

                seat.setSeatNumber(rowConfig.getRowLabel() + col);
                seat.setRowNo(rowNo);
                seat.setColNo(col);
                seat.setSeatType(rowConfig.getSeatType());

                seats.add(seat);
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(seats);

        Screen screen = new Screen();

        screen.setName(requestDTO.getName());
        screen.setFeatures(requestDTO.getFeatures());
        screen.setSeats(savedSeats);

        Screen savedScreen = screenRepository.save(screen);

        List<Screen> screens = theatre.getScreens() == null
                ? new ArrayList<>()
                : new ArrayList<>(theatre.getScreens());

        screens.add(savedScreen);
        theatre.setScreens(screens);

        theatreRepository.save(theatre);

        log.info("Screen created successfully with id : {}", savedScreen.getId());

        return savedScreen;
    }

    public Screen getScreenById(Long id) throws InvalidRequestException {

        Optional<Screen> screenOptional = screenRepository.findById(id);

        if (screenOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Screen Id");
        }

        return screenOptional.get();
    }
}
