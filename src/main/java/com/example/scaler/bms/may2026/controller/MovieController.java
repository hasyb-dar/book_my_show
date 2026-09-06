package com.example.scaler.bms.may2026.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.scaler.bms.may2026.dto.CreateMovieRequestDTO;
import com.example.scaler.bms.may2026.dto.MovieResponseDTO;
import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Movie;
import com.example.scaler.bms.may2026.service.MovieServiceImpl;
import com.example.scaler.bms.may2026.translator.MovieTranslator;

@RestController
@RequestMapping("/movie")
public class MovieController {

    @Autowired
    private MovieServiceImpl movieServiceImpl;

    @PostMapping
    public ResponseEntity<MovieResponseDTO> createMovie(
            @RequestBody CreateMovieRequestDTO requestDTO) throws InvalidRequestException {

        Movie movie = movieServiceImpl.createMovie(
                requestDTO.getName(),
                requestDTO.getRating(),
                requestDTO.getFeatures(),
                requestDTO.getLanguages(),
                requestDTO.getDuration(),
                requestDTO.getActorIds());

        return ResponseEntity.status(HttpStatus.CREATED).body(MovieTranslator.transform(movie));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDTO> getMovie(@PathVariable Long id)
            throws InvalidRequestException {

        Movie movie = movieServiceImpl.getMovieById(id);

        return ResponseEntity.ok(MovieTranslator.transform(movie));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponseDTO>> getAllMovies() {

        List<MovieResponseDTO> movies = movieServiceImpl.getAllMovies()
                .stream()
                .map(MovieTranslator::transform)
                .collect(Collectors.toList());

        return ResponseEntity.ok(movies);
    }
}
