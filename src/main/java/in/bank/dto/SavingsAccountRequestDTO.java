package in.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Request body for creating a savings account")
public class SavingsAccountRequestDTO {

    @Schema(description = "User ID", example = "1")
    @NotNull(message = "User id is required")
    private Long userId;

    @Schema(description = "Savings Product ID", example = "1")
    @NotNull(message = "Savings product id is required")
    private Long savingsProductId;

    @Schema(description = "Opening balance amount", example = "1000.00")
    @NotNull(message = "Opening balance required")
    @DecimalMin(value = "0.0", message = "Opening balance must be >= 0")
    private BigDecimal openingBalance;
}