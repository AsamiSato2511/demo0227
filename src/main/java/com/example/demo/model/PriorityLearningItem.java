package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriorityLearningItem {
    private String minorName;
    private double correctRate;
    private long questionCount;
    private long recentWrongCount;
    private long wrongCount;
    private long dueReviewCount;
    private double priorityScore;
}
