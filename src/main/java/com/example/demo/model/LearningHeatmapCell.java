package com.example.demo.model;

import java.time.LocalDate;
import lombok.Data;

@Data
public class LearningHeatmapCell {
    private LocalDate learningDate;
    private Integer actionCount;
}
