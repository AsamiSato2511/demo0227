package com.example.demo.mapper;

import java.util.List;

import com.example.demo.model.Category;
import org.apache.ibatis.annotations.Param;

public interface CategoryMapper {

    List<Category> findAll();

    Category findById(@Param("id") Long id);
}
