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
    private Boolean quizEnabled;
    private String choice1;
    private String choice2;
    private String choice3;
    private String choice4;
    private Integer answerIndex;
    private LocalDate lastReviewedOn;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime lastCorrectAt;
    private Integer priority;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
