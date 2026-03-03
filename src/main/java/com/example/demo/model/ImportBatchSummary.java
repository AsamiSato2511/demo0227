package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ImportBatchSummary {

    private String importBatchId;
    private Long totalCount;
    private LocalDate minAttemptedOn;
    private LocalDate maxAttemptedOn;
    private LocalDateTime latestCreatedAt;
}
