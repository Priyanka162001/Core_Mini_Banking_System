package in.bank.exception;

import in.bank.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private ConstraintViolationException constraintViolationException;

    @Mock
    private ConstraintViolation<?> constraintViolation;

    @Mock
    private Path path;

    // ================= TEST 1: Handle Validation with Errors =================
    @Test
    @DisplayName("TC1: Handle MethodArgumentNotValidException with validation errors")
    void testHandleValidation_WithErrors() {
        // Given
        FieldError fieldError = new FieldError("object", "email", "Email cannot be blank");
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleValidation(methodArgumentNotValidException);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getStatus()).isEqualTo("ERROR");
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getCode()).isEqualTo("VALIDATION_400");
        assertThat(body.getData()).isNotNull();
    }

    // ================= TEST 2: Handle Validation with Empty Errors (Covers yellow line) =================
    @Test
    @DisplayName("TC2: Handle MethodArgumentNotValidException with empty errors")
    void testHandleValidation_WithEmptyErrors() {
        // Given
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleValidation(methodArgumentNotValidException);
        ApiResponse<Object> body = response.getBody();

        // Then - This covers: errors.isEmpty() ? "Request body cannot be empty or malformed"
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Request body cannot be empty or malformed");
    }

    // ================= TEST 3: Handle ConstraintViolationException =================
    @Test
    @DisplayName("TC3: Handle ConstraintViolationException with violations")
    void testHandleConstraintViolation() {
        // Given
        when(constraintViolationException.getConstraintViolations()).thenReturn(Set.of(constraintViolation));
        when(constraintViolation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("email");
        when(constraintViolation.getMessage()).thenReturn("Email is required");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleConstraintViolation(constraintViolationException);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getCode()).isEqualTo("VALIDATION_400");
    }

    // ================= TEST 4: Handle HttpMessageNotReadableException =================
    @Test
    @DisplayName("TC4: Handle HttpMessageNotReadableException")
    void testHandleEmptyOrMalformedBody() {
        // Given
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleEmptyOrMalformedBody(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Request body cannot be empty or malformed");
    }

    // ================= TEST 5: Handle ResourceNotFoundException =================
    @Test
    @DisplayName("TC5: Handle ResourceNotFoundException")
    void testHandleNotFound() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleNotFound(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.getMessage()).isEqualTo("User not found");
        assertThat(body.getCode()).isEqualTo("NOT_FOUND_404");
    }

    // ================= TEST 6: Handle DuplicateResourceException =================
    @Test
    @DisplayName("TC6: Handle DuplicateResourceException")
    void testHandleDuplicate() {
        // Given
        DuplicateResourceException ex = new DuplicateResourceException("Email already exists");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleDuplicate(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body.getMessage()).isEqualTo("Email already exists");
        assertThat(body.getCode()).isEqualTo("CONFLICT_409");
    }

    // ================= TEST 7: Handle BadRequestException =================
    @Test
    @DisplayName("TC7: Handle BadRequestException")
    void testHandleBadRequest() {
        // Given
        BadRequestException ex = new BadRequestException("Invalid request");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBadRequest(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Invalid request");
        assertThat(body.getCode()).isEqualTo("BAD_REQUEST_400");
    }

    // ================= TEST 8: Handle IllegalArgumentException =================
    @Test
    @DisplayName("TC8: Handle IllegalArgumentException")
    void testHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBusinessErrors(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Invalid argument");
    }

    // ================= TEST 9: Handle InsufficientBalanceException =================
    @Test
    @DisplayName("TC9: Handle InsufficientBalanceException")
    void testHandleInsufficientBalance() {
        // Given
        InsufficientBalanceException ex = new InsufficientBalanceException("Insufficient balance");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBusinessErrors(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Insufficient balance");
    }

    // ================= TEST 10: Handle AccountFrozenException =================
    @Test
    @DisplayName("TC10: Handle AccountFrozenException")
    void testHandleAccountFrozen() {
        // Given
        AccountFrozenException ex = new AccountFrozenException("Account is frozen");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBusinessErrors(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Account is frozen");
    }

    // ================= TEST 11: Handle BadCredentialsException =================
    @Test
    @DisplayName("TC11: Handle BadCredentialsException")
    void testHandleBadCredentials() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAuth(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body.getMessage()).isEqualTo("Invalid username or password");
        assertThat(body.getCode()).isEqualTo("AUTH_401");
    }

    // ================= TEST 12: Handle AccessDeniedException =================
    @Test
    @DisplayName("TC12: Handle AccessDeniedException")
    void testHandleAccessDenied() {
        // Given
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAccessDenied(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body.getMessage()).isEqualTo("You are not authorized to perform this action");
        assertThat(body.getCode()).isEqualTo("FORBIDDEN_403");
    }

    // ================= TEST 13: Handle RuntimeException with token message =================
    @Test
    @DisplayName("TC13: Handle RuntimeException with token message")
    void testHandleRuntime_WithTokenMessage() {
        // Given
        RuntimeException ex = new RuntimeException("Invalid token");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleRuntime(ex);
        ApiResponse<Object> body = response.getBody();

        // Then - Covers the IF branch
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body.getMessage()).isEqualTo("Invalid or expired token");
        assertThat(body.getCode()).isEqualTo("AUTH_401");
    }

    // ================= TEST 14: Handle RuntimeException without token message =================
    @Test
    @DisplayName("TC14: Handle RuntimeException without token message")
    void testHandleRuntime_WithoutTokenMessage() {
        // Given
        RuntimeException ex = new RuntimeException("Database connection failed");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleRuntime(ex);
        ApiResponse<Object> body = response.getBody();

        // Then - Covers the ELSE branch
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getMessage()).isEqualTo("Database connection failed");
        assertThat(body.getCode()).isEqualTo("SERVER_500");
    }

    // ================= TEST 15: Handle RuntimeException with null message =================
    @Test
    @DisplayName("TC15: Handle RuntimeException with null message")
    void testHandleRuntime_WithNullMessage() {
        // Given
        RuntimeException ex = new RuntimeException();

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleRuntime(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getCode()).isEqualTo("SERVER_500");
    }

    // ================= TEST 16: Handle MissingServletRequestParameterException =================
    @Test
    @DisplayName("TC16: Handle MissingServletRequestParameterException")
    void testHandleMissingParams() {
        // Given
        MissingServletRequestParameterException ex = 
            new MissingServletRequestParameterException("email", "String");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleMissingParams(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Missing required request parameter: email");
        assertThat(body.getCode()).isEqualTo("VALIDATION_400");
    }

    // ================= TEST 17: Handle generic Exception =================
    @Test
    @DisplayName("TC17: Handle generic Exception")
    void testHandleGeneric() {
        // Given
        Exception ex = new Exception("Unexpected error");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGeneric(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getMessage()).isEqualTo("Something went wrong. Please try again.");
        assertThat(body.getCode()).isEqualTo("SERVER_500");
    }

    // ================= TEST 18: Handle ExpiredJwtException =================
    @Test
    @DisplayName("TC18: Handle ExpiredJwtException")
    void testHandleExpiredJwt() {
        // Given
        io.jsonwebtoken.ExpiredJwtException ex = mock(io.jsonwebtoken.ExpiredJwtException.class);

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleExpiredJwt(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body.getMessage()).isEqualTo("JWT token has expired. Please login again.");
        assertThat(body.getCode()).isEqualTo("TOKEN_EXPIRED_401");
    }

    // ================= TEST 19: Handle custom AccessDeniedException =================
    @Test
    @DisplayName("TC19: Handle custom AccessDeniedException")
    void testHandleCustomAccessDenied() {
        // Given
        in.bank.exception.AccessDeniedException ex = 
            new in.bank.exception.AccessDeniedException("Access denied");

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleCustomAccessDenied(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body.getMessage()).isEqualTo("You are not authorized to perform this action");
        assertThat(body.getCode()).isEqualTo("FORBIDDEN_403");
    }

    // ================= TEST 20: Handle JwtException =================
    @Test
    @DisplayName("TC20: Handle JwtException")
    void testHandleInvalidJwt() {
        // Given
        io.jsonwebtoken.JwtException ex = mock(io.jsonwebtoken.JwtException.class);

        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleInvalidJwt(ex);
        ApiResponse<Object> body = response.getBody();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body.getMessage()).isEqualTo("Invalid JWT token");
        assertThat(body.getCode()).isEqualTo("INVALID_TOKEN_401");
    }
    @Test
    @DisplayName("TC21: Handle multiple validation errors for the same field (tests merge function)")
    void testHandleValidation_MultipleErrorsForSameField() throws Exception {
        // Given - Create a MethodArgumentNotValidException with multiple errors for the same field
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        // Create multiple FieldError objects for the same field "email"
        FieldError error1 = new FieldError("object", "email", "Email cannot be blank");
        FieldError error2 = new FieldError("object", "email", "Invalid email format");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
        
        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleValidation(ex);
        ApiResponse<Object> body = response.getBody();
        
        // Then - Verify the merge function combined the messages
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getStatus()).isEqualTo("ERROR");
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getCode()).isEqualTo("VALIDATION_400");
        
        // Verify that the merge function worked (messages combined with "; ")
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.getData();
        assertThat(errors).containsKey("email");
        assertThat(errors.get("email")).contains("Email cannot be blank");
        assertThat(errors.get("email")).contains("Invalid email format");
        assertThat(errors.get("email")).contains("; ");
    }
    
    @Test
    @DisplayName("TC22: Handle multiple constraint violations for same field (tests merge function)")
    void testHandleConstraintViolation_MultipleErrorsForSameField() {
        // Given
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        
        when(path.toString()).thenReturn("email");
        when(violation1.getPropertyPath()).thenReturn(path);
        when(violation1.getMessage()).thenReturn("Email cannot be blank");
        when(violation2.getPropertyPath()).thenReturn(path);
        when(violation2.getMessage()).thenReturn("Invalid email format");
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation1, violation2));
        
        // When
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleConstraintViolation(ex);
        ApiResponse<Object> body = response.getBody();
        
        // Then - Verify the merge function combined the messages
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.getData();
        assertThat(errors).containsKey("email");
        assertThat(errors.get("email")).contains("Email cannot be blank");
        assertThat(errors.get("email")).contains("Invalid email format");
        assertThat(errors.get("email")).contains("; ");
    }
    
}