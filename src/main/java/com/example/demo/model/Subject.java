package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    private Long id;
    private String fieldName;
    private String majorName;
    private String minorName;
    private String color;
}
