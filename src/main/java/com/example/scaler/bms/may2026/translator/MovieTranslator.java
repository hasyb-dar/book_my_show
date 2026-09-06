package com.example.scaler.bms.may2026.translator;

import java.util.List;
import java.util.stream.Collectors;

import com.example.scaler.bms.may2026.dto.MovieResponseDTO;
import com.example.scaler.bms.may2026.model.Movie;

public class MovieTranslator {

    private MovieTranslator() {
        // Prevent instantiation
    }

    public static MovieResponseDTO transform(Movie movie) {

        MovieResponseDTO dto = new MovieResponseDTO();

        dto.setMovieId(movie.getId());
        dto.setName(movie.getName());
        dto.setRating(movie.getRating());
        dto.setFeatures(movie.getFeatures());
        dto.setLanguages(movie.getLanguages());
        dto.setDuration(movie.getDuration());

        List<String> actorNames = movie.getActors() == null
                ? List.of()
                : movie.getActors().stream()
                        .map(actor -> actor.getName())
                        .collect(Collectors.toList());

        dto.setActorNames(actorNames);

        return dto;
    }
}
