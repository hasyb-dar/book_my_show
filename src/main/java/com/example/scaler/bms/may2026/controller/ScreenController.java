package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.CreateScreenRequestDTO;
import com.example.scaler.bms.may2026.dto.ScreenResponseDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Screen;
import com.example.scaler.bms.may2026.service.ScreenServiceImpl;
import com.example.scaler.bms.may2026.translator.ScreenTranslator;

@RestController
@RequestMapping("/screen")
public class ScreenController {

    @Autowired
    private ScreenServiceImpl screenServiceImpl;

    @PostMapping
    public ResponseEntity<ScreenResponseDTO> createScreen(
            @RequestBody CreateScreenRequestDTO requestDTO) throws InvalidRequestException {

        Screen screen = screenServiceImpl.createScreen(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScreenTranslator.transform(screen, requestDTO.getTheatreId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreenResponseDTO> getScreen(@PathVariable Long id)
            throws InvalidRequestException {

        Screen screen = screenServiceImpl.getScreenById(id);

        return ResponseEntity.ok(ScreenTranslator.transform(screen, null));
    }
}
