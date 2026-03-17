package in.bank.dto;

import in.bank.entity.GenderType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompleteProfileRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Username must contain only letters")
    private String username;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private GenderType gender;

    @NotBlank(message = "Permanent address is required")
    @Size(max = 255, message = "Permanent address too long")
    private String permanentAddressLine;

    @NotBlank(message = "Current address is required")
    @Size(max = 255, message = "Current address too long")
    private String currentAddressLine;

    @NotBlank(message = "City is required")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "City must contain only letters")
    private String city;

    @NotBlank(message = "State is required")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "State must contain only letters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be 6 digits")
    private String postalCode;

}