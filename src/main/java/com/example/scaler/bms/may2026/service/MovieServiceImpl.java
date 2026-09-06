package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.Actor;
import com.example.scaler.bms.may2026.model.Feature;
import com.example.scaler.bms.may2026.model.Movie;
import com.example.scaler.bms.may2026.repository.ActorRepository;
import com.example.scaler.bms.may2026.repository.MovieRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MovieServiceImpl {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ActorRepository actorRepository;

    public Movie createMovie(String name,
                              String rating,
                              List<Feature> features,
                              List<String> languages,
                              Long duration,
                              List<Long> actorIds) throws InvalidRequestException {

        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Movie name is required.");
        }

        if (duration == null || duration <= 0) {
            throw new InvalidRequestException("Movie duration must be a positive number.");
        }

        List<Actor> actors = new ArrayList<>();

        if (actorIds != null) {

            for (Long actorId : actorIds) {

                Optional<Actor> actorOptional = actorRepository.findById(actorId);

                if (actorOptional.isEmpty()) {
                    throw new InvalidRequestException("Invalid Actor Id: " + actorId);
                }

                actors.add(actorOptional.get());
            }
        }

        Movie movie = new Movie();

        movie.setName(name);
        movie.setRating(rating);
        movie.setFeatures(features);
        movie.setLanguages(languages);
        movie.setDuration(duration);
        movie.setActors(actors);

        Movie savedMovie = movieRepository.save(movie);

        log.info("Movie created successfully with id : {}", savedMovie.getId());

        return savedMovie;
    }

    public Movie getMovieById(Long id) throws InvalidRequestException {

        Optional<Movie> movieOptional = movieRepository.findById(id);

        if (movieOptional.isEmpty()) {
            throw new InvalidRequestException("Invalid Movie Id");
        }

        return movieOptional.get();
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }
}
