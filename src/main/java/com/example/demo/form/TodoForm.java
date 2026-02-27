package com.example.demo.form;

import com.example.demo.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

@Data
public class TodoForm {

    @NotBlank(message = "Author is required")
    @Size(max = 50, message = "Author must be 50 characters or less")
    private String author;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;

    @Size(max = 500, message = "Detail must be 500 characters or less")
    private String detail;

    @NotNull(message = "Priority is required")
    private Priority priority = Priority.MEDIUM;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;
}
