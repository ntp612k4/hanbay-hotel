package com.tpanh.hotel.common.exception;

import com.tpanh.hotel.authentication.exception.AuthenticationRateLimitExceededException;
import com.tpanh.hotel.authentication.exception.EmailAlreadyExistsException;
import com.tpanh.hotel.authentication.exception.PasswordMismatchException;
import com.tpanh.hotel.authentication.exception.PhoneAlreadyExistsException;
import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.user.exception.ProfileValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProfileValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleProfileValidation(
            ProfileValidationException exception, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                Instant.now(),
                exception.getErrors());
        return ResponseEntity.status(exception.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "AUTH_FORBIDDEN", "Bạn không có quyền truy cập", request.getRequestURI(), Instant.now(), Map.of());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Giá trị không hợp lệ"
                                : fieldError.getDefaultMessage(),
                        (first, second) -> first));
        ApiErrorResponse response = new ApiErrorResponse(
                "VALIDATION_FAILED",
                "Dữ liệu gửi lên không hợp lệ",
                request.getRequestURI(),
                Instant.now(),
                fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "MALFORMED_REQUEST",
                "Nội dung yêu cầu không đúng định dạng",
                request.getRequestURI(),
                Instant.now(),
                Map.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestValidation(
            RequestValidationException exception, HttpServletRequest request) {
        return validationErrorResponse(request, Map.of(exception.getField(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String message = exception.getName() + " phải có kiểu dữ liệu hợp lệ: " + getRequiredTypeName(exception);
        return validationErrorResponse(request, Map.of(exception.getName(), message));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException exception, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getErrorCode(), exception.getMessage(), request.getRequestURI(), Instant.now(), Map.of());
        return ResponseEntity.status(exception.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AuthenticationRateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationRateLimit(
            AuthenticationRateLimitExceededException exception, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getErrorCode(), exception.getMessage(), request.getRequestURI(), Instant.now(), Map.of());
        return ResponseEntity.status(exception.getHttpStatus())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception, HttpServletRequest request) {
        return domainErrorResponse(exception, request, Map.of("email", exception.getMessage()));
    }

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handlePhoneAlreadyExists(
            PhoneAlreadyExistsException exception, HttpServletRequest request) {
        return domainErrorResponse(exception, request, Map.of("phone", exception.getMessage()));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordMismatch(
            PasswordMismatchException exception, HttpServletRequest request) {
        return domainErrorResponse(exception, request, Map.of("confirmPassword", exception.getMessage()));
    }

    private ResponseEntity<ApiErrorResponse> domainErrorResponse(
            DomainException exception, HttpServletRequest request, Map<String, String> errors) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getErrorCode(), exception.getMessage(), request.getRequestURI(), Instant.now(), errors);
        return ResponseEntity.status(exception.getHttpStatus()).body(response);
    }

    private ResponseEntity<ApiErrorResponse> validationErrorResponse(
            HttpServletRequest request, Map<String, String> errors) {
        ApiErrorResponse response = new ApiErrorResponse(
                "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ", request.getRequestURI(), Instant.now(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private String getRequiredTypeName(MethodArgumentTypeMismatchException exception) {
        Class<?> requiredType = exception.getRequiredType();
        return requiredType == null ? "value" : requiredType.getSimpleName().toLowerCase();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled exception while processing {}", request.getRequestURI(), exception);
        ApiErrorResponse response = new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Đã xảy ra lỗi không mong muốn",
                request.getRequestURI(),
                Instant.now(),
                Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
