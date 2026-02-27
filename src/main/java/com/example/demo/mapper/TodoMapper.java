package com.example.demo.mapper;

import java.util.List;

import com.example.demo.model.Todo;
import org.apache.ibatis.annotations.Param;

public interface TodoMapper {

    List<Todo> findPage(@Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("sort") String sort,
                        @Param("direction") String direction,
                        @Param("limit") int limit,
                        @Param("offset") long offset);

    List<Todo> findAllForExport();

    long countByConditions(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int deleteById(Long id);

    int deleteByIds(@Param("ids") List<Long> ids);
}

