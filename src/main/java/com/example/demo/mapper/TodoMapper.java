package com.example.demo.mapper;

import java.util.List;

import com.example.demo.model.Todo;
import org.apache.ibatis.annotations.Param;

public interface TodoMapper {

    List<Todo> findAll(@Param("sort") String sort, @Param("direction") String direction);

    List<Todo> searchByTitle(@Param("keyword") String keyword, @Param("sort") String sort, @Param("direction") String direction);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int deleteById(Long id);
}
