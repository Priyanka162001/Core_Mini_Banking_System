package in.bank.dto;

import in.bank.entity.AddressType;
import in.bank.entity.GenderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for completing customer profile")
public class CompleteProfileRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    @Schema(description = "Unique username of the customer", example = "john_doe123", required = true)
    private String username;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Customer's date of birth", example = "1990-05-20", required = true, type = "string", format = "date")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Schema(description = "Gender of the customer", example = "MALE", required = true)
    private GenderType gender;

    @NotEmpty(message = "At least one address is required")
    @Valid
    @Schema(description = "List of customer addresses", required = true)
    private List<AddressRequestDTO> addresses;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Address details")
    public static class AddressRequestDTO {

        @NotNull(message = "Address type is required")
        @Schema(description = "Type of the address", example = "PERMANENT", required = true)
        private AddressType addressType;

        @NotBlank(message = "Address line is required")
        @Size(max = 255, message = "Address line must not exceed 255 characters")
        @Schema(description = "Street address or building details", example = "123 MG Road", required = true)
        private String addressLine;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        @Schema(description = "City name", example = "Pune", required = true)
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        @Schema(description = "State name", example = "Maharashtra", required = true)
        private String state;

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid Indian postal code")
        @Schema(description = "6-digit postal code", example = "411001", required = true)
        private String postalCode;

        @Size(max = 100, message = "Country must not exceed 100 characters")
        @Schema(description = "Country name", example = "India")
        private String country;

        @Schema(description = "Whether this address is currently active", example = "true")
        private Boolean isActive;
    }
}