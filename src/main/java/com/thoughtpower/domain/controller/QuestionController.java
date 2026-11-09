package com.thoughtpower.domain.controller;

import com.thoughtpower.domain.dto.QuestionGenerateRequest;
import com.thoughtpower.domain.dto.QuestionGenerateResponse;
import com.thoughtpower.domain.service.QuestionService;
import com.thoughtpower.domain.service.AiQuestionException;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
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
            @Valid @RequestBody QuestionGenerateRequest request
    ) {
        return questionService.generateQuestion(request);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_REQUEST",
                "message", "필수 입력값과 입력 형식을 확인해주세요."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleQuestionException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "code", "INVALID_REQUEST",
                "message", exception.getReason() == null
                        ? "입력값을 확인해주세요." : exception.getReason()));
    }
    @ExceptionHandler(AiQuestionException.class)
    public ResponseEntity<Map<String, String>> handleAiException(AiQuestionException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of(
                "code", exception.getCode(), "message", exception.getMessage()));
    }
}
