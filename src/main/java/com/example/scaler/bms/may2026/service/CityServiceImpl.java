package com.example.scaler.bms.may2026.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.City;
import com.example.scaler.bms.may2026.repository.CityRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CityServiceImpl {

    @Autowired
    private CityRepository cityRepository;

    public City createCity(String name) throws InvalidRequestException {

        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("City name is required.");
        }

        City city = new City();
        city.setName(name);

        City savedCity = cityRepository.save(city);

        log.info("City created successfully with id : {}", savedCity.getId());

        return savedCity;
    }

    public City getCityById(Long id) throws InvalidRequestException {

        Optional<City> cityOptional = cityRepository.findById(id);

        if (cityOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid City Id");
        }

        return cityOptional.get();
    }

    public List<City> getAllCities() {
        return cityRepository.findAll();
    }
}
