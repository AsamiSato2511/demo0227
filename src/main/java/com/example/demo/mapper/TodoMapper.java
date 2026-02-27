package com.example.demo.mapper;

import java.util.List;

import com.example.demo.model.Todo;

public interface TodoMapper {

    List<Todo> findAll();

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int deleteById(Long id);
}
