package com.example.demo.mapper;

import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.CategoryRateRow;
import com.example.demo.model.QuestionAttempt;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface QuestionAttemptMapper {

    int insertBatch(@Param("items") List<QuestionAttempt> items);

    List<CorrectRateSummary> findFieldRates(@Param("fromDate") LocalDate fromDate);

    List<CorrectRateSummary> findMinorRates(@Param("fromDate") LocalDate fromDate);

    List<CategoryRateRow> findCategoryRates(@Param("fromDate") LocalDate fromDate);

    List<CorrectRateSummary> findWorstMinorRates(@Param("fromDate") LocalDate fromDate, @Param("limit") int limit);

    List<QuestionAttempt> findRecent(@Param("fromDate") LocalDate fromDate, @Param("limit") int limit);
}
