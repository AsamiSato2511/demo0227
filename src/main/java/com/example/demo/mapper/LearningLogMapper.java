package com.example.demo.mapper;

import com.example.demo.model.LearningHeatmapCell;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LearningLogMapper {

    int insert(@Param("actionType") String actionType,
               @Param("wordId") Long wordId,
               @Param("answeredAt") LocalDateTime answeredAt,
               @Param("correct") Boolean correct);

    List<LearningHeatmapCell> findDailyCounts(@Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);
}
