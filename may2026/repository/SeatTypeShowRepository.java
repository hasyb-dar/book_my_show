package com.example.scaler.bms.may2026.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.scaler.bms.may2026.model.SeatType;
import com.example.scaler.bms.may2026.model.SeatTypeShow;
import com.example.scaler.bms.may2026.model.Show;

@Repository
public interface SeatTypeShowRepository extends JpaRepository<SeatTypeShow, Long> {

    Optional<SeatTypeShow> findByShowAndSeatType(
            Show show,
            SeatType seatType);

}
