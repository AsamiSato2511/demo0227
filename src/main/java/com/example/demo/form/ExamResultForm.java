package com.example.demo.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class ExamResultForm {

    @NotNull(message = "受験日は必須です")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate takenOn;

    @NotBlank(message = "回・年度は必須です")
    @Size(max = 100, message = "回・年度は100文字以内で入力してください")
    private String source;

    @NotNull(message = "得点は必須です")
    @Min(value = 0, message = "得点は0以上で入力してください")
    @Max(value = 100, message = "得点は100以下で入力してください")
    private Integer scoreTotal;

    @Size(max = 500, message = "メモは500文字以内で入力してください")
    private String memo;
}
