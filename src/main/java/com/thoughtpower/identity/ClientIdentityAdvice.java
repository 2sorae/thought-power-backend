package com.thoughtpower.identity;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ClientIdentityAdvice {
    @ExceptionHandler(ClientIdentityException.class)
    public ResponseEntity<Map<String, String>> handle(ClientIdentityException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }
}
