package com.example.demo.service;

import java.util.List;

import com.example.demo.mapper.TodoMapper;
import com.example.demo.model.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public Page<Todo> findPage(String keyword, Long subjectId, String sort, String direction, Pageable pageable) {
        long total = todoMapper.countByConditions(keyword, subjectId);
        List<Todo> content = todoMapper.findPage(keyword, subjectId, sort, direction, pageable.getPageSize(), pageable.getOffset());
        return new PageImpl<>(content, pageable, total);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public List<Todo> findAllForExport() {
        return todoMapper.findAllForExport();
    }

    public void insert(Todo todo) {
        todoMapper.insert(todo);
    }

    public boolean update(Todo todo) {
        return todoMapper.update(todo) > 0;
    }

    public boolean toggleCompleted(Long id) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            return false;
        }
        boolean current = Boolean.TRUE.equals(todo.getCompleted());
        todo.setCompleted(!current);
        return todoMapper.update(todo) > 0;
    }

    public boolean deleteById(Long id) {
        return todoMapper.deleteById(id) > 0;
    }

    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return todoMapper.deleteByIds(ids);
    }
}
