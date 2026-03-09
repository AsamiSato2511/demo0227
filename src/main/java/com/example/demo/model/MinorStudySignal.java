package com.example.demo.model;

import lombok.Data;

@Data
public class MinorStudySignal {
    private String minorName;
    private Long wrongCountTotal;
    private Long dueReviewCount;
}
