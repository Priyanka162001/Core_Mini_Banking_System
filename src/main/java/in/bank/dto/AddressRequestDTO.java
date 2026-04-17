package in.bank.dto;

import in.bank.entity.AddressType;
import jakarta.validation.constraints.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing an address for a customer")
public class AddressRequestDTO {

    @NotNull(message = "Address type is required")
    @Schema(description = "Type of the address", example = "PERMANENT", required = true, allowableValues = {"PERMANENT", "CORRESPONDENCE"})
    private AddressType addressType;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must not exceed 255 characters")
    @Schema(description = "Street address or building details", example = "123 MG Road", required = true)
    private String addressLine;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "City must contain letters only")
    @Schema(description = "City name", example = "Pune", required = true)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "State must contain letters only")
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