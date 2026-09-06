package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.exception.SeatNotAvailableException;
import com.example.scaler.bms.may2026.model.Booking;
import com.example.scaler.bms.may2026.model.BookingStatus;
import com.example.scaler.bms.may2026.model.SeatTypeShow;
import com.example.scaler.bms.may2026.model.Show;
import com.example.scaler.bms.may2026.model.ShowSeat;
import com.example.scaler.bms.may2026.model.ShowSeatStatus;
import com.example.scaler.bms.may2026.model.User;
import com.example.scaler.bms.may2026.repository.BookingRepository;
import com.example.scaler.bms.may2026.repository.SeatTypeShowRepository;
import com.example.scaler.bms.may2026.repository.ShowRepository;
import com.example.scaler.bms.may2026.repository.ShowSeatRepository;
import com.example.scaler.bms.may2026.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingServiceImpl {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatTypeShowRepository seatTypeShowRepository;

    @Autowired
    private ReentrantLock lock;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Booking createBooking(List<String> seatNumbers,
                                 Long showId,
                                 Long userId)
            throws InvalidRequestException, SeatNotAvailableException {

        Optional<Show> showOptional = showRepository.findById(showId);

        if (showOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Show Id");
        }

        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid User Id");
        }

        Show show = showOptional.get();
        User user = userOptional.get();

        List<ShowSeat> allShowSeats = showSeatRepository.findByShow(show);
        List<ShowSeat> lockedSeats = new ArrayList<>();

        lock.lock();

        try {

            for (ShowSeat showSeat : allShowSeats) {

                if (seatNumbers.contains(showSeat.getSeat().getSeatNumber())
                        && showSeat.getShowSeatStatus() != ShowSeatStatus.AVAILABLE) {

                    throw new SeatNotAvailableException(
                            "Seat "
                                    + showSeat.getSeat().getSeatNumber()
                                    + " is not available.");
                }
            }

            for (ShowSeat showSeat : allShowSeats) {

                if (seatNumbers.contains(showSeat.getSeat().getSeatNumber())) {

                    showSeat.setShowSeatStatus(ShowSeatStatus.LOCKED);

                    lockedSeats.add(showSeat);
                }
            }

            showSeatRepository.saveAll(lockedSeats);

        } finally {

            lock.unlock();
        }

        Booking booking = new Booking();

        booking.setBookingCreatedAt(new Date());
        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        booking.setCreatedBy(user);
        booking.setShow(show);
        booking.setShowSeats(lockedSeats);
        booking.setPayment(null);
        booking.setTotalAmount(calculateTotalAmount(show, lockedSeats));

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking created successfully : {}", savedBooking.getId());

        return savedBooking;
    }
    public Booking getBookingById(Long id) throws InvalidRequestException {

        Optional<Booking> bookingOptional = bookingRepository.findById(id);

        if (bookingOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Ticket Id");
        }

        return bookingOptional.get();
    }

    private Double calculateTotalAmount(Show show,
                                        List<ShowSeat> lockedSeats) {

        double total = 0.0;

        for (ShowSeat showSeat : lockedSeats) {

            Optional<SeatTypeShow> seatTypeShow =
                    seatTypeShowRepository.findByShowAndSeatType(
                            show,
                            showSeat.getSeat().getSeatType());

            if (seatTypeShow.isPresent()) {
                total += seatTypeShow.get().getPrice();
            }
        }

        return total;
    }
}