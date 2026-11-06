package com.thoughtpower.domain.controller;

import com.thoughtpower.domain.dto.QuestionGenerateRequest;
import com.thoughtpower.domain.dto.QuestionGenerateResponse;
import com.thoughtpower.domain.service.QuestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries")
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/questions")
    public QuestionGenerateResponse generateQuestion(
            @RequestBody QuestionGenerateRequest request
    ) {
        return questionService.generateQuestion(request);
    }
}
