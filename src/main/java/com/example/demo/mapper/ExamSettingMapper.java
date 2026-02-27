package com.example.demo.mapper;

import com.example.demo.model.ExamSetting;
import org.apache.ibatis.annotations.Param;

public interface ExamSettingMapper {

    ExamSetting findOne();

    int updateExamDate(@Param("examDate") java.time.LocalDate examDate);
}
