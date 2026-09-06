package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.City;
import com.example.scaler.bms.may2026.model.Theatre;
import com.example.scaler.bms.may2026.repository.CityRepository;
import com.example.scaler.bms.may2026.repository.TheatreRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TheatreServiceImpl {

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private CityRepository cityRepository;

    public Theatre createTheatre(String name, Long cityId) throws InvalidRequestException {

        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Theatre name is required.");
        }

        if (cityId == null) {
            throw new InvalidRequestException("City Id is required.");
        }

        Optional<City> cityOptional = cityRepository.findById(cityId);

        if (cityOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid City Id");
        }

        Theatre theatre = new Theatre();

        theatre.setName(name);
        theatre.setCity(cityOptional.get());
        theatre.setScreens(new ArrayList<>());

        Theatre savedTheatre = theatreRepository.save(theatre);

        log.info("Theatre created successfully with id : {}", savedTheatre.getId());

        return savedTheatre;
    }

    public Theatre getTheatreById(Long id) throws InvalidRequestException {

        Optional<Theatre> theatreOptional = theatreRepository.findById(id);

        if (theatreOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Theatre Id");
        }

        return theatreOptional.get();
    }
}
