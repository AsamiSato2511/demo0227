package com.example.demo.mapper;

import com.example.demo.model.ForgettingReminderItem;
import com.example.demo.model.MinorStudySignal;
import com.example.demo.model.Word;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface WordMapper {

    List<Word> findAll(@Param("keyword") String keyword,
                       @Param("minorName") String minorName,
                       @Param("status") String status,
                       @Param("importance") String importance);

    List<Word> findAllPool(@Param("minorName") String minorName);

    List<String> findMinorNames();

    List<Word> findWeakWords(@Param("limit") int limit);

    Word findById(@Param("id") Long id);

    int incrementWrongCount(@Param("id") Long id);

    int markCorrect(@Param("id") Long id);

    int touchReview(@Param("id") Long id);

    int updateQuizEnabledByIds(@Param("ids") List<Long> ids, @Param("quizEnabled") boolean quizEnabled);

    int updateQuizEnabledById(@Param("id") Long id, @Param("quizEnabled") boolean quizEnabled);

    List<MinorStudySignal> findMinorStudySignals();

    List<ForgettingReminderItem> findForgettingReminderItems(@Param("limit") int limit);

    int updateById(Word word);

    int upsert(Word word);
}
