package com.example.demo.model;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

    public boolean isCompleted() {
        return Boolean.TRUE.equals(completed);
    }
}
