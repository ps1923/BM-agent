package com.bmhs.experimentcreation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of(
                "code", exception.code(),
                "message", exception.getMessage(),
                "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数无效");
        return ResponseEntity.badRequest().body(Map.of(
                "code", "VALIDATION_FAILED",
                "message", message,
                "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException exception) {
        return error(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_JSON", "请求 JSON 格式或字段值无效");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException exception) {
        return error(org.springframework.http.HttpStatus.CONFLICT, "DATABASE_CONFLICT", "数据状态冲突，请刷新后重试");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, Object>> handleDatabase(DataAccessException exception) {
        return error(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_UNAVAILABLE", "数据库暂时不可用，请稍后重试");
    }

    private ResponseEntity<Map<String, Object>> error(org.springframework.http.HttpStatus status,
                                                       String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "code", code, "message", message, "timestamp", Instant.now().toString()));
    }
}
