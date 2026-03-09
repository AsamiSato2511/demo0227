package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForgettingReminderItem {
    private Long wordId;
    private String term;
    private String minorName;
    private long daysFromLastReview;
}
