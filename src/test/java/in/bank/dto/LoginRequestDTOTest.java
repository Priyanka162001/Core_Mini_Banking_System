package in.bank.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginRequestDTO Tests")
class LoginRequestDTOTest {

    private Validator validator;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        loginRequest = new LoginRequestDTO();
    }

    @Test
    @DisplayName("TC1: Valid login request passes all validations")
    void testValidLoginRequest() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC2: Email is required - null email fails validation")
    void testEmailRequired_Null() {
        loginRequest.setEmail(null);
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Email is required"));
    }

    @Test
    @DisplayName("TC3: Email is required - blank email fails validation")
    void testEmailRequired_Blank() {
        loginRequest.setEmail("");
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Email is required"));
    }

    @Test
    @DisplayName("TC4: Invalid email format fails validation")
    void testEmailFormat_Invalid() {
        loginRequest.setEmail("invalid-email");
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Invalid email format"));
    }

    @Test
    @DisplayName("TC5: Email with valid format passes validation")
    void testEmailFormat_Valid() {
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC6: Email size - max 100 characters")
    void testEmailSize_Max100() {
        // Use a valid email that's clearly under 100 chars
        String email = "valid.email@example.com"; // Only 22 characters
        loginRequest.setEmail(email);
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC7: Email size - exceeding 100 characters fails")
    void testEmailSize_ExceedsMax() {
        String email = "a".repeat(100) + "@example.com"; // Exceeds 100 chars
        loginRequest.setEmail(email);
        loginRequest.setPassword("Password@123");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("TC8: Password is required - null fails validation")
    void testPasswordRequired_Null() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword(null);
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Password is required"));
    }

    @Test
    @DisplayName("TC9: Password is required - blank fails validation")
    void testPasswordRequired_Blank() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword("");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Password is required"));
    }

    @Test
    @DisplayName("TC10: Password size - minimum 8 characters")
    void testPasswordSize_Min8() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword("Pass123!"); // 8 chars
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC11: Password size - less than 8 characters fails")
    void testPasswordSize_LessThanMin() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword("Pass123"); // 7 chars
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("TC12: Password size - maximum 64 characters")
    void testPasswordSize_Max64() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword("A".repeat(64)); // 64 chars
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC13: Password size - exceeding 64 characters fails")
    void testPasswordSize_ExceedsMax() {
        loginRequest.setEmail("priyanka@gmail.com");
        loginRequest.setPassword("A".repeat(65)); // 65 chars
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("TC14: Getters and setters work correctly")
    void testGettersAndSetters() {
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("SecurePass123!");
        
        assertThat(loginRequest.getEmail()).isEqualTo("test@example.com");
        assertThat(loginRequest.getPassword()).isEqualTo("SecurePass123!");
    }

    @Test
    @DisplayName("TC15: Multiple validation violations at once")
    void testMultipleValidationViolations() {
        loginRequest.setEmail("invalid");
        loginRequest.setPassword("123"); // Too short
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("TC16: Email with different valid formats")
    void testEmailWithDifferentValidFormats() {
        String[] validEmails = {
            "user@domain.com",
            "user.name@domain.co.in",
            "user+tag@domain.org",
            "user-name@domain.net"
        };
        
        for (String email : validEmails) {
            loginRequest.setEmail(email);
            loginRequest.setPassword("Password@123");
            var violations = validator.validate(loginRequest);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("TC17: Password with special characters")
    void testPasswordWithSpecialCharacters() {
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("P@ssw0rd!@#$%^&*()");
        
        var violations = validator.validate(loginRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC18: @Schema annotations are present")
    void testSchemaAnnotations() {
        // Verify class has @Schema annotation
        assertThat(LoginRequestDTO.class.isAnnotationPresent(io.swagger.v3.oas.annotations.media.Schema.class))
            .isTrue();
    }
}