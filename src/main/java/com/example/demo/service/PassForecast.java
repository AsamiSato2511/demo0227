package com.example.demo.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PassForecast {
    private double passProbability;
    private double predictedScore;
}
