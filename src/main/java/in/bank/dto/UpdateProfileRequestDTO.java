package in.bank.dto;

import in.bank.entity.AddressType;
import in.bank.entity.GenderType;
import jakarta.validation.constraints.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for updating customer profile. Fields are optional, but validated if provided.")
public class UpdateProfileRequestDTO {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    @Schema(description = "Unique username of the customer", example = "john_doe123")
    private String username;

    @Schema(description = "Gender of the customer", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private GenderType gender;


    @Schema(description = "Type of the address", example = "PERMANENT", allowableValues = {"PERMANENT", "CORRESPONDENCE"})
    private AddressType addressType;

    @Size(max = 255, message = "Address line must not exceed 255 characters")
    @Schema(description = "Street address or building details", example = "123 MG Road")
    private String addressLine;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "City must contain letters only")
    @Schema(description = "City name", example = "Pune")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "State must contain letters only")
    @Schema(description = "State name", example = "Maharashtra")
    private String state;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid Indian postal code")
    @Schema(description = "6-digit postal code", example = "411001")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country name", example = "India")
    private String country;
}