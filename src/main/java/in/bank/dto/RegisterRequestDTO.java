package in.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for registering a new user")  // ✅ Add this
public class RegisterRequestDTO {

    @Schema(example = "John")   // ✅ Add example to each field
    @NotBlank(message = "First name is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z\\s'-]+$", message = "Invalid first name")
    private String firstName;

    @Schema(example = "Doe")
    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z\\s'-]+$", message = "Invalid first name")
    private String lastName;

    @Schema(example = "john@example.com")
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Size(max = 100)
    private String email;

    @Schema(example = "+91")
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{0,2}$", message = "Invalid country code")
    private String countryCode;

    @Schema(example = "9876543210")
    @NotBlank
    @Pattern(regexp = "[6-9]\\d{9}")
    private String phoneNumber;

    @Schema(example = "Pass@1234")
    @NotBlank
    @Pattern(
    		  regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
    		  message = "Password must be 8+ chars with uppercase, lowercase, number & special character"
    		)
    private String password;

    @Schema(example = "Pass@1234")
    @NotBlank
    private String confirmPassword;
}