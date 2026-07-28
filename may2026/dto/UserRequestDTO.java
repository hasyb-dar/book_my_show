package com.example.scaler.bms.may2026.dto;

import lombok.Data;

@Data
public class UserRequestDTO {

    private Long userId;

    private String userEmail;

    private String password;

    private ErrorDTO errorDTO;

}