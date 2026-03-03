package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Word {
    private Long id;
    private String term;
    private String meaning;
    private String fieldName;
    private String majorName;
    private String minorName;
    private String status;
    private Integer wrongCount;
    private LocalDate lastReviewedOn;
    private LocalDateTime createdAt;
}
