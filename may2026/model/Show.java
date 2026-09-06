package com.example.scaler.bms.may2026.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Show extends BaseModel {

    @ManyToOne
    private Movie movie;

    private Date startTime;

    @ManyToOne
    private Screen screen;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<Feature> features;
}