package com.example.demo.mapper;

import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.CategoryRateRow;
import com.example.demo.model.ImportBatchSummary;
import com.example.demo.model.QuestionAttempt;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface QuestionAttemptMapper {

    int insertBatch(@Param("items") List<QuestionAttempt> items);

    List<ImportBatchSummary> findImportBatches();

    List<CorrectRateSummary> findFieldRates(@Param("fromDate") LocalDate fromDate,
                                            @Param("importBatchId") String importBatchId);

    List<CorrectRateSummary> findMinorRates(@Param("fromDate") LocalDate fromDate,
                                            @Param("importBatchId") String importBatchId);

    List<CategoryRateRow> findCategoryRates(@Param("fromDate") LocalDate fromDate,
                                            @Param("importBatchId") String importBatchId);

    List<CorrectRateSummary> findWorstMinorRates(@Param("fromDate") LocalDate fromDate,
                                                 @Param("importBatchId") String importBatchId,
                                                 @Param("limit") int limit);

    List<QuestionAttempt> findRecent(@Param("fromDate") LocalDate fromDate,
                                     @Param("importBatchId") String importBatchId,
                                     @Param("limit") int limit);
}
