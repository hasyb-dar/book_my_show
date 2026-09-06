package com.example.scaler.bms.may2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.scaler.bms.may2026.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}