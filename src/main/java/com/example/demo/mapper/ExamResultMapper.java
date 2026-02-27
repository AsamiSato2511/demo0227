package com.example.demo.mapper;

import com.example.demo.model.ExamResult;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface ExamResultMapper {

    List<ExamResult> findRecent(@Param("limit") int limit);

    int insert(ExamResult examResult);

    Map<String, Object> findStats();
}
