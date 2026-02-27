package com.example.demo.service;

import java.util.List;

import com.example.demo.mapper.TodoMapper;
import com.example.demo.model.Todo;
import org.springframework.stereotype.Service;

@Service
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public List<Todo> findAll() {
        return todoMapper.findAll();
    }

    public void insert(Todo todo) {
        todoMapper.insert(todo);
    }

    public boolean deleteById(Long id) {
        return todoMapper.deleteById(id) > 0;
    }
}
