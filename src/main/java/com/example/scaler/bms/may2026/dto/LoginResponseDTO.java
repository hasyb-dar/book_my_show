package com.example.scaler.bms.may2026.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {

    private boolean loggedIn;

    private String message;

    private Long userId;

}