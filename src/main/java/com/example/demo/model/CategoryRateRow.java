package com.example.demo.model;

import lombok.Data;

@Data
public class CategoryRateRow {

    private String fieldName;
    private String majorName;
    private String minorName;
    private Long totalCount;
    private Long correctCount;
    private Double correctRate;
}
