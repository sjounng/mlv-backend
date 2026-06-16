package kr.maribel.backend.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiError(Instant.now(), exception.getStatus().value(), exception.getCode(), exception.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", request.getRequestURI(), fields));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "DATA_CONFLICT", "이미 처리된 요청이거나 중복된 데이터입니다.", request.getRequestURI(), null));
    }

    public record ApiError(
            Instant timestamp,
            int status,
            String code,
            String message,
            String path,
            Map<String, String> fields
    ) {
    }
}
