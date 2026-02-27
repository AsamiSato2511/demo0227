package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamResult {

    private Long id;
    private LocalDate takenOn;
    private String source;
    private Integer scoreTotal;
    private String memo;
    private LocalDateTime createdAt;
}
