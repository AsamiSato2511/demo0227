package com.example.demo.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldImpact {
    private String fieldName;
    private double accuracyRate;
    private long totalQuestions;
    private double weightPercent;
    private double gainPointsForPlus10Rate;
}
