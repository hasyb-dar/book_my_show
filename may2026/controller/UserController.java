package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.ErrorDTO;
import com.example.scaler.bms.may2026.dto.UserRequestDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.User;
import com.example.scaler.bms.may2026.service.UserServiceImpl;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserServiceImpl userServiceImpl;

    @PostMapping
    public ResponseEntity<UserRequestDTO> createUser(
            @RequestBody UserRequestDTO userRequestDTO) {

        UserRequestDTO response = new UserRequestDTO();

        try {

            User createdUser = userServiceImpl.registerUser(
                    userRequestDTO.getUserEmail(),
                    userRequestDTO.getPassword());

            response.setUserId(createdUser.getId());
            response.setUserEmail(createdUser.getEmail());
            response.setPassword(createdUser.getPassword());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidRequestException e) {

            ErrorDTO errorDTO = new ErrorDTO();
            errorDTO.setErrorCode("ERROR_CODE_101");
            errorDTO.setErrorMsg(e.getMessage());

            response.setErrorDTO(errorDTO);

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Boolean> loginUser(
            @RequestBody UserRequestDTO userRequestDTO)
            throws InvalidRequestException {

        boolean isLoggedIn = userServiceImpl.login(
                userRequestDTO.getUserEmail(),
                userRequestDTO.getPassword());

        return ResponseEntity.ok(isLoggedIn);
    }
}