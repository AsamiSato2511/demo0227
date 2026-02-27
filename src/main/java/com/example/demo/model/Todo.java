package com.example.demo.model;

import java.time.LocalDateTime;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Todo {

    private Long id;
    private String title;
    private Boolean completed;
    private Priority priority;
    private Category category;
    private LocalDate deadline;
    private LocalDateTime createdAt;

    public boolean isCompleted() {
        return Boolean.TRUE.equals(completed);
    }

    public boolean isOverdue() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }

    public boolean isNearDeadline() {
        if (deadline == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !deadline.isBefore(today) && !deadline.isAfter(today.plusDays(3));
    }
}
