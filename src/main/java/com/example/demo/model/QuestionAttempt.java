package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAttempt {

    private Long id;
    private LocalDate attemptedOn;
    private Boolean correct;
    private String fieldName;
    private String majorName;
    private String minorName;
    private String sourceLabel;
    private String sourceUrl;
    private String importBatchId;
    private LocalDateTime createdAt;
}
