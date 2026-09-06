package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.CreateShowRequestDTO;
import com.example.scaler.bms.may2026.dto.ShowResponseDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Show;
import com.example.scaler.bms.may2026.service.ShowServiceImpl;
import com.example.scaler.bms.may2026.translator.ShowTranslator;

@RestController
@RequestMapping("/show")
public class ShowController {

    @Autowired
    private ShowServiceImpl showServiceImpl;

    @PostMapping
    public ResponseEntity<ShowResponseDTO> createShow(
            @RequestBody CreateShowRequestDTO requestDTO) throws InvalidRequestException {

        Show show = showServiceImpl.createShow(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ShowTranslator.transform(show, showServiceImpl.getShowSeats(show)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponseDTO> getShow(@PathVariable Long id)
            throws InvalidRequestException {

        Show show = showServiceImpl.getShowById(id);

        return ResponseEntity.ok(ShowTranslator.transform(show, showServiceImpl.getShowSeats(show)));
    }
}
