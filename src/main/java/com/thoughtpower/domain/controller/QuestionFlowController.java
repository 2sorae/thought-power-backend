package com.thoughtpower.domain.controller;

import com.thoughtpower.domain.dto.*;
import com.thoughtpower.domain.service.AiQuestionException;
import com.thoughtpower.domain.service.QuestionFlowService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/diaries/question-flows")
public class QuestionFlowController {
    private final QuestionFlowService flows;
    public QuestionFlowController(QuestionFlowService flows) { this.flows = flows; }
    @PostMapping
    public QuestionFlowResponse start(@Valid @RequestBody QuestionGenerateRequest request) {
        return flows.start(request);
    }
    @PostMapping("/{flowId}/answers")
    public QuestionFlowResponse submit(@PathVariable UUID flowId,
            @Valid @RequestBody QuestionAnswerSubmitRequest request) {
        return flows.submit(flowId, request);
    }
    @ExceptionHandler(AiQuestionException.class)
    public ResponseEntity<Map<String, String>> handleAi(AiQuestionException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleInvalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", "입력값과 형식을 확인해주세요."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleService(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("code", "INVALID_REQUEST", "message", "질문 순서와 이전 답변을 확인해주세요."));
    }
}
