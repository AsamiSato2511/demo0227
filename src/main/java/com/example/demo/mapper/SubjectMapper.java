package com.example.demo.mapper;

import com.example.demo.model.Subject;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SubjectMapper {

    List<Subject> findAll();

    Subject findById(@Param("id") Long id);

    int updateUnderstanding(@Param("id") Long id, @Param("understanding") Integer understanding);

    int updateUnderstandingByCategory(@Param("fieldName") String fieldName,
                                      @Param("majorName") String majorName,
                                      @Param("minorName") String minorName,
                                      @Param("understanding") Integer understanding);

    int mergeFromCategory(@Param("fieldName") String fieldName,
                          @Param("majorName") String majorName,
                          @Param("minorName") String minorName,
                          @Param("color") String color);

    Double findAverageUnderstanding();
}
