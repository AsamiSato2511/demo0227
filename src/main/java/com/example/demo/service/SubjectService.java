package com.example.demo.service;

import com.example.demo.mapper.SubjectMapper;
import com.example.demo.model.Subject;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SubjectService {

    private final SubjectMapper subjectMapper;

    public SubjectService(SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    public List<Subject> findAll() {
        return subjectMapper.findAll();
    }

    public Subject findById(Long id) {
        return subjectMapper.findById(id);
    }

    public boolean updateUnderstanding(Long id, Integer understanding) {
        if (understanding == null || understanding < 0 || understanding > 100) {
            return false;
        }
        return subjectMapper.updateUnderstanding(id, understanding) > 0;
    }

    public double findAverageUnderstanding() {
        Double avg = subjectMapper.findAverageUnderstanding();
        return avg != null ? avg : 0.0;
    }
}
