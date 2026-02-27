package com.example.demo.form;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class ExamSettingForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate examDate;
}
