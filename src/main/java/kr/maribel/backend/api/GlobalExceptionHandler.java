package kr.maribel.backend.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "MALFORMED_REQUEST", "요청 본문을 해석할 수 없습니다.", request.getRequestURI(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return ResponseEntity.status(413)
                .body(new ApiError(Instant.now(), 413, "FILE_TOO_LARGE", "업로드 가능한 파일 크기를 초과했습니다.", request.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) throws Exception {
        // 인증/인가 예외는 Security 필터 체인이 401/403 응답을 만들도록 다시 던진다.
        if (exception instanceof AccessDeniedException || exception instanceof AuthenticationException) {
            throw exception;
        }
        // 404(NoResourceFound), 405(MethodNotSupported) 등 스프링 MVC 표준 예외는 원래 상태 코드를 유지한다.
        if (exception instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            String code = status == 404 ? "NOT_FOUND" : "REQUEST_ERROR";
            return ResponseEntity.status(status)
                    .body(new ApiError(Instant.now(), status, code, "요청을 처리할 수 없습니다.", request.getRequestURI(), null));
        }
        log.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.internalServerError()
                .body(new ApiError(Instant.now(), 500, "INTERNAL_ERROR", "서버 오류가 발생했습니다.", request.getRequestURI(), null));
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
