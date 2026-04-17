package in.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
    name = "KycRequestDTO",
    description = "Request body for submitting KYC documents for verification"
)
public class KycRequestDTO {

    @Schema(
        description = "Type of KYC document",
        example = "AADHAAR",
        required = true,
        allowableValues = {"PAN", "AADHAAR", "PASSPORT", "DRIVING_LICENSE", "VOTER_ID"}
    )
    @NotBlank(message = "Document type is required")
    @Pattern(
        regexp = "^(PAN|AADHAAR|PASSPORT|DRIVING_LICENSE|VOTER_ID)$",
        message = "docType must be one of: PAN, AADHAAR, PASSPORT, DRIVING_LICENSE, VOTER_ID"
    )
    private String docType;

    @Schema(
        description = "Number of the KYC document",
        example = "123456789012",
        required = true,
        minLength = 5,
        maxLength = 20
    )
    @NotBlank(message = "Document number is required")
    @Size(min = 5, max = 20, message = "Document number must be between 5 and 20 characters")
    @Pattern(
        regexp = "^[A-Z0-9 ]+$", // spaces allowed for Aadhaar
        message = "Document number must contain only uppercase letters, digits, or spaces"
    )
    private String docNumber;

    @Schema(
        description = "URL of the uploaded document image or PDF",
        example = "https://example.com/aadhaar.jpg",
        required = true,
        format = "uri"
    )
    @NotBlank(message = "Document image URL is required")
    @Pattern(
        regexp = "^(https?://).+\\.(jpg|jpeg|png|pdf|svg)$",
        message = "Must be a valid image URL (jpg, jpeg, png, pdf, svg)"
    )
    private String documentImageUrl;
}