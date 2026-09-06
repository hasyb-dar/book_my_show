package com.example.scaler.bms.may2026.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.CityResponseDTO;
import com.example.scaler.bms.may2026.dto.CreateCityRequestDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.City;
import com.example.scaler.bms.may2026.service.CityServiceImpl;
import com.example.scaler.bms.may2026.translator.CityTranslator;

@RestController
@RequestMapping("/city")
public class CityController {

    @Autowired
    private CityServiceImpl cityServiceImpl;

    @PostMapping
    public ResponseEntity<CityResponseDTO> createCity(
            @RequestBody CreateCityRequestDTO requestDTO) throws InvalidRequestException {

        City city = cityServiceImpl.createCity(requestDTO.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(CityTranslator.transform(city));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponseDTO> getCity(@PathVariable Long id)
            throws InvalidRequestException {

        City city = cityServiceImpl.getCityById(id);

        return ResponseEntity.ok(CityTranslator.transform(city));
    }

    @GetMapping
    public ResponseEntity<List<CityResponseDTO>> getAllCities() {

        List<CityResponseDTO> cities = cityServiceImpl.getAllCities()
                .stream()
                .map(CityTranslator::transform)
                .collect(Collectors.toList());

        return ResponseEntity.ok(cities);
    }
}
