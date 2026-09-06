package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.CreateTheatreRequestDTO;
import com.example.scaler.bms.may2026.dto.TheatreResponseDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Theatre;
import com.example.scaler.bms.may2026.service.TheatreServiceImpl;
import com.example.scaler.bms.may2026.translator.TheatreTranslator;

@RestController
@RequestMapping("/theatre")
public class TheatreController {

    @Autowired
    private TheatreServiceImpl theatreServiceImpl;

    @PostMapping
    public ResponseEntity<TheatreResponseDTO> createTheatre(
            @RequestBody CreateTheatreRequestDTO requestDTO) throws InvalidRequestException {

        Theatre theatre = theatreServiceImpl.createTheatre(
                requestDTO.getName(),
                requestDTO.getCityId());

        return ResponseEntity.status(HttpStatus.CREATED).body(TheatreTranslator.transform(theatre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheatreResponseDTO> getTheatre(@PathVariable Long id)
            throws InvalidRequestException {

        Theatre theatre = theatreServiceImpl.getTheatreById(id);

        return ResponseEntity.ok(TheatreTranslator.transform(theatre));
    }
}
