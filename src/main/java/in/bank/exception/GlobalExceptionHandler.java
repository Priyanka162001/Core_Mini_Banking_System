package in.bank.exception;

import in.bank.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------- Validation errors → 400 ----------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage(),
                        (existing, replacement) -> existing + "; " + replacement // merge multiple messages for same field
                ));

        String message = errors.isEmpty() ? "Request body cannot be empty or malformed" : "Validation failed";

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message(message)
                        .data(errors)
                        .code("VALIDATION_400")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Constraint violations (e.g., @RequestParam, @PathVariable) → 400 ----------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (existing, replacement) -> existing + "; " + replacement // merge multiple messages
                ));

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("Validation failed")
                        .data(errors)
                        .code("VALIDATION_400")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Empty or malformed JSON body → 400 ----------------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmptyOrMalformedBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("Request body cannot be empty or malformed")
                        .data(Collections.emptyMap())
                        .code("VALIDATION_400")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Resource not found → 404 ----------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message(ex.getMessage())
                        .data(Collections.emptyMap())
                        .code("NOT_FOUND_404")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Duplicate resource → 409 ----------------
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message(ex.getMessage())
                        .data(Collections.emptyMap())
                        .code("CONFLICT_409")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Bad request → 400 ----------------
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message(ex.getMessage())
                        .data(Collections.emptyMap())
                        .code("BAD_REQUEST_400")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Business errors → 400 ----------------
    @ExceptionHandler({IllegalArgumentException.class, InsufficientBalanceException.class, AccountFrozenException.class})
    public ResponseEntity<ApiResponse<Object>> handleBusinessErrors(RuntimeException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message(ex.getMessage())
                        .data(Collections.emptyMap())
                        .code("BAD_REQUEST_400")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Authentication errors → 401 ----------------
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuth(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("Invalid username or password")
                        .data(Collections.emptyMap())
                        .code("AUTH_401")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Role-based access → 403 ----------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("You are not authorized to perform this action")
                        .data(Collections.emptyMap())
                        .code("FORBIDDEN_403")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Token issues → 401 | Unexpected runtime errors → 500 ----------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntime(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("token")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.builder()
                            .status("ERROR")
                            .message("Invalid or expired token")
                            .data(Collections.emptyMap())
                            .code("AUTH_401")
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message(ex.getMessage())
                        .data(Collections.emptyMap())
                        .code("SERVER_500")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Missing request parameters → 400 ----------------
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("Missing required request parameter: " + ex.getParameterName())
                        .data(Collections.emptyMap())
                        .code("VALIDATION_400")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ---------------- Generic checked exceptions → 500 ----------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("Something went wrong. Please try again.")
                        .data(Collections.emptyMap())
                        .code("SERVER_500")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    
    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredJwt(
            io.jsonwebtoken.ExpiredJwtException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("JWT token has expired. Please login again.")
                        .data(Collections.emptyMap())
                        .code("TOKEN_EXPIRED_401")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    
    @ExceptionHandler(in.bank.exception.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomAccessDenied(
            in.bank.exception.AccessDeniedException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("You are not authorized to perform this action")
                        .data(Collections.emptyMap())
                        .code("FORBIDDEN_403")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJwt(
            io.jsonwebtoken.JwtException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.builder()
                        .status("ERROR")
                        .message("Invalid JWT token")
                        .data(Collections.emptyMap())
                        .code("INVALID_TOKEN_401")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
  
}