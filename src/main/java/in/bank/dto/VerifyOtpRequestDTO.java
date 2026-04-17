package in.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "priyanka@gmail.com")
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    @Schema(example = "123456")
    private String otp;
}