package com.example.demo.service;

import java.util.List;

import com.example.demo.mapper.CategoryMapper;
import com.example.demo.model.Category;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> findAll() {
        return categoryMapper.findAll();
    }

    public Category findById(Long id) {
        return categoryMapper.findById(id);
    }
}
