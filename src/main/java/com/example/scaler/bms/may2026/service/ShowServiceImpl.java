package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.scaler.bms.may2026.dto.CreateShowRequestDTO;
import com.example.scaler.bms.may2026.dto.SeatTypePriceDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Movie;
import com.example.scaler.bms.may2026.model.Screen;
import com.example.scaler.bms.may2026.model.Seat;
import com.example.scaler.bms.may2026.model.SeatTypeShow;
import com.example.scaler.bms.may2026.model.Show;
import com.example.scaler.bms.may2026.model.ShowSeat;
import com.example.scaler.bms.may2026.model.ShowSeatStatus;
import com.example.scaler.bms.may2026.repository.MovieRepository;
import com.example.scaler.bms.may2026.repository.ScreenRepository;
import com.example.scaler.bms.may2026.repository.SeatTypeShowRepository;
import com.example.scaler.bms.may2026.repository.ShowRepository;
import com.example.scaler.bms.may2026.repository.ShowSeatRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ShowServiceImpl {

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private SeatTypeShowRepository seatTypeShowRepository;

    @Transactional
    public Show createShow(CreateShowRequestDTO requestDTO) throws InvalidRequestException {

        if (requestDTO.getMovieId() == null) {
            throw new InvalidRequestException("Movie Id is required.");
        }

        if (requestDTO.getScreenId() == null) {
            throw new InvalidRequestException("Screen Id is required.");
        }

        if (requestDTO.getStartTime() == null) {
            throw new InvalidRequestException("Show start time is required.");
        }

        if (requestDTO.getSeatPrices() == null || requestDTO.getSeatPrices().isEmpty()) {
            throw new InvalidRequestException("At least one seat type price is required.");
        }

        for (SeatTypePriceDTO priceDTO : requestDTO.getSeatPrices()) {

            if (priceDTO.getSeatType() == null) {
                throw new InvalidRequestException("Seat type is required for pricing.");
            }

            if (priceDTO.getPrice() == null || priceDTO.getPrice() <= 0) {
                throw new InvalidRequestException(
                        "Price for seat type " + priceDTO.getSeatType() + " must be positive.");
            }
        }

        Optional<Movie> movieOptional = movieRepository.findById(requestDTO.getMovieId());

        if (movieOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Movie Id");
        }

        Optional<Screen> screenOptional = screenRepository.findById(requestDTO.getScreenId());

        if (screenOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Screen Id");
        }

        Screen screen = screenOptional.get();

        if (screen.getSeats() == null || screen.getSeats().isEmpty()) {
            throw new InvalidRequestException("Screen has no seats configured.");
        }

        Show show = new Show();

        show.setMovie(movieOptional.get());
        show.setScreen(screen);
        show.setStartTime(requestDTO.getStartTime());
        show.setFeatures(requestDTO.getFeatures());

        Show savedShow = showRepository.save(show);

        List<ShowSeat> showSeats = new ArrayList<>();

        for (Seat seat : screen.getSeats()) {

            ShowSeat showSeat = new ShowSeat();

            showSeat.setShow(savedShow);
            showSeat.setSeat(seat);
            showSeat.setShowSeatStatus(ShowSeatStatus.AVAILABLE);

            showSeats.add(showSeat);
        }

        showSeatRepository.saveAll(showSeats);

        List<SeatTypeShow> seatTypeShows = new ArrayList<>();

        for (SeatTypePriceDTO priceDTO : requestDTO.getSeatPrices()) {

            SeatTypeShow seatTypeShow = new SeatTypeShow();

            seatTypeShow.setShow(savedShow);
            seatTypeShow.setSeatType(priceDTO.getSeatType());
            seatTypeShow.setPrice(priceDTO.getPrice());

            seatTypeShows.add(seatTypeShow);
        }

        seatTypeShowRepository.saveAll(seatTypeShows);

        log.info("Show created successfully with id : {}", savedShow.getId());

        return savedShow;
    }

    public Show getShowById(Long id) throws InvalidRequestException {

        Optional<Show> showOptional = showRepository.findById(id);

        if (showOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Show Id");
        }

        return showOptional.get();
    }

    public List<ShowSeat> getShowSeats(Show show) {
        return showSeatRepository.findByShow(show);
    }
}
