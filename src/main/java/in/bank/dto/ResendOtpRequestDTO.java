package in.bank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "ResendOtpRequestDTO", description = "Request object to resend OTP to the user's email")
public class ResendOtpRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(
        description = "The email address of the user to which the OTP should be resent",
        example = "user@example.com",
        required = true
    )
    private String email;
}