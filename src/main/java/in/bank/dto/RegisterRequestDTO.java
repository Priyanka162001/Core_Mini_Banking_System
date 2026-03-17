package in.bank.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be less than 50 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "First name must contain only letters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "Last name must contain only letters")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "\\+\\d{1,3}", message = "Country code must be like +91")
    private String countryCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "[6-9]\\d{9}", message = "Invalid Indian phone number")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
        message = "Password must be 8+ chars with uppercase, lowercase, number & special char"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}