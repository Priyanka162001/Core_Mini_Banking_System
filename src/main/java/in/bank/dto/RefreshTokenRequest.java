package in.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "RefreshTokenRequest", description = "Request object for refreshing an access token using a refresh token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(
        description = "The refresh token issued previously to the user for obtaining a new access token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        required = true
    )
    private String refreshToken;
}