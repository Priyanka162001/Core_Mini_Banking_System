package in.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Login request payload")
public class LoginRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    @Schema(description = "User email", example = "priyanka@gmail.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64)
    @Schema(description = "User password", example = "Password@123")
    private String password;
}