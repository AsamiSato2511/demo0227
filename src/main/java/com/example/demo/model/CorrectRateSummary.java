package com.example.demo.model;

import lombok.Data;

@Data
public class CorrectRateSummary {

    private String name;
    private Long totalCount;
    private Long correctCount;
    private Double correctRate;
}
