package com.thoughtpower.domain.controller;

import com.thoughtpower.domain.dto.DiaryCreateRequest;
import com.thoughtpower.domain.dto.DiaryCreateResponse;
import com.thoughtpower.domain.service.DiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping
    public ResponseEntity<DiaryCreateResponse> createDiary(
            @Valid @RequestBody DiaryCreateRequest request
    ) {
        DiaryCreateResponse response = diaryService.createDiary(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @ExceptionHandler(DiaryService.DiaryApiException.class)
    public ResponseEntity<Map<String, String>> handleDiaryException(
            DiaryService.DiaryApiException exception
    ) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(Map.of(
                        "code", exception.getCode(),
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            Exception exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "code", "INVALID_REQUEST",
                        "message", "필수 입력값과 입력 형식을 확인해주세요."
                ));
    }
}