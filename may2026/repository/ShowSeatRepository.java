package com.example.scaler.bms.may2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.scaler.bms.may2026.model.Show;
import com.example.scaler.bms.may2026.model.ShowSeat;
import com.example.scaler.bms.may2026.model.ShowSeatStatus;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShow(Show show);

    List<ShowSeat> findByShowAndShowSeatStatus(
            Show show,
            ShowSeatStatus status);

}