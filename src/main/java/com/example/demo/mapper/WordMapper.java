package com.example.demo.mapper;

import com.example.demo.model.Word;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface WordMapper {

    List<Word> findAll(@Param("keyword") String keyword,
                       @Param("minorName") String minorName,
                       @Param("status") String status);

    List<Word> findAllPool(@Param("minorName") String minorName);

    int incrementWrongCount(@Param("id") Long id);

    int upsert(Word word);
}
