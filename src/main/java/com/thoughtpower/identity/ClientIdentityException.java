package com.thoughtpower.identity;

import org.springframework.http.HttpStatus;

public class ClientIdentityException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public ClientIdentityException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
