package in.bank.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SavingsAccountRequestDTO {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Savings product id is required")
    private Long savingsProductId;

    @NotNull(message = "Opening balance required")
    @DecimalMin(value = "0.0", message = "Opening balance must be >= 0")
    private BigDecimal openingBalance;
}