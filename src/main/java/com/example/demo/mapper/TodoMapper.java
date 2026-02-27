package com.example.demo.mapper;

import java.util.List;

import com.example.demo.model.Todo;

public interface TodoMapper {

    List<Todo> findAll();

    int insert(Todo todo);

    int deleteById(Long id);
}
