package com.example.demo.mapper;

import com.example.demo.model.Subject;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SubjectMapper {

    List<Subject> findAll();

    Subject findById(@Param("id") Long id);

    int mergeFromCategory(@Param("fieldName") String fieldName,
                          @Param("majorName") String majorName,
                          @Param("minorName") String minorName,
                          @Param("color") String color);
}
