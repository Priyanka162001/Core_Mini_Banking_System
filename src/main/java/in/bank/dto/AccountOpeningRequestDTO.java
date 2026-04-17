package in.bank.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "AccountOpeningRequestDTO", description = "Request object for opening a new savings account")
public class AccountOpeningRequestDTO {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the savings product for the account", example = "1", required = true)
    private Long productId;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Deposit must be greater than 0")
    @Schema(description = "Initial deposit amount for the account, must be greater than 0", example = "5000.00", required = true)
    private BigDecimal initialDeposit;
}